# 秒杀压测说明

## 目标

这份说明用于验证当前秒杀链路是否具备第一版抗高并发能力，重点观察下面 4 件事：

1. 秒杀提交接口在高并发下是否还能快速返回。
2. Redis 预扣库存是否能挡住超卖。
3. RabbitMQ 异步建单后，最终结果是否能正确收敛为成功或失败。
4. 提交失败码是否符合当前统一规则。

## 前置条件

开始前请先自己确认下面服务都已经启动：

- 后端服务
- MySQL
- Redis
- RabbitMQ

建议先确认一个用于秒杀测试的商品已经配置好：

- 商品已上架
- `merchantId` 和 `productId` 已知
- 秒杀开始时间已到或即将开始
- 数据库库存明确，例如 `10`

## 推荐压测流程

1. 管理端重置该商品的秒杀状态：
   - `POST /admin/product/{id}/seckill/reset`
2. 管理端预热该商品库存：
   - `POST /admin/product/{id}/seckill/preheat`
3. 准备一批已注册的测试用户账号。
4. 使用 `scripts/seckill-pressure-test.ps1` 并发提交秒杀请求。
5. 查看脚本输出的汇总结果。
6. 必要时再调用管理端状态接口排查：
   - `GET /admin/product/{id}/seckill/status?userId=xxx`

## 测试账号文件

脚本从 CSV 文件读取测试账号，示例见 [seckill-users.sample.csv](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/scripts/seckill-users.sample.csv)。

格式要求：

```csv
phone,password
13800000001,123456
13800000002,123456
```

这些账号必须已经在系统里注册过。

## 脚本用途

[seckill-pressure-test.ps1](file:///d:/学习/自己的项目/zlx/后端/HuiXiangLife/scripts/seckill-pressure-test.ps1) 会按下面顺序执行：

1. 读取测试账号 CSV。
2. 逐个调用 `/user/auth/login` 登录，拿到 JWT。
3. 以并发方式调用 `/user/order/seckill`。
4. 可选轮询 `/user/order/seckill/result`，直到拿到终态。
5. 汇总登录结果、提交结果、最终秒杀结果，并输出 JSON 报告。

## 使用示例

### 只测提交接口

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\seckill-pressure-test.ps1 `
  -BaseUrl "http://localhost:8080" `
  -MerchantId 1 `
  -ProductId 1001 `
  -UsersFile ".\scripts\seckill-users.csv"
```

### 提交后继续轮询最终结果

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\seckill-pressure-test.ps1 `
  -BaseUrl "http://localhost:8080" `
  -MerchantId 1 `
  -ProductId 1001 `
  -UsersFile ".\scripts\seckill-users.csv" `
  -PollResult `
  -MaxWaitSeconds 40 `
  -OutputPath ".\scripts\reports\seckill-report.json"
```

## 结果怎么看

### 提交接口

当前项目的提交接口顶层 `Result.code` 可以重点关注：

- `1`：提交成功，表示已进入排队或异步建单流程
- `101`：重复抢购
- `102`：商品已抢完
- `103`：秒杀活动未开始
- `104`：秒杀活动已结束
- `199`：系统繁忙或预扣异常

### 轮询结果

`/user/order/seckill/result` 的 `data.status` 可以重点关注：

- `SUCCESS`：最终建单成功
- `FAILED`：最终失败
- `PENDING`：仍在异步处理中
- `EMPTY`：无记录，通常表示未成功进入链路或查询过早

## 推荐观测项

压测时建议你同时观察：

- 后端日志里是否有大量异常堆栈
- Redis 秒杀库存是否按预期递减
- RabbitMQ 队列是否出现明显积压
- 数据库订单数是否不超过库存数
- 管理端秒杀状态接口返回的 Redis 状态是否正常

## 合理预期

如果当前链路工作正常，一般应该满足：

- 不会出现数据库超卖
- 提交接口大部分请求会快速返回
- 成功订单数不会超过库存数
- 超过库存后的请求会逐步转为 `102`
- 重复用户再次请求会命中 `101`

## 面试可讲的点

你可以基于这套压测结果这样讲：

- 秒杀入口先走 Redis + Lua 预扣，减少数据库直接承压
- 建单通过 RabbitMQ 异步化，削峰填谷
- 数据库最终扣减采用原子 SQL，避免超卖
- 接口和异步结果都定义了统一失败码，方便前端提示和压测统计
- 通过预热、重置、状态查询和压测脚本，秒杀链路具备可验证和可演示能力
