# LCS-Picture 智能图片管理平台

<p align="center">
  <b>一个基于 Spring Boot 的智能图片管理与检索系统</b>
</p>

---

## 📖 项目简介

**LCS-Picture** 是一个功能完善的智能图片管理平台，支持图片上传、存储、检索、审核等核心功能。系统采用前后端分离架构，后端基于 Spring Boot 2.7 构建，集成腾讯云对象存储（COS），提供高效、安全的图片管理解决方案。

## ✨ 核心功能

### 🖼️ 图片管理
- **多方式上传**：支持本地文件上传和 URL 链接上传
- **批量操作**：支持批量上传、批量编辑图片信息
- **图片信息**：自动提取图片宽高、格式、颜色、大小等元数据
- **分类标签**：支持图片分类和多标签管理
- **缩略图**：自动生成缩略图，优化加载速度

### 🔍 智能检索
- **以图搜图**：集成百度/360图片搜索 API，支持相似图片查找
- **颜色搜索**：根据图片主色调进行相似颜色检索
- **多条件查询**：支持按名称、分类、标签、尺寸等多维度检索
- **缓存优化**：集成 Redis 缓存 + Caffeine 本地缓存，提升查询性能

### 📁 空间管理
- **个人空间**：用户独立的私有图片空间
- **空间级别**：支持普通版/专业版/旗舰版多级空间容量
- **配额管理**：限制空间最大图片数量和总大小

### 👥 用户系统
- **用户认证**：完整的注册、登录、登出功能
- **会话管理**：基于 Spring Session + Redis 的分布式会话
- **权限控制**：区分普通用户和管理员角色

### ✅ 审核机制
- **图片审核**：支持待审核、通过、拒绝三种审核状态
- **审核记录**：记录审核人和审核时间

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **框架** | Spring Boot 2.7.6 |
| **数据库** | MySQL + MyBatis-Plus 3.5.9 |
| **缓存** | Redis + Caffeine |
| **对象存储** | 腾讯云 COS |
| **接口文档** | Knife4j (Swagger) |
| **工具库** | Hutool、Jsoup、Lombok |

## 📁 项目结构

```
lcs-picture/
├── src/main/java/com/lcs/lcspicture/
│   ├── annotation/        # 自定义注解（权限校验等）
│   ├── aop/               # 切面（权限拦截）
│   ├── api/               # 外部 API 集成
│   │   └── imagesearch/   # 图片搜索（百度/360）
│   ├── common/            # 通用类（响应封装、分页等）
│   ├── config/            # 配置类
│   ├── constant/          # 常量定义
│   ├── controller/        # 控制器
│   │   ├── UserController     # 用户接口
│   │   ├── PictureController  # 图片接口
│   │   ├── SpaceController    # 空间接口
│   │   └── FileController     # 文件上传接口
│   ├── exception/         # 异常处理
│   ├── manager/           # 业务管理器（COS、文件上传等）
│   ├── mapper/            # MyBatis Mapper
│   ├── model/             # 数据模型
│   │   ├── dto/           # 请求参数对象
│   │   ├── entity/        # 数据库实体
│   │   ├── enums/         # 枚举类
│   │   └── vo/            # 响应视图对象
│   ├── service/           # 业务逻辑层
│   └── utils/             # 工具类
└── src/main/resources/
    ├── application.yml    # 应用配置
    └── mapper/            # MyBatis XML 映射文件
```

## 🚀 快速开始

### 环境要求

- **JDK** 17+
- **MySQL** 8.0+
- **Redis** 6.0+
- **Maven** 3.6+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/lcs-picture.git
   cd lcs-picture
   ```

2. **创建数据库**
   ```sql
   CREATE DATABASE lcs_picture DEFAULT CHARACTER SET utf8mb4;
   ```

3. **修改配置**
   
   编辑 `src/main/resources/application.yml`，配置以下信息：
   ```yaml
   # 数据库配置
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/lcs_picture
       username: your_username
       password: your_password
   
   # Redis 配置
     redis:
       host: localhost
       port: 6379
   
   # 腾讯云 COS 配置
   cos:
     client:
       host: your-cos-host
       secretId: your-secret-id
       secretKey: your-secret-key
       region: your-region
       bucket: your-bucket
   ```

4. **启动项目**
   ```bash
   mvn spring-boot:run
   ```

5. **访问接口文档**
   
   打开浏览器访问：`http://localhost:8123/api/doc.html`

## 📡 API 接口概览

### 用户接口 `/user`

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/register` | 用户注册 |
| POST | `/login` | 用户登录 |
| POST | `/logout` | 用户登出 |
| GET | `/get/login` | 获取当前登录用户 |

### 图片接口 `/picture`

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/upload` | 文件上传图片 |
| POST | `/upload/url` | URL上传图片 |
| POST | `/upload/batch` | 批量上传图片 |
| POST | `/delete` | 删除图片 |
| POST | `/edit` | 编辑图片信息 |
| POST | `/list/page/vo` | 分页查询图片 |
| GET | `/get/vo` | 获取图片详情 |
| POST | `/search/color` | 按颜色搜索 |
| GET | `/tag_category` | 获取标签分类 |

### 空间接口 `/space`

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/add` | 创建空间 |
| POST | `/delete` | 删除空间 |
| POST | `/edit` | 编辑空间 |
| GET | `/get/vo` | 获取空间详情 |
| GET | `/level/list` | 获取空间级别列表 |

## 📝 开发说明

- **权限控制**：使用 `@AutoCheck(mustRole = "admin")` 注解进行接口权限控制
- **数据脱敏**：敏感数据通过 VO 层进行脱敏处理
- **逻辑删除**：所有表使用 `isDelete` 字段实现逻辑删除
- **分布式会话**：通过 Spring Session + Redis 实现多实例会话共享

## 📄 License

本项目仅供学习交流使用。
