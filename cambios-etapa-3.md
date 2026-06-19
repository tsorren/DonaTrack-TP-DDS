# Cambios - Etapa 3: Correcciones en `incentivos-service`

Se implementaron las siguientes modificaciones para solucionar problemas de inconsistencia al enviar notificaciones asíncronas de donantes:

1. **Corrección de ID en Eventos de Misión e Historial**:
   - Se modificó `IncentivosService.java` para pasar `donante.getIdPersona()` (Persona ID) en lugar de `donante.getId()` (Donor ID) al invocar `notificacionesClient.notificarMisionCumplida` y `notificacionesClient.notificarAscensoCategoria`.

2. **Corrección de ID en Chequeo de Inactividad**:
   - Se modificó `InactividadJob.java` para pasar `donante.getIdPersona()` (Persona ID) en lugar de `donante.getId()` (Donor ID) al disparar la notificación `notificacionesClient.notificarInactividad`.

Esto asegura que `notificaciones-service` logre encontrar la persona asociada al evento y no falle con una excepción por no encontrar la entidad.
