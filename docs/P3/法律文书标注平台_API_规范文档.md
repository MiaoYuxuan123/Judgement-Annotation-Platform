# 法律文书标注平台 API 规范文档

---

# 1. 文档说明

本文档基于《法律文书标注平台》系统实际后端实现进行 RESTful API 设计，用于规范前后端的数据交互方式。

系统主要包含以下模块：

- 用户与权限管理
- 文书总库管理
- 标签配置中心
- 任务管理
- 标注工作台
- 裁定工作台
- 结果查看与导出

## 系统模块架构

![系统模块架构](../img/P3-系统模块架构.png)

> Mermaid 源文件：[P3-系统模块架构.mmd](../img/P3-系统模块架构.mmd)

---

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

---

## 2.2 数据格式

普通接口：

```http
Content-Type: application/json
```

文件上传接口：

```http
Content-Type: multipart/form-data
```

---

## 2.3 身份认证

除登录接口外，其余接口均需携带 Token：

```http
Authorization: Bearer {token}
```

认证流程：

![认证流程](../img/P3-认证流程.png)

> Mermaid 源文件：[P3-认证流程.mmd](../img/P3-认证流程.mmd)

---

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

---

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

---

# 3. 用户与权限模块 API

对应页面：

- P3 用户管理

---

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
      "realName": "管理员",
      "role": "admin",
      "canCreateTask": true,
      "status": "在线",
      "lastSeen": "2026-05-04T10:30:00"
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

---

## 3.2 用户登出

### 接口说明

当前用户退出登录，Token 失效。

### URL

```http
POST /api/auth/logout
```

### 请求头

```http
Authorization: Bearer {token}
```

### 成功响应

```json
{
  "code": 200,
  "message": "已退出",
  "data": null
}
```

---

## 3.3 获取当前用户信息

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
    "realName": "管理员",
    "role": "admin",
    "canCreateTask": true,
    "status": "在线",
    "lastSeen": "2026-05-04T10:30:00"
  }
}
```

---

## 3.4 获取用户列表

### 接口说明

获取所有用户列表（管理员权限）。

### URL

```http
GET /api/users
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
  "data": [
    {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "role": "admin",
      "canCreateTask": true,
      "status": "在线",
      "lastSeen": "2026-05-04T10:30:00"
    }
  ]
}
```

---

## 3.5 新增用户

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
| role          | string  | 是   | admin / creator / user           |
| canCreateTask | boolean | 是   | 是否允许创建任务                 |

### 请求示例

```json
{
  "username": "user01",
  "realName": "张三",
  "password": "123456",
  "role": "user",
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

---

## 3.6 批量创建用户

### 接口说明

批量导入用户。

### URL

```http
POST /api/users/batch
```

### 请求参数

| 参数名 | 类型  | 必填 | 说明           |
| ------ | ----- | ---- | -------------- |
| users  | array | 是   | 用户信息列表   |

### 请求示例

```json
{
  "users": [
    {
      "username": "annotator01",
      "realName": "李四",
      "password": "123456",
      "role": "user",
      "canCreateTask": false
    },
    {
      "username": "annotator02",
      "realName": "王五",
      "password": "123456",
      "role": "user",
      "canCreateTask": false
    }
  ]
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "批量创建完成，成功 2 个用户",
  "data": [
    { "userId": 13 },
    { "userId": 14 }
  ]
}
```

---

## 3.7 编辑用户

### 接口说明

修改用户信息。

### URL

```http
PUT /api/users/{id}
```

### 请求参数

| 参数名        | 类型    | 必填 | 说明             |
| ------------- | ------- | ---- | ---------------- |
| realName      | string  | 否   | 用户姓名         |
| password      | string  | 否   | 新密码           |
| role          | string  | 否   | 角色             |
| canCreateTask | boolean | 否   | 是否允许创建任务 |

### 成功响应

```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

---

## 3.8 删除用户

### 接口说明

软删除指定用户（标记 is_deleted=1）。

### URL

```http
DELETE /api/users/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

# 4. 文书总库模块 API

对应页面：

- P1 文书总库

---

## 4.1 批量上传文书

### 接口说明

上传 PDF、Word、TXT 文书文件。

系统自动进行：

- 文件格式校验
- 文本提取与解析

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
  "message": "解析完成，请确认后保存",
  "data": {
    "documents": [
      {
        "title": "合同纠纷案",
        "content": "依法成立的合同..."
      }
    ]
  }
}
```

---

## 4.2 新建文书

### 接口说明

手动创建单篇文书（直接输入文本内容）。

### URL

```http
POST /api/documents
```

### 请求参数

| 参数名  | 类型   | 必填 | 说明         |
| ------- | ------ | ---- | ------------ |
| title   | string | 是   | 文书标题     |
| type    | string | 是   | 文书类型     |
| content | string | 是   | 文书文本内容 |

### 请求示例

```json
{
  "title": "民事判决书-001",
  "type": "民事判决书",
  "content": "原告XX与被告XX合同纠纷一案..."
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "文书创建成功",
  "data": {
    "documentId": 1
  }
}
```

---

## 4.3 获取文书列表

### 接口说明

获取文书库列表，支持关键词搜索。

### URL

```http
GET /api/documents
```

### Query 参数

| 参数名  | 类型   | 必填 | 说明       |
| ------- | ------ | ---- | ---------- |
| keyword | string | 否   | 搜索关键词 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "documentId": "W001",
        "title": "合同纠纷案",
        "type": "民事判决书",
        "uploadDate": "2026-05-04",
        "content": "..."
      }
    ]
  }
}
```

