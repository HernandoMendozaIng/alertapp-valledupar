#!/usr/bin/env bash
# Borra todos los datos de la base local y deja solo los datos sintéticos de demostración.
# Útil antes de grabar el video o de una revisión, porque las pruebas e2e crean reportes y avisos de prueba.
# Después de correrlo, reinicia la API: Flyway vuelve a crear el esquema y DatosDemo carga los datos.
set -euo pipefail

BASE="${DB_NAME:-alertapp}"
USUARIO="${DB_USER:-alertapp}"

read -r -p "Esto borra todos los datos de la base '$BASE'. ¿Continuar? (s/N) " respuesta
[ "$respuesta" = "s" ] || { echo "Cancelado."; exit 0; }

psql -U "$USUARIO" -d "$BASE" -v ON_ERROR_STOP=1 -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
rm -rf "$(cd "$(dirname "$0")/.." && pwd)/backend/evidencias"
echo "Base vacía. Reinicia la API para recrear el esquema y cargar los datos de demostración."
