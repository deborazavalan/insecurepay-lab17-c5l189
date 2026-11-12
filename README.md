# InsecurePay — Laboratorio 17: Auditoría Forense y Remediación OWASP Top 10

**Curso:** IF0009 – Desarrollo de Software IV · Universidad de Costa Rica, Sede del Atlántico
**Estudiante:** Débora Zavala (C5L189)

Este repositorio contiene la solución del Laboratorio 17 sobre la plataforma **InsecurePay** (Spring Boot 3.3.4 + Java 21, Angular 19, H2 en memoria): auditoría forense, pruebas de concepto y remediación defensiva de cinco vulnerabilidades del OWASP Top 10:2021.

| Entregable | Ubicación |
| :--- | :--- |
| Bitácora pericial forense (5 hallazgos, CVSS, antes/después) | [`BITACORA_PERICIAL.md`](BITACORA_PERICIAL.md) |
| Evidencias de explotación y de bloqueo | [`evidencias/`](evidencias/) |
| Código vulnerable original (línea base) | Rama [`version-vulnerable`](../../tree/version-vulnerable) |
| Código remediado | Rama `main` |
| Pruebas de seguridad automatizadas | `backend/src/test/java/cr/ac/ucr/if0009/insecurepay/InsecurePaySecurityTest.java` |
| Scripts de PoC reproducibles | `scripts/poc.sh`, `scripts/xss-playwright.mjs` |
| Pipeline de compilación, pruebas y evidencias | `.github/workflows/auditoria.yml` |

## Hallazgos remediados

| ID | Vulnerabilidad | OWASP 2021 | CVSS v3.1 |
| :--- | :--- | :--- | :--- |
| HALLAZGO-01 | Inyección SQL en `/api/transacciones/buscar` | A03 (CWE-89) | 8.2 Alta |
| HALLAZGO-02 | IDOR / BOLA en cuentas, saldos y transferencias | A01 (CWE-639) | 8.1 Alta |
| HALLAZGO-03 | Stored XSS en el comprobante de transacción | A03 (CWE-79) | 8.7 Alta |
| HALLAZGO-04 | Contraseñas en texto plano | A02 / A07 (CWE-256) | 7.4 Alta |
| HALLAZGO-05 | Stack traces, consola H2 expuesta y secreto JWT débil | A05 / A02 (CWE-209, CWE-798) | 9.8 Crítica |

## Requisitos

- JDK 21 o superior
- Node.js 20 LTS y npm
- Maven 3.9+ (o el wrapper `mvnw` incluido)

## Ejecución

### 1. Backend

El backend **no inicia** sin un secreto JWT de al menos 256 bits definido como variable de entorno.

Linux / macOS:

```bash
cd backend
export INSECUREPAY_JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```

Windows (PowerShell):

```powershell
cd backend
$env:INSECUREPAY_JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
.\mvnw.cmd spring-boot:run
```

Variables opcionales: `INSECUREPAY_JWT_EXPIRATION_MS` (por defecto 3600000) e `INSECUREPAY_CORS_ORIGINS` (por defecto `http://localhost:4200`).

### 2. Frontend

```bash
cd frontend
npm install
npm start
```

Abrir `http://localhost:4200`.

### 3. Pruebas

```bash
cd backend
mvn clean test
```

## Usuarios de prueba

Las contraseñas se almacenan como hashes BCrypt (factor 12) en `data.sql`.

| Usuario | Contraseña | Rol | Cuenta |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | ADMIN | ID 1 – `CR05015202001026458900` |
| `auditor_seguridad` | `auditor2026` | AUDITOR | — |
| `carlos_mora` | `carlos123` | CLIENTE | ID 2 – `CR05015202001026458901` |
| `maria_rojas` | `maria123` | CLIENTE | ID 3 – `CR05015202001026458902` |
| `juan_perez` | `juan123` | CLIENTE | ID 4 – `CR05015202001026458903` |

Los roles `AUDITOR` y `ADMIN` pueden consultar cuentas y movimientos de cualquier cliente; los clientes solo acceden a sus propios recursos.

## Endpoints

Todos los endpoints `/api/**`, excepto `POST /api/auth/login`, requieren `Authorization: Bearer <token>`. Los errores se devuelven en formato RFC 7807 (`application/problem+json`).

| Método | Endpoint | Control aplicado |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | BCrypt, mensaje genérico ante fallo |
| `GET` | `/api/cuentas` | Cuentas propias (todas para AUDITOR/ADMIN) |
| `GET` | `/api/cuentas/{id}` | Propiedad verificada → 403 si es ajena |
| `GET` | `/api/cuentas/{id}/saldo` | Propiedad verificada → 403 si es ajena |
| `GET` | `/api/cuentas/usuario/{usuarioId}` | Solo el propio usuario → 403 |
| `GET` | `/api/transacciones` | Movimientos propios |
| `GET` | `/api/transacciones/{id}` | Solo participantes → 403 |
| `GET` | `/api/transacciones/cuenta/{cuentaId}` | Propiedad verificada → 403 |
| `GET` | `/api/transacciones/buscar?termino=` | JPQL parametrizado y filtrado por usuario |
| `POST` | `/api/transacciones/transferir` | Cuenta origen propia → 403, concepto escapado |

La consola H2 está deshabilitada en la versión remediada.

## Reproducir las evidencias

El workflow `Auditoria forense InsecurePay` se ejecuta en cada push a `main`: compila y prueba el proyecto, levanta la versión vulnerable (`version-vulnerable`) y la remediada (`main`), ejecuta los mismos scripts de PoC sobre ambas y publica las salidas y capturas en la rama `evidencias-ci`.

Para ejecutarlo localmente con el backend y el frontend levantados:

```bash
bash scripts/poc.sh
cd scripts && npm install --no-save playwright && npx playwright install chromium && node xss-playwright.mjs
```

> **Advertencia ética:** la rama `version-vulnerable` contiene vulnerabilidades intencionales con fines exclusivamente educativos. No debe desplegarse en entornos productivos ni utilizarse contra sistemas sin autorización.
