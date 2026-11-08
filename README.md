# TechSupport Solutions S.A.C. — Sistema de Gestión de Soporte Técnico TI (SOA)

> **¿Primera vez?** Sigue la [Guía de levantamiento y pruebas](GUIA.md): instalación, usuarios, Postman y prueba de caída de BD.

Implementación del proyecto del curso **Arquitectura Orientada a Servicios — Grupo 02**: tres servicios de negocio
(Authentication, User y Ticket) integrados mediante un **API Gateway** (ESB ligero). Cada servicio tiene su propia
base de datos MySQL, hay caché en **Redis** y se audita con **Prometheus + Grafana**. Todo corre en **Docker** con
**10 contenedores**: gateway, 3 servicios, 3 MySQL, Redis, Prometheus y Grafana.

```
Cliente / Postman / VS Code (api.http)
        │ REST + JSON (único puerto expuesto: 8080)
        ▼
┌──────────────────────────────────────────────┐
│ API Gateway (Spring Cloud Gateway)           │  JWT + lista negra · RBAC · rate limit (Redis)
│                                              │  circuit breaker + fallback · Swagger agregado
└──────┬──────────────────┬──────────────────┬─┘
       ▼                  ▼                  ▼
  Auth Service       User Service  ◄──── Ticket Service (orquestador)
       │                  │        REST      │
       ▼                  ▼                  ▼
  MySQL auth_db      MySQL user_db      MySQL ticket_db        Redis (caché + blacklist + rate limit)

Auditoría: Prometheus (métricas + alertas) · Grafana (3 dashboards)
```

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Docker Desktop / Docker Engine + Compose v2 | 24+ |
| RAM libre para Docker | ~4 GB (4 JVM + 3 MySQL + Redis + Prometheus + Grafana) |
| Visual Studio Code | recomendado (ver sección 2.1) |
| Opcional: JDK 21 + Maven 3.9 | solo para que VS Code compile/analice el código y correr las pruebas unitarias fuera de Docker |

No hace falta instalar Java ni Maven: la imagen compila el código en una etapa de build.

## 2. Levantar el sistema

```bash
cp .env.example .env          # opcional: todos los valores tienen un valor por defecto para desarrollo
docker compose up -d --build  # la primera vez descarga dependencias Maven (~3-6 min)
docker compose ps             # esperar a que los servicios estén "healthy"
```

| Qué | URL | Credenciales |
|---|---|---|
| API Gateway | http://localhost:8080 | — |
| Swagger UI (los 3 servicios) | http://localhost:8080/swagger-ui.html | botón **Authorize** → pegar el `accessToken` |
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus (alertas en /alerts) | http://localhost:9090 | — |

Usuarios de prueba (sembrados con Flyway):

| Rol | Correo | Contraseña | Notas |
|---|---|---|---|
| ADMIN | admin@techsupport.pe | Admin123! | token de 15 min |
| CLIENTE | cgonzalez@techsupport.pe | Cliente123! | USR-C0001, activo |
| CLIENTE | atorres@textilessur.pe | Cliente123! | USR-C0002, activo |
| CLIENTE | mrojas@comercialandina.pe | Cliente123! | USR-C0003, **SUSPENDIDO** (no puede abrir tickets) |
| TECNICO | lramirez@techsupport.pe | Tecnico123! | USR-T0001 · N2 · Hardware, Redes, BD |
| TECNICO | mtorres@techsupport.pe | Tecnico123! | USR-T0002 · N3 · BD, Seguridad |
| TECNICO | aquispe@ · jsalazar@ · rhuaman@ · pcastillo@techsupport.pe | Tecnico123! | N1, N1, N2, N3 |

### 2.1 Abrirlo en Visual Studio Code

1. **Archivo → Abrir carpeta…** y elegir la carpeta `techsupport-soa`.
2. Aceptar **"Instalar extensiones recomendadas"** (Java, Spring Boot, Docker, REST Client, YAML).
3. `Ctrl+Shift+B` ejecuta la tarea por defecto **Docker: levantar todo (build)**.
4. `Ctrl+Shift+P` → **Tasks: Run Task** muestra el resto: estado, logs, detener, **Caos: tumbar BD**,
   **Caos: levantar BD**, **Caos: escenario automático**, Redis y pruebas unitarias.
