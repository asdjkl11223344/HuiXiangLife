# 后端开发文档

## 1. 文档目标

这份文档用于基于当前后端代码状态，整理项目的模块结构、中间件依赖、核心能力、联调方式和文档入口，方便后续继续开发、联调和项目讲解。

当前版本已经覆盖：

- 秒杀高并发治理
- 商品搜索与商户搜索 Elasticsearch 化
- 管理端操作日志审计
- 三层测试体系

## 2. 项目结构

项目是一个多模块 Maven 工程，主要包含下面 3 个模块：

- `huixiang-common`：通用常量、上下文、异常、统一返回结构、JWT 工具
- `huixiang-pojo`：实体、DTO、Query、VO
- `huixiang-server`：控制器、业务服务、Mapper、拦截器、任务、搜索索引、MQ 监听器

后端启动入口：

- [HuixiangApplication.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/HuixiangApplication.java)

## 3. 本地依赖

当前后端开发环境主要依赖下面这些中间件：

- MySQL：业务数据持久化
- Redis：秒杀库存预热、预扣、结果缓存、热点缓存
- RabbitMQ：秒杀异步建单、消息削峰
- Elasticsearch：商品搜索、商户搜索全文检索

当前开发配置见：

- [application-dev.yml](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/resources/application-dev.yml)

默认端口通常为：

- `3306`：MySQL
- `6379`：Redis
- `5672`：RabbitMQ
- `9200`：Elasticsearch
- `8080`：后端服务

## 4. 当前核心能力

### 4.1 秒杀高并发治理

当前秒杀链路是：

1. Redis + Lua 原子预扣库存
2. 重复抢购和售罄在入口快速拦截
3. RabbitMQ 异步建单削峰
4. MySQL 原子扣减库存，保证最终一致性

管理端已经补齐这些能力：

- 单商品预热库存
- 批量预热库存
- 单商品重置库存
- 批量重置库存
- 秒杀状态查询
- 定时扫描未来活动并预热

相关代码：

- [SeckillServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/SeckillServiceImpl.java)
- [OrderServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/OrderServiceImpl.java)
- [SeckillPreheatTask.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/task/SeckillPreheatTask.java)

### 4.2 搜索能力

当前搜索能力已经扩展到两条线：

- 商品搜索 ES 化
- 商户搜索 ES 化

实际方案不是让 ES 直接返回最终分页结果，而是：

1. Elasticsearch 先按关键字召回业务 ID
2. MySQL 再回表做过滤、分页和最终详情组装
3. ES 不可用时自动回退到 MySQL `like`

当前已经支持：

- 商品新增、修改、上下架、删除同步索引
- 商户新增、修改、状态变更、删除同步索引
- 管理端单条同步商品索引
- 管理端单条同步商户索引
- 管理端全量重建商品索引
- 管理端全量重建商户索引

相关代码：

- [ProductServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductServiceImpl.java)
- [MerchantServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantServiceImpl.java)
- [ProductSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductSearchIndexServiceImpl.java)
- [MerchantSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantSearchIndexServiceImpl.java)

### 4.3 管理端操作日志

当前管理端已经补了一层统一操作审计：

- 记录成功的 `POST`、`PUT`、`DELETE /admin/**`
- 自动落库 `operation_log`
- 支持按操作人、模块、动作、业务 ID 分页查询

相关代码：

- [AdminOperationLogInterceptor.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/interceptor/AdminOperationLogInterceptor.java)
- [OperationLogServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/OperationLogServiceImpl.java)
- [AdminOperationLogController.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/controller/admin/AdminOperationLogController.java)

## 5. 本轮重点新增接口

### 5.1 搜索索引管理

- `POST /admin/product/{id}/search/sync`
- `POST /admin/product/search/rebuild`
- `POST /admin/merchant/{id}/search/sync`
- `POST /admin/merchant/search/rebuild`

### 5.2 商户管理闭环

- `DELETE /admin/merchant/{id}`

删除规则：

- 商户下还有商品时不允许删除
- 商户下还有订单时不允许删除
- 删除成功后会同步删除 ES 索引并清理相关缓存

### 5.3 操作日志

- `GET /admin/operation-log/page`

## 6. 测试与验证

当前后端已经形成三层测试：

### 6.1 单元测试

覆盖重点：

- 秒杀失败码与消息发送回滚
- 商品搜索 ES 召回和 MySQL 兜底
- 商户删除校验与搜索排序
- 操作日志落库与分页结果组装

### 6.2 控制器测试

覆盖重点：

- 商户删除接口
- 商品索引重建接口
- 商户索引重建接口
- 操作日志分页接口

### 6.3 真实中间件集成测试

覆盖重点：

- MySQL 种子数据存在性
- Redis 秒杀预热与预扣行为
- RabbitMQ 拓扑可声明
- Elasticsearch 商品/商户搜索命中

测试文件入口：

- [OrderServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/OrderServiceImplTest.java)
- [ProductServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/ProductServiceImplTest.java)
- [MerchantServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/MerchantServiceImplTest.java)
- [OperationLogServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/OperationLogServiceImplTest.java)
- [AdminProductControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminProductControllerTest.java)
- [AdminMerchantControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminMerchantControllerTest.java)
- [AdminOperationLogControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminOperationLogControllerTest.java)
- [LocalMiddlewareIntegrationTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/integration/LocalMiddlewareIntegrationTest.java)

常用命令：

```bash
mvn -pl huixiang-server -am -DskipTests compile
```

```bash
mvn -pl huixiang-server -am "-Dtest=OrderServiceImplTest,ProductServiceImplTest,MerchantServiceImplTest,OperationLogServiceImplTest,AdminProductControllerTest,AdminMerchantControllerTest,AdminOperationLogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

```bash
mvn -pl huixiang-server -am "-Dtest=LocalMiddlewareIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## 7. 联调建议

推荐按下面顺序联调：

1. 确认 MySQL、Redis、RabbitMQ、Elasticsearch 和后端服务都已启动
2. 管理端登录获取 token
3. 先调用商品和商户搜索索引全量重建接口
4. 再验证用户端商品搜索和商户搜索
5. 调用一个管理端写接口后，再查操作日志分页
6. 最后跑一遍单元测试 / 控制器测试 / 真实中间件集成测试

## 8. 文档索引

专项文档如下：

- 搜索专项说明：[elasticsearch-search.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/elasticsearch-search.md)
- 操作日志说明：[admin-operation-log.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/admin-operation-log.md)
- 秒杀压测说明：[seckill-pressure-test.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/seckill-pressure-test.md)
- 测试结果与面试话术：[backend-test-and-interview.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/backend-test-and-interview.md)

接口文档入口：

- [huixianglife-openapi.json](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/apifox/huixianglife-openapi.json)
