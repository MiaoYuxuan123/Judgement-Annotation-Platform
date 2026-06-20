# 裁判文书标注系统（Judgment Annotation Platform）

面向法律文本论证结构挖掘的可视化协同标注工具。系统支持文书入库、指南版本管理、任务创建、命题标注、关系标注、逻辑图示生成、多人独立标注、人工裁定和结果导出。

当前仓库实现的是一个可运行的课程项目：后端 Spring Boot + MyBatis + MySQL，前端 Vue 3 构建完整演示流程。

## 功能概览

- 超级管理员后台：文书总库、配置中心、用户管理。
- 任务创建者工作台：创建任务、选择文书、指定标注者和裁决者。
- 参与者任务列表：普通用户只看到自己被分配的任务；顶栏消息中心（非 admin）。
- 标注工作台：原文选取、标签浮层、命题列表、关系构建器、手动生成逻辑图示。
- 论证图编辑器：全屏 Vue Flow 编辑布局（`/annotate/:taskId/:dataId/graph`）。
- 裁定界面：查看不同标注员结果、全部采纳或进入部分修改。
- 结果查看与导出：浏览器端 ZIP 打包（命题/关系 CSV + 论证图 PNG）。

## 技术栈

### 前端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

### 后端

- Java 17
- Spring Boot 3
- Spring Web
- Spring Validation
- MyBatis 3
- MySQL 8
- JWT（`JwtService` + 请求拦截器）

## 目录结构

```text
.
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
├── frontend/                # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
├── docs/                    # 项目阶段文档、需求、架构、API、数据库设计
├── README.md
└── .gitignore
```

## 环境要求

- Java 17+
- Maven 3.8+
- Node.js 18+（当前开发环境使用 Node 24）
- npm 9+
- MySQL 8.x

检查版本：

```bash
java -version
mvn -version
node -v
npm -v
```

## 快速启动

建议先启动 MySQL，配置数据库环境变量，再启动后端与前端。

### 0. 配置 MySQL 环境变量

Windows PowerShell 示例：

```powershell
$env:DB_COMMON_USER = "你的MySQL用户名"
$env:DB_COMMON_PWD  = "你的MySQL密码"
```

默认连接 `jdbc:mysql://localhost:3306/jap`；库不存在时会自动创建。首次启动由 `DatabaseInitializer` 建表并导入演示数据。详见 `backend/README.md`。

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

也可以先打包再运行：

```bash
cd backend
mvn -DskipTests package
java -jar target/judgment-annotation-platform-0.0.1-SNAPSHOT.jar
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置代理，前端请求 `/api` 会转发到 `http://localhost:8080`。

## 演示账号

所有账号密码均为：

```text
123456
```

| 账号 | 身份说明 |
| --- | --- |
| `admin` | 超级管理员 |
| `creator` | 任务创建者 |
| `annotator1` | 普通用户，可在演示任务中作为标注者 |
| `annotator2` | 普通用户，可在演示任务中作为标注者 |
| `reviewer` | 普通用户，可在演示任务中作为裁决者 |

说明：系统不把“标注者 / 裁决者”作为全局角色。普通用户在某个任务中是否能标注或裁决，由任务创建者分配决定。

## 角色与权限

### 超级管理员

登录 `admin` 后只显示后台管理功能：

- 文书总库
- 配置中心
- 用户管理

超级管理员负责维护基础资源，不直接创建标注任务。

### 任务创建者

登录 `creator` 后进入任务管理视图，可：

- 创建任务
- 选择文书
- 选择指南版本
- 指定标注者
- 指定裁决者
- 查看任务详情和结果

任务创建者不应同时作为该任务的标注者或裁决者。

### 普通参与者

登录 `annotator1`、`annotator2`、`reviewer` 后只看到自己参与的任务。

- 被分配为标注者：进入数据选择页和标注工作台（含论证图编辑器）。
- 被分配为裁决者：进入裁定界面。
- 标注员、创建者、裁定者可在顶栏使用消息中心（admin 不展示）。
- 标注者可查看结果；导出以创建者和裁定者为主。

## 标注工作台说明

标注工作台是系统核心页面，分为四个主要区域：

- 左侧：命题列表、关系列表。
- 中上：原文展示区。
- 中下：关系生成区。
- 右侧：逻辑图示区。

主要交互：

1. 在原文中选中文本，系统弹出标签选择浮层。
2. 选择一级标签；当一级标签为 `GM` 时，可继续选择二级标签。
3. 确认后生成命题，命题编号为 `P1`、`P2`、`P3`。
4. 在关系生成区选择关系类型和成员。
5. 关系编号为 `R1`、`R2`、`R3`。
6. `J` 组合关系和 `I` 同一关系支持多个成员，可点击 `＋` 增加成员槽位。
7. `S` 支持、`A` 反对、`M` 匹配默认只支持两个成员。
8. 命题或关系变更后，右侧图示不会立即刷新。
9. 点击关系生成区右上侧的「生成图示」按钮后，右侧逻辑图示才会生成或刷新。
10. 可通过「绘制图示 / 编辑图示」进入全屏论证图编辑器保存布局。