5. Abrir `api.http` y pulsar **Send Request** sobre cada petición para probar el flujo sin salir del editor
   (en la respuesta se ve la cabecera `X-Cache`).

El panel de Docker de VS Code muestra los 10 contenedores y permite ver logs o detener una BD con clic derecho.

Pruebas funcionales: importar `postman/TechSupport-SOA.postman_collection.json` y ejecutarla completa con el
*Collection Runner* (43 peticiones con aserciones: autenticación, CRUD de usuarios, flujo completo del ticket y
casos negativos de seguridad).

## 3. Servicios y endpoints

Todos se consumen **a través del gateway** (`http://localhost:8080`). El inventario coincide con las Tablas 5, 6 y 7
del documento; los marcados con ➕ se añadieron para completar los flujos.

**Authentication Service** — `/auth`
`POST /login` · `POST /refresh` ➕ · `POST /validate` · `POST /logout` · `POST /register` · `GET /roles`

**User Service** — `/users`
`GET|POST /clients` · `GET|PUT /clients/{id}` · `GET /clients/{id}/validate` · `GET|POST /technicians` ·
`GET /technicians/{id}` ➕ · `GET /technicians/available?specialty=&level=` · `GET /technicians/specialty/{s}` ·
`PATCH /technicians/{id}/status` · `GET /technicians/{id}/workload` · `GET /specialties` ➕
Internos (solo servicio-a-servicio, el gateway los bloquea): `PATCH /technicians/{id}/workload`, `DELETE /clients|technicians/{id}`.

**Ticket Service** — `/tickets`
`POST /` · `GET /{id}` · `GET /client/{clientId}` · `GET /status/{status}` · `PATCH /{id}/priority` ·
`PATCH /{id}/assign` · `PATCH /{id}/escalate` · `PATCH /{id}/reassign` · `PATCH /{id}/resolve` · `PATCH /{id}/close` ·
`GET /` ➕ · `GET /queue` ➕ · `GET /technician/{id}` ➕ · `PATCH /{id}/start` ➕ (EN_PROCESO) · `GET /{id}/events` ➕ (bitácora)

## 4. Cómo se conecta (orquestación)

Registro de un ticket crítico (`POST /tickets`, sección 9 del documento):

1. **Gateway**: valida el JWT localmente (firma HMAC-SHA256, emisor, expiración), consulta en Redis si fue revocado,
   aplica RBAC y propaga `X-User-Id`, `X-User-Email` y `X-User-Role`. Elimina esas cabeceras si vienen del cliente.
2. **Ticket Service → User Service** `GET /users/clients/{id}/validate`: el cliente existe y está ACTIVO.
3. **Evaluación de criticidad** con la tabla del documento (categoría base + palabras clave, sin tildes ni mayúsculas).
   CRÍTICA exige un técnico N2 o superior.
4. CRÍTICA/ALTA → **asignación inmediata**: `GET /technicians/available` (por especialidad y nivel, menor carga primero)
   y luego `PATCH /technicians/{id}/workload {+1}`, una **reserva atómica** (`UPDATE … WHERE current_tickets < max`),
   así dos asignaciones simultáneas nunca superan el límite del técnico.
5. Sin técnico disponible → el ticket queda en `PRIORITY_QUEUE` o `STANDARD_QUEUE` con `queueInfo` (posición y tiempo estimado).
   Un despachador lo reintenta cada 20 s, primero por prioridad y dentro de cada prioridad por orden de llegada.

**Escalamiento**: jerárquico (N1→N2→N3) o funcional (otra especialidad). Libera el cupo del técnico anterior, guarda
el historial y busca un nuevo especialista excluyendo al anterior (estado `REASIGNADO`). **Registro de usuarios**:
es una saga; Auth crea el perfil en User Service y, si falla al guardar la credencial, compensa borrando el perfil.

Estados: `REGISTRADO → ASIGNADO → EN_PROCESO → (ESCALADO → REASIGNADO) → RESUELTO → CERRADO`. Una máquina de
estados en el agregado `Ticket` rechaza las transiciones inválidas con 409.

## 5. Seguridad (sección 11 del documento)

