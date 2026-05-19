ALTER TABLE farmer_bank_details
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN verified_by_user_id UUID REFERENCES users(id),
    ADD COLUMN verification_notes TEXT;

CREATE INDEX idx_farmer_bank_details_verified_by
    ON farmer_bank_details (tenant_id, verified_by_user_id)
    WHERE verified_by_user_id IS NOT NULL;
