# 算子设计边界参考

## 算子职责

`RuleOperator` 逻辑上绑定在 `VerificationItem` 上；当前接口输入仍是核验项视图下的：

- `InvestigationStep`
- `List<Fact>`

输出是：

- `DecisionResult`

算子只做判断，不查询客户数据库，不拼接报告正文，不处理租户字段映射。

`InvestigationStep` 是业务调查步骤，`VerificationItem` 是步骤下的可执行核验项。一个业务步骤包含多个自动判断条件时，拆多个 `VerificationItem`，不要仅因为需要多个算子就拆多个同级业务步骤。

## 现有类型语义

- `OBJECTIVE_DESCRIBE`：客观事实描述，不单独判断疑点。
- `THRESHOLD_COMPARE`：单指标阈值比较。
- `RATIO_COMPARE`：多个指标计算比例后比较阈值。
- `TIME_INTERVAL_COMPARE`：两个事件之间的时间间隔判断。
- `SEQUENCE_PATTERN`：按时间顺序识别组合行为。
- `PRICE_DEVIATION`：成交价与收盘价、均价、估值、基准价的偏离判断。
- `RELATION_MATCH`：客户、交易对手、关联账户之间的关系判断。
- `COUNTERPARTY_MATCH`：交易对手行为匹配，例如对敲数量、时间、标的匹配。
- `IDENTITY_ASSET_MISMATCH`：身份、收入、注册资本与资产或资金流不匹配。
- `PROFILE_RISK_CHECK`：年龄、职业、收入区间等客户画像风险。
- `MANUAL_REVIEW`：需要人工补充材料或回访的步骤。

## 新增算子检查清单

新增算子前先确认：

1. 该逻辑是否真的不能由已有算子加参数完成。
2. 所需输入是否已经能通过 `DataRequirement` 和 `Fact` 表达。
3. 输出是否能归约为 `HIT`、`NOT_HIT`、`NEUTRAL`、`PENDING_MANUAL`、`DATA_MISSING`。
4. 阈值、窗口、字段名是否来自配置，而不是硬编码。
5. 是否需要覆盖命中、未命中、数据缺失三类测试。
6. 第三方或外部事实是否已经真实接入；未接入时返回 `DATA_MISSING` 或转 `MANUAL_REVIEW`，不要在算子或 seed 中伪造外部事实。

如果一个规则文本同时包含多个稳定判断语义，例如“转账金额较大、证券交易金额较小、交易笔数较少、资金交易比异常”，优先拆为多个核验项：

- `THRESHOLD_COMPARE`：转账金额是否超过阈值。
- `THRESHOLD_COMPARE`：证券交易金额或笔数是否低于阈值。
- `RATIO_COMPARE`：转账金额与证券交易金额比例是否异常。

保留同一个 `InvestigationStep` 承载业务问题，例如“查看客户真实交易意图”。

## 不要做的事

- 不要在算子里写 SQL。
- 不要在算子里识别客户物理字段名。
- 不要在算子里根据 `indicatorCode` 写特例分支。
- 不要在算子里生成完整报告。
- 不要为了一个指标创建只服务一个步骤的窄算子，除非规则确实具有独立可复用语义。
- 不要把一个业务步骤按算子数量硬拆成多个 `InvestigationStep`；需要多个算子时拆 `VerificationItem`。
- 不要为了消除 `DATA_MISSING` 往样例库里填充并不存在的第三方数据；公告、司法、工商、外部受益所有人等材料缺失时先留白或人工核查。

## 1214_1005 的判断示例

`1214_1005` 不是只需要 `PRICE_DEVIATION`。

它至少涉及：

- 大宗交易事实：`OBJECTIVE_DESCRIBE`
- 价格偏离：`PRICE_DEVIATION`
- 交易对手是否我司客户：新数据域或 `COUNTERPARTY_MATCH`
- 同设备、同联系方式、股权、实控人：`RELATION_MATCH`
- 公告、解禁、减持、定增验证：外部事件领域加人工核查
- 开户后快速大宗交易：`TIME_INTERVAL_COMPARE`
- 大宗交易占比：`RATIO_COMPARE`
- 交易后清仓、转出、销户：`SEQUENCE_PATTERN`
- 身份与资产规模不匹配：`IDENTITY_ASSET_MISMATCH`
- 资金来源和合理性：`MANUAL_REVIEW` 加报告层汇总

如果一个指标步骤当前落到 `GenericNeutralOperator`，它只能说明事实已生成，不能说明核验要求已满足。

`1214_1005` 中公告、司法、工商、外部关系等第三方材料可以先保持空数据或 `MANUAL_REVIEW`。内部交易、转账、客户画像等已可接入的数据，应尽量通过通用算子完成自动判断。
