# Catálogo Unificado de Endpoints REST — DonaTrack

> **Fuente Canónica:** Especificaciones OpenAPI 3.0 en [`docs/arquitectura/contratos/`](../arquitectura/contratos/)
> **Total de Endpoints:** 94

<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->

## Microservicio: DonaTrack - Donaciones Service API (`openapi-donaciones.yaml`)

* **Versión:** `1.0.0`
* **Descripción:** API REST para gestión de donaciones independientes (máquina de 7 estados), necesidades de entidades beneficiarias, donantes y propuestas de asignación algorítmica.
* **Servidor Local:** `http://localhost:8080`

| Método | Path | Operación | Request Body | Códigos de Respuesta |
|:---:|---|---|---|---|
| `GET` | `/donaciones-independientes` | `listarDonacionesIndependientes` | `-` | `200 (DonacionIndependienteResponseDTO)` |
| `GET` | `/donaciones-independientes/{id}` | `obtenerDonacionIndependientePorId` | `-` | `200 (DonacionIndependienteResponseDTO), 404 (-)` |
| `PATCH` | `/donaciones-independientes/{id}/estado` | `cambiarEstadoDonacionIndependiente` | `CambioEstadoDonacionIndependienteRequestDTO` | `200 (DonacionIndependienteResponseDTO), 400 (-)` |
| `POST` | `/api/donaciones` | `registrarDonacion` | `DonacionInputDTO` | `201 (DonacionOutputDTO), 400 (-)` |
| `GET` | `/api/donaciones` | `listarDonaciones` | `-` | `200 (DonacionOutputDTO)` |
| `GET` | `/api/donaciones/{id}` | `obtenerDonacionPorId` | `-` | `200 (DonacionOutputDTO), 404 (-)` |
| `POST` | `/api/necesidades` | `registrarNecesidad` | `-` | `201 (-)` |
| `GET` | `/api/necesidades` | `listarNecesidades` | `-` | `200 (-)` |
| `GET` | `/api/necesidades/{id}` | `obtenerNecesidadPorId` | `-` | `200 (-), 404 (-)` |
| `PUT` | `/api/necesidades/{id}` | `actualizarNecesidad` | `-` | `200 (-), 400 (-), 404 (-)` |
| `DELETE` | `/api/necesidades/{id}` | `eliminarNecesidad` | `-` | `204 (-), 404 (-)` |
| `POST` | `/api/asignaciones/ejecuciones` | `ejecutarAsignacion` | `-` | `201 (-)` |
| `GET` | `/api/asignaciones/ejecuciones` | `historialEjecucionesAsignacion` | `-` | `200 (-)` |
| `GET` | `/api/asignaciones/propuestas` | `listarPropuestasAsignacion` | `-` | `200 (-)` |
| `PUT` | `/api/asignaciones/propuestas/{id}/estado` | `actualizarEstadoPropuesta` | `-` | `200 (-)` |
| `POST` | `/api/entidades` | `registrarEntidadBeneficiaria` | `-` | `201 (-)` |
| `GET` | `/api/entidades` | `listarEntidadesBeneficiarias` | `-` | `200 (-)` |
| `GET` | `/api/entidades/{id}` | `obtenerEntidadPorId` | `-` | `200 (-), 404 (-)` |
| `PUT` | `/api/entidades/{id}` | `actualizarEntidad` | `-` | `200 (-), 400 (-), 404 (-)` |
| `DELETE` | `/api/entidades/{id}` | `eliminarEntidad` | `-` | `204 (-), 404 (-)` |
| `POST` | `/api/donantes` | `registrarDonante` | `-` | `201 (-)` |
| `GET` | `/api/donantes` | `listarDonantes` | `-` | `200 (-)` |
| `GET` | `/api/donantes/{id}` | `obtenerDonantePorId` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/donantes/{id}` | `eliminarDonante` | `-` | `204 (-), 404 (-)` |
| `POST` | `/api/donantes/archivos` | `cargarArchivoDonantes` | `-` | `202 (-), 400 (-)` |
| `GET` | `/api/donantes/archivos/{id}` | `obtenerArchivoDonantes` | `-` | `200 (-), 404 (-)` |
| `GET` | `/api/categorias` | `listarCategorias` | `-` | `200 (-)` |
| `POST` | `/api/categorias` | `crearCategoria` | `-` | `201 (-)` |
| `GET` | `/api/categorias/{id}` | `obtenerCategoria` | `-` | `200 (-), 404 (-)` |
| `PUT` | `/api/categorias/{id}` | `actualizarCategoria` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/categorias/{id}` | `eliminarCategoria` | `-` | `200 (-), 404 (-)` |
| `GET` | `/api/subcategorias` | `listarSubcategorias` | `-` | `200 (-)` |
| `POST` | `/api/subcategorias` | `crearSubcategoria` | `-` | `201 (-)` |
| `GET` | `/api/subcategorias/{id}` | `obtenerSubcategoria` | `-` | `200 (-), 404 (-)` |
| `PUT` | `/api/subcategorias/{id}` | `actualizarSubcategoria` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/subcategorias/{id}` | `eliminarSubcategoria` | `-` | `200 (-), 404 (-)` |
| `POST` | `/api/subcategorias/{id}/aliases` | `agregarAliasSubcategoria` | `AliasSubcategoriaInputDTO` | `200 (SubcategoriaOutputDTO), 400 (-), 404 (-)` |
| `DELETE` | `/api/subcategorias/{id}/aliases/{alias}` | `quitarAliasSubcategoria` | `-` | `200 (SubcategoriaOutputDTO), 404 (-)` |
| `GET` | `/api/items-normalizados/pendientes` | `listarItemsNormalizadosPendientes` | `-` | `200 (ItemDonacionNormalizadoOutputDTO)` |
| `GET` | `/api/items-normalizados/{id}` | `obtenerItemNormalizadoPorId` | `-` | `200 (ItemDonacionNormalizadoOutputDTO), 404 (-)` |
| `PATCH` | `/api/items-normalizados/{id}` | `actualizarEstadoItemNormalizado` | `ItemDonacionNormalizadoPatchDTO` | `200 (ItemDonacionNormalizadoOutputDTO), 400 (-), 404 (-)` |
| `GET` | `/api/personas` | `listarPersonasDonaciones` | `-` | `200 (-)` |
| `POST` | `/api/personas` | `crearPersonaDonaciones` | `-` | `201 (-)` |
| `PUT` | `/api/personas/{id}` | `actualizarPersonaDonaciones` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/personas/{id}` | `eliminarPersonaDonaciones` | `-` | `204 (-), 404 (-)` |

