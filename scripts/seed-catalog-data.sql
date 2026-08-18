-- Seeds the catalog tables with the same data as lavander's frontend mock
-- data (src/app/mock-data/mock-catalog-data.ts). IDs are kept identical to
-- the mock data for easy cross-reference.
--
-- Usage:
--   PGPASSWORD=parola123 psql -h localhost -p 5433 -U postgres -d lavander -f scripts/seed-catalog-data.sql
--
-- Safe to re-run: truncates all catalog tables first.

BEGIN;

TRUNCATE TABLE
  variant_tag,
  promotion_group_tag,
  promotion_group,
  tag,
  property_value,
  product_variant,
  product_extra_properties,
  category_properties,
  product,
  product_category,
  product_sub_category_group,
  product_category_group,
  property_definition
RESTART IDENTITY CASCADE;

-- Property definitions
INSERT INTO property_definition (id, property_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'RAM'),
  (2, 'Screen Size'),
  (3, 'Chip'),
  (4, 'Tip'),
  (5, 'Cantitate'),
  (6, 'Parfum'),
  (7, 'Cantitate'),
  (8, 'Tip'),
  (9, 'Parfum'),
  (10, 'Tip Produs'),
  (11, 'Cantitate'),
  (12, 'Numar Role'),
  (13, 'Numar Straturi'),
  (14, 'Tip Produs'),
  (15, 'Cantitate');

