from __future__ import annotations

import os
from pathlib import Path

import psycopg

from data_sources import read_foreigners, read_gdp

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://szzvp:szzvp@localhost:15432/szzvp_db")


def execute_schema(connection: psycopg.Connection) -> None:
    schema_path = Path(__file__).resolve().parents[1] / "sql" / "schema.sql"
    connection.execute(schema_path.read_text(encoding="utf-8"))


def upsert_dimension(connection: psycopg.Connection, table: str, key_column: str, rows: list[tuple]) -> dict[str, int]:
    if table == "sexes":
        sql = """
        INSERT INTO sexes(code, label) VALUES (%s, %s)
        ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
        RETURNING sex_id, code
        """
        id_column = "sex_id"
    elif table == "age_groups":
        sql = """
        INSERT INTO age_groups(code, label) VALUES (%s, %s)
        ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
        RETURNING age_group_id, code
        """
        id_column = "age_group_id"
    elif table == "regions":
        sql = """
        INSERT INTO regions(code, label) VALUES (%s, %s)
        ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
        RETURNING region_id, code
        """
        id_column = "region_id"
    else:
        raise ValueError(table)

    mapping: dict[str, int] = {}
    with connection.cursor() as cursor:
        for row in rows:
            cursor.execute(sql, row)
            returned = cursor.fetchone()
            mapping[returned[1]] = returned[0]
    return mapping


def load() -> None:
    foreigners = read_foreigners()
    gdp = read_gdp()

    with psycopg.connect(DATABASE_URL) as connection:
        with connection.transaction():
            execute_schema(connection)

            sex_map = upsert_dimension(
                connection,
                "sexes",
                "code",
                sorted(set(zip(foreigners["sex_code"], foreigners["sex_label"]))),
            )
            age_map = upsert_dimension(
                connection,
                "age_groups",
                "code",
                sorted(set(zip(foreigners["age_group_code"], foreigners["age_group_label"]))),
            )
            region_map = upsert_dimension(
                connection,
                "regions",
                "code",
                sorted(set(zip(foreigners["region_code"], foreigners["region_label"]))),
            )

            country_map: dict[str, int] = {}
            with connection.cursor() as cursor:
                for row in foreigners[["country_name", "iso3"]].drop_duplicates().itertuples(index=False):
                    wb_name = None
                    if row.iso3 and row.iso3 in set(gdp["iso3"]):
                        wb_name = gdp.loc[gdp["iso3"].eq(row.iso3), "world_bank_name"].dropna().head(1).squeeze()
                    cursor.execute(
                        """
                        INSERT INTO countries(iso3, csu_name, world_bank_name)
                        VALUES (%s, %s, %s)
                        ON CONFLICT (csu_name) DO UPDATE
                        SET iso3 = EXCLUDED.iso3,
                            world_bank_name = COALESCE(EXCLUDED.world_bank_name, countries.world_bank_name)
                        RETURNING country_id, csu_name
                        """,
                        (row.iso3 if row.iso3 == row.iso3 else None, row.country_name, wb_name if wb_name == wb_name else None),
                    )
                    returned = cursor.fetchone()
                    country_map[returned[1]] = returned[0]

                country_ids_by_iso = {
                    iso3: country_id
                    for iso3, country_id in connection.execute(
                        "SELECT iso3, country_id FROM countries WHERE iso3 IS NOT NULL"
                    ).fetchall()
                }

                for row in gdp.dropna(subset=["iso3"]).itertuples(index=False):
                    country_id = country_ids_by_iso.get(row.iso3)
                    if country_id is None:
                        continue
                    cursor.execute(
                        """
                        INSERT INTO gdp_per_capita(country_id, year, gdp_per_capita_usd)
                        VALUES (%s, %s, %s)
                        ON CONFLICT (country_id, year) DO UPDATE
                        SET gdp_per_capita_usd = EXCLUDED.gdp_per_capita_usd
                        """,
                        (country_id, int(row.year), None if row.gdp_per_capita_usd != row.gdp_per_capita_usd else float(row.gdp_per_capita_usd)),
                    )

                for row in foreigners.itertuples(index=False):
                    cursor.execute(
                        """
                        INSERT INTO foreigner_counts(
                            country_id, sex_id, age_group_id, region_id, year, residence_type, count_value
                        )
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (country_id, sex_id, age_group_id, region_id, year, residence_type)
                        DO UPDATE SET count_value = EXCLUDED.count_value
                        """,
                        (
                            country_map[row.country_name],
                            sex_map[row.sex_code],
                            age_map[row.age_group_code],
                            region_map[row.region_code],
                            int(row.year),
                            row.residence_type,
                            int(row.count_value),
                        ),
                    )

                cursor.execute("SELECT refresh_country_year_summary()")

    print(f"Loaded {len(foreigners)} foreigner rows and {len(gdp)} GDP rows.")


if __name__ == "__main__":
    load()

