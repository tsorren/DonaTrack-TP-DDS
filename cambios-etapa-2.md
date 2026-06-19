# Cambios - Etapa 2: Correcciones en `donaciones-service`

Se implementaron las siguientes modificaciones para solucionar problemas de inconsistencia en los IDs de donantes y asegurar la persistencia correcta del estado al realizar asignaciones y fragmentaciones:

1. **Corrección de IDs de Donante**:
   - Se modificó `ProcesadorDeDonaciones.java` para pasar el Donor ID (`donante.getId()`) en lugar del Persona ID (`persona.getId()`) en el payload de `NuevaDonacionRequest`.
   - Se modificó `DonacionesIndependientesService.java` para obtener y enviar el Donor ID en lugar de Persona ID al notificar una donación exitosa a `incentivos-service` (`procesarDonacionExitosa`).
   - Se actualizó la clase de prueba unitaria `DonacionesIndependientesServiceTest.java` para verificar la invocación de `procesarDonacionExitosa` con el ID del Donante.

2. **Persistencia de Fragmentaciones y Necesidades**:
   - Se modificó `AlgoritmosService.java` en la confirmación de propuestas (`APROBADA`) para persistir:
     - La necesidad que se satisface.
     - Las donaciones independientes originales (que pueden haber visto reducida su cantidad debido a una fragmentación).
     - Todas las donaciones independientes asignadas (incluyendo fragmentos recién creados e instancias completas).

3. **Datos Iniciales de Catálogo (Subcategorías)**:
   - Se creó `CatalogDataInitializer.java` que implementa `CommandLineRunner`. Este componente pre-popula el catálogo de categorías ("Alimentos", "Ropa", "Muebles") y sus respectivas subcategorías y alias en el repositorio en memoria al iniciar la aplicación.
