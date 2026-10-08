-- =====================================================================
-- ObraIA · V2: domain block (20 tables)
-- Rules: English snake_case names; statuses use CHECK with English values
-- (translated by the frontend); money uses numeric(14,2).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Base catalogs
-- ---------------------------------------------------------------------
CREATE TABLE project_types (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(100) NOT NULL UNIQUE,
    description varchar(255),
    created_at  timestamptz  NOT NULL DEFAULT now()
);

-- City catalog. Price quotes use the project's city.
CREATE TABLE locations (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    city       varchar(100) NOT NULL,
    department varchar(100) NOT NULL,
    country    varchar(60)  NOT NULL DEFAULT 'Colombia',
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_locations_city UNIQUE (city, department, country)
);

-- Administrator's company (company projects).
CREATE TABLE companies (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by uuid         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name       varchar(150) NOT NULL,
    tax_id     varchar(20)  NOT NULL UNIQUE,   -- Colombian tax id (NIT)
    phone      varchar(20),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX ix_companies_created_by ON companies (created_by);
CREATE TRIGGER trg_companies_updated_at BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Site manager: registered as a contact, without an account or login.
CREATE TABLE site_managers (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by      uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name       varchar(150) NOT NULL,
    document_number varchar(20),
    phone           varchar(20),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_site_managers_created_by ON site_managers (created_by);
CREATE TRIGGER trg_site_managers_updated_at BEFORE UPDATE ON site_managers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- Project
-- ---------------------------------------------------------------------
CREATE TABLE projects (
    id                 uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by         uuid          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    project_type_id    uuid          NOT NULL REFERENCES project_types (id) ON DELETE RESTRICT,
    location_id        uuid          NOT NULL REFERENCES locations (id) ON DELETE RESTRICT,
    company_id         uuid          REFERENCES companies (id) ON DELETE RESTRICT,  -- null = individual owner
    site_manager_id    uuid          REFERENCES site_managers (id) ON DELETE SET NULL,
    name               varchar(150)  NOT NULL,
    address            varchar(255),
    built_area_m2      numeric(10,2) NOT NULL,
    floors             smallint      NOT NULL DEFAULT 1,
    -- Computed by PostgreSQL so it can never hold an inconsistent value.
    size_category      varchar(10)   GENERATED ALWAYS AS (
                           CASE WHEN built_area_m2 <= 120 THEN 'small' ELSE 'large' END
                       ) STORED,
    status             varchar(20)   NOT NULL DEFAULT 'draft',
    planned_start_date date,
    planned_end_date   date,
    actual_start_date  date,
    actual_end_date    date,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    updated_at         timestamptz   NOT NULL DEFAULT now(),
    deleted_at         timestamptz,
    CONSTRAINT ck_projects_area CHECK (built_area_m2 > 0),
    CONSTRAINT ck_projects_floors CHECK (floors BETWEEN 1 AND 2),
    CONSTRAINT ck_projects_status CHECK (status IN
        ('draft', 'planned', 'in_progress', 'paused', 'completed', 'cancelled')),
    CONSTRAINT ck_projects_planned_dates CHECK (planned_end_date >= planned_start_date),
    CONSTRAINT ck_projects_actual_dates CHECK (actual_end_date >= actual_start_date)
);
CREATE INDEX ix_projects_created_by ON projects (created_by);
CREATE INDEX ix_projects_project_type ON projects (project_type_id);
CREATE INDEX ix_projects_location ON projects (location_id);
CREATE INDEX ix_projects_company ON projects (company_id);
CREATE INDEX ix_projects_site_manager ON projects (site_manager_id);
CREATE TRIGGER trg_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Role of each user within each project (owner or company_admin).
CREATE TABLE project_members (
    project_id uuid        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id    uuid        NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    joined_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);
CREATE INDEX ix_project_members_user ON project_members (user_id);
CREATE INDEX ix_project_members_role ON project_members (role_id);

-- ---------------------------------------------------------------------
-- Blueprints and measurements (Module 1)
-- ---------------------------------------------------------------------
CREATE TABLE blueprints (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    uploaded_by       uuid         REFERENCES users (id) ON DELETE SET NULL,
    file_path         varchar(500) NOT NULL,   -- relative path or storage key, never the file itself
    original_filename varchar(255) NOT NULL,
    file_type         varchar(15)  NOT NULL,
    mime_type         varchar(100) NOT NULL,
    size_bytes        bigint       NOT NULL,
    storage_provider  varchar(10)  NOT NULL DEFAULT 'local',
    processing_status varchar(15)  NOT NULL DEFAULT 'pending',
    ocr_raw_output    jsonb,
    uploaded_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_blueprints_file_type CHECK (file_type IN ('image', 'scanned_pdf', 'vector_pdf')),
    CONSTRAINT ck_blueprints_size CHECK (size_bytes > 0),
    CONSTRAINT ck_blueprints_storage CHECK (storage_provider IN ('local', 'external')),
    CONSTRAINT ck_blueprints_processing_status CHECK (processing_status IN
        ('pending', 'processing', 'completed', 'failed'))
);
CREATE INDEX ix_blueprints_project ON blueprints (project_id);
CREATE INDEX ix_blueprints_uploaded_by ON blueprints (uploaded_by);

-- Each measurement stores its source. The system compares OCR against
-- manual input and records the difference in discrepancy_pct.
CREATE TABLE blueprint_measurements (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    blueprint_id    uuid         REFERENCES blueprints (id) ON DELETE SET NULL,
    element_type    varchar(15)  NOT NULL,
    floor_number    smallint     NOT NULL DEFAULT 1,
    length_m        numeric(8,2) NOT NULL,
    width_m         numeric(8,2),
    height_m        numeric(8,2),
    quantity        integer      NOT NULL DEFAULT 1,
    source          varchar(10)  NOT NULL,
    discrepancy_pct numeric(6,2),
    notes           text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_measurements_element_type CHECK (element_type IN
        ('foundation', 'column', 'beam', 'slab', 'wall', 'floor', 'roof')),
    CONSTRAINT ck_measurements_floor CHECK (floor_number BETWEEN 1 AND 2),
    CONSTRAINT ck_measurements_dimensions CHECK (
        length_m > 0 AND (width_m IS NULL OR width_m > 0) AND (height_m IS NULL OR height_m > 0)),
    CONSTRAINT ck_measurements_quantity CHECK (quantity > 0),
    CONSTRAINT ck_measurements_source CHECK (source IN ('ocr', 'manual'))
);
CREATE INDEX ix_measurements_project ON blueprint_measurements (project_id);
CREATE INDEX ix_measurements_blueprint ON blueprint_measurements (blueprint_id);

-- ---------------------------------------------------------------------
-- Roadmap (Module 4)
-- ---------------------------------------------------------------------
-- Phase templates by project type and size (Template Method pattern).
CREATE TABLE phase_templates (
    id                      uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_type_id         uuid         NOT NULL REFERENCES project_types (id) ON DELETE CASCADE,
    size_category           varchar(10)  NOT NULL,
    name                    varchar(100) NOT NULL,
    sequence_order          integer      NOT NULL,
    estimated_duration_days integer      NOT NULL,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_phase_templates_size CHECK (size_category IN ('small', 'large')),
    CONSTRAINT ck_phase_templates_order CHECK (sequence_order > 0),
    CONSTRAINT ck_phase_templates_duration CHECK (estimated_duration_days > 0),
    CONSTRAINT uq_phase_templates_order UNIQUE (project_type_id, size_category, sequence_order)
);

-- State pattern statuses: planned -> in_progress -> delayed -> completed
CREATE TABLE phases (
    id                 uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id         uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    phase_template_id  uuid         REFERENCES phase_templates (id) ON DELETE SET NULL,
    name               varchar(100) NOT NULL,
    sequence_order     integer      NOT NULL,
    status             varchar(15)  NOT NULL DEFAULT 'planned',
    planned_start_date date,
    planned_end_date   date,
    actual_start_date  date,
    actual_end_date    date,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_phases_status CHECK (status IN ('planned', 'in_progress', 'delayed', 'completed')),
    CONSTRAINT ck_phases_order CHECK (sequence_order > 0),
    CONSTRAINT ck_phases_planned_dates CHECK (planned_end_date >= planned_start_date),
    CONSTRAINT ck_phases_actual_dates CHECK (actual_end_date >= actual_start_date),
    CONSTRAINT uq_phases_order UNIQUE (project_id, sequence_order)
);
CREATE INDEX ix_phases_template ON phases (phase_template_id);
CREATE TRIGGER trg_phases_updated_at BEFORE UPDATE ON phases
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE tasks (
    id                 uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id           uuid         NOT NULL REFERENCES phases (id) ON DELETE CASCADE,
    name               varchar(150) NOT NULL,
    status             varchar(15)  NOT NULL DEFAULT 'planned',
    planned_start_date date,
    planned_end_date   date,
    actual_start_date  date,
    actual_end_date    date,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tasks_status CHECK (status IN ('planned', 'in_progress', 'delayed', 'completed')),
    CONSTRAINT ck_tasks_planned_dates CHECK (planned_end_date >= planned_start_date),
    CONSTRAINT ck_tasks_actual_dates CHECK (actual_end_date >= actual_start_date)
);
CREATE INDEX ix_tasks_phase ON tasks (phase_id);
CREATE TRIGGER trg_tasks_updated_at BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- Site progress tracking (Module 3)
-- ---------------------------------------------------------------------
-- Recorded by an account holder; site_manager_id says which site manager reported it.
CREATE TABLE progress_reports (
    id                    uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id               uuid         NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    reported_by           uuid         REFERENCES users (id) ON DELETE SET NULL,
    site_manager_id       uuid         REFERENCES site_managers (id) ON DELETE SET NULL,
    completion_percentage numeric(5,2) NOT NULL,
    notes                 text,
    photo_path            varchar(500),
    report_date           date         NOT NULL DEFAULT current_date,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_progress_reports_percentage CHECK (completion_percentage BETWEEN 0 AND 100)
);
CREATE INDEX ix_progress_reports_task ON progress_reports (task_id);
CREATE INDEX ix_progress_reports_reported_by ON progress_reports (reported_by);
CREATE INDEX ix_progress_reports_site_manager ON progress_reports (site_manager_id);

CREATE TABLE risk_assessments (
    id                   uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id           uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    risk_level           varchar(10)  NOT NULL,
    risk_score           numeric(5,2) NOT NULL,
    predicted_delay_days integer      NOT NULL DEFAULT 0,
    risk_factors         jsonb        NOT NULL DEFAULT '[]'::jsonb,
    model_version        varchar(30)  NOT NULL DEFAULT 'rules-v1',
    trigger_source       varchar(20)  NOT NULL,
    assessed_at          timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_risk_level CHECK (risk_level IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT ck_risk_score CHECK (risk_score BETWEEN 0 AND 100),
    CONSTRAINT ck_risk_delay CHECK (predicted_delay_days >= 0),
    CONSTRAINT ck_risk_trigger CHECK (trigger_source IN ('progress_report', 'expense', 'scheduled', 'manual'))
);
CREATE INDEX ix_risk_assessments_project ON risk_assessments (project_id, assessed_at DESC);

-- ---------------------------------------------------------------------
-- Materials and price quotes
-- ---------------------------------------------------------------------
-- Reference price in COP, VAT included.
CREATE TABLE material_catalog (
    id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    name             varchar(150)  NOT NULL UNIQUE,
    category         varchar(20)   NOT NULL,
    unit             varchar(20)   NOT NULL,
    reference_price  numeric(14,2) NOT NULL,
    price_updated_at date          NOT NULL DEFAULT current_date,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_material_catalog_category CHECK (category IN
        ('cement_concrete', 'aggregates', 'masonry', 'steel', 'roofing', 'finishes', 'other')),
    CONSTRAINT ck_material_catalog_price CHECK (reference_price >= 0)
);
CREATE TRIGGER trg_material_catalog_updated_at BEFORE UPDATE ON material_catalog
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Material consumption per element unit (Strategy pattern).
CREATE TABLE material_formulas (
    id                   uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    project_type_id      uuid          NOT NULL REFERENCES project_types (id) ON DELETE CASCADE,
    material_id          uuid          NOT NULL REFERENCES material_catalog (id) ON DELETE RESTRICT,
    element_type         varchar(15)   NOT NULL,
    consumption_per_unit numeric(12,4) NOT NULL,
    base_unit            varchar(5)    NOT NULL,
    waste_factor         numeric(5,4)  NOT NULL DEFAULT 0.05,
    created_at           timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_formulas_element_type CHECK (element_type IN
        ('foundation', 'column', 'beam', 'slab', 'wall', 'floor', 'roof')),
    CONSTRAINT ck_formulas_consumption CHECK (consumption_per_unit > 0),
    CONSTRAINT ck_formulas_base_unit CHECK (base_unit IN ('m2', 'm3', 'ml')),
    CONSTRAINT ck_formulas_waste CHECK (waste_factor BETWEEN 0 AND 1),
    CONSTRAINT uq_formulas UNIQUE (project_type_id, material_id, element_type)
);
CREATE INDEX ix_formulas_material ON material_formulas (material_id);

-- unit_price_snapshot freezes the quoted price even if the catalog changes.
CREATE TABLE project_materials (
    id                  uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          uuid          NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    material_id         uuid          NOT NULL REFERENCES material_catalog (id) ON DELETE RESTRICT,
    calculated_quantity numeric(12,2) NOT NULL,
    unit_price_snapshot numeric(14,2) NOT NULL,
    subtotal            numeric(16,2) GENERATED ALWAYS AS (calculated_quantity * unit_price_snapshot) STORED,
    created_at          timestamptz   NOT NULL DEFAULT now(),
    updated_at          timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_project_materials_quantity CHECK (calculated_quantity >= 0),
    CONSTRAINT ck_project_materials_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT uq_project_materials UNIQUE (project_id, material_id)
);
CREATE INDEX ix_project_materials_material ON project_materials (material_id);
CREATE TRIGGER trg_project_materials_updated_at BEFORE UPDATE ON project_materials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- Finance (Module 2)
-- ---------------------------------------------------------------------
-- Each recalculation creates a 'draft' version; when approved, the previous
-- one becomes 'superseded'. Only one approved budget per project.
CREATE TABLE budgets (
    id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       uuid          NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    version          integer       NOT NULL,
    status           varchar(12)   NOT NULL DEFAULT 'draft',
    projected_amount numeric(14,2) NOT NULL,
    actual_amount    numeric(14,2) NOT NULL DEFAULT 0,
    approved_at      timestamptz,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_budgets_version CHECK (version > 0),
    CONSTRAINT ck_budgets_status CHECK (status IN ('draft', 'approved', 'superseded')),
    CONSTRAINT ck_budgets_amounts CHECK (projected_amount >= 0 AND actual_amount >= 0),
    CONSTRAINT ck_budgets_approved_at CHECK (status <> 'approved' OR approved_at IS NOT NULL),
    CONSTRAINT uq_budgets_version UNIQUE (project_id, version)
);
CREATE UNIQUE INDEX uq_budgets_one_approved ON budgets (project_id) WHERE status = 'approved';

-- Budget layers (Decorator pattern): an item is a fixed amount or a
-- percentage (taxes, contingency).
CREATE TABLE budget_items (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id      uuid          NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    item_type      varchar(15)   NOT NULL,
    description    varchar(255)  NOT NULL,
    amount         numeric(14,2) NOT NULL,
    percentage     numeric(5,2),
    is_percentage  boolean       NOT NULL DEFAULT false,
    sequence_order integer       NOT NULL DEFAULT 1,
    CONSTRAINT ck_budget_items_type CHECK (item_type IN
        ('materials', 'labor', 'equipment', 'tax', 'contingency', 'other')),
    CONSTRAINT ck_budget_items_amount CHECK (amount >= 0),
    CONSTRAINT ck_budget_items_percentage CHECK (
        (is_percentage AND percentage BETWEEN 0 AND 100) OR (NOT is_percentage AND percentage IS NULL))
);
CREATE INDEX ix_budget_items_budget ON budget_items (budget_id);

-- Actual expenses: basis for cash flow and deviation detection.
CREATE TABLE expenses (
    id            uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    uuid          NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    phase_id      uuid          REFERENCES phases (id) ON DELETE SET NULL,
    registered_by uuid          REFERENCES users (id) ON DELETE SET NULL,
    category      varchar(15)   NOT NULL,
    description   varchar(255)  NOT NULL,
    amount        numeric(14,2) NOT NULL,
    expense_date  date          NOT NULL DEFAULT current_date,
    receipt_path  varchar(500),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_expenses_category CHECK (category IN
        ('materials', 'labor', 'equipment', 'tax', 'other')),
    CONSTRAINT ck_expenses_amount CHECK (amount > 0)
);
CREATE INDEX ix_expenses_project_date ON expenses (project_id, expense_date);
CREATE INDEX ix_expenses_phase ON expenses (phase_id);
CREATE INDEX ix_expenses_registered_by ON expenses (registered_by);

-- ---------------------------------------------------------------------
-- Automatic notifications (Observer pattern)
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id uuid        REFERENCES projects (id) ON DELETE CASCADE,
    type       varchar(25) NOT NULL,
    message    text        NOT NULL,
    is_read    boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'budget_deviation', 'delay_risk', 'phase_status_changed', 'task_overdue',
        'budget_approved', 'deletion_scheduled', 'system'))
);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);
CREATE INDEX ix_notifications_project ON notifications (project_id);
