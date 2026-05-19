CREATE TABLE farmer_bank_details (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    farmer_profile_id UUID NOT NULL REFERENCES farmer_profiles(id),
    account_holder_name VARCHAR(180) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    upi_id VARCHAR(120),
    bank_name VARCHAR(180) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_farmer_bank_details_profile UNIQUE (tenant_id, farmer_profile_id),
    CONSTRAINT chk_farmer_bank_details_status
        CHECK (status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_farmer_bank_details_ifsc
        CHECK (ifsc_code ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),
    CONSTRAINT chk_farmer_bank_details_account_number
        CHECK (length(btrim(account_number)) > 0)
);

COMMENT ON TABLE farmer_bank_details IS
    'MVP bank details storage. Production must encrypt or tokenize sensitive bank identifiers before shared or live use.';
COMMENT ON COLUMN farmer_bank_details.account_number IS
    'Plain text for MVP only; production must encrypt or tokenize this value.';

CREATE INDEX idx_farmer_bank_details_tenant_status
    ON farmer_bank_details (tenant_id, status);

CREATE INDEX idx_farmer_bank_details_profile
    ON farmer_bank_details (tenant_id, farmer_profile_id);
