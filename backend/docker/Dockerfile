FROM postgres:16-alpine

COPY backend/docker/10-create-app-user.sh /docker-entrypoint-initdb.d/10-create-app-user.sh
RUN chmod 0555 /docker-entrypoint-initdb.d/10-create-app-user.sh
