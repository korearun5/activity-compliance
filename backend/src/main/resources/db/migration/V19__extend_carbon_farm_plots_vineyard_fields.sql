ALTER TABLE carbon_farm_plots
    ADD COLUMN variety VARCHAR(255),
    ADD COLUMN rootstock VARCHAR(255),
    ADD COLUMN planting_date DATE,
    ADD COLUMN block_code VARCHAR(100),
    ADD COLUMN spacing VARCHAR(50),
    ADD COLUMN row_count INTEGER;

CREATE INDEX idx_carbon_farm_plots_block_code
    ON carbon_farm_plots (tenant_id, block_code);
