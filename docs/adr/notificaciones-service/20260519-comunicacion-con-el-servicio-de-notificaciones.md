# Comunicación con el Servicio de Notificaciones
- Status: accepted
- Date: 2026-06-12
- Deciders: Decisión Grupal
- Tags: servicio-notificaciones

## Contexto y Problema
Debemos decidir cómo se van a comunicar los distintos servicios con el Servicio de Notificaciones, ya que este generará
mensajes a los usuarios en base a los distintos eventos del sistema.

## Atributos de Calidad y Drivers de Decisión
* Flexibilidad
* Escalabilidad
* Tolerancia a Fallos

## Alternativas Consideradas
* Comunicación Sincrónica
* Comunicación Asincrónica

## Resultado de la Decisión

Alternativa elegida: "Comunicación Sincrónica"

Justificación:
Por el momento no vemos necesario adoptar una arquitectura que soporte la comunicación asincrónica. Se diseñará una integración flexible que permita modificar el modo de comunicación en el futuro.

## Análisis de Alternativas

### Comunicación Sincrónica

Los distintos servicios comunicarán directamente los eventos de forma push-based mediante la exposición de una API en elServicio de Notificaciones.

#### Pros
* Baja complejidad arquitectónica

#### Contras
* Los demás servicios deberán conocer los endpoints de este, aumentando el acoplamiento entre ellos.

### Comunicación Asincrónica

Cada servicio escuchará y enviará los eventos que le sean relevantes en base a una cola de mensajería. Sin necesidad deconocer que se hará con esa información ni quien lo hará.

#### Pros
* Reduce el acoplamiento entre servicios
* Permite escalar horizontalmente los servicios sin tener que modificar las conexiones de comunicación
* Con una buena implementación reduciría la latencia del sistema, ya que los servicios no se bloquearían entre ellos
* No se perderán los eventos que se generen si el Servidor de Notificaciones está caído, ya que cuando se reinicie leerá

#### Contras
* Puede ser un Single Point of Failure en el sistema.
* Si no se maneja correctamente traería mayor congestión.
* La implementación es más compleja.

## Links
* [Eventos Notificables](../20260519-eventos-como-disparadores-de-notificaciones)