## Microservicio: DonaTrack - Incentivos Service API (`openapi-incentivos.yaml`)

* **Versión:** `1.0.0`
* **Descripción:** API REST para gamificación, misiones por Template Method, asignación de insignias y cómputo de ranking mensual de donantes.
* **Servidor Local:** `http://localhost:8082`

| Método | Path | Operación | Request Body | Códigos de Respuesta |
|:---:|---|---|---|---|
| `GET` | `/api/incentivos/ranking/ultimo` | `obtenerUltimoRanking` | `-` | `200 (RankingMensualDTO), 204 (-)` |
| `GET` | `/api/incentivos/ranking/historial` | `obtenerHistorialRankings` | `-` | `200 (RankingMensualDTO)` |
| `POST` | `/api/incentivos/ranking/calcular` | `calcularRanking` | `-` | `200 (RankingMensualDTO)` |
| `GET` | `/api/incentivos/ranking/posicion/{donanteId}` | `obtenerPosicionRanking` | `-` | `200 (-), 204 (-)` |
| `GET` | `/api/incentivos/ranking/{periodo}` | `obtenerRankingPorPeriodo` | `-` | `200 (RankingMensualDTO), 400 (-), 404 (-)` |
| `POST` | `/api/incentivos/donantes/{donanteId}` | `registrarDonanteIncentivos` | `RegistrarDonanteRequest` | `201 (DonanteRegistradoDTO), 400 (-)` |
| `DELETE` | `/api/incentivos/donantes/{donanteId}` | `darDeBajaDonanteIncentivos` | `-` | `204 (-), 404 (-)` |
| `PATCH` | `/api/incentivos/donantes/{donanteId}` | `modificarDonanteIncentivos` | `ModificarDonanteRequest` | `200 (-), 400 (-), 404 (-)` |
| `GET` | `/api/incentivos/donantes/{donanteId}` | `obtenerDonantePerfil` | `-` | `200 (DonantePerfilDTO), 404 (-)` |
| `GET` | `/api/incentivos/donantes/{donanteId}/ascensos` | `obtenerAscensosDonante` | `-` | `200 (CambioCategoriaDTO), 404 (-)` |
| `GET` | `/api/incentivos/donantes/{donanteId}/metricas` | `obtenerMetricasDonante` | `-` | `200 (-), 404 (-)` |
| `GET` | `/api/incentivos/admin/resumen` | `obtenerResumenSistema` | `-` | `200 (-)` |
| `GET` | `/api/incentivos/donantes/{donanteId}/misiones` | `obtenerMisionesDonante` | `-` | `200 (MisionDTO), 404 (-)` |
| `GET` | `/api/incentivos/donantes/{donanteId}/insignias` | `obtenerInsigniasDonante` | `-` | `200 (InsigniaDTO)` |
| `PUT` | `/api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad` | `configurarVisibilidadInsignia` | `-` | `200 (-)` |
| `POST` | `/api/incentivos/donaciones` | `procesarNuevaDonacion` | `NuevaDonacionRequest` | `200 (-)` |
| `POST` | `/api/incentivos/donaciones/exitosa` | `procesarDonacionExitosa` | `DonacionExitosaRequest` | `200 (-)` |
| `POST` | `/api/incentivos/evaluaciones-inactividad` | `ejecutarEvaluacionInactividad` | `-` | `200 (-)` |
| `POST` | `/api/incentivos/verificaciones-racha` | `ejecutarVerificacionRachas` | `-` | `200 (-)` |

