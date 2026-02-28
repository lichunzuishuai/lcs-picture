<p align="center">
  <img src="./yu-picture-frontend/src/assets/logo.png" width="120" alt="LCS-Picture Logo">
</p>

<h1 align="center">LCS-Picture 智能图片管理平台</h1>

<p align="center">
  <b>企业级架构的智能图片管理、检索引擎与云端图库服务</b>
</p>

<p align="center">
  <a href="#-项目简介">项目简介</a> •
  <a href="#-系统特性">系统特性</a> •
  <a href="#-技术架构">技术架构</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-核心功能接口">核心API接口</a>
</p>

---

## 📖 项目简介

**LCS-Picture** 是一个功能强大的全栈智能图片管理系统，旨在提供安全、高效、智能的云端图片存储与处理服务。系统采用微服务演进的前后端分离架构，后端基于 Spring Boot 2.7 强力驱动，无缝集成腾讯云对象存储（COS）；前端采用 Vue 3 生态，为用户呈现现代化、响应式的操作界面。

无论是个人相册管理，还是团队素材协作，LCS-Picture 都能提供稳定可靠的解决方案。

## ✨ 系统特性

### 🖼️ 极客体验的图片管理
- **多样化上传**：不仅支持拖拽及常规的本地文件上传，更支持基于 URL 链接的云端极速拉取上传。
- **批量处理中心**：强大的批量上传与信息编辑能力，应对海量图片毫无压力。
- **智能元数据提取**：上传即自动获取图片分辨率、格式、主色调、文件大小等EXIF/元数据信息。
- **灵活标签分类**：支持深度分类体系与自定义多维标签管理，让图片组织井井有条。
- **动态缩略图引擎**：自动生成适配不同网络环境的缩略图，大幅优化前端加载性能。

### 🔍 AI 赋能的机器视觉检索
- **以图搜图技术**：深度集成业界领先的图像搜索引擎（百度/360接口），精准实现同源或相似图片溯源。
- **RGB主色调检索**：创新的颜色搜索引擎，支持根据图片核心色块进行相似氛围图片的查找。
- **多维复合筛选**：支持按名称片段、系统分类、自定义标签、图片尺寸等多维度组合的高阶聚合检索。
- **双重缓存架构**：Redis 分布式缓存 + Caffeine 本地一级缓存，保障千万级数据下的检索毫秒级响应。

### 📁 灵活弹性的空间控制
- **独立死岛空间**：为每位用户划分严格隔离的私密个人图片空间。
- **会员分级体系**：精细的空间级别控制（普通版 / 专业版 / 旗舰版），实现不同级别的容量上限限制。
- **动态配额管控**：严格监管空间的最大图片并发数与总存储流量策略。

### 👥 严密安全的用户机制
- **细粒度角色控制**：标准的 RBAC (Role-Based Access Control) 权限模型，区分普通用户与超级管理员权限。
- **无状态分布式会话**：基于 Spring Session 结合 Redis，实现高可用的分布式 Session 共享。
- **全局拦截与脱敏**：敏感接口统一校验，出参数据实时脱敏，保障系统安全。

### ✅ 规范化的内容审核
- **图片多维度审核**：完备的审核流转体系（待审核 / 审核通过 / 违规拒绝）。
- **审核操作追溯**：详细记录每一次审核动作的操作人员与时间节点，确保内容溯源合规。

---

## 🛠️ 技术架构体系

### 📦 后端核心架构
| 技术生态区 | 核心框架/组件 | 版本/说明 |
|:---:|:---|:---|
| **核心基准** | Spring Boot | 2.7.6 长期支持版 |
| **持久层** | MySQL + MyBatis-Plus | 8.0+ / 3.5.9（单表千万级处理） |
| **分布式引擎**| Redis | 6.0+（会话管理/分布式锁/高速缓存） |
| **应用层缓存**| Caffeine | 高性能本地一级缓存 |
| **云原生基建**| 腾讯云 COS | 亿级对象存储服务引擎 |
| **API契约** | Knife4j (Swagger) | 自动化接口调试与文档生成 |
| **基础工具箱**| Hutool / Jsoup / Lombok | 极致提升研发效率 |

### 🎨 前端展现架构
（基于 Vue 3 生态栈构建）
- **核心框架**：Vue 3
- **构建工具**：Vite (极速热更体验)
- **状态管理**：Pinia
- **路由控制**：Vue Router 4
- **UI 框架**：Ant Design Vue / Element Plus
- **语言扩展**：TypeScript (强类型保障)

---

## 📁 核心工程目录