---

## 4.4 获取文书详情

### URL

```http
GET /api/documents/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "documentId": "W001",
    "title": "合同纠纷案",
    "type": "民事判决书",
    "content": "依法成立的合同，自成立时生效。"
  }
}
```

---

## 4.5 删除文书

### 接口说明

删除指定文书（管理员权限）。

### URL

```http
DELETE /api/documents/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

# 5. 配置中心模块 API

对应页面：

- P2 配置中心

---

## 5.1 获取配置版本列表

### URL

```http
GET /api/configs/versions
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "versionName": "V1.0",
      "description": "初始版本",
      "active": true,
      "createdAt": "2026-05-01",
      "attachmentName": null,
      "primaryTags": [
        { "shortName": "GF", "name": "一般事实判断", "description": null, "parentTag": null }
      ],
      "secondaryTags": [
        { "shortName": "GM-L", "name": "法律条文", "description": null, "parentTag": "GM" }
      ],
      "relationTypes": [
        { "shortName": "S", "name": "支持", "description": null, "parentTag": null }
      ]
    }
  ]
}
```

---

## 5.2 获取当前启用的配置

### URL

```http
GET /api/configs/versions/active
```

### 成功响应

同 5.1 单项结构，返回当前 `active=true` 的版本。

---

## 5.3 获取指定配置版本

### URL

```http
GET /api/configs/versions/{id}
```

### 成功响应

同 5.1 单项结构。

---

## 5.4 创建指南版本

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
  "description": "初始版本",
  "primaryTags": [
    {
      "shortName": "GF",
      "name": "一般事实判断"
    }
  ],
  "secondaryTags": [
    {
      "shortName": "GM-L",
      "name": "法律条文",
      "parentTag": "GM"
    }
  ],
  "relationTypes": [
    {
      "shortName": "S",
      "name": "支持",
      "isBinary": true
    }
  ]
}
```

| 字段                           | 类型    | 必填 | 说明                         |
| ------------------------------ | ------- | ---- | ---------------------------- |
| versionName                    | string  | 是   | 版本名称                     |
| description                    | string  | 否   | 版本说明                     |
| primaryTags[].shortName        | string  | 是   | 一级标签简称                 |
| primaryTags[].name             | string  | 是   | 一级标签全称                 |
| secondaryTags[].shortName      | string  | 是   | 二级标签简称                 |
| secondaryTags[].name           | string  | 是   | 二级标签全称                 |
| secondaryTags[].parentTag      | string  | 是   | 所属一级标签简称             |
| relationTypes[].shortName      | string  | 是   | 关系类型简称                 |
| relationTypes[].name           | string  | 是   | 关系类型全称                 |
| relationTypes[].isBinary       | boolean | 否   | 是否二元关系，默认 true      |

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

---

## 5.5 更新配置版本

### URL

```http
PUT /api/configs/versions/{id}
```

### 请求参数

同 5.4 创建参数结构。

### 成功响应

```json
{
  "code": 200,
  "message": "配置更新成功",
  "data": { ... }
}
```

---

## 5.6 删除配置版本

### URL

```http
DELETE /api/configs/versions/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 5.7 上传配置附件

### URL

```http
POST /api/configs/versions/{id}/attachment
```

### 请求类型

```http
multipart/form-data
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明                       |
| ------ | ---- | ---- | -------------------------- |
| file   | File | 是   | 附件文件（PDF/DOCX/TXT）   |

---

## 5.8 下载配置附件

### URL

```http
GET /api/configs/versions/{id}/attachment
```

### 成功响应

返回文件流（Content-Type 根据文件类型自动设置）。

---

