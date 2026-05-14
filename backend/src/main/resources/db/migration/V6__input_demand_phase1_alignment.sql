ALTER TABLE input_demand_estimates
    ADD COLUMN recommended_quantity_per_acre NUMERIC(14,4),
    ADD COLUMN total_demand_quantity NUMERIC(14,4),
    ADD COLUMN buffer_percent NUMERIC(5,2),
    ADD COLUMN buffer_quantity NUMERIC(14,4),
    ADD COLUMN final_demand_quantity NUMERIC(14,4);

UPDATE input_demand_estimates
SET recommended_quantity_per_acre = estimated_quantity,
    total_demand_quantity = estimated_quantity,
    buffer_percent = 5.00,
    buffer_quantity = 0,
    final_demand_quantity = estimated_quantity
WHERE recommended_quantity_per_acre IS NULL;

ALTER TABLE input_demand_estimates
    ALTER COLUMN recommended_quantity_per_acre SET NOT NULL,
    ALTER COLUMN total_demand_quantity SET NOT NULL,
    ALTER COLUMN buffer_percent SET NOT NULL,
    ALTER COLUMN buffer_quantity SET NOT NULL,
    ALTER COLUMN final_demand_quantity SET NOT NULL;
