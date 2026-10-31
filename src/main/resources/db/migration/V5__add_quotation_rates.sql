-- =====================================================================
-- ObraIA - V5: data for the quotation (block 6)
-- Each city gets the cost of bringing the materials to the site, as a
-- percentage of their price. The budget layers gain the types "waste"
-- and "transport", so every layer of the quotation is a row of its own.
-- =====================================================================

ALTER TABLE locations
    ADD COLUMN transport_pct numeric(5,2) NOT NULL DEFAULT 4.00,
    ADD CONSTRAINT ck_locations_transport_pct CHECK (transport_pct BETWEEN 0 AND 100);

-- Approximate values: bigger cities have more suppliers close to the site.
UPDATE locations SET transport_pct = r.pct
FROM (VALUES
    ('Bogotá',        3.00),
    ('Medellín',      3.00),
    ('Cali',          3.00),
    ('Barranquilla',  3.50),
    ('Cartagena',     4.50),
    ('Bucaramanga',   4.00),
    ('Pereira',       4.00),
    ('Manizales',     4.50),
    ('Ibagué',        4.50),
    ('Villavicencio', 5.00),
    ('Pasto',         6.00),
    ('Tunja',         5.00)
) AS r(city, pct)
WHERE locations.city = r.city;

ALTER TABLE budget_items DROP CONSTRAINT ck_budget_items_type;
ALTER TABLE budget_items ADD CONSTRAINT ck_budget_items_type CHECK (item_type IN
    ('materials', 'waste', 'transport', 'labor', 'equipment', 'tax', 'contingency', 'other'));
