#!/usr/bin/env bash
# Pruebas de caos: tumbar y levantar bases de datos, Redis o servicios para observar la resiliencia.
#
#   ./chaos/chaos.sh db-down ticket      # detiene mysql-ticket
#   ./chaos/chaos.sh db-up ticket
#   ./chaos/chaos.sh db-down all
#   ./chaos/chaos.sh redis-down | redis-up
#   ./chaos/chaos.sh service-down user | service-up user
#   ./chaos/chaos.sh status
#   ./chaos/chaos.sh scenario            # demo automática de caída y recuperación de la BD de tickets
set -euo pipefail
cd "$(dirname "$0")/.."

BASE="${BASE_URL:-http://localhost:8080}"
DC="docker compose"

dbs() {
  case "${1:-}" in
    auth|user|ticket) echo "mysql-$1" ;;
    all) echo "mysql-auth mysql-user mysql-ticket" ;;
    *) echo "Uso: $0 db-down|db-up auth|user|ticket|all" >&2; exit 1 ;;
  esac
}

stamp() { date '+%H:%M:%S'; }

http() { # http METHOD PATH [TOKEN] [BODY] → imprime "status|x-cache|body"
  local method=$1 path=$2 token=${3:-} body=${4:-}
  local args=(-s -o /tmp/chaos_body -D /tmp/chaos_headers -w '%{http_code}' -X "$method" "$BASE$path" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body" ]] && args+=(-d "$body")
  local code; code=$(curl "${args[@]}" || echo "000")
  local cache; cache=$(grep -i '^x-cache:' /tmp/chaos_headers 2>/dev/null | tr -d '\r' | awk '{print $2}' || true)
  echo "$code|${cache:--}|$(head -c 220 /tmp/chaos_body 2>/dev/null)"
}

field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }

report() { # report "descripción" "status|cache|body"
  IFS='|' read -r code cache body <<< "$2"
  printf '[%s] %-52s → HTTP %s  X-Cache:%-6s %s\n' "$(stamp)" "$1" "$code" "$cache" "${body:0:110}"
}

wait_db() {
  echo "[$(stamp)] Esperando a que $1 esté healthy..."
  until [[ "$(docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null)" == "healthy" ]]; do sleep 2; done
  echo "[$(stamp)] $1 healthy"
}

scenario() {
  echo "=== Escenario: caída y recuperación de la BD de tickets (mysql-ticket) ==="
  local login token ticket r
  login=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"email":"cgonzalez@techsupport.pe","password":"Cliente123!"}')
  token=$(echo "$login" | field accessToken)
  [[ -z "$token" ]] && { echo "No se pudo iniciar sesión: $login"; exit 1; }
  echo "[$(stamp)] Sesión iniciada como USR-C0001"

  r=$(http POST /tickets "$token" '{"title":"Caída total del ERP","description":"El ERP no conecta en producción","category":"BASES_DATOS"}')
  report "1. Crear ticket crítico (BD arriba)" "$r"
  ticket=$(echo "$r" | cut -d'|' -f3- | field ticketId)
  [[ -z "$ticket" ]] && ticket=$(http GET /tickets/client/USR-C0001 "$token" | cut -d'|' -f3- | field ticketId)

  report "2. GET /tickets/$ticket (llena la caché)" "$(http GET "/tickets/$ticket" "$token")"
  report "3. GET /tickets/$ticket (desde Redis)" "$(http GET "/tickets/$ticket" "$token")"
  report "4. GET /tickets/client/USR-C0001" "$(http GET /tickets/client/USR-C0001 "$token")"

  echo; echo "[$(stamp)] >>> docker compose stop mysql-ticket"; $DC stop mysql-ticket >/dev/null; sleep 2

  for i in 1 2 3; do
    report "5.$i GET /tickets/$ticket (BD caída)" "$(http GET "/tickets/$ticket" "$token")"
  done
  report "6. GET /tickets/client/USR-C0001 (BD caída)" "$(http GET /tickets/client/USR-C0001 "$token")"
  for i in 1 2 3 4 5; do
    report "7.$i POST /tickets (BD caída → 503 rápido)" "$(http POST /tickets "$token" '{"title":"Prueba","description":"Con BD caída","category":"SISTEMAS_SOFTWARE"}')"
  done
  report "8. GET /users/clients/USR-C0001 (otra BD, no afectada)" "$(http GET /users/clients/USR-C0001 "$token")"

  echo; echo "[$(stamp)] >>> docker compose start mysql-ticket"; $DC start mysql-ticket >/dev/null
  wait_db mysql-ticket
  echo "[$(stamp)] Esperando que el circuit breaker pase a semi-abierto (10 s)..."; sleep 12
  report "9. POST /tickets (BD recuperada)" "$(http POST /tickets "$token" '{"title":"Post recuperación","description":"La BD volvió","category":"SISTEMAS_SOFTWARE"}')"
  report "10. GET /tickets/$ticket (datos frescos)" "$(http GET "/tickets/$ticket" "$token")"
  echo; echo "Revise Grafana → '3 · Resiliencia y caché' (http://localhost:3000) y Prometheus → Alerts."
}

case "${1:-}" in
  db-down)      $DC stop $(dbs "${2:-}") && echo "[$(stamp)] BD detenida: $(dbs "${2:-}")" ;;
  db-up)        $DC start $(dbs "${2:-}") && for d in $(dbs "${2:-}"); do wait_db "$d"; done ;;
  redis-down)   $DC stop redis ;;
  redis-up)     $DC start redis ;;
  service-down) $DC stop "${2:?auth|user|ticket}-service" ;;
  service-up)   $DC start "${2:?auth|user|ticket}-service" ;;
  status)       $DC ps --format 'table {{.Name}}\t{{.Status}}' ;;
  scenario)     scenario ;;
  *) sed -n '2,11p' "$0"; exit 1 ;;
esac
