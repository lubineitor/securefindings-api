# SecureFindings API

> API REST segura para registrar y gestionar hallazgos de seguridad mediante Java y Spring.

## Estado

🚧 **En desarrollo**

El proyecto se encuentra en una fase activa de construcción. Su objetivo es servir como proyecto práctico para profundizar en:

- Java y desarrollo backend.
- Spring Boot y Spring Security.
- Application Security.
- OWASP.
- Secure Coding.
- Persistencia relacional.
- Testing automatizado.
- Integración continua.
- Control de acceso.
- Aislamiento de datos por organización.
- Protección frente al abuso de la API.
- Trazabilidad de peticiones.

El alcance del MVP, sus criterios de aceptación y las tareas pendientes están en
[`docs/mvp.md`](docs/mvp.md).

## Objetivo

SecureFindings proporciona una API para registrar, consultar y gestionar hallazgos de seguridad aplicando progresivamente principios de desarrollo seguro.

Actualmente se trabajan los siguientes conceptos:

- Gestión completa de hallazgos.
- Severidad y estado.
- Ciclo de vida controlado.
- Búsqueda textual.
- Filtros combinables.
- Ordenación segura.
- Paginación.
- Auditoría de operaciones.
- Filtros del historial de auditoría.
- Comentarios asociados a hallazgos.
- Aislamiento por organización.
- Validación de datos.
- Manejo controlado de errores.
- Autenticación mediante JWT.
- Autorización basada en roles.
- Limitación de peticiones.
- Trazabilidad de peticiones mediante `X-Request-ID`.
- Persistencia con PostgreSQL.
- Migraciones versionadas.
- Pruebas unitarias, web e integración.
- Análisis automatizado de seguridad.

## Funcionalidades implementadas

### Gestión de hallazgos

La API permite:

- Crear hallazgos.
- Consultar un hallazgo por identificador.
- Listar hallazgos de forma paginada.
- Actualizar título, descripción y severidad.
- Actualizar el estado.
- Eliminar hallazgos.
- Mantener los eventos de auditoría después de una eliminación.

Cada hallazgo contiene:

- Identificador único.
- Título.
- Descripción.
- Severidad.
- Estado.
- Fecha de creación.
- Fecha de actualización.

### Ciclo de vida de los hallazgos

Los estados disponibles son:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `FALSE_POSITIVE`

Las transiciones se validan dentro del dominio:

| Estado actual | Estados permitidos |
|---|---|
| `OPEN` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `FALSE_POSITIVE` |
| `IN_PROGRESS` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `FALSE_POSITIVE` |
| `RESOLVED` | `RESOLVED`, `OPEN` |
| `FALSE_POSITIVE` | `FALSE_POSITIVE`, `OPEN` |

Los estados `RESOLVED` y `FALSE_POSITIVE` pueden reabrirse utilizando `OPEN`, pero no pueden cambiar directamente entre sí.

Una transición no permitida devuelve:

```http
409 Conflict
```

con el código:

```text
INVALID_STATUS_TRANSITION
```

La validación se realiza antes de guardar el hallazgo y antes de registrar el evento de auditoría.

### Búsqueda y filtros

El listado permite combinar:

- Búsqueda textual en título y descripción mediante `q`.
- Filtro por severidad.
- Filtro por estado.
- Rango de fechas de creación mediante `createdFrom` y `createdTo`.
- Paginación.
- Ordenación ascendente o descendente.

Los límites de fecha son inclusivos y aceptan instantes en formato ISO-8601.
Si `createdFrom` es posterior a `createdTo`, la API responde `400 VALIDATION_ERROR`.

La búsqueda textual no distingue entre mayúsculas y minúsculas.

### Ordenación segura

La API utiliza una lista controlada de campos de ordenación:

- `createdAt`
- `updatedAt`
- `title`
- `severity`
- `status`

Las direcciones disponibles son:

- `ASC`
- `DESC`

La ordenación predeterminada es:

```text
createdAt DESC
id ASC
```

El identificador se utiliza como segundo criterio para garantizar resultados deterministas cuando varios hallazgos tienen el mismo valor principal.

Los valores de ordenación se validan mediante enums. No se aceptan nombres de propiedades JPA o SQL arbitrarios.

