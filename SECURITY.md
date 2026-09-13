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

## Alcance

Esta política cubre:

- Código de la API REST.
- Autenticación y autorización.
- Aislamiento entre organizaciones.
- Validación de entradas.
- Persistencia de hallazgos.
- Ciclo de vida de los hallazgos.
- Auditoría.
- Comentarios.
- Búsqueda y ordenación.
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
| Alto | Acceso no autorizado a datos sensibles o modificación de información de otra organización |
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
| Tokens | Manipulación o falsificación | Validación de firma, emisor y expiración |
| Parámetros de búsqueda | Inyección o consultas no controladas | Parámetros enlazados mediante JPA |
| Ordenación | Manipulación de propiedades internas | Lista blanca de campos permitidos |
| Datos recibidos | Valores inválidos o excesivos | Bean Validation |
| Historial | Falta de trazabilidad | Eventos de auditoría |
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
- CSRF para la API stateless.

La desactivación de CSRF se realiza porque la API utiliza autenticación mediante token Bearer y no cookies de sesión como mecanismo principal de autenticación.

## Claims utilizados

El contexto de seguridad utiliza principalmente:

```text
preferred_username
organization_id
```

### `preferred_username`

Identifica al usuario que realiza la operación.

Este valor se utiliza como actor en los eventos de auditoría.

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

Las consultas combinan el identificador del recurso con el identificador de organización. Conceptualmente:

```text
finding_id + organization_id
```

Esto evita que conocer un UUID permita acceder a información de otra organización.

Cuando un recurso no pertenece a la organización actual, la aplicación puede responder con `404` para no revelar si el identificador existe en otra organización.

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

Ejemplo:

```json
{
  "code": "INVALID_STATUS_TRANSITION",
  "message": "No se puede cambiar el estado del hallazgo...",
  "errors": {}
}
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
- Número de página.
- Tamaño de página.
- Longitud de la búsqueda textual.
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

Por ejemplo, el siguiente valor debe rechazarse:

```text
sortBy=password
```

La respuesta esperada es:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La petición contiene datos no válidos",
  "errors": {
    "parameter": "El campo de ordenación no está permitido: password"
  }
}
```

Esta validación evita utilizar el parámetro de ordenación como vector para manipular consultas o acceder a propiedades internas de persistencia.

## Protección frente a inyección SQL

La aplicación utiliza Spring Data JPA y parámetros enlazados.

Las consultas no deben construirse concatenando directamente:

- Identificadores recibidos.
- Texto de búsqueda.
- Severidades.
- Estados.
- Valores de organización.
- Campos de ordenación.
- Direcciones de ordenación.

La búsqueda se realiza mediante parámetros enlazados.

La ordenación se valida mediante una lista blanca antes de construir el `Pageable`.

Las consultas que reciben un término de búsqueda nulo utilizan `COALESCE` para mantener un tipo textual compatible con PostgreSQL y evitar errores derivados de la inferencia de tipos del controlador JDBC.

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

La auditoría permite conocer quién realizó una operación y cuándo se produjo.

La eliminación de un hallazgo conserva su evento de auditoría para mantener la trazabilidad histórica.

Las transiciones de estado inválidas no registran eventos porque la operación no llega a modificar el recurso.

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

La API evita devolver:

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
- Transiciones válidas.
- Transiciones inválidas.
- Respuesta `409` ante transiciones no permitidas.
- Ausencia de guardado tras una transición inválida.
- Ausencia de auditoría tras una transición inválida.
- Persistencia de auditoría.
- Persistencia de comentarios.
- Respuestas JSON `401`.
- Respuestas JSON `403`.
- Respuestas JSON `404`.

Las pruebas de integración utilizan PostgreSQL para verificar el comportamiento real de las consultas y restricciones de persistencia.

## Limitaciones actuales

Antes de utilizar la aplicación en producción deberían revisarse, como mínimo:

- Rate limiting.
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
- Protección frente a ataques automatizados.
- Límites de tamaño de petición.
- Gestión de logs y datos personales.

## Respuesta ante incidentes

Ante una posible vulnerabilidad:

1. No publicar detalles sensibles en una issue pública.
2. Abrir un aviso privado de seguridad.
3. Revocar o rotar las credenciales afectadas.
4. Invalidar tokens comprometidos cuando sea posible.
5. Revisar logs y eventos de auditoría.
6. Identificar las organizaciones afectadas.
7. Determinar el periodo de exposición.
8. Aplicar una corrección en `develop`.
9. Ejecutar la suite completa de pruebas.
10. Revisar CodeQL y las dependencias.
11. Integrar mediante pull request hacia `main`.
12. Documentar el impacto y la solución.
13. Comunicar las medidas correctivas a los afectados cuando corresponda.

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
- Actualización de la documentación cuando corresponda.
- Ejecución de la suite completa de pruebas.

Comando mínimo recomendado:

```powershell
.\mvnw.cmd clean test
```