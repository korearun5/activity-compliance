ALTER TABLE seasonal_crop_plans
    ADD COLUMN crop_year VARCHAR(20),
    ADD COLUMN expected_yield_quintals NUMERIC(14,4),
    ADD COLUMN confirmed_at TIMESTAMPTZ;

UPDATE seasonal_crop_plans plan
SET crop_year = COALESCE(season.season_year::TEXT, EXTRACT(YEAR FROM plan.created_at)::TEXT)
FROM crop_seasons season
WHERE plan.season_id = season.id
  AND plan.crop_year IS NULL;

UPDATE seasonal_crop_plans
SET crop_year = EXTRACT(YEAR FROM created_at)::TEXT
WHERE crop_year IS NULL;

UPDATE seasonal_crop_plans
SET confirmed_at = updated_at
WHERE status = 'CONFIRMED'
  AND confirmed_at IS NULL;

ALTER TABLE seasonal_crop_plans
    ALTER COLUMN crop_year SET NOT NULL;

CREATE INDEX idx_seasonal_crop_plans_confirmed_at
    ON seasonal_crop_plans (tenant_id, confirmed_at);
