from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import pandas as pd

from auto_mpg_pipeline import clean_auto_mpg, evaluate_models, generate_report_outputs


class AutoMpgPipelineTests(unittest.TestCase):
    def sample(self) -> pd.DataFrame:
        return pd.DataFrame(
            {
                "mpg": [18.0, 15.0, 36.0, 27.0, 30.0, 22.0, 41.5, 26.0, 19.0, 33.0, 24.0, 29.0],
                "cylinders": [8, 8, 4, 4, 4, 6, 4, 4, 6, 4, 4, 4],
                "displacement": [307, 350, 98, 140, 90, 198, 85, 151, 200, 91, 121, 97],
                "horsepower": [130, "?", 70, 86, 48, 95, 65, 90, 88, 67, 110, 75],
                "weight": [3504, 3693, 2120, 2790, 1985, 2833, 2020, 2678, 3060, 1995, 2600, 2171],
                "acceleration": [12.0, 11.5, 15.5, 15.6, 21.5, 15.5, 19.2, 16.5, 17.1, 16.2, 12.8, 16.0],
                "model_year": [70, 70, 80, 82, 78, 74, 80, 79, 81, 82, 76, 75],
                "origin": [1, 1, 3, 1, 2, 1, 3, 1, 1, 3, 2, 3],
                "car_name": ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"],
            }
        )

    def test_clean_replaces_origin_and_missing_horsepower(self) -> None:
        df = clean_auto_mpg(self.sample())

        self.assertIn("USA", set(df["origin"]))
        self.assertTrue(df["horsepower"].isna().any())

    def test_evaluate_models_returns_metrics(self) -> None:
        df = clean_auto_mpg(self.sample())

        results, _ = evaluate_models(df, quick=True)

        self.assertIn("rmse", results.columns)
        self.assertGreaterEqual(len(results), 5)

    def test_report_outputs_are_created(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            results = generate_report_outputs(Path(tmp), quick=True)

            self.assertFalse(results.empty)
            self.assertTrue((Path(tmp) / "model_results.csv").exists())
            self.assertTrue((Path(tmp) / "model_rmse_comparison.png").exists())


if __name__ == "__main__":
    unittest.main()

