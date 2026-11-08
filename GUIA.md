# Guía de levantamiento y pruebas — Gestor de Tickets (TechSupport SOA)

Guía paso a paso para que cualquier integrante del equipo descargue el proyecto, lo levante con Docker, lo pruebe y
ejecute las pruebas de caída de base de datos. Para la explicación de la arquitectura ver [README.md](README.md).

---

## 1. Qué se levanta

10 contenedores Docker:

| Contenedor | Función | Puerto en tu PC |
|---|---|---|
| `api-gateway` | Punto único de entrada (JWT, roles, rate limit) | **8080** |
| `auth-service` | Login, tokens JWT, registro | — (interno) |
| `user-service` | Clientes y técnicos | — (interno) |
| `ticket-service` | Incidencias: prioridad, asignación, escalamiento | — (interno) |
| `mysql-auth`, `mysql-user`, `mysql-ticket` | Una base de datos por servicio | — (interno) |
| `redis` | Memoria caché | — (interno) |
| `prometheus` | Recolecta métricas y alertas | **9090** |
| `grafana` | Dashboards de auditoría | **3000** |

---

## 2. Requisitos (instalar una sola vez)

1. **Docker Desktop** — https://www.docker.com/products/docker-desktop/
   - En Windows usa el backend **WSL 2** (Docker Desktop lo propone al instalar).
   - En *Settings → Resources* asigna **al menos 4 GB de memoria**.
   - Verifica en una terminal: `docker --version` y `docker compose version`.
2. **Git** — https://git-scm.com/downloads
3. **Visual Studio Code** — https://code.visualstudio.com/
4. *(Opcional)* **JDK 21 + Maven 3.9**: solo si quieres que VS Code analice el código Java o correr las pruebas
   unitarias. Para levantar el sistema **no** hace falta: Docker compila todo.
5. *(Opcional)* **Postman** — https://www.postman.com/downloads/

---

## 3. Descargar el proyecto

```bash
git clone https://github.com/Omeer1928/GestorDeTickets.git
cd GestorDeTickets
```

### Abrir en Visual Studio Code

1. **Archivo → Abrir carpeta…** → `GestorDeTickets`.
2. Instalar las extensiones recomendadas:
   - si aparece el aviso abajo a la derecha → **Instalar**;
   - si no aparece: `Ctrl+Shift+X` → escribir `@recommended` → botón de nube *Install Workspace Recommended Extensions*.
3. `Ctrl+Shift+P` → **Developer: Reload Window**.

Extensiones: Java Pack, Spring Boot, Docker, REST Client y YAML.

---

## 4. Levantar el sistema

