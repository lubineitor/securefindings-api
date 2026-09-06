# SecureFindings API

API REST desarrollada en Java para registrar, consultar y gestionar hallazgos de seguridad.

El proyecto aplica progresivamente principios de:

- Secure Coding.
- OWASP.
- Application Security.
- Autenticación y autorización.
- Persistencia segura.
- Aislamiento organizativo.
- Auditoría.
- Testing automatizado.
- Integración continua.

## Estado

🚧 En desarrollo activo.

Actualmente incluye:

- Gestión completa de hallazgos.
- Persistencia en PostgreSQL.
- Migraciones con Flyway.
- Autenticación mediante Keycloak y JWT.
- Autorización basada en roles.
- Aislamiento de datos por organización.
- Auditoría de operaciones.
- Paginación.
- Filtros por severidad y estado.
- Búsqueda textual por título y descripción.
- Validación de peticiones y parámetros.
- Manejo centralizado de errores.
- Tests unitarios, web e integración.
- CodeQL.
- Dependency Review.
- GitHub Actions.

## Tecnologías

- Java 21.
- Spring Boot 4.1.1.
- Spring Web.
- Spring Data JPA.
- Spring Security.
- OAuth2 Resource Server.
- PostgreSQL 17.
- Flyway.
- Keycloak 26.7.3.
- Maven.
- JUnit 5.
- Mockito.
- MockMvc.
- Testcontainers.
- Docker Compose.
- OpenAPI y Swagger UI.
- GitHub Actions.
- CodeQL.

## Arquitectura

```text
src/
├── main/
│   ├── java/
│   │   └── com/securefindings/
│   │       ├── api/
│   │       │   └── error/
│   │       ├── audit/
│   │       ├── finding/
│   │       ├── health/
│   │       └── security/
│   └── resources/
│       ├── application.properties
│       └── db/
│           └── migration/
└── test/
    └── java/
        └── com/securefindings/
```

Las responsabilidades principales son:

- `api`: controladores y objetos de petición/respuesta.
- `application`: servicios y casos de uso.
- `domain`: reglas y modelos del dominio.
- `persistence`: entidades JPA y repositorios.
- `security`: autenticación, autorización y contexto organizativo.
- `audit`: registro de operaciones.
- `api.error`: tratamiento global de errores.

## Funcionalidades

### Gestión de hallazgos

La API permite:

- Crear hallazgos.
- Consultar hallazgos.
- Actualizar información.
- Actualizar estados.
- Eliminar hallazgos.
- Buscar por texto.
- Filtrar por severidad.
- Filtrar por estado.
- Consultar el historial de auditoría.

Cada hallazgo contiene:

- Identificador único.
- Título.
- Descripción.
- Severidad.
- Estado.
- Organización.
- Fecha de creación.
- Fecha de actualización.

### Severidades

```text
LOW
MEDIUM
HIGH
CRITICAL
```

### Estados

```text
OPEN
IN_PROGRESS
RESOLVED
FALSE_POSITIVE
```

## Listado, paginación y búsqueda

El listado utiliza paginación:

```http
GET /api/v1/findings?page=0&size=20
```

Parámetros disponibles:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `page` | No | Número de página. Empieza en `0`. |
| `size` | No | Elementos por página. Entre `1` y `100`. |
| `q` | No | Texto buscado en título y descripción. Máximo `100` caracteres. |
| `severity` | No | Filtra por severidad. |
| `status` | No | Filtra por estado. |

Ejemplo de búsqueda:

```http
GET /api/v1/findings?q=SQL
```

La búsqueda no distingue entre mayúsculas y minúsculas y se aplica sobre:

- `title`.
- `description`.

Los filtros pueden combinarse:

