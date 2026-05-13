CREATE TABLE platform_modules (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenant_module_subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    module_id UUID NOT NULL REFERENCES platform_modules (id),
    status VARCHAR(32) NOT NULL,
    enabled_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_module_subscriptions UNIQUE (tenant_id, module_id)
);

INSERT INTO platform_modules (id, code, name, description, status) VALUES
    ('00000000-0000-0000-0000-000000000101', 'MEMBER_DATA', 'Member Data', 'FPO member registration, farmer profile, and member lifecycle data.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000102', 'LAND_RECORDS', 'Land Records', 'Landholding and farm plot records including GPS-ready plot metadata.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000103', 'GEO_TAGGING', 'Geo Tagging', 'Farm and activity location capture for field verification.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000104', 'CROP_PLANNING', 'Crop Planning', 'Crop catalog, crop history, seasonal plans, and crop workflow configuration.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000105', 'INPUT_DEMAND', 'Input Demand', 'Acreage-based input planning and demand aggregation.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000106', 'ADVISORY', 'Advisory', 'Basic notification and advisory foundation for farmers and coordinators.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000107', 'ACTIVITY_COMPLIANCE', 'Activity Compliance', 'Workflow execution, task timelines, and field process tracking.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000108', 'EVIDENCE_REVIEW', 'Evidence Review', 'Proof upload, evidence review, and compliance verification.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000109', 'REPORT_EXPORT', 'Report Export', 'PDF and Excel export for administration and government reporting.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000110', 'INVENTORY', 'Inventory', 'Input inventory and stock movement tracking.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000111', 'PROCUREMENT', 'Procurement', 'Bulk procurement planning, supplier coordination, and purchase workflows.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000112', 'TRACEABILITY', 'Traceability', 'Lot-level produce traceability, grading, and buyer-facing records.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000113', 'SUSTAINABILITY', 'Sustainability', 'Carbon, sustainability, and climate program data foundation.', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000114', 'ANALYTICS', 'Analytics', 'Advanced dashboards, forecasting, and decision support analytics.', 'ACTIVE')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = now();

CREATE INDEX idx_platform_modules_status ON platform_modules (status);
CREATE INDEX idx_tenant_module_subscriptions_tenant_status
    ON tenant_module_subscriptions (tenant_id, status);
