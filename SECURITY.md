# Política de seguridad

## Estado del proyecto

SecureFindings API es un proyecto en desarrollo orientado al aprendizaje y a la aplicación práctica de principios de seguridad en APIs REST.

Actualmente no debe considerarse una solución preparada para producción sin una revisión adicional de:

- Infraestructura.
- Configuración.
- Gestión de secretos.
- Observabilidad.
- Control de acceso.
- Hardening de contenedores.
- Pruebas de seguridad.
- Rate limiting distribuido.
- Protección perimetral.

## Alcance

Esta política cubre:

- Código de la API REST.
- Autenticación y autorización.
- Aislamiento entre organizaciones.
- Validación de entradas.
- Persistencia de hallazgos.
- Ciclo de vida de los hallazgos.
- Auditoría.
- Filtros del historial de auditoría.
- Comentarios.
- Búsqueda y ordenación.
- Limitación de peticiones.
- Trazabilidad de peticiones y logs.
- Configuración de Docker y PostgreSQL.
- Migraciones de base de datos.
- Workflows de GitHub Actions.
- Dependencias del proyecto.

## Cómo informar de una vulnerabilidad

No publiques vulnerabilidades de seguridad en una issue pública.

Utiliza preferentemente un aviso privado de seguridad de GitHub:

```text
https://github.com/lubineitor/securefindings-api/security/advisories/new
```

El informe debería incluir:

- Descripción del problema.
- Endpoint, clase o componente afectado.
- Pasos para reproducirlo.
- Impacto posible.
- Evidencias o payloads de prueba.
- Versión o commit afectado.
- Propuesta de mitigación, si existe.

No incluyas en el informe:

- Contraseñas reales.
- Tokens JWT válidos.
- Claves privadas.
- Datos personales.
- Información sensible de organizaciones.

## Clasificación orientativa

| Nivel | Descripción |
|---|---|
| Crítico | Compromiso completo de la aplicación, bypass de autenticación o acceso masivo entre organizaciones |
| Alto | Acceso no autorizado a datos sensibles, modificación de información de otra organización o abuso masivo de la API |
| Medio | Escalada de privilegios limitada, filtrado de información o fallo relevante de autorización |
| Bajo | Problema con impacto limitado o condiciones de explotación poco probables |

## Modelo de amenazas

Los principales riesgos considerados son:

| Activo | Amenaza | Control aplicado |
|---|---|---|
| Hallazgos | Acceso entre organizaciones | Filtros obligatorios por `organization_id` |
| Estados | Cambios de ciclo de vida no autorizados | Validación de transiciones en el dominio |
| Operaciones administrativas | Eliminación por usuarios no autorizados | Rol `ADMIN` |
| API REST | Acceso sin autenticación | OAuth2 Resource Server y JWT |
| API REST | Abuso automatizado o exceso de solicitudes | Rate limiting configurable |
| Tokens | Manipulación o falsificación | Validación de firma, emisor y expiración |
| Parámetros de búsqueda | Inyección o consultas no controladas | Parámetros enlazados mediante JPA |
| Ordenación | Manipulación de propiedades internas | Lista blanca de campos permitidos |
| Filtros de auditoría | Acceso a eventos de otra organización o filtros arbitrarios | Organización derivada del token, enums y parámetros enlazados |
| Datos recibidos | Valores inválidos o excesivos | Bean Validation |
| Historial | Falta de trazabilidad | Eventos de auditoría |
| Peticiones | Dificultad para relacionar errores y logs | Cabecera `X-Request-ID` y MDC |
| Comentarios | Contenido no validado | Validación de longitud y obligatoriedad |
| Credenciales | Exposición en el repositorio | `.env` excluido y `.env.example` sin secretos |
| Dependencias | Vulnerabilidades conocidas | Revisión automatizada y CodeQL |

## Autenticación

La API utiliza tokens JWT emitidos por Keycloak.

Las peticiones protegidas deben incluir:

```http
Authorization: Bearer <token>
```

