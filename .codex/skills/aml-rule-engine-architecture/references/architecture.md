# 架构边界参考

## 核心分层

```text
API 层
  ExecutionController
    -> RuleExecutionRequest

核心执行层
  RuleExecutionService
    -> IndicatorModelRepository
    -> DomainDataAdapterRegistry
    -> RiskSection 数据需求预加载
    -> FactFactory
    -> RuleOperatorRegistry
    -> ExecutionResultRepository

规则模型层
  IndicatorModel
    RiskSection
      InvestigationStep
        VerificationItem
          DataRequirement
          ThresholdProfile
          DecisionPolicy
        ReportMapping

数据适配层
  DomainDataAdapter
    ConfigurableJdbcDomainDataAdapter
    专用 DomainDataAdapter

数据绑定层
  DomainBinding
  rule_domain_binding
  rule_field_mapping
  rule_code_mapping

结果层
  ExecutionResult
  StepExecutionResult
  rule_execution
  rule_step_execution
```

## 放置规则

### 放到规则模型

- 指标由哪些风险段组成。
- 每个风险段有哪些调查步骤。
- `InvestigationStep` 表达业务调查步骤和报告字段映射。
- `VerificationItem` 表达步骤下具体核验项、算子类型、数据需求、阈值和判断依据。
- 一个业务步骤包含多个判断条件时，新增多个 `VerificationItem`，不要为了算子拆成多个同级 `InvestigationStep`。

### 放到执行编排

- 进入单个 `RiskSection` 时，汇总该风险段下所有核验项的 `DataRequirement`。
- 按领域、范围、粒度、过滤条件、标准字段去重后预加载数据。
- 同一数据需求被多个核验项复用时，只查询一次；不同 `sampleLimit` 取最大值，核验项消费时再裁剪样本。
- 预加载只做编排和缓存，不写客户 SQL、不写指标特例。

### 放到数据源绑定

- 客户表名。
- 客户字段名。
- 客户码值到标准码值的映射。
- 不改变业务含义的单表过滤。

### 放到 DomainDataAdapter

- 从客户数据源查询并返回标准领域数据。
- API、文件、跨库、复杂拼装等无法用简单绑定表达的数据获取逻辑。
- 只做数据投影，不做疑点判断。

### 放到 FactFactory 或专用事实构造器

- 从明细样本提取 `count`、`totalAmount`、`totalQuantity` 等事实指标。
- 为算子准备稳定的 metrics 和 samples。
- 数据质量说明。
- 不要假设所有规则都需要统计指标；关系、画像、人工核查类规则可以只依赖结构化字段或样本。

### 放到 RuleOperator

- 命中、未命中、待人工、数据缺失的判断。
- 阈值比较、比例计算、时间窗口判断、关系匹配、价格偏离等业务逻辑。
- 输出 `DecisionResult`，不要直接写报告正文。
- 算子消费的是核验项所需事实；一个 `InvestigationStep` 可以通过多个核验项执行多个算子。

### 放到报告层

- 多个步骤结论如何拼成报告字段。
- 12 字段报告生成。
- 文案压缩、合并、排序、引用证据。

## 枚举和字符串边界

使用 Java 枚举：

- `OperatorType`
- `DecisionStatus`
- `RequirementScope`
- `DataGrain`
- `MissingDataPolicy`
- `ClientType`

保留字符串配置：

- `indicatorCode`
- `stepCode`
- `requirementCode`
- `domain`
- `scenario`
- 报告字段名称

原因：枚举代表稳定协议；字符串配置代表指标、客户、领域扩展点。

## 验收问题

实现新需求前后都要回答：

- 这个需求需要新增数据域吗？
- 现有绑定能否映射客户字段？
- 需要新事实指标，还是现有 `Fact.metrics` 已足够？
- 是否需要新增算子，还是只是新增步骤配置？
- 一个业务步骤是否包含多个核验项？若是，拆 `VerificationItem`，不要硬拆业务步骤。
- 同一风险段内的数据需求是否能复用？是否应由 `RiskSection` 预加载缓存？
- 是否有人工核查闭环？
- 报告字段是否只是映射，还是需要新的报告生成逻辑？
