# Sistema Bingo 🎟️

**Caso de Éxito Real:** Este sistema web fue diseñado y desarrollado a solicitud de **3 asociaciones estudiantiles de la Universidad Ricardo Palma (URP)** en el ramo de Geotecnia (**GEOURP**, **CIVIAL** y **ACI**), con el propósito de gestionar y asignar de forma segura y concurrente **3,000 cartones de bingo únicos** para una actividad pro-fondos.

La plataforma permite a los representantes autorizados de cada agrupación seleccionar y reservar cartones seriados importados desde un archivo CSV (`cartones_generados.csv`), previniendo colisiones de asignación mediante mecanismos avanzados de concurrencia.

---

## 📁 Estructura del Proyecto

```text
backend/    API Spring Boot 3, JWT, JPA, Flyway, PDF
frontend/   Angular con Bootstrap y guards por rol
database/   SQL inicial y ejemplo Nginx
```

---

## 🛠️ Stack Tecnológico

*   **Backend:** Java 21 / Spring Boot 3 / Spring Data JPA / Flyway (Migraciones de BD).
*   **Frontend:** Angular con Bootstrap y guards de seguridad por roles.
*   **Base de Datos:** MySQL 8.0 (Dockerizada localmente).
*   **Despliegue y Hosting:** Desplegado con éxito en producción en una **VPS Linux (Contabo)**, utilizando **Nginx** como Proxy Inverso y certificado **HTTPS** gestionado con Certbot.

---

## ⚙️ Concurrencia, Idempotencia y Robustez

Debido al volumen de usuarios concurrentes al momento de la venta de cartones, la aplicación implementa salvaguardas robustas en el backend:
*   **Bloqueo Pesimista (Pessimistic Locking):** Bloqueo en base de datos sobre los registros de usuario y cartón disponible durante el proceso de asignación para evitar que dos representantes reserven el mismo cartón.
*   **Idempotencia mediante Claves:** Soporte de cabecera `Idempotency-Key` en peticiones críticas. Si el frontend reintenta la misma solicitud por inestabilidad de red, el backend retorna la transacción previa sin generar duplicados.
*   **Integración de PDF y WhatsApp:** Generación automatizada de PDF en servidor con opción en frontend para descarga directa o compartir mediante un enlace público de validación por WhatsApp.

---

## 📦 Instrucciones de Ejecución Local

### Prerrequisitos
*   Java 21 JDK instalado.
*   Node.js v22 o compatible.
*   Docker y Docker Compose (para MySQL local).

### 1. Base de Datos Local
Inicia el contenedor de MySQL:
```bash
docker compose up -d mysql
```
*   **Base de datos:** `bingo_db`
*   **Usuario:** `bingo`
*   **Contraseña:** `bingo123`
*(Script de inicialización en `database/01_create_database.sql`)*

### 2. Levantar el Backend
El backend incluye Maven Wrapper. Inícialo con:
```bash
cd backend
./mvnw spring-boot:run
```
Al arrancar, Flyway creará el esquema automáticamente e importará de forma idempotente los 3,000 cartones desde `backend/src/main/resources/cartones_generados.csv`.

#### Variables de Entorno Recomendadas:
```bash
DB_URL=jdbc:mysql://localhost:3306/bingo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=bingo
DB_PASSWORD=bingo123
JWT_SECRET=change-this-secret-in-production-change-this-secret
FRONTEND_URL=http://localhost:4200
PUBLIC_URL=http://localhost:8080
```

### 3. Levantar el Frontend
```bash
cd frontend
npm install
npm start
```
Abre tu navegador en `http://localhost:4200`.

---

## 🔑 Usuarios Iniciales (Seeders)
Las contraseñas de todos los representantes iniciales están cifradas con BCrypt.
Contraseña por defecto: **`Bingo2026`**

**Usuarios disponibles:**
*   Administrador: `admin`
*   Representantes GEOURP: `geourp01`, `geourp02`, `geourp03`, `geourp04`
*   Representantes CIVIAL: `civial01`, `civial02`, `civial03`, `civial04`
*   Representantes ACI: `aci01`, `aci02`, `aci03`, `aci04`

---

## 🔌 Endpoints Principales

*   `POST /api/auth/login` - Autenticación y obtención de JWT.
*   `POST /api/representative/cards/generate` - Asignación de cartón (requiere idempotency key).
*   `GET /api/cards/{serial}/pdf` - Descarga del PDF de un cartón.
*   `GET /api/public/cards/{serial}/verify` - Endpoint público de validación de autenticidad.
