INSERT INTO users (name, email, phone, password_hash, profile)
SELECT 'Test Client',
       'test.client@example.test',
       '555-0100',
       'DEV-SEED-NOT-A-LOGIN-HASH',
       'CLIENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'test.client@example.test'
);

INSERT INTO users (name, email, phone, password_hash, profile)
SELECT 'Test Client 2',
       'test.client2@example.test',
       '555-0101',
       'DEV-SEED-NOT-A-LOGIN-HASH2',
       'CLIENT'
    WHERE NOT EXISTS (
    SELECT 2
    FROM users
    WHERE email = 'test.client2@example.test'
);

INSERT INTO salon_services (name, description, duration_minutes, price, active)
SELECT 'Corte de cabelo', 'Serviço de corte de cabelo', 45, 35.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM salon_services
    WHERE name = 'Corte de cabelo'
);

INSERT INTO salon_services (name, description, duration_minutes, price, active)
SELECT 'Coloração', 'Serviço de coloração de cabelo', 120, 120.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM salon_services
    WHERE name = 'Coloração'
);

INSERT INTO salon_services (name, description, duration_minutes, price, active)
SELECT 'Escova', 'Serviço de escova de cabelo', 40, 25.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM salon_services
    WHERE name = 'Escova'
);