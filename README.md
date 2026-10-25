# obraia-backend

API REST de **ObraIA**, plataforma con inteligencia artificial para planear, presupuestar y hacer seguimiento a obras pequeñas.
Proyecto final de Patrones de Software: Kevin Basante y Arley Riascos.

El backend es la única capa que se comunica con la base de datos y con el servicio de IA.
El frontend solo conoce la URL de esta API.

## Tecnologías

- Java 17 + Spring Boot 4
- Spring Security con tokens JWT para la autenticación
- PostgreSQL (Neon) con migraciones Flyway
- Brevo (API HTTP) para enviar los correos de la cuenta
- Swagger (springdoc-openapi) para la documentación
- Docker para el despliegue en Render

## Requisitos

- Java 17 o superior
- Git
- PostgreSQL local (opcional, para desarrollo)

No hace falta instalar Maven: el proyecto trae el Maven Wrapper (`mvnw`).

## Instalación

```bash
git clone https://github.com/Kevin-Basante/obraia-backend.git
cd obraia-backend
```

## Variables de entorno

1. Copia `.env.example` y renómbralo a `.env`.
2. Completa los valores de tu base de datos (local o Neon).

El archivo `.env` está en `.gitignore` y **nunca** se sube a GitHub.

| Variable       | Descripción                                                             |
| -------------- | ----------------------------------------------------------------------- |
| `DB_URL`       | Conexión JDBC, por ejemplo `jdbc:postgresql://localhost:5432/obraia_dev` |
| `DB_USER`      | Usuario de la base de datos                                             |
| `DB_PASSWORD`  | Contraseña de la base de datos                                          |
| `FRONTEND_URL` | Orígenes del frontend permitidos (CORS), separados por comas. El primero se usa en los enlaces de los correos |
| `JWT_SECRET`   | Clave para firmar los tokens: texto aleatorio de al menos 32 caracteres  |
| `MAIL_PROVIDER`| `log` muestra los correos en la consola (desarrollo); `brevo` los envía   |
| `MAIL_FROM`    | Remitente de los correos (debe estar verificado en Brevo)               |
| `BREVO_API_KEY`| Clave de la API de Brevo (solo si `MAIL_PROVIDER=brevo`)                |
| `PORT`         | Puerto del servidor (por defecto 8080; Render lo asigna solo)           |

## Comandos

| Comando (Windows)                 | Comando (Mac / Linux)          | Qué hace                                  |
| --------------------------------- | ------------------------------ | ----------------------------------------- |
| `.\mvnw.cmd spring-boot:run`      | `./mvnw spring-boot:run`       | Inicia el servidor en modo desarrollo     |
| `.\mvnw.cmd test`                 | `./mvnw test`                  | Ejecuta las pruebas unitarias             |
| `.\mvnw.cmd package -DskipTests`  | `./mvnw package -DskipTests`   | Genera el `.jar` en `target/`             |

Al arrancar, Flyway crea las tablas y carga los datos iniciales si todavía no existen.

## Endpoints

| Método | Ruta             | Respuesta                                                                                |
| ------ | ---------------- | ---------------------------------------------------------------------------------------- |
| GET    | `/`              | Información general de la API                                                            |
| GET    | `/api/health`    | Estado del servidor y de la base de datos (200 si está conectada, 503 si no)             |
| GET    | `/api/hello`     | "Hello world" con datos leídos de la base de datos (503 si la base no responde)          |
| GET    | `/api/docs`      | Documentación interactiva (Swagger)                                                      |
| GET    | `/api/docs.json` | Documento OpenAPI en JSON                                                                |

### Autenticación (públicos)

| Método | Ruta                             | Qué hace                                                        |
| ------ | -------------------------------- | --------------------------------------------------------------- |
| POST   | `/api/auth/register`             | Crea la cuenta y envía el correo de verificación (201)          |
| POST   | `/api/auth/verify-email`         | Confirma el correo con el token del enlace                      |
| POST   | `/api/auth/resend-verification`  | Envía un nuevo enlace de verificación (202)                     |
| POST   | `/api/auth/login`                | Inicia sesión y devuelve el token de acceso y el de renovación  |
| POST   | `/api/auth/refresh`              | Entrega tokens nuevos con el token de renovación                |
| POST   | `/api/auth/logout`               | Cierra la sesión (revoca el token de renovación) (204)          |
| POST   | `/api/auth/forgot-password`      | Envía el enlace para cambiar la contraseña (202)                |
| POST   | `/api/auth/reset-password`       | Cambia la contraseña con el token del enlace                    |

### Catálogos (públicos, solo lectura)

