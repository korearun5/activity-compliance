CREATE TABLE carbon_activity_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    carbon_profile_id UUID NOT NULL REFERENCES carbon_profiles (id),
    carbon_farm_plot_id UUID REFERENCES carbon_farm_plots (id),
    category_id UUID NOT NULL REFERENCES carbon_activity_categories (id),
    activity_date DATE NOT NULL,
    crop_name VARCHAR(160) NOT NULL,
    input_used VARCHAR(180),
    quantity_value NUMERIC(12,4),
    quantity_unit VARCHAR(40),
    remarks TEXT,
    evidence_count INTEGER NOT NULL DEFAULT 0,
    verification_status VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_carbon_activity_records_quantity
        CHECK (quantity_value IS NULL OR quantity_value >= 0),
    CONSTRAINT chk_carbon_activity_records_evidence_count
        CHECK (evidence_count >= 0),
    CONSTRAINT chk_carbon_activity_records_verification_status
        CHECK (verification_status IN ('PENDING_EVIDENCE', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_carbon_activity_records_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE INDEX idx_carbon_activity_records_profile
    ON carbon_activity_records (tenant_id, carbon_profile_id, activity_date DESC);
CREATE INDEX idx_carbon_activity_records_plot
    ON carbon_activity_records (tenant_id, carbon_farm_plot_id);
CREATE INDEX idx_carbon_activity_records_category
    ON carbon_activity_records (tenant_id, category_id);
CREATE INDEX idx_carbon_activity_records_status
    ON carbon_activity_records (tenant_id, status);
