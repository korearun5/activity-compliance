CREATE UNIQUE INDEX IF NOT EXISTS uq_fpo_member_profiles_tenant_farmer_profile
    ON fpo_member_profiles (tenant_id, farmer_profile_id)
    WHERE farmer_profile_id IS NOT NULL;
