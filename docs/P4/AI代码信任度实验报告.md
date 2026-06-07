# AI 代码信任度实验报告
 
**实验功能点：配置中心二级标签创建与校验**

---

## 一、实验设计

### 1.1 实验目的

通过让 AI 直接生成"配置中心二级标签创建"功能的完整代码，观察 AI 生成代码的质量，分析其是否可直接用于项目，以及人工审查能发现什么问题。

### 1.2 功能点选择理由

选择**配置中心二级标签的创建与校验**作为实验对象，原因：
- 涉及核心业务约束：二级标签的添加必须基于已存在的一级标签，不允许孤立创建
- 规模适中，包含前端表单（选择一级标签 + 填写标签名/简称/排序）、后端校验（级联存在性检查）、数据库写入
- 项目中实际出现过相关 Bug（"配置中心中二级标签在没有对应的一级标签时仍可添加"），验证 AI 生成的代码是否能主动防范此类问题
- 属于 P1 配置中心模块，不影响标注核心流程的稳定性

### 1.3 使用 Prompt

向 AI 工具（Deepseek）提交以下 Prompt：

> 为"裁判文书论证标注系统"的配置中心模块生成二级标签创建功能，技术栈：Vue 3 + Element Plus + Spring Boot + MyBatis + MySQL。要求：
> 1. 二级标签属于某个 guide_version 下的一级标签（label_l1），字段包含：所属指南版本ID、所属一级标签ID、名称name、简称abbr、排序sort_order
> 2. 前端：在配置中心页面，用户选择一级标签后，弹窗输入二级标签名称、简称、排序，提交到后端
> 3. 后端：接收 JSON 后校验参数，插入 label_l2 表
> 4. 需要基本的参数校验

---

## 二、AI 直接生成结果

### 2.1 AI 生成代码摘要

**后端 Controller（AI 直出）**：
```java
@PostMapping("/api/config/label-l2")
public ApiResponse<Void> createLabelL2(@RequestBody LabelL2 label) {
    labelL2Mapper.insert(label);
    return ApiResponse.ok("创建成功", null);
}
```

**后端 Service（AI 直出）**：
```java
public void createLabelL2(LabelL2 label) {
    // 校验名称不为空
    if (label.getName() == null || label.getName().trim().isEmpty()) {
        throw new RuntimeException("标签名称不能为空");
    }
    labelL2Mapper.insert(label);
}
```

**前端 Vue（AI 直出）**：
```html
<el-dialog v-model="visible" title="添加二级标签">
  <el-form :model="form">
    <el-form-item label="一级标签"><el-select v-model="form.parentL1Id"><el-option v-for="l1 in l1List" :key="l1.id" :label="l1.name" :value="l1.id" /></el-select></el-form-item>
    <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
    <el-form-item label="简称"><el-input v-model="form.abbr" /></el-form-item>
    <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
  </el-form>
  <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
</el-dialog>
```

### 2.2 AI 直出运行结果

| 指标 | 结果 |
|------|------|
| 编译是否通过 | 后端通过，前端通过 |
| 功能是否可运行 | 基本可运行，但存在关键业务规则缺失 |
| 测试通过情况 | 未编写测试 |
| 代码行数 | 后端 ~15 行，前端 ~40 行 |

---

## 三、人工检查与发现的问题

### 3.1 问题一（致命）：未校验所属一级标签是否存在

**问题描述**：AI 生成的 Service 仅校验了 `name` 字段是否为空，但没有校验 `parentL1Id` 对应的一级标签是否真实存在。前端随意选择一个一级标签 ID 甚至伪造请求中的 `parentL1Id`，后端都会直接插入。

**影响**：可以创建挂载到不存在的一级标签下的"孤儿"二级标签，导致标注界面加载二级标签列表时出现数据不一致。这与项目中实际出现的 Bug（"配置中心中二级标签在没有对应的一级标签时仍可添加"）完全吻合。

**修复**：
```java
LabelL1 parent = labelL1Mapper.selectById(label.getParentL1Id());
if (parent == null) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属一级标签不存在");
}
```

### 3.2 问题二：未校验 guide_version_id 与一级标签的版本一致性

**问题描述**：`label_l2` 表同时包含 `guide_version_id` 和 `parentL1Id`，AI 代码没有校验请求中的 `guide_version_id` 是否与一级标签所属的版本一致。如果前端传了版本 A 的 `guide_version_id` 但选了版本 B 的一级标签，会导致数据混乱。

