-- 1. Cizinci podle roku a statniho obcanstvi.
SELECT
    fc.year,
    c.csu_name AS citizenship,
    SUM(fc.count_value) AS foreigners
FROM foreigner_counts fc
JOIN countries c ON c.country_id = fc.country_id
JOIN sexes s ON s.sex_id = fc.sex_id
WHERE s.code = 'TOTAL'
GROUP BY fc.year, c.csu_name
ORDER BY fc.year, foreigners DESC;

-- 2. Cizinci seskupeni podle GDP bucketu zemi obcanstvi.
SELECT
    year,
    gdp_bucket,
    SUM(total_foreigners) AS foreigners
FROM country_year_summary
GROUP BY year, gdp_bucket
ORDER BY year, foreigners DESC;

-- 3. Analyticka funkce: poradi zemi podle poctu cizincu v poslednim roce.
SELECT
    c.csu_name,
    s.year,
    s.total_foreigners,
    s.gdp_per_capita_usd,
    RANK() OVER (ORDER BY s.total_foreigners DESC) AS rank_latest_year
FROM country_year_summary s
JOIN countries c ON c.country_id = s.country_id
WHERE s.year = (SELECT MAX(year) FROM country_year_summary)
ORDER BY rank_latest_year
LIMIT 20;

-- 4. Rekurzivni dotaz: jednoduchy hierarchicky vypis OLAP modelu.
WITH RECURSIVE olap_tree(level, node, parent) AS (
    VALUES
        (1, 'Fact: foreigner_counts', NULL::TEXT),
        (2, 'Dim: countries', 'Fact: foreigner_counts'),
        (2, 'Dim: sexes', 'Fact: foreigner_counts'),
        (2, 'Dim: age_groups', 'Fact: foreigner_counts'),
        (2, 'Dim: regions', 'Fact: foreigner_counts'),
        (2, 'Fact: gdp_per_capita', 'Dim: countries')
    UNION ALL
    SELECT child.level + 1, repeat('  ', child.level - 1) || child.node, child.parent
    FROM olap_tree parent_node
    JOIN olap_tree child ON child.parent = parent_node.node
    WHERE parent_node.parent IS NULL
)
SELECT level, node FROM olap_tree ORDER BY level, node;

