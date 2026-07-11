# 惠享生活 HuiXiangLife

惠享生活是一个面向本地生活场景的团购与到店消费平台，包含用户端、管理端和 Spring Boot 后端服务。项目覆盖商户与商品展示、优惠券、订单支付、退款、评价、搜索、秒杀、后台运营管理、操作日志等业务能力，重点体现交易链路、缓存、消息队列、搜索引擎和后台管理的完整工程实践。

## 功能概览

### 用户端

- 用户注册、登录、个人中心
- 首页聚合信息展示
- 商户列表、商户详情
- 商品列表、商品详情
- 商品与商户搜索
- 优惠券领取与使用
- 普通下单、秒杀下单、支付结果页
- 订单查看、评价、收藏等用户行为

### 管理端

- 管理员登录与权限控制
- 控制台数据概览
- 商户管理、商户分类管理
- 商品管理、上下架与搜索索引同步
- 优惠券模板管理
- 订单管理、退款处理
- 秒杀库存预热、重置与状态查询
- 用户管理、评价管理
- 管理端操作日志审计

### 后端核心能力

- JWT 双端鉴权：管理端与用户端独立登录态
- MyBatis-Plus 持久层与统一分页查询
- Redis 缓存、秒杀库存预扣和热点状态存储
- RabbitMQ 异步建单、消息削峰和通知处理
- Elasticsearch 商品与商户全文检索，异常时回退 MySQL 查询
- Spring Boot Actuator + Micrometer 暴露运行指标
- 统一响应结构、全局异常处理、业务错误码
- 管理端操作日志拦截记录
- 单元测试、控制器测试、集成测试与秒杀压测脚本

## 技术栈

### 前端

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- Element Plus
- Nginx

### 后端

- Java 17
- Spring Boot 3.3.12
- Spring MVC
- Spring Boot Actuator
- MyBatis-Plus 3.5.7
- Maven 多模块工程
- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- Prometheus
- Grafana
- Docker Compose

## 项目结构

```text
惠享生活
├── .github/workflows/ci.yml              # GitHub Actions CI/CD 配置
├── docker-compose.yml                    # 一键本地/演示环境
├── deploy
│   ├── mysql/init.sql                    # 数据库初始化脚本
│   ├── nginx/default.conf                # 前端容器 Nginx 配置
│   ├── prometheus/prometheus.yml         # Prometheus 采集配置
│   └── grafana                           # Grafana 数据源与仪表盘
├── README.md
├── 开发文档.md
├── 前端
│   ├── Dockerfile
│   ├── src
│   │   ├── api                           # 前端接口封装
│   │   ├── layouts                       # 用户端/管理端布局
│   │   ├── router                        # 路由与鉴权守卫
│   │   ├── stores                        # Pinia 状态管理
│   │   ├── types                         # TypeScript 类型定义
│   │   ├── utils                         # 通用工具
│   │   └── views                         # 页面视图
│   ├── package.json
│   ├── vite.config.ts
│   └── nginx.conf
└── 后端
    └── HuiXiangLife
        ├── Dockerfile
        ├── docs                          # 后端说明、接口与测试文档
        ├── scripts                       # 秒杀压测和测试用户脚本
        ├── huixiang-common               # 通用常量、异常、返回结构、JWT 工具
        ├── huixiang-pojo                 # Entity、DTO、Query、VO
        ├── huixiang-server               # Controller、Service、Mapper、配置、监听器
        └── pom.xml
```

## 一键启动

已提供 Docker Compose 环境，包含前端、后端、MySQL、Redis、RabbitMQ、Elasticsearch、Prometheus、Grafana。

### 环境要求

- Docker 24+
- Docker Compose v2+

### 启动

在项目根目录执行：

```powershell
docker compose up -d --build
```

首次启动时会自动执行数据库初始化脚本：

```text
deploy/mysql/init.sql
```

常用访问地址：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 前端入口 | `http://localhost` | 用户端与管理端统一入口 |
| 后端接口 | `http://localhost:8080` | Spring Boot 服务 |
| 管理端登录 | `http://localhost/login` | 管理后台 |
| 用户端首页 | `http://localhost/user` | 用户侧页面 |
| RabbitMQ 管理台 | `http://localhost:15672` | 账号 `huixiang`，密码 `huixiang123` |
| Elasticsearch | `http://localhost:9200` | 单节点开发配置 |
| Prometheus | `http://localhost:9090` | 指标采集 |
| Grafana | `http://localhost:3000` | 账号 `admin`，密码 `admin123` |

演示账号：

| 类型 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | `13800000000` | `123456` |
| 普通用户 | `13900000000` | `123456` |

停止环境：

```powershell
docker compose down
```

> `docker compose down` 会停止并移除容器，但不会删除数据卷。需要清空数据库、Redis 等持久化数据时，请手动确认后再处理 Docker volume。

## 本地开发环境

如果不使用 Docker Compose，建议准备以下环境：

- JDK 17+
- Maven 3.8+
- Node.js 20+ 与 npm
- MySQL 8+
- Redis 6+
- RabbitMQ 3+
- Elasticsearch 8+

后端默认开发配置位于：

```text
后端/HuiXiangLife/huixiang-server/src/main/resources/application-dev.yml
```

