-- =====================================================================
-- ObraIA - V4: content of the uploaded blueprints
-- Render's free plan erases its disk on every deploy, so in production
-- the files are kept in the database. blueprints.file_path holds the key
-- of the row; the metadata table stays as it was in V2.
-- =====================================================================

CREATE TABLE blueprint_files (
    storage_key varchar(500) PRIMARY KEY,
    content     bytea        NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_blueprint_files_size CHECK (octet_length(content) > 0)
);

COMMENT ON TABLE blueprint_files IS 'File bytes of the blueprints when STORAGE_PROVIDER=database';
