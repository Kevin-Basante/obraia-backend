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
| `STORAGE_PROVIDER` | Dónde se guardan los planos: `database` (por defecto) o `local` (carpeta `uploads/`) |
| `AI_SERVICE_URL` | URL del servicio de IA (`obraia-ia`), por ejemplo `http://localhost:8000`. Vacía apaga el análisis de planos |
| `AI_SERVICE_TOKEN` | Clave compartida con el servicio de IA (su `SERVICE_TOKEN`) |
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
| GET    | `/api/catalogs/locations`                           | Ciudades con su % de transporte (`?department=Antioquia`, `?search=bogota`) |
| GET    | `/api/catalogs/locations/departments`               | Departamentos con ciudades                                          |
| GET    | `/api/catalogs/material-categories`                 | Categorías de materiales                                            |
| GET    | `/api/catalogs/materials`                           | Materiales con precio de referencia (`?category=steel`, `?search=arena`) |
| GET    | `/api/catalogs/materials/{id}`                      | Un material                                                         |
| GET    | `/api/catalogs/element-types`                       | Elementos que se miden, su unidad (m³ o m²) y las medidas que necesitan |

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

### Planos (requieren token)

| Método | Ruta                                           | Qué hace                                                  |
| ------ | ---------------------------------------------- | --------------------------------------------------------- |
| GET    | `/api/projects/{id}/blueprints`                | Planos de la obra, del más nuevo al más viejo             |
| POST   | `/api/projects/{id}/blueprints`                | Subir un plano (`multipart/form-data`, campo `file`)      |
| GET    | `/api/projects/{id}/blueprints/{planoId}/file` | Abrir el archivo del plano                                |
| DELETE | `/api/projects/{id}/blueprints/{planoId}`      | Borrar un plano (sus medidas se conservan)                |
| POST   | `/api/projects/{id}/blueprints/{planoId}/analyze` | Leer el plano con la IA y guardar sus medidas          |

### Medidas (requieren token)

| Método | Ruta                                              | Qué hace                                                 |
| ------ | ------------------------------------------------- | -------------------------------------------------------- |
| GET    | `/api/projects/{id}/measurements`                 | Medidas de la obra (`?floor=1`, `?element=wall`)         |
| POST   | `/api/projects/{id}/measurements`                 | Agregar una medida manual                                |
| PUT    | `/api/projects/{id}/measurements/{medidaId}`      | Editar una medida manual                                 |
| DELETE | `/api/projects/{id}/measurements/{medidaId}`      | Borrar una medida manual                                 |
| GET    | `/api/projects/{id}/measurements/comparison`      | Totales manuales contra los de la IA, por elemento y piso |

### Fases y tareas (requieren token)

| Método | Ruta                                               | Qué hace                                                      |
| ------ | -------------------------------------------------- | ------------------------------------------------------------- |
| GET    | `/api/projects/{id}/phases`                        | Fases con sus tareas y el avance de la obra                   |
| GET    | `/api/projects/{id}/phases/preview`                | Ver las fases que tendría el plan, sin guardarlas             |
| POST   | `/api/projects/{id}/phases/generate`               | Crear el plan con las fases estándar del tipo de obra         |
| DELETE | `/api/projects/{id}/phases`                        | Borrar el plan (solo si no tiene reportes de avance)          |
| PUT    | `/api/projects/{id}/phases/{faseId}`               | Cambiar el nombre o las fechas planeadas de una fase          |
| PATCH  | `/api/projects/{id}/phases/{faseId}/status`        | Cambiar el estado de una fase                                 |
| POST   | `/api/projects/{id}/tasks`                         | Agregar una tarea a una fase                                  |
| PUT    | `/api/projects/{id}/tasks/{tareaId}`               | Editar una tarea (o pasarla a otra fase)                      |
| DELETE | `/api/projects/{id}/tasks/{tareaId}`               | Borrar una tarea con sus reportes                             |
| PATCH  | `/api/projects/{id}/tasks/{tareaId}/status`        | Cambiar el estado de una tarea                                |
| GET    | `/api/projects/{id}/tasks/{tareaId}/progress`      | Reportes de avance de la tarea, del más nuevo al más viejo    |
| POST   | `/api/projects/{id}/tasks/{tareaId}/progress`      | Reportar el avance de la tarea (0 a 100 %)                    |

