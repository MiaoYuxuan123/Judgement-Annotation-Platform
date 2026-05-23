# 法律文书标注平台 API 规范文档

------

# 1. 文档说明

本文档基于《法律文书标注平台》系统需求与界面设计进行 RESTful API 设计，用于规范前后端的数据交互方式。

系统主要包含以下模块：

- 用户与权限管理
- 文书总库管理
- 标签配置中心
- 任务管理
- 标注工作台
- 裁定工作台
- 结果查看与导出

------

# 2. API 设计规范

## 2.1 基础路径

所有接口统一以：

```http
/api
```

作为基础路径。

示例：

```http
/api/auth/login
/api/tasks
/api/documents
```

------

## 2.2 数据格式

普通接口：

```http
Content-Type: application/json
```

文件上传接口：

```http
Content-Type: multipart/form-data
```

------

## 2.3 身份认证

除登录接口外，其余接口均需携带 Token：

```http
Authorization: Bearer {token}
```

------

## 2.4 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

------

## 2.5 统一错误码定义

| 错误码 | 含义                |
| ------ | ------------------- |
| 200    | 请求成功            |
| 400    | 请求参数错误        |
| 401    | 未登录或 Token 无效 |
| 403    | 权限不足            |
| 404    | 资源不存在          |
| 409    | 数据冲突            |
| 500    | 服务器内部错误      |

------

# 3. 用户与权限模块 API

对应页面：

- P3 用户管理

------

## 3.1 用户登录

### 接口说明

用户输入账号密码后登录系统。

登录成功后返回 JWT Token 与用户角色信息。

### URL

```http
POST /api/auth/login
```

### HTTP Method

```http
POST
```

### 请求参数

| 参数名   | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| username | string | 是   | 用户账号 |
| password | string | 是   | 用户密码 |

