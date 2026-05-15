CREATE TABLE carbon_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    user_id UUID REFERENCES users (id),
    fpo_member_profile_id UUID REFERENCES fpo_member_profiles (id),
    carbon_identity_id VARCHAR(80) NOT NULL,
    participant_type VARCHAR(32) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    mobile_number VARCHAR(40),
    language_preference VARCHAR(40),
    village VARCHAR(160),
    taluka VARCHAR(160),
    district_name VARCHAR(160),
    state_name VARCHAR(160),
    gps_latitude NUMERIC(10,7),
    gps_longitude NUMERIC(10,7),
    total_land_holding_acres NUMERIC(12,4),
    cropping_pattern TEXT,
    livestock_count INTEGER,
    tillage_status VARCHAR(80),
    bank_status VARCHAR(80),
    aadhaar_status VARCHAR(80),
    document_status VARCHAR(80),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_carbon_profiles_tenant_identity UNIQUE (tenant_id, carbon_identity_id),
    CONSTRAINT chk_carbon_profiles_participant_type
        CHECK (participant_type IN ('FARMER', 'FPO_FPC', 'AGRONOMIST')),
    CONSTRAINT chk_carbon_profiles_language
        CHECK (language_preference IS NULL OR language_preference IN ('English', 'Hindi', 'Marathi')),
    CONSTRAINT chk_carbon_profiles_latitude
        CHECK (gps_latitude IS NULL OR gps_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_carbon_profiles_longitude
        CHECK (gps_longitude IS NULL OR gps_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_carbon_profiles_area
        CHECK (total_land_holding_acres IS NULL OR total_land_holding_acres >= 0),
    CONSTRAINT chk_carbon_profiles_livestock
        CHECK (livestock_count IS NULL OR livestock_count >= 0),
    CONSTRAINT chk_carbon_profiles_tillage_status
        CHECK (tillage_status IS NULL OR tillage_status IN ('Conventional', 'Reduced tillage', 'No tillage')),
    CONSTRAINT chk_carbon_profiles_bank_status
        CHECK (bank_status IS NULL OR bank_status IN ('Linked', 'Pending', 'Not required')),
    CONSTRAINT chk_carbon_profiles_aadhaar_status
        CHECK (aadhaar_status IS NULL OR aadhaar_status IN ('Provided', 'Optional not captured')),
    CONSTRAINT chk_carbon_profiles_document_status
        CHECK (document_status IS NULL OR document_status IN ('Not started', 'Partial', 'Ready'))
);

CREATE TABLE carbon_farm_plots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    carbon_profile_id UUID NOT NULL REFERENCES carbon_profiles (id),
    farm_name VARCHAR(160) NOT NULL,
    survey_number VARCHAR(120),
    area_acres NUMERIC(12,4) NOT NULL,
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    boundary_geojson JSONB,
    irrigation_source VARCHAR(120),
    primary_crop VARCHAR(160),
    tillage_status VARCHAR(80),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_carbon_farm_plots_area CHECK (area_acres >= 0),
    CONSTRAINT chk_carbon_farm_plots_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_carbon_farm_plots_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_carbon_farm_plots_tillage_status
        CHECK (tillage_status IS NULL OR tillage_status IN ('Conventional', 'Reduced tillage', 'No tillage'))
);

