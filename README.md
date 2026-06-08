# Sistema Bingo

Sistema web para asignar cartones de bingo ya generados en CSV. No crea numeros aleatorios: importa `cartones_generados.csv`, conserva 3000 cartones seriados y permite que cada representante seleccione un carton disponible de su agrupacion.

## Estructura

```text
backend/    API Spring Boot 3, JWT, JPA, Flyway, PDF
frontend/   Angular con Bootstrap y guards por rol
database/   SQL inicial y ejemplo Nginx
```

## Requisitos

- Java 21
- Node 22 o compatible
- Docker y Docker Compose para MySQL local

No necesitas Maven instalado: el backend incluye Maven Wrapper.

## Base de datos local

```bash
docker compose up -d mysql
```

Credenciales por defecto:

- Base de datos: `bingo_db`
- Usuario: `bingo`
- Password: `bingo123`

El script equivalente esta en `database/01_create_database.sql`.

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

Variables utiles:

```bash
DB_URL=jdbc:mysql://localhost:3306/bingo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=bingo
DB_PASSWORD=bingo123
JWT_SECRET=change-this-secret-in-production-change-this-secret
FRONTEND_URL=http://localhost:4200
PUBLIC_URL=http://localhost:8080
```

Al iniciar, Flyway crea las tablas y el importador carga `backend/src/main/resources/cartones_generados.csv`. La importacion es idempotente: si los seriales ya existen, no los duplica.

## Usuarios iniciales

El backend crea usuarios mediante Seeder con password BCrypt. Password inicial para todos:

```text
Bingo2026
```

Usuarios:

- Admin: `admin`
- Representantes GEOURP: `geourp01`, `geourp02`, `geourp03`, `geourp04`
- Representantes CIVIAL: `civial01`, `civial02`, `civial03`, `civial04`
- Representantes ACI: `aci01`, `aci02`, `aci03`, `aci04`

## Frontend

```bash
cd frontend
npm install
npm start
```

URL local:

```text
http://localhost:4200
```

## Endpoints principales

- `POST /api/auth/login`
- `POST /api/account/change-password`
- `GET /api/admin/dashboard`
- `GET /api/admin/cards`
- `PATCH /api/admin/cards/{id}/cancel`
- `GET /api/representative/dashboard`
- `GET /api/representative/cards/available?search=012&page=0&size=50`
- `POST /api/representative/cards/generate`
- `GET /api/representative/cards`
- `GET /api/cards/{serial}/pdf`
- `GET /api/public/cards/{serial}/verify`

## Concurrencia e idempotencia

La generacion de cartones usa:

- Transaccion de base de datos.
- Bloqueo pesimista sobre usuario y carton disponible.
- Restricciones unicas en `serial`, `positional_signature` e `idempotency_requests`.
- Header obligatorio `Idempotency-Key` en `POST /api/vendor/cards/generate`.

La generacion recibe `buyerName`, `serial` e `idempotencyKey`. Si el frontend reintenta la misma solicitud con la misma clave, el backend devuelve el mismo carton.

## PDF y WhatsApp

Cada carton asignado puede descargarse desde:

```text
GET /api/cards/{serial}/pdf
```

El frontend incluye botones para ver PDF, descargar PDF como blob autenticado y compartir por WhatsApp un texto con enlace publico de verificacion.

## Build

Backend:

```bash
cd backend
./mvnw -DskipTests package
```

Frontend:

```bash
cd frontend
npm run build
```

## Despliegue VPS Linux

1. Instalar Java 21, Node/Nginx y MySQL.
2. Crear base de datos y usuario con `database/01_create_database.sql`.
3. Copiar `backend/target/bingo-backend-0.0.1-SNAPSHOT.jar` al servidor.
4. Ejecutar backend como servicio systemd con variables de entorno seguras.
5. Compilar frontend con `npm run build` y copiar `frontend/dist/frontend/browser` a `/var/www/bingo/frontend`.
6. Usar `database/nginx-bingo.conf` como base para el reverse proxy.
7. Configurar HTTPS con Certbot.

En produccion cambia obligatoriamente `JWT_SECRET`, passwords de MySQL y `PUBLIC_URL`.
