from __future__ import annotations

import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk

from models import Experiment, ModelResult
from repository import Repository


class MlResultsApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("ML Results")
        self.geometry("980x620")
        self.repo = Repository(Path(__file__).resolve().parents[1] / "ml_results.sqlite3")

        self.experiment_var = tk.StringVar()
        self.filter_var = tk.StringVar(value="Vse")
        self.sort_desc_var = tk.BooleanVar(value=True)
        self.model_type_var = tk.StringVar(value="RandomForest")

        self._build_ui()
        self.refresh_experiments()

    def _build_ui(self) -> None:
        root = ttk.Frame(self, padding=12)
        root.pack(fill=tk.BOTH, expand=True)
        root.columnconfigure(1, weight=1)
        root.rowconfigure(2, weight=1)

        exp_box = ttk.LabelFrame(root, text="Experiment", padding=10)
        exp_box.grid(row=0, column=0, sticky="nsew", padx=(0, 10))
        ttk.Label(exp_box, text="Jmeno").grid(row=0, column=0, sticky="w")
        self.exp_name = ttk.Entry(exp_box, width=28)
        self.exp_name.grid(row=1, column=0, sticky="ew", pady=(0, 8))
        ttk.Label(exp_box, text="Popis").grid(row=2, column=0, sticky="w")
        self.exp_description = ttk.Entry(exp_box, width=28)
        self.exp_description.grid(row=3, column=0, sticky="ew", pady=(0, 8))
        ttk.Button(exp_box, text="Ulozit experiment", command=self.add_experiment).grid(row=4, column=0, sticky="ew")

        model_box = ttk.LabelFrame(root, text="Model", padding=10)
        model_box.grid(row=0, column=1, sticky="nsew")
        for index in range(6):
            model_box.columnconfigure(index, weight=1)

        ttk.Label(model_box, text="Experiment").grid(row=0, column=0, sticky="w")
        self.experiment_combo = ttk.Combobox(model_box, textvariable=self.experiment_var, state="readonly")
        self.experiment_combo.grid(row=1, column=0, sticky="ew", padx=(0, 8))

        ttk.Label(model_box, text="Typ").grid(row=0, column=1, sticky="w")
        type_combo = ttk.Combobox(model_box, textvariable=self.model_type_var, values=["RandomForest", "SVC"], state="readonly")
        type_combo.grid(row=1, column=1, sticky="ew", padx=(0, 8))
        type_combo.bind("<<ComboboxSelected>>", lambda _event: self.update_parameter_labels())

        ttk.Label(model_box, text="Jmeno").grid(row=0, column=2, sticky="w")
        self.model_name = ttk.Entry(model_box)
        self.model_name.grid(row=1, column=2, sticky="ew", padx=(0, 8))

        ttk.Label(model_box, text="Popis").grid(row=0, column=3, sticky="w")
        self.model_description = ttk.Entry(model_box)
        self.model_description.grid(row=1, column=3, sticky="ew", padx=(0, 8))

        ttk.Label(model_box, text="Vysledek").grid(row=0, column=4, sticky="w")
        self.model_result = ttk.Entry(model_box)
        self.model_result.grid(row=1, column=4, sticky="ew")

        self.param_labels: list[ttk.Label] = []
        self.param_entries: list[ttk.Entry] = []
        for i in range(3):
            label = ttk.Label(model_box)
            label.grid(row=2, column=i, sticky="w", pady=(12, 0))
            entry = ttk.Entry(model_box)
            entry.grid(row=3, column=i, sticky="ew", padx=(0, 8))
            self.param_labels.append(label)
            self.param_entries.append(entry)

        ttk.Button(model_box, text="Ulozit model", command=self.add_model).grid(row=3, column=4, sticky="ew", pady=(12, 0))
        self.update_parameter_labels()

        filter_box = ttk.Frame(root)
        filter_box.grid(row=1, column=0, columnspan=2, sticky="ew", pady=10)
        ttk.Label(filter_box, text="Filtr typu").pack(side=tk.LEFT)
        ttk.Combobox(filter_box, textvariable=self.filter_var, values=["Vse", "RandomForest", "SVC"], state="readonly", width=18).pack(side=tk.LEFT, padx=8)
        ttk.Checkbutton(filter_box, text="Radit od nejlepsiho", variable=self.sort_desc_var).pack(side=tk.LEFT)
        ttk.Button(filter_box, text="Obnovit", command=self.refresh_models).pack(side=tk.LEFT, padx=8)

        columns = ("experiment", "type", "name", "result", "parameters")
        self.table = ttk.Treeview(root, columns=columns, show="headings")
        for col, title in zip(columns, ["Experiment", "Typ", "Jmeno", "Vysledek", "Parametry"]):
            self.table.heading(col, text=title)
            self.table.column(col, width=150 if col != "parameters" else 360)
        self.table.grid(row=2, column=0, columnspan=2, sticky="nsew")

    def selected_experiment_id(self) -> int | None:
        value = self.experiment_var.get()
        if not value:
            return None
        return int(value.split(":", 1)[0])

    def add_experiment(self) -> None:
        if not self.exp_name.get().strip():
            messagebox.showerror("Chyba", "Zadejte jmeno experimentu.")
            return
        self.repo.add_experiment(Experiment(None, self.exp_name.get().strip(), self.exp_description.get().strip()))
        self.exp_name.delete(0, tk.END)
        self.exp_description.delete(0, tk.END)
        self.refresh_experiments()

    def add_model(self) -> None:
        experiment_id = self.selected_experiment_id()
        if experiment_id is None:
            messagebox.showerror("Chyba", "Vyberte experiment.")
            return
        try:
            params = self.read_parameters()
            self.repo.add_model(
                ModelResult(
                    id=None,
                    experiment_id=experiment_id,
                    model_type=self.model_type_var.get(),
                    name=self.model_name.get().strip(),
                    description=self.model_description.get().strip(),
                    parameters=params,
                    result=float(self.model_result.get()),
                )
            )
            self.refresh_models()
        except Exception as exc:
            messagebox.showerror("Chyba", str(exc))

    def read_parameters(self) -> dict[str, str]:
        keys = self.parameter_keys()
        return {key: entry.get().strip() for key, entry in zip(keys, self.param_entries)}

    def parameter_keys(self) -> list[str]:
        if self.model_type_var.get() == "SVC":
            return ["c", "kernel", "gamma"]
        return ["n_estimators", "max_depth", "criterion"]

    def update_parameter_labels(self) -> None:
        defaults = {
            "RandomForest": ["100", "8", "gini"],
            "SVC": ["1.0", "rbf", "scale"],
        }
        for label, entry, key, value in zip(self.param_labels, self.param_entries, self.parameter_keys(), defaults[self.model_type_var.get()]):
            label.configure(text=key)
            entry.delete(0, tk.END)
            entry.insert(0, value)

    def refresh_experiments(self) -> None:
        experiments = self.repo.list_experiments()
        values = [f"{exp.id}: {exp.name}" for exp in experiments]
        self.experiment_combo.configure(values=values)
        if values and not self.experiment_var.get():
            self.experiment_var.set(values[0])
        self.refresh_models()

    def refresh_models(self) -> None:
        for item in self.table.get_children():
            self.table.delete(item)
        experiment_id = self.selected_experiment_id()
        if experiment_id is None:
            return
        model_type = None if self.filter_var.get() == "Vse" else self.filter_var.get()
        for model in self.repo.list_models(experiment_id, model_type, self.sort_desc_var.get()):
            self.table.insert(
                "",
                tk.END,
                values=(experiment_id, model.model_type, model.name, f"{model.result:.4f}", model.parameters),
            )


if __name__ == "__main__":
    MlResultsApp().mainloop()