列表操作：

- 命题列表和关系列表每一项在鼠标悬停时显示“修改 / 删除”。
- 已标注文本会在原文中高亮，避免重复标注。
- 与已有命题范围重叠的文本不能再次加入命题。

## 标签与关系体系

一级标签：

- `SF`：个别事实
- `GF`：一般事实
- `SM`：个别规范
- `GM`：一般规范

`GM` 二级标签：

- `GM-L`：法律
- `GM-I`：解释
- `GM-C`：合同
- `GM-U`：习惯
- `GM-M`：道德
- `GM-O`：其他规范

关系类型：

- `S`：支持
- `A`：反对
- `J`：组合
- `M`：匹配
- `I`：同一

## 常用页面

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 登录 | `/login` | 输入账号密码登录 |
| 工作台 | `/dashboard` | 任务概览 |
| 文书总库 | `/documents` | 超级管理员维护文书 |
| 配置中心 | `/configs` | 超级管理员维护指南版本 |
| 用户管理 | `/users` | 超级管理员维护账号 |
| 任务管理 | `/tasks` | 创建者或参与者任务列表 |
| 任务详情 | `/tasks/:id` | 查看任务配置和数据 |
| 数据选择 | `/tasks/:id/data` | 参与者选择待处理文书 |
| 标注工作台 | `/annotate/:taskId/:dataId` | 标注者进行命题和关系标注 |
| 论证图编辑器 | `/annotate/:taskId/:dataId/graph` | 全屏编辑论证图布局 |
| 裁定界面 | `/review/:taskId` | 裁决者查看并采纳标注 |
| 结果查看 | `/results/:taskId` | 查看最终结果和 ZIP 导出 |

## 后端 API 概览

基础路径：

```text
/api
```

主要接口：

- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users`
- `GET /api/documents`
- `POST /api/documents`
- `DELETE /api/documents/{id}`
- `GET /api/configs/versions`
- `GET /api/tasks`
- `GET /api/tasks/my`
- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `GET /api/tasks/{taskId}/items`
- `GET /api/tasks/{taskId}/items/{dataId}`
- `POST /api/annotations/submit`
- `POST /api/annotations/layout`
- `GET /api/reviews/{taskId}`
- `POST /api/reviews/adopt`
- `POST /api/reviews/manual`
- `GET /api/messages`
- `GET /api/messages/unread-count`

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 构建与验证

后端测试：

```bash
cd backend
mvn test
```

后端测试 + 覆盖率校验：

```bash
cd backend
mvn verify
```

JaCoCo 报告位置：

```text
backend/target/site/jacoco/index.html
```

前端测试：

```bash
cd frontend
npm run test
```

前端测试 + 覆盖率报告：

```bash
cd frontend
npm run test:coverage
```

Vitest 覆盖率报告位置：

```text
frontend/coverage/index.html
```

后端构建：

```bash
cd backend
mvn -DskipTests package
```

前端构建：

```bash
cd frontend
npm run build
```

说明：前端构建可能出现 Vite chunk size warning，这是 Element Plus 等依赖打包后的体积提示，不影响运行。

## 常见问题

### 1. 前端请求接口失败

确认后端已经启动在 `http://localhost:8080`，并且前端通过 `npm run dev` 启动。

### 2. 后端启动报数据库连接错误

确认 MySQL 已启动，并已设置 `DB_COMMON_USER`、`DB_COMMON_PWD` 环境变量。详见 `backend/README.md`。

### 3. 后端打包失败，提示 jar 无法重命名

通常是之前启动的后端 jar 仍在运行，Windows 占用了目标文件。停止该 Java 进程后重新执行：

```bash
cd backend
mvn -DskipTests package
```

### 4. 演示数据被改乱或需要重置

删除 `jap` 数据库后重启后端，`DatabaseInitializer` 会重新建表并导入种子数据。详见 `docs/P4/演示说明.md` 第十四节。

## 项目状态说明

当前版本重点服务课程展示和流程验证：

- 已实现完整前后端演示闭环（MySQL 持久化）。
- 文书上传支持 PDF/Word/TXT 等解析入库。
- 结果导出由**前端浏览器**打包 ZIP（CSV + PNG），非后端文件流下载。
- 认证使用 JWT（`Authorization: Bearer`）。
- 完整演示路径与预置数据见 `docs/P4/演示说明.md`。
