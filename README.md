# obraia-backend

API REST de **ObraIA**, plataforma con inteligencia artificial para planear, presupuestar y hacer seguimiento a obras pequeñas.
Proyecto final de Patrones de Software: Kevin Basante y Arley Riascos.

El backend es la única capa que se comunica con la base de datos y con el servicio de IA.
El frontend solo conoce la URL de esta API.

## Tecnologías

- Java 17 + Spring Boot 4
- PostgreSQL (Neon) con migraciones Flyway
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
| `FRONTEND_URL` | Orígenes del frontend permitidos (CORS), separados por comas            |
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

Los errores siempre responden en JSON con la forma `{ "error": { "code", "message" } }`.

## Estructura

Arquitectura por capas: controladores → servicios → repositorios → base de datos.

```
database/                  Diagrama entidad-relación y documentación de la base
src/main/java/com/obraia/backend/
├── config/                CORS y configuración de Swagger
├── controller/            Reciben la petición y responden con el código HTTP correcto
├── dto/                   Objetos que se envían como respuesta (JSON)
├── exception/             Errores de la aplicación y manejador global de errores
├── repository/            Acceso a la base de datos (patrón Repository)
├── service/               Lógica de negocio
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
| `FRONTEND_URL` | URL del frontend en Vercel                               |

Cada vez que se sube un commit a `main`, Render vuelve a desplegar automáticamente.
En el plan gratuito el servicio se duerme tras unos minutos sin uso: la primera petición después de eso tarda cerca de un minuto.

## Convenciones

- Código, comentarios, rutas y claves JSON en inglés; documentación en español.
- Commits con prefijo según el tipo de cambio: `feat:`, `fix:`, `docs:`, `chore:`, `test:`.
- La base de datos solo cambia con migraciones nuevas (ver `database/README.md`).
