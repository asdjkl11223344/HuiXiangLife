package com.huixiang.integration;

import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.MqConstant;
import com.huixiang.entity.Product;
import com.huixiang.service.SeckillService;
import com.huixiang.service.impl.SeckillServiceImpl;
import com.huixiang.vo.SeckillResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Disabled("Skip integration tests in CI environment as it requires external middleware")
class LocalMiddlewareIntegrationTest {

    private static final AppDevConfig CONFIG = AppDevConfig.load();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private LettuceConnectionFactory redisConnectionFactory;
    private StringRedisTemplate stringRedisTemplate;
    private Long redisTestProductId;

    @AfterEach
    void tearDown() {
        if (redisTestProductId != null && stringRedisTemplate != null) {
            deleteRedisTestKeys(redisTestProductId);
        }
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @Test
    void mysqlShouldContainSeedProductsAndMerchants() throws Exception {
        assumeTrue(isPortOpen("localhost", extractPortFromJdbcUrl(CONFIG.datasourceUrl)),
                "MySQL 未启动，跳过真实数据库集成测试");

        try (Connection connection = DriverManager.getConnection(
                CONFIG.datasourceUrl,
                CONFIG.datasourceUsername,
                CONFIG.datasourcePassword
        )) {
            assertEquals(2, countRows(connection,
                    "SELECT COUNT(1) FROM product WHERE id IN (2001, 2002) AND deleted = 0"));
            assertEquals(2, countRows(connection,
                    "SELECT COUNT(1) FROM merchant WHERE id IN (1001, 1002) AND deleted = 0"));

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, sub_title FROM product WHERE id = 2001");
                 ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("双人火锅套餐", resultSet.getString("name"));
                assertEquals("周末通用", resultSet.getString("sub_title"));
            }
        }
    }

    @Test
    void redisSeckillShouldWorkAgainstRealRedis() {
        assumeTrue(isPortOpen(CONFIG.redisHost, CONFIG.redisPort),
                "Redis 未启动，跳过真实 Redis 集成测试");

        initRedisTemplate();
        SeckillService seckillService = new SeckillServiceImpl(stringRedisTemplate);

        redisTestProductId = System.currentTimeMillis();
        Product product = new Product();
        product.setId(redisTestProductId);
        product.setStock(1);
        product.setEndTime(LocalDateTime.now().plusHours(2));

        seckillService.preheatStock(product);

        int firstSubmit = seckillService.tryPreDeduct(9001L, product, "req-" + UUID.randomUUID());
        int repeatSubmit = seckillService.tryPreDeduct(9001L, product, "req-" + UUID.randomUUID());
        int soldOutSubmit = seckillService.tryPreDeduct(9002L, product, "req-" + UUID.randomUUID());
        SeckillResultVO resultVO = seckillService.getResult(9001L, redisTestProductId);

        assertEquals(SeckillService.PRE_DEDUCT_SUCCESS, firstSubmit);
        assertEquals(SeckillService.PRE_DEDUCT_REPEAT, repeatSubmit);
        assertEquals(SeckillService.PRE_DEDUCT_SOLD_OUT, soldOutSubmit);
        assertEquals(SeckillService.RESULT_CODE_PENDING, resultVO.getStatusCode());
    }

    @Test
    void elasticsearchShouldReturnSeedSearchHits() throws Exception {
        assumeTrue(isPortOpen(CONFIG.elasticsearchHost, CONFIG.elasticsearchPort),
                "Elasticsearch 未启动，跳过真实 Elasticsearch 集成测试");

        HttpResponse<String> productResponse = postJson(
                CONFIG.elasticsearchUri + "/product/_search",
                """
                {
                  "query": {
                    "multi_match": {
                      "query": "周末",
                      "fields": ["name^4", "subTitle^2", "content", "merchantName^2"]
                    }
                  },
                  "size": 5
                }
                """
        );
        HttpResponse<String> merchantResponse = postJson(
                CONFIG.elasticsearchUri + "/merchant/_search",
                """
                {
                  "query": {
                    "multi_match": {
                      "query": "连锁",
                      "fields": ["name^4", "description^2", "address", "categoryName^2"]
                    }
                  },
                  "size": 5
                }
                """
        );

        assertEquals(200, productResponse.statusCode());
        assertEquals(200, merchantResponse.statusCode());
        assertTrue(productResponse.body().contains("\"_id\":\"2001\""));
        assertTrue(merchantResponse.body().contains("\"_id\":\"1002\""));
    }