```http
GET /api/v1/findings?page=0&size=10&q=SQL&severity=HIGH&status=OPEN
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

Todas las consultas se ejecutan dentro de la organización asociada al token JWT.

## Endpoints

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/v1/health` | Estado de la aplicación | Público |
| `GET` | `/api/v1/findings` | Listar, filtrar y buscar hallazgos | `ANALYST`, `ADMIN` |
| `GET` | `/api/v1/findings/{id}` | Obtener un hallazgo | `ANALYST`, `ADMIN` |
| `POST` | `/api/v1/findings` | Crear un hallazgo | `ANALYST`, `ADMIN` |
| `PUT` | `/api/v1/findings/{id}` | Actualizar un hallazgo | `ANALYST`, `ADMIN` |
| `PATCH` | `/api/v1/findings/{id}/status` | Actualizar el estado | `ANALYST`, `ADMIN` |
| `DELETE` | `/api/v1/findings/{id}` | Eliminar un hallazgo | `ADMIN` |
| `GET` | `/api/v1/findings/{id}/audit` | Consultar auditoría | `ANALYST`, `ADMIN` |

## Manejo de errores

Los errores de validación utilizan una estructura común:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene parámetros no válidos",
  "errors": {
    "page": "El valor del parámetro no es válido"
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

- Cuerpos JSON.
- Paginación.
- Longitud máxima de `q`.
- Severidades.
- Estados.
- Identificadores UUID.
- Reglas del dominio.

## Persistencia y migraciones

La aplicación utiliza PostgreSQL y Flyway.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Migraciones actuales:

- `V1`: creación de `findings`.
- `V2`: creación de la auditoría.
- `V3`: creación de organizaciones y asignación de hallazgos.

Hibernate utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate únicamente valida el esquema. Los cambios estructurales se realizan mediante migraciones Flyway.

## Docker Compose

El archivo utilizado es:

```text
compose.yml
```

Iniciar PostgreSQL y Keycloak:

```powershell
docker compose up -d
```

Comprobar los contenedores:

```powershell
docker compose ps
```

Detener los contenedores conservando los datos:

```powershell
docker compose stop
```

Detener y eliminar los contenedores conservando los volúmenes:

```powershell
docker compose down
```

Los datos se almacenan en volúmenes Docker:

- `securefindings_postgres_data`.
- `securefindings-keycloak-data`.

Las contraseñas reales se cargan mediante variables de entorno y no deben subirse al repositorio.

## Keycloak

La autenticación utiliza tokens JWT emitidos por Keycloak.

Configuración principal:

```text
Realm: securefindings
Issuer: http://localhost:8081/realms/securefindings
```

Ejemplo de token:

```json
{
  "preferred_username": "analista",
  "organization_id": "00000000-0000-0000-0000-000000000001"
}
```

Roles principales:

```text
ANALYST
ADMIN
```

El claim `organization_id` se utiliza para aislar los datos entre organizaciones.

## Auditoría

Las siguientes operaciones generan eventos:

```text
CREATED
UPDATED
DELETED
```

Cada evento registra:

- Hallazgo afectado.
- Organización.
- Acción.
- Usuario.
- Fecha y hora.

La auditoría permite conocer quién realizó cada operación y cuándo se produjo.

## OpenAPI

Swagger UI:

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

### 2. Comprobar el estado

```powershell
docker compose ps
```

### 3. Ejecutar los tests

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
- Tests de paginación.
- Tests de filtros.
- Tests de búsqueda textual.
- Tests de validación.
- Tests de auditoría.
- Tests de aislamiento organizativo.
- Tests de persistencia con Testcontainers.

Comando principal:

```powershell
.\mvnw.cmd clean test
```

## Integración continua

GitHub Actions ejecuta:

- Compilación.
- Tests.
- Dependency Review.
- CodeQL.
- Análisis del código Java.

El flujo de trabajo es:

```text
develop
   │
   └── Pull Request
           │
           ▼
          main
```

La rama `develop` se utiliza para el desarrollo. La rama `main` contiene cambios terminados y revisados.

## Objetivo de seguridad

El proyecto sigue un enfoque Secure by Design.

Se presta especial atención a:

- Broken Access Control.
- IDOR.
- Aislamiento organizativo.
- Inyección SQL.
- Validación de entradas.
- Gestión de secretos.
- Seguridad de JWT.
- Privilegios mínimos.
- Auditoría.
- Dependencias vulnerables.