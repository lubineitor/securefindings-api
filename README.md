# SecureFindings API

API REST desarrollada en Java para registrar, consultar y gestionar hallazgos de seguridad.

El proyecto se desarrolla aplicando principios de:

- Secure Coding
- OWASP
- Application Security
- Autenticación y autorización
- Persistencia segura
- Multi-tenancy
- Auditoría
- Testing automatizado
- Integración continua

## Estado

🚧 En desarrollo activo.

Actualmente la API dispone de:

- Gestión completa de hallazgos.
- Persistencia en PostgreSQL.
- Migraciones controladas con Flyway.
- Autenticación mediante Keycloak y JWT.
- Autorización basada en roles.
- Aislamiento de datos por organización.
- Registro de auditoría.
- Paginación y filtros.
- Validación de peticiones.
- Manejo centralizado de errores.
- Pruebas unitarias, web e integración.
- Análisis de código mediante CodeQL.
- Revisión de dependencias mediante GitHub Actions.

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- PostgreSQL 17
- Flyway
- Keycloak 26.7.3
- Maven
- JUnit 5
- Mockito
- Spring MockMvc
- Testcontainers
- Docker Compose
- OpenAPI y Swagger UI
- GitHub Actions
- CodeQL
- Dependency Review

## Arquitectura

El código se organiza por funcionalidades y responsabilidades:

```text
src/
├── main/
│   ├── java/
│   │   └── com/securefindings/
│   │       ├── audit/
│   │       ├── finding/
│   │       ├── health/
│   │       ├── security/
│   │       └── api/
│   │           └── error/
│   └── resources/
│       ├── application.properties
│       └── db/
│           └── migration/
└── test/
    └── java/
        └── com/securefindings/
```

Las capas principales son:

- `api`: controladores y objetos de petición/respuesta.
- `application`: casos de uso y servicios.
- `domain`: reglas y modelos del dominio.
- `persistence`: entidades JPA y repositorios.
- `security`: autenticación, autorización y contexto organizativo.
- `audit`: registro de operaciones realizadas.
- `api.error`: tratamiento centralizado de errores HTTP.

## Funcionalidades principales

### Gestión de hallazgos

La API permite:

- Crear hallazgos.
- Consultar un hallazgo concreto.
- Listar hallazgos.
- Actualizar título, descripción y severidad.
- Actualizar el estado.
- Eliminar hallazgos.
- Consultar el historial de auditoría.

Cada hallazgo contiene:

- Identificador único.
- Título.
- Descripción.
- Severidad.
- Estado.
- Organización propietaria.
- Fecha de creación.
- Fecha de actualización.

### Severidades disponibles

```text
LOW
MEDIUM
HIGH
CRITICAL
```

### Estados disponibles

```text
OPEN
IN_PROGRESS
RESOLVED
FALSE_POSITIVE
```

## Paginación y filtros

El listado de hallazgos utiliza paginación:

```http
GET /api/v1/findings?page=0&size=20
```

Parámetros disponibles:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `page` | No | Número de página. Empieza en `0`. |
| `size` | No | Número de elementos. Entre `1` y `100`. |
| `severity` | No | Filtra por severidad. |
| `status` | No | Filtra por estado. |

Ejemplo:

```http
GET /api/v1/findings?page=0&size=10&severity=HIGH&status=OPEN
```

Respuesta:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

La consulta se ejecuta siempre dentro de la organización asociada al token JWT.

## Endpoints

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/v1/health` | Estado de la aplicación | Público |
| `GET` | `/api/v1/findings` | Listar hallazgos | `ANALYST`, `ADMIN` |
| `GET` | `/api/v1/findings/{id}` | Obtener un hallazgo | `ANALYST`, `ADMIN` |
| `POST` | `/api/v1/findings` | Crear un hallazgo | `ANALYST`, `ADMIN` |
| `PUT` | `/api/v1/findings/{id}` | Actualizar un hallazgo | `ANALYST`, `ADMIN` |
| `PATCH` | `/api/v1/findings/{id}/status` | Actualizar el estado | `ANALYST`, `ADMIN` |
| `DELETE` | `/api/v1/findings/{id}` | Eliminar un hallazgo | `ADMIN` |
| `GET` | `/api/v1/findings/{id}/audit` | Consultar auditoría | `ANALYST`, `ADMIN` |

## Manejo de errores

Los errores funcionales y de validación utilizan una estructura común:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene parámetros no válidos",
  "errors": {
    "page": "El valor no es válido"
  }
}
```

Errores principales:

