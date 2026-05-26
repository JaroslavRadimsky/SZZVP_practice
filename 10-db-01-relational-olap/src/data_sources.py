from __future__ import annotations

import json
from urllib.request import urlopen
from pathlib import Path

import pandas as pd

CSU_URL = "https://data.csu.gov.cz/opendata/sady/CIZ002/distribuce/csv"
WORLD_BANK_URL = "https://api.worldbank.org/v2/country/all/indicator/NY.GDP.PCAP.CD?format=json&per_page=20000"

COUNTRY_MAP = {
    "Ukrajina": "UKR",
    "Slovensko": "SVK",
    "Vietnam": "VNM",
    "Rusko": "RUS",
    "Německo": "DEU",
    "Polsko": "POL",
    "Bulharsko": "BGR",
    "Rumunsko": "ROU",
    "Mongolsko": "MNG",
    "Moldavsko": "MDA",
    "Čína": "CHN",
    "Indie": "IND",
    "Spojené státy": "USA",
    "Spojené státy americké": "USA",
    "Kazachstán": "KAZ",
    "Bělorusko": "BLR",
    "Maďarsko": "HUN",
    "Rakousko": "AUT",
    "Francie": "FRA",
    "Itálie": "ITA",
    "Španělsko": "ESP",
    "Spojené království": "GBR",
    "Turecko": "TUR",
    "Srbsko": "SRB",
    "Chorvatsko": "HRV",
    "Bosna a Hercegovina": "BIH",
    "Severní Makedonie": "MKD",
    "Arménie": "ARM",
    "Ázerbájdžán": "AZE",
    "Gruzie": "GEO",
    "Kanada": "CAN",
    "Japonsko": "JPN",
    "Korejská republika": "KOR",
    "Filipíny": "PHL",
    "Uzbekistán": "UZB",
    "Kyrgyzstán": "KGZ",
    "Egypt": "EGY",
    "Sýrie": "SYR",
    "Irák": "IRQ",
    "Írán": "IRN",
    "Izrael": "ISR",
    "Nizozemsko": "NLD",
    "Belgie": "BEL",
    "Švýcarsko": "CHE",
    "Řecko": "GRC",
    "Portugalsko": "PRT",
}


def project_root() -> Path:
    return Path(__file__).resolve().parents[1]


def workspace_root() -> Path:
    return Path(__file__).resolve().parents[2]


def raw_dir() -> Path:
    path = project_root() / "data" / "raw"
    path.mkdir(parents=True, exist_ok=True)
    return path


def read_foreigners(limit_rows: int | None = None) -> pd.DataFrame:
    local = raw_dir() / "CIZ002.csv"
    if not local.exists():
        df = pd.read_csv(CSU_URL)
        df.to_csv(local, index=False)
    else:
        df = pd.read_csv(local)
    if limit_rows:
        df = df.head(limit_rows)
    return clean_foreigners(df)


def read_gdp() -> pd.DataFrame:
    local = raw_dir() / "world_bank_gdp_per_capita.json"
    if not local.exists():
        with urlopen(WORLD_BANK_URL) as response:
            payload = json.loads(response.read().decode("utf-8"))
        local.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
        records = payload[1]
    else:
        records = json.loads(local.read_text(encoding="utf-8"))[1]
    return clean_gdp(pd.DataFrame(records))


def clean_foreigners(df: pd.DataFrame) -> pd.DataFrame:
    required = {
        "Pohlaví",
        "Typ pobytu",
        "Státní občanství-Země",
        "STOBCAN5.STOBCAN2",
        "Území",
        "Uz0",
        "CASQD",
        "Hodnota",
    }
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"CSU dataset missing columns: {sorted(missing)}")

    cleaned = df.copy()
    cleaned["country_name"] = cleaned["Státní občanství-Země"].fillna("").astype(str).str.strip()
    cleaned = cleaned[cleaned["country_name"] != ""]
    cleaned = cleaned[cleaned["Území"].fillna("").astype(str).str.strip().eq("Česko")]
    cleaned = cleaned[cleaned["Typ pobytu"].fillna("").astype(str).str.strip().eq("Celkem")]
    date = pd.to_datetime(cleaned["CASQD"], format="%Y-%m-%d", errors="coerce")
    date = date.fillna(pd.to_datetime(cleaned["CASQD"], format="%d.%m.%Y", errors="coerce"))
    if "Čtvrtletí" in cleaned.columns:
        date = date.fillna(pd.to_datetime(cleaned["Čtvrtletí"], format="%d.%m.%Y", errors="coerce"))
    cleaned["date"] = date
    cleaned = cleaned[cleaned["date"].dt.month.eq(12) & cleaned["date"].dt.day.eq(31)]
    cleaned["year"] = cleaned["date"].dt.year.astype(int)
    cleaned["count_value"] = pd.to_numeric(cleaned["Hodnota"], errors="coerce").fillna(0).astype(int)
    cleaned["sex_label"] = cleaned["Pohlaví"].fillna("Celkem").astype(str).str.strip()
    cleaned["sex_code"] = cleaned["sex_label"].map({"Celkem": "TOTAL", "Muži": "M", "Ženy": "F"}).fillna(cleaned["sex_label"])
    cleaned["region_code"] = cleaned["Uz0"].fillna("CZ").astype(str).str.strip()
    cleaned["region_label"] = cleaned["Území"].fillna("Česko").astype(str).str.strip()
    cleaned["age_group_code"] = "ALL"
    cleaned["age_group_label"] = "Celkem"
    cleaned["residence_type"] = cleaned["Typ pobytu"].fillna("Celkem").astype(str).str.strip()
    cleaned["csu_country_code"] = cleaned["STOBCAN5.STOBCAN2"].fillna("").astype(str).str.strip()
    cleaned["iso3"] = cleaned["country_name"].map(COUNTRY_MAP)
    return cleaned[
        [
            "country_name",
            "csu_country_code",
            "iso3",
            "year",
            "sex_code",
            "sex_label",
            "age_group_code",
            "age_group_label",
            "region_code",
            "region_label",
            "residence_type",
            "count_value",
        ]
    ].drop_duplicates()


def clean_gdp(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        return pd.DataFrame(columns=["iso3", "world_bank_name", "year", "gdp_per_capita_usd"])
    cleaned = pd.DataFrame(
        {
            "iso3": df["countryiso3code"],
            "world_bank_name": df["country"].apply(lambda item: item.get("value") if isinstance(item, dict) else None),
            "year": pd.to_numeric(df["date"], errors="coerce"),
            "gdp_per_capita_usd": pd.to_numeric(df["value"], errors="coerce"),
        }
    )
    cleaned = cleaned.dropna(subset=["iso3", "year"])
    cleaned = cleaned[cleaned["iso3"].astype(str).str.len().eq(3)]
    cleaned["year"] = cleaned["year"].astype(int)
    return cleaned