### Auditoría

Las operaciones importantes generan eventos de auditoría:

- `CREATED`
- `UPDATED`
- `DELETED`
- `COMMENTED`

Cada evento registra:

- Hallazgo afectado.
- Acción realizada.
- Usuario responsable.
- Fecha y hora de la operación.
- Organización asociada.
- Identificador de la petición HTTP (`requestId`), cuando existe.

El usuario se obtiene del claim `preferred_username` del token JWT. En operaciones técnicas o pruebas sin autenticación se utiliza el actor `system`.

La auditoría puede consultarse de forma paginada y se conserva incluso cuando el hallazgo es eliminado.

Antes de registrar un evento, el servicio verifica que el hallazgo pertenece a la organización actual. Para conservar la trazabilidad de una eliminación, permite el evento `DELETED` únicamente cuando ya existe historial de ese hallazgo en la misma organización.

Las transiciones de estado no permitidas no generan eventos de auditoría.

El campo `requestId` se obtiene de la cabecera `X-Request-ID` y del contexto MDC. Se persiste en `finding_audit.request_id` y se devuelve al consultar el historial del hallazgo.

También se puede consultar el historial completo de la organización actual:

```text
GET /api/v1/organizations/current/audit

#### Filtros de auditoría

El historial puede filtrarse por:

- Acción de auditoría.
- Identificador de petición.
- Actor.
- Combinaciones de estos filtros.

Ejemplo:

```text
GET /api/v1/findings/{findingId}/audit?occurredFrom=2026-10-01T00:00:00Z&occurredTo=2026-10-19T23:59:59Z
```

Los valores permitidos para `action` son:

```text
CREATED
UPDATED
DELETED
COMMENTED
```

El parámetro `requestId` debe contener entre 1 y 64 caracteres alfanuméricos, puntos, guiones o guiones bajos.

Los filtros:

- Se aplican siempre dentro de la organización actual.
- Utilizan parámetros enlazados.
- No permiten nombres de campos arbitrarios.
- No permiten expresiones SQL.
- Mantienen la paginación y el orden cronológico.

### Comentarios

La API permite añadir comentarios a los hallazgos y consultarlos de forma paginada.

Cada comentario está asociado a:

- Un hallazgo.
- Una organización.
- Un autor.
- Una fecha de creación.
- Un contenido validado.

La creación de un comentario genera un evento de auditoría `COMMENTED`.

### Aislamiento por organización

Los hallazgos, comentarios y eventos de auditoría pertenecen a una organización.

La organización se obtiene del claim:

```text
organization_id
```

del token JWT.

El identificador de organización:

- No se recibe como parámetro confiable desde el cliente.
- Se obtiene del contexto de seguridad.
- Se valida contra la tabla `organizations`.
- Se aplica en las consultas de persistencia.
- Impide consultar o modificar datos de otra organización.

Un usuario puede recibir una respuesta `404` al intentar acceder a un identificador perteneciente a otra organización, evitando revelar información sobre su existencia.

### Limitación de peticiones

La API incorpora un límite configurable para proteger los endpoints internos frente a un uso excesivo o automatizado.

El límite se aplica a los endpoints bajo:

```text
/api/v1/
```

El endpoint público de health check queda excluido:

```text
/api/v1/health
```

La clave utilizada para aplicar el límite es:

- JWT autenticado con `organization_id` UUID válido: organización, emisor (`iss`) y subject (`sub`).
- JWT sin organización válida y otras autenticaciones: nombre principal obtenido del contexto de seguridad.
- Petición no autenticada: dirección IP remota obtenida mediante `getRemoteAddr()`.

La combinación de `iss` y `sub` mantiene la identidad estable aunque cambie `preferred_username`, y evita que dos cuentas con el mismo nombre compartan cuota. Si falta el emisor o el subject en un token con organización válida, se usa el nombre principal dentro de esa organización. Si el claim de organización no contiene un UUID válido, se conserva la cuota por nombre principal.

Este fallback solo determina qué contador consume la petición; no concede acceso. El contexto de organización sigue rechazando los tokens sin un claim válido.

La aplicación no confía directamente en cabeceras como `X-Forwarded-For`, porque podrían ser manipuladas por el cliente si no existe un proxy de confianza correctamente configurado.

La configuración predeterminada es:

```properties
securefindings.rate-limit.max-requests=60
securefindings.rate-limit.window=60s
securefindings.rate-limit.max-tracked-clients=100000
```

También puede configurarse mediante variables de entorno:

```text
SECUREFINDINGS_RATE_LIMIT_MAX_REQUESTS=60
SECUREFINDINGS_RATE_LIMIT_WINDOW=60s
SECUREFINDINGS_RATE_LIMIT_MAX_TRACKED_CLIENTS=100000
```

`max-requests` define la capacidad del cubo de tokens. `window` indica cuánto tarda en reponerse por completo un cubo vacío. Los tokens se recuperan de forma continua: con los valores predeterminados se repone un token por segundo. Cada petición aceptada consume un token; el cliente puede realizar una ráfaga inicial hasta agotar la capacidad y después recibe tokens gradualmente, sin un reinicio brusco en el límite de un intervalo.

`max-tracked-clients` establece el máximo de identidades cuyas cuotas se mantienen a la vez; su valor predeterminado es `100000`. Al alcanzar ese máximo, las identidades nuevas reciben `429` hasta que se limpien cuotas vencidas. Este límite acota la memoria que puede consumir el rate limiter.

Cuando se supera el límite, la API devuelve:

```http
429 Too Many Requests
```

Incluyendo la cabecera:

```http
Retry-After: <segundos>
```

Las respuestas también incluyen:

- `X-RateLimit-Limit`: capacidad total del cubo.
- `X-RateLimit-Remaining`: tokens enteros disponibles tras la petición.
- `X-RateLimit-Reset`: instante Unix, en segundos, en que el cubo volverá a estar lleno.

En una respuesta `429`, `Retry-After` indica el tiempo hasta que se reponga el siguiente token; `X-RateLimit-Reset` señala cuándo se recuperará la capacidad completa.

Ejemplo de respuesta:

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Se ha superado el límite de peticiones",
  "errors": {}
}
```