## Microservicio: DonaTrack - Logística Service API (`openapi-logistica.yaml`)

* **Versión:** `1.0.0`
* **Descripción:** API REST para planificación de rutas de transporte, seguimiento de camiones, asignación de choferes y trazabilidad de entregas.
* **Servidor Local:** `http://localhost:8083`

| Método | Path | Operación | Request Body | Códigos de Respuesta |
|:---:|---|---|---|---|
| `POST` | `/api/entregas` | `crearEntrega` | `CrearEntregaRequestDTO` | `201 (EntregaResponseDTO), 400 (-)` |
| `GET` | `/api/entregas` | `listarEntregas` | `-` | `200 (EntregaResponseDTO)` |
| `GET` | `/api/entregas/{id}` | `obtenerEntregaPorId` | `-` | `200 (EntregaResponseDTO), 404 (-)` |
| `PATCH` | `/api/entregas/{id}/estado` | `cambiarEstadoEntrega` | `CambioEstadoEntregaRequestDTO` | `200 (EntregaResponseDTO), 400 (-)` |
| `PATCH` | `/api/entregas/{id}/fotos` | `adjuntarFotoRecepcion` | `-` | `200 (EntregaResponseDTO)` |
| `GET` | `/api/entregas/{id}/historial` | `obtenerHistorialEntrega` | `-` | `200 (CambioEstadoEntregaResponseDTO), 404 (-)` |
| `GET` | `/api/rutas` | `listarRutas` | `-` | `200 (-)` |
| `GET` | `/api/rutas/{id}` | `obtenerRutaPorId` | `-` | `200 (-)` |
| `GET` | `/api/rutas/{id}/entregas` | `obtenerRutaConEntregas` | `-` | `200 (-)` |
| `POST` | `/api/rutas/{id}/entregas` | `agregarEntregaRuta` | `-` | `201 (-)` |
| `PATCH` | `/api/rutas/{id}/estado` | `cambiarEstadoRuta` | `-` | `200 (-)` |
| `POST` | `/api/camiones` | `registrarCamion` | `-` | `201 (-)` |
| `GET` | `/api/camiones` | `listarCamiones` | `-` | `200 (-)` |
| `GET` | `/api/camiones/{id}` | `obtenerCamionPorId` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/camiones/{id}` | `darDeBajaCamion` | `-` | `204 (-), 404 (-)` |
| `PATCH` | `/api/camiones/{id}/estado` | `cambiarEstadoCamion` | `-` | `200 (-), 400 (-), 404 (-)` |
| `POST` | `/api/choferes` | `registrarChofer` | `-` | `201 (-)` |
| `GET` | `/api/choferes` | `listarChoferes` | `-` | `200 (-)` |
| `GET` | `/api/choferes/{id}` | `obtenerChoferPorId` | `-` | `200 (-), 404 (-)` |
| `DELETE` | `/api/choferes/{id}` | `darDeBajaChofer` | `-` | `204 (-), 404 (-)` |
| `PATCH` | `/api/choferes/{id}/estado` | `cambiarEstadoChofer` | `-` | `200 (-), 400 (-), 404 (-)` |
| `POST` | `/api/logistica/planificaciones/ejecuciones` | `iniciarPlanificacionManual` | `-` | `202 (-)` |
| `GET` | `/api/logistica/planificaciones/{id}` | `obtenerSolicitudPlanificacion` | `-` | `200 (-)` |
| `POST` | `/api/logistica/resultados` | `procesarCallbackPlanificacion` | `-` | `200 (-)` |
| `POST` | `/api/logistica/callback/rutas` | `procesarCallbackRutasAlias` | `-` | `200 (-)` |

## Microservicio: DonaTrack - Notificaciones Service API (`openapi-notificaciones.yaml`)

* **Versión:** `1.0.0`
* **Descripción:** API REST para despacho de alertas multicanal (Email, WhatsApp, SMS), sincronización idempotente de personas replicadas y gestión de historial de notificaciones.
* **Servidor Local:** `http://localhost:8081`

| Método | Path | Operación | Request Body | Códigos de Respuesta |
|:---:|---|---|---|---|
| `POST` | `/notificaciones` | `procesarEventoNotificable` | `EventoNotificableDTO` | `202 (-), 400 (-)` |
| `GET` | `/notificaciones/persona/{personaId}` | `obtenerNotificacionesPorPersona` | `-` | `200 (NotificacionDTO)` |
| `PUT` | `/api/notificaciones/personas` | `sincronizarPersona` | `PersonaReplicaDTO` | `200 (-), 400 (-)` |
| `GET` | `/api/notificaciones/personas/{id}` | `obtenerPersona` | `-` | `200 (PersonaReplicaDTO), 404 (-)` |
| `DELETE` | `/api/notificaciones/personas/{id}` | `anonimizarPersona` | `-` | `204 (-)` |

---
*Generado mecánicamente por DonaTrack Knowledge Engine.*