- **JWT HS256** con claims `sub`, `userId`, `role`, `fullName`, `jti`, `iat` y `exp`. Duración por rol: ADMIN 15 min, TECNICO y CLIENTE 30 min.
- **Refresh token opaco** de 24 h. En la BD solo se guarda su hash SHA-256. Se rota en cada uso y, si se reutiliza uno
  ya rotado, se revoca toda la familia (detección de robo).
- **Logout**: el `jti` del token va a una lista negra en Redis con el TTL restante, y el gateway la consulta en cada petición.
- **Anti fuerza bruta**: tras 5 intentos fallidos la cuenta se bloquea 15 min (423). El login tarda lo mismo exista o no
  el correo, para no revelar qué cuentas existen.
- **Defensa en profundidad**: RBAC grueso en el gateway y fino en cada servicio (`@PreAuthorize` + propiedad del
  recurso: un CLIENTE solo ve sus tickets y un TECNICO solo los asignados). Las llamadas internas usan el rol `SYSTEM`
  con una clave interna que el gateway nunca reenvía.
- **Red**: solo el gateway (y Grafana/Prometheus para la demo) publican puertos. Las BD viven en la red `data`, sin acceso externo.
- Rate limiting en Redis: 20 req/s sostenidas por usuario, o por IP si es anónimo (login y registro).

## 6. Caché y resiliencia — qué pasa al tumbar una base de datos

Estrategia **cache-aside con copia stale** (`common/.../ResilientCache.java`):

| Situación | Respuesta | Cabecera |
|---|---|---|
| Dato en Redis | se sirve desde Redis | `X-Cache: HIT` |
| No está en Redis | se lee de MySQL y se guarda en Redis (copia fresca con TTL corto y copia stale de 24 h) | `X-Cache: MISS` |
| **MySQL caído** y existe copia | **200 con el último dato conocido** | `X-Cache: STALE` |
| MySQL caído y sin copia | **503** `dependency_unavailable` en 2 s como máximo, sin quedarse colgado | `Retry-After: 10` |
| Escritura con MySQL caído | 503; tras 4 fallos el circuit breaker se abre y responde 503 `circuit_open` al instante | `Retry-After: 10` |
| MySQL vuelve | el circuito pasa a semiabierto a los 10 s, se cierra solo y el servicio sigue sin reiniciarse | — |
| Redis caído | la caché se omite y todo va contra MySQL. La lista negra no responde y el gateway la deja pasar (*fail-open*), lo que se registra como métrica | — |

Efecto de cada caída:

- **`mysql-ticket`**: consultas de tickets en modo STALE; crear o modificar tickets da 503. Auth y User siguen normales.
- **`mysql-user`**: Ticket Service sigue validando clientes y viendo técnicos desde su propia caché (stale), pero
  reservar un técnico falla, así que los tickets nuevos quedan en cola y se asignan solos al volver la BD.
- **`mysql-auth`**: el login sigue funcionando para usuarios cacheados (modo degradado, sin refresh token).
  Los tokens ya emitidos siguen siendo válidos porque el gateway los valida sin consultar la BD.
- **Un servicio completo** (`docker compose stop user-service`): el gateway responde con un fallback JSON 503 inmediato.

### Guion de la prueba

