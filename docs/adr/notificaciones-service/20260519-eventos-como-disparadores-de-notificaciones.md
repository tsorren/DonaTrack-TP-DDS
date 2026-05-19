# Eventos como disparadores de notificaciones
- Status: proposed
- Date: 2026-05-19
- Deciders: 
- Tags: servicio-notificaciones

## Contexto y Problema

Debido a que las notificaciones tienen mensajes distintos pero siguen cierta estructura, debemos modelar una solución que permita la creación polimórfica de estas notificaciones.
Los eventos notificables son:
El registro de un donante
El cumplimiento de una misión de un donante
La subida de categoría de un donante
La asignación de una donación a una entidad beneficiaria y la recepción de una donación (se notifica al donante y a la entidad beneficiaria en ambos casos)

## Atributos de Calidad y Drivers de Decisión
* Flexibilidad
* Extensibilidad

## Alternativas Consideradas
* Interfaz con metodo generarMensajes
* Clase Abstracta EventoNotificable

## Resultado de la Decisión

Alternativa elegida: "Clase Abstracta EventoNotificable", porque: Permite gestionar las notificaciones con mayor facilidad, evita errores de la lógica de negocio al asociar los mensajes con los destinatarios.

La clase abstracta EventoNotificable es extendida por las clases DonanteRegistrado, MisionCumplida, SubioCategoria, y la clase abstracta EventoDeDonacion(Ver decisión de diseño asociada).

### Consecuencias
Bueno, porque:

* Obtenemos mayor flexibilidad en el diseño para generar distintos tipos de notificaciones y se facilita la implementación de eventos notificables futuros

Malo, porque: 

* Se dificultará más la persistencia de estas clases ya que en los modelos relacionales no existe una forma nativa de representar las clases abstractas

### Validación

Se realizarán los tests unitarios correspondientes, verificando que se generen las notificaciones con sus mensajes y destinatarios correspondientes al evento en cuestión.

## Pros y Contras de las Alternativas

### Interfaz con metodo generarMensajes

Se modelarían los eventos en base a una interfaz común con el método generarMensajes(): List<String>

Bueno, porque: 

* Fácil de modelar
* Bajo acoplamiento

Malo, porque:

* No asocia el mensaje al destinatario
* Dificulta el envio de la notificación

### Clase Abstracta EventoNotificable

Se modelarían los eventos en base a una clase abstracta con el método generarNotificaciones(): List<Notificacion>

Bueno, porque: 

* Reduce la repetición de código al compartir los atributos persona y fecha
* Genera notificaciones completas en vez de mensajes, permitiendo vincular a los destinatarios con su respectivo mensaje

Malo, porque: 

* Complejiza la persistencia al usar una clase abstracta

## Links

