from __future__ import annotations

import os

from pymongo import MongoClient, UpdateOne

from data_sources import read_foreigners, read_gdp

MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27018")
DB_NAME = os.getenv("MONGO_DB", "szzvp_nosql")


def gdp_bucket(value: float | None) -> str:
    if value is None:
        return "nezname HDP"
    if value < 10000:
        return "nizke HDP"
    if value < 30000:
        return "stredni HDP"
    return "vysoke HDP"


def build_documents() -> list[dict]:
    foreigners = read_foreigners()
    gdp = read_gdp()
    gdp_lookup = {
        (row.iso3, int(row.year)): (None if row.gdp_per_capita_usd != row.gdp_per_capita_usd else float(row.gdp_per_capita_usd), row.world_bank_name)
        for row in gdp.itertuples(index=False)
    }

    documents: list[dict] = []
    for (country_name, year), group in foreigners.groupby(["country_name", "year"], sort=True):
        iso_values = group["iso3"].dropna().unique()
        iso3 = str(iso_values[0]) if len(iso_values) else None
        gdp_value, wb_name = gdp_lookup.get((iso3, int(year)), (None, None))
        observations = [
            {
                "sex_code": row.sex_code,
                "sex": row.sex,
                "age_group": row.age_group,
                "region": row.region,
                "residence_type": row.residence_type,
                "count": int(row.count),
            }
            for row in group.itertuples(index=False)
        ]
        document_id = f"{iso3 or country_name}-{int(year)}"
        documents.append(
            {
                "_id": document_id,
                "country": {
                    "csu_name": country_name,
                    "iso3": iso3,
                    "world_bank_name": wb_name,
                },
                "year": int(year),
                "gdp_per_capita_usd": gdp_value,
                "gdp_bucket": gdp_bucket(gdp_value),
                "observations": observations,
            }
        )
    return documents


def load() -> None:
    documents = build_documents()
    client = MongoClient(MONGO_URL)
    collection = client[DB_NAME]["country_year_foreigners"]
    collection.create_index([("country.iso3", 1), ("year", 1)])
    collection.create_index("gdp_bucket")
    operations = [UpdateOne({"_id": doc["_id"]}, {"$set": doc}, upsert=True) for doc in documents]
    if operations:
        collection.bulk_write(operations)
    print(f"Upserted {len(documents)} documents into {DB_NAME}.country_year_foreigners")


if __name__ == "__main__":
    load()
