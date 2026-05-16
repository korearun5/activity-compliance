ALTER TABLE carbon_profiles
    DROP CONSTRAINT IF EXISTS chk_carbon_profiles_language;

ALTER TABLE carbon_profiles
    DROP COLUMN IF EXISTS language_preference;
