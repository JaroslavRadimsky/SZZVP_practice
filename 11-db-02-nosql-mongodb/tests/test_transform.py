from __future__ import annotations

import unittest
from pathlib import Path
import sys

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from data_sources import clean_foreigners
from load_mongo import gdp_bucket


class TransformTests(unittest.TestCase):
    def test_clean_foreigners_maps_country_and_sex(self) -> None:
        raw = pd.DataFrame(
            {
                "Pohlaví": ["Ženy"],
                "Typ pobytu": ["Celkem"],
                "Státní občanství-Země": ["Slovensko"],
                "Území": ["Česko"],
                "CASQD": ["31.12.2020"],
                "Hodnota": ["77"],
            }
        )

        cleaned = clean_foreigners(raw)

        self.assertEqual("SVK", cleaned.iloc[0]["iso3"])
        self.assertEqual("F", cleaned.iloc[0]["sex_code"])

    def test_gdp_bucket(self) -> None:
        self.assertEqual("nizke HDP", gdp_bucket(5000))
        self.assertEqual("stredni HDP", gdp_bucket(15000))
        self.assertEqual("vysoke HDP", gdp_bucket(35000))


if __name__ == "__main__":
    unittest.main()

