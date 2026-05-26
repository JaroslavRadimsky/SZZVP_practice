from __future__ import annotations

import os
from pathlib import Path

import psycopg

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://szzvp:szzvp@localhost:15432/szzvp_db")
ROOT = Path(__file__).resolve().parents[1]


def ensure(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def main() -> None:
    with psycopg.connect(DATABASE_URL) as connection:
        tables = ["countries", "foreigner_counts", "gdp_per_capita", "country_year_summary"]
        for table in tables:
            count = connection.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            ensure(count > 0, f"Table {table} is empty.")

    ensure((ROOT / "output" / "top_countries_latest_year.csv").exists(), "Missing CSV report.")
    ensure((ROOT / "output" / "top_countries_latest_year.png").exists(), "Missing PNG report.")
    print("OK")


if __name__ == "__main__":
    main()

