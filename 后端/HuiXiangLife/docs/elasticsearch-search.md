# Elasticsearch 搜索说明

## 目标

这份说明用于把当前项目的商品搜索能力和商户搜索能力切到 Elasticsearch，形成一个可演示、可联调、可兜底的最小可用版本。

当前实现是：

- 用户端关键字搜索优先走 Elasticsearch 召回商品 ID
- 用户端商户关键字搜索优先走 Elasticsearch 召回商户 ID
- MySQL 负责二次过滤、分页和最终商品详情返回
- MySQL 负责二次过滤、分页和最终商户详情返回
- Elasticsearch 不可用时自动回退到 MySQL `like` 查询
- 商品新增、修改、上下架、删除时会同步搜索索引
- 商户新增、修改、状态变更、删除时会同步搜索索引

## 前置条件

开始联调前，请先自己确认下面服务都已经启动：

- 后端服务
- MySQL
- Elasticsearch

当前开发配置使用的是：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

如果本机 `9200` 没有监听，`/admin/product/search/rebuild` 会直接报业务异常，用户端搜索则会自动回退到 MySQL。

## 已接入的能力

当前已经接入的 Elasticsearch 能力包括：

- 用户端商品关键字搜索优先走 ES
- 用户端商户关键字搜索优先走 ES
- 管理端支持全量重建商品索引
- 管理端支持全量重建商户索引
- 管理端支持单个商品手动同步索引
- 管理端支持单个商户手动同步索引
- 商品新增后自动同步索引
- 商品修改后自动同步索引
- 商品上下架后自动同步索引
- 商品删除后自动删除索引
- 商户新增后自动同步索引
- 商户修改后自动同步索引
- 商户启停用后自动同步索引
- 商户删除后自动删除索引

## 真实联调结果

当前这套搜索能力已经完成真实联调验证：

- 商品索引全量重建成功
- 商户索引全量重建成功
- 关键字 `周末` 可以命中商品 `2001`
- 关键字 `连锁` 可以命中商户 `1002`
- `LocalMiddlewareIntegrationTest` 已覆盖 Elasticsearch 商品/商户搜索命中验证

## 管理端接口

### 1. 单商品同步索引

```http
POST /admin/product/{id}/search/sync
```

用途：

- 适合在你手动改了商品数据后，立即把这一条商品重新同步到 ES
- 如果商品已下架，这次同步会把该商品从 ES 索引中移除

### 2. 全量重建索引

```http
POST /admin/product/search/rebuild
```

用途：

- 适合首次接入 ES 后做全量初始化
- 适合索引丢失、结构调整后重新构建
- 只会同步当前已上架商品

成功时返回重建条数。

### 3. 单商户同步索引

```http
POST /admin/merchant/{id}/search/sync
```

用途：

- 适合在你手动改了商户名称、简介、地址、分类后，立即把这一条商户重新同步到 ES
- 如果商户已停用，这次同步会把该商户从 ES 索引中移除

### 4. 删除商户

```http
DELETE /admin/merchant/{id}
```

用途：

- 用于删除没有商品、没有订单关联的商户
- 删除成功后会同步清理该商户的 ES 索引和相关缓存
- 如果商户下仍有关联商品或订单，接口会直接拒绝删除

### 5. 全量重建商户索引

```http
POST /admin/merchant/search/rebuild
```

用途：

- 适合首次接入商户搜索 ES 化后做全量初始化
- 适合索引丢失、字段调整后重新构建
- 只会同步当前启用中的商户

## 接口文档

这轮新增的搜索管理接口已经补进 [huixianglife-openapi.json](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/apifox/huixianglife-openapi.json)，导入 Apifox 后可以直接调试：

- `POST /admin/product/{id}/search/sync`
- `POST /admin/product/search/rebuild`
- `DELETE /admin/merchant/{id}`
- `POST /admin/merchant/{id}/search/sync`
- `POST /admin/merchant/search/rebuild`

## 推荐联调流程

1. 先确认 Elasticsearch 已启动，并且 `9200` 可以访问。
2. 启动后端服务。
3. 调用 `POST /admin/product/search/rebuild` 初始化商品索引。
4. 调用 `POST /admin/merchant/search/rebuild` 初始化商户索引。
5. 如需验证单条同步，再调用 `POST /admin/product/{id}/search/sync` 或 `POST /admin/merchant/{id}/search/sync`。
6. 使用用户端商品分页接口或商户分页接口，带上 `keyword` 参数验证搜索效果。

## 验证建议

建议至少验证下面几种场景：

1. 商品名称命中关键字，能正常搜到。
2. 修改商品名称后，重新同步索引，搜索结果随之变化。
3. 商品下架后重新同步，该商品不再被搜索到。
4. 商户名称、分类名或简介命中关键字，能正常搜到。
5. 商户停用后重新同步，该商户不再被搜索到。
6. Elasticsearch 停掉后，搜索接口仍能走 MySQL 兜底返回结果。

## 代码落点

这次商品搜索 ES 化的核心代码主要在：

- [ProductDocument.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/search/document/ProductDocument.java)
- [MerchantDocument.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/search/document/MerchantDocument.java)
- [ProductSearchRepository.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/search/repository/ProductSearchRepository.java)
- [MerchantSearchRepository.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/search/repository/MerchantSearchRepository.java)
- [ProductSearchIndexService.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/ProductSearchIndexService.java)
- [MerchantSearchIndexService.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/MerchantSearchIndexService.java)
- [ProductSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductSearchIndexServiceImpl.java)
- [MerchantSearchIndexServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantSearchIndexServiceImpl.java)
- [ProductServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/ProductServiceImpl.java)
- [MerchantServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/MerchantServiceImpl.java)
- [AdminProductController.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/controller/admin/AdminProductController.java)
- [AdminMerchantController.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/controller/admin/AdminMerchantController.java)

## 面试可讲的点

你可以这样介绍这部分改造：

- 之前项目虽然引了 Elasticsearch 依赖，但商品搜索实际还是走 MySQL `like`
- 现在把商品搜索和商户搜索都改成了 ES 召回 + MySQL 详情返回的组合方案
- 这样既能体现全文检索能力，也不需要一次性改掉原有分页和业务过滤逻辑
- 同时保留了 MySQL 兜底，避免 ES 不可用时用户端完全不可搜
- 管理端补了商品/商户两套单条同步和全量重建接口，方便演示和联调
