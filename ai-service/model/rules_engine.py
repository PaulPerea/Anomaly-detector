from dataclasses import dataclass
from schemas import AnalysisRequest


@dataclass
class RuleResult:
    risk_level: str
    reason: str
    confidence: float
    is_flagged: bool


class RulesEngine:
    """
    Motor de reglas deterministas — se ejecuta ANTES que el modelo ML.
    Casos de alto riesgo evidentes no necesitan ML para detectarse.
    """

    MAX_QUERIES_5MIN = 15
    MAX_FAILED_ATTEMPTS = 4
    MASSIVE_QUERY_THRESHOLD = 30  # consulta masiva de información

    def evaluate(self, req: AnalysisRequest) -> RuleResult:
        reasons = []
        risk_level = "NORMAL"
        confidence = 0.05

        # Regla 1: Demasiadas consultas en poco tiempo
        if req.queryCount >= self.MASSIVE_QUERY_THRESHOLD:
            reasons.append(f"Consulta masiva: {req.queryCount} accesos recientes")
            risk_level = "HIGH_RISK"
            confidence = 0.98

        elif req.queryCount >= self.MAX_QUERIES_5MIN:
            reasons.append(f"Frecuencia alta: {req.queryCount} consultas en 5 min")
            if risk_level != "HIGH_RISK":
                risk_level = "SUSPICIOUS"
                confidence = 0.80

        # Regla 2: Múltiples intentos fallidos
        if req.failedAttemptsLast10Min >= self.MAX_FAILED_ATTEMPTS:
            reasons.append(f"Múltiples fallos: {req.failedAttemptsLast10Min} intentos fallidos")
            if risk_level == "NORMAL":
                risk_level = "SUSPICIOUS"
                confidence = max(confidence, 0.85)
            elif risk_level == "SUSPICIOUS":
                risk_level = "HIGH_RISK"
                confidence = 0.95

        # Regla 3: Acceso fuera de horario habitual (8h–18h)
        if req.isOffHours:
            reasons.append("Acceso fuera del horario laboral (8h–18h)")
            if risk_level == "NORMAL":
                risk_level = "SUSPICIOUS"
                confidence = max(confidence, 0.65)
            else:
                confidence = min(confidence + 0.05, 1.0)

        # Regla 4: Combinación de factores (off-hours + consultas altas)
        if req.isOffHours and req.queryCount >= 10:
            if risk_level != "HIGH_RISK":
                risk_level = "HIGH_RISK"
                confidence = 0.92
                reasons.append("Combinación crítica: horario inusual + volumen alto")

        return RuleResult(
            risk_level=risk_level,
            reason=" | ".join(reasons) if reasons else "",
            confidence=confidence,
            is_flagged=risk_level != "NORMAL"
        )
