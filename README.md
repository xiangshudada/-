# AML Rule Engine Product

独立的可配置 AML 核查引擎产品原型，用 Java/Spring Boot 实现。当前重点不是导入 Excel 规则文档，而是让同一套核查流程快速适配不同客户的数据源、表结构、字段名和码值体系。

## 设计目标

- 规则不绑定本地 SQL。
- 指标由调查步骤、数据需求、事实、算子和报告映射组成。
- 客户差异收敛到数据源绑定配置，规则和算子不直接依赖客户物理表。
- MySQL 使用独立 schema：`aml_engine`。

## 当前状态

项目已经具备产品化执行内核：

- 可按 `tenantId` 和 `dataSourceProfile` 选择客户数据源绑定。
- 可通过 `rule_domain_binding`、`rule_field_mapping`、`rule_code_mapping` 将客户表投影为标准领域数据。
- 可执行指标模型并生成步骤结果、报告草稿。
- 可将执行主记录和步骤记录落库。

项目尚不能完整替代旧系统的 `IndicatorExecutor`：

- 复杂业务算子仍需补齐，例如价格偏离、交易对手匹配、关系匹配、时间窗口模式。
- 标准领域数据仍需扩展，例如预警明细、交易对手、客户关系、资产、行情、司法文书、公告事件。
- 报告结果目前是草稿聚合，尚未形成完整 12 字段报告生成链路。
- 人工核查、二轮执行、批量任务、失败重试仍需继续实现。

## 运行

```bash
mysql -uroot -prootpwd < src/main/resources/sql/create_schema.sql
./mvnw spring-boot:run
```

如果使用本仓库已有 Maven Wrapper，也可以在父目录执行：

```bash
../ai-financial-risk-monitor/aml-backend/mvnw -f pom.xml spring-boot:run
```

默认连接：

```text
jdbc:mysql://localhost:3306/aml_engine?createDatabaseIfNotExist=true
username=root
password=rootpwd
```

## API

- `GET /api/health`
- `GET /api/indicators/{indicatorCode}/model`
- `POST /api/executions`

执行示例：

```json
{
  "tenantId": "default",
  "dataSourceProfile": "default",
  "indicatorCode": "1214_1011",
  "clientId": "C0001",
  "alertDate": "2026-06-24",
  "warningStartDate": "2026-06-01",
  "warningEndDate": "2026-06-30"
}
```

## 核心抽象

```text
IndicatorModel
  RiskSection
    InvestigationStep
      DataRequirement  -> DomainDataAdapter -> DomainDataset
      Fact             -> RuleOperator       -> DecisionResult
      ReportMapping    -> reportDraft
```

同一个算子可服务多个指标，指标差异通过 `DataRequirement`、阈值和报告映射表达。

## 数据源适配

规则执行只依赖标准领域数据，例如 `CustomerProfile`、`SecurityTrade`、`MarginTrade`、`BlockTrade`、`FundTransfer`。不同客户的数据源差异通过以下配置收敛：

- `rule_domain_binding`：租户、数据源 profile、领域、物理表、客户号字段、日期字段。
- `rule_field_mapping`：标准字段到客户表字段的映射。
- `rule_code_mapping`：客户码值到标准码值的映射。

执行请求中的 `tenantId` 和 `dataSourceProfile` 会定位一套绑定配置。未传时默认使用 `default/default`，对应内置示例表。

接入新客户时，优先按配置完成：

1. 在 `rule_domain_binding` 中登记领域和物理表。
2. 在 `rule_field_mapping` 中登记标准字段到客户字段的映射。
3. 在 `rule_code_mapping` 中登记客户码值到标准码值的映射。
4. 只有配置无法表达的数据来源，才新增自定义 `DomainDataAdapter`。

例如不同客户的证券交易表可以都映射为 `SecurityTrade`：

```text
客户 A: hs_deliver.client_id      -> SecurityTrade.clientId
客户 B: trade_confirm.cust_no    -> SecurityTrade.clientId

客户 A: hs_deliver.business_date -> SecurityTrade.tradeDate
客户 B: trade_confirm.init_date  -> SecurityTrade.tradeDate
```

执行结果会写入：

- `rule_execution`
- `rule_step_execution`

## 扩展示例

新增一个标准领域：

1. 在规则配置中新增 `DataRequirement.domain`。
2. 若客户数据可通过表字段映射获得，新增 `rule_domain_binding` 和 `rule_field_mapping`。
3. 若需要跨系统调用或复杂拼装，实现新的 `DomainDataAdapter`。
4. 在 `FactFactory` 或专用算子中消费该领域事实。

新增一个算子：

1. 扩展 `OperatorType`。
2. 实现 `RuleOperator`。
3. 将指标步骤的 `operator_type` 指向该类型。
4. 补充单元测试，覆盖命中、未命中、数据缺失三类情况。
