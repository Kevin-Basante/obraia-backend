-- =====================================================================
-- ObraIA · V3: seed data (catalogs the application needs in order to
-- work). Data values may be in Spanish; technical names may not.
-- Prices and formulas are APPROXIMATE REFERENCE VALUES (COP, VAT
-- included). To change them, create a new migration; do not edit this one.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles and permissions
-- ---------------------------------------------------------------------
INSERT INTO roles (name, scope, description) VALUES
    ('user',          'platform', 'Cuenta registrada con correo verificado'),
    ('system_admin',  'platform', 'Mantiene catálogos, plantillas y usuarios'),
    ('owner',         'project',  'Propietario: persona natural que construye en su terreno'),
    ('company_admin', 'project',  'Administrador de una obra de empresa');

INSERT INTO permissions (code, description) VALUES
    ('project.create',        'Crear obras'),
    ('project.read',          'Ver una obra y sus módulos'),
    ('project.update',        'Editar los datos de una obra'),
    ('project.delete',        'Solicitar el borrado de una obra'),
    ('member.manage',         'Agregar o quitar usuarios de una obra'),
    ('site_manager.manage',   'Registrar y asignar maestros de obra'),
    ('blueprint.upload',      'Subir planos'),
    ('measurement.manage',    'Ingresar y corregir medidas'),
    ('budget.generate',       'Generar o recalcular presupuestos'),
    ('budget.approve',        'Aprobar una versión del presupuesto'),
    ('expense.register',      'Registrar gastos'),
    ('progress.report',       'Registrar reportes de avance'),
    ('risk.read',             'Consultar el riesgo de retraso'),
    ('catalog.manage',        'Editar materiales, precios y fórmulas'),
    ('phase_template.manage', 'Editar las plantillas de fases'),
    ('user.manage',           'Activar o desactivar cuentas');

-- user: can create projects. system_admin: catalogs and accounts.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'project.create'
WHERE r.name = 'user';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('catalog.manage', 'phase_template.manage', 'user.manage')
WHERE r.name = 'system_admin';

-- owner and company_admin: every permission within their project.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
    'project.read', 'project.update', 'project.delete', 'member.manage',
    'site_manager.manage', 'blueprint.upload', 'measurement.manage',
    'budget.generate', 'budget.approve', 'expense.register',
    'progress.report', 'risk.read')
WHERE r.name IN ('owner', 'company_admin');

-- ---------------------------------------------------------------------
-- Project types (2 floors maximum)
-- ---------------------------------------------------------------------
INSERT INTO project_types (name, description) VALUES
    ('Vivienda unifamiliar', 'Casa de 1 o 2 pisos construida sobre lote propio'),
    ('Local comercial',      'Local de 1 o 2 pisos para comercio o bodega pequeña');

-- ---------------------------------------------------------------------
-- Cities
-- ---------------------------------------------------------------------
INSERT INTO locations (city, department) VALUES
    ('Bogotá',        'Bogotá D.C.'),
    ('Medellín',      'Antioquia'),
    ('Cali',          'Valle del Cauca'),
    ('Barranquilla',  'Atlántico'),
    ('Cartagena',     'Bolívar'),
    ('Bucaramanga',   'Santander'),
    ('Pereira',       'Risaralda'),
    ('Manizales',     'Caldas'),
    ('Ibagué',        'Tolima'),
    ('Villavicencio', 'Meta'),
    ('Pasto',         'Nariño'),
    ('Tunja',         'Boyacá');

-- ---------------------------------------------------------------------
-- Phase templates (Template Method). Duration in calendar days.
-- Small project: up to 120 m2. Large project: more than 120 m2.
-- ---------------------------------------------------------------------
INSERT INTO phase_templates (project_type_id, size_category, name, sequence_order, estimated_duration_days)
SELECT pt.id, t.size_category, t.name, t.sequence_order, t.days
FROM project_types pt
CROSS JOIN (VALUES
    ('small', 'Preliminares y replanteo',              1,  5),
    ('small', 'Excavación y movimiento de tierras',    2,  7),
    ('small', 'Cimentación',                           3, 14),
    ('small', 'Estructura (columnas, vigas y placas)', 4, 30),
    ('small', 'Mampostería',                           5, 21),
    ('small', 'Cubierta',                              6, 10),
    ('small', 'Instalaciones hidrosanitarias y eléctricas', 7, 14),
    ('small', 'Pañetes, pisos y acabados',             8, 30),
    ('large', 'Preliminares y replanteo',              1,  7),
    ('large', 'Excavación y movimiento de tierras',    2, 12),
    ('large', 'Cimentación',                           3, 21),
    ('large', 'Estructura (columnas, vigas y placas)', 4, 50),
    ('large', 'Mampostería',                           5, 35),
    ('large', 'Cubierta',                              6, 15),
    ('large', 'Instalaciones hidrosanitarias y eléctricas', 7, 21),
    ('large', 'Pañetes, pisos y acabados',             8, 45)
) AS t(size_category, name, sequence_order, days);