### 请求示例

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "jwt-token-string",
    "user": {
      "id": 1,
      "username": "admin",
      "role": "admin"
    }
  }
}
```

### 失败响应

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 400    | 参数为空   |
| 401    | 登录失败   |
| 500    | 服务器异常 |

------

## 3.2 获取当前用户信息

### 接口说明

获取当前登录用户的基本信息与角色权限。

### URL

```http
GET /api/users/me
```

### 请求头

```http
Authorization: Bearer {token}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "role": "admin"
  }
}
```

### 失败响应

```json
{
  "code": 401,
  "message": "Token 无效",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 401    | Token 无效 |
| 500    | 服务器异常 |

------

## 3.3 新增用户

### 接口说明

管理员新增系统用户。

对应页面：

- P3 用户管理

### URL

```http
POST /api/users
```

### HTTP Method

```http
POST
```

### 请求参数

| 参数名        | 类型    | 必填 | 说明                             |
| ------------- | ------- | ---- | -------------------------------- |
| username      | string  | 是   | 登录账号                         |
| realName      | string  | 是   | 用户姓名                         |
| password      | string  | 是   | 用户密码                         |
| role          | string  | 是   | admin/creator/annotator/reviewer |
| canCreateTask | boolean | 是   | 是否允许创建任务                 |

### 请求示例

```json
{
  "username": "user01",
  "realName": "张三",
  "password": "123456",
  "role": "annotator",
  "canCreateTask": false
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "用户创建成功",
  "data": {
    "userId": 12
  }
}
```

### 失败响应

```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明     |
| ------ | -------- |
| 400    | 参数缺失 |
| 403    | 权限不足 |
| 409    | 用户重复 |
| 500    | 创建失败 |

------

## 3.4 编辑用户

### 接口说明

修改用户信息。

### URL

```http
PUT /api/users/{userId}
```

### 请求参数

| 参数名        | 类型    | 必填 | 说明             |
| ------------- | ------- | ---- | ---------------- |
| realName      | string  | 否   | 用户姓名         |
| password      | string  | 否   | 新密码           |
| canCreateTask | boolean | 否   | 是否允许创建任务 |

### 成功响应

```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

### 失败响应

```json
{
  "code": 404,
  "message": "用户不存在",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 400    | 参数非法   |
| 403    | 无权限     |
| 404    | 用户不存在 |
| 500    | 修改失败   |

------

## 3.5 删除用户

### 接口说明

删除指定用户。

### URL

```http
DELETE /api/users/{userId}
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | int  | 是   | 用户ID |

### 成功响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 失败响应

```json
{
  "code": 403,
  "message": "权限不足",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 403    | 权限不足   |
| 404    | 用户不存在 |
| 500    | 删除失败   |

------

# 4. 文书总库模块 API

对应页面：

- P1 文书总库

------

## 4.1 批量上传文书

### 接口说明

上传 PDF、Word、TXT 文书文件。

系统自动进行：

- 文件格式校验
- 哈希去重校验

### URL

```http
POST /api/documents/upload
```

### 请求类型

```http
multipart/form-data
```

### 请求参数

| 参数名 | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| files  | File[] | 是   | 上传文件列表 |

### 成功响应

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "count": 5
  }
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "文件格式错误",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明         |
| ------ | ------------ |
| 400    | 文件格式错误 |
| 409    | 文件重复上传 |
| 500    | 上传失败     |

------

## 4.2 获取文书列表

### 接口说明

获取文书库列表。

支持：

- 文书ID筛选
- 标题筛选
- 上传时间筛选

### URL

```http
GET /api/documents
```

### Query 参数

| 参数名     | 类型   | 必填 | 说明     |
| ---------- | ------ | ---- | -------- |
| documentId | string | 否   | 文书ID   |
| title      | string | 否   | 文书标题 |
| uploadDate | string | 否   | 上传日期 |
| page       | int    | 否   | 页码     |
| size       | int    | 否   | 每页数量 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "documentId": "W001",
        "title": "合同纠纷案",
        "uploadDate": "2026-05-04"
      }
    ]
  }
}
```

### 失败响应

```json
{
  "code": 500,
  "message": "查询失败",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明     |
| ------ | -------- |
| 400    | 参数错误 |
| 500    | 查询失败 |

------

# 5. 配置中心模块 API

对应页面：

- P2 配置中心

------

## 5.1 创建指南版本

### 接口说明

创建新的标签配置版本。

包含：

- 一级标签
- 二级标签
- 关系类型

### URL

```http
POST /api/configs/versions
```

### 请求参数

```json
{
  "versionName": "V1.0",
  "primaryTags": [
    {
      "name": "一般事实判断",
      "shortName": "GF"
    }
  ],
  "secondaryTags": [
    {
      "name": "法律条文",
      "shortName": "GM-L"
    }
  ],
  "relationTypes": [
    {
      "name": "支持",
      "shortName": "S"
    }
  ]
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "配置保存成功",
  "data": {
    "configId": 1
  }
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "版本名称不能为空",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明     |
| ------ | -------- |
| 400    | 参数错误 |
| 403    | 权限不足 |
| 500    | 保存失败 |

------

# 6. 任务管理模块 API

对应页面：

- P4
- P5
- P6

------

## 6.1 创建任务

### 接口说明

创建新的法律文书标注任务。

对应作业中的：

> 发布需求

### URL

```http
POST /api/tasks
```

### 请求参数

| 参数名         | 类型     | 必填 | 说明                |
| -------------- | -------- | ---- | ------------------- |
| taskName       | string   | 是   | 任务名称            |
| configId       | int      | 是   | 标签配置ID          |
| annotatorIds   | int[]    | 是   | 标注员列表          |
| reviewerId     | int      | 是   | 裁定者ID            |
| dataSourceType | string   | 是   | text/upload/library |
| content        | string   | 否   | 文本内容            |
| documentIds    | string[] | 否   | 文书ID列表          |

### 请求示例

```json
{
  "taskName": "合同法标注任务",
  "configId": 1,
  "annotatorIds": [2,3],
  "reviewerId": 5,
  "dataSourceType": "library",
  "documentIds": ["W001","W002"]
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": 1001
  }
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "标注员不能为空",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 400    | 参数不完整 |
| 403    | 无权限     |
| 404    | 配置不存在 |
| 500    | 创建失败   |

------

## 6.2 获取任务列表

### 接口说明

获取任务列表。

支持：

- 角色筛选
- 状态筛选
- 关键词搜索

对应作业中的：

> 浏览需求列表

### URL

```http
GET /api/tasks
```

### Query 参数

| 参数名  | 类型   | 必填 | 说明                       |
| ------- | ------ | ---- | -------------------------- |
| role    | string | 否   | creator/annotator/reviewer |
| status  | string | 否   | 标注中/裁决中/已完成       |
| keyword | string | 否   | 搜索关键词                 |
| page    | int    | 否   | 页码                       |
| size    | int    | 否   | 每页数量                   |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "list": [
      {
        "taskId": 1001,
        "taskName": "合同法标注",
        "status": "标注中"
      }
    ]
  }
}
```

### 错误码定义

| 错误码 | 说明     |
| ------ | -------- |
| 400    | 参数错误 |
| 500    | 查询失败 |

------

## 6.3 获取任务详情

### 接口说明

获取指定任务详细信息。

对应作业中的：

> 查看订单详情

### URL

```http
GET /api/tasks/{taskId}
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| taskId | int  | 是   | 任务ID |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1001,
    "taskName": "合同法任务",
    "status": "标注中",
    "annotators": [],
    "reviewer": {}
  }
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 404    | 任务不存在 |
| 500    | 查询失败   |

------

# 7. 标注工作台 API

对应页面：

- P8 标注工作台
- P11 数据选择页

------

## 7.1 获取待标注数据列表

### URL

```http
GET /api/tasks/{taskId}/items
```

### Query 参数

| 参数名 | 类型   | 必填 | 说明          |
| ------ | ------ | ---- | ------------- |
| status | string | 否   | 待标注/已标注 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "dataId": "D001",
      "status": "待标注"
    }
  ]
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 404    | 任务不存在 |
| 500    | 查询失败   |

------

## 7.2 获取标注详情

### URL

```http
GET /api/tasks/{taskId}/items/{dataId}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": "依法成立的合同，自成立时生效。",
    "propositions": [],
    "relations": []
  }
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 404    | 数据不存在 |
| 500    | 查询失败   |

------

## 7.3 提交标注结果

### 接口说明

提交或暂存标注结果。

对应作业中的：

> 提交评价

### URL

```http
POST /api/annotations/submit
```

### 请求参数

```json
{
  "taskId": 1001,
  "dataId": "D001",
  "propositions": [
  	  { "propId": "P1", "text": "依法成立的合同", "tag": "GM-L" },
      { "propId": "P2", "text": "自成立时生效", "tag": "GM-I" }
  ],
  "relations": [
      { "relId": "R1", "type": "S", "source": "P1", "target": "P2", "level": "M1" },
      { "relId": "R2", "type": "M", "source": "R1", "target": "P3" } 
  ]，
  "isDraft": false
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "提交成功",
  "data": null
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "关系非法",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明         |
| ------ | ------------ |
| 400    | 关系结构非法 |
| 403    | 无标注权限   |
| 404    | 数据不存在   |
| 500    | 提交失败     |

------

# 8. 裁定模块 API

对应页面：

- P9 裁定界面

------

## 8.1 获取裁定数据

### URL

```http
GET /api/reviews/{taskId}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "annotatorResults": []
  }
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 403    | 无裁定权限 |
| 404    | 任务不存在 |

