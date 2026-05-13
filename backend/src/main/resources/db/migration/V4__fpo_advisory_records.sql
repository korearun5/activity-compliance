CREATE TABLE fpo_advisories (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    crop_id UUID REFERENCES crop_catalog (id),
    season_id UUID REFERENCES crop_seasons (id),
    target_type VARCHAR(40) NOT NULL,
    target_village VARCHAR(160),
    target_member_id UUID REFERENCES fpo_member_profiles (id),
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by_user_id UUID REFERENCES users (id),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fpo_advisories_tenant_status
    ON fpo_advisories (tenant_id, status);
CREATE INDEX idx_fpo_advisories_crop
    ON fpo_advisories (tenant_id, crop_id);
CREATE INDEX idx_fpo_advisories_season
    ON fpo_advisories (tenant_id, season_id);
CREATE INDEX idx_fpo_advisories_target_member
    ON fpo_advisories (tenant_id, target_member_id);
CREATE INDEX idx_fpo_advisories_target_village
    ON fpo_advisories (tenant_id, target_village);
