# Cambios - Etapa 1: Correcciones y mejoras en `notificaciones-service`

Se implementaron las siguientes modificaciones para solucionar el bug de aislamiento de repositorios y permitir la consulta de réplicas y notificaciones para validación en las pruebas cross-service:

1. **Fusión de Repositorios**:
   - Se eliminó el archivo redundante `PersonasRepositoryEnMemoria.java`.
   - Se modificó `PersonaRepositoryEnMemoria.java` para que implemente tanto `PersonaRepository` como `IPersonasRepository`. Esto unifica el almacenamiento en memoria en un único bean singleton compartido.
   - Se corrigió la instanciación del repositorio en la clase de prueba unitaria `PersonasRepositoryTest.java`.

2. **Endpoints de Depuración y Consulta (Query)**:
   - Se declaró e implementó el método `obtenerPersona` en `IPersonasController.java`, `PersonasController.java`, `IPersonasService.java` y `PersonasService.java` para permitir obtener réplicas de personas mediante `GET /api/notificaciones/personas/{id}`.
   - Se creó el DTO `NotificacionDTO.java` para serializar de forma segura los datos de las notificaciones.
   - Se declaró e implementó el método `obtenerPorPersona` en `NotificacionController.java` y `NotificacionService.java` para consultar todas las notificaciones de una persona usando `GET /notificaciones/persona/{personaId}`.
