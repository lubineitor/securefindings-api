# Security Policy

## Descripción

SecureFindings API es un proyecto en desarrollo para la gestión de hallazgos
de seguridad.

La aplicación incorpora controles de autenticación, autorización, validación,
persistencia, auditoría y aislamiento de datos por organización.

## Estado de seguridad

El proyecto está orientado actualmente a un entorno local y educativo.

La configuración actual no debe considerarse una configuración de producción.
Antes de desplegar la aplicación en un entorno real sería necesario revisar
los secretos, HTTPS, Keycloak, la infraestructura, la monitorización, las
copias de seguridad y la gestión de usuarios.

## Autenticación

La autenticación se realiza mediante Keycloak.

La API funciona como OAuth2 Resource Server y valida los tokens JWT emitidos
por el realm:

```text
securefindings
```

Las peticiones protegidas deben enviar:

```http
Authorization: Bearer <access_token>
```

El issuer utilizado en el entorno local es:

```text
http://localhost:8081/realms/securefindings
```

La aplicación utiliza una arquitectura stateless:

- No utiliza sesiones de aplicación.
- No utiliza Form Login.
- No utiliza HTTP Basic.
- Valida los tokens mediante Spring Security.

## Autorización

El acceso se controla mediante roles del realm de Keycloak.

| Operación | `ANALYST` | `ADMIN` |
|---|---:|---:|
| Consultar hallazgos | Permitido | Permitido |
| Crear hallazgos | Permitido | Permitido |
| Actualizar hallazgos | Permitido | Permitido |
| Consultar auditoría | Permitido | Permitido |
| Eliminar hallazgos | Denegado | Permitido |

El endpoint de salud es público:

```http
GET /api/v1/health
```

Las demás rutas requieren autenticación según la configuración de Spring
Security.

## Aislamiento por organización

Cada hallazgo y cada evento de auditoría pertenece a una organización.

La organización se obtiene del claim JWT:

```text
organization_id
```

Este valor no se acepta desde el cuerpo JSON ni desde parámetros enviados por
el cliente.

El backend:

1. Obtiene el claim del JWT.
2. Comprueba que tiene formato UUID.
3. Comprueba que la organización existe.
4. Utiliza la organización en las consultas de hallazgos.
5. Utiliza la organización en las consultas de auditoría.
6. Rechaza el acceso si el claim falta o es inválido.

La organización utilizada en el entorno local es:

```text
00000000-0000-0000-0000-000000000001
```

El `organizationId` no forma parte del modelo público `Finding`, evitando que
el cliente pueda modificar o elegir el ámbito de sus datos.

## Configuración de Keycloak

El atributo de usuario configurado es:

```text
organization_id
```

Los usuarios actuales tienen asignada la organización de desarrollo:

```text
analista
administrador
```

El cliente utilizado para obtener tokens durante las pruebas es:

```text
securefindings-cli
```

El mapper copia el atributo del usuario al access token como claim:

```text
organization_id
```

El claim se incluye en el access token porque es el token que consume la API.
La aplicación no utiliza introspección remota del token.

## Validación de entradas

Las peticiones se validan mediante Jakarta Validation y mediante las
restricciones del dominio.

Se validan:

- Título obligatorio.
- Descripción obligatoria.
- Longitudes máximas.
- Severidad válida.
- Estado válido.
- Identificadores UUID.
- Campos necesarios para cada operación.

Severidades permitidas:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Estados permitidos:

```text
OPEN
IN_PROGRESS
RESOLVED
FALSE_POSITIVE
```

## Auditoría

La aplicación registra las operaciones principales sobre los hallazgos:

```text
CREATED
UPDATED
DELETED
```

Los eventos conservan:

- Identificador del evento.
- Identificador del hallazgo.
- Organización.
- Acción.
- Usuario que ejecutó la operación.
- Fecha y hora.

La consulta también está filtrada por organización:

```http
GET /api/v1/findings/{id}/audit
```