### Presupuestos (requieren token)

| Método | Ruta                                               | Qué hace                                                      |
| ------ | -------------------------------------------------- | ------------------------------------------------------------- |
| GET    | `/api/projects/{id}/budgets`                       | Versiones del presupuesto, de la más nueva a la más vieja     |
| GET    | `/api/projects/{id}/budgets/preview`               | Cotizar la obra sin guardar                                   |
| POST   | `/api/projects/{id}/budgets`                       | Cotizar la obra y guardarla como una versión nueva (borrador) |
| GET    | `/api/projects/{id}/budgets/{presupuestoId}`       | Una versión con sus filas (una por capa de costo)             |
| POST   | `/api/projects/{id}/budgets/{presupuestoId}/approve` | Aprobar un borrador; el aprobado anterior queda reemplazado |
| GET    | `/api/projects/{id}/materials`                     | Materiales de la última cotización, con desperdicio           |

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

### Eliminar una obra

1. El frontend pide `deletion-impact` y muestra las advertencias: planos, fases, presupuestos, gastos y miembros que tiene la obra.
2. Solo el creador puede pedir la eliminación. El backend envía un correo con un enlace que vence en 30 minutos. Si el correo no sale, la solicitud se cancela (503).
3. El enlace abre el frontend (`/confirm-deletion?token=...`), que llama a `/api/projects/deletion/confirm`. La obra desaparece de la lista de inmediato.
4. Los datos se guardan 30 días por si hubo un error y después una tarea diaria (4:00 a. m., hora de Colombia) los borra para siempre.

## Cómo funcionan los planos y las medidas

- Cada obra puede tener hasta 10 planos de máximo 10 MB: PDF o imagen (PNG, JPG o WEBP). El backend reconoce el tipo por los primeros bytes del archivo, no por el nombre, así que un archivo renombrado a `.pdf` se rechaza.
- Un PDF con texto (hecho en un programa de dibujo) queda como `vector_pdf`; uno sin texto, como `scanned_pdf`. Las fotos quedan como `image`. Todos empiezan en estado `pending`; al analizarlos con la IA quedan `completed` o, si la IA falla, `failed` (se pueden analizar de nuevo).
- **Patrón Bridge:** los servicios guardan los archivos a través de la interfaz `BlueprintStorage`, sin saber dónde quedan. Hay dos implementaciones:
  - `DatabaseBlueprintStorage` (`STORAGE_PROVIDER=database`): guarda los bytes en la tabla `blueprint_files` (migración V4). Es la que se usa en Render, porque su plan gratuito borra el disco en cada despliegue.
  - `LocalBlueprintStorage` (`STORAGE_PROVIDER=local`): guarda los archivos en la carpeta `uploads/`, que no se sube a Git.
- Cada medida es de un elemento de la obra en un piso. Las medidas que necesita dependen del elemento:

  | Elemento                                   | Unidad | Medidas          | Cantidad calculada        |
  | ------------------------------------------ | ------ | ---------------- | ------------------------- |
  | Cimentación, columnas, vigas, losas        | m³     | largo, ancho, alto | largo × ancho × alto × cantidad |
  | Muros                                      | m²     | largo, alto      | largo × alto × cantidad   |
  | Piso y cubierta                            | m²     | largo, ancho     | largo × ancho × cantidad  |

- El piso no puede ser mayor que los pisos de la obra. Si falta una medida, la respuesta 400 indica el campo en `fields`.
- Las medidas leídas por la IA (fuente `ocr`) no se pueden editar ni borrar. La comparación suma cada elemento por piso y marca `match` si la diferencia es de 10 % o menos y `review` si es mayor. Mientras la IA no mida, cada fila sale como `manual_only`.
- Una obra terminada o cancelada no acepta planos ni medidas nuevas, pero se pueden seguir consultando.
- Al borrar una obra para siempre (tarea de las 4:00 a. m.) también se borran los archivos de sus planos.

## Cómo funciona la lectura de planos con IA

La IA vive en otro servicio, `obraia-ia` (Python con FastAPI y Gemini), porque el curso pide que esté separada. El backend lo llama así:

```text
BlueprintAnalysisService / QuotationFacade  →  AiServiceProxy  →  AiServiceAdapter  →  obraia-ia  →  Gemini
```

