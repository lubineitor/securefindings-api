# SecureFindings API

> API REST para registrar y gestionar hallazgos de seguridad mediante Java y Spring.

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
- Comentarios asociados a hallazgos.
- Aislamiento por organización.
- Validación de datos.
- Manejo controlado de errores.
- Autenticación mediante JWT.
- Autorización basada en roles.
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

- Búsqueda textual en título y descripción.
- Filtro por severidad.
- Filtro por estado.
- Paginación.
- Ordenación ascendente o descendente.

La búsqueda textual utiliza el parámetro `q` y no distingue entre mayúsculas y minúsculas.

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

El usuario se obtiene del claim `preferred_username` del token JWT. En operaciones técnicas o pruebas sin autenticación se utiliza el actor `system`.

La auditoría puede consultarse de forma paginada y se conserva incluso cuando el hallazgo es eliminado.

Las transiciones de estado no permitidas no generan eventos de auditoría.

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

## API REST

### Health check

```text
GET /api/v1/health
```

Este endpoint está disponible sin autenticación.

### Hallazgos

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/findings` | Lista hallazgos paginados | `ANALYST`, `ADMIN` |
| `GET` | `/api/v1/findings/{id}` | Obtiene un hallazgo | `ANALYST`, `ADMIN` |
| `POST` | `/api/v1/findings` | Crea un hallazgo | `ANALYST`, `ADMIN` |
| `PUT` | `/api/v1/findings/{id}` | Actualiza los datos | `ANALYST`, `ADMIN` |
| `PATCH` | `/api/v1/findings/{id}/status` | Actualiza el estado | `ANALYST`, `ADMIN` |
| `DELETE` | `/api/v1/findings/{id}` | Elimina un hallazgo | `ADMIN` |

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

Ejemplo:

```text
GET /api/v1/findings?page=0&size=20&q=SQL&severity=HIGH&status=OPEN&sortBy=title&direction=ASC
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

```text
GET /api/v1/findings/{findingId}/audit
```

Permite consultar el historial paginado de un hallazgo.

Parámetros:

```text
page
size
```

Ejemplo:

```text
GET /api/v1/findings/3bfa1ad2-eee1-4ea5-ba7c-16b47d1da147/audit?page=0&size=20
```

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

Errores principales:

| HTTP | Código | Situación |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Datos o parámetros inválidos |
| `401` | `UNAUTHORIZED` | Token ausente o inválido |
| `403` | `FORBIDDEN` | Usuario sin permisos suficientes |
| `404` | `FINDING_NOT_FOUND` | Hallazgo no disponible para la organización |
| `409` | `INVALID_STATUS_TRANSITION` | Transición de estado no permitida |
| `500` | Error interno | Error no controlado |

Ejemplo de transición inválida:

```json
{
  "code": "INVALID_STATUS_TRANSITION",
  "message": "No se puede cambiar el estado del hallazgo a una transición no permitida",
  "errors": {}
}
```

Los errores de ordenación inválida también se responden con `400`:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "parameter": "El campo de ordenación no está permitido: password"
  }
}
```

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

El token debe incluir, como mínimo:

```text
preferred_username
organization_id
```

Los roles se obtienen desde Keycloak:

- `ANALYST`: puede consultar, crear y modificar hallazgos.
- `ADMIN`: puede realizar todas las operaciones, incluida la eliminación.

La aplicación no utiliza sesiones ni autenticación mediante formulario:

```text
SessionCreationPolicy.STATELESS
```

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
- Acceso a un entorno Keycloak para obtener tokens JWT.

## Configuración local

Crear el archivo de configuración local a partir del ejemplo:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` puede contener credenciales de base de datos y Keycloak. Nunca debe subirse al repositorio.

Iniciar los servicios de infraestructura:

```powershell
docker compose up -d
```

Comprobar los servicios:

```powershell
docker compose ps
```

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

Las pruebas cubren:

- Dominio de hallazgos.
- Transiciones válidas e inválidas.
- Servicios de aplicación.
- Controladores REST.
- Validación de peticiones.
- Respuestas `400`, `401`, `403`, `404` y `409`.
- Seguridad y roles.
- Contexto de organización.
- Persistencia con PostgreSQL.
- Auditoría.
- Comentarios.
- Paginación.
- Búsqueda.
- Filtros.
- Ordenación.
- Rechazo de parámetros de ordenación no permitidos.

## Integración continua

El repositorio incluye los siguientes workflows:

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
funcionalidad: validar transiciones de estado
```

Las pull requests hacia `main` se reservan para cambios importantes o para agrupar varios commits relacionados.

## Seguridad

Las decisiones de seguridad y el modelo de amenazas se documentan en:

```text
SECURITY.md
```

## Próximos pasos

El proyecto continúa en desarrollo. Algunas líneas futuras son:

- Mejorar la administración de organizaciones.
- Añadir más reglas de autorización.
- Incorporar rate limiting.
- Añadir métricas y observabilidad.
- Mejorar la configuración de producción.
- Automatizar la configuración de Keycloak.
- Ampliar las pruebas de seguridad.
- Añadir despliegue automatizado.