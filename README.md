# SecureFindings API

API REST desarrollada en Java y Spring Boot para registrar, consultar y gestionar hallazgos de seguridad.

El proyecto está orientado a practicar desarrollo backend seguro, control de acceso, persistencia, auditoría, testing, integración continua y aislamiento de datos entre organizaciones.

## Estado

🚧 **En desarrollo**

Actualmente, el proyecto incluye:

- Gestión completa de hallazgos.
- Persistencia en PostgreSQL.
- Migraciones de base de datos con Flyway.
- Autenticación mediante Keycloak y OAuth2/OIDC.
- Autorización basada en roles.
- Aislamiento de datos por organización.
- Historial de auditoría.
- Validación de entradas.
- Manejo controlado de errores.
- Paginación de resultados.
- Filtros por severidad y estado.
- Tests unitarios, web y de integración.
- Integración continua con GitHub Actions.
- Revisión automática de dependencias en pull requests.
- Análisis estático con CodeQL.
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
- Análisis estático del código.

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
- Actualizar título, descripción y severidad.
- Actualizar el estado.
- Eliminar hallazgos.
- Consultar el historial de auditoría.
- Paginar los resultados.
- Filtrar por severidad.
- Filtrar por estado.
- Combinar filtros de severidad y estado.
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

### Paginación y filtros

El listado de hallazgos utiliza paginación para evitar cargar todos los registros en memoria.

Endpoint:

```http
GET /api/v1/findings
```

Parámetros disponibles:

| Parámetro | Obligatorio | Valor predeterminado | Descripción |
|---|---:|---:|---|
| `page` | No | `0` | Número de página. Empieza en cero |
| `size` | No | `20` | Elementos por página. Máximo `100` |
| `severity` | No | — | Filtra por severidad |
| `status` | No | — | Filtra por estado |

Ejemplos:

```http
GET /api/v1/findings
```

```http
GET /api/v1/findings?page=0&size=20
```

```http
GET /api/v1/findings?severity=HIGH
```

```http
GET /api/v1/findings?status=OPEN
```

```http
GET /api/v1/findings?severity=HIGH&status=OPEN
```

Los filtros se aplican mediante consultas parametrizadas de Spring Data JPA. No se construyen consultas SQL concatenando valores recibidos del cliente.

La respuesta contiene la información de paginación:

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

Los resultados se ordenan por:

1. Fecha de creación descendente.
2. Identificador ascendente como orden estable.

Todos los filtros se combinan con el aislamiento por organización. El cliente nunca puede indicar la organización mediante un parámetro de consulta.

### Auditoría

Las operaciones importantes generan eventos de auditoría:

- `CREATED`
- `UPDATED`
- `DELETED`

Cada evento almacena:

- Identificador del evento.
- Identificador del hallazgo.
- Organización.
- Acción realizada.
- Usuario que realizó la acción.
- Fecha y hora UTC.

## Aislamiento entre organizaciones

Cada organización posee un identificador único:

```text
organization_id
```

Este valor se obtiene del token JWT emitido por Keycloak.

La API no acepta la organización desde el cuerpo de la petición, los parámetros de consulta ni las cabeceras controladas por el cliente.

El flujo es:

1. Keycloak emite el token.
2. El token contiene el claim `organization_id`.
3. Spring Security valida el token.
4. `OrganizationContext` obtiene y valida la organización.
5. Los servicios utilizan ese identificador.
6. Los repositorios filtran las consultas por organización.
7. La auditoría también queda asociada a la organización.

Por tanto, un usuario de una organización no puede consultar ni eliminar hallazgos pertenecientes a otra.

Esta garantía se prueba mediante:

```text
FindingOrganizationIsolationIntegrationTest
```

El test utiliza PostgreSQL real mediante Testcontainers y comprueba que:

- La organización A puede crear un hallazgo.
- La organización B no puede recuperarlo.
- La organización B no puede eliminarlo.
- El hallazgo continúa disponible para la organización A.

La segunda organización se crea únicamente en la base de datos temporal del test. No se añade como dato permanente mediante una migración Flyway.