-- Top-level category groups
INSERT INTO product_category_group (id, group_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Electronics'),
  (4, 'Curatenie'),
  (5, 'Igiena si Cosmetice');

-- Subgroups (Computers is the only subgroup in the mock data)
INSERT INTO product_sub_category_group (id, group_name, parent_group_id) OVERRIDING SYSTEM VALUE VALUES
  (2, 'Computers', 1);

-- Categories: Laptops attaches to the Computers subgroup; the rest attach
-- directly to a top-level group.
INSERT INTO product_category (id, category_name, parent_group_id, parent_subgroup_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Laptops', NULL, 2),
  (2, 'Detergenti', 4, NULL),
  (3, 'Balsam Rufe', 4, NULL),
  (4, 'Odorizant WC', 4, NULL),
  (5, 'Igiena Orala', 5, NULL),
  (6, 'Hartie Igienica', 5, NULL),
  (7, 'Cosmetice', 5, NULL);

-- Category <-> property definition (many-to-many)
INSERT INTO category_properties (category_id, property_definition_id) VALUES
  (1, 1),  -- Laptops: RAM
  (1, 2),  -- Laptops: Screen Size
  (2, 4),  -- Detergenti: Tip
  (2, 5),  -- Detergenti: Cantitate
  (3, 6),  -- Balsam Rufe: Parfum
  (3, 7),  -- Balsam Rufe: Cantitate
  (4, 8),  -- Odorizant WC: Tip
  (4, 9),  -- Odorizant WC: Parfum
  (5, 10), -- Igiena Orala: Tip Produs
  (5, 11), -- Igiena Orala: Cantitate
  (6, 12), -- Hartie Igienica: Numar Role
  (6, 13), -- Hartie Igienica: Numar Straturi
  (7, 14), -- Cosmetice: Tip Produs
  (7, 15); -- Cosmetice: Cantitate

-- Products
INSERT INTO product (id, product_name, product_description, product_category_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Dell', 'Dell laptops', 1),
  (2, 'Apple', 'Apple laptops', 1),
  (3, 'Lenovo', 'Lenovo laptops', 1),
  (4, 'Ariel', 'Ariel detergenti', 2),
  (5, 'Lenor', 'Lenor balsam de rufe', 3),
  (6, 'Domestos', 'Domestos odorizante WC', 4),
  (7, 'Blend-a-med', 'Blend-a-med produse de igiena orala', 5),
  (8, 'Alint', 'Alint hartie igienica', 6),
  (9, 'Pantene', 'Pantene produse cosmetice', 7);

-- Product <-> extra property definition (many-to-many). Only Apple has an
-- extra property (Chip) in the mock data.
INSERT INTO product_extra_properties (product_id, property_definition_id) VALUES
  (2, 3);

-- Product variants
INSERT INTO product_variant (id, variant_name, variant_description, product_id, price, star_rating) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Dell XPS 13', '13-inch Dell XPS laptop', 1, 4999, 4),
  (2, 'MacBook Pro 14', '14-inch MacBook Pro', 2, 9999, 5),
  (3, 'Lenovo ThinkPad X1', '14-inch Lenovo ThinkPad X1 Carbon', 3, 6499, 4),
  (4, 'Ariel Detergent Lichid Alpine', 'Detergent lichid pentru rufe Ariel', 4, 65, 4),
  (5, 'Lenor Perle Parfumate Spring Awakening', 'Balsam de rufe Lenor', 5, 25, 5),
  (6, 'Domestos Pine Fresh', 'Dezinfectant si odorizant WC Domestos', 6, 15, 4),
  (7, 'Blend-a-med 3D White Clinical Miracle Glow', 'Pasta de dinti Blend-a-med', 7, 12, 5),
  (8, 'Alint Hartie Igienica Piersica', 'Hartie igienica Alint', 8, 10, 3),
  (9, 'Pantene Pro-V Miracles Lift & Volume', 'Sampon Pantene', 9, 22, 4),
  (10, 'MacBook Pro 16 (24GB)', '16-inch MacBook Pro, 24GB RAM', 2, 14999, 5),
  (11, 'Ariel Detergent Lichid Alpine XXL', 'Detergent lichid pentru rufe Ariel, format XXL', 4, 110, 4),
  (12, 'MacBook Pro 16', '16-inch MacBook Pro', 2, 12999, 5);

-- Variant property values
INSERT INTO property_value (id, variant_id, property_definition_id, property_value) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 1, '16GB'),
  (2, 1, 2, '13 inch'),
  (3, 2, 1, '18GB'),
  (4, 2, 2, '14 inch'),
  (5, 2, 3, 'M3 Pro'),
  (6, 3, 1, '16GB'),
  (7, 3, 2, '14 inch'),
  (8, 4, 4, 'Lichid'),
  (9, 4, 5, '3.6L'),
  (10, 5, 6, 'Spring Awakening'),
  (11, 5, 7, '140 g'),
  (12, 6, 8, 'Gel'),
  (13, 6, 9, 'Pine Fresh'),
  (14, 7, 10, 'Pasta de Dinti'),
  (15, 7, 11, '75ml'),
  (16, 8, 12, '8 Role'),
  (17, 8, 13, '3 Straturi'),
  (18, 9, 14, 'Sampon'),
  (19, 9, 15, '300 ml'),
  (20, 10, 1, '24GB'),
  (21, 10, 2, '16 inch'),
  (22, 10, 3, 'M3 Pro'),
  (23, 11, 4, 'Lichid'),
  (24, 11, 5, '5.5L'),
  (25, 12, 1, '18GB'),
  (26, 12, 2, '16 inch'),
  (27, 12, 3, 'M3 Pro');

-- Tags
INSERT INTO tag (id, tag_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Produs sub 20 Lei');

-- Promotion groups
INSERT INTO promotion_group (id, group_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Produse sub 20 Lei');

-- Promotion group <-> tag (many-to-many)
INSERT INTO promotion_group_tag (promotion_group_id, tag_id) VALUES
  (1, 1);

-- Variant <-> tag (many-to-many) — deliberately cross-category (Curatenie + Igiena)
-- to prove a promotion pools variants that don't share a real category.
INSERT INTO variant_tag (variant_id, tag_id) VALUES
  (6, 1),  -- Domestos Pine Fresh, 15 Lei
  (8, 1);  -- Alint Hartie Igienica Piersica, 10 Lei

-- Re-align each identity sequence with the max explicit id inserted above,
-- so the next application-generated insert doesn't collide.
SELECT setval(pg_get_serial_sequence('property_definition', 'id'), (SELECT MAX(id) FROM property_definition));
SELECT setval(pg_get_serial_sequence('product_category_group', 'id'), (SELECT MAX(id) FROM product_category_group));
SELECT setval(pg_get_serial_sequence('product_sub_category_group', 'id'), (SELECT MAX(id) FROM product_sub_category_group));
SELECT setval(pg_get_serial_sequence('product_category', 'id'), (SELECT MAX(id) FROM product_category));
SELECT setval(pg_get_serial_sequence('product', 'id'), (SELECT MAX(id) FROM product));
SELECT setval(pg_get_serial_sequence('product_variant', 'id'), (SELECT MAX(id) FROM product_variant));
SELECT setval(pg_get_serial_sequence('property_value', 'id'), (SELECT MAX(id) FROM property_value));
SELECT setval(pg_get_serial_sequence('tag', 'id'), (SELECT MAX(id) FROM tag));
SELECT setval(pg_get_serial_sequence('promotion_group', 'id'), (SELECT MAX(id) FROM promotion_group));

COMMIT;
