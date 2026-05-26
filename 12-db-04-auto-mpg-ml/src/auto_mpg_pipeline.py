from __future__ import annotations

from pathlib import Path
from urllib.request import urlopen
import warnings

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.decomposition import PCA
from sklearn.ensemble import RandomForestRegressor
from sklearn.exceptions import ConvergenceWarning
from sklearn.impute import SimpleImputer
from sklearn.inspection import permutation_importance
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.metrics import mean_absolute_error, r2_score
try:
    from sklearn.metrics import root_mean_squared_error
except ImportError:  # pragma: no cover
    root_mean_squared_error = None
from sklearn.model_selection import GridSearchCV, train_test_split
from sklearn.neural_network import MLPRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.svm import SVR

DATA_URL = "https://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data"
COLUMNS = [
    "mpg",
    "cylinders",
    "displacement",
    "horsepower",
    "weight",
    "acceleration",
    "model_year",
    "origin",
    "car_name",
]
NUMERIC_FEATURES = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year"]
CATEGORICAL_FEATURES = ["origin"]
TARGET = "mpg"


def project_root() -> Path:
    return Path(__file__).resolve().parents[1]


def load_auto_mpg(cache: bool = True) -> pd.DataFrame:
    raw_dir = project_root() / "data" / "raw"
    raw_dir.mkdir(parents=True, exist_ok=True)
    path = raw_dir / "auto-mpg.data"
    if cache and path.exists():
        raw = path.read_text(encoding="utf-8")
    else:
        with urlopen(DATA_URL) as response:
            raw = response.read().decode("utf-8")
        if cache:
            path.write_text(raw, encoding="utf-8")
    df = parse_auto_mpg(raw)
    return clean_auto_mpg(df)


def parse_auto_mpg(raw: str) -> pd.DataFrame:
    rows: list[list[object]] = []
    for line in raw.splitlines():
        if not line.strip():
            continue
        parts = line.strip().split(maxsplit=8)
        if len(parts) != 9:
            raise ValueError(f"Unexpected Auto MPG row: {line}")
        values = parts[:8] + [parts[8].strip().strip('"')]
        rows.append(values)
    return pd.DataFrame(rows, columns=COLUMNS).replace("?", np.nan)


def clean_auto_mpg(df: pd.DataFrame) -> pd.DataFrame:
    cleaned = df.copy()
    cleaned["horsepower"] = pd.to_numeric(cleaned["horsepower"], errors="coerce")
    cleaned["origin"] = cleaned["origin"].map({1: "USA", 2: "Europe", 3: "Japan"}).fillna("Unknown")
    for column in NUMERIC_FEATURES + [TARGET]:
        cleaned[column] = pd.to_numeric(cleaned[column], errors="coerce")
    cleaned["car_name"] = cleaned["car_name"].astype(str)
    cleaned = cleaned.dropna(subset=[TARGET])
    return cleaned


def split_features(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.Series]:
    return df[NUMERIC_FEATURES + CATEGORICAL_FEATURES], df[TARGET]


def build_preprocessor() -> ColumnTransformer:
    numeric = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )
    categorical = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("onehot", OneHotEncoder(handle_unknown="ignore")),
        ]
    )
    return ColumnTransformer(
        transformers=[
            ("num", numeric, NUMERIC_FEATURES),
            ("cat", categorical, CATEGORICAL_FEATURES),
        ]
    )


def model_spaces(quick: bool = False) -> dict[str, tuple[object, dict[str, list]]]:
    if quick:
        return {
            "LinearRegression": (LinearRegression(), {}),
            "Ridge": (Ridge(), {"model__alpha": [1.0]}),
            "SVR": (SVR(), {"model__C": [10.0], "model__epsilon": [0.2]}),
            "RandomForest": (
                RandomForestRegressor(random_state=42),
                {"model__n_estimators": [80], "model__max_depth": [8]},
            ),
            "MLPRegressor": (
                MLPRegressor(random_state=42, max_iter=800),
                {"model__hidden_layer_sizes": [(32,)], "model__activation": ["relu"], "model__alpha": [0.001]},
            ),
        }
    return {
        "LinearRegression": (LinearRegression(), {}),
        "Ridge": (Ridge(), {"model__alpha": [0.1, 1.0, 10.0]}),
        "SVR": (SVR(), {"model__C": [1.0, 10.0, 50.0], "model__epsilon": [0.1, 0.2], "model__gamma": ["scale", "auto"]}),
        "RandomForest": (
            RandomForestRegressor(random_state=42),
            {"model__n_estimators": [120, 220], "model__max_depth": [6, 10, None], "model__min_samples_leaf": [1, 3]},
        ),
        "MLPRegressor": (
            MLPRegressor(random_state=42, max_iter=1200, early_stopping=True),
            {"model__hidden_layer_sizes": [(32,), (64, 32)], "model__activation": ["relu", "tanh"], "model__alpha": [0.0001, 0.001]},
        ),
    }


