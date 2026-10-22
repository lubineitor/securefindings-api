# SecureFindings API: alcance del MVP

## Objetivo

Construir una API REST para registrar y gestionar hallazgos de seguridad con
autenticación, autorización por roles, aislamiento entre organizaciones,
auditoría y controles frente al abuso de solicitudes.

El MVP está orientado al desarrollo local y al aprendizaje. No se considera
preparado para producción hasta completar las tareas de endurecimiento indicadas
en este documento y en `SECURITY.md`.

## Funcionalidad implementada

- Autenticación JWT con Keycloak, validación de emisor y audiencia.
- Roles `ANALYST` y `ADMIN` para autorizar operaciones.
- Aislamiento de hallazgos, comentarios y auditoría por organización.
- Consulta, creación, actualización, cambio de estado y eliminación de hallazgos,
  con validación de transiciones.
- Paginación, filtros y ordenación limitada a campos permitidos.
- Consulta y actualización del nombre visible de la organización actual.
- Listado global paginado de organizaciones, restringido al rol `ADMIN`.
- Comentarios asociados a hallazgos y eventos de auditoría.
- Identificador `X-Request-ID` para relacionar peticiones y logs.
- Rate limiting distribuido mediante Redis, con respuestas `429` y `503` ante
  exceso de cuota o indisponibilidad de Redis.
- Probes de liveness y readiness, y métricas Prometheus protegidas para `ADMIN`.
- Migraciones PostgreSQL con Flyway, documentación OpenAPI y ejecución local con
  Docker Compose.
- Resumen de hallazgos agrupados por estado y severidad para la organización actual.
- Listado global paginado, consulta y actualización por UUID, y creación de
  organizaciones, restringidos al rol `ADMIN`.
- Exportación CSV UTF-8 filtrada por organización, limitada a 1.000 hallazgos y
  protegida frente a inyección de fórmulas en hojas de cálculo.
- Listado global paginado, consulta individual y creación de organizaciones,
  restringidos al rol `ADMIN`.
- Búsqueda y listado global paginado, consulta y actualización por UUID, y
  creación de organizaciones, restringidos al rol `ADMIN`.

## Criterios de aceptación del MVP

1. PostgreSQL, Redis y Keycloak arrancan con la configuración local documentada.
2. La API valida la firma, el emisor, la audiencia y los claims necesarios del
   token.
3. Las operaciones de hallazgos, comentarios y auditoría no permiten acceder a
   datos de otra organización.
4. Los roles y las transiciones de estado se aplican en cada operación protegida.
5. Las respuestas de error, el rate limiting y los identificadores de petición
   mantienen el formato documentado.
6. Las pruebas y los workflows de CI terminan correctamente antes de integrar
   cambios.

## Pendiente antes de considerar producción

- Configurar paneles y alertas para las métricas Prometheus.
- Ampliar la administración de organizaciones y revisar las reglas de
  autorización necesarias.
- Revisar CORS, TLS, gestión centralizada y rotación de secretos.
- Endurecer y escanear imágenes y contenedores.
- Definir copias de seguridad, restauración y retención de auditoría.
- Automatizar el despliegue y documentar la operación del entorno.
- Revisar los límites del rate limiter junto con el API Gateway o la protección
  perimetral del entorno.

## Orden de trabajo sugerido

1. Completar la observabilidad con paneles y alertas basados en las métricas ya
   expuestas.
2. Ampliar las pruebas de autorización y de aislamiento entre organizaciones
   para cada endpoint nuevo.
3. Preparar la configuración de producción y el despliegue automatizado.

Las decisiones de seguridad y sus limitaciones se mantienen en
[`SECURITY.md`](../SECURITY.md). Las instrucciones de instalación, configuración
y uso están en [`README.md`](../README.md).