------

## 8.2 全部采纳

### URL

```http
POST /api/reviews/adopt
```

### 请求参数

| 参数名      | 类型 | 必填 | 说明           |
| ----------- | ---- | ---- | -------------- |
| taskId      | int  | 是   | 任务ID         |
| annotatorId | int  | 是   | 被采纳标注员ID |

### 成功响应

```json
{
  "code": 200,
  "message": "裁定完成",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明           |
| ------ | -------------- |
| 403    | 无裁定权限     |
| 404    | 标注结果不存在 |
| 500    | 裁定失败       |

------

# 9. 结果查看与导出 API

对应页面：

- P7
- P10

------

## 9.1 获取结果列表

### URL

```http
GET /api/tasks/{taskId}/results
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

### 错误码定义

| 错误码 | 说明       |
| ------ | ---------- |
| 404    | 任务不存在 |
| 500    | 查询失败   |

------

## 9.2 导出结果

### 接口说明

导出：

- PNG
- SVG
- JSON
- XLSX
- ZIP

格式结果。

仅任务创建者与裁定者允许导出。

### URL

```http
GET /api/tasks/{taskId}/export
```

### Query 参数

| 参数名 | 类型   | 必填 | 说明                  |
| ------ | ------ | ---- | --------------------- |
| format | string | 是   | json/xlsx/png/svg/zip |

### 成功响应

```json
{
  "code": 200,
  "message": "导出成功",
  "data": {
    "downloadUrl": "/download/task1001.zip"
  }
}
```

### 失败响应

```json
{
  "code": 403,
  "message": "无导出权限",
  "data": null
}
```

### 错误码定义

| 错误码 | 说明         |
| ------ | ------------ |
| 400    | 导出格式非法 |
| 403    | 无导出权限   |
| 404    | 任务不存在   |
| 500    | 导出失败     |

------

# 10. AI 辅助实验分析

## 10.1 接口命名问题

AI 初版生成存在：

```http
/getTaskList
/updateUser
```

等不规范命名。

修正后统一采用 RESTful 风格：

```http
GET /api/tasks
PUT /api/users/{id}
```

------

## 10.2 缺少错误处理

AI 初版仅包含成功响应。

未考虑：

- Token 失效
- 权限不足
- 参数错误
- 数据不存在

等情况。

因此增加：

- 统一错误码
- 失败响应结构

------

## 10.3 参数校验不完整

AI 初版未校验：

- 标注员不能为空
- 文件格式是否合法
- 关系是否合法

修正后增加：

- 必填字段校验
- 文件类型校验
- 关系结构校验

------

# 11. 总结

本 API 文档基于法律文书标注分析平台完整业务流程设计，采用 RESTful 风格实现：

- 用户管理
- 文书管理
- 配置管理
- 标注任务管理
- 标注与裁定流程
- 结果查看与导出

并针对：

- 参数校验
- 权限控制
- 错误处理
- Token 认证

进行了完整设计，可支持后续 Swagger/OpenAPI 自动化生成与前后端联调开发。