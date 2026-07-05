CREATE TABLE IF NOT EXISTS rule_domain_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    data_source_profile VARCHAR(64) NOT NULL DEFAULT 'default',
    domain VARCHAR(64) NOT NULL,
    source_name VARCHAR(128) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    client_id_column VARCHAR(128) NOT NULL,
    date_column VARCHAR(128),
    static_filters_json JSON,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_domain_binding (tenant_id, data_source_profile, domain)
);

CREATE TABLE IF NOT EXISTS rule_field_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    binding_id BIGINT NOT NULL,
    canonical_field VARCHAR(128) NOT NULL,
    source_column VARCHAR(128) NOT NULL,
    required TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_rule_field_mapping (binding_id, canonical_field),
    CONSTRAINT fk_field_mapping_binding
        FOREIGN KEY (binding_id) REFERENCES rule_domain_binding(id)
);

CREATE TABLE IF NOT EXISTS rule_code_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    binding_id BIGINT NOT NULL,
    canonical_field VARCHAR(128) NOT NULL,
    source_value VARCHAR(255) NOT NULL,
    canonical_value VARCHAR(255) NOT NULL,
    UNIQUE KEY uk_rule_code_mapping (binding_id, canonical_field, source_value),
    CONSTRAINT fk_code_mapping_binding
        FOREIGN KEY (binding_id) REFERENCES rule_domain_binding(id)
);

CREATE TABLE IF NOT EXISTS rule_execution (
    execution_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    data_source_profile VARCHAR(64) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    alert_date DATE NOT NULL,
    warning_start_date DATE NOT NULL,
    warning_end_date DATE NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    report_draft_json JSON,
    INDEX idx_rule_execution_lookup (tenant_id, indicator_code, client_id, executed_at)
);

CREATE TABLE IF NOT EXISTS rule_step_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) NOT NULL,
    step_id BIGINT,
    step_code VARCHAR(64),
    step_name VARCHAR(255),
    operator_type VARCHAR(64),
    decision_status VARCHAR(64),
    decision_summary TEXT,
    decision_details_json JSON,
    facts_json JSON,
    report_field VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_step_execution_execution (execution_id),
    CONSTRAINT fk_step_execution_execution
        FOREIGN KEY (execution_id) REFERENCES rule_execution(execution_id)
);

INSERT INTO rule_domain_binding
    (id, tenant_id, data_source_profile, domain, source_name, table_name, client_id_column, date_column, static_filters_json, enabled)
VALUES
    (1, 'default', 'default', 'CustomerProfile', 'sample-mysql', 'domain_customer_profile', 'client_id', NULL, JSON_OBJECT(), 1),
    (2, 'default', 'default', 'SecurityTrade', 'sample-mysql', 'domain_security_trade', 'client_id', 'trade_date', JSON_OBJECT(), 1),
    (3, 'default', 'default', 'MarginTrade', 'sample-mysql', 'domain_security_trade', 'client_id', 'trade_date', JSON_OBJECT(), 1),
    (4, 'default', 'default', 'BlockTrade', 'sample-mysql', 'domain_security_trade', 'client_id', 'trade_date', JSON_OBJECT(), 1),
    (5, 'default', 'default', 'FundTransfer', 'sample-mysql', 'domain_fund_transfer', 'client_id', 'transfer_date', JSON_OBJECT(), 1)
ON DUPLICATE KEY UPDATE
    source_name = VALUES(source_name),
    table_name = VALUES(table_name),
    client_id_column = VALUES(client_id_column),
    date_column = VALUES(date_column),
    static_filters_json = VALUES(static_filters_json),
    enabled = VALUES(enabled);

INSERT INTO rule_field_mapping (binding_id, canonical_field, source_column, required)
VALUES
    (1, 'clientId', 'client_id', 1),
    (1, 'clientType', 'client_type', 1),
    (1, 'age', 'age', 0),
    (1, 'occupation', 'occupation', 0),
    (1, 'annualIncome', 'annual_income', 0),
    (1, 'registeredCapital', 'registered_capital', 0),

    (2, 'tradeDate', 'trade_date', 1),
    (2, 'tradeTime', 'trade_time', 0),
    (2, 'clientId', 'client_id', 1),
    (2, 'securityCode', 'security_code', 1),
    (2, 'securityName', 'security_name', 0),
    (2, 'direction', 'direction', 0),
    (2, 'businessFlag', 'business_flag', 0),
    (2, 'businessFlags', 'business_flag', 0),
    (2, 'tradeMode', 'trade_mode', 0),
    (2, 'price', 'price', 0),
    (2, 'quantity', 'quantity', 0),
    (2, 'amount', 'amount', 0),

    (3, 'tradeDate', 'trade_date', 1),
    (3, 'tradeTime', 'trade_time', 0),
    (3, 'clientId', 'client_id', 1),
    (3, 'securityCode', 'security_code', 1),
    (3, 'securityName', 'security_name', 0),
    (3, 'direction', 'direction', 0),
    (3, 'businessFlag', 'business_flag', 0),
    (3, 'businessFlags', 'business_flag', 0),
    (3, 'tradeMode', 'trade_mode', 0),
    (3, 'price', 'price', 0),
    (3, 'quantity', 'quantity', 0),
    (3, 'amount', 'amount', 0),

    (4, 'tradeDate', 'trade_date', 1),
    (4, 'tradeTime', 'trade_time', 0),
    (4, 'clientId', 'client_id', 1),
    (4, 'securityCode', 'security_code', 1),
    (4, 'securityName', 'security_name', 0),
    (4, 'direction', 'direction', 0),
    (4, 'businessFlag', 'business_flag', 0),
    (4, 'businessFlags', 'business_flag', 0),
    (4, 'tradeMode', 'trade_mode', 0),
    (4, 'price', 'price', 0),
    (4, 'quantity', 'quantity', 0),
    (4, 'amount', 'amount', 0),

    (5, 'transferDate', 'transfer_date', 1),
    (5, 'clientId', 'client_id', 1),
    (5, 'direction', 'direction', 0),
    (5, 'amount', 'amount', 0),
    (5, 'bankName', 'bank_name', 0)
ON DUPLICATE KEY UPDATE
    source_column = VALUES(source_column),
    required = VALUES(required);
