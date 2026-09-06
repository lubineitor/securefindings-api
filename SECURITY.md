# Security

## Alcance

SecureFindings API es un proyecto en desarrollo orientado a practicar seguridad aplicada al backend y al ciclo de desarrollo.

La seguridad se aborda desde varias capas:

- Identidad.
- Autenticación.
- Autorización.
- Aislamiento de datos.
- Validación de entradas.
- Persistencia segura.
- Auditoría.
- Gestión de secretos.
- Seguridad de dependencias.
- Configuración de infraestructura.
- Integración continua.
- Pruebas automatizadas.

## Autenticación

La autenticación se delega en Keycloak mediante OAuth2/OIDC.

La API actúa como Resource Server y valida los tokens JWT emitidos por Keycloak.

Configuración local:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/securefindings
```

El token se envía mediante:

```http
Authorization: Bearer <access_token>
```

La aplicación:

- No recibe contraseñas.
- No almacena contraseñas.
- No gestiona directamente usuarios.
- No utiliza sesiones de usuario.
- No utiliza autenticación basada en cookies.

Las credenciales son gestionadas por Keycloak.

## Autorización

Spring Security utiliza los roles contenidos en el token JWT.

Las reglas actuales son:

| Recurso | ANALYST | ADMIN |
|---|---:|---:|
| `GET /api/v1/health` | Público | Público |
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

El borrado requiere específicamente el rol `ADMIN`.

Si no existe una autenticación válida:

```http
401 Unauthorized
```

Respuesta JSON:

```json
{
  "code": "UNAUTHORIZED",
  "message": "La autenticación es necesaria para acceder a este recurso",
  "errors": {}
}
```

Si el usuario está autenticado pero no tiene permisos:

```http
403 Forbidden
```

Respuesta JSON:

```json
{
  "code": "FORBIDDEN",
  "message": "El usuario no tiene permisos para acceder a este recurso",
  "errors": {}
}
```

La aplicación utiliza sesiones sin estado:

```java
SessionCreationPolicy.STATELESS
```

También desactiva:

- Inicio de sesión basado en formulario.
- Autenticación HTTP Basic.

CSRF está desactivado porque la API utiliza tokens Bearer y no autenticación basada en cookies.

## Roles de Keycloak

El realm utilizado localmente es:

```text
securefindings
```

Los roles principales son:

```text
ANALYST
ADMIN
```

El cliente utilizado para las pruebas locales es:

```text
securefindings-cli
```

`KeycloakRealmRoleConverter` transforma los roles del realm en autoridades reconocidas por Spring Security.

Por ejemplo, el rol:

```text
ANALYST
```

se utiliza en las reglas mediante:

```java
.hasRole("ANALYST")
```

## Claim de organización

Los usuarios deben tener configurado el atributo:

```text
organization_id
```

El cliente de Keycloak utiliza un mapper para incluirlo en el access token:

```json
{
  "preferred_username": "analista",
  "organization_id": "00000000-0000-0000-0000-000000000001"
}
```

El mapper debe:

- Incluir el claim en el access token.
- Utilizar el tipo `String`.
- Evitar exponerlo innecesariamente en otros tokens.

## Aislamiento entre organizaciones

La organización se obtiene exclusivamente del token validado.

La API no acepta el identificador de organización desde:

- Parámetros de consulta.
- Cuerpo JSON.
- Cabeceras controladas por el cliente.
- Identificadores enviados manualmente.

El flujo de validación es:

1. Keycloak autentica al usuario.
2. Keycloak emite un JWT.
3. Spring Security valida la firma, el emisor y la vigencia.
4. `OrganizationContext` obtiene `organization_id`.
5. El valor se convierte en `UUID`.
6. Se comprueba que la organización existe.
7. Los servicios utilizan esa organización.
8. Los repositorios filtran las operaciones.
9. La auditoría se registra dentro de la misma organización.

Si el claim:

- No existe.
- Está vacío.
- No tiene formato UUID.
- Hace referencia a una organización inexistente.

La operación se rechaza mediante `AccessDeniedException`.

## Defensa en profundidad

El aislamiento no depende de una única comprobación.

### Capa de contexto

`OrganizationContext` valida la organización procedente del token.

### Capa de aplicación

`FindingService` y `AuditService` obtienen siempre la organización actual antes de consultar o modificar datos.

Los servicios no reciben la organización desde el cuerpo de la petición ni desde parámetros del cliente.

### Capa de persistencia

Los repositorios incluyen la organización en las consultas:

```java
findByIdAndOrganizationId(...)
existsByIdAndOrganizationId(...)
deleteByIdAndOrganizationId(...)
findPageByFilters(...)
findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(...)
```

### Capa de base de datos

Las tablas `findings` y `finding_audit` contienen:

```text
organization_id
```

Las columnas son obligatorias y tienen claves foráneas hacia `organizations`.

También existen índices para las consultas por organización.

## Paginación y seguridad

Los endpoints paginados aceptan:

```text
page >= 0
1 <= size <= 100
```

El límite máximo de `100` evita solicitar cantidades excesivas de información en una sola petición.

La paginación no modifica el aislamiento organizativo.

En cada página del historial se siguen aplicando simultáneamente estos filtros:

- `finding_id`.
- `organization_id`.

Por tanto, conocer el identificador de un hallazgo de otra organización no permite consultar su historial.

El endpoint es:

```http
GET /api/v1/findings/{id}/audit?page=0&size=20
```

La respuesta contiene:

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

La respuesta no contiene información de eventos pertenecientes a otra organización.

## Búsqueda y filtros

La búsqueda textual utiliza el parámetro:

```text
q
```

Ejemplo:

```http
GET /api/v1/findings?q=sql&page=0&size=20
```

El valor se busca en:

- `title`.
- `description`.

El parámetro tiene una longitud máxima de 100 caracteres.

Las consultas se realizan mediante repositorios Spring Data JPA y parámetros enlazados. No se construyen consultas SQL concatenando directamente valores del usuario.

Esto reduce el riesgo de inyección SQL.

También existen filtros controlados para:

```text
severity
status
```

Los valores se validan contra los enums permitidos por la aplicación.

## Auditoría

Las operaciones de negocio generan eventos en `finding_audit`.

Acciones disponibles:

```text
CREATED
UPDATED
DELETED
```

Cada evento almacena:

- Identificador del evento.
- Identificador del hallazgo.
- Identificador de la organización.
- Acción.
- Usuario que realizó la operación.
- Fecha y hora UTC.

El actor se obtiene preferentemente del claim:

```text
preferred_username
```

Si no está disponible, se utiliza el nombre de la autenticación.

Las operaciones internas o de prueba pueden utilizar:

```text
system
```

La auditoría se registra dentro de la misma transacción de negocio.

El historial se consulta:

- Filtrado por hallazgo.
- Filtrado por organización.
- Ordenado cronológicamente.
- De forma paginada.

## Validación de entradas

Las peticiones REST utilizan Jakarta Validation.

Se validan:

- Campos obligatorios.
- Longitudes máximas.
- Valores permitidos de severidad.
- Valores permitidos de estado.
- Formato de UUID.
- Número de página.
- Tamaño máximo de página.
- Estructura de las peticiones JSON.

La validación se realiza en varias capas:

1. Entrada HTTP.
2. Objetos de aplicación.
3. Objetos de dominio.
4. Persistencia.
5. Restricciones de base de datos.

La API no debe confiar en que el cliente envíe datos correctos.

## Persistencia segura

Hibernate está configurado con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

La aplicación no modifica automáticamente el esquema de producción.

Flyway controla la evolución mediante migraciones versionadas:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

La migración V3 introduce:

- La tabla `organizations`.
- La organización inicial.
- `organization_id` en `findings`.
- `organization_id` en `finding_audit`.
- Claves foráneas.
- Índices de consulta por organización.

## Integración continua

El workflow principal se encuentra en:

```text
.github/workflows/ci.yml
```

Se ejecuta en:

- Cada `push`.
- Cada `pull_request`.

El pipeline:

1. Descarga el código.
2. Configura Java 21 mediante Eclipse Temurin.
3. Utiliza la caché de Maven.
4. Ejecuta `clean verify`.
5. Compila desde cero.
6. Ejecuta todos los tests.
7. Ejecuta los tests de integración con Testcontainers.

El proyecto también utiliza CodeQL para analizar el código Java.

La revisión de dependencias se ejecuta en pull requests y ayuda a detectar vulnerabilidades nuevas en las dependencias modificadas.

Los workflows utilizan permisos mínimos siempre que es posible.

## Gestión de secretos

Los siguientes valores no deben incluirse en Git:

- Contraseñas de PostgreSQL.
- Contraseñas de Keycloak.
- Tokens JWT.
- Claves privadas.
- Credenciales de producción.
- Archivos `.env`.

La configuración local utiliza:

```text
.env
```

El repositorio contiene únicamente:

```text
.env.example
```

Los tokens utilizados durante las pruebas manuales:

- No deben imprimirse completos en la terminal.
- No deben incluirse en capturas.
- No deben subirse al repositorio.
- Deben renovarse cuando caduquen.
- Deben almacenarse únicamente de forma temporal.

## Docker

Docker Compose ejecuta PostgreSQL y Keycloak localmente mediante `compose.yml`.

Los datos se almacenan en volúmenes Docker:

```text
securefindings_postgres_data
securefindings-keycloak-data
```

Detener los servicios sin borrar datos:

```powershell
docker compose stop
```

Eliminar los contenedores conservando los volúmenes:

```powershell
docker compose down
```

Eliminar también los datos persistidos:

```powershell
docker compose down -v
```

El último comando debe utilizarse únicamente cuando se quiera reiniciar completamente el entorno local.

En producción:

- PostgreSQL no debe exponerse directamente a Internet.
- Keycloak no debe ejecutarse con `start-dev`.
- Deben utilizarse redes privadas.
- Debe utilizarse HTTPS.
- Las credenciales deben gestionarse mediante un sistema de secretos.

## Respuestas de error

La API utiliza respuestas controladas para evitar exponer trazas internas.

Ejemplo:

```json
{
  "code": "FINDING_NOT_FOUND",
  "message": "No se ha encontrado el hallazgo",
  "errors": {}
}
```

Códigos habituales:

| Código | Significado |
|---:|---|
| `400` | Petición inválida |
| `401` | Falta autenticación o el token no es válido |
| `403` | El usuario no tiene permisos |
| `404` | El recurso no existe dentro de la organización |
| `409` | Conflicto de datos |
| `500` | Error interno no esperado |

Para evitar filtraciones, un hallazgo perteneciente a otra organización se trata como no encontrado.

## Tests de seguridad

El proyecto incluye pruebas para:

- Validación del contexto de organización.
- Claims ausentes o inválidos.
- Organizaciones inexistentes.
- Autorización por roles.
- Acceso de `ANALYST`.
- Acceso de `ADMIN`.
- Restricción de eliminación.
- Aislamiento entre organizaciones.
- Búsqueda y filtros.
- Paginación.
- Persistencia en PostgreSQL.
- Registro de auditoría.
- Orden cronológico del historial.
- Respuestas HTTP `401` y `403`.

Los tests se ejecutan localmente con:

```powershell
.\mvnw.cmd clean test
```

Y automáticamente en GitHub Actions mediante:

```bash
./mvnw clean verify
```

## Consideraciones para producción

Antes de desplegar el proyecto en producción sería necesario:

- Utilizar HTTPS.
- No utilizar `start-dev` en Keycloak.
- Crear una cuenta administrativa permanente.
- Eliminar usuarios temporales.
- Configurar una base de datos gestionada.
- Utilizar secretos externos.
- Restringir la red de PostgreSQL.
- No publicar PostgreSQL directamente a Internet.
- Configurar logs centralizados.
- Añadir monitorización.
- Configurar límites de peticiones.
- Revisar las políticas CORS.
- Rotar credenciales.
- Validar la configuración de Keycloak.
- Revisar los permisos de los roles.
- Ejecutar análisis de dependencias.
- Incorporar escaneo SAST y DAST.
- Incorporar escaneo de secretos.
- Proteger la rama principal.
- Exigir que los workflows pasen antes de fusionar pull requests.

## Notificación de vulnerabilidades

Las vulnerabilidades deben comunicarse de forma responsable y no publicarse antes de que exista una solución.

En un proyecto real se debería proporcionar:

- Descripción del problema.
- Pasos para reproducirlo.
- Impacto.
- Evidencias mínimas.
- Posible mitigación.
- Versión afectada.