# 6. 任务管理模块 API

对应页面：

- P4 任务列表
- P5 我的任务
- P6 任务详情

## 任务阶段流转

![任务阶段流转](../img/P3-任务阶段流转.png)

> Mermaid 源文件：[P3-任务阶段流转.mmd](../img/P3-任务阶段流转.mmd)

---

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
| description    | string   | 否   | 任务描述            |
| configId       | int      | 是   | 标签配置版本ID      |
| annotatorIds   | int[]    | 是   | 标注员ID列表        |
| reviewerId     | int      | 是   | 裁定者ID            |
| documentIds    | int[]    | 是   | 文书ID列表          |
| dataSourceType | string   | 否   | 数据来源类型        |

### 请求示例

```json
{
  "taskName": "合同法标注任务",
  "description": "对合同纠纷相关文书进行标注",
  "configId": 1,
  "annotatorIds": [2, 3],
  "reviewerId": 5,
  "documentIds": [1, 2]
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

---

## 6.2 获取任务列表

### 接口说明

获取所有任务列表。

支持：

- 状态筛选
- 关键词搜索

### URL

```http
GET /api/tasks
```

### Query 参数

| 参数名  | 类型   | 必填 | 说明                       |
| ------- | ------ | ---- | -------------------------- |
| status  | string | 否   | 标注中 / 待裁定 / 可导出   |
| keyword | string | 否   | 搜索关键词                 |

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
        "description": "...",
        "status": "标注中",
        "documentCount": 5,
        "annotatorCount": 2,
        "reviewerName": "赵六",
        "creatorName": "管理员",
        "creatorId": 1,
        "createdAt": "2026-05-04T10:00:00"
      }
    ]
  }
}
```

---

## 6.3 获取我的任务

### 接口说明

获取当前用户参与的任务（作为标注员或裁定者）。

### URL

```http
GET /api/tasks/my
```

### 请求头

```http
Authorization: Bearer {token}
```

### 成功响应

结构同 6.2。

---

## 6.4 获取任务详情

### URL

```http
GET /api/tasks/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "summary": {
      "taskId": 1001,
      "taskName": "合同法标注",
      "description": "...",
      "status": "标注中",
      "documentCount": 5,
      "annotatorCount": 2,
      "reviewerName": "赵六",
      "creatorName": "管理员",
      "creatorId": 1,
      "createdAt": "2026-05-04T10:00:00"
    },
    "documents": [
      {
        "id": 1,
        "documentId": "W001",
        "title": "合同纠纷案",
        "type": "民事判决书",
        "status": "标注中"
      }
    ],
    "annotators": [
      { "id": 2, "username": "annotator01", "realName": "李四" }
    ],
    "reviewer": { "id": 5, "username": "reviewer01", "realName": "赵六" },
    "configSnapshot": {
      "id": 1,
      "versionName": "V1.0",
      "primaryTags": [...],
      "secondaryTags": [...],
      "relationTypes": [...]
    }
  }
}
```

---

## 6.5 推进任务阶段

### 接口说明

将任务推进到下一阶段（标注中 → 待裁定 → 可导出）。

### URL

```http
PUT /api/tasks/{id}/stage
```

### 请求参数

| 参数名    | 类型   | 必填 | 说明               |
| --------- | ------ | ---- | ------------------ |
| newStatus | string | 是   | 目标阶段状态       |

### 成功响应

```json
{
  "code": 200,
  "message": "阶段已推进",
  "data": { ... }
}
```

---

## 6.6 更新任务配置

### 接口说明

更新任务的标签配置版本。

### URL

```http
PUT /api/tasks/{id}/config
```

### 请求参数

| 参数名   | 类型 | 必填 | 说明           |
| -------- | ---- | ---- | -------------- |
| configId | int  | 是   | 新配置版本ID   |

### 成功响应

```json
{
  "code": 200,
  "message": "配置已更新",
  "data": { ... }
}
```

---

## 6.7 删除任务

### 接口说明

删除指定任务（仅创建者可操作）。

### URL

```http
DELETE /api/tasks/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "任务已删除",
  "data": null
}
```

---

## 6.8 上传任务文书

### 接口说明

向任务中上传新的文书文件（解析后返回前端确认）。

### URL

```http
POST /api/tasks/documents/upload
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
  "message": "解析完成",
  "data": {
    "documents": [...]
  }
}
```

---

# 7. 标注工作台 API

对应页面：

- P8 标注工作台
- P11 数据选择页

## 标注流程

![标注流程](../img/P3-标注流程.png)

> Mermaid 源文件：[P3-标注流程.mmd](../img/P3-标注流程.mmd)

