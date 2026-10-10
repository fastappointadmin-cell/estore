-- Seeds the catalog with a clean category taxonomy (10 top-level groups, 23
-- subgroups, 82 leaf categories) and nothing else — no properties, products,
-- variants, tags, or promotion groups. Those get built up through the admin
-- UI once this structure is in place.
--
-- Usage:
--   PGPASSWORD=parola123 psql -h localhost -p 5433 -U postgres -d lavander -f scripts/seed-catalog-data.sql
--
-- Safe to re-run: truncates all catalog tables first.

BEGIN;

TRUNCATE TABLE
  review,
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

-- Top-level category groups
INSERT INTO product_category_group (id, group_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Detergenti si Curatenie'),
  (2, 'Ingrijire personala'),
  (3, 'Igiena orala'),
  (4, 'Igiena feminina si intima'),
  (5, 'Bebelusi si Copii'),
  (6, 'Hartie si Servetele'),
  (7, 'Odorizante'),
  (8, 'Casa si Gospodarie'),
  (9, 'Cosmetice si Parfumuri'),
  (10, 'Incontinenta si Ingrijire adulti');

-- Subgroups
INSERT INTO product_sub_category_group (id, group_name, parent_group_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Detergenti de rufe', 1),
  (2, 'Detergenti de vase', 1),
  (3, 'Solutii de curatare', 1),
  (4, 'Anticalcar', 1),
  (5, 'Dezinfectare si inalbire', 1),
  (6, 'Insecte si daunatori', 1),
  (7, 'Ingrijirea parului', 2),
  (8, 'Ingrijirea corpului', 2),
  (9, 'Ingrijirea fetei si a pielii', 2),
  (10, 'Ras si barbierit', 2),
  (11, 'Igiena feminina', 4),
  (12, 'Produse intime', 4),
  (13, 'Demachiere si ingrijire', 4),
  (14, 'Scutece si ingrijire', 5),
  (15, 'Odorizante pentru casa', 7),
  (16, 'Odorizante auto', 7),
  (17, 'Bureti si accesorii', 8),
  (18, 'Intretinerea hainelor', 8),
  (19, 'Ingrijirea incaltamintei', 8),
  (20, 'Consumabile si accesorii', 8),
  (21, 'Machiaj si manichiura', 9),
  (22, 'Parfumuri', 9),
  (23, 'Cadouri', 9);

-- Categories. parent_group_id is set only for categories attached directly to
-- a group (no subgroup); parent_subgroup_id is set for the rest.
INSERT INTO product_category (id, category_name, parent_group_id, parent_subgroup_id) OVERRIDING SYSTEM VALUE VALUES
  -- Detergenti de rufe (subgroup 1)
  (1, 'Detergent de rufe automat', NULL, 1),
  (2, 'Detergent de rufe capsule', NULL, 1),
  (3, 'Detergent de rufe lichid', NULL, 1),
  (4, 'Detergent de rufe manual', NULL, 1),
  (5, 'Sapun de rufe', NULL, 1),
  (6, 'Balsam de rufe', NULL, 1),
  (7, 'Apret', NULL, 1),
  -- Detergenti de vase (subgroup 2)
  (8, 'Detergent de vase', NULL, 2),
  -- Solutii de curatare (subgroup 3)
  (9, 'Solutie curatare aragaz', NULL, 3),
  (10, 'Solutie curatare baie', NULL, 3),
  (11, 'Solutie curatare bucatarie', NULL, 3),
  (12, 'Solutie curatare covoare', NULL, 3),
  (13, 'Solutie pentru desfundat', NULL, 3),
  (14, 'Solutie pentru geamuri', NULL, 3),
  (15, 'Solutie pentru lemn', NULL, 3),
  (16, 'Solutie pentru mobila', NULL, 3),
  (17, 'Solutie pentru pardoseli', NULL, 3),
  (18, 'Solutie pentru scos pete', NULL, 3),
  (19, 'Solutie universala', NULL, 3),
  (20, 'Solutie pentru WC', NULL, 3),
  -- Anticalcar (subgroup 4)
  (21, 'Anticalcar gel', NULL, 4),
  (22, 'Anticalcar lichid', NULL, 4),
  (23, 'Anticalcar pudra', NULL, 4),
  (24, 'Anticalcar tablete', NULL, 4),
  -- Dezinfectare si inalbire (subgroup 5)
  (25, 'Inalbitor', NULL, 5),
  (26, 'Dezinfectant masina de spalat', NULL, 5),
  -- Insecte si daunatori (subgroup 6)
  (27, 'Insecticide', NULL, 6),
  (28, 'Repelent', NULL, 6),
  -- Ingrijirea parului (subgroup 7)
  (29, 'Sampon', NULL, 7),
  (30, 'Balsam de par', NULL, 7),
  (31, 'Fixativ de par', NULL, 7),
  (32, 'Gel de par', NULL, 7),
  (33, 'Spuma de par', NULL, 7),
  (34, 'Tratamente pentru par', NULL, 7),
  (35, 'Vopsea pentru par', NULL, 7),
  -- Ingrijirea corpului (subgroup 8)
  (36, 'Gel de dus', NULL, 8),
  (37, 'Sapun', NULL, 8),
  (38, 'Sapun lichid', NULL, 8),
  (39, 'Deodorante', NULL, 8),
  (40, 'Crema de corp', NULL, 8),
  (41, 'Crema de maini', NULL, 8),
  (42, 'Balsam de buze', NULL, 8),
  (43, 'Sare de baie', NULL, 8),
  (44, 'Lotiune de plaja', NULL, 8),
  -- Ingrijirea fetei si a pielii (subgroup 9)
  (45, 'Crema de fata', NULL, 9),
  (46, 'Tratamente pentru piele', NULL, 9),
  -- Ras si barbierit (subgroup 10)
  (47, 'After Shave', NULL, 10),
  (48, 'Aparat de ras', NULL, 10),
  (49, 'Crema de ras', NULL, 10),
  (50, 'Spuma de ras', NULL, 10),
  -- Igiena orala (group 3, no subgroup)
  (51, 'Apa de gura', 3, NULL),
  (52, 'Pasta de dinti', 3, NULL),
  (53, 'Periuta de dinti', 3, NULL),
  -- Igiena feminina (subgroup 11)
  (54, 'Absorbante', NULL, 11),
  (55, 'Tampoane', NULL, 11),
  -- Produse intime (subgroup 12)
  (56, 'Prezervative', NULL, 12),
  -- Demachiere si ingrijire (subgroup 13)
  (57, 'Dischete demachiante', NULL, 13),
  (58, 'Servetele demachiante', NULL, 13),
  (59, 'Vata', NULL, 13),
  -- Scutece si ingrijire (subgroup 14)
  (60, 'Cosmetice Baby', NULL, 14),
  (61, 'Scutece pentru copii', NULL, 14),
  -- Hartie si Servetele (group 6, no subgroup)
  (62, 'Hartie igienica', 6, NULL),
  (63, 'Prosoape de bucatarie', 6, NULL),
  (64, 'Servetele nazale', 6, NULL),
  (65, 'Servetele umede', 6, NULL),
  -- Odorizante pentru casa (subgroup 15)
  (66, 'Odorizante camera', NULL, 15),
  (67, 'Odorizante baie', NULL, 15),
  (68, 'Odorizante dulap', NULL, 15),
  (69, 'Odorizante frigider', NULL, 15),
  (70, 'Odorizante WC', NULL, 15),
  -- Odorizante auto (subgroup 16)
  (71, 'Odorizante auto', NULL, 16),
  -- Bureti si accesorii (subgroup 17)
  (72, 'Burete de vase', NULL, 17),
  (73, 'Bureti de baie', NULL, 17),
  -- Intretinerea hainelor (subgroup 18)
  (74, 'Apa pentru fier de calcat', NULL, 18),
  -- Ingrijirea incaltamintei (subgroup 19)
  (75, 'Crema de ghete', NULL, 19),
  -- Consumabile si accesorii (subgroup 20)
  (76, 'Pungi', NULL, 20),
  (77, 'Baterii', NULL, 20),
  -- Machiaj si manichiura (subgroup 21)
  (78, 'Machiaj', NULL, 21),
  (79, 'Acetona', NULL, 21),
  -- Parfumuri (subgroup 22)
  (80, 'Apa de toaleta', NULL, 22),
  -- Cadouri (subgroup 23)
  (81, 'Casete cadou', NULL, 23),
  -- Incontinenta si Ingrijire adulti (group 10, no subgroup)
  (82, 'Scutece pentru adulti', 10, NULL);

-- Re-align each identity sequence with the max explicit id inserted above,
-- so the next application-generated insert doesn't collide. Every other
-- catalog table is left empty by the TRUNCATE ... RESTART IDENTITY above,
-- so their sequences are already correctly reset to 1 with nothing to realign.
SELECT setval(pg_get_serial_sequence('product_category_group', 'id'), (SELECT MAX(id) FROM product_category_group));
SELECT setval(pg_get_serial_sequence('product_sub_category_group', 'id'), (SELECT MAX(id) FROM product_sub_category_group));
SELECT setval(pg_get_serial_sequence('product_category', 'id'), (SELECT MAX(id) FROM product_category));

COMMIT;
