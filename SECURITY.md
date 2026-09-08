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
- No almacena credenciales de usuarios.
- No gestiona sesiones de usuario.
- No utiliza autenticación mediante formulario.
- No utiliza autenticación HTTP Basic.

Las credenciales son gestionadas por Keycloak.

La aplicación utiliza sesiones sin estado:

```java
SessionCreationPolicy.STATELESS
```

CSRF está desactivado porque la API no utiliza autenticación basada en cookies y funciona como Resource Server stateless.

## Autorización

Spring Security utiliza los roles incluidos en el token JWT.

Las reglas principales son:

| Recurso | ANALYST | ADMIN |
|---|---:|---:|
| `GET /api/v1/health` | Público | Público |
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Crear comentarios | Sí | Sí |
| Consultar comentarios | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

El borrado de hallazgos requiere específicamente el rol `ADMIN`.

Si no existe un token válido:

```http
401 Unauthorized
```

Si el usuario está autenticado pero no tiene permisos suficientes:

```http
403 Forbidden
```

Las respuestas de seguridad se devuelven en formato JSON controlado.

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

Por ejemplo:

```java
.hasRole("ANALYST")
```

requiere que el usuario tenga el rol `ANALYST`.

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
- No permitir que el usuario lo modifique desde la petición.
- No exponerlo innecesariamente en otros tokens.

## Aislamiento entre organizaciones

La API no acepta el identificador de organización desde:

- Parámetros de consulta.
- Cuerpo JSON.
- Cabeceras controladas por el cliente.
- Identificadores enviados manualmente por el usuario.

El valor se obtiene exclusivamente del token validado.

### Flujo de validación

1. Keycloak autentica al usuario.
2. Keycloak emite un JWT.
3. Spring Security valida la firma, el emisor y la vigencia.
4. `OrganizationContext` obtiene `organization_id`.
5. El valor se convierte en `UUID`.
6. Se comprueba que la organización existe.
7. Los servicios utilizan esa organización.
8. Los repositorios filtran las operaciones.
9. Los eventos de auditoría quedan asociados a ella.

Si el claim:

- No existe.
- Está vacío.
- No tiene formato UUID.
- Hace referencia a una organización inexistente.

La operación se rechaza mediante `AccessDeniedException`.

## Defensa en profundidad

El aislamiento no depende de una única comprobación.

### Capa de contexto

`OrganizationContext` valida la organización procedente del token y evita confiar en valores proporcionados por el cliente.

### Capa de aplicación

Los servicios obtienen siempre la organización actual antes de consultar o modificar datos.

No reciben la organización desde:

- El cuerpo de la petición.
- El path.
- Los parámetros de consulta.
- Las cabeceras del cliente.

### Capa de persistencia

Los repositorios utilizan métodos que incluyen el identificador de organización:

```java
findByIdAndOrganizationId(...)
existsByIdAndOrganizationId(...)
deleteByIdAndOrganizationId(...)
findByOrganizationId(...)
findByFindingIdAndOrganizationId(...)
findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(...)
findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(..., Pageable)
findByFindingIdAndOrganizationIdOrderByCreatedAtAscIdAsc(...)
```

Esto evita recuperar o modificar registros pertenecientes a otra organización.

### Capa de base de datos

Las tablas principales contienen `organization_id`:

- `findings`
- `finding_audit`
- `finding_comments`

Las columnas son obligatorias y tienen claves foráneas hacia `organizations`.

Los comentarios también tienen una relación obligatoria con el hallazgo mediante `finding_id`.

## Comentarios de hallazgos

Los comentarios se crean mediante:

```http
POST /api/v1/findings/{id}/comments
```

El cuerpo solo contiene el contenido:

```json
{
  "content": "Se ha validado la entrada."
}
```

La API no acepta desde el cliente:

- El autor.
- La organización.
- La fecha de creación.
- El identificador del comentario.

El autor se obtiene preferentemente del claim:

```text
preferred_username
```

Si no está disponible, se utiliza el nombre de la autenticación o el actor técnico `system`.

El contenido:

- Es obligatorio.
- No puede estar vacío.
- Tiene una longitud máxima de 5000 caracteres.
- Se valida en la capa HTTP.
- Se valida también en el dominio.
- Está protegido por una restricción de base de datos.

La consulta de comentarios es paginada:

```http
GET /api/v1/findings/{id}/comments?page=0&size=20
```

Solo devuelve comentarios del hallazgo y de la organización actual.

## Auditoría

Las operaciones de negocio generan eventos en `finding_audit`.

Acciones disponibles:

```text
CREATED
UPDATED
DELETED
COMMENTED
```

Cada evento almacena:

- Identificador del evento.
- Identificador del hallazgo.
- Identificador de la organización.
- Acción.
- Actor.
- Fecha y hora UTC.

El evento `COMMENTED` se registra cuando se crea un comentario correctamente.

El registro de auditoría se realiza después de guardar el comentario dentro de la misma operación transaccional.

El actor se obtiene preferentemente de:

```text
preferred_username
```

Las operaciones internas o de prueba pueden utilizar:

```text
system
```

El historial se consulta de forma paginada y aislada por organización.

Un usuario no puede consultar el historial de un hallazgo perteneciente a otra organización.

## Prueba de aislamiento

La prueba:

```text
FindingOrganizationIsolationIntegrationTest
```

utiliza:

- Spring Boot.
- PostgreSQL real mediante Testcontainers.
- Dos organizaciones.
- Contextos JWT simulados.
- Servicios de aplicación.
- Repositorios JPA.
- Flyway.

El flujo probado es:

