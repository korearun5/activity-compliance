ALTER TABLE carbon_soil_profiles
    ADD COLUMN verification_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    ADD COLUMN verification_notes TEXT,
    ADD COLUMN verified_by_user_id UUID REFERENCES users (id),
    ADD COLUMN verified_at TIMESTAMPTZ;

ALTER TABLE carbon_soil_profiles
    ADD CONSTRAINT chk_carbon_soil_profiles_verification_status
        CHECK (verification_status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED'));

CREATE INDEX idx_carbon_soil_profiles_tenant_verification_status
    ON carbon_soil_profiles (tenant_id, verification_status);
