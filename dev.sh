#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

JAVA_CANDIDATE="/home/sam/.jdks/ms-25.0.4"
JAVA_SERVICES=(gateway chain-ingest enrichment risk-ai monitor)
declare -A SERVICE_JVM_ARGS=(
  [risk-ai]="-XX:+UseParallelGC"
)

ensure_java() {
  if [ -x "$JAVA_CANDIDATE/bin/java" ]; then
    export JAVA_HOME="$JAVA_CANDIDATE"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  if ! java -version 2>&1 | grep -q '"25'; then
    echo "Нужен JDK 25 в PATH/JAVA_HOME (проект требует Java 25 LTS). Сейчас:" >&2
    java -version 2>&1 >&2 || true
    exit 1
  fi
}

load_env() {
  if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
    set +a
  else
    echo ".env не найден в корне проекта — сервисы упадут без ключей" >&2
  fi
}

wait_for_postgres() {
  echo "Жду Postgres..."
  for _ in $(seq 1 30); do
    if docker exec risk-postgres pg_isready -U risk -d risk >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "Postgres не поднялся вовремя" >&2
  exit 1
}

wait_for_service() {
  local name="$1" marker="$2"
  local logfile="$LOG_DIR/$name.log"
  echo "Жду $name..."
  for _ in $(seq 1 60); do
    if grep -q "$marker" "$logfile" 2>/dev/null; then
      echo "$name готов"
      return 0
    fi
    if [ -f "$LOG_DIR/$name.pid" ] && ! kill -0 "$(cat "$LOG_DIR/$name.pid")" 2>/dev/null; then
      echo "$name упал при старте, смотри $logfile" >&2
      exit 1
    fi
    sleep 2
  done
  echo "$name не поднялся за отведённое время, смотри $logfile" >&2
  exit 1
}

start_process() {
  local name="$1" cwd="$2" cmd="$3"
  (
    cd "$cwd"
    setsid nohup bash -c "$cmd" > "$LOG_DIR/$name.log" 2>&1 < /dev/null &
    echo $! > "$LOG_DIR/$name.pid"
  )
}

start_java_service() {
  local module="$1"
  local jvm_args="${SERVICE_JVM_ARGS[$module]:-}"
  local run_args=""
  if [ -n "$jvm_args" ]; then
    run_args=" -Dspring-boot.run.jvmArguments=\"$jvm_args\""
  fi

  start_process "$module" "$ROOT_DIR" "./mvnw -pl $module spring-boot:run$run_args"
}

start_frontend() {
  start_process "frontend" "$ROOT_DIR/frontend" "npm run dev"
}

check_not_running() {
  local running=()
  for name in "${JAVA_SERVICES[@]}" frontend; do
    local pidfile="$LOG_DIR/$name.pid"
    if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
      running+=("$name")
    fi
  done
  if [ "${#running[@]}" -gt 0 ]; then
    echo "Уже запущено: ${running[*]}. Сначала ./dev.sh stop, иначе старая сессия осиротеет и продолжит держать партиции Kafka." >&2
    exit 1
  fi
}

cmd_start() {
  ensure_java
  check_not_running
  load_env

  echo "Поднимаю Kafka + Postgres..."
  docker compose up -d kafka kafka-ui postgres
  wait_for_postgres

  echo "Собираю проект (common + все модули)..."
  ./mvnw -q -DskipTests install

  for module in "${JAVA_SERVICES[@]}"; do
    echo "Запускаю $module..."
    start_java_service "$module"
  done

  wait_for_service gateway "Started GatewayApplication"
  wait_for_service chain-ingest "Started ChainIngestApplication"
  wait_for_service enrichment "Started EnrichmentApplication"
  wait_for_service risk-ai "Started RiskAiApplication"
  wait_for_service monitor "Started MonitorApplication"

  echo "Запускаю frontend..."
  start_frontend

  echo
  echo "Всё поднято:"
  echo "  gateway      http://localhost:8081"
  echo "  kafka-ui     http://localhost:8090"
  echo "  frontend     http://localhost:5173"
  echo "Логи — в ./logs/*.log, статус — ./dev.sh status, остановить — ./dev.sh stop"
}

cmd_stop() {
  for name in "${JAVA_SERVICES[@]}" frontend; do
    pidfile="$LOG_DIR/$name.pid"
    if [ -f "$pidfile" ]; then
      pid="$(cat "$pidfile")"
      if kill -0 "$pid" 2>/dev/null; then
        echo "Останавливаю $name (pid $pid)..."
        kill -TERM "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
      fi
      rm -f "$pidfile"
    fi
  done
  echo "Гашу docker (kafka, kafka-ui, postgres)..."
  docker compose down
}

cmd_status() {
  for name in "${JAVA_SERVICES[@]}" frontend; do
    pidfile="$LOG_DIR/$name.pid"
    if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
      echo "$name: запущен (pid $(cat "$pidfile"))"
    else
      echo "$name: остановлен"
    fi
  done
  docker compose ps
}

case "${1:-}" in
  start) cmd_start ;;
  stop) cmd_stop ;;
  status) cmd_status ;;
  *)
    echo "Использование: $0 {start|stop|status}"
    exit 1
    ;;
esac