默认端口与连接信息：

| 服务 | 默认配置 |
| --- | --- |
| 后端服务 | `http://localhost:8080` |
| 前端开发服务 | `http://localhost:5173` |
| MySQL | `localhost:3306/huixiang_life` |
| Redis | `localhost:6379`，database `1` |
| RabbitMQ | `localhost:5672`，账号 `guest/guest` |
| Elasticsearch | `http://localhost:9200` |

手动初始化数据库时，可执行：

```text
deploy/mysql/init.sql
```

## 启动后端

进入后端 Maven 根目录：

```powershell
cd "后端/HuiXiangLife"
```

编译并运行测试：

```powershell
mvn clean test
```

启动服务：

```powershell
mvn -pl huixiang-server spring-boot:run
```

启动成功后，后端接口默认监听：

```text
http://localhost:8080
```

主要接口前缀：

- `/admin/**`：管理端接口
- `/user/**`：用户端接口
- `/notify/**`：支付或异步通知接口
- `/actuator/**`：健康检查与监控指标

## 启动前端

进入前端目录：

```powershell
cd "前端"
```

安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

常用页面入口：

- 管理端登录：`http://localhost:5173/login`
- 管理端控制台：`http://localhost:5173/dashboard`
- 用户端首页：`http://localhost:5173/user`
- 用户登录：`http://localhost:5173/user/login`
- 用户注册：`http://localhost:5173/user/register`

## 构建与部署

### 前端构建

```powershell
cd "前端"
npm run build
```

构建产物输出到：

```text
前端/dist
```

仓库提供了 Nginx 配置示例：

```text
前端/nginx.conf
deploy/nginx/default.conf
```

### 后端打包

```powershell
cd "后端/HuiXiangLife"
mvn clean package
```

服务入口模块为：

```text
huixiang-server
```

### Docker 镜像

后端镜像构建：

```powershell
docker build -t huixianglife/backend:local "后端/HuiXiangLife"
```

前端镜像构建：

```powershell
docker build -t huixianglife/frontend:local -f "前端/Dockerfile" .
```

## CI/CD

已提供 GitHub Actions 配置：

```text
.github/workflows/ci.yml
```

流水线包含：

- 后端 Maven 测试与打包
- 前端 npm 安装与构建
- 后端 Docker 镜像构建检查
- 前端 Docker 镜像构建检查
- 手动触发的部署模板占位任务

如果需要接入真实线上部署，可在 `deploy-template` 中补充镜像仓库登录、镜像推送、SSH 登录服务器、执行 `docker compose pull && docker compose up -d` 等步骤。

## 线上监控

后端已加入 Actuator 与 Prometheus 指标依赖，Docker 环境默认暴露：

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

Prometheus 配置：

```text
deploy/prometheus/prometheus.yml
```

Grafana 自动配置：

```text
deploy/grafana/provisioning/datasources/prometheus.yml
deploy/grafana/provisioning/dashboards/dashboards.yml
deploy/grafana/dashboards/huixianglife-overview.json
```

Docker Compose 启动后，访问 `http://localhost:3000`，使用 `admin/admin123` 登录，即可查看 `HuiXiangLife Overview` 仪表盘。

## 测试与压测

后端测试位于：

```text
后端/HuiXiangLife/huixiang-server/src/test
```

运行全部后端测试：

```powershell
cd "后端/HuiXiangLife"
mvn test
```

秒杀压测脚本位于：

```text
后端/HuiXiangLife/scripts
```

相关报告示例位于：

```text
后端/HuiXiangLife/scripts/reports
```

更多说明可查看：

- `后端/HuiXiangLife/docs/seckill-pressure-test.md`
- `后端/HuiXiangLife/docs/backend-test-and-interview.md`

## 接口与文档

项目文档入口：

- `开发文档.md`：项目整体设计与业务说明
- `后端/HuiXiangLife/docs/backend-development.md`：后端模块、核心能力和联调说明
- `后端/HuiXiangLife/docs/elasticsearch-search.md`：搜索能力说明
- `后端/HuiXiangLife/docs/admin-operation-log.md`：管理端操作日志说明
- `后端/HuiXiangLife/docs/apifox/huixianglife-openapi.json`：OpenAPI 接口描述文件

## 开发说明

- 本地开发默认使用 `dev` 环境配置。
- Docker Compose 默认使用 `docker` 环境配置。
- 修改数据库、Redis、RabbitMQ、Elasticsearch 地址时，请调整对应的 `application-*.yml` 或 Compose 环境变量。
- 前端接口通过 Axios 封装，管理端和用户端分别有独立请求实例。
- 后端采用多模块 Maven 结构，公共类型和工具应优先放入 `huixiang-common` 或 `huixiang-pojo`。
- 秒杀链路依赖 Redis 和 RabbitMQ，搜索链路依赖 Elasticsearch；如果中间件未启动，相关功能可能不可用或降级。

## 项目状态

当前项目已具备完整的前后端代码结构、数据库初始化脚本、一键 Docker Compose 环境、CI/CD 基础流水线和 Prometheus/Grafana 监控配置，适合作为本地生活平台、Java 后端实习项目或全栈练习项目继续扩展。
