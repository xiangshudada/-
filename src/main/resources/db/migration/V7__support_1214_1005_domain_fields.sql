ALTER TABLE domain_customer_profile
    ADD COLUMN account_open_date DATE,
    ADD COLUMN account_asset DECIMAL(20, 2),
    ADD COLUMN year_net_inflow_amount DECIMAL(20, 2);

ALTER TABLE domain_security_trade
    ADD COLUMN account_open_date DATE,
    ADD COLUMN counterparty_client_id VARCHAR(64),
    ADD COLUMN counterparty_is_client TINYINT,
    ADD COLUMN benchmark_price DECIMAL(20, 6),
    ADD COLUMN previous_close_price DECIMAL(20, 6),
    ADD COLUMN market_average_price DECIMAL(20, 6),
    ADD COLUMN valuation_price DECIMAL(20, 6),
    ADD COLUMN market_amount DECIMAL(20, 2),
    ADD COLUMN market_total_amount DECIMAL(20, 2),
    ADD COLUMN price_deviation_rate DECIMAL(20, 8),
    ADD COLUMN short_term_price_move_rate DECIMAL(20, 8),
    ADD COLUMN market_value_benefit_amount DECIMAL(20, 2),
    ADD COLUMN account_to_first_block_trade_days INT,
    ADD COLUMN account_to_block_trade_days INT,
    ADD COLUMN position_clearance_days INT,
    ADD COLUMN empty_to_close_account_days INT;

CREATE TABLE IF NOT EXISTS domain_customer_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    relation_date DATE NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    related_client_id VARCHAR(64) NOT NULL,
    relation_type VARCHAR(64),
    same_device TINYINT,
    same_phone TINYINT,
    same_address TINYINT,
    same_contact TINYINT,
    ownership TINYINT,
    controller TINYINT,
    legal_representative TINYINT,
    beneficial_owner TINYINT,
    agent TINYINT,
    INDEX idx_customer_relation_client_date (client_id, relation_date)
);

INSERT INTO rule_domain_binding
    (id, tenant_id, data_source_profile, domain, source_name, table_name, client_id_column, date_column, static_filters_json, enabled)
VALUES
    (6, 'default', 'default', 'CustomerRelation', 'sample-mysql', 'domain_customer_relation', 'client_id', 'relation_date', JSON_OBJECT(), 1)
ON DUPLICATE KEY UPDATE
    source_name = VALUES(source_name),
    table_name = VALUES(table_name),
    client_id_column = VALUES(client_id_column),
    date_column = VALUES(date_column),
    static_filters_json = VALUES(static_filters_json),
    enabled = VALUES(enabled);

