from __future__ import annotations

import unittest
from pathlib import Path
import sys

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from data_sources import clean_foreigners


class TransformTests(unittest.TestCase):
    def test_clean_foreigners_keeps_december_country_rows(self) -> None:
        raw = pd.DataFrame(
            {
                "Pohlaví": ["Celkem", "Muži"],
                "Typ pobytu": ["Celkem", "Celkem"],
                "Státní občanství-Země": ["Ukrajina", "Ukrajina"],
                "STOBCAN5.STOBCAN2": ["UKR", "UKR"],
                "Území": ["Česko", "Česko"],
                "Uz0": ["CZ", "CZ"],
                "CASQD": ["31.12.2019", "30.09.2019"],
                "Hodnota": ["100", "50"],
            }
        )

        cleaned = clean_foreigners(raw)

        self.assertEqual(1, len(cleaned))
        self.assertEqual("UKR", cleaned.iloc[0]["iso3"])
        self.assertEqual(2019, int(cleaned.iloc[0]["year"]))


if __name__ == "__main__":
    unittest.main()

