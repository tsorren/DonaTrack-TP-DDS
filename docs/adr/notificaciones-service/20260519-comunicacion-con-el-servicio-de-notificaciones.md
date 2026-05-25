# Comunicación con el Servicio de Notificaciones

- Status: draft
- Date: 2026-05-19
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

Alternativa elegida: "Comunicación Asincrónica"

Justificación:
Valoramos la escalabilidad del sistema y la flexibilidad que nos aporta este tipo de comunicación, permitiendo
desacoplar los servicios y definir claramente las responsabilidades de cada uno.

### Consecuencias Positivas

* Facilitará escalar el sistema horizontalmente y reducirá la complejidad cuando tengamos mayor cantidad de eventos.

### Consecuencias Negativas

* Puede traer problemas a futuro si no consideramos maneras de garantizar la disponibilidad de este componente
  intermedio para gestionar eventos.

### Validación

Se deberá desplegar en un entorno no productivo los distintos servicios y realizar tests de integración. También es
crucial el uso de Trace IDs que identifiquen el caso de uso específico a lo largo de los servicios para tener
trazabilidad en los logs.

## Análisis de Alternativas

### Comunicación Sincrónica

Los distintos servicios comunicarán directamente los eventos de forma push-based mediante la exposición de una API en el
Servicio de Notificaciones.

#### Pros

* Baja complejidad arquitectónica

#### Contras

* Los demás servicios deberán conocer los endpoints de este, aumentando el acoplamiento entre ellos.

### Comunicación Asincrónica

Cada servicio escuchará y enviará los eventos que le sean relevantes en base a una cola de mensajería. Sin necesidad de
conocer que se hará con esa información ni quien lo hará.

#### Pros

* Reduce el acoplamiento entre servicios
* Permite escalar horizontalmente los servicios sin tener que modificar las conexiones de comunicación
* Con una buena implementación reduciría la latencia del sistema, ya que los servicios no se bloquearían entre ellos
  esperando respuestas.
* No se perderán los eventos que se generen si el Servidor de Notificaciones está caído, ya que cuando se reinicie leerá
  la cola de eventos.

#### Contras

* Puede ser un Single Point of Failure en el sistema.
* Si no se maneja correctamente traería mayor congestión.
* La implementación es más compleja.

## Links

* [Eventos Notificables](../20260519-eventos-como-disparadores-de-notificaciones)