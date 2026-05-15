ALTER TABLE carbon_profiles
    ADD COLUMN coordinator_user_id UUID REFERENCES users (id);

CREATE INDEX idx_carbon_profiles_coordinator
    ON carbon_profiles (tenant_id, coordinator_user_id);