| HTTP | Código | Situación |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Petición o parámetros incorrectos |
| `401` | — | Token ausente, inválido o expirado |
| `403` | — | El usuario no tiene permisos |
| `404` | `FINDING_NOT_FOUND` | Hallazgo inexistente |

Se validan:

- Cuerpo de las peticiones.
- Parámetros de paginación.
- Valores de severidad.
- Valores de estado.
- Identificadores UUID.
- Reglas propias del dominio.

Las excepciones de seguridad son gestionadas por Spring Security y las excepciones funcionales por `GlobalExceptionHandler`.

## Persistencia y migraciones

La aplicación utiliza PostgreSQL y Flyway.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Migraciones actuales:

- `V1`: creación de la tabla `findings`.
- `V2`: creación de la tabla de auditoría.
- `V3`: creación de organizaciones y asignación de hallazgos.

Hibernate utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Esto significa que Hibernate valida el esquema existente, pero no modifica la estructura de la base de datos. Los cambios estructurales deben realizarse mediante nuevas migraciones Flyway.

## Docker Compose

El archivo utilizado es:

```text
compose.yml
```

Iniciar PostgreSQL y Keycloak:

```powershell
docker compose up -d
```

Comprobar el estado:

```powershell
docker compose ps
```

Detener los contenedores conservando los datos:

```powershell
docker compose stop
```

Detener y eliminar los contenedores, conservando los volúmenes:

```powershell
docker compose down
```

Los datos se almacenan en volúmenes Docker:

- `securefindings_postgres_data`
- `securefindings-keycloak-data`

No se deben incluir contraseñas reales en el repositorio. La configuración local se carga mediante `.env`, mientras que `.env.example` sirve como plantilla.

## Keycloak

La autenticación se realiza mediante tokens JWT emitidos por Keycloak.

Configuración principal:

```text
Realm: securefindings
Issuer: http://localhost:8081/realms/securefindings
```

El token contiene información como:

```json
{
  "preferred_username": "analista",
  "organization_id": "00000000-0000-0000-0000-000000000001"
}
```

Los roles principales son:

```text
ANALYST
ADMIN
```

El claim `organization_id` se utiliza para aplicar aislamiento entre organizaciones.

Un usuario de una organización no puede:

- Listar hallazgos de otra organización.
- Consultar hallazgos de otra organización.
- Modificar hallazgos de otra organización.
- Eliminar hallazgos de otra organización.
- Consultar auditorías de otra organización.

## Auditoría

Las operaciones relevantes generan eventos de auditoría:

```text
CREATED
UPDATED
DELETED
```

Cada evento registra:

- Identificador del hallazgo.
- Organización.
- Acción.
- Usuario que realizó la operación.
- Fecha y hora.

La auditoría se conserva incluso después de eliminar el hallazgo, cuando la relación de base de datos lo permite.

## OpenAPI

La documentación de la API está disponible cuando la aplicación está iniciada:

```text
http://localhost:8080/swagger-ui/index.html
```

Especificación OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

## Ejecución local

### 1. Iniciar la infraestructura

```powershell
docker compose up -d
```

### 2. Comprobar PostgreSQL y Keycloak

```powershell
docker compose ps
```

### 3. Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

### 4. Compilar

```powershell
.\mvnw.cmd clean package
```

### 5. Ejecutar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

## Testing

El proyecto contiene:

- Tests unitarios del dominio.
- Tests unitarios de servicios.
- Tests de controladores con MockMvc.
- Tests de seguridad.
- Tests de persistencia con Testcontainers.
- Tests de aislamiento organizativo.
- Tests de auditoría.
- Tests de validación de parámetros.
- Tests de errores HTTP.

Comando principal:

```powershell
.\mvnw.cmd clean test
```

## Integración continua

GitHub Actions ejecuta automáticamente:

- Compilación.
- Tests.
- Revisión de dependencias.
- CodeQL.
- Análisis de código Java.

El flujo de trabajo utilizado es:

```text
develop
   │
   └── Pull Request
           │
           ▼
          main
```

La rama `develop` se utiliza para el desarrollo. La rama `main` contiene únicamente cambios revisados y terminados.

## Objetivo de seguridad

El proyecto se desarrolla siguiendo un enfoque Secure by Design.

Se presta especial atención a:

- Broken Access Control.
- IDOR y aislamiento organizativo.
- Validación de entradas.
- Inyección SQL.
- Gestión de secretos.
- Seguridad de JWT.
- Privilegios mínimos.
- Auditoría.
- Dependencias vulnerables.
- Trazabilidad de cambios.