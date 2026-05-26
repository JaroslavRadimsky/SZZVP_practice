from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


@dataclass(frozen=True)
class Experiment:
    id: int | None
    name: str
    description: str
    created_at: str = ""


@dataclass(frozen=True)
class ModelResult:
    id: int | None
    experiment_id: int
    model_type: str
    name: str
    description: str
    parameters: dict[str, Any]
    result: float
    created_at: str = ""

