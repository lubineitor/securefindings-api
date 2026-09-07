# SecureFindings API

API REST desarrollada en Java y Spring Boot para registrar, consultar y gestionar hallazgos de seguridad.

El proyecto está orientado a practicar desarrollo backend seguro, control de acceso, persistencia, auditoría, testing, integración continua y aislamiento de datos entre organizaciones.

## Estado

🚧 **En desarrollo**

Actualmente, el proyecto incluye:

- Gestión de hallazgos de seguridad.
- Persistencia en PostgreSQL.
- Migraciones de base de datos con Flyway.
- Autenticación mediante Keycloak y OAuth2/OIDC.
- Autorización basada en roles.
- Aislamiento de datos por organización.
- Historial de auditoría.
- Paginación de hallazgos y eventos de auditoría.
- Búsqueda textual y filtros.
- Validación de entradas.
- Manejo controlado de errores.
- Tests unitarios, web y de integración.
- Integración continua con GitHub Actions.
- Análisis de código con CodeQL.
- Revisión de dependencias en pull requests.
- Entorno local reproducible con Docker Compose.

## Objetivo

SecureFindings pretende proporcionar una API para registrar y gestionar hallazgos de seguridad durante procesos de análisis o revisión de aplicaciones.

El proyecto aplica progresivamente los siguientes principios:

- Secure by Design.
- Defensa en profundidad.
- Principio de mínimo privilegio.
- Separación entre dominio, aplicación, persistencia y API.
- Validación de entradas.
- Control de acceso.
- Trazabilidad de operaciones.
- Aislamiento multi-organización.
- Automatización de pruebas.
- Integración continua.
- Revisión de dependencias.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal |
| Spring Boot 4.1.1 | Framework de aplicación |
| Spring MVC | Desarrollo de la API REST |
| Spring Data JPA | Persistencia |
| Hibernate | ORM |
| PostgreSQL 17 | Base de datos |
| Flyway | Migraciones versionadas |
| Keycloak 26.7.3 | Identidad y autorización |
| Spring Security | Autenticación y autorización |
| JUnit 5 | Tests |
| Mockito | Tests unitarios |
| MockMvc | Tests web |
| Testcontainers | Tests con PostgreSQL real |
| Maven Wrapper | Compilación y ejecución |
| Docker Compose | Infraestructura local |
| GitHub Actions | Integración continua |
| Springdoc OpenAPI | Documentación de la API |
| CodeQL | Análisis estático de seguridad |

## Funcionalidades

### Gestión de hallazgos

La API permite:

- Crear hallazgos.
- Consultar los hallazgos de la organización actual.
- Consultar un hallazgo por identificador.
- Actualizar título, descripción y severidad.
- Actualizar el estado.
- Eliminar hallazgos.
- Consultar el historial de auditoría.
- Buscar por título o descripción.
- Filtrar por severidad.
- Filtrar por estado.
- Paginar los resultados.
- Validar las peticiones recibidas.

### Severidades disponibles

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

### Estados disponibles

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `FALSE_POSITIVE`

### Auditoría

Las operaciones importantes generan eventos de auditoría:

- `CREATED`
- `UPDATED`
- `DELETED`

Cada evento almacena:

- Identificador del evento.
- Identificador del hallazgo.
- Identificador de la organización.
- Acción realizada.
- Usuario que realizó la operación.
- Fecha y hora UTC.

El historial se consulta de forma paginada y ordenada cronológicamente.

## Aislamiento entre organizaciones

Cada organización posee un identificador único:

```text
organization_id
```

Este valor se obtiene exclusivamente del token JWT emitido por Keycloak.

La API no acepta la organización desde:

- Parámetros de consulta.
- Cuerpo JSON.
- Cabeceras controladas por el cliente.
- Identificadores enviados manualmente por el usuario.

El flujo es:

1. Keycloak autentica al usuario.
2. Keycloak emite un token JWT.
3. Spring Security valida el token.
4. `OrganizationContext` obtiene el claim `organization_id`.
5. Se convierte el valor a `UUID`.
6. Se comprueba que la organización existe.
7. Los servicios utilizan esa organización.
8. Los repositorios filtran las consultas.
9. La auditoría queda asociada a la misma organización.

Por tanto, un usuario de una organización no puede consultar ni modificar hallazgos pertenecientes a otra.

## API REST

Todas las operaciones protegidas requieren:

```http
Authorization: Bearer <access_token>
```