## API REST

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
Authorization: Bearer <token>
```

También permite filtros:

```http
GET /api/v1/findings?severity=HIGH&status=OPEN
Authorization: Bearer <token>
```

### Consultar un hallazgo

```http
GET /api/v1/findings/{id}
Authorization: Bearer <token>
```

### Crear un hallazgo

```http
POST /api/v1/findings
Content-Type: application/json
Authorization: Bearer <token>
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
Authorization: Bearer <token>
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
Authorization: Bearer <token>
```

Cuerpo:

```json
{
  "status": "IN_PROGRESS"
}
```

### Consultar auditoría

```http
GET /api/v1/findings/{id}/audit
Authorization: Bearer <token>
```

### Eliminar un hallazgo

```http
DELETE /api/v1/findings/{id}
Authorization: Bearer <token>
```

Respuesta correcta:

```http
204 No Content
```

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

Con la aplicación arrancada, la especificación OpenAPI está disponible en:

```text
http://localhost:8080/v3/api-docs
```

La interfaz Swagger UI está disponible en:

```text
http://localhost:8080/swagger-ui.html
```

Estos endpoints pueden requerir autenticación según la configuración de Spring Security.

## Persistencia y migraciones

La aplicación utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate no crea ni modifica tablas automáticamente. Solo valida que las entidades Java coincidan con la estructura existente.

Flyway controla la estructura de la base de datos mediante migraciones:

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

PostgreSQL debe aparecer como `healthy`.

Para detener los contenedores conservando los datos:

```powershell
docker compose stop
```

Para detenerlos y eliminarlos conservando los volúmenes:

```powershell
docker compose down
```

Los volúmenes se mantienen mientras no se utilice:

```powershell
docker compose down -v
```

Ese último comando elimina también los datos persistidos de PostgreSQL y Keycloak.

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

Los workflows de GitHub Actions se encuentran en:

```text
.github/workflows/ci.yml
.github/workflows/codeql.yml
```

### CI

El workflow de CI se ejecuta automáticamente en:

- Cada `push`.
- Cada `pull_request`.

El job `build-and-test`:

1. Descarga el código.
2. Configura Java 21.
3. Utiliza Eclipse Temurin.
4. Activa la caché de Maven.
5. Ejecuta `clean verify`.
6. Ejecuta todos los tests.
7. Ejecuta los tests de integración con Testcontainers.

### Dependency Review

El job `dependency-review` se ejecuta únicamente en pull requests.

Su objetivo es detectar dependencias nuevas con vulnerabilidades de severidad moderada o superior antes de fusionar los cambios.

Para que funcione, el repositorio debe tener activado el Dependency Graph de GitHub.

### CodeQL

El workflow de CodeQL analiza el código Java:

- En cada `push`.
- En cada `pull_request`.
- Manualmente mediante `workflow_dispatch`.
- De forma programada.

La compilación se ejecuta antes del análisis para que CodeQL pueda analizar correctamente el código Java compilado.

## Flujo de ramas

El desarrollo activo se realiza en:

```text
develop
```

La rama estable es:

```text
main
```

Los cambios se incorporan a `main` mediante pull requests desde `develop`.

La rama `main` mantiene:

- Pull requests obligatorios.
- Checks de CI obligatorios.
- Revisión de dependencias.
- Análisis CodeQL.
- Protección frente a cambios directos.

## Tests

El proyecto contiene diferentes niveles de prueba.

### Tests de dominio

Comprueban las reglas propias de `Finding`, sus estados, severidades y validaciones.

### Tests de aplicación

Comprueban `FindingService`, incluyendo:

- Creación.
- Consulta.
- Actualización.
- Eliminación.
- Manejo de hallazgos inexistentes.
- Uso del contexto de organización.
- Aplicación del filtro por organización.
- Paginación.
- Filtro por severidad.
- Filtro por estado.
- Combinación de filtros.

### Tests web

Comprueban los controladores REST mediante MockMvc:

- Códigos HTTP.
- Validación de peticiones.
- Respuestas JSON.
- Autorización.
- Manejo de errores.
- Paginación.
- Filtro por severidad.
- Filtro por estado.
- Combinación de filtros.
- Restricción de eliminación para usuarios `ANALYST`.

### Tests de persistencia

Utilizan PostgreSQL real mediante Testcontainers y comprueban:

- Persistencia de hallazgos.
- Recuperación de datos.
- Actualizaciones.
- Eliminaciones.
- Auditoría.
- Restricciones de base de datos.
- Aislamiento por organización.

### Tests de contexto de seguridad

`OrganizationContextTest` comprueba:

- Claim `organization_id` válido.
- Claim ausente.
- Claim con formato inválido.
- Organización inexistente.
- Contextos no autenticados utilizados en pruebas internas.

### Tests de aislamiento

`FindingOrganizationIsolationIntegrationTest` comprueba que los datos de una organización no estén disponibles para otra.

El test ejecuta la aplicación contra PostgreSQL real y verifica el aislamiento tanto para consultas como para eliminaciones.

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