| Método | Ruta                                                | Qué hace                                                            |
| ------ | --------------------------------------------------- | ------------------------------------------------------------------- |
| GET    | `/api/catalogs/project-types`                       | Tipos de obra                                                       |
| GET    | `/api/catalogs/project-types/{id}`                  | Un tipo de obra                                                     |
| GET    | `/api/catalogs/project-types/{id}/phases`           | Fases estándar y duración total (`?size=small` o `?area=95`)        |
| GET    | `/api/catalogs/project-types/{id}/material-formulas` | Consumo de materiales por elemento (`?element=wall`)               |
| GET    | `/api/catalogs/locations`                           | Ciudades (`?department=Antioquia`, `?search=bogota`)                |
| GET    | `/api/catalogs/locations/departments`               | Departamentos con ciudades                                          |
| GET    | `/api/catalogs/material-categories`                 | Categorías de materiales                                            |
| GET    | `/api/catalogs/materials`                           | Materiales con precio de referencia (`?category=steel`, `?search=arena`) |
| GET    | `/api/catalogs/materials/{id}`                      | Un material                                                         |

### Usuario (requieren token)

| Método | Ruta            | Qué hace                         |
| ------ | --------------- | -------------------------------- |
| GET    | `/api/users/me` | Datos del usuario que inició sesión |

### Obras (requieren token)

| Método | Ruta                                  | Qué hace                                                        |
| ------ | ------------------------------------- | --------------------------------------------------------------- |
| POST   | `/api/projects`                       | Crear una obra (queda en borrador)                              |
| GET    | `/api/projects`                       | Mis obras, de la más nueva a la más vieja (`?status=draft`, `?search=casa`) |
| GET    | `/api/projects/{id}`                  | Datos completos de una obra                                     |
| PUT    | `/api/projects/{id}`                  | Editar una obra                                                 |
| PATCH  | `/api/projects/{id}/status`           | Cambiar el estado (`{ "status": "in_progress" }`)               |
| PUT    | `/api/projects/{id}/site-manager`     | Asignar un maestro de obra                                      |
| DELETE | `/api/projects/{id}/site-manager`     | Quitar el maestro de obra                                       |
| GET    | `/api/projects/{id}/members`          | Personas que administran la obra                                |
| POST   | `/api/projects/{id}/members`          | Agregar a un usuario registrado por su correo                   |
| DELETE | `/api/projects/{id}/members/{userId}` | Quitar a una persona de la obra                                 |
| GET    | `/api/projects/{id}/deletion-impact`  | Lo que se perdería al eliminar la obra                          |
| POST   | `/api/projects/{id}/deletion-request` | Enviar el correo para confirmar la eliminación                  |
| POST   | `/api/projects/deletion/confirm`      | Confirmar la eliminación con el token del correo (pública)      |

### Maestros de obra (requieren token)

| Método | Ruta                       | Qué hace                         |
| ------ | -------------------------- | -------------------------------- |
| GET    | `/api/site-managers`       | Mis maestros de obra             |
| POST   | `/api/site-managers`       | Guardar un maestro de obra       |
| PUT    | `/api/site-managers/{id}`  | Editar un maestro de obra        |
| DELETE | `/api/site-managers/{id}`  | Borrar un maestro de obra        |

Las rutas protegidas necesitan el encabezado `Authorization: Bearer <accessToken>`.
En Swagger se usa el botón **Authorize** con el token que devuelve el login.

Los errores siempre responden en JSON con la forma `{ "error": { "code", "message" } }`.
Los errores de validación agregan `fields` con el problema de cada campo.

## Cómo funciona la autenticación

1. **Registro:** la contraseña se guarda cifrada con BCrypt y se envía un enlace de verificación que vence en 24 horas.
2. **Verificación:** el enlace abre el frontend (`/verify-email?token=...`), que llama a `/api/auth/verify-email`. Sin verificar el correo no se puede iniciar sesión.
3. **Inicio de sesión:** devuelve un **token de acceso** (JWT, dura 15 minutos) y un **token de renovación** (dura 7 días).
4. **Renovación:** cuando el token de acceso vence, el frontend llama a `/api/auth/refresh`. Cada token de renovación sirve una sola vez; si alguien reutiliza uno viejo, se cierran todas las sesiones de ese usuario.
5. **Contraseña olvidada:** el enlace vence en 30 minutos. Al cambiar la contraseña se cierran todas las sesiones.

En la base de datos solo se guarda el hash SHA-256 de los tokens, nunca el token. Cada evento (registro, inicio de sesión, intento fallido, cambio de contraseña...) queda en la tabla `auth_audit_logs`.
Los mensajes de "correo enviado" son iguales exista o no la cuenta, para que nadie pueda averiguar qué correos están registrados.

Con `MAIL_PROVIDER=log` los correos no se envían: el enlace aparece en la consola del servidor, para probar en local.

## Cómo funcionan los catálogos

- Los datos vienen de la migración V3 y no cambian mientras el servidor está encendido. Por eso el backend los guarda en memoria (caché) después de la primera consulta y no vuelve a preguntarle a la base de datos.
- Las respuestas llevan `Cache-Control: max-age=600`, así el navegador puede reutilizarlas durante 10 minutos.
- Las búsquedas ignoran mayúsculas y tildes: `bogota` encuentra "Bogotá" y `rio` encuentra "Arena de río".
- El tamaño de la obra decide sus fases: hasta 120 m² es pequeña (`small`) y más de 120 m² es grande (`large`).
- Un filtro con un valor desconocido responde 400 con la lista de valores válidos; un id que no existe responde 404.

