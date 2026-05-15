ALTER TABLE fpo_advisories
    ADD COLUMN category VARCHAR(80) NOT NULL DEFAULT 'AGRONOMY';

UPDATE fpo_advisories
SET channel = 'IN_APP'
WHERE channel <> 'IN_APP';

UPDATE fpo_advisories
SET target_type = 'ALL_MEMBERS',
    crop_id = NULL
WHERE target_type IN ('VILLAGE', 'MEMBER');

DROP INDEX IF EXISTS idx_fpo_advisories_target_member;
DROP INDEX IF EXISTS idx_fpo_advisories_target_village;

ALTER TABLE fpo_advisories
    DROP COLUMN IF EXISTS target_village,
    DROP COLUMN IF EXISTS target_member_id;

ALTER TABLE fpo_advisories
    ADD CONSTRAINT chk_fpo_advisories_category
        CHECK (category IN (
            'AGRONOMY',
            'PEST_DISEASE_MANAGEMENT',
            'SOIL_HEALTH',
            'WEATHER_ALERT'
        )),
    ADD CONSTRAINT chk_fpo_advisories_target_type
        CHECK (target_type IN ('ALL_MEMBERS', 'CROP')),
    ADD CONSTRAINT chk_fpo_advisories_target_crop
        CHECK (
            (target_type = 'ALL_MEMBERS' AND crop_id IS NULL)
            OR (target_type = 'CROP' AND crop_id IS NOT NULL)
        ),
    ADD CONSTRAINT chk_fpo_advisories_channel_phase1
        CHECK (channel = 'IN_APP');

CREATE TABLE fpo_advisory_images (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    advisory_id UUID NOT NULL REFERENCES fpo_advisories (id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    storage_key TEXT,
    original_filename VARCHAR(255),
    content_type VARCHAR(120),
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fpo_advisory_images_content_type
        CHECK (content_type IS NULL OR content_type LIKE 'image/%')
);

CREATE INDEX idx_fpo_advisory_images_advisory
    ON fpo_advisory_images (tenant_id, advisory_id, sort_order);
