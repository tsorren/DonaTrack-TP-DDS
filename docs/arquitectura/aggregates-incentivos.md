# Modelo de Agregados — Servicio de Incentivos (DDD)

Este documento detalla el diseño táctico de **Domain-Driven Design (DDD)** para el **Servicio de Incentivos** en DonaTrack, basado en el diagrama de clases del motor de gamificación y rankings.

---

## 1. Principios de Diseño del Servicio

El *Servicio de Incentivos* tiene como objetivo gamificar la participación de los colaboradores mediante misiones, insignias y categorías, además de generar tableros de clasificación mensuales. Sus principios de diseño son:
1.  **Independencia de Negocio**: Aunque los incentivos dependen de las donaciones realizadas, este servicio no interfiere en las transacciones de entrega. Escucha eventos de integración (`DonacionEntregadaEvent`) de forma asincrónica para actualizar el progreso del donante en segundo plano.
2.  **Encapsulación de Misiones**: El progreso de cada misión (`Mision`) es de incumbencia exclusiva de cada donante. Nadie fuera del agregado de gamificación del donante puede consultar o modificar misiones activas de manera directa.
3.  **Consistencia Eventual del Perfil**: Al ascender de categoría o completar misiones, se publican eventos para sincronizar los logros del donante de forma asíncrona con otros servicios.

---

## 2. Catálogo Detallado de Agregados

### 2.1. Agregado: DonanteIncentivos (Gamificación del Donante)
*   **Aggregate Root**: `DonanteIncentivos`.
*   **Componentes Internos (Entidades y Objetos de Valor)**:
    *   `Mision` (Clase abstracta polimórfica en `grupo5.incentivos.models.entities.misiones` y sus subclases `MisionHabilDonador`, `MisionRacha`, `MisionCompletitud`, `MisionDonacionesExitosas` basadas en **Template Method**). Representan misiones con un objetivo, progreso actual y estado de completitud.
    *   `Insignia` (Objeto de Valor/Entidad en `grupo5.incentivos.models.entities.insignias` asignada al donante).
    *   `CategoriaDonante` (Enum: *COLABORADOR*, *SOSTENEDOR*, *TRANSFORMADOR*).
    *   `CambioCategoria` (Auditoría inmutable de ascensos/descensos).
*   **Referencias Externas (por ID)**: 
    *   `donanteId` (`UUID` que actúa como la clave primaria del agregado, apuntando al Donante originado en el *Servicio de Donaciones*).
*   **Responsabilidad**: Centralizar y validar las reglas de gamificación del donante. Procesa el impacto de nuevas donaciones en el progreso de las misiones y determina si el donante califica para un ascenso de categoría o para recibir insignias adicionales.
*   **Paquete**: `grupo5.incentivos.models.entities.donante` y `entities.misiones`

### 2.2. Agregado: Insignia (Catálogo de Logros)
*   **Aggregate Root**: `Insignia`.
*   **Atributos**: `id` (UUID), `nombre`, `descripcion`, `urlImagen`, `fechaCreacion`.
*   **Responsabilidad**: Gestionar el catálogo disponible de reconocimientos y medallas del sistema mediante `InsigniaRepository`.
*   **Paquete**: `grupo5.incentivos.models.entities.insignias`

### 2.3. Agregado: RankingMensual (Tablero de Líderes)
*   **Aggregate Root**: `RankingMensual`.
*   **Componentes Internos (Entidades y Objetos de Valor)**:
    *   `EntradaRanking` (Entidad interna que describe la fila del podio: posición, nombre de donante y cantidad de misiones completadas).
*   **Referencias Externas (por ID)**: 
    *   `donanteId` (`UUID` de la entrada que referencia a `DonanteIncentivos`).
*   **Responsabilidad**: Registrar el podio de posiciones de los donantes más activos para un período mensual (`YearMonth`) determinado de manera inmutable e histórica.
*   **Paquete**: `grupo5.incentivos.models.entities.ranking`

---

## 3. Clases de Lógica y Transitorias (No son Agregados)

*   **`EventoDonacion`**: Objeto de dominio transitorio (`donacionId`, `organizacionId`, `subcategoria`, `cantidadBienes`, `exitosa`, `fecha`) utilizado para alimentar el procesamiento de misiones en `DonanteIncentivos`.
*   **`InactividadJob` / `RachaJob` / `RankingMensualJob`**: Cron jobs programados (`@Scheduled`) en `grupo5.incentivos.infrastructure.schedulers` para el cálculo automático de rachas, inactividad y ranking.
*   **`MisionFactory`**: Implementación del patrón *Factory* encargado de la creación inicial de misiones del catálogo estándar para los nuevos donantes.
