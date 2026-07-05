---
name: aml-rule-engine-architecture
description: Project-level guidance for the AML rule engine product. Use when Codex is asked to evaluate or change the aml-rule-engine-product architecture, add suspicious-transaction requirements, add or classify RuleOperator implementations, add DomainDataAdapter/data-source bindings, decide whether a requirement belongs in the rule model, adapter layer, operator layer, or report layer, or assess whether an indicator such as 1214_1005 is fully supported.
---

# AML 规则引擎架构 Skill

## 工作原则

先判断需求属于哪一层，再动代码。不要把客户字段、客户 SQL、指标特例写进 `RuleExecutionService` 或通用算子。

目标项目默认位于：

```text
/Users/youzi/Desktop/zlpei/aml-rule-engine-product
```

旧项目参考实现位于：

```text
/Users/youzi/Desktop/zlpei/ai-financial-risk-monitor
```

## 接入新需求的判断流程

1. 先读目标指标或需求对应的规则文本、现有 seed 配置、已实现算子。
2. 判断缺的是数据、事实指标、算子、报告映射，还是人工核查流程。
3. 只在边界清晰后实现：
   - 客户表字段差异：放到数据源绑定。
   - 新领域数据：新增 `DomainDataAdapter` 或绑定配置。
   - 新判断逻辑：新增 `RuleOperator`。
   - 新指标步骤：新增 `InvestigationStep`；同一步骤内多个自动核验条件新增 `VerificationItem`。
   - 报告组织：放到报告层，不放进算子。
   - 第三方或外部数据暂缺：保留为空数据、人工核查或待补领域，不为了演示效果伪造外部事实。
4. 修改后运行：

```bash
/Users/youzi/Desktop/zlpei/ai-financial-risk-monitor/aml-backend/mvnw -f /Users/youzi/Desktop/zlpei/aml-rule-engine-product/pom.xml test
```

## 快速边界

- `RuleExecutionService` 只负责编排；按 `RiskSection` 预加载去重后的数据需求，不放客户 SQL、不放指标特例。
- `DomainDataAdapter` 只负责把客户数据投影为标准 `DomainDataset`。
- `FactFactory` 只负责从领域数据形成事实指标和样本。
- `InvestigationStep` 表达业务调查步骤；`VerificationItem` 表达可执行核验项和算子绑定。
- `RuleOperator` 只负责根据核验项事实和配置做判断。
- 复杂指标不能长期落在 `GenericNeutralOperator`；可自动判断的稳定语义应补通用算子，外部材料不足的语义留给 `MANUAL_REVIEW`。
- `ReportMapping` 只负责报告字段归属，不负责业务判断。
- Java 枚举用于引擎协议值；指标编码、步骤编码、领域名保留为可配置字符串。

## 何时读取引用

- 需要解释或调整整体架构时，读取 [references/architecture.md](references/architecture.md)。
- 需要新增、拆分或评估算子时，读取 [references/operator-boundaries.md](references/operator-boundaries.md)。
