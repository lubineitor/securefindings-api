# SecureFindings API

API REST para registrar y gestionar hallazgos de seguridad.

El proyecto está desarrollado con Java y Spring Boot y aplica conceptos de
Application Security, autenticación, autorización, persistencia, auditoría,
validación y aislamiento de datos por organización.

## Estado

🚧 **En desarrollo**

SecureFindings se desarrolla como proyecto práctico de portfolio para
profundizar en:

- Desarrollo backend con Java.
- Diseño de APIs REST.
- Secure Coding.
- OWASP.
- Autenticación y autorización.
- Persistencia de datos.
- Testing automatizado.
- Auditoría de operaciones.
- Aislamiento multi-organización.

## Funcionalidades actuales

La API permite:

- Crear hallazgos de seguridad.
- Consultar un hallazgo concreto.
- Listar hallazgos.
- Actualizar los datos de un hallazgo.
- Actualizar su estado.
- Eliminar hallazgos.
- Filtrar y paginar resultados.
- Registrar las operaciones realizadas.
- Consultar el historial de auditoría.
- Validar los datos recibidos.
- Gestionar errores de forma controlada.
- Separar los datos por organización.
- Proteger los endpoints mediante roles.

## Tecnologías

- Java 21.
- Spring Boot 4.1.1.
- Spring Web MVC.
- Spring Security.
- OAuth2 Resource Server.
- JWT.
- Spring Data JPA.
- Hibernate.
- PostgreSQL 17.
- Flyway.
- Keycloak 26.
- Maven.
- JUnit 5.
- Mockito.
- MockMvc.
- Testcontainers.
- Docker Compose.
- Springdoc OpenAPI.
- GitHub Actions.

## Arquitectura

El código está organizado mediante módulos dentro del paquete:

```text
com.securefindings
```

Estructura principal:

```text
com.securefindings
├── audit
├── finding
├── organization
└── security
```

Responsabilidades principales:

- `finding`: dominio, servicios, controladores y persistencia de hallazgos.
- `audit`: registro y consulta de acciones realizadas.
- `organization`: organizaciones y asociación de datos.
- `security`: autenticación, autorización y contexto de organización.

El dominio público `Finding` no expone `organizationId`. La organización se
gestiona en la capa de seguridad y persistencia.

## Endpoints

### Health check

```http
GET /api/v1/health
```

Comprueba que la aplicación está disponible. Este endpoint no requiere
autenticación.

### Hallazgos

```http
GET    /api/v1/findings
GET    /api/v1/findings/{id}
POST   /api/v1/findings
PUT    /api/v1/findings/{id}
PATCH  /api/v1/findings/{id}/status
DELETE /api/v1/findings/{id}
```

La consulta de hallazgos admite paginación:

```http
GET /api/v1/findings?page=0&size=20
```

También permite filtrar por severidad y estado:

```http
GET /api/v1/findings?severity=HIGH&status=OPEN
```

### Auditoría

```http
GET /api/v1/findings/{id}/audit
```

Se registran las acciones:

```text
CREATED
UPDATED
DELETED
```

Cada evento conserva el usuario que realizó la operación y la fecha en la que
se produjo.

## Seguridad

La API utiliza Keycloak como proveedor de identidad y Spring Security para
validar los tokens JWT.

Los roles principales son:

| Operación | `ANALYST` | `ADMIN` |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

Las peticiones protegidas deben incluir un token bearer:

```http
Authorization: Bearer <access_token>
```

La configuración detallada se encuentra en:

[`SECURITY.md`](SECURITY.md)

## Aislamiento por organización

Los hallazgos y eventos de auditoría pertenecen a una organización.

La organización se obtiene exclusivamente del claim JWT:

```text
organization_id
```

Actualmente se utiliza la organización de desarrollo:

```text
00000000-0000-0000-0000-000000000001
```

El flujo es:

1. Keycloak autentica al usuario.
2. Keycloak incluye `organization_id` en el access token.
3. Spring Security valida el JWT.
4. `OrganizationContext` obtiene la organización.
5. Los repositorios filtran los datos por esa organización.
6. La auditoría se guarda y consulta con el mismo filtro.

El cliente no puede enviar `organizationId` en el JSON para elegir otra
organización.

En Keycloak, el mapper se encuentra configurado en el cliente:

```text
securefindings-cli
```

Los usuarios actuales son:

```text
analista
administrador
```

## Persistencia

La aplicación utiliza PostgreSQL y Spring Data JPA.

Hibernate está configurado para validar el esquema:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Esto significa que Hibernate no crea ni modifica tablas. La evolución de la
base de datos se realiza mediante Flyway.

## Migraciones Flyway

Las migraciones actuales son:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

Estas migraciones crean:

- La tabla de hallazgos.
- La tabla de auditoría.
- La tabla de organizaciones.
- Las relaciones entre organizaciones, hallazgos y auditoría.
- Los índices necesarios para filtrar por organización.

Las migraciones ya ejecutadas no deben modificarse. Los nuevos cambios deben
realizarse mediante nuevas migraciones versionadas.

## Entorno local

Servicios utilizados:

| Servicio | Dirección |
|---|---|
| API | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |
| Keycloak | `http://localhost:8081` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |

PostgreSQL y Keycloak se ejecutan mediante:

```text
compose.yml
```

### Iniciar la infraestructura

```powershell
docker compose up -d
```

### Comprobar los contenedores

```powershell
docker compose ps
```

### Detener los contenedores conservando los datos

```powershell
docker compose stop
```

### Detener y eliminar los contenedores

```powershell
docker compose down
```

Los datos se conservan en volúmenes Docker nombrados.

## Configuración

La aplicación utiliza variables de entorno para la configuración de la base
de datos y Keycloak.

La plantilla pública es:

```text
.env.example
```

El archivo local `.env` no debe subirse al repositorio porque puede contener
credenciales.

Variables principales:

```text
POSTGRES_HOST
POSTGRES_PORT
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
KEYCLOAK_ISSUER_URI
```

## Ejecución

Iniciar PostgreSQL y Keycloak:

```powershell
docker compose up -d
```

Ejecutar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

## Testing

Ejecutar todos los tests:

```powershell
.\mvnw.cmd clean test
```

La suite incluye:

- Tests del dominio.
- Tests de servicios.
- Tests de controladores.
- Tests de seguridad.
- Tests de auditoría.
- Tests de persistencia.
- Tests de integración con PostgreSQL mediante Testcontainers.

## Estructura del proyecto

```text
.
├── .env.example
├── .gitignore
├── compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── SECURITY.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── securefindings
│   │   │           ├── audit
│   │   │           ├── finding
│   │   │           ├── organization
│   │   │           ├── security
│   │   │           └── SecureFindingsApplication.java
│   │   └── resources
│   │       ├── application.properties
│   │       └── db
│   │           └── migration
│   └── test
│       └── java
│           └── com
│               └── securefindings
```

## Licencia

Este proyecto se desarrolla con fines educativos y de portfolio profesional.