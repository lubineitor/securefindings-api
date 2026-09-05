# Seguridad de SecureFindings API

## Objetivo

Este documento describe las medidas de seguridad implementadas en SecureFindings API, las decisiones adoptadas y las limitaciones conocidas del entorno actual.

El proyecto se desarrolla aplicando principios de:

- Secure by Design.
- Defense in Depth.
- Least Privilege.
- Validación de entradas.
- Separación de responsabilidades.
- Auditoría.
- Seguridad por defecto.

## Alcance

La aplicación protege una API REST para gestionar hallazgos de seguridad.

Los recursos principales son:

- Hallazgos.
- Organizaciones.
- Eventos de auditoría.
- Usuarios y roles de Keycloak.

## Autenticación

La autenticación se delega en Keycloak.

La API funciona como OAuth2 Resource Server y valida tokens JWT mediante el issuer configurado:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/securefindings
```

La aplicación no gestiona directamente:

- Contraseñas.
- Sesiones de usuario.
- Recuperación de contraseñas.
- Emisión de tokens.

Spring Security valida:

- Firma del token.
- Emisor.
- Caducidad.
- Claims.
- Autoridades y roles.

Las peticiones protegidas deben incluir:

```http
Authorization: Bearer <access_token>
```

Un token ausente o inválido produce:

```text
401 Unauthorized
```

## Autorización

La autorización utiliza roles de Keycloak.

Matriz actual:

| Operación | ANALYST | ADMIN |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Actualizar estados | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Eliminar hallazgos | No | Sí |

Un usuario autenticado sin el rol necesario recibe:

```text
403 Forbidden
```

La eliminación se reserva a `ADMIN` porque es una operación destructiva.

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

La aplicación obtiene la organización desde el contexto de seguridad y no desde datos enviados por el cliente.

Esto evita que un usuario pueda intentar seleccionar manualmente otra organización mediante:

- Parámetros de consulta.
- Campos JSON.
- Cabeceras personalizadas.
- Identificadores manipulados.

Las consultas de hallazgos incluyen siempre el identificador de la organización actual.

Ejemplo conceptual:

```text
findByIdAndOrganizationId(id, organizationId)
```

No se considera suficiente comprobar únicamente el identificador del hallazgo.

Esta protección evita vulnerabilidades de:

- IDOR.
- Broken Object Level Authorization.
- Acceso cruzado entre tenants.

## Validación de entradas

La API valida las peticiones en varios niveles.

### Validación de cuerpos

Las peticiones de creación y actualización utilizan DTOs con Bean Validation.

Se validan:

- Campos obligatorios.
- Longitudes máximas.
- Valores no vacíos.
- Enumeraciones.
- Formato de los datos.

### Validación de parámetros

Los parámetros del listado tienen límites definidos:

```text
page >= 0
1 <= size <= 100
```

También se validan los valores de:

- `severity`.
- `status`.

Los errores de validación se transforman en una respuesta consistente:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene parámetros no válidos",
  "errors": {
    "page": "El valor del parámetro no es válido"
  }
}
```

Las validaciones de cuerpo utilizan la misma estructura:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "title": "El título no puede estar vacío"
  }
}
```

El objetivo es:

- Evitar datos inválidos.
- Reducir errores inesperados.
- Evitar que detalles internos lleguen al cliente.
- Mantener un contrato HTTP estable.

## Manejo de errores

`GlobalExceptionHandler` centraliza las excepciones funcionales y de validación.

Respuestas principales:

| HTTP | Código | Descripción |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Datos o parámetros inválidos |
| `401` | — | Token ausente o inválido |
| `403` | — | Falta de permisos |
| `404` | `FINDING_NOT_FOUND` | Hallazgo inexistente |

Las excepciones de validación controladas son:

- `MethodArgumentNotValidException`.
- `HandlerMethodValidationException`.
- `MethodArgumentTypeMismatchException`.

Las excepciones no deben exponer:

- Stack traces.
- Consultas SQL.
- Credenciales.
- Tokens.
- Configuración interna.
- Rutas sensibles del sistema.
- Detalles del servidor.

## Persistencia

La aplicación utiliza Spring Data JPA y PostgreSQL.

Las consultas se construyen mediante repositorios tipados y métodos derivados de Spring Data JPA.

No se deben concatenar valores recibidos del usuario dentro de consultas SQL.

La estructura de la base de datos se controla con Flyway:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate únicamente valida el esquema. Las modificaciones se realizan mediante migraciones versionadas.

Esto permite:

- Revisar los cambios de base de datos.
- Reproducir el esquema.
- Evitar cambios automáticos inesperados.
- Mantener trazabilidad.

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
- Acción realizada.
- Actor.
- Fecha y hora.

La auditoría permite investigar:

- Quién creó un hallazgo.
- Quién lo modificó.
- Quién lo eliminó.
- Cuándo ocurrió cada operación.

El actor se obtiene del token autenticado, utilizando preferentemente:

```text
preferred_username
```

No se confía en un nombre de usuario enviado por el cliente.

## Protección contra acceso cruzado

Las operaciones de consulta, actualización y eliminación deben comprobar la organización actual.

La respuesta ante un hallazgo inexistente o perteneciente a otra organización es:

```text
404 Not Found
```

Esto evita revelar innecesariamente si un identificador existe en otra organización.

No se debe devolver una respuesta diferente para distinguir entre:

- Hallazgo inexistente.
- Hallazgo perteneciente a otra organización.

## Gestión de secretos

Las credenciales se cargan mediante variables de entorno.

Ejemplo:

```properties
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