    @Test
    void rabbitMqShouldAcceptProjectTopology() {
        assumeTrue(isPortOpen(CONFIG.rabbitHost, CONFIG.rabbitPort),
                "RabbitMQ 未启动，跳过真实 RabbitMQ 集成测试");

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(CONFIG.rabbitHost, CONFIG.rabbitPort);
        connectionFactory.setUsername(CONFIG.rabbitUsername);
        connectionFactory.setPassword(CONFIG.rabbitPassword);
        connectionFactory.setVirtualHost(CONFIG.rabbitVirtualHost);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        try {
            rabbitTemplate.execute(channel -> {
                channel.exchangeDeclare(MqConstant.ASYNC_ORDER_EXCHANGE, "direct", true);
                channel.queueDeclare(MqConstant.ASYNC_ORDER_CREATE_QUEUE, true, false, false, null);
                channel.queueBind(
                        MqConstant.ASYNC_ORDER_CREATE_QUEUE,
                        MqConstant.ASYNC_ORDER_EXCHANGE,
                        MqConstant.ASYNC_ORDER_CREATE_ROUTING_KEY
                );
                channel.exchangeDeclarePassive(MqConstant.ASYNC_ORDER_EXCHANGE);
                var queueInfo = channel.queueDeclarePassive(MqConstant.ASYNC_ORDER_CREATE_QUEUE);
                assertEquals(MqConstant.ASYNC_ORDER_CREATE_QUEUE, queueInfo.getQueue());
                return null;
            });
        } finally {
            connectionFactory.destroy();
        }
    }

    private void initRedisTemplate() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(CONFIG.redisHost, CONFIG.redisPort);
        configuration.setDatabase(CONFIG.redisDatabase);
        if (CONFIG.redisPassword != null && !CONFIG.redisPassword.isBlank()) {
            configuration.setPassword(CONFIG.redisPassword);
        }
        redisConnectionFactory = new LettuceConnectionFactory(configuration);
        redisConnectionFactory.afterPropertiesSet();
        stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        stringRedisTemplate.afterPropertiesSet();
    }

    private void deleteRedisTestKeys(Long productId) {
        stringRedisTemplate.delete(List.of(
                CacheConstant.SECKILL_STOCK_KEY_PREFIX + productId,
                CacheConstant.SECKILL_USER_ORDER_KEY_PREFIX + "9001:" + productId,
                CacheConstant.SECKILL_USER_ORDER_KEY_PREFIX + "9002:" + productId,
                CacheConstant.SECKILL_RESULT_KEY_PREFIX + "9001:" + productId,
                CacheConstant.SECKILL_RESULT_KEY_PREFIX + "9002:" + productId
        ));
    }

    private HttpResponse<String> postJson(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int extractPortFromJdbcUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile(":(\\d+)/").matcher(jdbcUrl);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 3306;
    }

    private long countRows(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }

    private record AppDevConfig(
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword,
            String redisHost,
            int redisPort,
            int redisDatabase,
            String redisPassword,
            String rabbitHost,
            int rabbitPort,
            String rabbitUsername,
            String rabbitPassword,
            String rabbitVirtualHost,
            String elasticsearchUri,
            String elasticsearchHost,
            int elasticsearchPort
    ) {
        private static AppDevConfig load() {
            YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
            factoryBean.setResources(new ClassPathResource("application-dev.yml"));
            Properties properties = factoryBean.getObject();
            if (properties == null) {
                throw new IllegalStateException("无法读取 application-dev.yml");
            }
            String esUri = properties.getProperty("spring.elasticsearch.uris");
            URI uri = URI.create(esUri);
            return new AppDevConfig(
                    properties.getProperty("spring.datasource.url"),
                    properties.getProperty("spring.datasource.username"),
                    properties.getProperty("spring.datasource.password"),
                    properties.getProperty("spring.data.redis.host"),
                    Integer.parseInt(properties.getProperty("spring.data.redis.port", "6379")),
                    Integer.parseInt(properties.getProperty("spring.data.redis.database", "0")),
                    properties.getProperty("spring.data.redis.password", ""),
                    properties.getProperty("spring.rabbitmq.host"),
                    Integer.parseInt(properties.getProperty("spring.rabbitmq.port", "5672")),
                    properties.getProperty("spring.rabbitmq.username"),
                    properties.getProperty("spring.rabbitmq.password"),
                    properties.getProperty("spring.rabbitmq.virtual-host", "/"),
                    esUri,
                    uri.getHost(),
                    uri.getPort() > 0 ? uri.getPort() : 9200
            );
        }
    }
}
