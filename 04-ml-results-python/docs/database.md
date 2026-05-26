# Databazova dokumentace

Databaze je SQLite soubor `ml_results.sqlite3` vytvoreny pri prvnim spusteni.

## Tabulky

### experiments

- `id` INTEGER PRIMARY KEY
- `name` TEXT NOT NULL
- `description` TEXT NOT NULL
- `created_at` TEXT NOT NULL

### models

- `id` INTEGER PRIMARY KEY
- `experiment_id` INTEGER NOT NULL, FK na `experiments.id`
- `model_type` TEXT NOT NULL (`RandomForest` nebo `SVC`)
- `name` TEXT NOT NULL
- `description` TEXT NOT NULL
- `parameters_json` TEXT NOT NULL
- `result` REAL NOT NULL
- `created_at` TEXT NOT NULL

Parametry modelu jsou ulozene jako JSON objekt. Aplikace uklada minimalne tri parametry:

- RandomForest: `n_estimators`, `max_depth`, `criterion`
- SVC: `c`, `kernel`, `gamma`