INSERT INTO rule_field_mapping (binding_id, canonical_field, source_column, required)
VALUES
    (1, 'accountOpenDate', 'account_open_date', 0),
    (1, 'accountAsset', 'account_asset', 0),
    (1, 'yearNetInflowAmount', 'year_net_inflow_amount', 0),

    (2, 'accountOpenDate', 'account_open_date', 0),
    (2, 'counterpartyClientId', 'counterparty_client_id', 0),
    (2, 'counterpartyIsClient', 'counterparty_is_client', 0),
    (2, 'benchmarkPrice', 'benchmark_price', 0),
    (2, 'previousClosePrice', 'previous_close_price', 0),
    (2, 'marketAveragePrice', 'market_average_price', 0),
    (2, 'valuationPrice', 'valuation_price', 0),
    (2, 'marketAmount', 'market_amount', 0),
    (2, 'marketTotalAmount', 'market_total_amount', 0),
    (2, 'priceDeviationRate', 'price_deviation_rate', 0),
    (2, 'shortTermPriceMoveRate', 'short_term_price_move_rate', 0),
    (2, 'marketValueBenefitAmount', 'market_value_benefit_amount', 0),
    (2, 'accountToFirstBlockTradeDays', 'account_to_first_block_trade_days', 0),
    (2, 'accountToBlockTradeDays', 'account_to_block_trade_days', 0),
    (2, 'positionClearanceDays', 'position_clearance_days', 0),
    (2, 'emptyToCloseAccountDays', 'empty_to_close_account_days', 0),

    (4, 'accountOpenDate', 'account_open_date', 0),
    (4, 'counterpartyClientId', 'counterparty_client_id', 0),
    (4, 'counterpartyIsClient', 'counterparty_is_client', 0),
    (4, 'benchmarkPrice', 'benchmark_price', 0),
    (4, 'previousClosePrice', 'previous_close_price', 0),
    (4, 'marketAveragePrice', 'market_average_price', 0),
    (4, 'valuationPrice', 'valuation_price', 0),
    (4, 'marketAmount', 'market_amount', 0),
    (4, 'marketTotalAmount', 'market_total_amount', 0),
    (4, 'priceDeviationRate', 'price_deviation_rate', 0),
    (4, 'shortTermPriceMoveRate', 'short_term_price_move_rate', 0),
    (4, 'marketValueBenefitAmount', 'market_value_benefit_amount', 0),
    (4, 'accountToFirstBlockTradeDays', 'account_to_first_block_trade_days', 0),
    (4, 'accountToBlockTradeDays', 'account_to_block_trade_days', 0),
    (4, 'positionClearanceDays', 'position_clearance_days', 0),
    (4, 'emptyToCloseAccountDays', 'empty_to_close_account_days', 0),

    (6, 'relationDate', 'relation_date', 1),
    (6, 'clientId', 'client_id', 1),
    (6, 'relatedClientId', 'related_client_id', 0),
    (6, 'relationType', 'relation_type', 0),
    (6, 'sameDevice', 'same_device', 0),
    (6, 'samePhone', 'same_phone', 0),
    (6, 'sameAddress', 'same_address', 0),
    (6, 'sameContact', 'same_contact', 0),
    (6, 'ownership', 'ownership', 0),
    (6, 'controller', 'controller', 0),
    (6, 'legalRepresentative', 'legal_representative', 0),
    (6, 'beneficialOwner', 'beneficial_owner', 0),
    (6, 'agent', 'agent', 0)
ON DUPLICATE KEY UPDATE
    source_column = VALUES(source_column),
    required = VALUES(required);

INSERT INTO rule_verification_data_requirement
    (verification_item_id, requirement_code, domain, scope_type, grain, filters_json, fields_json, sample_limit, missing_data_policy)