---

## 7.1 获取待标注数据列表

### URL

```http
GET /api/tasks/{taskId}/items
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "dataId": 1,
      "documentId": "W001",
      "title": "合同纠纷案",
      "status": "标注中"
    }
  ]
}
```

---

## 7.2 获取标注详情

### 接口说明

获取指定文书的标注详情。

支持查看其他标注员的标注结果（通过 sourceUserId 参数）或裁定结果（通过 sourceArbitration 参数）。

### URL

```http
GET /api/tasks/{taskId}/items/{dataId}
```

### Query 参数

| 参数名            | 类型    | 必填 | 说明                           |
| ----------------- | ------- | ---- | ------------------------------ |
| sourceUserId      | long    | 否   | 查看指定用户的标注结果         |
| sourceArbitration | boolean | 否   | 是否查看裁定结果               |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": "依法成立的合同，自成立时生效。",
    "propositions": [
      {
        "propId": "P1",
        "sequenceNo": 1,
        "startPos": 0,
        "endPos": 6,
        "text": "依法成立的合同",
        "tag": "GM-L"
      }
    ],
    "relations": [
      {
        "relId": "R1",
        "type": "S",
        "source": "P1",
        "target": "P2",
        "members": ["P1", "P2"]
      }
    ]
  }
}
```

---

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

| 参数名       | 类型          | 必填 | 说明                       |
| ------------ | ------------- | ---- | -------------------------- |
| taskId       | long          | 是   | 任务ID                     |
| dataId       | long          | 是   | 文书数据ID                 |
| propositions | Proposition[] | 否   | 命题列表                   |
| relations    | Relation[]    | 否   | 关系列表                   |
| isDraft      | boolean       | 是   | 是否为暂存草稿             |
| graphLayout  | object        | 否   | 论证图布局信息             |

**Proposition 结构：**

| 字段       | 类型   | 说明               |
| ---------- | ------ | ------------------ |
| propId     | string | 命题标识（如 P1）  |
| sequenceNo | int    | 文中序号           |
| startPos   | int    | 起始字符位置       |
| endPos     | int    | 结束字符位置       |
| text       | string | 选中的文本片段     |
| tag        | string | 标签路径（二级标签简称） |

**Relation 结构：**

| 字段    | 类型           | 说明                       |
| ------- | -------------- | -------------------------- |
| relId   | string         | 关系标识（如 R1）          |
| type    | string         | 关系类型简称（S/M等）      |
| source  | string         | 源节点ID（命题或关系ID）   |
| target  | string         | 目标节点ID（命题或关系ID） |
| members | string[]       | 成员ID列表                 |

### 请求示例

```json
{
  "taskId": 1001,
  "dataId": 1,
  "propositions": [
    { "propId": "P1", "sequenceNo": 1, "startPos": 0, "endPos": 6, "text": "依法成立的合同", "tag": "GM-L" },
    { "propId": "P2", "sequenceNo": 2, "startPos": 7, "endPos": 12, "text": "自成立时生效", "tag": "GM-I" }
  ],
  "relations": [
    { "relId": "R1", "type": "S", "source": "P1", "target": "P2", "members": ["P1", "P2"] }
  ],
  "isDraft": false,
  "graphLayout": null
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

---

## 7.4 保存论证图布局

### 接口说明

保存论证图的节点布局坐标信息。

### URL

```http
POST /api/annotations/layout
```

### 请求参数

| 参数名      | 类型   | 必填 | 说明           |
| ----------- | ------ | ---- | -------------- |
| taskId      | long   | 是   | 任务ID         |
| dataId      | long   | 是   | 文书数据ID     |
| graphLayout | object | 是   | 论证图布局数据 |

### 成功响应

```json
{
  "code": 200,
  "message": "布局已保存",
  "data": null
}
```

---

# 8. 裁定模块 API

对应页面：

- P9 裁定界面

## 裁定流程

![裁定流程](../img/P3-裁定流程.png)

> Mermaid 源文件：[P3-裁定流程.mmd](../img/P3-裁定流程.mmd)

---

## 8.1 获取裁定数据

### 接口说明

获取指定任务的所有标注员提交结果，用于裁定比对。

### URL

```http
GET /api/reviews/{taskId}
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
    "annotatorResults": [
      {
        "userId": 2,
        "userName": "李四",
        "propositions": [...],
        "relations": [...]
      }
    ]
  }
}
```

---

## 8.2 全部采纳

### 接口说明

直接采纳某一标注员的全部标注结果作为最终裁定。

### URL

```http
POST /api/reviews/adopt
```

### 请求参数

| 参数名      | 类型   | 必填 | 说明           |
| ----------- | ------ | ---- | -------------- |
| taskId      | long   | 是   | 任务ID         |
| dataId      | long   | 是   | 文书数据ID     |
| annotatorId | long   | 是   | 被采纳标注员ID |

### 成功响应

```json
{
  "code": 200,
  "message": "裁定完成",
  "data": null
}
```

---

## 8.3 手动裁定

### 接口说明

裁定者手动编辑标注结果，保存为裁定草稿。

### URL

```http
POST /api/reviews/manual
```

### 请求参数

| 参数名       | 类型          | 必填 | 说明         |
| ------------ | ------------- | ---- | ------------ |
| taskId       | long          | 是   | 任务ID       |
| dataId       | long          | 是   | 文书数据ID   |
| propositions | Proposition[] | 否   | 裁定后命题   |
| relations    | Relation[]    | 否   | 裁定后关系   |
| graphLayout  | object        | 否   | 论证图布局   |

### 成功响应

```json
{
  "code": 200,
  "message": "裁定草稿已保存，请在裁定界面确认",
  "data": null
}
```

---

## 8.4 确认裁定

### 接口说明

确认裁定结果生效。

### URL

```http
POST /api/reviews/confirm
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明       |
| ------ | ---- | ---- | ---------- |
| taskId | long | 是   | 任务ID     |
| dataId | long | 是   | 文书数据ID |

### 成功响应

```json
{
  "code": 200,
  "message": "裁定结果已确认",
  "data": null
}
```

---

## 8.5 取消待确认裁定

### 接口说明

取消之前已确认但未最终生效的裁定结果。

### URL

```http
POST /api/reviews/cancel-pending
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明       |
| ------ | ---- | ---- | ---------- |
| taskId | long | 是   | 任务ID     |
| dataId | long | 是   | 文书数据ID |

### 成功响应

```json
{
  "code": 200,
  "message": "已取消待确认的裁定结果",
  "data": null
}
```

---

## 8.6 退回标注员

### 接口说明

将标注结果退回指定标注员重新标注。

### URL

```http
POST /api/reviews/reject
```

### 请求参数

| 参数名 | 类型   | 必填 | 说明           |
| ------ | ------ | ---- | -------------- |
| taskId | long   | 是   | 任务ID         |
| dataId | long   | 是   | 文书数据ID     |
| userId | long   | 是   | 被退回标注员ID |
| reason | string | 是   | 退回原因       |

### 成功响应

```json
{
  "code": 200,
  "message": "已退回标注员重新标注",
  "data": null
}
```

---

# 9. 结果查看与导出 API

对应页面：

- P7 结果查看
- P10 结果导出

---

## 9.1 获取任务结果列表

### URL

```http
GET /api/tasks/{taskId}/results
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "taskId": 1001,
      "dataId": 1,
      "arbitratorId": 5,
      "propositions": [...],
      "relations": [...],
      "adoptedFrom": "annotator_2",
      "arbitratedAt": "2026-05-04T15:00:00",
      "finalResult": true
    }
  ]
}
```

---

## 9.2 导出结果

### 接口说明

导出任务标注与裁定结果。

仅任务创建者与裁定者允许导出。

### URL

```http
GET /api/tasks/{taskId}/export
```

### 成功响应

直接返回文件下载流。

---

# 10. API 架构总览

![API架构总览](../img/P3-API架构总览.png)

> Mermaid 源文件：[P3-API架构总览.mmd](../img/P3-API架构总览.mmd)

---

# 11. 总结

本 API 文档基于法律文书标注分析平台实际后端代码设计，共涵盖 **7 大模块 38 个接口**：

| 模块       | 接口数 | 说明                                       |
| ---------- | ------ | ------------------------------------------ |
| 认证       | 2      | 登录、登出                                 |
| 用户管理   | 6      | CRUD + 批量导入 + 当前用户信息             |
| 文书总库   | 5      | 文书上传、创建、列表、详情、删除           |
| 配置中心   | 8      | 指南版本 CRUD + 激活版本 + 附件上传下载    |
| 任务管理   | 12     | 任务 CRUD + 阶段推进 + 配置更新 + 文书管理 + 结果查看导出 |
| 标注工作台 | 2      | 标注提交、布局保存                         |
| 裁定工作台 | 6      | 裁定获取、采纳、手动裁定、确认、退回       |

采用 RESTful 风格，统一 JSON 数据格式，JWT Token 认证，并对参数校验、权限控制、错误处理进行了完整设计，可支持后续 Swagger/OpenAPI 自动化生成与前后端联调开发。
