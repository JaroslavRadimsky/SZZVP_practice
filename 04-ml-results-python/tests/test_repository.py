import tempfile
import unittest
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from models import Experiment, ModelResult
from repository import Repository


class RepositoryTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tempdir = tempfile.TemporaryDirectory()
        self.repo = Repository(Path(self.tempdir.name) / "test.sqlite3")

    def tearDown(self) -> None:
        self.repo.close()
        self.tempdir.cleanup()

    def test_adds_experiment_and_model(self) -> None:
        exp_id = self.repo.add_experiment(Experiment(None, "Klasifikace", "Test"))
        self.repo.add_model(
            ModelResult(
                None,
                exp_id,
                "RandomForest",
                "RF baseline",
                "Prvni model",
                {"n_estimators": 100, "max_depth": 8, "criterion": "gini"},
                0.91,
            )
        )

        models = self.repo.list_models(exp_id)

        self.assertEqual(1, len(models))
        self.assertEqual("RF baseline", models[0].name)

    def test_filters_and_sorts_models(self) -> None:
        exp_id = self.repo.add_experiment(Experiment(None, "Experiment", ""))
        self.repo.add_model(ModelResult(None, exp_id, "SVC", "SVC", "", {"c": 1, "kernel": "rbf", "gamma": "scale"}, 0.8))
        self.repo.add_model(ModelResult(None, exp_id, "RandomForest", "RF", "", {"n_estimators": 100, "max_depth": 4, "criterion": "gini"}, 0.9))

        models = self.repo.list_models(exp_id, "RandomForest", sort_desc=True)

        self.assertEqual(["RF"], [model.name for model in models])

    def test_requires_three_parameters_for_known_model(self) -> None:
        exp_id = self.repo.add_experiment(Experiment(None, "Experiment", ""))

        with self.assertRaises(ValueError):
            self.repo.add_model(ModelResult(None, exp_id, "SVC", "Bad", "", {"c": 1}, 0.5))


if __name__ == "__main__":
    unittest.main()