#### Métricas Prometheus

Las métricas se consultan en `GET /actuator/prometheus`. El endpoint requiere un JWT
con el rol `ADMIN`.

El rate limiter publica el contador `securefindings.rate_limit.requests`, con la
etiqueta `outcome`:

- `allowed`: petición permitida.
- `rate_limited`: petición rechazada por superar el límite.
- `unavailable`: petición rechazada porque el almacenamiento del rate limiter no estaba disponible.

Las etiquetas no contienen usuarios, organizaciones ni direcciones IP.

### Trazabilidad de peticiones

La API asigna a cada petición un identificador de correlación mediante la cabecera:

```http
X-Request-ID
```

Comportamiento:

- Si el cliente envía un identificador válido, se conserva.
- Si no se envía, la aplicación genera un UUID.
- Si el valor recibido no cumple el formato permitido, se reemplaza.
- El identificador se devuelve en la respuesta HTTP.
- También está disponible en el contexto MDC de los logs.
- Se persiste en los eventos de auditoría generados durante la petición.

Los identificadores recibidos deben contener entre 1 y 64 caracteres alfanuméricos, puntos, guiones o guiones bajos.

Ejemplo:

```http
X-Request-ID: finding-request-123
```

La aplicación utiliza el siguiente patrón de logs:

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [requestId=%X{requestId}] %logger{36} - %msg%n
```

Ejemplo:

```text
2026-09-17 10:15:23.421 INFO [http-nio-8080-exec-1] [requestId=finding-request-123] ...
```

El identificador sirve únicamente para correlacionar peticiones y logs. No sustituye la autenticación, la autorización ni la identidad del usuario.

## API REST

### Health check

```text
GET /api/v1/health
```

Este endpoint está disponible sin autenticación y no consume cuota del límite de peticiones.

### Probes de despliegue

Además de `GET /api/v1/health`, la aplicación ofrece pruebas para comprobar su disponibilidad:

| Método | Endpoint | Comprobación |
|---|---|---|
| `GET` | `/livez` | Estado interno de la aplicación. |
| `GET` | `/readyz` | Estado de la aplicación, PostgreSQL y Redis. |
| `GET` | `/actuator/health/liveness` | Equivalente a `/livez`. |
| `GET` | `/actuator/health/readiness` | Equivalente a `/readyz`. |

Las pruebas públicas devuelven únicamente el estado, sin mostrar componentes ni detalles. El endpoint global `/actuator/health` requiere autenticación.

### Hallazgos

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/findings` | Lista hallazgos paginados | `ANALYST`, `ADMIN` |
| `GET` | `/api/v1/findings/{id}` | Obtiene un hallazgo | `ANALYST`, `ADMIN` |
| `POST` | `/api/v1/findings` | Crea un hallazgo | `ANALYST`, `ADMIN` |
| `PUT` | `/api/v1/findings/{id}` | Actualiza los datos | `ANALYST`, `ADMIN` |
| `PATCH` | `/api/v1/findings/{id}/status` | Actualiza el estado | `ANALYST`, `ADMIN` |
| `DELETE` | `/api/v1/findings/{id}` | Elimina un hallazgo | `ADMIN` |
| `GET` | `/api/v1/findings/summary` | Resume hallazgos por estado y severidad | `ANALYST`, `ADMIN` |
| `GET` | `/api/v1/findings/export` | Exporta hallazgos filtrados a CSV | `ANALYST`, `ADMIN` |

