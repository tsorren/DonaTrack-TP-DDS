# Granularidad de los Eventos de Logística: Notificación por Donación en vez de por Ruta Completa
- Status: accepted
- Date: 2026-07-02
- Deciders: Decisión Grupal

## Contexto y Problema
La consigna de la Entrega 3 pide que, al iniciar el recorrido, "se notificará a todas las entidades beneficiarias y a los donantes cuyas entregas formen parte de la ruta iniciada por el chofer". Una ruta de un camión puede incluir múltiples donaciones, cada una con su propio donante y su propia entidad beneficiaria. Debemos decidir si el evento DonacionEnCamino (y de forma análoga EntregaFallida) representa la ruta completa de un camión (con una lista de destinatarios) o una única donación dentro de esa ruta, delegando en quien dispara el evento (Servicio de Donaciones) la responsabilidad de emitir un evento por cada donación transportada.

## Alternativas Consideradas
* Evento por donación individual

## Resultado de la Decisión

Alternativa elegida: "Evento por donación individual"

Justificación:
Es la alternativa que mejor se ajusta al modelo ya construido en la Entrega 2 (EventoDeDonacion, con un único persona donante y una única entidadBeneficiaria), evitando introducir una segunda forma de modelar eventos solo para este caso. El servicio de Donaciones —que es quien conoce la relación entre la ruta y las donaciones que contiene— es responsable de iterar sobre las donaciones de la ruta y emitir un EventoDonacionEnCaminoDTO (o EventoEntregaFallidaDTO) por cada una. notificaciones-service no necesita conocer el concepto de "ruta" en absoluto, lo cual es coherente con el requerimiento de la propia Entrega 3 de que el servicio de Logística no se comunique con el de Notificaciones: es Donaciones quien arma y envía estos eventos.

### Consecuencias Positivas
* Se reutiliza sin modificaciones la jerarquía EventoDeDonacion existente desde la Entrega 2.
* Si una notificación de una donación de la ruta falla, no afecta el procesamiento de las demás.

### Consecuencias Negativas
* Si una ruta incluye muchas donaciones, se generan muchas llamadas HTTP individuales.