### Estado de la aplicación

```http
GET /api/v1/health
```

Este endpoint no requiere autenticación.

Respuesta:

```json
{
  "status": "UP",
  "timestamp": "2026-09-03T09:00:00Z"
}
```

### Listar hallazgos

```http
GET /api/v1/findings?page=0&size=20
```

Parámetros disponibles:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `page` | No | Número de página. Empieza en `0`. |
| `size` | No | Elementos por página. Entre `1` y `100`. |
| `q` | No | Busca en título y descripción. Máximo 100 caracteres. |
| `severity` | No | `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`. |
| `status` | No | `OPEN`, `IN_PROGRESS`, `RESOLVED` o `FALSE_POSITIVE`. |

Ejemplo:

```http
GET /api/v1/findings?page=0&size=20&q=sql&severity=HIGH&status=OPEN
```

Respuesta:

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

Los resultados están limitados a la organización del token.

### Consultar un hallazgo

```http
GET /api/v1/findings/{id}
```

### Crear un hallazgo

```http
POST /api/v1/findings
Content-Type: application/json
Authorization: Bearer <access_token>
```

Cuerpo:

```json
{
  "title": "SQL Injection",
  "description": "Entrada de usuario sin validar",
  "severity": "HIGH"
}
```

### Actualizar los datos de un hallazgo

```http
PUT /api/v1/findings/{id}
Content-Type: application/json
Authorization: Bearer <access_token>
```

Cuerpo:

```json
{
  "title": "SQL Injection corregido",
  "description": "La entrada se valida y parametriza correctamente",
  "severity": "MEDIUM"
}
```

### Actualizar el estado

```http
PATCH /api/v1/findings/{id}/status
Content-Type: application/json
Authorization: Bearer <access_token>
```

Cuerpo:

```json
{
  "status": "IN_PROGRESS"
}
```

### Consultar el historial de auditoría

```http
GET /api/v1/findings/{id}/audit?page=0&size=20
Authorization: Bearer <access_token>
```

Los parámetros son:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `page` | No | Número de página. Empieza en `0`. |
| `size` | No | Eventos por página. Entre `1` y `100`. |

Respuesta:

```json
{
  "content": [
    {
      "id": "6c4d7f76-3ad9-4a19-8d7f-8987d25ed2d8",
      "findingId": "d534aae0-9eb0-4794-a854-de55f3712625",
      "action": "CREATED",
      "actor": "analista",
      "occurredAt": "2026-09-03T09:16:21.148144Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Los eventos se ordenan cronológicamente por `occurredAt`.

La paginación no permite acceder a eventos de otra organización. Cada consulta filtra por:

- Identificador del hallazgo.
- Identificador de la organización obtenida del token.

### Eliminar un hallazgo

```http
DELETE /api/v1/findings/{id}
Authorization: Bearer <access_token>
```

Respuesta correcta:

```http
204 No Content
```

Solo un usuario con rol `ADMIN` puede eliminar hallazgos.

## Roles

| Operación | ANALYST | ADMIN |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

Los roles proceden de Keycloak y se convierten en autoridades de Spring Security.

## Documentación OpenAPI

Con la aplicación arrancada:

```text
http://localhost:8080/v3/api-docs
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Swagger permite introducir un token JWT mediante el botón `Authorize`.

Debe pegarse únicamente el token, sin añadir manualmente el prefijo `Bearer`, ya que Swagger lo incorpora automáticamente.

## Persistencia y migraciones

La aplicación utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate no crea ni modifica tablas automáticamente. Solo valida que las entidades coincidan con la estructura existente.

Flyway controla la estructura mediante migraciones versionadas:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

La migración V3:

- Crea la tabla `organizations`.
- Crea la organización inicial.
- Añade `organization_id` a `findings`.
- Añade `organization_id` a `finding_audit`.
- Crea las claves foráneas.
- Crea índices para las consultas por organización.

## Configuración local

