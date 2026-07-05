ALTER TABLE domain_customer_profile
    ADD COLUMN account_close_request_date DATE,
    ADD COLUMN empty_to_close_account_days INT;

INSERT INTO rule_field_mapping (binding_id, canonical_field, source_column, required)
VALUES
    (1, 'accountCloseRequestDate', 'account_close_request_date', 0),
    (1, 'emptyToCloseAccountDays', 'empty_to_close_account_days', 0)
ON DUPLICATE KEY UPDATE
    source_column = VALUES(source_column),
    required = VALUES(required);

INSERT INTO rule_verification_data_requirement
    (verification_item_id, requirement_code, domain, scope_type, grain, filters_json, fields_json, sample_limit, missing_data_policy)
VALUES
    (1005020303, 'REQ_1005_CLOSE_ACCOUNT_PROFILE', 'CustomerProfile', 'ALERT_DETAIL', 'ACCOUNT_SUMMARY',
     JSON_OBJECT(), JSON_ARRAY('clientId', 'clientType', 'accountCloseRequestDate', 'emptyToCloseAccountDays'), 1, 'RETURN_EMPTY_WITH_REASON')
ON DUPLICATE KEY UPDATE
    domain = VALUES(domain),
    scope_type = VALUES(scope_type),
    grain = VALUES(grain),
    filters_json = VALUES(filters_json),
    fields_json = VALUES(fields_json),
    sample_limit = VALUES(sample_limit),
    missing_data_policy = VALUES(missing_data_policy);

UPDATE domain_customer_profile
SET account_close_request_date = '2026-07-10',
    empty_to_close_account_days = 20
WHERE client_id = 'C0002';
