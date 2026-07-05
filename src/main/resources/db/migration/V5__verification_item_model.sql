CREATE TABLE IF NOT EXISTS rule_verification_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    item_code VARCHAR(128) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_order INT NOT NULL,
    method_text TEXT,
    rule_text TEXT,
    operator_type VARCHAR(64) NOT NULL,
    applicable_client_types VARCHAR(128) NOT NULL DEFAULT 'PERSON,ORG,PRODUCT',
    thresholds_json JSON,
    judgment_basis TEXT,
    manual_review_required TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_rule_verification_item (step_id, item_code),
    CONSTRAINT fk_verification_item_step
        FOREIGN KEY (step_id) REFERENCES rule_investigation_step(id)
);

CREATE TABLE IF NOT EXISTS rule_verification_data_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    verification_item_id BIGINT NOT NULL,
    requirement_code VARCHAR(128) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    grain VARCHAR(64) NOT NULL,
    filters_json JSON,
    fields_json JSON,
    sample_limit INT NOT NULL DEFAULT 20,
    missing_data_policy VARCHAR(64) NOT NULL DEFAULT 'RETURN_EMPTY_WITH_REASON',
    UNIQUE KEY uk_verification_requirement (verification_item_id, requirement_code),
    CONSTRAINT fk_requirement_verification_item
        FOREIGN KEY (verification_item_id) REFERENCES rule_verification_item(id)
);
