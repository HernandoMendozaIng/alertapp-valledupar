#!/usr/bin/env bash
# Arranca los tres componentes del prototipo en local:
#   1) microservicio de IA (Python, puerto 8000)
#   2) API REST (Spring Boot, puerto 8080) - crea el esquema y carga datos de prueba la primera vez
#   3) aplicación web (React + Vite, puerto 5173)
# Requisitos: Java 21, Maven, Python 3.11+, Node 20+, PostgreSQL 15+ con la base "alertapp" creada.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$RAIZ/.logs"
mkdir -p "$LOGS"

echo "Iniciando microservicio de IA..."
cd "$RAIZ/ia-service"
if [ ! -d .venv ]; then
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi
.venv/bin/uvicorn app:app --port 8000 > "$LOGS/ia.log" 2>&1 &
PID_IA=$!

echo "Iniciando API (Spring Boot)..."
cd "$RAIZ/backend"
mvn -q spring-boot:run > "$LOGS/backend.log" 2>&1 &
PID_API=$!

echo "Iniciando aplicación web..."
cd "$RAIZ/frontend"
[ -d node_modules ] || npm install --silent
npx vite --port 5173 > "$LOGS/web.log" 2>&1 &
PID_WEB=$!

trap 'echo; echo "Deteniendo..."; kill $PID_IA $PID_API $PID_WEB 2>/dev/null' INT TERM

echo
echo "Listo. Abre http://localhost:5173"
echo "  Ciudadano: ciudadano@alertapp.co / Valledupar2026"
echo "  Autoridad: autoridad@alertapp.co / Valledupar2026"
echo "  Documentación de la API: http://localhost:8080/docs"
echo "Registros en $LOGS. Ctrl+C para detener."
wait
