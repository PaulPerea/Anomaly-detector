from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class AnalysisRequest(BaseModel):
    userId: str
    ipAddress: str
    resource: str
    queryCount: int
    accessedAt: datetime
    failedAttemptsLast10Min: int
    isOffHours: bool


class AnalysisResponse(BaseModel):
    riskLevel: str          # NORMAL | SUSPICIOUS | HIGH_RISK
    reason: str
    confidenceScore: float