```text
lcs-picture 全栈工程
├── src/main/java/com/lcs/lcspicture/    [后端核心源码区]
│   ├── annotation/        # AOP自定义注解拦截处理器层（细化至方法级权限验证）
│   ├── aop/               # 切面拦截织入层（鉴权拦截/日志捕获）
│   ├── api/               # 外部开放API深度集成网关（如：百度图片逆向搜索引擎）
│   ├── common/            # 系统全局公共基建（全局响应体、分页载体、基础异常类）
│   ├── config/            # Bean与应用参数配置注入中心（Redis/COS/Web等参数配置）
│   ├── constant/          # 静态系统级常量池控制区
│   ├── controller/        # Restful API 前置路由分发控制器层
│   │   ├── UserController     # 用户生命周期控制器
│   │   ├── PictureController  # 媒体业务生命周期控制器
│   │   ├── SpaceController    # 虚拟空间生命周期控制器
│   │   └── FileController     # 物理文件分发控制器
│   ├── exception/         # 统一全局异常调度与处理中心
│   ├── manager/           # 第三方基石业务调度中心（Tencent COS 引擎交互、分布式逻辑集成）
│   ├── mapper/            # 数据持久层 MyBatis 映射接口层
│   ├── model/             # 领域驱动实体传输模型层
│   │   ├── dto/           # Domain to Object 请求参数装载箱
│   │   ├── entity/        # 面向数据库表的原生实体映射集
│   │   ├── enums/         # 严格内聚的数据状态枚举域
│   │   └── vo/            # View Object 干净视图展示层对象（经过格式化及脱敏）
│   ├── service/           # 核心领域业务逻辑聚合层（CRUD封装/复杂业务流编排）
│   └── utils/             # 无状态系统操作辅佐工具库
├── src/main/resources/    [项目资源库区]
│   ├── application.yml    # 多环境核心配置编排文件
│   └── mapper/            # MyBatis XML SQL 隔离映射集聚地
└── yu-picture-frontend/   [前端全栈工程区 (Vue3 + TS)]
    ├── src/
    │   ├── api/           # OpenAPI规范化请求封装接口池
    │   ├── components/    # 细粒度、高复用的业务组件构建单元
    │   ├── layouts/       # 基础布局/母版页骨架层
    │   ├── pages/         # 一级页面业务集合区
    │   ├── router/        # 前端路由守卫与网关分发配置
    │   ├── stores/        # Pinia 前端状态分布式管理机
    │   └── utils/         # 前端无状态基础工具集合
```

---

## 🚀 极速部署指南

### 1️⃣ 环境预备要求
确保服务器或本地已安转如下基建设施：
- **运行环境**：JDK 17 或以上版本、Node.js 16+（针对前端）
- **持久层支持**：MySQL 8.0 或以上版本
- **缓存支持**：Redis 6.0 或以上版本
- **构建工具**：Maven 3.6+、npm/yarn/pnpm

### 2️⃣ 基础数据初始化
```sql
# 建立基础字符集编码隔离方案
CREATE DATABASE lcs_picture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
# 执行脚本（请根据项目下 db_schema 补充执行初始化语句）
```

### 3️⃣ 应用配置注入
修改核心配置文件：`src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lcs_picture?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_db_username
    password: your_db_password
  redis:
    host: 127.0.0.1
    port: 6379

cos:
  client:
    # 替换至您的腾讯COS控制台真实凭据参数
    host: https://your-cos-host.myqcloud.com
    secretId: your-secret-id
    secretKey: your-secret-key
    region: ap-guangzhou
    bucket: your-bucket-1234567890
```

### 4️⃣ 多端启动
#### 🛠 后端中心启动
```bash
# 进入后端工程根目录使用 Maven 构建并拉起
mvn clean install -DskipTests
mvn spring-boot:run
```

#### 💻 前端视图启动
```bash
# 导航到前端子项目中并拉取 node 依赖，启动本地Vite服务
cd yu-picture-frontend
npm install
npm run dev
```

### 5️⃣ 沙盒体验入口
- **后端 Swagger 文档调度台**: `http://localhost:8123/api/doc.html`
- **前端可视化操控管理界面**: 默认位于 `http://localhost:5173`

---

## 📡 核心 API 路由寻址池

### 🛡 用户凭证网关 (`/api/user`)
| 动作 | URI片段 | 核心职能 |
|:---:|:---:|:---|
| `POST` | `/register` | 引导注册流程（闭环加密） |
| `POST` | `/login` | 颁发授权与分布式会话认证 |
| `POST` | `/logout` | 销毁与回收会话 Token |
| `GET` | `/get/login` | 拉取当前授权主体上下文数据 |

### 🏞 图片调度网关 (`/api/picture`)
| 动作 | URI片段 | 核心职能 |
|:---:|:---:|:---|
| `POST` | `/upload` | 基于I/O流传输物理图像实体 |
| `POST` | `/upload/url` | 基于URL由服务器异步嗅探拉取 |
| `POST` | `/upload/batch` | 大规模图片离线异步装箱上云 |
| `POST` | `/list/page/vo` | 获取分页流图集（内置VO格式规范化）|
| `GET` | `/get/vo` | 精确命中单例媒体对象参数组 |
| `POST` | `/search/color` | 使用特定 RGB 代码域执行高频检索引擎 |

### 🧊 物理空间网关 (`/api/space`)
| 动作 | URI片段 | 核心职能 |
|:---:|:---:|:---|
| `POST` | `/add` | 在数据库层分配用户专属网格空间 |
| `GET` | `/get/vo` | 获取虚拟空间当前流量与容量负载状态 |
| `GET` | `/level/list` | 读取系统已注册的所有弹性容量级别策略清单 |

---

## 📝 二次开发工程规范
- **垂直越权控制**：严格遵循AOP代理，使用 `@AutoCheck(mustRole = "admin")` 行使高危方法的物理隔绝。
- **隐私脱敏规范**：实体对象绝对禁止直接向外抛出（严禁表现层使用Entity），所有展示动作均必须经过 `VO (View Object)` 层清洗过滤。
- **强制逻辑删除**：针对所有领域核心表模型，采用 `isDelete = 1` 物理标识屏蔽，实现数据冷隔离。
- **无状态分布式基建**：Session等任何可能引发单点故障的状态均已通过 `Spring Session + Redis` 实现完全解耦化。

---

## 🤝 参与贡献
欢迎提交 [Issues](https://github.com/lichunzuishuai/lcs-picture/issues) 或 Pull Requests ，与我们一同将 **LCS-Picture** 演进成为企业级开源基建中的明星项目。

## 📄 开源许可证 (License)
基于 **MIT License** 发行。
本项目可免费用于学习交流、产品孵化及商业化衍生构建。如需引用，请保留原作者归属声明。

---
<p align="center">
  <i>Written with ❤️ by <a href="https://github.com/lichunzuishuai">李春深</a></i>
</p>