**影响**：二级标签与一级标签分属不同指南版本，导出一致性问题。

**修复**：在插入前校验 `parent.getGuideVersionId().equals(label.getGuideVersionId())`。

### 3.3 问题三：简称唯一性约束未校验

**问题描述**：`label_l2` 表设计了唯一复合索引 `uk_version_abbr (guide_version_id, abbr)`，即同一指南版本下简称不可重复。AI 代码未校验简称重复，直接 insert 会抛数据库异常，且异常信息对前端不友好。

**影响**：用户创建时看到数据库层的原始异常信息（`Duplicate entry`），而非友好的业务提示。

**修复**：插入前先查询 `labelL2Mapper.existsByVersionAndAbbr(guideVersionId, abbr)`，如已存在则返回友好错误"该简称在当前版本下已存在"。

### 3.4 问题四：前端未展示一级标签与二级标签的级联关系

**问题描述**：AI 生成的前端仅是一个表单弹窗，但未考虑"按一级标签分组展示已有二级标签"的列表UI，用户无法直观看到哪些一级标签下已有哪些二级标签。

**影响**：用户可能重复创建相同的二级标签，或不清楚当前版本的标签结构。且一级标签下拉列表中未排除已删除或被禁用的标签。

**修复**：前端增加树形或分组列表展示；将"添加二级标签"按钮放在每个一级标签行内而非独立的全局按钮。

---

## 四、修复后结果对比

| 指标 | AI 直出 | 人工审查修复后 |
|------|--------|-------------|
| 编译是否通过 | 通过 | 通过 |
| 功能是否可运行 | 部分可运行 | 正常可运行 |
| 一级标签存在性校验 | 无 | 有 |
| 版本一致性校验 | 无 | 有 |
| 简称唯一性校验 | 无（依赖数据库异常） | 有（业务层友好提示） |
| 级联关系展示 | 无 | 有（分组列表） |
| 异常处理 | 仅校验 name 为空 | 覆盖各级联校验的三类异常 |
| 测试是否编写 | 无 | 人工补充基本验证 |

---

## 五、实验结论

### 5.1 AI 代码的优势

- **框架层代码几乎没有错误**：Spring Boot 注解、Vue 组件结构、Element Plus 组件用法均正确。
- **基本的非空校验能主动覆盖**：AI 知道校验 `name` 不为空，说明对"必填字段校验"有基本意识。

### 5.2 AI 代码的典型缺陷

- **外键/级联关系的存在性校验完全缺失**：这是本次实验最关键的发现。AI 无法从数据库 DDL 中推断出"二级标签必须挂在已存在的一级标签下"这一业务约束，只生成了最直接的 INSERT 语句。这直接导致了项目中实际出现过的 Bug。
- **跨表数据一致性校验缺失**：`guide_version_id` 与 `parentL1Id` 的一致性 AI 完全没有意识。
- **唯一约束依赖数据库层而非业务层**：AI 默认"数据库报错即报错"，不做前端友好的业务校验。
- **UI 层面缺少数据关系的直观展示**：AI 只生成 CRUD 表单，不考虑用户理解数据结构的需要。

### 5.3 信任度评估

- AI 直出代码直接用于项目的风险等级：**高** — 问题一（孤儿标签）会直接破坏数据完整性
- AI 直出代码的测试通过率（假设有测试）：约 **30%**（仅正常路径通过，所有边界和级联校验场景失败）
- 人工审查修复后的测试通过率：约 **90%**（补全三级校验后覆盖大多数场景）
- 核心结论：**AI 对涉及外键级联关系的业务约束完全盲区。数据库的 DDL 约束（外键、唯一索引）AI 不会自动转化为应用层校验逻辑。这类代码必须经过掌握完整数据库设计的人工审查，否则会引入数据完整性 Bug。**

---

## 六、修复后的最终代码

修复后的代码核心改动：
- 后端 Service 增加三级校验：一级标签存在性 → 版本一致性 → 简称唯一性
- 前端改为按一级标签分组的列表 + 行内添加按钮的模式
- 异常返回统一使用 `ResponseStatusException` 携带友好中文提示

人工修改幅度：约 **70%**（Service 层的校验逻辑几乎全部重写，前端布局大幅调整）
