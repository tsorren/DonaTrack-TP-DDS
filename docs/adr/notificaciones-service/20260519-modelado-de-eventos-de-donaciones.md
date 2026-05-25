# Modelado de Eventos de Donaciones

- Status: proposed
- Date: 2026-05-19
- Deciders: Decisión Grupal
- Tags: servicio-notificaciones

## Contexto y Problema

Debido a que la asignación de una donación a una entidad beneficiaria y la recepción de una donación notifica al donante
y a la entidad beneficiaria con mensajes distintos en base al mismo evento, se debe encontrar una manera de modelar este
comportamiento complejo.

## Atributos de Calidad y Drivers de Decisión

* Flexibilidad
* Extensibilidad

## Alternativas Consideradas

* Separar Evento en EventoDonante y EventoBeneficiario
* Clase Abstracta EventoDeDonación con TemplateMethod

## Resultado de la Decisión

Alternativa elegida: "Clase Abstracta EventoDeDonación con TemplateMethod"

Justificación:
Facilita la implementación de la lógica de negocio requerida, permitiendo extender el modelo a nuevas notificaciones de
multiples destinatarios.

### Consecuencias Positivas

* Obtenemos mayor flexibilidad en el diseño para generar distintos tipos de notificaciones y se facilita la
  implementación de eventos notificables futuros

### Consecuencias Negativas

* Se dificultará más la persistencia de estas clases ya que en los modelos relacionales no existe una forma nativa de
  representar las clases abstractas

### Validación

Se realizarán los tests unitarios correspondientes, verificando que se generen las notificaciones con sus mensajes y
destinatarios correspondientes al evento en cuestión.

## Análisis de Alternativas

### Separar Evento en EventoDonante y EventoBeneficiario

Se modelarían los eventos con un solo destinatario, al recibir una DonacionAsignada se crearian los distintos eventos
asociados a los receptores

#### Pros

* Facilita la generación de las notificaciones

#### Contras

* El modelado no representa que parten del mismo evento común

### Clase Abstracta EventoDeDonación con TemplateMethod

Se modelarían los eventos en base a una clase abstracta con metodos abstractos para generar los mensajes de donante y
beneficiario

#### Pros

* Reduce la repetición de código al compartir los atributos de la entidad beneficiaria y la donación
* Genera listas de notificaciones que corresponden al mismo evento

#### Contras

* Complejiza la persistencia al usar una clase abstracta

## Links

* [Eventos Notificables](../20260519-eventos-como-disparadores-de-notificaciones)