Con Grafana abierto en **"3 · Resiliencia y caché"** (http://localhost:3000):

```bash
./chaos/chaos.sh scenario            # demo automática: crea ticket, tumba mysql-ticket, prueba lecturas/escrituras, la levanta
./chaos/chaos.sh db-down ticket      # o a mano (Windows: .\chaos\chaos.ps1 db-down ticket, o la tarea de VS Code)
#   ... probar con api.http / Postman: GET responde STALE, POST responde 503 ...
./chaos/chaos.sh db-up ticket
./chaos/chaos.sh db-down all         # caída total de la capa de datos
./chaos/chaos.sh redis-down          # caída de la caché
./chaos/chaos.sh service-down user   # caída de un servicio completo
```

Qué observar en Grafana: la línea de la BD (`database_up`, que cada servicio reporta cada 5 s) pasa a **CAÍDO**, el circuit breaker `db` pasa a **ABIERTO**,
la serie `stale` sube en *Lecturas de caché por resultado*, aparecen 503 controlados y la latencia p95 se mantiene baja.
En Prometheus → *Alerts* se disparan `BaseDeDatosCaida` y `CircuitBreakerAbierto`. Al levantar la BD, todo vuelve a
verde sin reiniciar nada.

## 7. Observabilidad y auditoría

| Dashboard | Contenido |
|---|---|
| **1 · Auditoría y negocio** | Logins correctos, fallidos y bloqueados; rechazos del gateway (401/403/429); tickets por prioridad; transiciones de estado; tiempo hasta asignación (MTTA p95); resultado del despacho; **tabla de eventos de auditoría** por servicio, evento, rol y resultado |
| **2 · Salud de servicios** | Disponibilidad (`up`), peticiones/s, latencia p95, códigos HTTP, latencia por ruta del gateway, heap JVM, pool HikariCP, CPU |
| **3 · Resiliencia y caché** | Estado de cada MySQL y de Redis, estado de los circuit breakers, hit ratio, lecturas hit/miss/stale, 503 y fallbacks, tasa de fallos, conexiones en espera, WARN/ERROR por servicio |

Cada acción de negocio queda auditada de tres formas: la métrica `audit_events_total` (en Grafana), un log JSON con
logger `AUDIT` (evento, actor, rol, recurso, resultado y `traceId`; se ve con `docker compose logs ticket-service`)
y, para los tickets, la bitácora inmutable en `ticket_event` (`GET /tickets/{id}/events`).

## 8. Estructura

```
├── common/            Librería compartida: seguridad por cabeceras, errores, ResilientCache, auditoría, RestClient interno
├── auth-service/      Authentication Service (JWT, refresh, logout, register, roles)
├── user-service/      User Service (clientes, técnicos, disponibilidad, reserva atómica de cupos)
├── ticket-service/    Ticket Service (orquestador: prioridad, asignación, cola, escalamiento, bitácora)
├── api-gateway/       Spring Cloud Gateway (rutas, filtro JWT, RBAC, rate limit, circuit breaker, Swagger)
├── docker/            Dockerfile multi-etapa (único, parametrizado por módulo)
├── infra/             Prometheus (+alertas) y Grafana (datasource + 3 dashboards)
├── postman/           Colección de pruebas funcionales
├── api.http           Pruebas rápidas desde VS Code (REST Client)
├── .vscode/           Extensiones recomendadas y tareas (levantar, logs, caos, pruebas)
└── chaos/             Scripts de caída/recuperación (bash y PowerShell)
```

Pruebas unitarias (reglas de criticidad, máquina de estados, JWT, RBAC del gateway, detección de fallos de infraestructura):

```bash
mvn test        # requiere JDK 21 y Maven
```

## 9. Decisiones y buenas prácticas aplicadas

- **Base de datos por servicio**, sin claves foráneas entre servicios. Cada servicio es dueño de sus datos y de su esquema (Flyway).
- **Java 21 + Spring Boot 3.5**: hilos virtuales para las peticiones HTTP, `RestClient` para las llamadas síncronas entre
  servicios (el sustituto recomendado de WebClient en apps MVC), records como DTOs y errores con un formato único.
- **Resilience4j**: los circuit breakers solo cuentan fallos de infraestructura, no los errores de negocio (un 404 no abre el circuito).
  Solo se reintenta lo que es idempotente: la reserva de cupo **no** se reintenta y la liberación sí.
- **Transacciones cortas**: ninguna transacción envuelve una llamada HTTP. Si la vinculación falla después de reservar
  un técnico, se compensa liberando el cupo.
- **Bloqueo optimista** (`@Version`) en tickets y técnicos.
- **Observabilidad sin contenedores extra**: cada servicio publica sus métricas (RED, negocio, estado de su BD y de
  Redis) en `/actuator/prometheus`; logs JSON estructurados con `traceId` (Micrometer Tracing) y alertas declarativas.
- **Contenedores**: build multi-etapa con caché de Maven, JRE Alpine, usuario sin privilegios, healthchecks de *liveness*
  (una BD caída no reinicia el servicio), límites de memoria y rotación de logs.

Pendiente para producción: TLS en el gateway, secretos en un gestor (Vault o Docker secrets), RS256 con JWKS en lugar
de clave compartida, patrón *outbox* para la liberación de cupos, ShedLock si el Ticket Service escala a varias réplicas
y Redis dedicado para la lista negra.
