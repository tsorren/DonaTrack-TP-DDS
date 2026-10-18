# Invariantes de Domain Events y Snapshot Inmutable en AgregadoConEventos

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: ddd, domain-events, concurrencia, common-lib, agregados

## Contexto y Problema

En Domain-Driven Design (DDD), los agregados registran eventos de dominio cuando ocurre una mutación significativa de negocio (ej: `EntregaConfirmada`, `NotificacionCreada`, `InsigniaGanadaEvent`). Originalmente, los métodos de lectura de eventos como `getDomainEvents()` retornaban vistas de solo lectura basadas en `Collections.unmodifiableList(this.domainEvents)` sobre la lista interna mutable del agregado. Durante la ejecución de flujos reactivos o reentrantes (donde un listener despacha un evento que a su vez invoca una acción secundaria sobre el mismo agregado), la mutación concurrente de la lista mientras se iteraba producía excepciones catastróficas `ConcurrentModificationException`. Se requiere estandarizar una abstracción base segura para el ciclo de vida de eventos en todas las raíces de agregado.

## Atributos de Calidad y Drivers de Decisión

* **Robustez y Seguridad Concurrente:** Garantizar que la publicación de eventos no sea vulnerable a modificaciones reentrantes o carreras de hilos.
* **Encapsulación DDD:** Asegurar que ninguna capa externa pueda alterar la colección de eventos sin pasar por métodos de negocio.
* **Mantenibilidad y Reutilización:** Proveer una clase base homogénea en `common-lib` que elimine código duplicado en los cuatro microservicios.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 2 (Domain Events) y Oleada 11 (Sincronización con `common-lib`) del Plan Genérico v2; Catálogo de Errores Recurrentes §6.
* **Hallazgo:** En `incentivos-service` y `notificaciones-service`, se constató que `Collections.unmodifiableList` solo previene mutaciones directas a través de la referencia devuelta, pero no protege si el método de despacho o un listener dispara una nueva mutación interna sobre el agregado mientras se recorre la lista. La solución obligatoria fue cambiar a snapshots inmutables desconectados vía `List.copyOf(this.domainEvents)` y crear tests canónicos de reentrancia.

## Alternativas Consideradas

* **Snapshot Inmutable Desconectado (`List.copyOf`) en `AgregadoConEventos<T>`:** Implementar la clase base abstracta `AgregadoConEventos<T extends EventoDeDominio>` en `common-lib`. `getDomainEvents()` retorna una copia inmutable congelada (`List.copyOf`), permitiendo que el agregado siga mutando o limpiándose sin afectar la iteración en curso.
* **Vista No Modificable (`Collections.unmodifiableList`):** Retornar una vista directa sobre la colección interna en memoria.
* **Publicación Inmediata con ApplicationEventPublisher en Entidades:** Inyectar el bus de eventos de Spring directamente en las entidades de dominio para despachar al instante.

## Resultado de la Decisión

Alternativa elegida: "Snapshot Inmutable Desconectado (`List.copyOf`) en `AgregadoConEventos<T>`"

Justificación:
El snapshot inmutable desacopla completamente el momento de lectura del momento de mutación. Cumple el principio fundamental de que el dominio es un POJO puro (sin dependencias del bus de Spring) que acumula eventos, mientras que el Application Service ejecuta el ciclo determinista: (1) método de dominio, (2) persistencia `repository.save()`, (3) `getDomainEvents()` (snapshot), (4) publicación al bus, y (5) `clearDomainEvents()`.

### Consecuencias Positivas

* Inmunidad total frente a `ConcurrentModificationException` por reentrancia o listeners anidados.
* Coherencia arquitectónica absoluta en los 4 servicios al heredar de `AgregadoConEventos`.
* El agregado permanece puro, testeable con JUnit 5 aislado y sin anotaciones de framework.

### Consecuencias Negativas

* Ligera asignación de memoria adicional al crear una lista inmutable en cada invocación de `getDomainEvents()` (despreciable frente a la seguridad transaccional).

### Validación

Se valida mediante:
1. Tests unitarios canónicos de reentrancia en cada módulo (ej. `NotificacionTest`, `EntregaTest`), verificando que mutar el agregado mientras se itera sobre `getDomainEvents()` no lance excepción.
2. `grep -rn "Collections.unmodifiableList(this.domainEvents)"` produce 0 resultados en todo el repositorio.

## Análisis de Alternativas

### Snapshot Inmutable Desconectado (`List.copyOf`)

#### Pros
* Seguridad contra reentrancia garantizada por el runtime de Java.
* Semántica inmutable innegociable.

#### Contras
* Copia superficial en memoria de las referencias a los eventos.

### Vista No Modificable (`Collections.unmodifiableList`)

#### Pros
* Cero asignación de nueva memoria de lista.

#### Contras
* Fragilidad extrema ante efectos secundarios concurrentes o listeners encadenados.

### Publicación Inmediata con Spring Publisher

#### Pros
* No requiere acumular eventos en una lista temporal.

#### Contras
* Contamina el dominio con dependencias de Spring (`@Autowired` o `ApplicationEventPublisher`).
* Si la transacción de base de datos hace rollback, el evento ya se despachó prematuramente (*phantom event*).