Esto evita consultar eventos de otra organización.

## Persistencia y Flyway

La base de datos utilizada es PostgreSQL.

Hibernate solo valida el esquema mediante:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Flyway controla las modificaciones mediante migraciones versionadas:

```text
V1__crear_tabla_findings.sql
V2__crear_tabla_finding_audit.sql
V3__crear_organizaciones_y_asignar_hallazgos.sql
```

No deben modificarse migraciones que ya hayan sido ejecutadas. Los nuevos
cambios deben incluir una nueva migración.

## Gestión de secretos

Las credenciales deben gestionarse mediante variables de entorno.

No deben subirse al repositorio:

- Contraseñas.
- Tokens JWT.
- Secretos de clientes.
- Claves privadas.
- Archivos `.env`.
- Datos reales de usuarios.
- Información sensible de bases de datos.

El archivo `.env.example` solo debe contener nombres de variables y valores de
ejemplo.

## Docker

El entorno local utiliza Docker Compose mediante:

```text
compose.yml
```

Los servicios principales son PostgreSQL y Keycloak.

Los puertos se vinculan a localhost:

```text
127.0.0.1:5432
127.0.0.1:8081
```

Esto evita exponer directamente estos servicios a otras interfaces de red.

Keycloak utiliza `start-dev`, una configuración válida para desarrollo local,
pero no adecuada como configuración final de producción.

## Errores de seguridad

La API diferencia los principales errores:

| Código | Significado |
|---|---|
| `400` | Petición inválida |
| `401` | Token ausente o no válido |
| `403` | Usuario sin permisos o sin organización válida |
| `404` | Recurso no encontrado |
| `204` | Operación realizada sin contenido de respuesta |

Los errores funcionales utilizan respuestas controladas para no exponer
información interna innecesaria.

## Testing

La aplicación contiene tests para comprobar:

- Reglas del dominio.
- Servicios de aplicación.
- Controladores REST.
- Configuración de Spring Security.
- Roles `ANALYST` y `ADMIN`.
- Aislamiento por organización.
- Persistencia de hallazgos.
- Persistencia de auditoría.
- Consulta del historial.
- Integración con PostgreSQL.

Ejecutar la suite:

```powershell
.\mvnw.cmd clean test
```

## Buenas prácticas

Durante el desarrollo:

- Utilizar tokens nuevos para cada prueba autenticada.
- No imprimir tokens completos en la terminal.
- No reutilizar tokens caducados.
- No aceptar la organización desde el cliente.
- Mantener actualizados los tests.
- No modificar migraciones Flyway aplicadas.
- No subir secretos.
- Revisar los permisos de cada endpoint.
- Mantener actualizados `README.md` y `SECURITY.md` cuando cambie la
  arquitectura o la seguridad.

## Consideraciones para producción

Antes de un despliegue real se debería revisar:

- Uso obligatorio de HTTPS.
- Gestión externa de secretos.
- Rotación de credenciales.
- Configuración productiva de Keycloak.
- Eliminación de `start-dev`.
- Políticas de contraseñas.
- MFA.
- Rotación de claves JWT.
- Restricción de audiencias de los tokens.
- Límites de tamaño y frecuencia de peticiones.
- Monitorización y alertas.
- Copias de seguridad de PostgreSQL.
- Registro y protección de logs.
- Configuración de CORS.
- Revisión de Swagger y OpenAPI.

## Notificación de vulnerabilidades

No publiques credenciales, tokens ni información explotable en issues públicos.

Al notificar una vulnerabilidad incluye:

- Componente afectado.
- Pasos mínimos para reproducirla.
- Impacto.
- Evidencias relevantes sin secretos.
- Posible mitigación.

## Referencias

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/API-Security/)
- [Spring Security](https://spring.io/projects/spring-security)
- [Keycloak](https://www.keycloak.org/documentation)
- [Flyway](https://documentation.red-gate.com/flyway)