#### Resumen de hallazgos

`GET /api/v1/findings/summary` devuelve los recuentos de hallazgos de la
organización asociada al token, agrupados por estado y severidad. Las categorías
sin hallazgos aparecen con valor `0`.

La consulta requiere un token con rol `ANALYST` o `ADMIN`.

Parámetros opcionales:

| Parámetro | Descripción |
|---|---|
| `createdFrom` | Fecha de creación inicial inclusiva, en formato ISO-8601. |
| `createdTo` | Fecha de creación final inclusiva, en formato ISO-8601. |

Si se envían ambos, `createdFrom` debe ser anterior o igual a `createdTo`.

#### Exportación CSV

`GET /api/v1/findings/export` descarga los hallazgos de la organización actual
como `securefindings.csv`. Acepta los filtros `q`, `severity`, `status`,
`createdFrom` y `createdTo`, con el mismo comportamiento que el listado. El orden
es `createdAt DESC` e `id ASC`. Las fechas son inclusivas y `q` admite hasta 100
caracteres.

El archivo contiene las columnas `id`, `title`, `description`, `severity`,
`status`, `createdAt` y `updatedAt`. Se descarga como `text/csv; charset=UTF-8`,
con nombre `securefindings.csv` y la cabecera `Cache-Control: no-store`. La
exportación admite hasta 1.000 filas; si los filtros coinciden con más resultados,
la API devuelve `413` con el código
`EXPORT_LIMIT_EXCEEDED` y el límite en `errors.limit`:

```json
{
  "code": "EXPORT_LIMIT_EXCEEDED",
  "message": "La exportación supera el máximo de 1000 hallazgos",
  "errors": {
    "limit": "1000"
  }
}
```

