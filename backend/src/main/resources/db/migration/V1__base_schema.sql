CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    username VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    phone VARCHAR(40),
    location_name VARCHAR(160),
    site_name VARCHAR(160),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username)
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    tenant_id UUID REFERENCES tenants (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE workflow_definitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    domain_key VARCHAR(80),
    duration_days INTEGER NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workflow_definitions_tenant_code_version UNIQUE (tenant_id, code, version)
);

CREATE TABLE workflow_tasks (
    id UUID PRIMARY KEY,
    workflow_definition_id UUID NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    code VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    sequence_number INTEGER NOT NULL,
    offset_days INTEGER NOT NULL,
    evidence_required BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workflow_tasks_definition_code UNIQUE (workflow_definition_id, code),
    CONSTRAINT uq_workflow_tasks_definition_sequence UNIQUE (workflow_definition_id, sequence_number)
);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    workflow_definition_id UUID NOT NULL REFERENCES workflow_definitions (id),
    participant_user_id UUID REFERENCES users (id),
    unit_name VARCHAR(180) NOT NULL,
    location_name VARCHAR(180),
    status VARCHAR(32) NOT NULL,
    started_on DATE NOT NULL,
    expected_completion DATE NOT NULL,
    completed_at TIMESTAMPTZ,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE activity_tasks (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    workflow_task_id UUID NOT NULL REFERENCES workflow_tasks (id),
    status VARCHAR(32) NOT NULL,
    due_on DATE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_activity_tasks_activity_workflow_task UNIQUE (activity_id, workflow_task_id)
);

CREATE TABLE evidence (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    activity_task_id UUID NOT NULL REFERENCES activity_tasks (id),
    participant_user_id UUID REFERENCES users (id),
    storage_key TEXT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    note TEXT,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_by_user_id UUID REFERENCES users (id),
    reviewed_at TIMESTAMPTZ
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    actor_user_id UUID REFERENCES users (id),
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    request_id VARCHAR(120),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE report_exports (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    requested_by_user_id UUID REFERENCES users (id),
    report_type VARCHAR(80) NOT NULL,
    format VARCHAR(16) NOT NULL,
    filter_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    storage_key TEXT,
    status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE notification_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    recipient_user_id UUID REFERENCES users (id),
    channel VARCHAR(32) NOT NULL,
    template_code VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    queued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX idx_users_tenant_status ON users (tenant_id, status);
CREATE INDEX idx_workflow_definitions_tenant_status ON workflow_definitions (tenant_id, status);
CREATE INDEX idx_activities_tenant_status ON activities (tenant_id, status);
CREATE INDEX idx_activities_participant ON activities (participant_user_id);
CREATE INDEX idx_activity_tasks_activity_status ON activity_tasks (activity_id, status);
CREATE INDEX idx_evidence_tenant_submitted ON evidence (tenant_id, submitted_at DESC);
CREATE INDEX idx_audit_events_aggregate ON audit_events (aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX idx_report_exports_tenant_requested ON report_exports (tenant_id, requested_at DESC);
CREATE INDEX idx_notification_events_status ON notification_events (tenant_id, status, queued_at);