Las contraseñas reales no deben:

- Subirse a Git.
- Escribirse en `application.properties`.
- Incluirse en `compose.yml`.
- Compartirse en capturas.
- Introducirse en logs.

El archivo `.env` se utiliza localmente y debe permanecer fuera del repositorio.

El archivo `.env.example` contiene únicamente una plantilla sin secretos reales.

## Docker y entorno local

Docker Compose se utiliza para levantar PostgreSQL y Keycloak durante el desarrollo.

El entorno local utiliza:

- PostgreSQL 17.
- Keycloak 26.7.3.
- Red local.
- Volúmenes persistentes.

Este entorno no debe considerarse una configuración de producción.

Para producción se necesitarían, como mínimo:

- TLS.
- Secretos gestionados externamente.
- Usuarios administrativos permanentes.
- Configuración de Keycloak endurecida.
- Bases de datos gestionadas.
- Restricción de puertos.
- Copias de seguridad.
- Monitorización.
- Rotación de credenciales.

## Dependencias y CI

GitHub Actions ejecuta:

- Compilación.
- Tests.
- Dependency Review.
- CodeQL.
- Análisis de seguridad del código.

CodeQL ayuda a detectar patrones inseguros en el código.

Dependency Review ayuda a identificar dependencias nuevas con posibles vulnerabilidades.

Los resultados de CI deben revisarse antes de integrar cambios en `main`.

## Riesgos OWASP considerados

### Broken Access Control

Mitigado mediante:

- Roles `ANALYST` y `ADMIN`.
- Reglas HTTP en Spring Security.
- Comprobación de organización.
- Restricción de la eliminación a administradores.

### Broken Object Level Authorization

Mitigado mediante consultas que combinan:

```text
finding_id + organization_id
```

### Injection

Mitigado mediante:

- Spring Data JPA.
- Parámetros tipados.
- Validación de entradas.
- Ausencia de concatenación SQL con datos del usuario.

### Identification and Authentication Failures

Mitigado mediante:

- Keycloak.
- Tokens JWT.
- Validación del issuer.
- Expiración de tokens.
- Roles incluidos en el token.

### Security Logging and Monitoring Failures

Mitigado parcialmente mediante:

- Auditoría de operaciones.
- Registro del actor.
- Registro de fechas.
- Eventos de creación, actualización y eliminación.

### Vulnerable and Outdated Components

Mitigado mediante:

- Dependency Review.
- CodeQL.
- Maven.
- CI automatizada.
- Revisión de actualizaciones.

## Limitaciones actuales

El proyecto todavía no incluye:

- Rate limiting.
- Protección avanzada contra abuso.
- Gestión centralizada de secretos.
- Rotación automática de claves.
- Despliegue productivo.
- TLS configurado dentro de la aplicación.
- Monitorización avanzada.
- Alertas de seguridad.
- Escaneo dinámico automatizado.
- Backup automatizado de PostgreSQL.

Estas limitaciones forman parte del desarrollo futuro.

## Reporte de vulnerabilidades

Las vulnerabilidades deben comunicarse de forma privada al responsable del repositorio.

No se deben publicar:

- Tokens.
- Contraseñas.
- Datos personales.
- Evidencias con información sensible.
- Detalles explotables antes de su corrección.

Un reporte debe incluir:

- Descripción.
- Endpoint afectado.
- Pasos para reproducirlo.
- Impacto.
- Evidencias mínimas.
- Propuesta de mitigación, si está disponible.

## Flujo de desarrollo seguro

El desarrollo se realiza en la rama:

```text
develop
```

Los cambios terminados se integran mediante Pull Request hacia:

```text
main
```

Antes de integrar un cambio se debe comprobar:

```powershell
.\mvnw.cmd clean test
```

También deben revisarse:

- Resultado de CodeQL.
- Dependency Review.
- Cambios de migraciones.
- Cambios de permisos.
- Actualización de documentación.
- Exposición accidental de secretos.