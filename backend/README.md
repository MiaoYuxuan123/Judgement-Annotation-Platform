裁判文书标注平台后端 MVP

## 技术栈

- Java 17
- Spring Boot 3
- 内存数据仓库，不依赖 MySQL

## 启动

```bash
mvn spring-boot:run
```

或先打包：

```bash
mvn -DskipTests package
java -jar target/judgment-annotation-platform-0.0.1-SNAPSHOT.jar
```

默认端口：`http://localhost:8080`

## 演示账号

密码均为 `123456`：

- `admin`
- `creator`
- `annotator1`
- `annotator2`
- `reviewer`