La aplicación funciona como un OAuth2 Resource Server y valida:

- Firma del token.
- Emisor configurado.
- Fecha de expiración.
- Claims necesarios.
- Roles asociados.

La aplicación no mantiene sesiones de usuario:

```text
SessionCreationPolicy.STATELESS
```

También se deshabilitan:

- Form Login.
- HTTP Basic.

La protección CSRF se ignora únicamente para las rutas REST:

```text
/api/v1/**
```

Fuera de estas rutas, la protección CSRF permanece activa.

La exclusión limitada se utiliza porque la API REST emplea autenticación mediante token Bearer y no cookies de sesión como mecanismo principal de autenticación.

## Claims utilizados

El contexto de seguridad utiliza principalmente:

```text
preferred_username
organization_id
iss
sub
```

### `preferred_username`

Identifica al usuario que realiza la operación.

Este valor se utiliza como actor en los eventos de auditoría. Para la clave de rate limiting, la aplicación prioriza la identidad JWT estable formada por `iss` y `sub`, descrita más adelante.

### `iss` y `sub`

`iss` identifica al emisor del token y `sub` identifica al usuario dentro de ese emisor. La aplicación combina ambos claims con `organization_id` para mantener estable la cuota aunque cambie el nombre de usuario y para separar cuentas distintas con el mismo nombre.

Si no existe un usuario autenticado, las operaciones técnicas o de prueba pueden utilizar:

```text
system
```

### `organization_id`

Identifica la organización activa del usuario.

Debe:

- Tener formato UUID.
- Estar presente en el token.
- Corresponder con una organización existente.
- Utilizarse para limitar el acceso a los datos.

Si el token:

- No contiene `organization_id`.
- Contiene un valor con formato inválido.
- Hace referencia a una organización inexistente.

La petición debe rechazarse.

La organización nunca debe aceptarse desde un parámetro enviado por el cliente como mecanismo de autorización.

## Autorización

Los permisos se aplican por endpoint:

| Operación | `ANALYST` | `ADMIN` |
|---|---:|---:|
| Consultar hallazgos | Sí | Sí |
| Crear hallazgos | Sí | Sí |
| Actualizar hallazgos | Sí | Sí |
| Actualizar estados | Sí | Sí |
| Consultar auditoría | Sí | Sí |
| Consultar comentarios | Sí | Sí |
| Crear comentarios | Sí | Sí |
| Eliminar hallazgos | No | Sí |

La aplicación responde:

- `401 UNAUTHORIZED` cuando falta autenticación válida.
- `403 FORBIDDEN` cuando el usuario está autenticado pero no tiene permisos suficientes.
- `409 INVALID_STATUS_TRANSITION` cuando la operación es válida para el usuario, pero no para el estado actual del hallazgo.
- `429 RATE_LIMIT_EXCEEDED` cuando el cliente supera el límite configurado.

## Limitación de peticiones

La API incorpora un filtro de limitación de peticiones para reducir el impacto de:

- Abuso automatizado.
- Repetición excesiva de solicitudes.
- Uso accidentalmente elevado.
- Ataques básicos de agotamiento de recursos.

El filtro se aplica a las rutas bajo:

```text
/api/v1/
```

El endpoint público:

```text
/api/v1/health
```

queda excluido para que pueda utilizarse en comprobaciones de disponibilidad.

### Identificación del cliente

Para peticiones autenticadas con JWT y un `organization_id` válido, la clave combina la organización con el emisor (`iss`) y el subject (`sub`). La pareja `iss` + `sub` identifica de forma estable la cuenta: un cambio de `preferred_username` no crea una cuota nueva, y dos cuentas distintas no comparten cuota aunque tengan el mismo nombre. Si falta `iss` o `sub`, el filtro usa el nombre del principal dentro de la organización.

Si el claim `organization_id` falta o no contiene un UUID válido, se utiliza la clave basada solo en el nombre del principal. Este fallback solo selecciona el contador del rate limiter; no autoriza la petición. El contexto de organización sigue rechazando tokens sin un claim `organization_id` válido o asociado a una organización existente.

