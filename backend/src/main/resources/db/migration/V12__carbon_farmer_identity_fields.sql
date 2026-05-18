ALTER TABLE carbon_profiles
    ADD COLUMN member_number VARCHAR(80),
    ADD COLUMN username VARCHAR(120),
    ADD COLUMN alternate_mobile_number VARCHAR(40),
    ADD COLUMN aadhaar_number VARCHAR(12),
    ADD COLUMN gender VARCHAR(32),
    ADD COLUMN age INTEGER,
    ADD COLUMN farmer_category VARCHAR(80),
    ADD CONSTRAINT chk_carbon_profiles_age
        CHECK (age IS NULL OR (age >= 0 AND age <= 120)),
    ADD CONSTRAINT chk_carbon_profiles_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    ADD CONSTRAINT chk_carbon_profiles_farmer_category
        CHECK (farmer_category IS NULL OR farmer_category IN ('MARGINAL', 'SMALL', 'SEMI_MEDIUM', 'MEDIUM', 'LARGE'));

CREATE UNIQUE INDEX uq_carbon_profiles_tenant_member_number
    ON carbon_profiles (tenant_id, lower(member_number))
    WHERE member_number IS NOT NULL;

CREATE INDEX idx_carbon_profiles_username
    ON carbon_profiles (tenant_id, username);
