#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
VAULT_EXE="/c/Dev/Software/vault/vault.exe"
SECRET_PATH="secret/banca-project/bank-ms-customer"

if [[ ! -f "$ENV_FILE" ]]; then
  printf 'No existe %s. Cópialo desde .env y completa las credenciales locales.\n' "$ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$VAULT_EXE" ]]; then
  printf 'No se encontró el Vault portable: %s\n' "$VAULT_EXE" >&2
  exit 1
fi

set -a
# .env must contain trusted local KEY=value assignments.
source "$ENV_FILE"
set +a

: "${CUSTOMER_DB_USERNAME:?Define CUSTOMER_DB_USERNAME en .env}"
: "${CUSTOMER_DB_PASSWORD:?Define CUSTOMER_DB_PASSWORD en .env}"
: "${VAULT_TOKEN:?Define VAULT_TOKEN en .env}"
export VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"

"$VAULT_EXE" status >/dev/null

"$VAULT_EXE" kv put "$SECRET_PATH" \
  "CUSTOMER_DB_USERNAME=$CUSTOMER_DB_USERNAME" \
  "CUSTOMER_DB_PASSWORD=$CUSTOMER_DB_PASSWORD"

"$VAULT_EXE" kv get -format=json "$SECRET_PATH" >/dev/null
printf '\n--- Secreto customer creado y verificado ---\n'
