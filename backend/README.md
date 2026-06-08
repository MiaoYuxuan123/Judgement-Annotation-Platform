# 裁判文书标注平台后端

## 技术栈

- Java 17
- Spring Boot 3.3
- MyBatis 3
- MySQL 8

## 启动方式

确认 MySQL 服务已启动，并且系统环境变量里已有：

```powershell
DB_COMMON_USER=你的MySQL用户名
DB_COMMON_PWD=你的MySQL密码
```

然后直接运行：

```bash
mvn spring-boot:run
```

后端会自动连接：

```text
jdbc:mysql://localhost:3306/jap
```

如果 `jap` 数据库不存在，JDBC 会自动创建；随后 Spring Boot 会自动执行：

- `src/main/resources/db/admin.sql`
- `src/main/resources/db/creator.sql`
- `src/main/resources/db/annotator.sql`
- `src/main/resources/db/reviewer.sql`
- `src/main/resources/db/data.sql`

因此不需要手动建库、手动建表或手动导入演示数据。

## 自定义数据库

如需换库名或端口，设置环境变量：

```powershell
DB_URL=jdbc:mysql://localhost:3306/jap?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

保留 `createDatabaseIfNotExist=true`，否则首次启动前仍需手动创建数据库。

## MyBatis 配置

配置入口在：

```text
src/main/resources/application.yml
```

关键配置：

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: edu.nju.jap.model.po
  configuration:
    map-underscore-to-camel-case: true
```

Mapper 接口位于：

```text
src/main/java/edu/nju/jap/mapper
```

SQL XML 位于：

```text
src/main/resources/mapper
```

启动类通过 `@MapperScan("edu.nju.jap.mapper")` 扫描 Mapper。

## 演示账号

密码均为 `123456`：

- `admin`
- `creator`
- `annotator1`
- `annotator2`
- `reviewer`

## 注意

当前 `spring.sql.init.mode=always`，并且四个建表脚本会重建表，适合课程开发阶段快速恢复演示数据。若后续要保留真实标注结果，请改为：

```yaml
spring:
  sql:
    init:
      mode: never
```

## 后端打包

```bash
mvn clean package -DskipTests
```