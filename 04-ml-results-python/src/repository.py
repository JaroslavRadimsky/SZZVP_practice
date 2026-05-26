from __future__ import annotations

import json
import sqlite3
from pathlib import Path
from typing import Iterable

from models import Experiment, ModelResult, utc_now


class Repository:
    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.connection = sqlite3.connect(self.path)
        self.connection.row_factory = sqlite3.Row
        self.init_schema()

    def init_schema(self) -> None:
        self.connection.executescript(
            """
            PRAGMA foreign_keys = ON;
            CREATE TABLE IF NOT EXISTS experiments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                created_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS models (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                experiment_id INTEGER NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
                model_type TEXT NOT NULL CHECK(model_type IN ('RandomForest', 'SVC')),
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                parameters_json TEXT NOT NULL,
                result REAL NOT NULL,
                created_at TEXT NOT NULL
            );
            """
        )
        self.connection.commit()

    def close(self) -> None:
        self.connection.close()

    def add_experiment(self, experiment: Experiment) -> int:
        cursor = self.connection.execute(
            "INSERT INTO experiments(name, description, created_at) VALUES (?, ?, ?)",
            (experiment.name, experiment.description, experiment.created_at or utc_now()),
        )
        self.connection.commit()
        return int(cursor.lastrowid)

    def list_experiments(self) -> list[Experiment]:
        rows = self.connection.execute("SELECT * FROM experiments ORDER BY created_at DESC, id DESC").fetchall()
        return [Experiment(row["id"], row["name"], row["description"], row["created_at"]) for row in rows]

    def add_model(self, model: ModelResult) -> int:
        self._validate_model(model)
        cursor = self.connection.execute(
            """
            INSERT INTO models(experiment_id, model_type, name, description, parameters_json, result, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                model.experiment_id,
                model.model_type,
                model.name,
                model.description,
                json.dumps(model.parameters, ensure_ascii=True),
                model.result,
                model.created_at or utc_now(),
            ),
        )
        self.connection.commit()
        return int(cursor.lastrowid)

    def list_models(
        self,
        experiment_id: int,
        model_type: str | None = None,
        sort_desc: bool = True,
    ) -> list[ModelResult]:
        sql = "SELECT * FROM models WHERE experiment_id = ?"
        params: list[object] = [experiment_id]
        if model_type:
            sql += " AND model_type = ?"
            params.append(model_type)
        sql += " ORDER BY result " + ("DESC" if sort_desc else "ASC")
        rows = self.connection.execute(sql, params).fetchall()
        return [self._row_to_model(row) for row in rows]

    @staticmethod
    def _row_to_model(row: sqlite3.Row) -> ModelResult:
        return ModelResult(
            id=row["id"],
            experiment_id=row["experiment_id"],
            model_type=row["model_type"],
            name=row["name"],
            description=row["description"],
            parameters=json.loads(row["parameters_json"]),
            result=row["result"],
            created_at=row["created_at"],
        )

    @staticmethod
    def _validate_model(model: ModelResult) -> None:
        required = {
            "RandomForest": {"n_estimators", "max_depth", "criterion"},
            "SVC": {"c", "kernel", "gamma"},
        }
        if model.model_type not in required:
            raise ValueError("Neznamy typ modelu.")
        missing = required[model.model_type] - set(model.parameters)
        if missing:
            raise ValueError(f"Chybi parametry: {', '.join(sorted(missing))}")

