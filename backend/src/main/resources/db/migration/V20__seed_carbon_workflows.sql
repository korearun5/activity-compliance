INSERT INTO tenants (id, code, name, status, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000020'::uuid,
    'default',
    'Local Development Client',
    'ACTIVE',
    now(),
    now()
WHERE NOT EXISTS (SELECT 1 FROM tenants);

CREATE INDEX IF NOT EXISTS idx_workflow_definitions_tenant_domain_status
    ON workflow_definitions (tenant_id, domain_key, status);

WITH workflow_seed(code, name, duration_days) AS (
    VALUES
        ('CARBON_PRUNING', 'Pruning and residue management', 7),
        ('CARBON_IRRIGATION', 'Efficient irrigation practice', 7),
        ('CARBON_SPRAY', 'Biological spray application', 5),
        ('CARBON_COMPOST', 'Compost application', 10),
        ('CARBON_COVER_CROP', 'Cover crop establishment', 21)
),
tenant_workflows AS (
    SELECT
        t.id AS tenant_id,
        COALESCE(
            existing.id,
            (
                substr(md5(t.id::text || ':' || ws.code), 1, 8) || '-' ||
                substr(md5(t.id::text || ':' || ws.code), 9, 4) || '-' ||
                substr(md5(t.id::text || ':' || ws.code), 13, 4) || '-' ||
                substr(md5(t.id::text || ':' || ws.code), 17, 4) || '-' ||
                substr(md5(t.id::text || ':' || ws.code), 21, 12)
            )::uuid
        ) AS workflow_id,
        ws.code,
        ws.name,
        ws.duration_days
    FROM tenants t
    CROSS JOIN workflow_seed ws
    LEFT JOIN workflow_definitions existing
        ON existing.tenant_id = t.id
        AND existing.code = ws.code
        AND existing.version = 1
)
INSERT INTO workflow_definitions (
    id,
    tenant_id,
    code,
    name,
    domain_key,
    duration_days,
    version,
    status,
    created_at,
    updated_at
)
SELECT
    workflow_id,
    tenant_id,
    code,
    name,
    'CARBON',
    duration_days,
    1,
    'ACTIVE',
    now(),
    now()
FROM tenant_workflows
ON CONFLICT (tenant_id, code, version) DO NOTHING;

WITH workflow_seed(code, name, duration_days) AS (
    VALUES
        ('CARBON_PRUNING', 'Pruning and residue management', 7),
        ('CARBON_IRRIGATION', 'Efficient irrigation practice', 7),
        ('CARBON_SPRAY', 'Biological spray application', 5),
        ('CARBON_COMPOST', 'Compost application', 10),
        ('CARBON_COVER_CROP', 'Cover crop establishment', 21)
),
tenant_workflows AS (
    SELECT
        t.id AS tenant_id,
        existing.id AS workflow_id,
        ws.code
    FROM tenants t
    CROSS JOIN workflow_seed ws
    JOIN workflow_definitions existing
        ON existing.tenant_id = t.id
        AND existing.code = ws.code
        AND existing.version = 1
),
task_seed(workflow_code, task_code, title, sequence_number, offset_days, evidence_required) AS (
    VALUES
        ('CARBON_PRUNING', 'BEFORE_PHOTO', 'Capture before photo and block condition', 10, 0, true),
        ('CARBON_PRUNING', 'PRACTICE_DETAILS', 'Record pruning/residue quantity and method', 20, 1, true),
        ('CARBON_PRUNING', 'AFTER_PHOTO', 'Capture after photo or completion evidence', 30, 2, true),
        ('CARBON_IRRIGATION', 'BEFORE_PHOTO', 'Capture irrigation setup before practice', 10, 0, true),
        ('CARBON_IRRIGATION', 'PRACTICE_DETAILS', 'Record irrigation duration and water-saving method', 20, 1, true),
        ('CARBON_IRRIGATION', 'AFTER_PHOTO', 'Capture completion evidence', 30, 2, true),
        ('CARBON_SPRAY', 'BEFORE_PHOTO', 'Capture spray material and target block', 10, 0, true),
        ('CARBON_SPRAY', 'PRACTICE_DETAILS', 'Record biological spray quantity and coverage', 20, 1, true),
        ('CARBON_SPRAY', 'AFTER_PHOTO', 'Capture spray completion evidence', 30, 2, true),
        ('CARBON_COMPOST', 'BEFORE_PHOTO', 'Capture compost material and target block', 10, 0, true),
        ('CARBON_COMPOST', 'PRACTICE_DETAILS', 'Record compost quantity and application area', 20, 1, true),
        ('CARBON_COMPOST', 'AFTER_PHOTO', 'Capture compost application evidence', 30, 2, true),
        ('CARBON_COVER_CROP', 'BEFORE_PHOTO', 'Capture block before cover crop activity', 10, 0, true),
        ('CARBON_COVER_CROP', 'PRACTICE_DETAILS', 'Record seed quantity and area covered', 20, 3, true),
        ('CARBON_COVER_CROP', 'AFTER_PHOTO', 'Capture cover crop establishment evidence', 30, 14, true)
)
INSERT INTO workflow_tasks (
    id,
    workflow_definition_id,
    code,
    title,
    sequence_number,
    offset_days,
    evidence_required,
    created_at
)
SELECT
    (
        substr(md5(tw.workflow_id::text || ':' || ts.task_code), 1, 8) || '-' ||
        substr(md5(tw.workflow_id::text || ':' || ts.task_code), 9, 4) || '-' ||
        substr(md5(tw.workflow_id::text || ':' || ts.task_code), 13, 4) || '-' ||
        substr(md5(tw.workflow_id::text || ':' || ts.task_code), 17, 4) || '-' ||
        substr(md5(tw.workflow_id::text || ':' || ts.task_code), 21, 12)
    )::uuid,
    tw.workflow_id,
    ts.task_code,
    ts.title,
    ts.sequence_number,
    ts.offset_days,
    ts.evidence_required,
    now()
FROM tenant_workflows tw
JOIN task_seed ts ON ts.workflow_code = tw.code
ON CONFLICT (workflow_definition_id, code) DO NOTHING;
