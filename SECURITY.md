# Security Policy

## Descripción

SecureFindings API es un proyecto en desarrollo orientado a la gestión segura de hallazgos de seguridad.

La aplicación incorpora controles relacionados con:

- Autenticación.
- Autorización.
- Validación de entradas.
- Control de acceso por organización.
- Persistencia segura.
- Auditoría.
- Gestión de secretos.
- Testing automatizado.
- Seguridad de la infraestructura local.

## Estado del proyecto

El proyecto se encuentra en desarrollo y está orientado principalmente a entornos locales, educativos y de portfolio.

La configuración actual no debe considerarse preparada para producción sin realizar una revisión adicional de infraestructura, secretos, observabilidad, alta disponibilidad y configuración de Keycloak.

## Autenticación

La API utiliza Keycloak como proveedor de identidad.

Spring Security funciona como OAuth2 Resource Server y valida los tokens JWT recibidos en las peticiones.

El emisor configurado para el entorno local es:

```text
http://localhost:8081/realms/securefindings
```

Las peticiones protegidas deben incluir:

```http
Authorization: Bearer <token>
```

El token debe proceder del realm `securefindings`.

La API no utiliza:

- Sesiones de usuario.
- Form Login.
- Autenticación HTTP Basic.

La política de sesión es stateless.

## Autorización

El acceso se controla mediante roles del realm de Keycloak.

### Matriz de permisos

| Operación | `ANALYST` | `ADMIN` |
|---|---:|---:|
| `GET /api/v1/findings` | Permitido | Permitido |
| `GET /api/v1/findings/{id}` | Permitido | Permitido |
| `POST /api/v1/findings` | Permitido | Permitido |
| `PUT /api/v1/findings/{id}` | Permitido | Permitido |
| `PATCH /api/v1/findings/{id}/status` | Permitido | Permitido |
| `GET /api/v1/findings/{id}/audit` | Permitido | Permitido |
| `DELETE /api/v1/findings/{id}` | Denegado | Permitido |

El endpoint siguiente es público:

```http
GET /api/v1/health
```

Cualquier ruta no declarada explícitamente requiere autenticación.

## Aislamiento por organización

La aplicación utiliza `organization_id` para separar los datos entre organizaciones.

Este valor debe llegar en un claim firmado del token JWT:

```text
organization_id
```

El cliente no puede indicar la organización en el cuerpo de la petición ni seleccionar una organización arbitraria mediante un parámetro HTTP.

El flujo de validación es:

1. Spring Security valida la firma y el emisor del token.
2. `OrganizationContext` obtiene el claim `organization_id`.
3. El claim se convierte a `UUID`.
4. Se comprueba que la organización existe en PostgreSQL.
5. Los repositorios filtran los datos por esa organización.
6. Si el claim no existe, no es válido o la organización no existe, el acceso se rechaza.

La organización de desarrollo utilizada por Flyway es:

```text
00000000-0000-0000-0000-000000000001
```

El `organizationId` no forma parte del modelo público `Finding`.

En las entidades de persistencia, la organización se almacena internamente y no se incluye en las respuestas JSON públicas.

## Auditoría

La aplicación registra las principales operaciones realizadas sobre los hallazgos:

```text
CREATED
UPDATED
DELETED
```

Cada evento de auditoría conserva:

- Identificador del evento.
- Identificador del hallazgo.
- Organización propietaria.
- Acción ejecutada.
- Usuario autenticado.
- Fecha y hora de la operación.

La consulta del historial también está filtrada por organización:

```http
GET /api/v1/findings/{id}/audit
```

Esto evita que un usuario consulte eventos pertenecientes a otra organización.

## Validación de entradas

Las peticiones de creación y actualización se validan mediante Jakarta Validation.

Se validan, entre otros aspectos:

- Título obligatorio.
- Descripción obligatoria.
- Longitudes máximas.
- Severidad válida.
- Estado válido.
- Formato UUID de los identificadores.

Los valores de severidad permitidos son:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Los valores de estado permitidos son:

```text
OPEN
IN_PROGRESS
RESOLVED
FALSE_POSITIVE
```

Las peticiones inválidas no deben llegar a la lógica de persistencia.

## Persistencia

La aplicación utiliza:

- PostgreSQL.
- Spring Data JPA.
- Hibernate.
- Flyway.

Hibernate se configura con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Esto significa que Hibernate valida el esquema existente, pero no crea ni modifica tablas automáticamente.

Flyway controla la evolución de la base de datos mediante migraciones versionadas.

Las migraciones actuales son:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

Las migraciones existentes no deben modificarse después de haber sido ejecutadas en una base de datos compartida. Los cambios posteriores deben realizarse mediante nuevas migraciones.

## Gestión de secretos

Las credenciales no deben escribirse directamente en:

- Código Java.
- `application.properties`.
- `compose.yml`.
- Tests.
- Documentación.
- Issues o commits.

La configuración local utiliza variables de entorno y un archivo `.env`.

El archivo `.env` debe permanecer fuera del control de versiones.

La plantilla pública es:

```text
.env.example
```

Esta plantilla debe contener únicamente nombres de variables y valores de ejemplo que no sean secretos reales.

## Docker

El entorno local utiliza Docker Compose mediante:

```text
compose.yml
```

Los servicios principales son:

- PostgreSQL.
- Keycloak.

Los puertos están vinculados a `localhost` para evitar exponerlos innecesariamente en la red local:

```text
127.0.0.1:5432
127.0.0.1:8081
```

Keycloak utiliza actualmente:

```yaml
command: ["start-dev"]
```

Esta configuración es válida para desarrollo local, pero no debe utilizarse como configuración final de producción.

## Seguridad de la API

La configuración de Spring Security:

- Desactiva CSRF porque la API utiliza autenticación stateless mediante bearer tokens.
- Desactiva Form Login.
- Desactiva HTTP Basic.
- No utiliza sesiones persistentes.
- Requiere autenticación para las rutas protegidas.
- Aplica autorización basada en roles.
- Valida tokens JWT mediante el issuer configurado.
- Utiliza el claim `preferred_username` como identidad principal.

Swagger y OpenAPI están permitidos para facilitar la documentación local de la API. En un entorno productivo debe revisarse si deben permanecer expuestos.

## Errores y respuestas

La API utiliza respuestas controladas para los errores funcionales.

Ejemplo de hallazgo inexistente:

```json
{
  "code": "FINDING_NOT_FOUND",
  "message": "No se ha encontrado el hallazgo",
  "errors": {}
}
```

Los errores de autenticación y autorización deben diferenciarse:

- `401 Unauthorized`: falta autenticación o el token no es válido.
- `403 Forbidden`: el usuario está autenticado, pero no tiene permisos.
- `404 Not Found`: el recurso no existe dentro del contexto autorizado.
- `400 Bad Request`: la petición no cumple las validaciones.

## Testing de seguridad

La aplicación dispone de tests para comprobar:

- Configuración de Spring Security.
- Acceso permitido a usuarios `ANALYST`.
- Acceso permitido a usuarios `ADMIN`.
- Prohibición de eliminación para `ANALYST`.
- Validación de peticiones.
- Aislamiento por organización.
- Persistencia de hallazgos.
- Persistencia de auditoría.
- Consulta del historial.

Ejecutar la suite completa:

```powershell
.\mvnw.cmd clean test
```

## Buenas prácticas para desarrollo

Antes de realizar un commit:

- No incluir `.env`.
- No incluir tokens JWT.
- No incluir contraseñas.
- No incluir datos personales reales.
- No modificar migraciones Flyway ya ejecutadas.
- Mantener los tests actualizados.
- Documentar los cambios sustanciales.
- Revisar los permisos de cada endpoint.
- Comprobar que las consultas contienen el filtro de organización correspondiente.

## Notificación de vulnerabilidades

No publiques credenciales, tokens, datos personales ni detalles explotables en issues públicos.

Para comunicar una vulnerabilidad:

1. No la divulgues públicamente de forma inmediata.
2. Describe el endpoint o componente afectado.
3. Incluye los pasos mínimos para reproducirla.
4. Explica el impacto de seguridad.
5. Evita incluir secretos reales.
6. Proporciona una posible medida de mitigación si la conoces.

## Referencias

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/API-Security/)
- [Spring Security](https://spring.io/projects/spring-security)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Flyway Documentation](https://documentation.red-gate.com/flyway)