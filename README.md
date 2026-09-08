# SecureFindings API

API REST desarrollada en Java y Spring Boot para registrar, consultar y gestionar hallazgos de seguridad.

El proyecto está orientado a practicar desarrollo backend seguro, control de acceso, persistencia, auditoría, comentarios, testing, integración continua y aislamiento de datos entre organizaciones.

## Estado

🚧 **En desarrollo**

Actualmente, el proyecto incluye:

- Gestión completa de hallazgos.
- Comentarios asociados a hallazgos.
- Persistencia en PostgreSQL.
- Migraciones de base de datos con Flyway.
- Autenticación mediante Keycloak y OAuth2/OIDC.
- Autorización basada en roles.
- Aislamiento de datos por organización.
- Historial de auditoría.
- Validación de entradas.
- Manejo controlado de errores.
- Paginación y filtrado de resultados.
- Tests unitarios, web y de integración.
- Integración continua con GitHub Actions.
- Análisis de código con CodeQL.
- Revisión automática de dependencias.
- Entorno local reproducible con Docker Compose.
- Documentación OpenAPI mediante Swagger.

## Objetivo

SecureFindings proporciona una API para registrar y gestionar hallazgos de seguridad durante procesos de análisis o revisión de aplicaciones.

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
- Auditoría de acciones relevantes.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal |
| Spring Boot 4.1.1 | Framework de aplicación |
| Spring MVC | API REST |
| Spring Data JPA | Persistencia |
| Hibernate | ORM |
| PostgreSQL 17 | Base de datos |
| Flyway | Migraciones versionadas |
| Keycloak 26.7.3 | Identidad y autorización |
| Spring Security | Autenticación y autorización |
| Jakarta Validation | Validación de entradas |
| JUnit 5 | Tests |
| Mockito | Tests unitarios |
| MockMvc | Tests web |
| Testcontainers | Tests con PostgreSQL real |
| Maven Wrapper | Compilación y ejecución |
| Docker Compose | Infraestructura local |
| GitHub Actions | Integración continua |
| CodeQL | Análisis estático de seguridad |
| Springdoc OpenAPI | Documentación de la API |

## Funcionalidades

### Gestión de hallazgos

La API permite:

- Crear hallazgos.
- Consultar los hallazgos visibles para la organización actual.
- Consultar un hallazgo por identificador.
- Buscar por título y descripción.
- Filtrar por severidad.
- Filtrar por estado.
- Combinar filtros.
- Actualizar título, descripción y severidad.
- Actualizar el estado.
- Eliminar hallazgos.
- Paginar los resultados.
- Validar las peticiones recibidas.

### Comentarios

Los comentarios permiten añadir contexto y seguimiento a un hallazgo.

Cada comentario:

- Pertenece a un hallazgo concreto.
- Pertenece a la misma organización que el hallazgo.
- Tiene un autor obtenido de la identidad autenticada.
- Tiene un contenido obligatorio.
- Tiene una longitud máxima de 5000 caracteres.
- Se almacena con fecha y hora UTC.
- Se consulta de forma paginada.
- Genera un evento de auditoría `COMMENTED`.

La organización y el autor no se aceptan desde el cuerpo de la petición. Ambos se determinan a partir del contexto autenticado y del hallazgo existente.

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

Las operaciones relevantes generan eventos de auditoría:

- `CREATED`
- `UPDATED`
- `DELETED`
- `COMMENTED`

Cada evento almacena:

- Identificador del evento.
- Identificador del hallazgo.
- Organización.
- Acción realizada.
- Usuario que realizó la acción.
- Fecha y hora UTC.

La auditoría también se consulta de forma paginada y aislada por organización.

## Aislamiento entre organizaciones

Cada organización posee un identificador único:

```text
organization_id
```

Este valor se obtiene del token JWT emitido por Keycloak.

La API no acepta la organización desde el cuerpo de la petición, parámetros de consulta o cabeceras controladas por el cliente.

El flujo es:

1. Keycloak emite el token.
2. El token contiene el claim `organization_id`.
3. Spring Security valida el token.
4. `OrganizationContext` obtiene y valida la organización.
5. Los servicios utilizan esa organización.
6. Los repositorios filtran las consultas.
7. Los hallazgos, comentarios y eventos de auditoría quedan asociados a la organización.

Por tanto, un usuario de una organización no puede consultar, modificar, comentar ni eliminar recursos pertenecientes a otra organización.

Esta garantía se prueba mediante tests de integración con PostgreSQL y Testcontainers.

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

También admite filtros:

```http
GET /api/v1/findings?page=0&size=20&q=inyeccion&severity=HIGH&status=OPEN
```

Parámetros:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `page` | No | Página, comenzando en `0` |
| `size` | No | Elementos por página, entre `1` y `100` |
| `q` | No | Busca en título y descripción |
| `severity` | No | `LOW`, `MEDIUM`, `HIGH` o `CRITICAL` |
| `status` | No | Estado del hallazgo |

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

Los resultados se limitan siempre a la organización contenida en el token.

### Consultar un hallazgo

```http
GET /api/v1/findings/{id}
```

