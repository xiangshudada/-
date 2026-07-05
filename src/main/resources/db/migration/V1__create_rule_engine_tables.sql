CREATE TABLE IF NOT EXISTS rule_indicator_set (
    indicator_code VARCHAR(64) PRIMARY KEY,
    indicator_name VARCHAR(255) NOT NULL,
    scenario VARCHAR(128) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rule_risk_section (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_code VARCHAR(64) NOT NULL,
    risk_type VARCHAR(128) NOT NULL,
    crime_type VARCHAR(255),
    risk_point TEXT,
    section_order INT NOT NULL,
    CONSTRAINT fk_risk_section_indicator
        FOREIGN KEY (indicator_code) REFERENCES rule_indicator_set(indicator_code)
);

CREATE TABLE IF NOT EXISTS rule_investigation_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    section_id BIGINT NOT NULL,
    step_code VARCHAR(64) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    step_order INT NOT NULL,
    method_text TEXT,
    rule_text TEXT,
    operator_type VARCHAR(64) NOT NULL,
    applicable_client_types VARCHAR(128) NOT NULL DEFAULT 'PERSON,ORG,PRODUCT',
    thresholds_json JSON,
    judgment_basis TEXT,
    manual_review_required TINYINT NOT NULL DEFAULT 0,
    report_field VARCHAR(255),
    result_target VARCHAR(255),
    CONSTRAINT fk_step_section
        FOREIGN KEY (section_id) REFERENCES rule_risk_section(id)
);

CREATE TABLE IF NOT EXISTS rule_data_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    requirement_code VARCHAR(128) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    grain VARCHAR(64) NOT NULL,
    filters_json JSON,
    fields_json JSON,
    sample_limit INT NOT NULL DEFAULT 20,
    missing_data_policy VARCHAR(64) NOT NULL DEFAULT 'RETURN_EMPTY_WITH_REASON',
    CONSTRAINT fk_requirement_step
        FOREIGN KEY (step_id) REFERENCES rule_investigation_step(id)
);

CREATE TABLE IF NOT EXISTS domain_customer_profile (
    client_id VARCHAR(64) PRIMARY KEY,
    client_type VARCHAR(32) NOT NULL,
    age INT,
    occupation VARCHAR(255),
    annual_income DECIMAL(20, 2),
    registered_capital DECIMAL(20, 2)
);

CREATE TABLE IF NOT EXISTS domain_security_trade (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    trade_time TIME,
    client_id VARCHAR(64) NOT NULL,
    security_code VARCHAR(32) NOT NULL,
    security_name VARCHAR(255),
    direction VARCHAR(32),
    business_flag INT,
    trade_mode VARCHAR(64),
    price DECIMAL(20, 6),
    quantity DECIMAL(20, 2),
    amount DECIMAL(20, 2),
    INDEX idx_security_trade_client_date (client_id, trade_date),
    INDEX idx_security_trade_flag (business_flag)
);

CREATE TABLE IF NOT EXISTS domain_fund_transfer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_date DATE NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount DECIMAL(20, 2) NOT NULL,
    bank_name VARCHAR(255),
    INDEX idx_fund_transfer_client_date (client_id, transfer_date)
);
