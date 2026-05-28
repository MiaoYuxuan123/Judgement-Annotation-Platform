# 裁判文书标注平台后端

## 技术栈

- Java 17
- Spring Boot 3.3
- MyBatis 3
- MySQL 8（库名：`judgment_annotation`）

## 一、数据库初始化（首次必做）

`data.sql` 已包含原内存版 `DemoDataStore` 中的**演示标注数据**（任务 1001/1002/1003 的命题、关系、裁定样例）。

### 1. 确认 MySQL 已启动

Windows 可在服务里查看 `MySQL80` 是否运行，或命令行：

```bash
mysql -u root -p
```

### 2. 在 DataGrip / Navicat / 命令行执行脚本

按顺序执行项目内 SQL（路径：`src/main/resources/db/`）：

1. `schema.sql` — 建库建表  
2. `data.sql` — 演示用户、文书、任务及标注/裁定种子数据  

若库已建好但**没有标注内容**，单独执行：`db/data-demo-annotations.sql`

命令行示例：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
mysql -u root -p < src/main/resources/db/data.sql
```

### 3. DataGrip 连接配置

| 项 | 值 |
|---|---|
| Host | `localhost` |
| Port | `3306` |
| Database | `judgment_annotation` |
| User | `root` |
| Password | 你的 MySQL 密码 |

连接后左侧应能看到 `sys_user`、`task`、`proposition` 等表。

## 二、修改后端数据库密码（重要）

报错 `Access denied for user 'root'@'localhost'` 表示 **Java 里填的密码和 MySQL 实际密码不一致**。

请编辑（已生成）：

`src/main/resources/application-local.yml`

```yaml
spring:
  datasource:
    password: "你在DataGrip里能连上的那个密码"
```

注意：

1. 密码必须与 **DataGrip 数据源测试连接成功** 时使用的完全一致  
2. 若 DataGrip 用的是别的用户（不是 root），同步改 `application.yml` 里的 `username`  
3. 改完后 **必须重启** 后端（`mvn spring-boot:run`）

## 三、启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

## 四、演示账号

密码均为 `123456`：

- `admin` / `creator` / `annotator1` / `annotator2` / `reviewer`

## 五、项目结构（MyBatis）

```
controller  →  service  →  mapper  →  MySQL
                ↓
         model/entity、dto、po
```

- `mapper/` + `resources/mapper/*.xml`：SQL 与接口  
- `model/po/`：与表字段一一对应  
- `model/entity/`、`model/dto/`：业务与 API 层  
- 已移除内存仓库 `DemoDataStore`

## 六、报错 Unknown column 'task_document_id'

说明数据库表还是**最初那份 DDL**，缺少后端需要的字段。在 DataGrip 中执行：

`src/main/resources/db/patch-missing-columns.sql`

执行后**重启后端**，再打开任务/标注等页面。

若提示 `Duplicate column name`，表示该列已有，忽略该条继续即可。

## 七、相对原建表脚本的补充

为兼容现有前端接口，在 `schema.sql` 中额外增加了：

- `proposition.task_document_id` — 支持一个任务多篇文书  
- `relation` 上的 `task_id` / `task_document_id` / `user_id`  
- `auth_token` — 登录 Token  
- `arbitration_snapshot` — 裁定元数据  
- `guide_version.is_active` — 当前启用指南

若你本地已执行过旧版 DDL，请对照 `schema.sql` 补字段或删库重建。