## Cómo funcionan las obras

- Solo los miembros de una obra pueden verla o cambiarla. Para cualquier otra persona la obra responde 404, como si no existiera.
- Quien crea la obra es su primer miembro (rol `owner`) y puede agregar a otros usuarios con cuenta verificada. Al creador no se le puede quitar.
- Estados y cambios permitidos:

  | Estado        | Puede pasar a                          |
  | ------------- | -------------------------------------- |
  | `draft`       | `planned`, `cancelled`                 |
  | `planned`     | `draft`, `in_progress`, `cancelled`    |
  | `in_progress` | `paused`, `completed`, `cancelled`     |
  | `paused`      | `in_progress`, `cancelled`             |
  | `completed`   | ninguno (final)                        |
  | `cancelled`   | ninguno (final)                        |

  La primera vez que una obra pasa a `in_progress` se guarda la fecha real de inicio; al pasar a `completed`, la fecha real de fin. Una obra terminada o cancelada ya no se puede editar.
- La respuesta de una obra trae `nextStatuses`, así el frontend solo ofrece los cambios válidos.
- Cada usuario guarda sus propios maestros de obra. Si borra uno, sus obras quedan sin maestro asignado.

## Estructura

Arquitectura por capas: controladores → servicios → repositorios → base de datos.

```
database/                  Diagrama entidad-relación y documentación de la base
src/main/java/com/obraia/backend/
├── config/                Seguridad, JWT, CORS, correo, Swagger y propiedades
├── controller/            Reciben la petición y responden con el código HTTP correcto
├── dto/                   Objetos que se reciben y se envían (JSON)
├── email/                 Envío de correos (consola o Brevo)
├── entity/                Tablas de la base de datos como clases Java (JPA)
├── entity/catalog/        Catálogos: tipos de obra, ciudades, materiales, fases y fórmulas
├── entity/project/        Obras, miembros, maestros de obra y solicitudes de eliminación
├── exception/             Errores de la aplicación y manejador global de errores
├── repository/            Acceso a la base de datos (patrón Repository)
├── security/              Tokens JWT, hash de tokens y respuestas 401/403 en JSON
├── service/               Lógica de negocio
├── util/                  Utilidades (búsqueda sin tildes)
├── validation/            Reglas de validación propias (contraseña segura)
└── ObraiaBackendApplication.java   Punto de entrada: inicia el servidor
src/main/resources/
├── application.properties Configuración (lee las variables de entorno)
└── db/migration/          Migraciones de Flyway (V1, V2, V3...)
src/test/                  Pruebas unitarias
```

## Despliegue en Render

El servicio se describe en `render.yaml` (región Ohio, plan gratuito, Docker).

| Configuración     | Valor                         |
| ----------------- | ----------------------------- |
| Language          | `Docker` (usa el `Dockerfile`) |
| Health Check Path | `/api/health`                 |

Variables que se configuran en el panel de Render (no van en el repositorio):

| Variable       | Valor                                                    |
| -------------- | -------------------------------------------------------- |
| `DB_URL`       | Conexión JDBC directa de Neon (sin *connection pooling*) |
| `DB_USER`      | Usuario de Neon                                          |
| `DB_PASSWORD`  | Contraseña de Neon                                       |
| `FRONTEND_URL` | URL del frontend en Vercel (primero) y `http://localhost:5173` |
| `JWT_SECRET`   | Texto aleatorio de al menos 32 caracteres (distinto al local) |
| `MAIL_PROVIDER`| `brevo` (o `log` mientras Brevo no esté configurado)     |
| `MAIL_FROM`    | Correo remitente verificado en Brevo                     |
| `BREVO_API_KEY`| Clave de la API de Brevo                                 |

El plan gratuito de Render bloquea el envío de correos por SMTP; por eso se usa la API HTTP de Brevo.

Cada vez que se sube un commit a `main`, Render vuelve a desplegar automáticamente.
En el plan gratuito el servicio se duerme tras unos minutos sin uso: la primera petición después de eso tarda cerca de un minuto.

## Equipo y responsabilidades

| Integrante    | Responsabilidad                                                    |
| ------------- | ------------------------------------------------------------------ |
| Kevin Basante | Backend (este repositorio), base de datos y servicio de IA         |
| Arley Riascos | Frontend (`obraia-frontend`)                                       |

Los dos trabajan juntos en el diseño y revisan el avance de los tres repositorios.

## Convenciones

- Código, comentarios, rutas y claves JSON en inglés; documentación en español.
- Commits con prefijo según el tipo de cambio: `feat:`, `fix:`, `docs:`, `chore:`, `test:`.
- La base de datos solo cambia con migraciones nuevas (ver `database/README.md`).