Para otras autenticaciones se utiliza el nombre del principal obtenido del contexto de seguridad.

Para peticiones no autenticadas se utiliza la dirección remota:

```text
HttpServletRequest.getRemoteAddr()
```

La aplicación no utiliza directamente cabeceras controladas por el cliente como:

```text
X-Forwarded-For
```

Estas cabeceras solo deberían interpretarse cuando existe un proxy de confianza y la infraestructura elimina o sobrescribe los valores enviados externamente.

### Configuración

Los valores predeterminados son:

```properties
securefindings.rate-limit.max-requests=60
securefindings.rate-limit.window=60s
```

También pueden configurarse mediante:

```text
SECUREFINDINGS_RATE_LIMIT_MAX_REQUESTS
SECUREFINDINGS_RATE_LIMIT_WINDOW
```

Los valores deben validarse al iniciar la aplicación:

- El número máximo de peticiones debe ser positivo.
- La duración de la ventana debe ser positiva.
- No deben utilizarse valores excesivamente bajos para endpoints necesarios por monitores o clientes legítimos.
- No deben utilizarse valores excesivamente altos como sustituto de una protección perimetral.

### Respuesta cuando se supera el límite

Cuando se supera el límite se devuelve:

```http
429 Too Many Requests
```

La respuesta incluye:

```http
Retry-After: <segundos>
```

También incluye un identificador de correlación:

```http
X-Request-ID: <identificador>
```

Ejemplo:

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Se ha superado el límite de peticiones",
  "errors": {}
}
```

La respuesta utiliza:

```http
Cache-Control: no-store
```

para evitar que un error temporal se almacene en cachés.

### Limitaciones de la implementación actual

El contador se almacena en memoria dentro de cada instancia de la aplicación.

Por tanto:

- El límite se reinicia al reiniciar la aplicación.
- Varias instancias tienen contadores independientes.
- No existe sincronización entre nodos.
- No protege por sí solo frente a ataques distribuidos.
- No sustituye a un firewall, WAF, API Gateway o protección DDoS.
- La configuración debe coordinarse con balanceadores y proxies.

Para producción se recomienda utilizar:

- Rate limiting en el API Gateway.
- Un almacén compartido como Redis.
- Límites por usuario, organización y dirección IP.
- Límites diferenciados por endpoint.
- Métricas de solicitudes rechazadas.
- Alertas ante incrementos anómalos.
- Protección adicional para endpoints costosos.

## Trazabilidad de peticiones

Cada petición recibe un identificador de correlación mediante:

```http
X-Request-ID
```

El filtro de correlación se ejecuta antes de la autenticación y garantiza que el identificador esté disponible también para:

- Respuestas `401`.
- Respuestas `403`.
- Respuestas `429`.
- Respuestas de validación.
- Errores de la aplicación.
- Logs generados durante el procesamiento.

### Validación del identificador

Si el cliente envía un valor válido, se conserva.

Los valores válidos deben contener entre 1 y 64 caracteres de los siguientes tipos:

```text
A-Z
a-z
0-9
.
_
-
```

Si el valor:

- No existe.
- Está vacío.
- Contiene espacios.
- Contiene saltos de línea.
- Contiene caracteres no permitidos.
- Supera la longitud máxima.

La aplicación genera un UUID nuevo.

Esto evita que una cabecera controlada por el cliente introduzca valores peligrosos en las respuestas o en los logs.

### Uso en logs

El identificador se almacena temporalmente en MDC con la clave:

```text
requestId
```

La configuración de consola es:

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [requestId=%X{requestId}] %logger{36} - %msg%n
```

El contexto MDC se limpia al finalizar la petición para evitar que un identificador se reutilice accidentalmente en otra solicitud atendida por el mismo hilo.

### Consideraciones de seguridad

El `X-Request-ID`:

