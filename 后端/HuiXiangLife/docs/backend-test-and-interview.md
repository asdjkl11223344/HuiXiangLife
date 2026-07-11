# 后端测试结果与面试话术

## 文档目标

这份文档用于把当前项目后端已经完成的能力、测试结果和面试表达整理成一套可以直接复用的材料。

适用场景：

- 你自己回顾项目时快速抓重点
- 简历项目讲解
- 面试中回答“你这个项目做了什么优化”“怎么验证效果”“有什么亮点”

## 当前后端能力总览

目前这个项目后端已经形成了 4 条比较完整的能力线：

1. 秒杀高并发治理
2. 商品搜索与商户搜索 Elasticsearch 化
3. 管理端操作日志审计
4. 自动化测试与真实中间件集成测试

你可以把项目整体概括成一句话：

> 我把一个本地生活类 Spring Boot 项目，重点往高并发秒杀、全文检索、后台可审计和可验证性这几个方向做了增强，让它不仅能跑，还能讲清楚、能联调、能验证。

## 一、秒杀高并发治理

### 做了什么

- 秒杀入口先走 Redis + Lua 原子预扣库存
- 重复抢购会直接拦截
- 抢完后会直接返回统一失败码
- 建单不在请求线程里同步完成，而是通过 RabbitMQ 异步建单削峰
- MySQL 最终扣减库存时使用原子 SQL，保证不会超卖
- 管理端补了预热、重置、批量操作、状态查询、定时预热这些调试能力

### 当前结果

- 已完成秒杀提交接口失败码统一
- 已完成售罄场景压测验证
- 已形成 `Redis 预扣 -> MQ 异步建单 -> MySQL 最终一致性` 闭环

### 关键失败码

- `101`：重复抢购
- `102`：商品抢完
- `103`：活动未开始
- `104`：活动已结束
- `199`：系统繁忙

### 可引用材料

- 秒杀压测说明见 [seckill-pressure-test.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/seckill-pressure-test.md)
- 秒杀服务核心实现见 [SeckillServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/SeckillServiceImpl.java)
- 秒杀提交核心逻辑见 [OrderServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/OrderServiceImpl.java)

### 面试话术

你可以这样讲：

> 这个项目里我专门把秒杀链路做了一轮高并发治理。入口先走 Redis + Lua 原子预扣库存，把重复请求和售罄请求挡在数据库外面；真正建单通过 RabbitMQ 异步化，避免流量高峰直接打满数据库；最终 MySQL 侧再用原子 SQL 做库存扣减，保证不会超卖。为了便于联调和演示，我还补了库存预热、状态重置、批量操作和压测脚本，最后做了售罄场景压测，验证了 50 个用户抢 10 个库存时没有超卖。

## 二、Elasticsearch 搜索改造

### 做了什么

- 商品搜索从 MySQL `like` 升级为 Elasticsearch 全文召回
- 商户搜索也切到了 Elasticsearch
- 用户端搜索采用 `ES 召回 ID + MySQL 回表过滤/分页` 的组合方式
- 商品新增、修改、上下架、删除时会同步 ES 索引
- 商户新增、修改、启停用、删除时会同步 ES 索引
- 管理端补了单条同步和全量重建接口
- ES 不可用时，用户端自动回退到 MySQL 查询

### 为什么这样做

这套方案比较适合简历项目：

- 能体现你用过 Elasticsearch
- 不需要一次性推翻原有 MySQL 分页逻辑
- 即使 ES 暂时不可用，项目仍然可用
- 技术上容易解释，演示时也容易看到效果

### 当前结果

- 商品搜索 ES 化已联调通过
- 商户搜索 ES 化已联调通过
- 已验证关键字命中副标题、简介等非名称字段时，确实走了 ES，而不是只走 MySQL `like`

### 可引用材料

- 搜索说明见 [elasticsearch-search.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/elasticsearch-search.md)
- 商品搜索核心实现见 [ProductServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductServiceImpl.java)
- 商户搜索核心实现见 [MerchantServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantServiceImpl.java)
- 商品索引服务见 [ProductSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductSearchIndexServiceImpl.java)
- 商户索引服务见 [MerchantSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantSearchIndexServiceImpl.java)

### 面试话术

你可以这样讲：

> 我发现项目里虽然引了 Elasticsearch 依赖，但原来的搜索其实还是 MySQL `like`，所以我把商品搜索和商户搜索都改成了 ES 召回。具体不是让 ES 直接返回完整分页结果，而是先用 ES 按关键字召回商品或商户 ID，再由 MySQL 回表做业务过滤和分页，这样改造成本更低，也能保留原来系统的分页和过滤逻辑。另外我还保留了 MySQL 兜底，这样 Elasticsearch 挂掉时搜索功能不会完全不可用。

## 三、管理端操作日志

### 做了什么

- 给 `/admin/**` 的成功写操作统一加了操作日志记录
- 自动记录操作人、模块、动作、业务 ID、请求详情和来源 IP
- 提供管理端分页查询接口，支持按模块、动作、业务 ID 等条件筛选

### 当前价值

- 便于排查“谁改了哪个商品/商户”
- 便于演示后台管理能力
- 面试时可以讲“后台可审计”

### 可引用材料

- 说明文档见 [admin-operation-log.md](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/admin-operation-log.md)
- 拦截器见 [AdminOperationLogInterceptor.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/interceptor/AdminOperationLogInterceptor.java)
- 服务实现见 [OperationLogServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/OperationLogServiceImpl.java)

