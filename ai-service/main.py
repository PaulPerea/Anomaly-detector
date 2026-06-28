from fastapi import FastAPI
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import logging

from schemas import AnalysisRequest, AnalysisResponse
from model.rules_engine import RulesEngine
from model.anomaly_detector import AnomalyDetector

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(name)s — %(message)s")
logger = logging.getLogger("ai-service")

rules_engine = RulesEngine()
detector = AnomalyDetector()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("AI Service listo — modelo Isolation Forest entrenado")
    yield
    logger.info("AI Service apagando...")


app = FastAPI(
    title="Anomaly Detection AI Service",
    version="1.0.0",
    description="Microservicio de detección de accesos anómalos usando ML",
    lifespan=lifespan
)


@app.post("/ai/analyze", response_model=AnalysisResponse)
def analyze(req: AnalysisRequest) -> AnalysisResponse:
    """
    Analiza un acceso y retorna el nivel de riesgo.
    Flujo: Reglas deterministas → Isolation Forest (si no fue flaggeado por reglas)
    """
    logger.info(
        f"Analizando acceso | user={req.userId} ip={req.ipAddress} "
        f"resource={req.resource} queries={req.queryCount} "
        f"failed={req.failedAttemptsLast10Min} offHours={req.isOffHours}"
    )

    # Paso 1: motor de reglas (rápido y determinista)
    rule_result = rules_engine.evaluate(req)

    if rule_result.is_flagged:
        logger.warning(f"RULES ENGINE flag → {rule_result.risk_level}: {rule_result.reason}")
        return AnalysisResponse(
            riskLevel=rule_result.risk_level,
            reason=rule_result.reason,
            confidenceScore=rule_result.confidence
        )

    # Paso 2: Isolation Forest (solo si las reglas no detectaron nada)
    ml_result = detector.predict(req)

    if ml_result.risk_level != "NORMAL":
        logger.warning(f"ML flag → {ml_result.risk_level}: {ml_result.reason}")

    return AnalysisResponse(
        riskLevel=ml_result.risk_level,
        reason=ml_result.reason,
        confidenceScore=ml_result.confidence
    )


@app.get("/health")
def health():
    return {"status": "ok", "service": "ai-anomaly-detector"}


@app.get("/ai/info")
def info():
    return {
        "rules_thresholds": {
            "max_queries_5min": rules_engine.MAX_QUERIES_5MIN,
            "max_failed_attempts": rules_engine.MAX_FAILED_ATTEMPTS,
            "massive_query_threshold": rules_engine.MASSIVE_QUERY_THRESHOLD,
        },
        "ml_model": "IsolationForest",
        "contamination": detector.CONTAMINATION,
        "features": ["query_count", "failed_attempts", "hour_of_day", "is_off_hours"]
    }