def rmse(y_true: pd.Series, y_pred: np.ndarray) -> float:
    if root_mean_squared_error is not None:
        return float(root_mean_squared_error(y_true, y_pred))
    return float(np.sqrt(np.mean((np.asarray(y_true) - np.asarray(y_pred)) ** 2)))


def evaluate_models(df: pd.DataFrame, quick: bool = False) -> tuple[pd.DataFrame, dict[str, GridSearchCV]]:
    x, y = split_features(df)
    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42)
    results: list[dict] = []
    searches: dict[str, GridSearchCV] = {}

    for name, (model, grid) in model_spaces(quick).items():
        pipeline = Pipeline([("preprocess", build_preprocessor()), ("model", model)])
        search = GridSearchCV(
            pipeline,
            param_grid=grid,
            cv=3,
            scoring="neg_root_mean_squared_error",
            n_jobs=1,
        )
        with warnings.catch_warnings():
            warnings.filterwarnings("ignore", category=ConvergenceWarning)
            search.fit(x_train, y_train)
        pred = search.predict(x_test)
        results.append(
            {
                "model": name,
                "mae": mean_absolute_error(y_test, pred),
                "rmse": rmse(y_test, pred),
                "r2": r2_score(y_test, pred),
                "best_params": search.best_params_,
            }
        )
        searches[name] = search

    result_df = pd.DataFrame(results).sort_values("rmse").reset_index(drop=True)
    return result_df, searches


def pca_summary(df: pd.DataFrame) -> pd.DataFrame:
    numeric = df[NUMERIC_FEATURES].copy()
    numeric = pd.DataFrame(SimpleImputer(strategy="median").fit_transform(numeric), columns=NUMERIC_FEATURES)
    scaled = StandardScaler().fit_transform(numeric)
    pca = PCA(n_components=min(4, len(NUMERIC_FEATURES))).fit(scaled)
    return pd.DataFrame(
        {
            "component": [f"PC{i + 1}" for i in range(len(pca.explained_variance_ratio_))],
            "explained_variance_ratio": pca.explained_variance_ratio_,
        }
    )


def permutation_feature_importance(df: pd.DataFrame, model: GridSearchCV) -> pd.DataFrame:
    x, y = split_features(df)
    result = permutation_importance(model.best_estimator_, x, y, n_repeats=8, random_state=42, n_jobs=1)
    return pd.DataFrame(
        {
            "feature": x.columns,
            "importance_mean": result.importances_mean,
            "importance_std": result.importances_std,
        }
    ).sort_values("importance_mean", ascending=False)


def generate_report_outputs(output_dir: Path | None = None, quick: bool = False) -> pd.DataFrame:
    output = output_dir or project_root() / "output"
    output.mkdir(parents=True, exist_ok=True)
    df = load_auto_mpg()

    df.describe(include="all").to_csv(output / "descriptive_statistics.csv")
    df.isna().sum().rename("missing_count").to_csv(output / "missing_values.csv")
    df[NUMERIC_FEATURES + [TARGET]].corr().to_csv(output / "correlation_matrix.csv")

    pca = pca_summary(df)
    pca.to_csv(output / "pca_summary.csv", index=False)

    results, searches = evaluate_models(df, quick=quick)
    results.to_csv(output / "model_results.csv", index=False)

    best_name = results.iloc[0]["model"]
    importance = permutation_feature_importance(df, searches[best_name])
    importance.to_csv(output / "feature_importance.csv", index=False)

    plt.figure(figsize=(8, 5))
    plt.hist(df[TARGET], bins=20, color="#315f72")
    plt.title("Distribuce MPG")
    plt.xlabel("MPG")
    plt.ylabel("Pocet aut")
    plt.tight_layout()
    plt.savefig(output / "mpg_distribution.png", dpi=140)
    plt.close()

    corr = df[NUMERIC_FEATURES + [TARGET]].corr()
    plt.figure(figsize=(8, 6))
    plt.imshow(corr, cmap="coolwarm", vmin=-1, vmax=1)
    plt.colorbar(label="Korelace")
    plt.xticks(range(len(corr.columns)), corr.columns, rotation=45, ha="right")
    plt.yticks(range(len(corr.columns)), corr.columns)
    plt.title("Korelacni matice")
    plt.tight_layout()
    plt.savefig(output / "correlation_matrix.png", dpi=140)
    plt.close()

    plt.figure(figsize=(9, 5))
    plt.bar(results["model"], results["rmse"], color="#2f6f5e")
    plt.title("Porovnani modelu podle RMSE")
    plt.ylabel("RMSE")
    plt.xticks(rotation=25, ha="right")
    plt.tight_layout()
    plt.savefig(output / "model_rmse_comparison.png", dpi=140)
    plt.close()

    plt.figure(figsize=(8, 5))
    plt.barh(importance["feature"], importance["importance_mean"], xerr=importance["importance_std"])
    plt.gca().invert_yaxis()
    plt.title(f"Permutation feature importance: {best_name}")
    plt.tight_layout()
    plt.savefig(output / "feature_importance.png", dpi=140)
    plt.close()

    return results


if __name__ == "__main__":
    table = generate_report_outputs()
    print(table.to_string(index=False))
