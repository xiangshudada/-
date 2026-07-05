INSERT INTO rule_indicator_set (indicator_code, indicator_name, scenario, rule_version, enabled)
VALUES
    ('1214_1011', '客户交易行为与其年龄不符', 'MARGIN_TRADE_AGE_MISMATCH', '2026-05-25', 1),
    ('1214_1005', '异常的大宗交易', 'BLOCK_TRADE', '2026-05-25', 1),
    ('1214_1018', '大额证券对敲', 'MATCHED_TRADE', '2026-05-25', 1)
ON DUPLICATE KEY UPDATE indicator_name = VALUES(indicator_name), scenario = VALUES(scenario), rule_version = VALUES(rule_version);

INSERT INTO rule_risk_section (id, indicator_code, risk_type, crime_type, risk_point, section_order)
VALUES
    (101101, '1214_1011', '基础调查', '-', '确认客户年龄及预警期间两融交易事实。', 1),
    (101102, '1214_1011', '非法资金注入', '贪腐、挪用资金、诈骗、非法集资、非法经营等犯罪', '排查客户账户资金和身份背景是否匹配。', 2),
    (100501, '1214_1005', '基础调查', '-', '确认被预警的大宗交易事实和价格偏离。', 1),
    (101801, '1214_1018', '基础调查', '-', '确认客户与交易对手的大额对敲交易事实。', 1)
ON DUPLICATE KEY UPDATE risk_type = VALUES(risk_type), crime_type = VALUES(crime_type), risk_point = VALUES(risk_point);

INSERT INTO rule_investigation_step
    (id, section_id, step_code, step_name, step_order, method_text, rule_text, operator_type,
     applicable_client_types, thresholds_json, judgment_basis, manual_review_required, report_field, result_target)
VALUES
    (10110101, 101101, 'STEP_1011_FACT', '确认事实情况', 1,
     '确认客户年龄；查看每一次两融交易时间、交易标的、买入和卖出单价、交易股数、交易总金额。',
     '客观陈述预警事项。',
     'OBJECTIVE_DESCRIBE', 'PERSON',
     JSON_OBJECT(),
     '本步骤客观描述，不单独判定疑点。',
     0, '可疑行为描述（资金、客户行为）', '概括写入【可疑行为描述】'),

    (10110201, 101102, 'STEP_1011_AGE', '客户年龄异常度', 1,
     '查看客户是否为高龄人员或低龄人员。',
     '年龄≥70岁或年龄≤22岁。',
     'PROFILE_RISK_CHECK', 'PERSON',
     JSON_OBJECT('metric', 'age', 'min', 22, 'max', 70),
     '若客户年龄显著偏离证券投资者主流年龄段，且无法说明合理资金来源，则存在疑点。',
     0, '疑点分析', NULL),

    (10050101, 100501, 'STEP_1005_BLOCK_FACT', '确认大宗交易情况', 1,
     '确认被预警的大宗交易时间、交易标的、方向、交易方式、单价、股数、总金额，以及价格偏离。',
     '客观陈述预警事项。',
     'OBJECTIVE_DESCRIBE', 'PERSON,ORG,PRODUCT',
     JSON_OBJECT(),
     '本步骤客观描述，不单独判定疑点。',
     0, '可疑行为描述（资金、客户行为）', '概括写入【可疑行为描述】'),

    (10050102, 100501, 'STEP_1005_PRICE_DEVIATION', '查看大宗交易价格偏离', 2,
     '查看成交价格相对前一日收盘价、当日均价或债券估值的偏离。',
     '根据标的类型计算价格偏离。',
     'PRICE_DEVIATION', 'PERSON,ORG,PRODUCT',
     JSON_OBJECT('metric', 'priceDeviationRate', 'min', 0.08),
     '若价格偏离较大且无法说明合理原因，则存在疑点。',
     0, '疑点分析/客户交易情况分析', NULL),

    (10180101, 101801, 'STEP_1018_MATCH_FACT', '确认交易情况', 1,
     '查看客户交易时间、标的、买入单价、交易股数、总金额、委托单价、委托股数，并查看交易对手同期数据。',
     '客观陈述预警事项。',
     'OBJECTIVE_DESCRIBE', 'PERSON,ORG,PRODUCT',
     JSON_OBJECT(),
     '本步骤客观描述，不单独判定疑点。',
     0, '可疑行为描述（资金、客户行为）', '概括写入【可疑行为描述】'),

    (10180102, 101801, 'STEP_1018_COUNTERPARTY_MATCH', '查看双方委托数量匹配性', 2,
     '对比客户和交易对手在该交易时点、该证券标的上的委托数量。',
     '委托数量差异率≤10%疑似联合行动，≥30%大概率排除。',
     'COUNTERPARTY_MATCH', 'PERSON,ORG,PRODUCT',
     JSON_OBJECT('metric', 'entrustQuantityDiffRate', 'max', 0.10),
     '若双方委托数量相近，则存在疑点。',
     0, '疑点分析/客户交易情况分析', NULL)
ON DUPLICATE KEY UPDATE step_name = VALUES(step_name), method_text = VALUES(method_text), rule_text = VALUES(rule_text);

INSERT INTO rule_data_requirement
    (step_id, requirement_code, domain, scope_type, grain, filters_json, fields_json, sample_limit, missing_data_policy)
VALUES
    (10110101, 'REQ_1011_CUSTOMER_PROFILE', 'CustomerProfile', 'ALERT_DETAIL', 'ACCOUNT_SUMMARY',
     JSON_OBJECT(), JSON_ARRAY('clientId', 'clientType', 'age', 'occupation', 'annualIncome'), 1, 'RETURN_EMPTY_WITH_REASON'),
    (10110101, 'REQ_1011_MARGIN_TRADE', 'MarginTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('businessFlags', JSON_ARRAY(4211, 4212, 4213, 4214)),
     JSON_ARRAY('tradeDate', 'tradeTime', 'securityCode', 'securityName', 'direction', 'price', 'quantity', 'amount'),
     20, 'RETURN_EMPTY_WITH_REASON'),
    (10110201, 'REQ_1011_AGE_PROFILE', 'CustomerProfile', 'ALERT_DETAIL', 'ACCOUNT_SUMMARY',
     JSON_OBJECT(), JSON_ARRAY('clientId', 'age', 'occupation', 'annualIncome'), 1, 'RETURN_EMPTY_WITH_REASON'),

    (10050101, 'REQ_1005_BLOCK_TRADE', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'),
     JSON_ARRAY('tradeDate', 'tradeTime', 'securityCode', 'securityName', 'direction', 'price', 'quantity', 'amount', 'tradeMode'),
     20, 'RETURN_EMPTY_WITH_REASON'),
    (10050102, 'REQ_1005_BLOCK_TRADE_PRICE', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'),
     JSON_ARRAY('tradeDate', 'securityCode', 'price', 'quantity', 'amount'),
     20, 'RETURN_EMPTY_WITH_REASON'),

    (10180101, 'REQ_1018_SECURITY_TRADE', 'SecurityTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(),
     JSON_ARRAY('tradeDate', 'tradeTime', 'securityCode', 'securityName', 'direction', 'price', 'quantity', 'amount'),
     20, 'RETURN_EMPTY_WITH_REASON'),
    (10180102, 'REQ_1018_SECURITY_TRADE_MATCH', 'SecurityTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(),
     JSON_ARRAY('tradeDate', 'tradeTime', 'securityCode', 'securityName', 'direction', 'price', 'quantity', 'amount'),
     20, 'RETURN_EMPTY_WITH_REASON');
