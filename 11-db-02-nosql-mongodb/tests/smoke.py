from __future__ import annotations

import os
from pathlib import Path

from pymongo import MongoClient

MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27018")
DB_NAME = os.getenv("MONGO_DB", "szzvp_nosql")
ROOT = Path(__file__).resolve().parents[1]


def ensure(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def main() -> None:
    coll = MongoClient(MONGO_URL)[DB_NAME]["country_year_foreigners"]
    ensure(coll.count_documents({}) > 0, "Collection is empty.")
    ensure(coll.count_documents({"observations.sex_code": "TOTAL"}) > 0, "Missing TOTAL observations.")
    ensure((ROOT / "output" / "top_countries_latest_year.csv").exists(), "Missing CSV output.")
    ensure((ROOT / "output" / "top_countries_latest_year.png").exists(), "Missing PNG output.")
    print("OK")


if __name__ == "__main__":
    main()