- **Patrón Adapter:** `AiServiceAdapter` es el único que sabe cómo se llama a `obraia-ia`: arma el formulario `multipart/form-data` con el plano y los pisos, envía la clave en `X-Service-Token`, lee la respuesta en `snake_case` y la traduce a clases del backend (`ElementType`, `BigDecimal` con dos decimales). Los elementos que el backend no conoce se cuentan como descartados. Si el servicio falla, no responde a tiempo o su respuesta no se puede leer, lanza 502 `AI_SERVICE_FAILED` con el mensaje que mandó `obraia-ia` (por ejemplo, `The AI service answered 502: Gemini answered 400: ...`). Si `obraia-ia` responde `ANALYZER_BUSY` (Gemini sigue saturado después de sus reintentos), lanza 503 `AI_SERVICE_BUSY`: el plano queda como fallido y se puede analizar de nuevo más tarde.
- **Patrón Proxy:** `AiServiceProxy` tiene la misma interfaz (`AiServiceClient`) y se pone delante del adaptador, así que quien lo usa no nota la diferencia. Hace tres cosas:
  - **Protección:** si `AI_SERVICE_URL` está vacía responde 503 `AI_SERVICE_DISABLED`, y cada obra puede llamar a la IA máximo 5 veces por hora (`app.ai.max-analyses-per-project`), porque el plan gratuito del modelo tiene cuota. Al pasarse responde 429 `AI_LIMIT_REACHED`. Solo cuentan los análisis que funcionan: si la IA falla o está saturada, el intento se devuelve.
  - **Caché:** guarda en memoria los últimos 50 análisis por el contenido del archivo (SHA-256) y los pisos. El mismo plano no se envía dos veces: la respuesta sale de la caché (`cached: true`) y no cuenta para el límite.
  - **Carga perezosa:** el adaptador es un bean `@Lazy`; se crea la primera vez que hay que leer un plano, no al arrancar.
- **Análisis** (`POST .../blueprints/{planoId}/analyze`): guarda lo que leyó la IA como medidas con fuente `ocr` ligadas al plano y deja el plano en `completed`. Analizar otra vez reemplaza las medidas que la IA había leído de ese plano. Las medidas en un piso que la obra no tiene se descartan.
- **Diferencia con lo manual:** cada medida de la IA guarda en `discrepancy_pct` cuánto se aleja el total de la IA del total manual para ese elemento y piso (vacío si no hay medidas manuales).
- La primera llamada después de un rato puede tardar cerca de un minuto, porque Render apaga `obraia-ia` cuando no se usa (el tiempo de espera es de 150 segundos).

## Cómo funcionan las fases y las tareas

- **Patrón Factory Method:** cada tipo de obra tiene su fábrica, que hereda de `ProjectCalculatorFactory` y decide qué planificador de fases crear (`createPhasePlanner()`):
  - `HouseCalculatorFactory` (Vivienda unifamiliar) crea `HousePhasePlanner`.
  - `CommercialUnitCalculatorFactory` (Local comercial) crea `CommercialUnitPhasePlanner`, que agrega al final la fase "Adecuación comercial (fachada, vitrina y cortina metálica)" (7 días hábiles en obra pequeña, 10 en grande).
  - La misma fábrica crea el calculador de materiales (`createMaterialCalculator()`), que usa la cotización. Los dos tipos usan hoy `StandardMaterialCalculator`.
  - Los servicios solo llaman a `planPhases(...)` y `calculateMaterials(...)`, así que un tipo de obra nuevo es una clase nueva, sin cambiar los servicios.
- El plan parte de las fases estándar del catálogo según el tamaño de la obra (pequeña hasta 120 m², grande desde ahí). Cada fase trae sus tareas iniciales, que se pueden editar, borrar o completar con tareas nuevas.
- Las fechas se calculan en días hábiles de lunes a sábado desde la fecha de inicio planeada de la obra; si la obra no tiene fecha, las fases quedan sin fechas. Cada fase empieza el día hábil siguiente al fin de la anterior.
- Estados de fases y tareas: `planned` → `in_progress` → `delayed` o `completed`. Una tarea o fase completada se puede reabrir (`in_progress`). Una fase o tarea está atrasada (`late`) si pasó su fecha de fin planeada y no está completada.
- Reportar avance mueve la tarea: más de 0 % la inicia y 100 % la completa. La fase empieza con su primera tarea y se completa sola cuando todas sus tareas están completas. Una fase no se puede completar a mano si le faltan tareas.
- El avance de una tarea es su último reporte (por fecha del reporte). El avance de una fase es el promedio de sus tareas y el de la obra es el promedio de todas las tareas.
- El reporte puede indicar que lo dio el maestro de obra asignado (`fromSiteManager: true`); si la obra no tiene maestro asignado, la respuesta es 400. La fecha del reporte no puede ser futura y, si no se envía, es hoy (hora de Colombia).
- El plan solo se puede crear una vez (409 `PHASES_ALREADY_CREATED`) y solo se puede borrar mientras no tenga reportes (409 `PHASES_HAVE_PROGRESS`).