### Organizaciones

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/organizations` | Lista organizaciones de forma paginada | `ADMIN` |
| `GET` | `/api/v1/organizations/{id}` | Consulta una organización por identificador | `ADMIN` |
| `PATCH` | `/api/v1/organizations/{id}` | Actualiza el nombre visible de una organización | `ADMIN` |
| `POST` | `/api/v1/organizations` | Crea una organización | `ADMIN` |
| `GET` | `/api/v1/organizations/current` | Consulta la organización asociada al token | `ANALYST`, `ADMIN` |
| `PATCH` | `/api/v1/organizations/current` | Actualiza el nombre visible de la organización | `ADMIN` |
| `DELETE` | `/api/v1/organizations/{id}` | Elimina una organización vacía | `ADMIN` |

#### Listar organizaciones

`GET /api/v1/organizations` devuelve el directorio global de organizaciones y
solo está disponible para el rol `ADMIN`. La respuesta se pagina con orden
`name ASC` e `id ASC`.

| Parámetro | Valor predeterminado | Límites |
|---|---:|---|
| `page` | `0` | Mayor o igual que `0` |
| `size` | `20` | Entre `1` y `100` |
| `q` | Sin filtro | Hasta 100 caracteres; busca por nombre o slug sin distinguir mayúsculas |

#### Consultar una organización

`GET /api/v1/organizations/{id}` devuelve una organización por su UUID. Solo
está disponible para el rol `ADMIN`. Si el UUID no existe, la API responde
`404 ORGANIZATION_NOT_FOUND`.

#### Actualizar una organización

`PATCH /api/v1/organizations/{id}` actualiza el nombre visible de una
organización y requiere el rol `ADMIN`. El UUID, el slug y la fecha de creación
se mantienen. El cuerpo utiliza el mismo formato que la actualización de la
organización actual:

```json
{
  "name": "Acme Security Europe"
}
```

#### Crear una organización

`POST /api/v1/organizations` crea una organización y devuelve `201 Created`, el
recurso en el cuerpo y su URL en la cabecera `Location`. La operación requiere
el rol `ADMIN`.

```json
{
  "name": "Acme Security",
  "slug": "acme-security"
}
```

#### Eliminar una organización

`DELETE /api/v1/organizations/{id}` elimina una organización solo si no tiene
hallazgos, comentarios ni eventos de auditoría asociados. La operación requiere
`ADMIN` y devuelve `204 No Content` si se completa. Si conserva datos, devuelve
`409 ORGANIZATION_HAS_ASSOCIATED_DATA`; la API no elimina ni modifica esos datos.

El nombre es obligatorio y admite hasta 150 caracteres. El slug es obligatorio,
único y admite hasta 100 caracteres; solo puede contener letras minúsculas,
números y guiones simples entre segmentos. Si ya existe, la API devuelve
`409 ORGANIZATION_SLUG_ALREADY_EXISTS`.

#### Organización actual

El cuerpo de la petición PATCH contiene el nombre, con un máximo de 150 caracteres:

```json
{
  "name": "Secure Findings Europe"
}
```

### Parámetros del listado

Endpoint:

```text
GET /api/v1/findings
```

Parámetros disponibles:

| Parámetro | Obligatorio | Valor predeterminado | Descripción |
|---|---:|---:|---|
| `page` | No | `0` | Número de página. Empieza en `0`. |
| `size` | No | `20` | Elementos por página. Valores entre `1` y `100`. |
| `q` | No | — | Busca en título y descripción. Máximo `100` caracteres. |
| `severity` | No | — | Filtra por severidad. |
| `status` | No | — | Filtra por estado. |
| `sortBy` | No | `createdAt` | Campo permitido para ordenar. |
| `direction` | No | `DESC` | Dirección `ASC` o `DESC`. |
| `createdFrom` | No | — | Fecha de creación inicial inclusiva, en formato ISO-8601. |
| `createdTo` | No | — | Fecha de creación final inclusiva, en formato ISO-8601. |

Ejemplo:

```text
GET /api/v1/findings?page=0&size=20&q=SQL&severity=HIGH&status=OPEN&sortBy=title&direction=ASC
```

Los filtros de fecha se pueden combinar con los demás parámetros. Se aceptan
instantes ISO-8601, por ejemplo:

```text
GET /api/v1/findings?createdFrom=2026-10-01T00:00:00Z&createdTo=2026-10-17T23:59:59Z
```

La respuesta paginada contiene:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### Auditoría

Endpoint:

```text
GET /api/v1/findings/{findingId}/audit
```

Permite consultar el historial paginado de un hallazgo.

Parámetros:

| Parámetro | Obligatorio | Valor predeterminado | Descripción |
|---|---:|---:|---|
| `page` | No | `0` | Número de página. |
| `size` | No | `20` | Elementos por página. Valores entre `1` y `100`. |
| `action` | No | — | `CREATED`, `UPDATED`, `DELETED` o `COMMENTED`. |
| `requestId` | No | — | Identificador de petición válido, máximo `64` caracteres. |
| `actor` | No | — | Filtra por actor responsable mediante coincidencia exacta. Máximo `255` caracteres. |
| `occurredFrom` | No | — | Fecha y hora inicial inclusiva, en formato ISO-8601. |
| `occurredTo` | No | — | Fecha y hora final inclusiva, en formato ISO-8601. |

Ejemplo sin filtros:

```text
GET /api/v1/findings/3bfa1ad2-eee1-4ea5-ba7c-16b47d1da147/audit?page=0&size=20
```

Ejemplo filtrado:

```text
GET /api/v1/findings/3bfa1ad2-eee1-4ea5-ba7c-16b47d1da147/audit?action=UPDATED&requestId=audit-request-123
```

Los filtros siempre se aplican dentro de la organización asociada al token JWT.

### Comentarios

Crear un comentario:

```text
POST /api/v1/findings/{findingId}/comments
```

Ejemplo de petición:

```json
{
  "content": "Se ha corregido la validación de la entrada"
}
```

Consultar comentarios:

```text
GET /api/v1/findings/{findingId}/comments?page=0&size=20
```

## Respuestas de error

Las respuestas de error utilizan un formato uniforme:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "title": "El título es obligatorio"
  }
}
```

