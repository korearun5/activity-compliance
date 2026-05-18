CREATE TABLE IF NOT EXISTS farmer_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    display_name VARCHAR(180) NOT NULL,
    mobile_number VARCHAR(40) NOT NULL,
    alternate_mobile_number VARCHAR(40),
    aadhaar_number VARCHAR(12),
    village VARCHAR(160) NOT NULL,
    taluka VARCHAR(160) NOT NULL,
    district_name VARCHAR(160) NOT NULL,
    state_name VARCHAR(160) NOT NULL,
    gender VARCHAR(32) NOT NULL,
    date_of_birth DATE,
    age INTEGER,
    farmer_category VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_farmer_profiles_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_farmer_profiles_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_farmer_profiles_category CHECK (
        farmer_category IN ('MARGINAL', 'SMALL', 'SEMI_MEDIUM', 'MEDIUM', 'LARGE')
    ),
    CONSTRAINT chk_farmer_profiles_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_farmer_profiles_age CHECK (age IS NULL OR (age >= 0 AND age <= 120))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_farmer_profiles_tenant_aadhaar
    ON farmer_profiles (tenant_id, aadhaar_number)
    WHERE aadhaar_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_farmer_profiles_tenant_status
    ON farmer_profiles (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_farmer_profiles_tenant_mobile
    ON farmer_profiles (tenant_id, mobile_number);

CREATE INDEX IF NOT EXISTS idx_farmer_profiles_geography
    ON farmer_profiles (tenant_id, village, taluka, district_name);

CREATE INDEX IF NOT EXISTS idx_farmer_profiles_created_at
    ON farmer_profiles (tenant_id, created_at);

ALTER TABLE fpo_member_profiles
    ADD COLUMN IF NOT EXISTS farmer_profile_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fpo_member_profiles_farmer_profile'
    ) THEN
        ALTER TABLE fpo_member_profiles
            ADD CONSTRAINT fk_fpo_member_profiles_farmer_profile
            FOREIGN KEY (farmer_profile_id) REFERENCES farmer_profiles(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_fpo_member_profiles_farmer_profile
    ON fpo_member_profiles (tenant_id, farmer_profile_id)
    WHERE farmer_profile_id IS NOT NULL;

ALTER TABLE carbon_profiles
    ADD COLUMN IF NOT EXISTS farmer_profile_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_carbon_profiles_farmer_profile'
    ) THEN
        ALTER TABLE carbon_profiles
            ADD CONSTRAINT fk_carbon_profiles_farmer_profile
            FOREIGN KEY (farmer_profile_id) REFERENCES farmer_profiles(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_carbon_profiles_farmer_profile
    ON carbon_profiles (tenant_id, farmer_profile_id)
    WHERE farmer_profile_id IS NOT NULL;