VALUES
    (1005010201, 'REQ_1005_BLOCK_PRICE', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'price', 'quantity', 'amount', 'tradeMode', 'benchmarkPrice', 'previousClosePrice', 'marketAveragePrice', 'valuationPrice', 'priceDeviationRate'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005010301, 'REQ_1005_COUNTERPARTY_FLAG', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'counterpartyClientId', 'counterpartyIsClient'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005010302, 'REQ_1005_SAME_CONTACT_DEVICE', 'CustomerRelation', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('relationDate', 'relatedClientId', 'relationType', 'sameDevice', 'samePhone', 'sameAddress'), 50, 'RETURN_EMPTY_WITH_REASON'),
    (1005010303, 'REQ_1005_OWNERSHIP_RELATION', 'CustomerRelation', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('relationDate', 'relatedClientId', 'relationType', 'ownership', 'controller', 'legalRepresentative', 'beneficialOwner'), 50, 'RETURN_EMPTY_WITH_REASON'),
    (1005020101, 'REQ_1005_ROLE_BLOCK_PRICE', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'price', 'quantity', 'amount', 'tradeMode', 'benchmarkPrice', 'previousClosePrice', 'priceDeviationRate'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005020201, 'REQ_1005_ACCOUNT_TO_BLOCK', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'tradeMode', 'accountOpenDate', 'accountToFirstBlockTradeDays'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005020301, 'REQ_1005_POST_TRADE_SECURITY', 'SecurityTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'price', 'quantity', 'amount', 'tradeMode', 'positionClearanceDays'), 200, 'RETURN_EMPTY_WITH_REASON'),
    (1005020303, 'REQ_1005_CLOSE_ACCOUNT_TRANSFER', 'FundTransfer', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('transferDate', 'direction', 'amount', 'bankName'), 200, 'RETURN_EMPTY_WITH_REASON'),
    (1005020402, 'REQ_1005_RELATED_OCCUPATION_RELATION', 'CustomerRelation', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('relationDate', 'relatedClientId', 'relationType', 'sameContact', 'sameDevice'), 50, 'RETURN_EMPTY_WITH_REASON'),
    (1005030101, 'REQ_1005_VOLUME_SHARE_BLOCK', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'amount', 'tradeMode', 'marketAmount', 'marketTotalAmount'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005030201, 'REQ_1005_SHORT_PRICE_MOVE', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'price', 'amount', 'tradeMode', 'shortTermPriceMoveRate'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005040103, 'REQ_1005_ACCOUNT_TO_BLOCK_FUNDS', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'amount', 'tradeMode', 'accountOpenDate', 'accountToBlockTradeDays'), 20, 'RETURN_EMPTY_WITH_REASON'),
    (1005040301, 'REQ_1005_PERSON_PROFILE_ASSET', 'CustomerProfile', 'ALERT_DETAIL', 'ACCOUNT_SUMMARY',
     JSON_OBJECT(), JSON_ARRAY('clientId', 'clientType', 'age', 'occupation', 'annualIncome', 'accountAsset', 'yearNetInflowAmount'), 1, 'RETURN_EMPTY_WITH_REASON'),
    (1005040302, 'REQ_1005_ORG_PROFILE_CAPITAL', 'CustomerProfile', 'ALERT_DETAIL', 'ACCOUNT_SUMMARY',
     JSON_OBJECT(), JSON_ARRAY('clientId', 'clientType', 'registeredCapital', 'accountAsset', 'yearNetInflowAmount'), 1, 'RETURN_EMPTY_WITH_REASON'),
    (1005050101, 'REQ_1005_PERSON_ORG_RELATION', 'CustomerRelation', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT(), JSON_ARRAY('relationDate', 'relatedClientId', 'relationType', 'legalRepresentative', 'agent', 'beneficialOwner', 'controller'), 50, 'RETURN_EMPTY_WITH_REASON'),
    (1005050201, 'REQ_1005_PERSONAL_PROFIT_BLOCK', 'BlockTrade', 'WARNING_PERIOD', 'DETAIL',
     JSON_OBJECT('tradeMode', 'BLOCK'), JSON_ARRAY('tradeDate', 'securityCode', 'direction', 'price', 'quantity', 'amount', 'tradeMode', 'benchmarkPrice', 'marketValueBenefitAmount'), 20, 'RETURN_EMPTY_WITH_REASON')
ON DUPLICATE KEY UPDATE
    domain = VALUES(domain),
    scope_type = VALUES(scope_type),
    grain = VALUES(grain),
    filters_json = VALUES(filters_json),
    fields_json = VALUES(fields_json),
    sample_limit = VALUES(sample_limit),
    missing_data_policy = VALUES(missing_data_policy);

UPDATE domain_customer_profile
SET account_open_date = '2026-06-01',
    account_asset = 1800000.00,
    year_net_inflow_amount = 1600000.00
WHERE client_id = 'C0002';

UPDATE domain_security_trade
SET account_open_date = '2026-06-01',
    counterparty_client_id = 'CP0001',
    counterparty_is_client = 1,
    benchmark_price = 17.200000,
    previous_close_price = 17.200000,
    market_average_price = 17.000000,
    market_amount = 1580000.00,
    market_total_amount = 10000000.00,
    price_deviation_rate = -0.08139535,
    short_term_price_move_rate = 0.08860759,
    market_value_benefit_amount = 140000.00,
    account_to_first_block_trade_days = 19,
    account_to_block_trade_days = 19,
    position_clearance_days = 15,
    empty_to_close_account_days = 20
WHERE client_id = 'C0002'
  AND trade_mode = 'BLOCK';

INSERT INTO domain_customer_relation
    (relation_date, client_id, related_client_id, relation_type, same_device, same_phone, same_address,
     same_contact, ownership, controller, legal_representative, beneficial_owner, agent)
VALUES
    ('2026-06-20', 'C0002', 'CP0001', 'SAME_DEVICE', 1, 0, 0, 0, 0, 0, 0, 0, 0),
    ('2026-06-20', 'C0002', 'ORG0001', 'LEGAL_REPRESENTATIVE', 0, 0, 0, 0, 0, 0, 1, 1, 0);