Con **Docker Desktop abierto**, en la terminal de VS Code (`` Ctrl+` ``):

```bash
docker compose up -d --build
```

O en VS Code: `Ctrl+Shift+B` (tarea **Docker: levantar todo**).

- La **primera vez** tarda entre 5 y 10 minutos (descarga imágenes y dependencias Maven). Las siguientes, 1-2 minutos.
- Ver el estado: `docker compose ps`. Esperar hasta que los servicios muestren **(healthy)**:

```
NAME             STATUS
api-gateway      Up 1 minute (healthy)
auth-service     Up 2 minutes (healthy)
user-service     Up 2 minutes (healthy)
ticket-service   Up 2 minutes (healthy)
mysql-auth       Up 2 minutes (healthy)
...
```

### Comprobar que funciona

| Abrir en el navegador | Debe mostrar |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger con los 3 servicios (selector arriba a la derecha) |
| http://localhost:3000 | Grafana (usuario `admin`, contraseña `admin123`) |
| http://localhost:9090/targets | Prometheus con los 4 servicios en estado **UP** |

---

## 5. Usuarios de prueba

Se crean automáticamente al levantar (no hay que registrarlos).

| Rol | Correo | Contraseña | Detalle |
|---|---|---|---|
| ADMIN | admin@techsupport.pe | Admin123! | Supervisa todo |
| CLIENTE | cgonzalez@techsupport.pe | Cliente123! | USR-C0001 (activo) |
| CLIENTE | atorres@textilessur.pe | Cliente123! | USR-C0002 (activo) |
| CLIENTE | mrojas@comercialandina.pe | Cliente123! | USR-C0003 (**suspendido**: no puede crear tickets) |
| TECNICO | lramirez@techsupport.pe | Tecnico123! | USR-T0001 · N2 · Hardware, Redes, BD |
| TECNICO | mtorres@techsupport.pe | Tecnico123! | USR-T0002 · N3 · BD, Seguridad |
| TECNICO | aquispe@techsupport.pe | Tecnico123! | USR-T0003 · N1 · Hardware, Software |
| TECNICO | jsalazar@techsupport.pe | Tecnico123! | USR-T0004 · N1 · Redes |
| TECNICO | rhuaman@techsupport.pe | Tecnico123! | USR-T0005 · N2 · Software, Seguridad |
| TECNICO | pcastillo@techsupport.pe | Tecnico123! | USR-T0006 · N3 · Redes, Software, Hardware |

---

## 6. Pruebas funcionales

### Opción A — Desde VS Code (`api.http`)

1. Abrir el archivo `api.http`.
2. Hacer clic en **Send Request** sobre cada bloque, en orden (1 → 13).
   Los tokens y el número de ticket se pasan solos de una petición a otra.
3. En cada respuesta mirar la cabecera **`X-Cache`**: `MISS` (vino de MySQL), `HIT` (vino de Redis) o `STALE`
   (la BD está caída y se respondió con la última copia).

### Opción B — Postman (batería completa con validaciones)

1. Postman → **Import** → `postman/TechSupport-SOA.postman_collection.json`.
2. Clic derecho en la colección → **Run collection** → **Run**.
3. Resultado esperado: las 43 peticiones en verde. Cubre:
   - **Autenticación**: login por rol, login fallido, validar, renovar token, registro.
   - **Usuarios**: CRUD de clientes, técnicos disponibles por especialidad y nivel, permisos.
   - **Ticket completo**: crear crítico (se asigna solo) → iniciar → escalar N2→N3 → resolver → cerrar.
   - **Seguridad**: sin token (401), token falso (401), rol sin permiso (403), cabecera falsificada, token revocado.

> Si ejecutas la colección dos veces seguidas, algunas validaciones de asignación pueden variar porque los técnicos
> ya tienen tickets. Para empezar desde cero: `docker compose down -v` y `docker compose up -d`.

### Opción C — Swagger

http://localhost:8080/swagger-ui.html → `POST /auth/login` con un usuario → copiar el `accessToken` →
botón **Authorize** → pegar el token → probar los endpoints con **Try it out**.

### Pruebas unitarias (opcional, requiere JDK 21 + Maven)

```bash
mvn test
```

---

## 7. Prueba de caída de base de datos (prueba principal)

**Antes de empezar**, abrir Grafana → menú **Dashboards → TechSupport SOA → "3 · Resiliencia y caché"**.

### 7.1 Escenario automático (recomendado para la exposición)

| Sistema | Comando |
|---|---|
| Windows (PowerShell) | `.\chaos\chaos.ps1 scenario` |
| Linux / macOS / Git Bash | `./chaos/chaos.sh scenario` |
| VS Code | `Ctrl+Shift+P` → *Tasks: Run Task* → **Caos: escenario automático** |

> Si PowerShell bloquea el script: `powershell -ExecutionPolicy Bypass -File .\chaos\chaos.ps1 scenario`

El script hace todo solo e imprime cada paso:

| Paso | Qué hace | Resultado esperado |
|---|---|---|
| 1 | Crea un ticket crítico | `201`, ticket asignado a un técnico |
| 2-3 | Consulta el ticket dos veces | `200` con `X-Cache: MISS` y luego `HIT` |
| — | **Detiene `mysql-ticket`** | — |
| 4 | Consulta el ticket con la BD caída | **`200` con `X-Cache: STALE`** (responde desde Redis) |
| 5 | Lista los tickets del cliente | `200 STALE` |
| 6 | Intenta crear tickets | **`503`** inmediato (primero `dependency_unavailable`, luego `circuit_open`) |
| 7 | Consulta el perfil del cliente (otra BD) | `200`: los demás servicios no se ven afectados |
| — | **Levanta `mysql-ticket`** y espera | — |
| 8 | Crea un ticket | `201`: el sistema se recuperó solo, sin reiniciar nada |
| 9 | Consulta el ticket | `200` con datos frescos |

### 7.2 Manual (para mostrar en vivo con Grafana)

```bash
# Windows: reemplazar ./chaos/chaos.sh por .\chaos\chaos.ps1
./chaos/chaos.sh db-down ticket      # tumba la BD de tickets
#   → probar con api.http: GET de un ticket = 200 STALE · POST /tickets = 503
./chaos/chaos.sh db-up ticket        # la levanta

./chaos/chaos.sh db-down user        # tumba la BD de usuarios (los tickets nuevos quedan en cola)
./chaos/chaos.sh db-up user          # al volver, el despachador los asigna solo (cada 20 s)

./chaos/chaos.sh db-down all         # tumba las 3 BD
./chaos/chaos.sh db-up all

./chaos/chaos.sh redis-down          # tumba la caché: todo sigue funcionando contra MySQL
./chaos/chaos.sh redis-up

./chaos/chaos.sh service-down user   # tumba un servicio completo: el gateway responde 503 controlado
./chaos/chaos.sh service-up user

./chaos/chaos.sh status              # estado de todos los contenedores
```

También desde VS Code: *Tasks: Run Task* → **Caos: tumbar BD** / **Caos: levantar BD** (pregunta cuál BD).

### 7.3 Qué debe verse en Grafana

| Panel | Con la BD caída | Al levantarla |
|---|---|---|
| Bases de datos MySQL | La BD afectada pasa a **CAÍDO** (rojo) | Vuelve a **ARRIBA** |
| Circuit breakers | `db` del servicio afectado pasa a **ABIERTO** | SEMI-ABIERTO → CERRADO |
| Lecturas de caché por resultado | Aparece la serie **stale** | Vuelven `hit` / `miss` |
| Errores 503 | Suben (escrituras rechazadas de forma controlada) | Bajan a 0 |
| Latencia p95 (dashboard 2) | Se mantiene baja: el sistema no se cuelga | — |

Además, en http://localhost:9090/alerts se disparan las alertas **BaseDeDatosCaida** y **CircuitBreakerAbierto**.

### 7.4 Comportamiento esperado por cada caída

| Se cae | Consultas | Escrituras | Otros servicios |
|---|---|---|---|
| `mysql-ticket` | 200 desde caché (STALE) | 503 rápido | Normales |
| `mysql-user` | 200 desde caché | Tickets nuevos quedan en cola | Auth y Ticket siguen |
| `mysql-auth` | Login sigue funcionando para usuarios ya cacheados | Registro 503 | Tokens ya emitidos siguen válidos |
| `redis` | Todo va contra MySQL (más lento) | Normal | Normales |
| Un servicio | Gateway responde 503 con JSON claro | — | Normales |

---

## 8. Auditoría en Grafana

http://localhost:3000 (admin / admin123) → **Dashboards → TechSupport SOA**:

- **1 · Auditoría y negocio**: logins exitosos y fallidos, accesos denegados, tickets por prioridad, estados,
  escalamientos, tiempo de asignación y tabla de eventos de auditoría (quién hizo qué).
- **2 · Salud de servicios**: disponibilidad, peticiones por segundo, latencia, errores, memoria.
- **3 · Resiliencia y caché**: estado de las BD y Redis, circuit breakers, uso de caché.

Detalle de cada evento auditado (actor, recurso, resultado):

```bash
docker compose logs ticket-service | grep AUDIT
```

Bitácora de un ticket: `GET /tickets/{id}/events` (está en `api.http`, paso 9).

---

## 9. Comandos útiles

| Acción | Comando |
|---|---|
| Levantar | `docker compose up -d --build` |
| Ver estado | `docker compose ps` |
| Ver logs de un servicio | `docker compose logs -f ticket-service` |
| Detener (conserva datos) | `docker compose down` |
| Reinicio limpio (borra datos) | `docker compose down -v` y luego `docker compose up -d --build` |
| Reconstruir tras cambiar código | `docker compose up -d --build <servicio>` |

---

## 10. Solución de problemas

| Problema | Solución |
|---|---|
| `port is already allocated` (8080, 3000 o 9090) | Otro programa usa el puerto. Cambiarlo en un archivo `.env` (copiar `.env.example`): `GATEWAY_PORT=8081` |
| Un servicio queda `unhealthy` o se reinicia | `docker compose logs <servicio>`. Suele ser falta de memoria: subir la RAM de Docker Desktop a 4-6 GB |
| `failed to resolve reference ... not found` al descargar una imagen | Una versión de imagen no existe: cambiarla en `.env` (por ejemplo `GRAFANA_VERSION=11.6.0`) |
| La primera compilación falla por red | Volver a ejecutar `docker compose up -d --build` (las dependencias quedan en caché) |
| Login responde `423 account_locked` | 5 intentos fallidos bloquean 15 min. Esperar o `docker compose restart redis` |
| Postman falla en asignaciones al repetir | `docker compose down -v` y levantar de nuevo para partir de cero |
| Grafana sin datos | Esperar 1 minuto tras levantar y revisar http://localhost:9090/targets |
| PowerShell no ejecuta `chaos.ps1` | `powershell -ExecutionPolicy Bypass -File .\chaos\chaos.ps1 <acción>` |
| `chaos.sh: Permission denied` | `chmod +x chaos/chaos.sh` |

---

## 11. Estructura del repositorio

```
GestorDeTickets/
├── GUIA.md               ← esta guía
├── README.md             ← arquitectura y decisiones técnicas
├── docker-compose.yml    ← los 10 contenedores
├── .env.example          ← variables configurables (puertos, contraseñas, versiones)
├── api.http              ← pruebas rápidas desde VS Code
├── common/               ← librería compartida (seguridad, caché, auditoría, errores)
├── auth-service/         ← Authentication Service
├── user-service/         ← User Service
├── ticket-service/       ← Ticket Service (orquestador)
├── api-gateway/          ← API Gateway
├── docker/               ← Dockerfile de los servicios
├── infra/                ← Prometheus (alertas) y Grafana (dashboards)
├── postman/              ← colección de pruebas
├── chaos/                ← scripts de caída y recuperación
└── .vscode/              ← extensiones recomendadas y tareas
```