## Cómo funciona la cotización

- **Patrón Facade:** `QuotationFacade.quoteProject(obra)` es la única llamada que hace el servicio de presupuestos. Por dentro elige las medidas, pide las cantidades a la fábrica del tipo de obra, toma el % de transporte de la ciudad y los días hábiles del plan, y arma las capas de costo.
- **Qué medidas usa:** las manuales si hay; si no, las que la IA ya leyó; si tampoco hay, le pide a la IA (a través de `AiServiceProxy`) que lea el plano más nuevo de la obra. `measurementSource` en la respuesta dice si se cotizó con medidas `manual` u `ocr`.
- **Cantidades:** para cada elemento medido se suma su cantidad (m³ o m²) y se multiplica por el consumo de cada fórmula de ese elemento (por ejemplo 12,5 bloques por m² de muro). El desperdicio de cada fórmula (5 % o 10 %) se calcula aparte.
- **Patrón Decorator:** el costo es un `CostComponent`. `MaterialsCost` es el componente base y cada decorador lo envuelve y agrega una capa:

  | Orden | Capa                       | Cómo se calcula                                                        |
  | ----- | -------------------------- | ---------------------------------------------------------------------- |
  | 1     | `MaterialsCost` (materiales) | Cantidad exacta × precio de referencia (el precio ya incluye IVA)      |
  | 2     | `WasteCost` (desperdicio)  | Material extra por roturas y cortes, según el desperdicio de cada fórmula |
  | 3     | `TransportCost` (transporte) | % de la ciudad sobre materiales y desperdicio (3 % en Bogotá, 6 % en Pasto) |
  | 4     | `LaborCost` (mano de obra) | Días hábiles × costo diario de la cuadrilla (`app.quotation.labor-daily-rate`, $280.000) |
  | 5     | `TaxCost` (IVA)            | 19 % sobre mano de obra y transporte (`app.quotation.vat-pct`)          |

  Cada capa es una fila del presupuesto (`budget_items`). Agregar una capa nueva (por ejemplo imprevistos) es un decorador nuevo, sin tocar los demás.
- **Días hábiles:** si la obra ya tiene fases con fechas, se cuentan los días hábiles entre la primera y la última; si no, se suman las duraciones de las fases estándar del tipo de obra (`workingDaysSource` dice cuál se usó).
- **Patrón Builder:** el resultado (`Quotation`) se arma paso a paso con `Quotation.builder()`, porque tiene muchas partes; `build()` revisa que estén las obligatorias.
- Cada cotización guardada es una versión nueva en borrador (`draft`) y reemplaza la lista de materiales de la obra (`project_materials`) con el precio de ese momento. Al aprobar una versión, la aprobada antes pasa a `superseded`; la base de datos solo permite un presupuesto aprobado por obra.
- La migración V5 agrega el % de transporte a cada ciudad y los tipos de fila `waste` y `transport`.
- Errores: sin medidas (ni manuales, ni de la IA, ni planos que leer) responde 409 `NO_MEASUREMENTS`; si ningún elemento medido tiene fórmulas, 409 `NO_MATERIAL_FORMULAS`; aprobar algo que no es borrador, 409 `BUDGET_NOT_DRAFT`.

## Caché

El backend usa la caché de Spring (`@EnableCaching` en `CacheConfig`) guardada en memoria (`ConcurrentMapCacheManager`).