Además, las respuestas incluyen la cabecera:

```http
X-Request-ID: <identificador>
```

Esto permite relacionar una respuesta de error con los logs de la aplicación.

Errores principales:

| HTTP | Código | Situación |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Datos o parámetros inválidos |
| `401` | `UNAUTHORIZED` | Token ausente o inválido |
| `403` | `FORBIDDEN` | Usuario sin permisos suficientes |
| `404` | `FINDING_NOT_FOUND` | Hallazgo no disponible para la organización |
| `409` | `INVALID_STATUS_TRANSITION` | Transición de estado no permitida |
| `429` | `RATE_LIMIT_EXCEEDED` | Límite de peticiones superado |
| `500` | Error interno | Error no controlado |

Los errores de ordenación inválida se responden con `400`:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "sortBy": "El campo de ordenación no está permitido"
  }
}
```

Los errores de acción de auditoría inválida también se responden con `400`.

## Documentación OpenAPI

La API dispone de documentación OpenAPI y Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Especificación OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

Las operaciones protegidas requieren un token Bearer JWT.

## Autenticación y autorización

La aplicación funciona como un OAuth2 Resource Server.

El cliente debe enviar:

```http
Authorization: Bearer <token>
```

Para las operaciones de hallazgos, comentarios, auditoría y las rutas
`/api/v1/organizations/current`, el token debe incluir `organization_id` con el
UUID de una organización existente. Las operaciones globales del directorio
(`GET` y `POST /api/v1/organizations`, además de `GET` y `PATCH`
`/api/v1/organizations/{id}`) requieren `ADMIN` y no dependen de
`organization_id`. El claim `preferred_username` se utiliza como actor en los
eventos de auditoría.

El token para `GET /actuator/prometheus` debe pertenecer al issuer configurado,
incluir la audiencia `securefindings-api` y tener el rol `ADMIN` o
`METRICS_READER`. Este endpoint no requiere `organization_id`; el rol
`METRICS_READER` no concede acceso a las operaciones de negocio de la API.

Los roles se obtienen desde Keycloak:

- `ANALYST`: puede consultar, crear y modificar hallazgos.
- `ADMIN`: puede realizar todas las operaciones sobre hallazgos, incluida la
  eliminación, y administrar el directorio de organizaciones.

La API valida también la audiencia del access token. El claim `aud` debe incluir `securefindings-api`, configurada mediante la variable `JWT_AUDIENCE`.

La aplicación no utiliza sesiones ni autenticación mediante formulario:

```text
SessionCreationPolicy.STATELESS
```

La protección CSRF se ignora únicamente en las rutas REST:

```text
/api/v1/**
```

Fuera de esas rutas, CSRF permanece activo.

Para comprobar la autenticación local, ejecuta el smoke test con la API en marcha
y un access token recién copiado al portapapeles:

```powershell
.\scripts\verificar-autenticacion.ps1
```

### Obtener un token para pruebas locales

El script `scripts/obtener-token.ps1` inicia sesión en Keycloak mediante
Authorization Code con PKCE `S256`, comprueba la audiencia `securefindings-api`
y copia el access token al portapapeles.

El realm importado configura el cliente `securefindings-cli` con Standard Flow,
PKCE `S256`, Direct Access Grants desactivado y esta URI de redirección:
`http://127.0.0.1:8765/callback/`.

```powershell
.\scripts\obtener-token.ps1
```

### Rate limiting

La API aplica un token bucket distribuido mediante Redis a las rutas
`/api/v1/**`, excepto `/api/v1/health`. La sección **“Limitación de peticiones”**
describe cómo se identifica cada cliente, cómo se configura el límite y qué
respuestas devuelve la API cuando se alcanza la cuota o Redis no está disponible.

## Tecnologías

- Java 21.
- Maven.
- Spring Boot.
- Spring Web MVC.
- Spring Data JPA.
- Spring Security.
- OAuth2 Resource Server.
- JWT.
- Keycloak.
- PostgreSQL.
- Flyway.
- Docker Compose.
- JUnit.
- Mockito.
- MockMvc.
- OpenAPI y Swagger UI.
- GitHub Actions.
- CodeQL.

## Estructura del proyecto

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── securefindings/
│   │           ├── api/
│   │           │   └── error/
│   │           ├── audit/
│   │           ├── comment/
│   │           ├── finding/
│   │           ├── health/
│   │           ├── organization/
│   │           └── security/
│   └── resources/
│       ├── application.properties
│       └── db/
│           └── migration/
└── test/
    └── java/
```

## Requisitos locales

Se necesita:

- Java 21.
- Docker Desktop.
- Git.
- Docker Compose para iniciar el Keycloak local incluido en el proyecto. No hace
  falta acceder a una instancia Keycloak externa para seguir esta guía.

## Configuración local

Crear el archivo de configuración local a partir del ejemplo:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` puede contener credenciales de base de datos y Keycloak. Nunca debe subirse al repositorio.

La variable `JWT_AUDIENCE` define la audiencia JWT que espera la API. Su valor local es `securefindings-api`.

Iniciar los servicios de infraestructura:

```powershell
docker compose -f docker-compose.yml -f docker-compose.observability.yml --profile observability up -d prometheus grafana alertmanager mailpit
```

Comprobar los servicios:

```powershell
docker compose ps
```

### Importación local de Keycloak

En el primer arranque, Docker Compose importa el realm `securefindings` desde
`keycloak/securefindings-realm.json`. La configuración crea los roles `ANALYST`
y `ADMIN`, el cliente público `securefindings-cli` con PKCE `S256`, la audiencia
`securefindings-api` y el mapper del atributo `organization_id`.

Keycloak omite la importación si el realm ya existe. Por tanto, este mecanismo no
actualiza ni reemplaza la configuración de una instalación existente. En ese
caso, revisa o modifica el realm desde la consola de administración.

Para cada usuario de pruebas, crea el usuario en el realm y asígnale el rol
`ANALYST` o `ADMIN`. Añade el atributo de usuario `organization_id` con el UUID
de la organización de desarrollo:
`00000000-0000-0000-0000-000000000001`.

### Configuración de Redis

La aplicación utiliza Redis para aplicar el rate limiting distribuido. Estas variables permiten configurar la conexión y limitar cuánto espera la API a Redis:

| Variable | Valor por defecto | Descripción |
| --- | --- | --- |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host de Redis. |
| `SPRING_DATA_REDIS_PORT` | `6379` | Puerto de Redis. |
| `SPRING_DATA_REDIS_CONNECT_TIMEOUT` | `1s` | Tiempo máximo para establecer la conexión. |
| `SPRING_DATA_REDIS_TIMEOUT` | `2s` | Tiempo máximo de espera para leer la respuesta. |

Si Redis no responde dentro de esos límites, el rate limiter rechaza la petición con una respuesta `503`.

### Configuración del límite de peticiones

Los valores predeterminados son:

```properties
securefindings.rate-limit.max-requests=${SECUREFINDINGS_RATE_LIMIT_MAX_REQUESTS:60}
securefindings.rate-limit.window=${SECUREFINDINGS_RATE_LIMIT_WINDOW:60s}
securefindings.rate-limit.max-tracked-clients=${SECUREFINDINGS_RATE_LIMIT_MAX_TRACKED_CLIENTS:100000}
```

Para modificar el límite localmente:

```properties
SECUREFINDINGS_RATE_LIMIT_MAX_REQUESTS=60
SECUREFINDINGS_RATE_LIMIT_WINDOW=60s
SECUREFINDINGS_RATE_LIMIT_MAX_TRACKED_CLIENTS=100000
```

En producción, los valores deben gestionarse mediante la configuración segura del entorno.

### Configuración de logs

El identificador de correlación se muestra en los logs mediante MDC:

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [requestId=%X{requestId}] %logger{36} - %msg%n
```

No deben registrarse tokens JWT, contraseñas, claves privadas ni datos sensibles.

## Ejecución de la aplicación

Desde PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación estará disponible en:

```text
http://localhost:8080
```

## Ejecución de pruebas

Ejecutar todas las pruebas:

```powershell
.\mvnw.cmd clean test
```

Compilar sin ejecutar pruebas:

```powershell
.\mvnw.cmd -DskipTests compile
```

Ejecutar una clase concreta:

```powershell
.\mvnw.cmd -Dtest=FindingControllerTest test
```

Ejecutar varias clases en PowerShell:

```powershell
.\mvnw.cmd "-Dtest=FindingAuditControllerTest,AuditServiceTest" test
```

Las pruebas cubren:

- Dominio de hallazgos.
- Transiciones válidas e inválidas.
- Servicios de aplicación.
- Controladores REST.
- Validación de peticiones.
- Respuestas `400`, `401`, `403`, `404`, `409` y `429`.
- Seguridad y roles.
- Contexto de organización.
- Persistencia con PostgreSQL.
- Auditoría.
- Comentarios.
- Paginación.
- Búsqueda.
- Filtros.
- Filtros de auditoría por acción.
- Filtros de auditoría por `requestId`.
- Filtros del historial de auditoría por actor.
- Filtros combinados de auditoría por acción, identificador de petición y actor.
- Ordenación.
- Rechazo de parámetros de ordenación no permitidos.
- Limitación por dirección IP.
- Limitación por usuario autenticado.
- Cuotas JWT separadas por organización y por identidad `iss` + `sub`.
- Cuotas separadas para cuentas con el mismo nombre y subjects distintos.
- Cuota conservada tras cambiar `preferred_username` del mismo subject.
- Reutilización de la cuota por nombre principal cuando el claim de organización no es un UUID válido.
- Cabecera `Retry-After`.
- Exclusión del endpoint de health check.
- Generación y validación de `X-Request-ID`.
- Propagación del identificador en respuestas de error.
- Limpieza del contexto MDC.
- Integración del filtro en Spring Security.
- Persistencia y recuperación del `requestId` en auditoría.

## Integración continua

El repositorio incluye:

```text
.github/workflows/ci.yml
.github/workflows/codeql.yml
```

El workflow de integración continua:

- Compila el proyecto.
- Ejecuta las pruebas.
- Revisa dependencias en pull requests.

La revisión de dependencias está definida dentro de `ci.yml`; no existe un workflow independiente llamado `dependency-review.yml`.

CodeQL analiza el código Java para detectar posibles problemas de seguridad y calidad.

## Flujo de trabajo Git

La rama principal de desarrollo es:

```text
develop
```

Actualizarla antes de comenzar:

```powershell
git switch develop
git pull --ff-only origin develop
```

Todo el desarrollo se realiza directamente en `develop`. No se crean ramas adicionales de funcionalidad.

Comprobar los cambios:

```powershell
git status
git diff --check
```

Ejecutar las pruebas antes del commit:

```powershell
.\mvnw.cmd clean test
```

Los commits deben ser pequeños y representar un único cambio coherente.

Ejemplo:

```text
funcionalidad: filtrar el historial de auditoría
```

Las pull requests hacia `main` se reservan para cambios importantes o para agrupar varios commits relacionados.

## Seguridad

Las decisiones de seguridad y el modelo de amenazas se documentan en:

```text
SECURITY.md
```

## Próximos pasos

El proyecto continúa en desarrollo. Algunas líneas futuras son:

- Ampliar las operaciones de administración de organizaciones.
- Añadir más reglas de autorización.
- Configurar paneles y alertas para las métricas Prometheus.
- Mejorar la configuración de producción.
- Automatizar la configuración de Keycloak.
- Ampliar las pruebas de seguridad.
- Añadir despliegue automatizado.