CREATE TABLE carbon_soil_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    carbon_profile_id UUID NOT NULL REFERENCES carbon_profiles (id),
    carbon_farm_plot_id UUID REFERENCES carbon_farm_plots (id),
    test_date DATE,
    lab_name VARCHAR(180),
    soil_organic_carbon_percent NUMERIC(10,4),
    ph NUMERIC(4,2),
    ec NUMERIC(10,4),
    nitrogen_kg_ha NUMERIC(12,4),
    phosphorus_kg_ha NUMERIC(12,4),
    potassium_kg_ha NUMERIC(12,4),
    bulk_density_g_cm3 NUMERIC(10,4),
    texture VARCHAR(120),
    microbial_count NUMERIC(14,4),
    microbial_count_unit VARCHAR(80),
    biological_notes TEXT,
    report_file_name VARCHAR(240),
    report_content_type VARCHAR(120),
    report_storage_key TEXT,
    report_url TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_carbon_soil_profiles_soc
        CHECK (soil_organic_carbon_percent IS NULL OR soil_organic_carbon_percent >= 0),
    CONSTRAINT chk_carbon_soil_profiles_ph
        CHECK (ph IS NULL OR (ph >= 0 AND ph <= 14)),
    CONSTRAINT chk_carbon_soil_profiles_ec
        CHECK (ec IS NULL OR ec >= 0),
    CONSTRAINT chk_carbon_soil_profiles_n
        CHECK (nitrogen_kg_ha IS NULL OR nitrogen_kg_ha >= 0),
    CONSTRAINT chk_carbon_soil_profiles_p
        CHECK (phosphorus_kg_ha IS NULL OR phosphorus_kg_ha >= 0),
    CONSTRAINT chk_carbon_soil_profiles_k
        CHECK (potassium_kg_ha IS NULL OR potassium_kg_ha >= 0),
    CONSTRAINT chk_carbon_soil_profiles_bulk_density
        CHECK (bulk_density_g_cm3 IS NULL OR bulk_density_g_cm3 >= 0),
    CONSTRAINT chk_carbon_soil_profiles_microbial_count
        CHECK (microbial_count IS NULL OR microbial_count >= 0),
    CONSTRAINT chk_carbon_soil_profiles_content_type
        CHECK (report_content_type IS NULL OR report_content_type IN ('application/pdf')
            OR report_content_type LIKE 'image/%')
);

CREATE TABLE carbon_activity_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    evidence_required BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO carbon_activity_categories (
    id,
    code,
    name,
    description,
    evidence_required,
    sort_order,
    status
) VALUES
    ('00000000-0000-0000-0000-000000000301', 'LAND_PREPARATION', 'Land Preparation', 'Tillage, residue management, bed preparation, and pre-season regenerative practice setup.', true, 10, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000302', 'SOWING', 'Sowing', 'Crop sowing, seed treatment, cover crop establishment, and crop start evidence.', true, 20, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000303', 'FERTIGATION', 'Fertigation', 'Fertilizer or nutrient application through irrigation or field application records.', true, 30, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000304', 'IRRIGATION', 'Irrigation', 'Water application, irrigation method, duration, and water-saving practice records.', true, 40, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000305', 'BIOLOGICAL_APPLICATION', 'Biological Application', 'Biological input, microbial product, or soil health product application records.', true, 50, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000306', 'COMPOST_ADDITION', 'Compost Addition', 'Compost, farmyard manure, vermicompost, or organic matter addition records.', true, 60, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000307', 'PRUNING_BIOMASS_INCORPORATION', 'Pruning Biomass Incorporation', 'Crop residue, pruning biomass, mulch, or biomass incorporation records.', true, 70, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000308', 'HARVESTING', 'Harvesting', 'Harvest completion, biomass handling, and end-of-season evidence records.', true, 80, 'ACTIVE')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    evidence_required = EXCLUDED.evidence_required,
    sort_order = EXCLUDED.sort_order,
    status = EXCLUDED.status,
    updated_at = now();

CREATE INDEX idx_carbon_profiles_tenant_status
    ON carbon_profiles (tenant_id, status);
CREATE INDEX idx_carbon_profiles_user
    ON carbon_profiles (tenant_id, user_id);
CREATE INDEX idx_carbon_profiles_member
    ON carbon_profiles (tenant_id, fpo_member_profile_id);
CREATE INDEX idx_carbon_profiles_location
    ON carbon_profiles (tenant_id, village, taluka, district_name);
CREATE INDEX idx_carbon_farm_plots_profile
    ON carbon_farm_plots (tenant_id, carbon_profile_id);
CREATE INDEX idx_carbon_farm_plots_status
    ON carbon_farm_plots (tenant_id, status);
CREATE INDEX idx_carbon_soil_profiles_profile
    ON carbon_soil_profiles (tenant_id, carbon_profile_id);
CREATE INDEX idx_carbon_soil_profiles_plot
    ON carbon_soil_profiles (tenant_id, carbon_farm_plot_id);
CREATE INDEX idx_carbon_soil_profiles_test_date
    ON carbon_soil_profiles (tenant_id, test_date);
CREATE INDEX idx_carbon_activity_categories_status
    ON carbon_activity_categories (status, sort_order);
