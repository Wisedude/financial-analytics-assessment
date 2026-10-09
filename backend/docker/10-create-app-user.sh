#!/bin/sh
set -eu

if [ "$APP_DB_USERNAME" = "$POSTGRES_USER" ]; then
  echo "APP_DB_USERNAME must be different from the PostgreSQL admin user" >&2
  exit 1
fi

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=app_username="$APP_DB_USERNAME" \
  --set=app_password="$APP_DB_PASSWORD" \
  --set=app_database="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"app_username" LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE :"app_database" TO :"app_username";
GRANT USAGE, CREATE ON SCHEMA public TO :"app_username";
ALTER SCHEMA public OWNER TO :"app_username";
SQL