- No es un mecanismo de autenticación.
- No representa la identidad del usuario.
- No sustituye al token JWT.
- No debe utilizarse para autorizar operaciones.
- No debe contener información personal.
- No debe contener tokens ni secretos.
- No debe almacenarse como sustituto de la auditoría funcional.

Su objetivo es facilitar la correlación técnica entre una petición, su respuesta y los logs asociados.

## Aislamiento entre organizaciones

Todos los accesos a hallazgos, comentarios y auditoría deben estar limitados a la organización del token.

El aislamiento se aplica en:

- Contexto de organización.
- Servicios de aplicación.
- Métodos de repositorio.
- Consultas paginadas.
- Búsquedas.
- Filtros.
- Consulta por identificador.
- Actualizaciones.
- Eliminaciones.
- Auditoría.
- Comentarios.

Las consultas combinan el identificador del recurso con el identificador de organización.

Conceptualmente:

```text
finding_id + organization_id
```

Esto evita que conocer un UUID permita acceder a información de otra organización.

Cuando un recurso no pertenece a la organización actual, la aplicación puede responder con `404` para no revelar si el identificador existe en otra organización.

El registro de auditoría aplica la misma defensa: antes de guardar un evento comprueba que el hallazgo pertenece a la organización activa. La excepción controlada es `DELETED`, que puede registrarse después de eliminar el hallazgo solo si ya existe historial de ese hallazgo dentro de la misma organización.

La base de datos refuerza este diseño mediante:

- Columna `organization_id` obligatoria.
- Claves externas hacia `organizations`.
- Índices por organización.
- Relación de organización en hallazgos.
- Relación de organización en auditoría.
- Relación de organización en comentarios.

## Ciclo de vida y transiciones de estado

Los estados disponibles son:

```text
OPEN
IN_PROGRESS
RESOLVED
FALSE_POSITIVE
```

Las transiciones permitidas son:

| Estado actual | Estados permitidos |
|---|---|
| `OPEN` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `FALSE_POSITIVE` |
| `IN_PROGRESS` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `FALSE_POSITIVE` |
| `RESOLVED` | `RESOLVED`, `OPEN` |
| `FALSE_POSITIVE` | `FALSE_POSITIVE`, `OPEN` |

Se permite mantener el mismo estado para que las operaciones sean idempotentes.

Los estados finales:

- `RESOLVED`
- `FALSE_POSITIVE`

solo pueden mantenerse o reabrirse como `OPEN`.

No se permite cambiar directamente:

```text
RESOLVED -> FALSE_POSITIVE
FALSE_POSITIVE -> RESOLVED
```

La validación se realiza en el dominio mediante una regla explícita de transición.

El servicio comprueba la transición antes de:

- Crear una nueva entidad para guardar.
- Ejecutar `save`.
- Registrar auditoría.
- Devolver una respuesta de éxito.

Una transición inválida produce:

```http
409 Conflict
```

Las transiciones rechazadas no modifican el hallazgo ni generan eventos de auditoría.

## Validación de entradas

Las peticiones se validan antes de llegar al servicio.

Se controlan, entre otros:

- Campos obligatorios.
- Cadenas vacías.
- Longitudes máximas.
- Severidades permitidas.
- Estados permitidos.
- Acciones de auditoría permitidas.
- Número de página.
- Tamaño de página.
- Longitud de la búsqueda textual.
- Formato de `requestId`.
- Identificadores UUID.
- Campos de ordenación.
- Direcciones de ordenación.
- Contenido de comentarios.