-- ---------------------------------------------------------------------
-- Material catalog: approximate reference price (COP, VAT incl.)
-- TODO: validate with a local hardware store before the final presentation.
-- ---------------------------------------------------------------------
INSERT INTO material_catalog (name, category, unit, reference_price, price_updated_at) VALUES
    ('Cemento gris uso general',         'cement_concrete', 'bulto 50 kg',  33000, DATE '2026-10-04'),
    ('Arena de pega',                    'aggregates',      'm3',           90000, DATE '2026-10-04'),
    ('Arena de río (concreto)',          'aggregates',      'm3',          100000, DATE '2026-10-04'),
    ('Gravilla',                         'aggregates',      'm3',          115000, DATE '2026-10-04'),
    ('Bloque de arcilla No. 5',          'masonry',         'unidad',        1600, DATE '2026-10-04'),
    ('Varilla corrugada 1/2" x 6 m',     'steel',           'unidad',       32000, DATE '2026-10-04'),
    ('Varilla corrugada 3/8" x 6 m',     'steel',           'unidad',       18000, DATE '2026-10-04'),
    ('Alambre negro calibre 18',         'steel',           'kg',            9000, DATE '2026-10-04'),
    ('Teja de fibrocemento No. 6',       'roofing',         'unidad',       48000, DATE '2026-10-04'),
    ('Pintura vinilo tipo 2',            'finishes',        'galón',        60000, DATE '2026-10-04');

-- ---------------------------------------------------------------------
-- Consumption formulas (Strategy). Approximate values per base unit:
--   concrete (foundation, columns, beams, slabs) per m3
--   walls per m2 · floor base per m2 · roof per m2
-- Loaded the same way for both project types.
-- ---------------------------------------------------------------------
INSERT INTO material_formulas (project_type_id, material_id, element_type, consumption_per_unit, base_unit, waste_factor)
SELECT pt.id, mc.id, f.element_type, f.consumption, f.base_unit, f.waste
FROM project_types pt
CROSS JOIN (VALUES
    -- 3000 psi concrete mixed on site
    ('foundation', 'Cemento gris uso general',      7.0000, 'm3', 0.05),
    ('foundation', 'Arena de río (concreto)',       0.5500, 'm3', 0.05),
    ('foundation', 'Gravilla',                      0.8000, 'm3', 0.05),
    ('foundation', 'Varilla corrugada 1/2" x 6 m',  6.0000, 'm3', 0.05),
    ('foundation', 'Varilla corrugada 3/8" x 6 m',  4.0000, 'm3', 0.05),
    ('foundation', 'Alambre negro calibre 18',      1.0000, 'm3', 0.10),
    ('column',     'Cemento gris uso general',      7.0000, 'm3', 0.05),
    ('column',     'Arena de río (concreto)',       0.5500, 'm3', 0.05),
    ('column',     'Gravilla',                      0.8000, 'm3', 0.05),
    ('column',     'Varilla corrugada 1/2" x 6 m', 16.0000, 'm3', 0.05),
    ('column',     'Varilla corrugada 3/8" x 6 m', 12.0000, 'm3', 0.05),
    ('column',     'Alambre negro calibre 18',      2.0000, 'm3', 0.10),
    ('beam',       'Cemento gris uso general',      7.0000, 'm3', 0.05),
    ('beam',       'Arena de río (concreto)',       0.5500, 'm3', 0.05),
    ('beam',       'Gravilla',                      0.8000, 'm3', 0.05),
    ('beam',       'Varilla corrugada 1/2" x 6 m', 14.0000, 'm3', 0.05),
    ('beam',       'Varilla corrugada 3/8" x 6 m', 10.0000, 'm3', 0.05),
    ('beam',       'Alambre negro calibre 18',      2.0000, 'm3', 0.10),
    ('slab',       'Cemento gris uso general',      7.0000, 'm3', 0.05),
    ('slab',       'Arena de río (concreto)',       0.5500, 'm3', 0.05),
    ('slab',       'Gravilla',                      0.8000, 'm3', 0.05),
    ('slab',       'Varilla corrugada 3/8" x 6 m', 10.0000, 'm3', 0.05),
    ('slab',       'Alambre negro calibre 18',      1.5000, 'm3', 0.10),
    -- No. 5 clay block wall with 1:4 mortar
    ('wall',       'Bloque de arcilla No. 5',      12.5000, 'm2', 0.05),
    ('wall',       'Cemento gris uso general',      0.1500, 'm2', 0.05),
    ('wall',       'Arena de pega',                 0.0200, 'm2', 0.05),
    -- 8 cm floor base
    ('floor',      'Cemento gris uso general',      0.5000, 'm2', 0.05),
    ('floor',      'Arena de río (concreto)',       0.0450, 'm2', 0.05),
    ('floor',      'Gravilla',                      0.0650, 'm2', 0.05),
    -- Fiber cement roof sheets
    ('roof',       'Teja de fibrocemento No. 6',    0.7500, 'm2', 0.05)
) AS f(element_type, material_name, consumption, base_unit, waste)
JOIN material_catalog mc ON mc.name = f.material_name;
