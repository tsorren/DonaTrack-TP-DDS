# Evento de Dominio DonacionVencida y Alerta Administrativa

- Status: accepted
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: notificaciones, eventos, donaciones, administrador, rest

## Contexto y Problema

En el flujo de vida de una donación, si los bienes no son asignados o retirados dentro de los plazos reglamentarios de acopio, la donación pasa al estado vencida. Se requiere que el servicio de notificaciones reciba este evento a través de su API REST y genere una alerta formal dirigida a la persona administradora del sistema para habilitar la toma de decisiones operativas (descarte, donación forzosa o auditoría).

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Extensibilidad (OCP):** Incorporar el nuevo evento como un subtipo polimórfico sin alterar endpoints existentes ni violar contratos previos.
* **Cohesión y Simplicidad (SRP / KISS):** El evento no involucra a un beneficiario; por lo tanto, no debe forzar la dependencia de `EventoDeDonacion`.
* **Robustez y Testeabilidad:** Validación exhaustiva en frontera (Bean Validation) y en dominio (guardas no nulas ni en blanco).

## Alternativas Consideradas

### 1. Jerarquía de Dominio
* **Alternativa A (Elegida):** `DonacionVencida` extiende directamente de `EventoNotificable`. Mantiene referencias al donante, la administración, el detalle de la donación, el motivo y la fecha, despachando una única notificación al administrador.
* **Alternativa B:** Forzar `DonacionVencida` a extender de `EventoDeDonacion`. Descartada porque `EventoDeDonacion` impone una `entidadBeneficiaria` obligatoria y notifica en tándem a donante y beneficiario, lo cual viola el principio de sustitución de Liskov (LSP) para un vencimiento en depósito.

### 2. Contrato de Entrada REST
* **Alternativa A (Elegida):** Extender `EventoNotificableDTO` mediante `@JsonSubTypes.Type(value = EventoDonacionVencidaDTO.class, name = "DONACION_VENCIDA")` sobre `POST /notificaciones`.
* **Alternativa B:** Crear un endpoint dedicado `POST /notificaciones/donaciones-vencidas`. Descartada por fragmentar innecesariamente la API REST del microservicio.

## Resultado de la Decisión

Se aprueba la implementación de `DonacionVencida` extendiendo `EventoNotificable` y su DTO `EventoDonacionVencidaDTO` sobre el endpoint `POST /notificaciones` con respuesta `HTTP 202 Accepted`.

### Consecuencias Positivas
* El administrador recibe una alerta contextualizada: `"Atención administrador: La donación de {detalleDonacion} del donante {donante} ha vencido. Motivo: {motivo}."`.
* Coherencia total con el diseño de eventos notificables preexistente.
* Bean Validation estricta (`@NotNull`, `@NotBlank`, `@PastOrPresent`) previniendo datos inválidos en frontera.

### Consecuencias Negativas / Deuda Asociada
* Ninguna identificada.