Crea el archivo `.env` a partir del ejemplo:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` contiene valores locales y no debe subirse al repositorio.

La configuración utiliza variables de entorno para:

- Host de PostgreSQL.
- Puerto de PostgreSQL.
- Nombre de la base de datos.
- Usuario de PostgreSQL.
- Contraseña de PostgreSQL.
- Usuario administrador de Keycloak.
- Contraseña del administrador de Keycloak.

## Ejecutar la infraestructura

Desde la raíz del proyecto:

```powershell
docker compose up -d
```

Comprobar el estado:

```powershell
docker compose ps
```

PostgreSQL debe aparecer como `healthy`.

Detener los contenedores conservando los datos:

```powershell
docker compose stop
```

Detener y eliminar los contenedores conservando los volúmenes:

```powershell
docker compose down
```

Eliminar también los datos persistidos:

```powershell
docker compose down -v
```

El último comando elimina los datos de PostgreSQL y Keycloak.

## Ejecutar la aplicación

Compilar:

```powershell
.\mvnw.cmd compile
```

Ejecutar los tests:

```powershell
.\mvnw.cmd test
```

Ejecutar una compilación limpia:

```powershell
.\mvnw.cmd clean test
```

Arrancar Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

Keycloak estará disponible en:

```text
http://localhost:8081
```

## Integración continua

El workflow principal está en:

```text
.github/workflows/ci.yml
```

Se ejecuta automáticamente en:

- Cada `push`.
- Cada `pull_request`.

El pipeline:

1. Descarga el código.
2. Configura Java 21.
3. Utiliza Eclipse Temurin.
4. Activa la caché de Maven.
5. Ejecuta `clean verify`.
6. Ejecuta todos los tests.
7. Ejecuta los tests de integración con Testcontainers.

La revisión de dependencias se ejecuta en pull requests.

El proyecto también utiliza CodeQL para analizar el código Java y detectar posibles problemas de seguridad.

## Tests

El proyecto contiene diferentes niveles de prueba.

### Tests de dominio

Comprueban las reglas de `Finding`, sus estados, severidades y validaciones.

### Tests de aplicación

Comprueban `FindingService`, incluyendo:

- Creación.
- Consulta.
- Actualización.
- Eliminación.
- Manejo de hallazgos inexistentes.
- Uso del contexto de organización.
- Aplicación de filtros.
- Búsqueda textual.
- Paginación.

### Tests web

Comprueban los controladores REST mediante MockMvc:

- Códigos HTTP.
- Validación de peticiones.
- Respuestas JSON.
- Paginación.
- Filtros.
- Autorización.
- Manejo de errores.
- Restricción de eliminación para usuarios `ANALYST`.
- Respuestas JSON para errores `401` y `403`.

### Tests de auditoría

Comprueban:

- Registro de eventos `CREATED`.
- Registro de eventos `UPDATED`.
- Registro de eventos `DELETED`.
- Actor procedente de `preferred_username`.
- Filtrado por organización.
- Orden cronológico.
- Paginación del historial.
- Respuesta vacía cuando no existen eventos.

### Tests de persistencia

Utilizan PostgreSQL real mediante Testcontainers y comprueban:

- Persistencia de hallazgos.
- Recuperación de datos.
- Actualizaciones.
- Eliminaciones.
- Persistencia de auditoría.
- Restricciones de base de datos.
- Aislamiento organizativo.

### Tests de seguridad

`OrganizationContextTest` comprueba:

- Claim `organization_id` válido.
- Claim ausente.
- Claim con formato inválido.
- Organización inexistente.
- Contextos no autenticados.

`SecurityConfigTest` comprueba:

- Acceso sin autenticación.
- Respuesta JSON `401`.
- Acceso de usuarios `ANALYST`.
- Restricción de eliminación.
- Respuesta JSON `403` para usuarios sin permisos.

## Estructura principal

```text
.github
└── workflows
    ├── ci.yml
    └── codeql.yml

src
├── main
│   ├── java
│   │   └── com
│   │       └── securefindings
│   │           ├── SecureFindingsApplication.java
│   │           ├── api
│   │           ├── audit
│   │           ├── finding
│   │           ├── health
│   │           ├── organization
│   │           └── security
│   └── resources
│       ├── application.properties
│       └── db
│           └── migration
└── test
    └── java
        └── com
            └── securefindings
                ├── audit
                ├── finding
                ├── health
                └── security
```

## Objetivos de seguridad

El proyecto trabaja progresivamente riesgos relacionados con:

- Broken Access Control.
- Fallos de autenticación.
- Validación insuficiente.
- Inyección SQL.
- Exposición de información.
- Gestión incorrecta de secretos.
- Falta de trazabilidad.
- Acceso entre organizaciones.
- Configuración insegura de infraestructura.
- Dependencias vulnerables.
- Fallos introducidos durante cambios de código.

## Licencia

Proyecto personal en desarrollo con finalidad educativa y de portfolio.