# 管理端操作日志说明

## 目标

这份说明用于描述当前项目的管理端操作日志能力，方便你后续联调、演示和面试讲解。

当前实现是：

- 管理端成功的写操作会自动记录到 `operation_log` 表
- 记录范围覆盖 `POST`、`PUT`、`DELETE` 类型的 `/admin/**` 接口
- 日志包含操作人、模块、动作、业务 ID、详情、来源 IP、创建时间
- 提供管理端分页查询接口，方便直接查看最近操作记录

## 前置条件

开始使用前，请先自己确认下面服务都已经启动：

- 后端服务
- MySQL

## 自动记录规则

当前会自动记录下面这类管理端操作：

- 商品新增、修改、删除、状态修改
- 商品搜索索引单条同步、全量重建
- 商户新增、修改、删除、状态修改
- 商户搜索索引单条同步、全量重建
- 秒杀库存预热、重置、批量操作、手动触发
- 其他后续新增的管理端写接口

这轮已经实际联调并确认可记录的典型接口包括：

- `POST /admin/product/search/rebuild`
- `POST /admin/merchant/search/rebuild`
- `POST /admin/merchant/{id}/search/sync`
- `DELETE /admin/merchant/{id}`

当前不会记录：

- `GET` 查询接口
- 登录接口 `/admin/auth/login`
- 执行失败或返回错误状态码的请求

## 查询接口

### 操作日志分页

```http
GET /admin/operation-log/page
```

支持的查询参数：

- `pageNo`
- `pageSize`
- `operatorId`
- `module`
- `action`
- `bizId`

## 返回字段

分页记录中重点字段包括：

- `operatorId`：操作人 ID
- `operatorName`：操作人昵称
- `module`：操作模块，例如“商品管理”“商户管理”
- `action`：操作动作，例如“同步搜索索引”“修改状态”
- `bizId`：对应业务主键 ID
- `detail`：请求路径和控制器方法
- `ip`：来源 IP
- `createTime`：操作时间

## 推荐联调方式

1. 使用管理员账号登录。
2. 调用一个管理端写接口，例如 `POST /admin/merchant/{id}/search/sync`。
3. 再调用 `GET /admin/operation-log/page?pageNo=1&pageSize=10` 查看最新日志。
4. 必要时带上 `module` 或 `bizId` 过滤某一类操作。

## 接口文档

这轮新增的操作日志接口已经补进 [huixianglife-openapi.json](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/docs/apifox/huixianglife-openapi.json)，导入 Apifox 后可以直接调试：

- `GET /admin/operation-log/page`

## 开发落点

这部分能力的主要代码位置：

- [AdminOperationLogInterceptor.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/interceptor/AdminOperationLogInterceptor.java)
- [WebMvcConfig.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/config/WebMvcConfig.java)
- [OperationLogServiceImpl.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/service/impl/OperationLogServiceImpl.java)
- [AdminOperationLogController.java](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/huixiang-server/src/main/java/com/huixiang/controller/admin/AdminOperationLogController.java)

## 面试可讲的点

你可以这样介绍这部分：

- 为了让后台管理操作可追踪，我给 `/admin/**` 写接口补了一层统一审计日志
- 成功的写操作会自动记录操作人、模块、动作、业务 ID 和来源 IP
- 这样后续排查“谁改了哪个商品/商户”会更方便
- 同时提供了日志分页查询接口，支持直接在后台联调和演示
