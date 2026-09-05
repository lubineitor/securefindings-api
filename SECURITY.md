# Security

## Alcance

SecureFindings API es un proyecto en desarrollo orientado a practicar seguridad aplicada al backend.

La seguridad se aborda desde varias capas:

- Identidad.
- Autenticación.
- Autorización.
- Aislamiento de datos.
- Validación de entradas.
- Persistencia segura.
- Auditoría.
- Gestión de secretos.
- Configuración de infraestructura.
- Pruebas automatizadas.

## Autenticación

La autenticación se delega en Keycloak mediante OAuth2/OIDC.

La API actúa como Resource Server y valida los tokens JWT emitidos por Keycloak.

Configuración local:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/securefindings
```

El token debe enviarse mediante la cabecera:

```http
Authorization: Bearer <access_token>
```

La aplicación no recibe ni almacena contraseñas de usuarios. Las credenciales son gestionadas por Keycloak.

## Autorización

Spring Security utiliza los roles obtenidos del token.

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

Si un usuario autenticado no tiene permisos suficientes, la API responde:

```http
403 Forbidden
```

Si no se presenta un token válido, responde:

```http
401 Unauthorized
```

La aplicación utiliza sesiones sin estado:

```java
SessionCreationPolicy.STATELESS
```

También desactiva el inicio de sesión basado en formulario y la autenticación HTTP Basic, porque la API utiliza tokens Bearer.

CSRF está desactivado porque la API no utiliza autenticación basada en cookies y funciona como Resource Server stateless.

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

El conversor `KeycloakRealmRoleConverter` transforma los roles del realm en autoridades reconocidas por Spring Security.

Por ello, una autoridad como:

```text
ANALYST
```

se utiliza en las reglas de autorización mediante:

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

El mapper debe estar configurado para:

- Incluir el claim en el access token.
- Utilizar el tipo `String`.
- No exponerlo innecesariamente en otros tokens.

## Aislamiento entre organizaciones

Cada organización posee un identificador único:

```text
organization_id
```

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
7. Los servicios utilizan ese identificador.
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

`FindingService` obtiene siempre la organización actual antes de consultar o modificar datos.

No recibe la organización desde el cuerpo de la petición.

### Capa de persistencia

Los repositorios utilizan métodos que incluyen el identificador de organización:

```java
findByIdAndOrganizationId(...)
existsByIdAndOrganizationId(...)
deleteByIdAndOrganizationId(...)
findAllByOrganizationId(...)
```

### Capa de base de datos

La tabla `findings` contiene:

```text
organization_id
```

La columna es obligatoria y tiene una clave foránea hacia `organizations`.

La tabla `finding_audit` también contiene:

```text
organization_id
```

Este diseño evita que un hallazgo o un evento de auditoría pueda existir sin una organización válida.

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
- `FindingService`.
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

La organización secundaria se inserta únicamente en la base de datos temporal de Testcontainers. No se añade a los entornos reales mediante una migración de producción.

Esta prueba valida el comportamiento completo de:

```text
JWT → OrganizationContext → FindingService → FindingRepository → PostgreSQL
```

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

Las operaciones internas o de prueba pueden utilizar el actor:

```text
system
```

El historial de auditoría también se consulta de forma aislada por organización.

Un usuario no puede consultar el historial de un hallazgo perteneciente a otra organización.

## Validación de entradas

Las peticiones REST utilizan validación mediante Jakarta Validation.

Se validan:

- Campos obligatorios.
- Longitudes máximas.
- Valores permitidos de severidad.
- Valores permitidos de estado.
- Formato de identificadores UUID.
- Estructura de las peticiones JSON.

La API no debe confiar en que el cliente envíe datos correctos.

La validación se realiza:

1. En la entrada HTTP.
2. En los objetos de dominio.
3. En las restricciones de base de datos.

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
- Las claves foráneas.
- Los índices de consulta por organización.

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

El repositorio contiene únicamente valores de ejemplo:

```text
.env.example
```

Antes de publicar la aplicación deben sustituirse las credenciales de desarrollo por secretos gestionados por la plataforma de despliegue.

Los tokens utilizados durante las pruebas manuales no deben imprimirse completos en la terminal ni incluirse en capturas.

## Docker

Docker Compose se utiliza para ejecutar PostgreSQL y Keycloak localmente.

Los datos se almacenan en volúmenes Docker:

```text
securefindings_postgres_data
securefindings-keycloak-data
```

Detener los servicios sin borrar datos:

```powershell
docker compose stop
```

Eliminar contenedores conservando volúmenes:

```powershell
docker compose down
```

Eliminar también los datos persistidos:

```powershell
docker compose down -v
```

El último comando debe utilizarse únicamente cuando se quiera reiniciar completamente el entorno local.

En producción, PostgreSQL y Keycloak no deben exponerse directamente a Internet sin controles adicionales de red, autenticación y cifrado.

## Respuestas de error

La API utiliza respuestas controladas para evitar exponer trazas internas.

Ejemplo de hallazgo inexistente:

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
- Persistencia en PostgreSQL.
- Registro de auditoría.
- Validación de errores HTTP.

Los tests se ejecutan con:

```powershell
.\mvnw.cmd test
```

## Consideraciones para producción

Antes de desplegar el proyecto en producción sería necesario:

- Utilizar HTTPS.
- No usar `start-dev` en Keycloak.
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
- Incorporar escaneo SAST y DAST al pipeline.
- Revisar periódicamente las migraciones y restricciones de base de datos.

## Notificación de vulnerabilidades

Las vulnerabilidades deben comunicarse de forma responsable y no publicarse antes de que exista una solución.

En un proyecto real se debería proporcionar:

- Descripción del problema.
- Pasos para reproducirlo.
- Impacto.
- Evidencias mínimas.
- Posible mitigación.
- Versión afectada.