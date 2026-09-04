# SecureFindings API

API REST para registrar y gestionar hallazgos de seguridad.

El proyecto está desarrollado con Java y Spring Boot, aplicando principios de desarrollo seguro, control de acceso, persistencia, auditoría y aislamiento de datos por organización.

## Estado

🚧 **En desarrollo**

SecureFindings se desarrolla como proyecto práctico para profundizar en:

- Desarrollo backend con Java.
- Application Security.
- OWASP.
- Secure Coding.
- Autenticación y autorización.
- Persistencia de datos.
- Testing automatizado.
- Diseño de APIs REST.

## Funcionalidades actuales

- Creación, consulta, actualización y eliminación de hallazgos.
- Clasificación por severidad.
- Gestión del estado del hallazgo.
- Validación de las peticiones.
- Manejo controlado de errores.
- Persistencia en PostgreSQL.
- Migraciones de base de datos con Flyway.
- Autenticación mediante tokens JWT de Keycloak.
- Autorización mediante roles `ANALYST` y `ADMIN`.
- Registro de auditoría de las operaciones.
- Consulta del historial de cada hallazgo.
- Paginación y filtros.
- Aislamiento de datos por organización.
- Documentación OpenAPI y Swagger UI.
- Tests unitarios, web y de integración.

## Tecnologías

- Java 21.
- Spring Boot 4.1.1.
- Spring Web MVC.
- Spring Security.
- OAuth2 Resource Server.
- Spring Data JPA.
- PostgreSQL 17.
- Flyway.
- Keycloak 26.
- Maven.
- JUnit 5.
- Mockito.
- Testcontainers.
- Docker Compose.
- Springdoc OpenAPI.
- GitHub Actions.

## Arquitectura

El código utiliza una estructura modular dentro del paquete neutral:

```text
com.securefindings
├── audit
├── finding
├── organization
└── security
```

La aplicación separa las principales responsabilidades:

- `finding`: dominio y gestión de hallazgos.
- `audit`: registro y consulta de operaciones.
- `organization`: representación y persistencia de organizaciones.
- `security`: autenticación, autorización y contexto de organización.

## API disponible

### Health check

```http
GET /api/v1/health
```

Endpoint público para comprobar que la aplicación está funcionando.

### Hallazgos

```http
GET    /api/v1/findings
GET    /api/v1/findings/{id}
POST   /api/v1/findings
PUT    /api/v1/findings/{id}
PATCH  /api/v1/findings/{id}/status
DELETE /api/v1/findings/{id}
```

La consulta paginada permite utilizar parámetros como:

```http
GET /api/v1/findings?page=0&size=20
```

También se pueden aplicar filtros por severidad y estado:

```http
GET /api/v1/findings?severity=HIGH&status=OPEN
```

### Auditoría

```http
GET /api/v1/findings/{id}/audit
```

El historial registra las siguientes acciones:

- `CREATED`
- `UPDATED`
- `DELETED`

Cada evento conserva:

- Identificador del hallazgo.
- Acción realizada.
- Usuario que realizó la operación.
- Fecha y hora de la operación.
- Organización propietaria, almacenada internamente.

## Seguridad

La API utiliza Keycloak como proveedor de identidad y Spring Security como servidor de recursos OAuth2.

Los tokens JWT contienen la identidad del usuario y sus roles.

### Roles

| Operación | `ANALYST` | `ADMIN` |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

El endpoint de salud no requiere autenticación.

La configuración detallada de seguridad se encuentra en:

[`SECURITY.md`](SECURITY.md)

## Aislamiento por organización

Los datos se separan mediante el identificador interno `organization_id`.

La organización no se recibe desde el cuerpo JSON de las peticiones. Se obtiene exclusivamente del claim firmado del token JWT:

```text
organization_id
```

El flujo es:

1. Keycloak emite el token con `organization_id`.
2. `OrganizationContext` obtiene y valida el claim.
3. Los repositorios filtran los hallazgos por organización.
4. Los eventos de auditoría se guardan y consultan por organización.
5. Si la organización no existe o el claim no es válido, la petición se rechaza.

La organización de desarrollo utilizada actualmente es:

```text
00000000-0000-0000-0000-000000000001
```

El dominio público `Finding` no expone `organizationId`, y los clientes no pueden elegir libremente la organización enviándola en una petición.

El backend ya contiene la lógica para validar este claim. La configuración del mapper correspondiente en Keycloak forma parte de la configuración de los usuarios y tokens del entorno local.

## Persistencia y migraciones

La aplicación utiliza PostgreSQL como base de datos y Flyway para controlar la evolución del esquema.

Las migraciones actuales son:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

Estas migraciones crean:

- La tabla `findings`.
- La tabla `finding_audit`.
- La tabla `organizations`.
- La relación entre hallazgos, auditoría y organizaciones.
- Los índices necesarios para consultar los datos por organización.

Hibernate utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate valida el esquema, pero las modificaciones de la base de datos son responsabilidad de Flyway.

## Entorno local

La aplicación utiliza los siguientes servicios:

| Servicio | Dirección |
|---|---|
| API Spring Boot | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |
| Keycloak | `http://localhost:8081` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

PostgreSQL y Keycloak se ejecutan mediante el archivo:

```text
compose.yml
```

### Iniciar los servicios

Desde la raíz del proyecto:

```powershell
docker compose up -d
```

Comprobar el estado:

```powershell
docker compose ps
```

Detener los servicios conservando los datos:

```powershell
docker compose stop
```

Detener y eliminar los contenedores manteniendo los volúmenes:

```powershell
docker compose down
```

Los datos de PostgreSQL y Keycloak se almacenan en volúmenes Docker nombrados.

## Configuración

Las credenciales y los valores específicos del entorno local se definen mediante variables de entorno.

La plantilla disponible es:

```text
.env.example
```

La aplicación utiliza, entre otras, las siguientes variables:

```text
POSTGRES_HOST
POSTGRES_PORT
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
KEYCLOAK_ISSUER_URI
```

## Ejecución

Iniciar la infraestructura:

```powershell
docker compose up -d
```

Ejecutar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación arrancará en el puerto `8080`.

## Tests

Ejecutar todos los tests:

```powershell
.\mvnw.cmd clean test
```

La suite incluye:

- Tests del dominio.
- Tests de servicios.
- Tests de controladores con MockMvc.
- Tests de seguridad.
- Tests de persistencia con PostgreSQL mediante Testcontainers.
- Tests de auditoría.

## Estructura principal

```text
src
├── main
│   ├── java
│   │   └── com
│   │       └── securefindings
│   │           ├── audit
│   │           ├── finding
│   │           ├── organization
│   │           ├── security
│   │           └── SecureFindingsApplication.java
│   └── resources
│       ├── application.properties
│       └── db
│           └── migration
└── test
    └── java
        └── com
            └── securefindings
```

## Licencia

Este proyecto se desarrolla con fines educativos y de portfolio profesional.