### 面试话术

你可以这样讲：

> 为了让后台操作可追踪，我额外给管理端写接口补了一层统一操作日志。用户每次在后台做新增、修改、删除、重建索引这类动作，系统都会自动记录操作人、模块、动作、业务 ID 和 IP。这样不仅便于排查问题，也让这个项目更像一个真实可运维的后台系统，而不只是把 CRUD 写完。

## 四、自动化测试结果

### 已完成的测试层次

目前项目已经有 3 层测试：

1. 单元测试
2. 控制器层测试
3. 真实中间件集成测试

### 单元测试覆盖

已覆盖的重点包括：

- 秒杀提交成功、重复抢购、MQ 失败回滚、活动未开始
- 商品搜索 ES 召回顺序、MySQL 兜底、空结果返回
- 商户删除限制、商户搜索排序
- 操作日志落库字段解析与分页结果组装

对应测试文件：

- [OrderServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/OrderServiceImplTest.java)
- [ProductServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/ProductServiceImplTest.java)
- [MerchantServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/MerchantServiceImplTest.java)
- [OperationLogServiceImplTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/service/impl/OperationLogServiceImplTest.java)

### 控制器层测试覆盖

已覆盖的接口包括：

- `DELETE /admin/merchant/{id}`
- `POST /admin/product/search/rebuild`
- `POST /admin/merchant/search/rebuild`
- `GET /admin/operation-log/page`

对应测试文件：

- [AdminMerchantControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminMerchantControllerTest.java)
- [AdminProductControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminProductControllerTest.java)
- [AdminOperationLogControllerTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/controller/admin/AdminOperationLogControllerTest.java)

### 真实中间件集成测试覆盖

已覆盖的真实依赖包括：

- MySQL
- Redis
- RabbitMQ
- Elasticsearch

对应测试文件：

- [LocalMiddlewareIntegrationTest.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/test/java/com/huixiang/integration/LocalMiddlewareIntegrationTest.java)

### 当前已验证结果

你现在可以明确讲这几条：

- 单元测试 + 控制器测试 + 真实中间件测试全部已经补上
- 真实中间件测试不是 Mock，而是直接连本机 MySQL / Redis / RabbitMQ / Elasticsearch
- 在中间件都启动后，`LocalMiddlewareIntegrationTest` 已经跑通，`4` 个用例全部通过
- 控制器测试和服务层测试累计已经跑通 `18` 个用例

### 面试话术

你可以这样讲：

> 为了避免项目只是“功能写完就算了”，我补了三层测试。第一层是服务层单元测试，重点验证秒杀失败码、搜索兜底、日志组装这些业务逻辑；第二层是控制器层测试，验证管理端接口的参数绑定和返回结构；第三层是真实中间件集成测试，直接连本机 MySQL、Redis、RabbitMQ 和 Elasticsearch，验证关键链路不是只在 Mock 环境里成立。这样项目的可信度会更高。

## 五、你在面试里可以怎么总讲

### 30 秒版本

> 这是一个本地生活类的 Spring Boot 项目，我重点做了三类增强：第一是秒杀高并发治理，使用 Redis 预扣库存、RabbitMQ 异步建单和 MySQL 原子扣减，验证了不会超卖；第二是把商品和商户搜索切到 Elasticsearch，实现 ES 召回加 MySQL 兜底；第三是补了后台操作日志和自动化测试，让项目不仅能跑，还能审计、能联调、能验证。

### 1 分钟版本

> 这个项目原来更偏基础 CRUD，我在后端侧做了一轮能力增强。秒杀这块我做了 Redis + Lua 原子预扣、RabbitMQ 异步建单和 MySQL 原子扣减，并配了压测脚本，验证售罄场景下不会超卖。搜索这块我把商品和商户搜索从 MySQL `like` 升级成了 Elasticsearch 召回，再由 MySQL 回表做过滤和分页，同时保留兜底逻辑。后台这边我又补了统一操作日志，把商品、商户、索引管理这些写操作都能审计。最后我补了单元测试、接口测试和真实中间件集成测试，保证这些能力不是只停留在代码层面。

### 被追问“为什么这样设计”时

你可以这样回答：

- 秒杀入口先挡在 Redis，是为了降低数据库瞬时压力
- 建单异步化，是为了削峰填谷，提高吞吐
- ES 只负责召回，不直接替代业务分页，是为了降低改造成本
- 保留 MySQL 兜底，是为了提升系统可用性
- 操作日志和测试，是为了让项目更像真实工程，而不是作业式项目

## 六、你现在最适合强调的亮点

简历或面试里，建议优先强调下面 4 点：

1. 做过高并发秒杀治理，并验证无超卖
2. 做过 Elasticsearch 搜索接入，并保留 MySQL 兜底
3. 做过后台操作审计
4. 做过自动化测试和真实中间件联调

## 七、建议截图留证

为了后面讲项目更有说服力，建议你保留这些截图：

1. 秒杀压测结果截图
2. `POST /admin/product/search/rebuild` 成功截图
3. `POST /admin/merchant/search/rebuild` 成功截图
4. `GET /admin/operation-log/page` 返回日志截图
5. `LocalMiddlewareIntegrationTest` 全部通过的测试结果截图

## 八、最后一句总结

如果面试官让你自己总结项目亮点，你可以收口成这句话：

> 这个项目最有代表性的地方，不是单纯把业务接口写完，而是我围绕高并发、搜索、后台审计和自动化验证，把它做成了一个更接近真实工程落地的后端项目。
