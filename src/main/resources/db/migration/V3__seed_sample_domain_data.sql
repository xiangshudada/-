INSERT INTO domain_customer_profile
    (client_id, client_type, age, occupation, annual_income, registered_capital)
VALUES
    ('C0001', 'PERSON', 72, '退休人员', 80000.00, NULL),
    ('C0002', 'PERSON', 35, '企业管理人员', 500000.00, NULL)
ON DUPLICATE KEY UPDATE age = VALUES(age), occupation = VALUES(occupation), annual_income = VALUES(annual_income);

INSERT INTO domain_security_trade
    (trade_date, trade_time, client_id, security_code, security_name, direction, business_flag, trade_mode, price, quantity, amount)
VALUES
    ('2026-06-12', '10:12:00', 'C0001', '600000', '浦发银行', 'BUY', 4211, 'NORMAL', 10.200000, 5000, 51000.00),
    ('2026-06-13', '14:20:00', 'C0001', '600000', '浦发银行', 'SELL', 4212, 'NORMAL', 10.500000, 5000, 52500.00),
    ('2026-06-18', '09:45:00', 'C0001', '000001', '平安银行', 'BUY', 4213, 'NORMAL', 12.100000, 7000, 84700.00),
    ('2026-06-20', '11:30:00', 'C0002', '300001', '特锐德', 'BUY', 1001, 'BLOCK', 15.800000, 100000, 1580000.00),
    ('2026-06-21', '13:02:00', 'C0002', '300001', '特锐德', 'BUY', 1001, 'NORMAL', 16.200000, 20000, 324000.00);

INSERT INTO domain_fund_transfer
    (transfer_date, client_id, direction, amount, bank_name)
VALUES
    ('2026-06-01', 'C0001', 'IN', 200000.00, '招商银行'),
    ('2026-06-25', 'C0001', 'OUT', 150000.00, '招商银行');
