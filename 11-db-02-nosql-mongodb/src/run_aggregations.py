from __future__ import annotations

import os
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd
from pymongo import MongoClient

MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27018")
DB_NAME = os.getenv("MONGO_DB", "szzvp_nosql")
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output"
OUTPUT.mkdir(exist_ok=True)


def collection():
    return MongoClient(MONGO_URL)[DB_NAME]["country_year_foreigners"]


def top_countries_pipeline(latest_year: int) -> list[dict]:
    return [
        {"$match": {"year": latest_year}},
        {"$unwind": "$observations"},
        {"$match": {"observations.sex_code": "TOTAL"}},
        {
            "$project": {
                "_id": 0,
                "country": "$country.csu_name",
                "year": 1,
                "foreigners": "$observations.count",
                "gdp_per_capita_usd": 1,
            }
        },
        {"$sort": {"foreigners": -1}},
        {"$limit": 15},
    ]


def gdp_bucket_pipeline() -> list[dict]:
    return [
        {"$unwind": "$observations"},
        {"$match": {"observations.sex_code": "TOTAL"}},
        {
            "$group": {
                "_id": {"year": "$year", "gdp_bucket": "$gdp_bucket"},
                "foreigners": {"$sum": "$observations.count"},
            }
        },
        {"$project": {"_id": 0, "year": "$_id.year", "gdp_bucket": "$_id.gdp_bucket", "foreigners": 1}},
        {"$sort": {"year": 1, "foreigners": -1}},
    ]


def sex_breakdown_pipeline(latest_year: int) -> list[dict]:
    return [
        {"$match": {"year": latest_year}},
        {"$unwind": "$observations"},
        {"$match": {"observations.sex_code": {"$in": ["M", "F"]}}},
        {
            "$group": {
                "_id": "$observations.sex",
                "foreigners": {"$sum": "$observations.count"},
            }
        },
        {"$project": {"_id": 0, "sex": "$_id", "foreigners": 1}},
        {"$sort": {"sex": 1}},
    ]


def run() -> None:
    coll = collection()
    latest = coll.find_one(sort=[("year", -1)])["year"]
    top = pd.DataFrame(coll.aggregate(top_countries_pipeline(latest)))
    by_bucket = pd.DataFrame(coll.aggregate(gdp_bucket_pipeline()))
    by_sex = pd.DataFrame(coll.aggregate(sex_breakdown_pipeline(latest)))

    top.to_csv(OUTPUT / "top_countries_latest_year.csv", index=False)
    by_bucket.to_csv(OUTPUT / "foreigners_by_gdp_bucket.csv", index=False)
    by_sex.to_csv(OUTPUT / "sex_breakdown_latest_year.csv", index=False)

    plt.figure(figsize=(11, 6))
    plt.barh(top["country"], top["foreigners"])
    plt.gca().invert_yaxis()
    plt.title("MongoDB agregace: top statni obcanstvi")
    plt.xlabel("Pocet cizincu")
    plt.tight_layout()
    plt.savefig(OUTPUT / "top_countries_latest_year.png", dpi=140)
    plt.close()

    if not by_sex.empty:
        plt.figure(figsize=(6, 5))
        plt.bar(by_sex["sex"], by_sex["foreigners"])
        plt.title("Rozpad podle pohlavi")
        plt.ylabel("Pocet cizincu")
        plt.tight_layout()
        plt.savefig(OUTPUT / "sex_breakdown_latest_year.png", dpi=140)
        plt.close()

    print(f"Aggregation outputs written to {OUTPUT}")


if __name__ == "__main__":
    run()

