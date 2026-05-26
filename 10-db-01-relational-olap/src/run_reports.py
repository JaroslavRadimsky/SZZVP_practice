from __future__ import annotations

import os
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd
import psycopg

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://szzvp:szzvp@localhost:15432/szzvp_db")
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output"
OUTPUT.mkdir(exist_ok=True)


def query_dataframe(connection: psycopg.Connection, sql: str) -> pd.DataFrame:
    with connection.cursor() as cursor:
        cursor.execute(sql)
        columns = [desc.name for desc in cursor.description]
        return pd.DataFrame(cursor.fetchall(), columns=columns)


def run() -> None:
    with psycopg.connect(DATABASE_URL) as connection:
        top_latest = query_dataframe(
            connection,
            """
            SELECT c.csu_name, s.year, s.total_foreigners, s.gdp_per_capita_usd
            FROM country_year_summary s
            JOIN countries c ON c.country_id = s.country_id
            WHERE s.year = (SELECT MAX(year) FROM country_year_summary)
            ORDER BY s.total_foreigners DESC
            LIMIT 15
            """,
        )
        by_bucket = query_dataframe(
            connection,
            """
            SELECT year, gdp_bucket, SUM(total_foreigners) AS foreigners
            FROM country_year_summary
            GROUP BY year, gdp_bucket
            ORDER BY year, gdp_bucket
            """,
        )
        scatter = query_dataframe(
            connection,
            """
            SELECT c.csu_name, s.year, s.total_foreigners, s.gdp_per_capita_usd
            FROM country_year_summary s
            JOIN countries c ON c.country_id = s.country_id
            WHERE s.gdp_per_capita_usd IS NOT NULL
              AND s.year = (SELECT MAX(year) FROM country_year_summary)
            """,
        )

    top_latest.to_csv(OUTPUT / "top_countries_latest_year.csv", index=False)
    by_bucket.to_csv(OUTPUT / "foreigners_by_gdp_bucket.csv", index=False)

    plt.figure(figsize=(11, 6))
    plt.barh(top_latest["csu_name"], top_latest["total_foreigners"])
    plt.gca().invert_yaxis()
    plt.title("Top statni obcanstvi cizincu v CR")
    plt.xlabel("Pocet cizincu")
    plt.tight_layout()
    plt.savefig(OUTPUT / "top_countries_latest_year.png", dpi=140)
    plt.close()

    if not scatter.empty:
        plt.figure(figsize=(8, 6))
        plt.scatter(scatter["gdp_per_capita_usd"], scatter["total_foreigners"])
        plt.title("HDP na hlavu vs. pocet cizincu v CR")
        plt.xlabel("GDP per capita (USD)")
        plt.ylabel("Pocet cizincu")
        plt.tight_layout()
        plt.savefig(OUTPUT / "gdp_vs_foreigners.png", dpi=140)
        plt.close()

    print(f"Reports written to {OUTPUT}")


if __name__ == "__main__":
    run()

