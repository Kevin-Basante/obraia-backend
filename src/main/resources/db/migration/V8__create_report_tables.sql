-- =====================================================================
-- ObraIA - V8: PDF reports of a project (block 13)
-- The public report is open to every member. The full report (budget,
-- expenses and risk) needs the approval of the creator of the project,
-- who gets an email link; after the approval the member can download it
-- for a while. A public report can also be shared with a link.
-- =====================================================================

CREATE TABLE report_requests (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    requested_by     uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash       varchar(64)  NOT NULL UNIQUE,
    status           varchar(20)  NOT NULL DEFAULT 'pending',
    link_expires_at  timestamptz  NOT NULL,
    approved_at      timestamptz,
    download_until   timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_request_status CHECK (status IN ('pending', 'approved')),
    CONSTRAINT ck_report_request_approval CHECK (
        (status = 'pending' AND approved_at IS NULL AND download_until IS NULL)
        OR (status = 'approved' AND approved_at IS NOT NULL AND download_until IS NOT NULL))
);

CREATE INDEX ix_report_requests_requester ON report_requests (project_id, requested_by, created_at DESC);

COMMENT ON TABLE report_requests IS
    'Requests of a member to download the full report; only the SHA-256 hash of the email link is stored';
COMMENT ON COLUMN report_requests.download_until IS
    'After the approval, the member can download the full report until this moment';

CREATE TABLE report_shares (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  uuid         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    created_by  uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  varchar(64)  NOT NULL UNIQUE,
    expires_at  timestamptz  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX ix_report_shares_project ON report_shares (project_id);

COMMENT ON TABLE report_shares IS
    'Links that open the public report without an account; only the SHA-256 hash of the token is stored';