Un hallazgo de otra organización se trata como inexistente y devuelve `404 Not Found`.

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

La operación genera un evento de auditoría `CREATED`.

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

La operación genera un evento de auditoría `UPDATED`.

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

La operación genera un evento de auditoría `UPDATED`.

### Crear un comentario

```http
POST /api/v1/findings/{id}/comments
Content-Type: application/json
Authorization: Bearer <access_token>
```

Cuerpo:

```json
{
  "content": "Se ha validado la entrada y se ha aplicado parametrización."
}
```

La organización y el autor se obtienen del contexto autenticado.

La operación genera un evento de auditoría `COMMENTED`.

### Consultar comentarios

```http
GET /api/v1/findings/{id}/comments?page=0&size=20
Authorization: Bearer <access_token>
```

La respuesta es paginada y ordenada por fecha de creación ascendente.

### Consultar auditoría

```http
GET /api/v1/findings/{id}/audit?page=0&size=20
Authorization: Bearer <access_token>
```

La respuesta contiene eventos como:

```json
{
  "content": [
    {
      "id": "6c4d7f76-3ad9-4a19-8d7f-8987d25ed2d8",
      "findingId": "d534aae0-9eb0-4794-a854-de55f3712625",
      "action": "COMMENTED",
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

### Eliminar un hallazgo

```http
DELETE /api/v1/findings/{id}
Authorization: Bearer <access_token>
```

Respuesta correcta:

```http
204 No Content
```

La eliminación requiere el rol `ADMIN` y genera un evento `DELETED`.

## Roles

| Operación | ANALYST | ADMIN |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Crear comentarios | Sí | Sí |
| Consultar comentarios | Sí | Sí |
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

Los controladores incluyen descripciones de operaciones, parámetros y respuestas HTTP.

## Persistencia y migraciones

La aplicación utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate no crea ni modifica tablas automáticamente. Solo valida que las entidades Java coincidan con la estructura existente.

Flyway controla la evolución de la base de datos mediante migraciones:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
V4__crear_comentarios_de_hallazgos.sql
V5__permitir_auditoria_de_comentarios.sql
```

La migración V3 introduce el aislamiento por organización.

La migración V4 crea `finding_comments` con:

- Identificador del comentario.
- Identificador del hallazgo.
- Identificador de la organización.
- Autor.
- Contenido.
- Fecha de creación.
- Claves foráneas.
- Restricción de contenido no vacío.
- Índice para consultar comentarios por hallazgo y organización.

La migración V5 amplía las acciones de auditoría para permitir `COMMENTED`.

## Configuración local

Crea el archivo `.env` a partir del ejemplo:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` contiene valores locales y no debe subirse al repositorio.

La aplicación utiliza variables de entorno para:

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

Para detener los contenedores conservando los datos:

```powershell
docker compose stop
```

Para detenerlos y eliminarlos conservando los volúmenes:

```powershell
docker compose down
```

Para eliminar también los datos persistidos:

```powershell
docker compose down -v
```

Este último comando elimina los datos locales de PostgreSQL y Keycloak.

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

Los workflows se encuentran en:

```text
.github/workflows/
```

La integración continua se ejecuta en cada `push` y `pull_request`.

Las comprobaciones principales son:

- Compilación con Java 21.
- Ejecución de tests.
- Tests de integración con Testcontainers.
- Revisión de dependencias en pull requests.
- Análisis de código con CodeQL.
- Análisis específico de Java mediante CodeQL.

Los jobs utilizan permisos mínimos y no persisten credenciales de Git innecesariamente.

## Tests

El proyecto contiene diferentes niveles de prueba.

### Tests de dominio

Comprueban las reglas de:

- Hallazgos.
- Estados.
- Severidades.
- Comentarios.
- Longitudes máximas.
- Campos obligatorios.

### Tests de aplicación

Comprueban:

- Creación y consulta de hallazgos.
- Actualización y eliminación.
- Búsqueda y filtrado.
- Aislamiento organizativo.
- Creación de comentarios.
- Consulta paginada de comentarios.
- Registro de auditoría.
- Manejo de recursos inexistentes.

### Tests web

Comprueban mediante MockMvc:

- Códigos HTTP.
- Validación de peticiones.
- Respuestas JSON.
- Autenticación.
- Autorización.
- Restricción de eliminación para usuarios `ANALYST`.
- Endpoints de comentarios.
- Paginación y parámetros inválidos.

### Tests de persistencia

Utilizan PostgreSQL real mediante Testcontainers y comprueban:

- Persistencia de hallazgos.
- Persistencia de comentarios.
- Recuperación de datos.
- Actualizaciones.
- Eliminaciones.
- Auditoría.
- Migraciones Flyway.
- Restricciones de base de datos.

### Tests de seguridad

Comprueban:

- Claim `organization_id`.
- Claims ausentes o inválidos.
- Organizaciones inexistentes.
- Autorización por roles.
- Respuestas JSON `401` y `403`.
- Restricción de eliminación.
- Aislamiento entre organizaciones.

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
│   │           ├── audit
│   │           ├── comment
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
                ├── comment
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