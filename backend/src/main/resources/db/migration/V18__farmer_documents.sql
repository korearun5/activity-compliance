CREATE TABLE farmer_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    farmer_profile_id UUID NOT NULL REFERENCES farmer_profiles(id),
    document_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    verification_notes TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified_at TIMESTAMPTZ,
    verified_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_farmer_documents_type
        CHECK (document_type IN ('AADHAAR', 'LAND_RECORD', 'SOIL_REPORT', 'BANK_PROOF', 'OTHER')),
    CONSTRAINT chk_farmer_documents_status
        CHECK (status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_farmer_documents_file_url
        CHECK (length(btrim(file_url)) > 0),
    CONSTRAINT chk_farmer_documents_file_name
        CHECK (length(btrim(file_name)) > 0)
);

CREATE INDEX idx_farmer_documents_profile
    ON farmer_documents (tenant_id, farmer_profile_id, uploaded_at DESC);

CREATE INDEX idx_farmer_documents_tenant_status
    ON farmer_documents (tenant_id, status, uploaded_at ASC);

CREATE INDEX idx_farmer_documents_verified_by
    ON farmer_documents (tenant_id, verified_by_user_id)
    WHERE verified_by_user_id IS NOT NULL;
