# Seguridad de SecureFindings API

## Objetivo

Este documento describe las medidas de seguridad implementadas en SecureFindings API y las limitaciones actuales del proyecto.

La aplicación se desarrolla aplicando:

- Secure by Design.
- Defense in Depth.
- Least Privilege.
- Validación de entradas.
- Separación de responsabilidades.
- Auditoría.
- Seguridad por defecto.

## Autenticación

La autenticación se delega en Keycloak.

La API funciona como OAuth2 Resource Server y valida tokens JWT mediante:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/securefindings
```

Spring Security valida:

- Firma del token.
- Emisor.
- Caducidad.
- Claims.
- Roles y autoridades.

Las peticiones protegidas deben incluir:

```http
Authorization: Bearer <access_token>
```

Un token ausente o inválido produce:

```text
401 Unauthorized
```

## Autorización

La autorización se basa en los roles del token.

| Operación | ANALYST | ADMIN |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Buscar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Actualizar estados | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

La eliminación se limita a `ADMIN` porque es una operación destructiva.

Un usuario autenticado sin permisos recibe:

```text
403 Forbidden
```

## Aislamiento organizativo

Cada usuario pertenece a una organización mediante el claim:

```text
organization_id
```

Ejemplo:

```json
{
  "preferred_username": "analista",
  "organization_id": "00000000-0000-0000-0000-000000000001"
}
```

La organización se obtiene del contexto de seguridad y no de valores enviados por el cliente.

Las consultas filtran siempre por organización:

```text
organization_id = organización_actual
```

Esto se aplica a:

- Listados.
- Búsquedas.
- Filtros.
- Consultas por identificador.
- Actualizaciones.
- Eliminaciones.
- Auditorías.

Esta protección evita:

- IDOR.
- Broken Object Level Authorization.
- Acceso cruzado entre organizaciones.
- Manipulación de identificadores.

## Búsqueda textual y SQL Injection

La búsqueda se realiza mediante el parámetro:

```http
GET /api/v1/findings?q=SQL
```

El valor se utiliza como parámetro de una consulta JPQL. No se concatena directamente con una consulta SQL.

La búsqueda se aplica sobre:

- Título.
- Descripción.

Además:

- No distingue entre mayúsculas y minúsculas.
- Tiene una longitud máxima de `100` caracteres.
- Se combina con severidad, estado y organización.
- Utiliza paginación.

El aislamiento organizativo se aplica antes de devolver los resultados.

## Validación de entradas

Se validan los cuerpos y parámetros recibidos por la API.

### Parámetros de listado

```text
page >= 0
1 <= size <= 100
q <= 100 caracteres
```

También se validan:

- Severidades.
- Estados.
- Identificadores UUID.
- Campos obligatorios.
- Longitudes máximas.
- Valores no vacíos.

Los errores se devuelven con una estructura común:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene parámetros no válidos",
  "errors": {
    "q": "El valor del parámetro no es válido"
  }
}
```

Esto evita que las excepciones internas lleguen directamente al cliente.

## Manejo de errores

`GlobalExceptionHandler` centraliza las excepciones funcionales y de validación.

Respuestas principales:

| HTTP | Código | Descripción |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Datos o parámetros inválidos |
| `401` | — | Token ausente o inválido |
| `403` | — | Falta de permisos |
| `404` | `FINDING_NOT_FOUND` | Hallazgo inexistente |

No se deben exponer:

- Stack traces.
- Consultas SQL.
- Tokens.
- Contraseñas.
- Rutas internas.
- Configuración sensible.
- Detalles de la infraestructura.

## Persistencia

La aplicación utiliza Spring Data JPA y PostgreSQL.

La búsqueda y los filtros se ejecutan mediante consultas parametrizadas y repositorios tipados.

No se deben concatenar valores recibidos del usuario dentro de consultas SQL.

La estructura de base de datos se controla con Flyway:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate únicamente valida el esquema existente.

## Auditoría

Las operaciones principales generan eventos:

```text
CREATED
UPDATED
DELETED
```

