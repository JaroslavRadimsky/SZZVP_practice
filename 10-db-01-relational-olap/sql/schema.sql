DROP TABLE IF EXISTS country_year_summary;
DROP TABLE IF EXISTS foreigner_counts;
DROP TABLE IF EXISTS gdp_per_capita;
DROP TABLE IF EXISTS countries;
DROP TABLE IF EXISTS sexes;
DROP TABLE IF EXISTS age_groups;
DROP TABLE IF EXISTS regions;

CREATE TABLE countries (
    country_id SERIAL PRIMARY KEY,
    iso3 CHAR(3) UNIQUE,
    csu_name TEXT NOT NULL UNIQUE,
    world_bank_name TEXT
);

CREATE TABLE sexes (
    sex_id SERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL
);

CREATE TABLE age_groups (
    age_group_id SERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL
);

CREATE TABLE regions (
    region_id SERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL
);

CREATE TABLE gdp_per_capita (
    gdp_id SERIAL PRIMARY KEY,
    country_id INTEGER NOT NULL REFERENCES countries(country_id),
    year INTEGER NOT NULL,
    gdp_per_capita_usd NUMERIC(14, 2),
    UNIQUE (country_id, year)
);

CREATE TABLE foreigner_counts (
    count_id SERIAL PRIMARY KEY,
    country_id INTEGER NOT NULL REFERENCES countries(country_id),
    sex_id INTEGER NOT NULL REFERENCES sexes(sex_id),
    age_group_id INTEGER NOT NULL REFERENCES age_groups(age_group_id),
    region_id INTEGER NOT NULL REFERENCES regions(region_id),
    year INTEGER NOT NULL,
    residence_type TEXT NOT NULL DEFAULT 'Celkem',
    count_value INTEGER NOT NULL,
    UNIQUE (country_id, sex_id, age_group_id, region_id, year, residence_type)
);

CREATE INDEX idx_foreigner_counts_year ON foreigner_counts(year);
CREATE INDEX idx_foreigner_counts_country ON foreigner_counts(country_id);
CREATE INDEX idx_gdp_country_year ON gdp_per_capita(country_id, year);

CREATE OR REPLACE FUNCTION reject_negative_foreigner_count()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.count_value < 0 THEN
        RAISE EXCEPTION 'count_value must not be negative';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reject_negative_foreigner_count
BEFORE INSERT OR UPDATE ON foreigner_counts
FOR EACH ROW
EXECUTE FUNCTION reject_negative_foreigner_count();

CREATE TABLE country_year_summary (
    country_id INTEGER NOT NULL REFERENCES countries(country_id),
    year INTEGER NOT NULL,
    total_foreigners INTEGER NOT NULL,
    gdp_per_capita_usd NUMERIC(14, 2),
    gdp_bucket TEXT NOT NULL,
    rank_by_foreigners INTEGER NOT NULL,
    PRIMARY KEY (country_id, year)
);

CREATE OR REPLACE FUNCTION refresh_country_year_summary()
RETURNS VOID AS $$
BEGIN
    TRUNCATE country_year_summary;

    INSERT INTO country_year_summary (
        country_id,
        year,
        total_foreigners,
        gdp_per_capita_usd,
        gdp_bucket,
        rank_by_foreigners
    )
    SELECT
        fc.country_id,
        fc.year,
        SUM(fc.count_value)::INTEGER AS total_foreigners,
        MAX(g.gdp_per_capita_usd) AS gdp_per_capita_usd,
        CASE
            WHEN MAX(g.gdp_per_capita_usd) IS NULL THEN 'nezname HDP'
            WHEN MAX(g.gdp_per_capita_usd) < 10000 THEN 'nizke HDP'
            WHEN MAX(g.gdp_per_capita_usd) < 30000 THEN 'stredni HDP'
            ELSE 'vysoke HDP'
        END AS gdp_bucket,
        RANK() OVER (PARTITION BY fc.year ORDER BY SUM(fc.count_value) DESC)::INTEGER AS rank_by_foreigners
    FROM foreigner_counts fc
    LEFT JOIN gdp_per_capita g
        ON g.country_id = fc.country_id AND g.year = fc.year
    JOIN sexes s ON s.sex_id = fc.sex_id
    WHERE s.code = 'TOTAL'
    GROUP BY fc.country_id, fc.year;
END;
$$ LANGUAGE plpgsql;