| Caché              | Qué guarda                                   | Cuándo se borra                                               |
| ------------------ | -------------------------------------------- | ------------------------------------------------------------- |
| `projectTypes`, `locations`, `materials`, `phaseTemplates`, `materialFormulas` | Catálogos (`@Cacheable` en los repositorios) | Nunca: solo cambian con migraciones, que reinician el servidor |
| `budgetVersions`   | Versiones del presupuesto de cada obra (`BudgetVersions.list`) | `@CacheEvict` cada vez que se cotiza o se aprueba |
| Análisis de la IA  | Últimos 50 planos leídos, por su contenido (caché propia de `AiServiceProxy`) | Sale el más viejo cuando se llena, o al reiniciar el servidor |

Para borrar `budgetVersions` en el momento correcto se usa el **patrón Observer** con los eventos de Spring: `BudgetService` publica `BudgetChangedEvent` y `BudgetVersions.onBudgetChanged` lo escucha con `@TransactionalEventListener`, que espera a que el cambio quede guardado en la base de datos antes de borrar la caché.
Además, los catálogos responden con `Cache-Control: max-age=600` para que el navegador también los guarde.

## Patrones de diseño

| Patrón          | Dónde                                                                 |
| --------------- | --------------------------------------------------------------------- |
| Facade          | `QuotationFacade`: una sola llamada para cotizar una obra            |
| Decorator       | `CostComponent`, `MaterialsCost` y los decoradores `WasteCost`, `TransportCost`, `LaborCost`, `TaxCost` |
| Factory Method  | `ProjectCalculatorFactory` y una fábrica por tipo de obra (planificador de fases y calculador de materiales) |
| Adapter         | `AiServiceAdapter`: traduce las llamadas y respuestas del servicio de IA en Python |
| Proxy           | `AiServiceProxy`: protección (servicio apagado y límite por obra), caché y carga perezosa del adaptador |
| Builder         | `Quotation.builder()`                                                |
| Bridge          | `BlueprintStorage` con almacenamiento en base de datos o en carpeta  |
| Template Method | `StandardPhasePlanner`: el algoritmo del plan es fijo y cada tipo de obra pone sus tareas y fases extra |
| Strategy        | `EmailSender`: correo por consola o por Brevo según `MAIL_PROVIDER`  |
| Observer        | `BudgetChangedEvent` y `BudgetVersions` (eventos de Spring)          |
| Repository, DTO y capas | Repositorios de Spring Data, objetos de `dto/` y controladores → servicios → repositorios |

## Estructura

Arquitectura por capas: controladores → servicios → repositorios → base de datos.

```
database/                  Diagrama entidad-relación y documentación de la base
src/main/java/com/obraia/backend/
├── ai/                    Servicio de IA: interfaz, adaptador (Adapter), proxy (Proxy) y resultados
├── config/                Seguridad, JWT, CORS, correo, almacenamiento, IA, Swagger y propiedades
├── controller/            Reciben la petición y responden con el código HTTP correcto
├── dto/                   Objetos que se reciben y se envían (JSON)
├── email/                 Envío de correos (consola o Brevo)
├── event/                 Eventos de la aplicación (Observer)
├── entity/                Tablas de la base de datos como clases Java (JPA)
├── entity/blueprint/      Planos, medidas y reglas para calcular cada elemento
├── entity/budget/         Presupuestos, sus filas y los materiales de cada obra
├── entity/catalog/        Catálogos: tipos de obra, ciudades, materiales, fases y fórmulas
├── entity/phase/          Fases, tareas, reportes de avance y sus estados
├── entity/project/        Obras, miembros, maestros de obra y solicitudes de eliminación
├── exception/             Errores de la aplicación y manejador global de errores
├── planning/              Fábricas por tipo de obra (Factory Method), planificadores, calculador de materiales y calendario de obra
├── quotation/             Cotización: fachada (Facade), capas de costo (Decorator) y resultado (Builder)
├── repository/            Acceso a la base de datos (patrón Repository)
├── security/              Tokens JWT, hash de tokens y respuestas 401/403 en JSON
├── service/               Lógica de negocio
├── storage/               Dónde se guardan los archivos de los planos (base de datos o carpeta)
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
| `AI_SERVICE_URL` | URL del servicio `obraia-ia` en Render                 |
| `AI_SERVICE_TOKEN` | El mismo valor de `SERVICE_TOKEN` en `obraia-ia`     |

`STORAGE_PROVIDER` no hace falta en Render: por defecto los planos se guardan en la base de datos.

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