Cada evento registra:

- Hallazgo afectado.
- Organización.
- Acción.
- Actor.
- Fecha y hora.

El actor se obtiene del token autenticado, preferentemente desde:

```text
preferred_username
```

No se confía en un nombre de usuario proporcionado por el cliente.

## Protección contra acceso cruzado

Si un hallazgo pertenece a otra organización, la API responde como si no existiera:

```text
404 Not Found
```

Esto evita revelar información sobre recursos de otras organizaciones.

La aplicación no diferencia públicamente entre:

- Hallazgo inexistente.
- Hallazgo perteneciente a otra organización.

## Gestión de secretos

Las credenciales se cargan mediante variables de entorno:

```properties
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

Las contraseñas reales no deben:

- Subirse a Git.
- Escribirse en `application.properties`.
- Incluirse en `compose.yml`.
- Compartirse en capturas.
- Aparecer en logs.

El archivo `.env` se utiliza localmente y debe permanecer fuera del repositorio.

## Docker y entorno local

Docker Compose se utiliza para PostgreSQL y Keycloak durante el desarrollo.

El entorno local no debe considerarse una configuración de producción.

Para producción serían necesarios:

- TLS.
- Gestión externa de secretos.
- Usuarios administrativos permanentes.
- Keycloak endurecido.
- Restricción de puertos.
- Copias de seguridad.
- Monitorización.
- Rotación de credenciales.
- Protección contra abuso.

## Dependencias y CI

GitHub Actions ejecuta:

- Compilación.
- Tests.
- Dependency Review.
- CodeQL.
- Análisis estático.

Dependency Review permite detectar dependencias nuevas con vulnerabilidades conocidas.

CodeQL ayuda a detectar patrones inseguros en el código.

Los resultados deben revisarse antes de integrar cambios en `main`.

## Riesgos OWASP considerados

### Broken Access Control

Mitigado mediante:

- Roles de Keycloak.
- Reglas de Spring Security.
- Restricción de eliminación a `ADMIN`.
- Validación de la organización actual.

### Broken Object Level Authorization

Mitigado mediante consultas que combinan:

```text
identificador_del_hallazgo + organization_id
```

### Injection

Mitigado mediante:

- Spring Data JPA.
- Consultas parametrizadas.
- Validación de entradas.
- Búsqueda textual sin concatenación SQL.
- Uso de tipos Java para filtros.

### Identification and Authentication Failures

Mitigado mediante:

- Keycloak.
- JWT.
- Validación del issuer.
- Expiración de tokens.
- Roles incluidos en el token.

### Security Logging and Monitoring Failures

Mitigado parcialmente mediante:

- Auditoría.
- Registro del actor.
- Registro de fechas.
- Eventos de creación, actualización y eliminación.

## Limitaciones actuales

El proyecto todavía no incluye:

- Rate limiting.
- Protección avanzada contra abuso.
- Gestión centralizada de secretos.
- Rotación automática de claves.
- Despliegue productivo.
- TLS dentro de la aplicación.
- Monitorización avanzada.
- Alertas de seguridad.
- Escaneo dinámico automatizado.
- Backup automatizado de PostgreSQL.

## Reporte de vulnerabilidades

Las vulnerabilidades deben comunicarse de forma privada al responsable del repositorio.

No se deben publicar:

- Tokens.
- Contraseñas.
- Datos personales.
- Evidencias sensibles.
- Detalles explotables antes de su corrección.

Un reporte debe incluir:

- Descripción.
- Endpoint afectado.
- Pasos para reproducirlo.
- Impacto.
- Evidencias mínimas.
- Propuesta de mitigación.

## Flujo de desarrollo seguro

El trabajo se realiza en:

```text
develop
```

Los cambios terminados se integran mediante Pull Request hacia:

```text
main
```

Antes de integrar cambios se ejecuta:

```powershell
.\mvnw.cmd clean test
```

También se revisan:

- CodeQL.
- Dependency Review.
- Cambios de migraciones.
- Cambios de permisos.
- Documentación.
- Exposición accidental de secretos.