Los valores inválidos producen una respuesta `400` con formato uniforme:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "field": "Descripción del error"
  }
}
```

Las respuestas de validación incluyen la cabecera `X-Request-ID` para facilitar su localización en los logs.

## Búsqueda textual

La búsqueda textual se realiza sobre el título y la descripción del hallazgo.

El parámetro `q`:

- Tiene una longitud máxima.
- Se normaliza antes de ejecutar la consulta.
- Ignora diferencias entre mayúsculas y minúsculas.
- Se procesa mediante parámetros enlazados.
- No se concatena directamente en SQL.
- Puede omitirse sin provocar errores de tipo en PostgreSQL.

Un valor vacío se trata como ausencia de filtro.

## Ordenación segura

La API únicamente permite ordenar por campos previamente definidos:

```text
createdAt
updatedAt
title
severity
status
```

Las direcciones disponibles son:

```text
ASC
DESC
```

Los valores se validan mediante enums antes de crear el objeto `Pageable`.

No se permite que el cliente proporcione directamente:

- Propiedades JPA arbitrarias.
- Nombres de columnas SQL.
- Expresiones SQL.
- Fragmentos de una cláusula `ORDER BY`.

Esta validación evita utilizar el parámetro de ordenación como vector para manipular consultas o acceder a propiedades internas de persistencia.

## Filtros del historial de auditoría

El historial de auditoría permite filtrar por:

```text
action
requestId
```

Las acciones válidas son:

```text
CREATED
UPDATED
DELETED
COMMENTED
```

El `requestId` debe cumplir el mismo formato que la cabecera `X-Request-ID`:

- Entre 1 y 64 caracteres.
- Letras mayúsculas y minúsculas.
- Dígitos.
- Puntos.
- Guiones.
- Guiones bajos.

Los filtros de auditoría:

- Se aplican junto al `organization_id` obtenido del token.
- No aceptan un identificador de organización enviado por el cliente.
- No permiten campos de filtrado arbitrarios.
- Utilizan métodos de repositorio con parámetros enlazados.
- Mantienen el orden cronológico.
- Mantienen la paginación.
- No exponen eventos de otra organización.

Ejemplo:

```text
GET /api/v1/findings/{findingId}/audit?action=UPDATED&requestId=audit-request-123
```

Una acción inválida o un `requestId` con formato incorrecto producen:

```http
400 Bad Request
```

## Protección frente a inyección SQL

La aplicación utiliza Spring Data JPA y parámetros enlazados.

Las consultas no deben construirse concatenando directamente:

- Identificadores recibidos.
- Texto de búsqueda.
- Severidades.
- Estados.
- Acciones de auditoría.
- Identificadores de petición.
- Valores de organización.
- Campos de ordenación.
- Direcciones de ordenación.

La búsqueda y los filtros de auditoría se realizan mediante parámetros enlazados.

La ordenación se valida mediante una lista blanca antes de construir el `Pageable`.

Las consultas que reciben un término de búsqueda nulo utilizan una construcción compatible con PostgreSQL para evitar errores derivados de la inferencia de tipos del controlador JDBC.

## Auditoría

Se registran las operaciones relevantes:

```text
CREATED
UPDATED
DELETED
COMMENTED
```

Los eventos incluyen:

- Identificador del hallazgo.
- Acción realizada.
- Actor.
- Fecha de ocurrencia.
- Organización.
- Identificador técnico de la petición (`requestId`), cuando existe.

La auditoría permite conocer quién realizó una operación y cuándo se produjo.

La eliminación de un hallazgo conserva su evento de auditoría para mantener la trazabilidad histórica.

Las transiciones de estado inválidas no registran eventos porque la operación no llega a modificar el recurso.

El identificador `X-Request-ID` facilita la trazabilidad técnica de la petición que generó un evento, pero no sustituye al actor.

El valor se persiste en:

```text
finding_audit.request_id
```

Esto permite relacionar el evento con:

- Los logs.
- La respuesta HTTP.
- La petición original.
- Otros eventos de la misma operación.

En eventos técnicos o registros históricos puede ser `null`.

## Comentarios

Los comentarios pertenecen a una organización y a un hallazgo concreto.

La aplicación valida:

- Que el hallazgo exista dentro de la organización.
- Que el contenido sea obligatorio.
- Que el contenido respete la longitud permitida.
- Que el usuario tenga permisos para crear el comentario.

La creación de un comentario genera un evento de auditoría:

```text
COMMENTED
```

Los comentarios de una organización no deben ser visibles para usuarios de otra organización.

## Manejo de errores

Las excepciones controladas se transforman en respuestas JSON.

Las respuestas incluyen:

```http
X-Request-ID: <identificador>
```

La aplicación evita devolver:

- Stack traces.
- Rutas internas.
- Consultas SQL.
- Contraseñas.
- Tokens.
- Claves privadas.
- Información innecesaria de infraestructura.

Errores principales:

| HTTP | Código | Situación |
|---:|---|---|
| `400` | `VALIDATION_ERROR` | Datos o parámetros inválidos |
| `401` | `UNAUTHORIZED` | Token ausente o inválido |
| `403` | `FORBIDDEN` | Usuario sin permisos suficientes |
| `404` | `FINDING_NOT_FOUND` | Hallazgo no disponible para la organización |
| `409` | `INVALID_STATUS_TRANSITION` | Transición de estado no permitida |
| `429` | `RATE_LIMIT_EXCEEDED` | Límite de peticiones superado |
| `500` | Error interno | Error no controlado |

## Base de datos y migraciones

La base de datos utilizada en desarrollo es PostgreSQL.

Los cambios de esquema se gestionan con Flyway mediante migraciones versionadas.

Las migraciones actuales incluyen cambios relacionados con:

- Estructura inicial de hallazgos.
- Creación de organizaciones.
- Asignación de hallazgos a organizaciones.
- Creación de comentarios.
- Auditoría de comentarios.
- Persistencia del identificador de petición en auditoría mediante `V6__registrar_request_id_en_auditoria.sql`.

Las migraciones no deben modificarse después de haberse aplicado en un entorno compartido.

Para nuevos cambios debe crearse una nueva migración versionada.

## Gestión de secretos

No deben subirse al repositorio:

- Contraseñas.
- Tokens JWT.
- Claves privadas.
- Credenciales de Keycloak.
- Credenciales de PostgreSQL.
- Archivos `.env` reales.
- Dumps con datos sensibles.
- Certificados privados.

Debe utilizarse:

```text
.env.example
```

para documentar las variables necesarias sin incluir valores reales.

En producción se recomienda utilizar:

- Secret managers.
- Variables protegidas del entorno.
- Rotación periódica.
- Credenciales diferentes por entorno.
- Principio de mínimo privilegio.
- Cuentas de base de datos con permisos limitados.
- Separación de credenciales de desarrollo y producción.

## Docker y entorno local

Docker Compose se utiliza para levantar la infraestructura local.

El entorno local no debe exponerse directamente a Internet.

Para entornos reales se recomienda:

- No utilizar contraseñas de desarrollo.
- Restringir los puertos publicados.
- Actualizar PostgreSQL y Keycloak.
- Utilizar redes privadas.
- Configurar TLS.
- Aplicar límites de recursos.
- Revisar los logs antes de almacenarlos.
- Evitar registrar tokens o datos sensibles.
- Ejecutar los contenedores con el menor privilegio posible.
- Escanear las imágenes utilizadas.
- Aplicar el rate limiting en un componente compartido o perimetral.

## Dependencias y automatización

El repositorio dispone de:

```text
.github/workflows/ci.yml
.github/workflows/codeql.yml
```

El workflow de integración continua:

- Compila el proyecto.
- Ejecuta las pruebas.
- Revisa dependencias en pull requests.

La revisión de dependencias está definida dentro de `ci.yml`. No existe un workflow independiente llamado `dependency-review.yml`.

CodeQL analiza el código Java para detectar posibles vulnerabilidades y problemas de seguridad.

Antes de integrar cambios debe comprobarse:

```powershell
.\mvnw.cmd clean test
```

## Pruebas de seguridad existentes

El proyecto incluye pruebas para comprobar:

- Acceso sin token.
- Acceso con rol insuficiente.
- Permisos de `ANALYST`.
- Permisos de `ADMIN`.
- Rechazo de claims inválidos.
- Rechazo de organizaciones inexistentes.
- Aislamiento entre organizaciones.
- Consulta de hallazgos de otra organización.
- Validación de datos.
- Parámetros de paginación.
- Filtros y búsqueda.
- Ordenación permitida.
- Ordenación no permitida.
- Filtros de auditoría por acción.
- Filtros de auditoría por `requestId`.
- Filtros combinados de auditoría.
- Rechazo de acciones de auditoría inválidas.
- Rechazo de identificadores de petición inválidos.
- Transiciones válidas.
- Transiciones inválidas.
- Respuesta `409` ante transiciones no permitidas.
- Ausencia de guardado tras una transición inválida.
- Ausencia de auditoría tras una transición inválida.
- Persistencia de auditoría.
- Persistencia y recuperación del `requestId` en auditoría.
- Persistencia de comentarios.
- Respuestas JSON `401`.
- Respuestas JSON `403`.
- Respuestas JSON `404`.
- Limitación por dirección IP.
- Limitación por usuario autenticado.
- Cuotas JWT aisladas por organización y por la pareja estable `iss` + `sub`.
- Subjects distintos no comparten cuota aunque coincida `preferred_username`.
- Un cambio de `preferred_username` conserva la cuota del mismo subject.
- Reutilización de la cuota por nombre principal cuando el claim de organización es inválido.
- Respuesta `429`.
- Cabecera `Retry-After`.
- Cabecera `X-Request-ID`.
- Generación de identificadores seguros.
- Rechazo de identificadores no válidos.
- Limpieza del contexto MDC.
- Exclusión del endpoint de health check.
- Integración del filtro con Spring Security.

Las pruebas de integración utilizan PostgreSQL para verificar el comportamiento real de las consultas y restricciones de persistencia.

## Limitaciones actuales

Antes de utilizar la aplicación en producción deberían revisarse, como mínimo:

- Rate limiting distribuido.
- Configuración CORS.
- Gestión centralizada de secretos.
- TLS y terminación HTTPS.
- Monitorización y alertas.
- Rotación de claves.
- Política de retención de auditoría.
- Copias de seguridad.
- Restauración ante incidentes.
- Hardening de contenedores.
- Escaneo de imágenes Docker.
- Gestión formal de usuarios y organizaciones.
- Pruebas de penetración.
- Revisión de configuración de Keycloak.
- Política de bloqueo ante abuso.
- Protección DDoS.
- Límites de tamaño de petición.
- Gestión de logs y datos personales.
- Almacenamiento centralizado de logs.
- Correlación entre logs de diferentes instancias.
- Propagación del identificador a sistemas externos.

## Respuesta ante incidentes

Ante una posible vulnerabilidad:

1. No publicar detalles sensibles en una issue pública.
2. Abrir un aviso privado de seguridad.
3. Revocar o rotar las credenciales afectadas.
4. Invalidar tokens comprometidos cuando sea posible.
5. Revisar logs y eventos de auditoría.
6. Comparar el `requestId` persistido en auditoría con los logs y la cabecera `X-Request-ID`.
7. Utilizar `X-Request-ID` para localizar las peticiones relacionadas.
8. Revisar solicitudes rechazadas por rate limiting.
9. Identificar las organizaciones afectadas.
10. Determinar el periodo de exposición.
11. Aplicar una corrección en `develop`.
12. Ejecutar la suite completa de pruebas.
13. Revisar CodeQL y las dependencias.
14. Integrar mediante pull request hacia `main`.
15. Documentar el impacto y la solución.
16. Comunicar las medidas correctivas a los afectados cuando corresponda.

## Revisión de cambios

Todo cambio que afecte a seguridad debe incluir:

- Explicación del riesgo.
- Justificación de la mitigación.
- Pruebas automatizadas.
- Revisión del impacto en organizaciones.
- Revisión de permisos.
- Revisión de entradas y salidas.
- Comprobación de que no se han añadido secretos.
- Revisión de las migraciones de base de datos.