1. Autenticar una organización A.
2. Crear un hallazgo.
3. Cambiar a una organización B.
4. Intentar recuperar el hallazgo.
5. Verificar que el resultado es vacío.
6. Intentar eliminarlo.
7. Verificar que se lanza `FindingNotFoundException`.
8. Volver a la organización A.
9. Verificar que el hallazgo sigue existiendo.

El mismo principio se aplica a:

- Comentarios.
- Auditoría.
- Consultas paginadas.
- Operaciones de actualización.
- Operaciones de eliminación.

## Validación de entradas

Las peticiones REST utilizan Jakarta Validation.

Se validan:

- Campos obligatorios.
- Longitudes máximas.
- Valores permitidos de severidad.
- Valores permitidos de estado.
- Contenido de comentarios.
- Tamaño de página.
- Número de página.
- Formato de identificadores UUID.
- Estructura de las peticiones JSON.

La API devuelve errores controlados en formato JSON.

Ejemplo:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "content": "El contenido no puede estar vacío"
  }
}
```

La validación se realiza en:

1. Entrada HTTP.
2. Objetos de dominio.
3. Servicios de aplicación.
4. Restricciones de base de datos.

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
V4__crear_comentarios_de_hallazgos.sql
V5__permitir_auditoria_de_comentarios.sql
```

La migración V4 crea la tabla de comentarios con:

- Clave primaria UUID.
- Clave foránea hacia `findings`.
- Clave foránea hacia `organizations`.
- Contenido obligatorio.
- Restricción de contenido no vacío.
- Índice por hallazgo, organización y fecha.

La migración V5 actualiza la restricción de acciones de auditoría para aceptar `COMMENTED`.

## Inyección SQL

La aplicación utiliza Spring Data JPA y métodos derivados de repositorio.

No se construyen consultas SQL concatenando directamente valores procedentes del usuario.

Los filtros y búsquedas se reciben como parámetros controlados por Spring Data y Hibernate.

La validación de entradas y el aislamiento organizativo se mantienen aunque el usuario manipule los parámetros de consulta.

## Gestión de secretos

Nunca deben incluirse en Git:

- Contraseñas de PostgreSQL.
- Contraseñas de Keycloak.
- Tokens JWT.
- Claves privadas.
- Credenciales de producción.
- Archivos `.env`.
- Secretos utilizados en pruebas manuales.

La configuración local utiliza:

```text
.env
```

El repositorio debe contener únicamente:

```text
.env.example
```

Los tokens utilizados manualmente no deben imprimirse completos en la terminal ni incluirse en capturas.

## Integración continua

Los workflows están en:

```text
.github/workflows/
```

La integración continua se ejecuta en:

- `push`.
- `pull_request`.

Las comprobaciones incluyen:

- Compilación con Java 21.
- Tests unitarios.
- Tests web.
- Tests de integración con Testcontainers.
- Revisión de dependencias.
- Análisis CodeQL.

Los workflows deben utilizar permisos mínimos:

```yaml
permissions:
  contents: read
```

También se debe evitar la persistencia innecesaria de credenciales:

```yaml
persist-credentials: false
```

### Revisión de dependencias

La revisión de dependencias se ejecuta en pull requests.

Busca:

- Dependencias vulnerables nuevas.
- Cambios con severidad moderada o superior.
- Problemas de licencias según la configuración de GitHub.

No sustituye a:

- Actualizaciones periódicas.
- Revisión de avisos de seguridad.
- SAST.
- DAST.
- Escaneo de secretos.
- Revisión manual.

### CodeQL

CodeQL analiza el código Java para detectar patrones potencialmente inseguros.

El análisis se ejecuta en:

- Pull requests.
- Pushes a la rama principal.
- Ejecuciones programadas.

Los resultados deben revisarse antes de fusionar cambios relevantes.

## Docker

Docker Compose ejecuta PostgreSQL y Keycloak localmente.

Los datos se almacenan en volúmenes Docker:

```text
securefindings_postgres_data
securefindings-keycloak-data
```

Detener los servicios sin borrar datos:

```powershell
docker compose stop
```

Eliminar los contenedores conservando volúmenes:

```powershell
docker compose down
```

Eliminar también los datos persistidos:

```powershell
docker compose down -v
```

Este último comando debe utilizarse únicamente cuando se quiera reiniciar completamente el entorno local.

En producción:

- PostgreSQL no debe exponerse directamente a Internet.
- Keycloak no debe ejecutarse con `start-dev`.
- Debe utilizarse HTTPS.
- Deben aplicarse controles de red.
- Las credenciales deben gestionarse mediante un sistema seguro de secretos.

## Respuestas de error

La API utiliza respuestas controladas para evitar exponer trazas internas.

Ejemplo de recurso inexistente:

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
| 400 | Petición inválida |
| 401 | Falta autenticación o el token no es válido |
| 403 | El usuario no tiene permisos |
| 404 | El recurso no existe dentro de la organización |
| 409 | Conflicto de datos |
| 500 | Error interno no esperado |

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
- Creación y consulta de comentarios.
- Auditoría de comentarios.
- Persistencia en PostgreSQL.
- Validación de errores HTTP.
- Paginación y límites de página.

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
- No usar `start-dev` en Keycloak.
- Crear cuentas administrativas controladas.
- Eliminar usuarios temporales.
- Utilizar una base de datos gestionada.
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
- Incorporar SAST y DAST.
- Incorporar escaneo de secretos.
- Proteger la rama principal.
- Exigir que los workflows pasen antes de fusionar pull requests.
- Revisar periódicamente las migraciones y restricciones de base de datos.

## Notificación de vulnerabilidades

Las vulnerabilidades deben comunicarse de forma responsable y no publicarse antes de que exista una solución.

La comunicación debería incluir:

- Descripción del problema.
- Pasos para reproducirlo.
- Impacto.
- Evidencias mínimas.
- Posible mitigación.
- Versión afectada.