CREATE TABLE fpo_member_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    user_id UUID NOT NULL REFERENCES users (id),
    member_number VARCHAR(80) NOT NULL,
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
    coordinator_user_id UUID REFERENCES users (id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fpo_members_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT uq_fpo_members_tenant_member_number UNIQUE (tenant_id, member_number),
    CONSTRAINT uq_fpo_members_tenant_mobile UNIQUE (tenant_id, mobile_number),
    CONSTRAINT chk_fpo_members_age CHECK (age IS NULL OR (age >= 0 AND age <= 120))
);

CREATE TABLE farm_landholdings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    member_profile_id UUID NOT NULL REFERENCES fpo_member_profiles (id),
    survey_number VARCHAR(120) NOT NULL,
    total_area_acres NUMERIC(12,4) NOT NULL,
    cultivable_area_acres NUMERIC(12,4),
    ownership_type VARCHAR(80) NOT NULL,
    irrigation_source VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_farm_landholdings_ownership_type
        CHECK (ownership_type IN ('Self-owned', 'Leased-in', 'Sharecropper')),
    CONSTRAINT chk_farm_landholdings_irrigation_source
        CHECK (irrigation_source IN ('Canal', 'Borewell', 'Open well', 'Pond', 'Rainfed', 'Drip'))
);

CREATE TABLE farm_plots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    member_profile_id UUID NOT NULL REFERENCES fpo_member_profiles (id),
    landholding_id UUID REFERENCES farm_landholdings (id),
    plot_name VARCHAR(160) NOT NULL,
    area_acres NUMERIC(12,4) NOT NULL,
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    boundary_geojson JSONB,
    soil_type VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_farm_plots_latitude
        CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_farm_plots_longitude
        CHECK (longitude BETWEEN -180 AND 180)
);

CREATE TABLE fpo_soil_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    member_profile_id UUID NOT NULL REFERENCES fpo_member_profiles (id),
    soil_organic_carbon NUMERIC(10,4),
    ph NUMERIC(4,2),
    nitrogen NUMERIC(12,4),
    phosphorus NUMERIC(12,4),
    potassium NUMERIC(12,4),
    report_file_name VARCHAR(240),
    report_content_type VARCHAR(120),
    report_url TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fpo_soil_profiles_soc
        CHECK (soil_organic_carbon IS NULL OR soil_organic_carbon >= 0),
    CONSTRAINT chk_fpo_soil_profiles_ph
        CHECK (ph IS NULL OR (ph >= 0 AND ph <= 14)),
    CONSTRAINT chk_fpo_soil_profiles_n
        CHECK (nitrogen IS NULL OR nitrogen >= 0),
    CONSTRAINT chk_fpo_soil_profiles_p
        CHECK (phosphorus IS NULL OR phosphorus >= 0),
    CONSTRAINT chk_fpo_soil_profiles_k
        CHECK (potassium IS NULL OR potassium >= 0)
);

CREATE TABLE crop_catalog (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    crop_code VARCHAR(80) NOT NULL,
    crop_name VARCHAR(160) NOT NULL,
    category VARCHAR(80),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_crop_catalog_tenant_code UNIQUE (tenant_id, crop_code)
);

CREATE TABLE crop_seasons (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    start_month INTEGER,
    end_month INTEGER,
    season_year INTEGER,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_crop_seasons_tenant_code_year UNIQUE (tenant_id, code, season_year),
    CONSTRAINT chk_crop_seasons_start_month CHECK (start_month IS NULL OR start_month BETWEEN 1 AND 12),
    CONSTRAINT chk_crop_seasons_end_month CHECK (end_month IS NULL OR end_month BETWEEN 1 AND 12)
);

CREATE TABLE farmer_crop_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    member_profile_id UUID NOT NULL REFERENCES fpo_member_profiles (id),
    crop_id UUID NOT NULL REFERENCES crop_catalog (id),
    season_id UUID REFERENCES crop_seasons (id),
    crop_year INTEGER,
    area_acres NUMERIC(12,4),
    yield_quantity NUMERIC(14,4),
    yield_unit VARCHAR(40),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE seasonal_crop_plans (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    member_profile_id UUID NOT NULL REFERENCES fpo_member_profiles (id),
    plot_id UUID REFERENCES farm_plots (id),
    crop_id UUID NOT NULL REFERENCES crop_catalog (id),
    season_id UUID NOT NULL REFERENCES crop_seasons (id),
    planned_area_acres NUMERIC(12,4) NOT NULL,
    planned_sowing_date DATE,
    expected_harvest_date DATE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE input_catalog (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    input_code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80),
    unit VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_input_catalog_tenant_code UNIQUE (tenant_id, input_code)
);

CREATE TABLE crop_input_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    crop_id UUID NOT NULL REFERENCES crop_catalog (id),
    input_id UUID NOT NULL REFERENCES input_catalog (id),
    quantity_per_acre NUMERIC(14,4) NOT NULL,
    application_stage VARCHAR(120),
    notes TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_crop_input_rules_tenant_crop_input_stage
        UNIQUE (tenant_id, crop_id, input_id, application_stage)
);

CREATE TABLE input_demand_estimates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    crop_plan_id UUID NOT NULL REFERENCES seasonal_crop_plans (id),
    input_id UUID NOT NULL REFERENCES input_catalog (id),
    estimated_quantity NUMERIC(14,4) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_input_demand_estimates_plan_input UNIQUE (crop_plan_id, input_id)
);

CREATE INDEX idx_fpo_member_profiles_tenant_status
    ON fpo_member_profiles (tenant_id, status);
CREATE INDEX idx_fpo_member_profiles_village
    ON fpo_member_profiles (tenant_id, village);
CREATE INDEX idx_fpo_member_profiles_coordinator
    ON fpo_member_profiles (coordinator_user_id);
CREATE INDEX idx_farm_landholdings_member
    ON farm_landholdings (tenant_id, member_profile_id);
CREATE INDEX idx_farm_plots_member
    ON farm_plots (tenant_id, member_profile_id);
CREATE INDEX idx_farm_plots_landholding
    ON farm_plots (landholding_id);
CREATE INDEX idx_fpo_soil_profiles_member
    ON fpo_soil_profiles (tenant_id, member_profile_id);
CREATE INDEX idx_crop_catalog_tenant_status
    ON crop_catalog (tenant_id, status);
CREATE INDEX idx_crop_seasons_tenant_status
    ON crop_seasons (tenant_id, status);
CREATE INDEX idx_farmer_crop_history_member
    ON farmer_crop_history (tenant_id, member_profile_id);
CREATE INDEX idx_farmer_crop_history_crop
    ON farmer_crop_history (tenant_id, crop_id);
CREATE INDEX idx_farmer_crop_history_season
    ON farmer_crop_history (tenant_id, season_id);
CREATE INDEX idx_seasonal_crop_plans_member
    ON seasonal_crop_plans (tenant_id, member_profile_id);
CREATE INDEX idx_seasonal_crop_plans_crop
    ON seasonal_crop_plans (tenant_id, crop_id);
CREATE INDEX idx_seasonal_crop_plans_season
    ON seasonal_crop_plans (tenant_id, season_id);
CREATE INDEX idx_input_catalog_tenant_status
    ON input_catalog (tenant_id, status);
CREATE INDEX idx_crop_input_rules_crop
    ON crop_input_rules (tenant_id, crop_id);
CREATE INDEX idx_input_demand_estimates_plan
    ON input_demand_estimates (tenant_id, crop_plan_id);
