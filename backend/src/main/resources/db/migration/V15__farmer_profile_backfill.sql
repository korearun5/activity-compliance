CREATE TABLE IF NOT EXISTS farmer_profile_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_version VARCHAR(40) NOT NULL,
    source_table VARCHAR(80) NOT NULL,
    source_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    action VARCHAR(80) NOT NULL,
    farmer_profile_id UUID REFERENCES farmer_profiles(id),
    user_id UUID REFERENCES users(id),
    mobile_number VARCHAR(40),
    display_name VARCHAR(180),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_farmer_profile_migration_audit_source_action
        UNIQUE (migration_version, source_table, source_id, action)
);

CREATE INDEX IF NOT EXISTS idx_farmer_profile_migration_audit_action
    ON farmer_profile_migration_audit (migration_version, action);

CREATE INDEX IF NOT EXISTS idx_farmer_profile_migration_audit_source
    ON farmer_profile_migration_audit (source_table, source_id);

WITH fpo_source AS (
    SELECT
        m.id AS source_id,
        gen_random_uuid() AS farmer_profile_id,
        m.tenant_id,
        m.user_id,
        trim(m.display_name) AS display_name,
        trim(m.mobile_number) AS mobile_number,
        NULLIF(trim(m.alternate_mobile_number), '') AS alternate_mobile_number,
        NULLIF(trim(m.aadhaar_number), '') AS aadhaar_number,
        trim(m.village) AS village,
        trim(m.taluka) AS taluka,
        trim(m.district_name) AS district_name,
        trim(m.state_name) AS state_name,
        upper(replace(replace(trim(m.gender), '-', '_'), ' ', '_')) AS gender,
        m.date_of_birth,
        m.age,
        CASE upper(replace(replace(trim(m.farmer_category), '-', '_'), ' ', '_'))
            WHEN 'SEMI_MEDIUM' THEN 'SEMI_MEDIUM'
            WHEN 'SEMIMEDIUM' THEN 'SEMI_MEDIUM'
            ELSE upper(replace(replace(trim(m.farmer_category), '-', '_'), ' ', '_'))
        END AS farmer_category,
        CASE
            WHEN m.status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED') THEN m.status
            ELSE 'ACTIVE'
        END AS status
    FROM fpo_member_profiles m
    WHERE m.farmer_profile_id IS NULL
      AND m.user_id IS NOT NULL
), valid_fpo_source AS (
    SELECT *
    FROM fpo_source
    WHERE display_name <> ''
      AND mobile_number <> ''
      AND village <> ''
      AND taluka <> ''
      AND district_name <> ''
      AND state_name <> ''
      AND gender IN ('MALE', 'FEMALE', 'OTHER')
      AND farmer_category IN ('MARGINAL', 'SMALL', 'SEMI_MEDIUM', 'MEDIUM', 'LARGE')
      AND EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = fpo_source.user_id
            AND r.code = 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = fpo_source.user_id
            AND r.code <> 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM farmer_profiles fp
          WHERE fp.tenant_id = fpo_source.tenant_id
            AND fp.user_id = fpo_source.user_id
      )
), inserted AS (
    INSERT INTO farmer_profiles (
        id,
        tenant_id,
        user_id,
        display_name,
        mobile_number,
        alternate_mobile_number,
        aadhaar_number,
        village,
        taluka,
        district_name,
        state_name,
        gender,
        date_of_birth,
        age,
        farmer_category,
        status,
        created_by_user_id,
        created_at,
        updated_at
    )
    SELECT
        farmer_profile_id,
        tenant_id,
        user_id,
        display_name,
        mobile_number,
        alternate_mobile_number,
        aadhaar_number,
        village,
        taluka,
        district_name,
        state_name,
        gender,
        date_of_birth,
        age,
        farmer_category,
        status,
        NULL,
        now(),
        now()
    FROM valid_fpo_source
    ON CONFLICT (tenant_id, user_id) DO NOTHING
    RETURNING id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'fpo_member_profiles',
    source.source_id,
    source.tenant_id,
    'FPO_FARMER_PROFILE_CREATED_BY_USER',
    inserted.id,
    source.user_id,
    source.mobile_number,
    source.display_name,
    jsonb_build_object('reason', 'Created canonical farmer profile from FPO member user_id.')
FROM valid_fpo_source source
JOIN inserted ON inserted.id = source.farmer_profile_id
ON CONFLICT DO NOTHING;

WITH linkable AS (
    SELECT
        m.id AS source_id,
        m.tenant_id,
        m.user_id,
        m.mobile_number,
        m.display_name,
        fp.id AS farmer_profile_id
    FROM fpo_member_profiles m
    JOIN farmer_profiles fp
      ON fp.tenant_id = m.tenant_id
     AND fp.user_id = m.user_id
    WHERE m.farmer_profile_id IS NULL
      AND EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = m.user_id
            AND r.code = 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = m.user_id
            AND r.code <> 'FARMER'
      )
), linked AS (
    UPDATE fpo_member_profiles member
    SET farmer_profile_id = linkable.farmer_profile_id,
        updated_at = now()
    FROM linkable
    WHERE member.id = linkable.source_id
    RETURNING
        member.id AS source_id,
        member.tenant_id,
        member.user_id,
        member.mobile_number,
        member.display_name,
        member.farmer_profile_id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'fpo_member_profiles',
    source_id,
    tenant_id,
    'FPO_AUTO_LINKED_BY_USER',
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    jsonb_build_object('reason', 'Linked FPO member to canonical farmer profile by exact user_id.')
FROM linked
ON CONFLICT DO NOTHING;

INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'fpo_member_profiles',
    m.id,
    m.tenant_id,
    'FPO_FLAGGED_UNLINKED',
    m.user_id,
    m.mobile_number,
    m.display_name,
    jsonb_build_object(
        'reason', 'FPO member could not be linked to farmer_profiles by user_id.',
        'gender', m.gender,
        'farmerCategory', m.farmer_category
    )
FROM fpo_member_profiles m
WHERE m.farmer_profile_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    cp.id,
    cp.tenant_id,
    'CARBON_FLAGGED_USER_MEMBER_MISMATCH',
    cp.user_id,
    cp.mobile_number,
    cp.display_name,
    jsonb_build_object(
        'reason', 'Carbon profile user_id does not match selected FPO member user_id.',
        'fpoMemberProfileId', cp.fpo_member_profile_id
    )
FROM carbon_profiles cp
JOIN fpo_member_profiles member ON member.id = cp.fpo_member_profile_id
WHERE cp.participant_type = 'FARMER'
  AND cp.farmer_profile_id IS NULL
  AND cp.user_id IS NOT NULL
  AND member.user_id <> cp.user_id
ON CONFLICT DO NOTHING;

WITH carbon_source AS (
    SELECT
        cp.id AS source_id,
        gen_random_uuid() AS farmer_profile_id,
        cp.tenant_id,
        cp.user_id,
        trim(coalesce(NULLIF(cp.display_name, ''), member.display_name, users.display_name)) AS display_name,
        trim(coalesce(NULLIF(cp.mobile_number, ''), member.mobile_number, users.phone)) AS mobile_number,
        NULLIF(trim(coalesce(NULLIF(cp.alternate_mobile_number, ''), member.alternate_mobile_number)), '')
            AS alternate_mobile_number,
        NULLIF(trim(coalesce(NULLIF(cp.aadhaar_number, ''), member.aadhaar_number)), '')
            AS aadhaar_number,
        trim(coalesce(NULLIF(cp.village, ''), member.village)) AS village,
        trim(coalesce(NULLIF(cp.taluka, ''), member.taluka)) AS taluka,
        trim(coalesce(NULLIF(cp.district_name, ''), member.district_name)) AS district_name,
        trim(coalesce(NULLIF(cp.state_name, ''), member.state_name)) AS state_name,
        upper(replace(replace(trim(coalesce(NULLIF(cp.gender, ''), member.gender)), '-', '_'), ' ', '_'))
            AS gender,
        cp.age,
        CASE upper(replace(replace(trim(coalesce(NULLIF(cp.farmer_category, ''), member.farmer_category)), '-', '_'), ' ', '_'))
            WHEN 'SEMI_MEDIUM' THEN 'SEMI_MEDIUM'
            WHEN 'SEMIMEDIUM' THEN 'SEMI_MEDIUM'
            ELSE upper(replace(replace(trim(coalesce(NULLIF(cp.farmer_category, ''), member.farmer_category)), '-', '_'), ' ', '_'))
        END AS farmer_category
    FROM carbon_profiles cp
    JOIN users ON users.id = cp.user_id
    LEFT JOIN fpo_member_profiles member ON member.id = cp.fpo_member_profile_id
    WHERE cp.participant_type = 'FARMER'
      AND cp.farmer_profile_id IS NULL
      AND cp.user_id IS NOT NULL
      AND (member.id IS NULL OR member.user_id = cp.user_id)
), valid_carbon_source AS (
    SELECT *
    FROM carbon_source
    WHERE display_name <> ''
      AND mobile_number <> ''
      AND village <> ''
      AND taluka <> ''
      AND district_name <> ''
      AND state_name <> ''
      AND gender IN ('MALE', 'FEMALE', 'OTHER')
      AND farmer_category IN ('MARGINAL', 'SMALL', 'SEMI_MEDIUM', 'MEDIUM', 'LARGE')
      AND EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = carbon_source.user_id
            AND r.code = 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = carbon_source.user_id
            AND r.code <> 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM farmer_profiles fp
          WHERE fp.tenant_id = carbon_source.tenant_id
            AND fp.user_id = carbon_source.user_id
      )
), inserted AS (
    INSERT INTO farmer_profiles (
        id,
        tenant_id,
        user_id,
        display_name,
        mobile_number,
        alternate_mobile_number,
        aadhaar_number,
        village,
        taluka,
        district_name,
        state_name,
        gender,
        date_of_birth,
        age,
        farmer_category,
        status,
        created_by_user_id,
        created_at,
        updated_at
    )
    SELECT
        farmer_profile_id,
        tenant_id,
        user_id,
        display_name,
        mobile_number,
        alternate_mobile_number,
        aadhaar_number,
        village,
        taluka,
        district_name,
        state_name,
        gender,
        NULL,
        age,
        farmer_category,
        'ACTIVE',
        NULL,
        now(),
        now()
    FROM valid_carbon_source
    ON CONFLICT (tenant_id, user_id) DO NOTHING
    RETURNING id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    source.source_id,
    source.tenant_id,
    'CARBON_FARMER_PROFILE_CREATED_BY_USER',
    inserted.id,
    source.user_id,
    source.mobile_number,
    source.display_name,
    jsonb_build_object('reason', 'Created canonical farmer profile from Carbon profile user_id.')
FROM valid_carbon_source source
JOIN inserted ON inserted.id = source.farmer_profile_id
ON CONFLICT DO NOTHING;

WITH linkable AS (
    SELECT
        cp.id AS source_id,
        cp.tenant_id,
        cp.user_id,
        cp.mobile_number,
        cp.display_name,
        fp.id AS farmer_profile_id
    FROM carbon_profiles cp
    JOIN farmer_profiles fp
      ON fp.tenant_id = cp.tenant_id
     AND fp.user_id = cp.user_id
    LEFT JOIN fpo_member_profiles member ON member.id = cp.fpo_member_profile_id
    WHERE cp.participant_type = 'FARMER'
      AND cp.farmer_profile_id IS NULL
      AND cp.user_id IS NOT NULL
      AND (member.id IS NULL OR member.user_id = cp.user_id)
      AND EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = cp.user_id
            AND r.code = 'FARMER'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM user_roles ur
          JOIN roles r ON r.id = ur.role_id
          WHERE ur.user_id = cp.user_id
            AND r.code <> 'FARMER'
      )
), linked AS (
    UPDATE carbon_profiles profile
    SET farmer_profile_id = linkable.farmer_profile_id,
        updated_at = now()
    FROM linkable
    WHERE profile.id = linkable.source_id
    RETURNING
        profile.id AS source_id,
        profile.tenant_id,
        profile.user_id,
        profile.mobile_number,
        profile.display_name,
        profile.farmer_profile_id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    source_id,
    tenant_id,
    'CARBON_AUTO_LINKED_BY_USER',
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    jsonb_build_object('reason', 'Linked Carbon profile to canonical farmer profile by exact user_id.')
FROM linked
ON CONFLICT DO NOTHING;

WITH linkable AS (
    SELECT
        cp.id AS source_id,
        cp.tenant_id,
        cp.user_id,
        cp.mobile_number,
        cp.display_name,
        member.farmer_profile_id
    FROM carbon_profiles cp
    JOIN fpo_member_profiles member ON member.id = cp.fpo_member_profile_id
    WHERE cp.participant_type = 'FARMER'
      AND cp.farmer_profile_id IS NULL
      AND cp.user_id IS NULL
      AND member.farmer_profile_id IS NOT NULL
), linked AS (
    UPDATE carbon_profiles profile
    SET farmer_profile_id = linkable.farmer_profile_id,
        updated_at = now()
    FROM linkable
    WHERE profile.id = linkable.source_id
    RETURNING
        profile.id AS source_id,
        profile.tenant_id,
        profile.user_id,
        profile.mobile_number,
        profile.display_name,
        profile.farmer_profile_id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    source_id,
    tenant_id,
    'CARBON_AUTO_LINKED_BY_FPO_MEMBER',
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    jsonb_build_object('reason', 'Linked Carbon profile through selected FPO member profile.')
FROM linked
ON CONFLICT DO NOTHING;

WITH unique_farmer_mobile AS (
    SELECT tenant_id, mobile_number, (array_agg(id))[1] AS farmer_profile_id
    FROM farmer_profiles
    WHERE mobile_number IS NOT NULL
    GROUP BY tenant_id, mobile_number
    HAVING count(*) = 1
), unique_carbon_mobile AS (
    SELECT tenant_id, mobile_number
    FROM carbon_profiles
    WHERE participant_type = 'FARMER'
      AND farmer_profile_id IS NULL
      AND user_id IS NULL
      AND mobile_number IS NOT NULL
    GROUP BY tenant_id, mobile_number
    HAVING count(*) = 1
), linkable AS (
    SELECT
        cp.id AS source_id,
        cp.tenant_id,
        cp.user_id,
        cp.mobile_number,
        cp.display_name,
        farmer_mobile.farmer_profile_id
    FROM carbon_profiles cp
    JOIN unique_carbon_mobile carbon_mobile
      ON carbon_mobile.tenant_id = cp.tenant_id
     AND carbon_mobile.mobile_number = cp.mobile_number
    JOIN unique_farmer_mobile farmer_mobile
      ON farmer_mobile.tenant_id = cp.tenant_id
     AND farmer_mobile.mobile_number = cp.mobile_number
    WHERE cp.participant_type = 'FARMER'
      AND cp.farmer_profile_id IS NULL
      AND cp.user_id IS NULL
), linked AS (
    UPDATE carbon_profiles profile
    SET farmer_profile_id = linkable.farmer_profile_id,
        updated_at = now()
    FROM linkable
    WHERE profile.id = linkable.source_id
    RETURNING
        profile.id AS source_id,
        profile.tenant_id,
        profile.user_id,
        profile.mobile_number,
        profile.display_name,
        profile.farmer_profile_id
)
INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    source_id,
    tenant_id,
    'CARBON_AUTO_LINKED_BY_UNIQUE_MOBILE',
    farmer_profile_id,
    user_id,
    mobile_number,
    display_name,
    jsonb_build_object('reason', 'Linked Carbon profile by unique tenant/mobile candidate.')
FROM linked
ON CONFLICT DO NOTHING;

INSERT INTO farmer_profile_migration_audit (
    migration_version,
    source_table,
    source_id,
    tenant_id,
    action,
    user_id,
    mobile_number,
    display_name,
    details
)
SELECT
    'V15',
    'carbon_profiles',
    cp.id,
    cp.tenant_id,
    CASE
        WHEN cp.user_id IS NULL THEN 'CARBON_FLAGGED_NO_SAFE_USER_OR_MOBILE_MATCH'
        ELSE 'CARBON_FLAGGED_MISSING_REQUIRED_IDENTITY'
    END,
    cp.user_id,
    cp.mobile_number,
    cp.display_name,
    jsonb_build_object(
        'reason', 'Carbon farmer profile could not be safely linked to farmer_profiles.',
        'fpoMemberProfileId', cp.fpo_member_profile_id,
        'participantType', cp.participant_type
    )
FROM carbon_profiles cp
WHERE cp.participant_type = 'FARMER'
  AND cp.farmer_profile_id IS NULL
ON CONFLICT DO NOTHING;
