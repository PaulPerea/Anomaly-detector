# Sistema Inteligente de Detección de Accesos Anómalos

Backend en **Quarkus + Java 21** con microservicio de IA en **Python + FastAPI**.  
Detecta comportamientos sospechosos usando reglas deterministas + **Isolation Forest (ML)**.

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Python | 3.11+ |
| Docker + Docker Compose | 24+ |
| Postman | cualquier versión reciente |
 
---

## Opción A — Arrancar con Docker Compose (recomendado)

```bash
# 1. Clonar / descomprimir el proyecto
cd anomaly-detector

# 2. Levantar todo con un solo comando
docker compose up --build

# 3. Esperar hasta ver en los logs:
#    anomaly-backend  | Listening on: http://0.0.0.0:8080
#    anomaly-ai-service | Application startup complete.
```

Los tres servicios quedan disponibles:
- **Quarkus Backend** → http://localhost:8080
- **AI Service (FastAPI)** → http://localhost:8081
- **PostgreSQL** → localhost:5432

Para parar todo: `docker compose down`  
Para parar y borrar datos: `docker compose down -v`

---

## Opción B — Arrancar en local (sin Docker)

### 1. PostgreSQL

```bash
# Con Docker solo para la BD
docker run -d --name pg-anomaly \
  -e POSTGRES_DB=anomaly_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. AI Service (Python)

```bash
cd ai-service
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --port 8081 --reload
```

Verificar: http://localhost:8081/health → `{"status":"ok"}`

### 3. Quarkus Backend

```bash
cd quarkus-backend
./mvnw quarkus:dev
```

O con Maven instalado:
```bash
mvn quarkus:dev
```

Verificar: http://localhost:8080/auth/login (debe devolver 400 sin body)

---

## Cómo probar con Postman

### Importar la colección

1. Abrir Postman
2. **Import** → seleccionar `anomaly-detector.postman_collection.json`
3. Las variables `baseUrl` y `aiUrl` ya están configuradas

### Flujo de prueba recomendado

#### Paso 1 — Autenticación
Ejecutar **Login Admin** → el token se guarda automáticamente en `adminToken`.  
Ejecutar **Login User (juan_perez)** → token guardado en `userToken`.

```
Credenciales de prueba:
  admin       / Password123!  (rol ADMIN)
  juan_perez  / Password123!  (rol USER)
  maria_gomez / Password123!  (rol USER)
  carlos_rivas / Password123! (rol USER)
```

#### Paso 2 — Datos sensibles
- **GET mis datos (USER)** → juan_perez solo ve sus propios registros
- **GET todos los datos (ADMIN)** → admin ve todos
- **GET datos sin token** → debe retornar 401

#### Paso 3 — Simular un ataque
En la carpeta **"3. Simular Ataque"**, ejecutar la request **⚠️ Consulta Repetida** unas 25 veces seguidas (puedes usar el runner de Postman: Run Collection → 25 iteraciones).

Esto dispara la regla de "frecuencia alta" del motor de IA y genera una alerta de `HIGH_RISK`.

#### Paso 4 — Revisar resultados
- **GET todos los logs** → ver todos los accesos registrados con su `riskLevel`
- **GET solo high-risk** → filtrado solo los peligrosos
- **GET todas las alertas** → ver las alertas generadas con `reason` y `riskLevel`
- **Acknowledge alerta** → marcar una alerta como revisada

#### Paso 5 — Probar la IA directamente

En la carpeta **"6. AI Service (Directo)"** puedes enviar escenarios manualmente:

| Request | queryCount | failed | offHours | Resultado esperado |
|---|---|---|---|---|
| Acceso NORMAL | 3 | 0 | false | `NORMAL` |
| Acceso SUSPICIOUS | 18 | 0 | false | `SUSPICIOUS` |
| Acceso HIGH_RISK | 45 | 6 | true | `HIGH_RISK` |

---

## Endpoints disponibles

### Auth (público)
```
POST /auth/login          → { token, username, role }
POST /auth/register       → { id, username, email, role }
```

### Datos sensibles (requiere JWT)
```
GET  /sensitive-data              → USER: sus datos / ADMIN: todos
GET  /sensitive-data/user/{id}    → ADMIN: datos de un usuario específico
```

### Audit Log (solo ADMIN)
```
GET  /access-log          → todos los logs de acceso
GET  /access-log/high-risk → solo SUSPICIOUS y HIGH_RISK
```

### Alertas (solo ADMIN)
```
GET  /alerts              → todas las alertas
GET  /alerts?pending=true → solo pendientes
GET  /alerts/user/{id}    → alertas de un usuario
PUT  /alerts/{id}/acknowledge → marcar como revisada
```

### AI Service (directo)
```
POST /ai/analyze          → analiza un acceso y retorna riskLevel
GET  /ai/info             → información del modelo y umbrales
GET  /health              → health check
```

---

## Lógica de detección

### Motor de Reglas (prioridad alta)

| Condición | Nivel |
|---|---|
| `queryCount >= 30` | HIGH_RISK |
| `queryCount >= 15` | SUSPICIOUS |
| `failedAttempts >= 4` | SUSPICIOUS (→ HIGH si ya era SUSPICIOUS) |
| `isOffHours = true` | SUSPICIOUS |
| `isOffHours = true` AND `queryCount >= 10` | HIGH_RISK |

### Isolation Forest (si reglas no detectan nada)

Features usados: `[query_count, failed_attempts, hour_of_day, is_off_hours]`

El modelo fue entrenado con 800 accesos normales + 100 anómalos sintéticos.  
Contamination = 8% → el 8% de los datos de entrenamiento se tratan como anomalías.

---

## Estructura del proyecto

```
anomaly-detector/
├── docker-compose.yml
├── anomaly-detector.postman_collection.json
├── quarkus-backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/security/
│       ├── domain/model/          ← User, SensitiveData, AccessLog, Alert
│       ├── application/service/   ← AuthService, AuditService, AlertService
│       ├── infrastructure/
│       │   ├── resource/          ← REST endpoints
│       │   └── client/            ← AiServiceClient (REST Client)
│       └── shared/
│           ├── dto/               ← Records de request/response
│           └── interceptor/       ← AuditInterceptor (registra cada acceso)
└── ai-service/
    ├── main.py                    ← FastAPI app
    ├── schemas.py                 ← Pydantic models
    ├── requirements.txt
    ├── Dockerfile
    └── model/
        ├── rules_engine.py        ← Reglas deterministas
        └── anomaly_detector.py    ← Isolation Forest
```

---

## Tecnologías

- **Java 21** + **Quarkus 3.9** (RESTEasy Reactive, SmallRye JWT, Hibernate ORM Panache)
- **PostgreSQL 16**
- **Python 3.12** + **FastAPI** + **scikit-learn** (Isolation Forest)
- **BCrypt** para hashing de contraseñas
- **Docker Compose** para orquestación
