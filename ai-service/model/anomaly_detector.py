import numpy as np
from dataclasses import dataclass
from sklearn.ensemble import IsolationForest
from schemas import AnalysisRequest


@dataclass
class MlResult:
    risk_level: str
    reason: str
    confidence: float


class AnomalyDetector:
    """
    Isolation Forest entrenado con datos sintéticos.
    Features: [query_count, failed_attempts, hour_of_day, is_off_hours]

    En producción, se reentrenarías con logs reales acumulados.
    """

    CONTAMINATION = 0.08  # ~8% de datos esperados como anómalos

    def __init__(self):
        self.model = IsolationForest(
            n_estimators=100,
            contamination=self.CONTAMINATION,
            max_samples="auto",
            random_state=42
        )
        self._train()

    def _train(self):
        """Genera dataset sintético de comportamiento normal vs anómalo."""
        rng = np.random.default_rng(42)

        # Accesos normales: pocas consultas, horario laboral, sin fallos
        normal = np.column_stack([
            rng.integers(1, 10, 800),        # query_count: 1-10
            rng.integers(0, 1, 800),         # failed_attempts: 0-1
            rng.integers(8, 18, 800),        # hour: 8-18 (horario laboral)
            np.zeros(800),                   # is_off_hours: 0
        ])

        # Accesos anómalos: muchas consultas, horario nocturno, varios fallos
        anomalous = np.column_stack([
            rng.integers(20, 60, 100),       # query_count: 20-60
            rng.integers(3, 10, 100),        # failed_attempts: 3-10
            rng.choice([1, 2, 3, 22, 23, 0], 100),  # hora nocturna
            np.ones(100),                    # is_off_hours: 1
        ])

        X = np.vstack([normal, anomalous])
        self.model.fit(X)

    def predict(self, req: AnalysisRequest) -> MlResult:
        hour = req.accessedAt.hour
        features = np.array([[
            req.queryCount,
            req.failedAttemptsLast10Min,
            hour,
            int(req.isOffHours)
        ]])

        prediction = self.model.predict(features)[0]   # 1=normal, -1=anomalía
        score = self.model.decision_function(features)[0]
        # score: más negativo = más anómalo
        # normalizamos: 0.0 = muy anómalo, 1.0 = muy normal
        norm_score = max(0.0, min(1.0, (score + 0.5) / 1.0))
        anomaly_confidence = round(1.0 - norm_score, 3)

        if prediction == -1:
            if anomaly_confidence > 0.7:
                return MlResult(
                    risk_level="HIGH_RISK",
                    reason=f"Patrón estadísticamente anómalo detectado por Isolation Forest "
                           f"(hora={hour}h, consultas={req.queryCount})",
                    confidence=anomaly_confidence
                )
            else:
                return MlResult(
                    risk_level="SUSPICIOUS",
                    reason=f"Comportamiento inusual detectado por ML (score={score:.3f})",
                    confidence=anomaly_confidence
                )

        return MlResult(
            risk_level="NORMAL",
            reason="",
            confidence=round(anomaly_confidence, 3)
        )
