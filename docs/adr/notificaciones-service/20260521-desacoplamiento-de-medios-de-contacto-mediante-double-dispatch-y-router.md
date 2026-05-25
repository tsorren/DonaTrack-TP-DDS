# Desacoplamiento de Medios de Contacto mediante Double Dispatch y Router

- Status: proposed
- Date: 2026-05-21
- Deciders: Decisión Grupal

## Contexto y Problema

El modelo de dominio de notificaciones requiere iterar sobre los distintos medios de contacto de un usuario (Correo,
Teléfono, WhatsApp), priorizar el que está marcado como predeterminado e intentar enviar el mensaje con un mecanismo de
fallback (si un medio falla, se intenta con el siguiente). Sin embargo, el dominio no debe conocer los detalles de
implementación de las APIs externas (SendGrid, Twilio, etc.) para realizar el envío físico, ya que esto violaría el
principio de Inversión de Dependencias y acoplaría la lógica de negocio a la infraestructura de red. Debemos encontrar
una forma de ejecutar la lógica de envío específica para cada tipo de medio sin utilizar estructuras condicionales
frágiles (como `instanceof`).

## Atributos de Calidad y Drivers de Decisión

* Extensibilidad
* Mantenibilidad

## Alternativas Consideradas

* Uso de Condicionales (`instanceof`) en capa Service
* Patrón Double Dispatch y Facade en Capa de Infraestructura

## Resultado de la Decisión

Alternativa elegida: "Patrón Double Dispatch y Facade en Capa de Infraestructura"

Justificación:
Aplicamos el patrón Double Dispatch haciendo que la entidad `Notificacion` pase una interfaz `NotificacionSender` a cada
`MedioDeContacto`. Cada medio concreto (ej. `Correo`) se pasa a sí mismo de vuelta al sender (
`sender.enviarA(this, mensaje)`). La implementación de este sender, el `NotificacionRouter`, reside exclusivamente en la
capa de infraestructura. Este router actúa como un pasamanos (Facade) que recibe el medio tipado y delega la ejecución
al adaptador externo correspondiente (`CorreoAdapter`, `TelefonoAdapter`, etc.). Esto mantiene el dominio completamente
puro y delega el "cómo se envía" a la infraestructura.

### Consecuencias Positivas

* El modelo de dominio queda completamente aislado de las bibliotecas de envío de mensajes y detalles de red.
* Se elimina la necesidad de usar `instanceof` o casteos para saber qué adaptador usar.
* Facilita enormemente el testing de la lógica de negocio (priorización y manejo de estados) inyectando un
  `NotificacionSender` falso o mockeado.

### Consecuencias Negativas

* El patrón Double Dispatch puede ser un poco contraintuitivo, ya que el flujo de ejecución "rebota" entre el dominio y
  el router.
* Si se agrega un nuevo medio de contacto (ej. `Telegram`), se debe modificar la interfaz `NotificacionSender` y su
  implementación en el router, lo que rompe ligeramente el principio Open/Closed a nivel de la interfaz.

## Análisis de Alternativas

### Uso de Condicionales (`instanceof`) en capa Service

Un servicio iteraría sobre los medios y usaría `if (medio instanceof Correo)` para llamar al adapter correspondiente.

#### Pros

* Es fácil de entender a simple vista.
* Menos interfaces y clases intermedias.

#### Contras

* Rompe fuertemente el principio Open/Closed; cada vez que se agrega un medio, hay que modificar el servicio.
* Acopla el servicio de aplicación a todas las implementaciones concretas de los medios y a todos los adaptadores
  simultáneamente.

### Patrón Double Dispatch y Facade en Capa de Infraestructura

El dominio expone la interfaz `NotificacionSender` con sobrecarga de métodos, y la infraestructura la implementa
mediante un Router que conoce a los adaptadores finales.

#### Pros

* Respeto absoluto por el encapsulamiento y el polimorfismo.
* La lógica de negocio (priorización, fallback y cambio de estados) queda contenida en la entidad `Notificacion` de
  forma cohesiva.

#### Contras

* Mayor cantidad de clases e interfaces involucradas.
