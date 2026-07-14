-- Seed data for the live demo (Render free-tier Postgres).
-- Run this after the app has started at least once (so Hibernate has created the schema via ddl-auto=update).
-- Usage: psql "$DATABASE_URL" -f docker/seed-demo-data.sql
--
-- Demo credentials:
--   admin  -> email: admin@demo.com  / password: Demo@Admin123
--   client -> email: client@demo.com / password: Demo@Client123
--   owner  -> email: ericklira99@gmail.com / password: 123456789Er@

INSERT INTO users (username, email, password, full_name, user_status, user_type, phone_number, cpf, image_url, creation_date, last_update_date)
VALUES
    ('demo_admin', 'admin@demo.com', '$2a$10$hcrYywnjm1opaPmPeOD6P.NMnNv51KNn1Or/zmOtTW2BftCMGEbxO', 'Demo Admin', 'ACTIVE', 'ADMIN', NULL, NULL, NULL, now(), now()),
    ('demo_client', 'client@demo.com', '$2a$10$errORthsSDTHrVROQeHRIuvNQhfxUSJdA9FqZO3z7fOgJkCdtsiaC', 'Demo Client', 'ACTIVE', 'CLIENT', NULL, NULL, NULL, now(), now()),
    ('ericklira99', 'ericklira99@gmail.com', '$2a$10$kCiyeNpjltj271oqveQ9COdUw1LVO3WiwpIN2kcPQlM4vH93hg57.', 'Erick Soares', 'ACTIVE', 'ADMIN', NULL, NULL, NULL, now(), now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO products (name, price, stock, description, creation_date, last_update_date)
VALUES
    ('Tattoo Ink - Black', 45.90, 100, 'Professional black tattoo ink, 30ml bottle', now(), now()),
    ('Disposable Needles - Round Liner 5RL', 12.50, 200, 'Box of 50 sterile disposable round liner needles', now(), now()),
    ('Tattoo Machine - Rotary Pro', 389.00, 15, 'Lightweight rotary tattoo machine, adjustable stroke', now(), now()),
    ('Nitrile Gloves (Box of 100)', 22.00, 80, 'Powder-free nitrile gloves, size M', now(), now()),
    ('Stencil Transfer Paper (A4, 100 sheets)', 34.90, 50, 'Thermal transfer paper for stencil application', now(), now())
ON CONFLICT DO NOTHING;
