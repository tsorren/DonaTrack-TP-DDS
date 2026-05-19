# Privacidad de Usuarios
- Status: proposed
- Date: 2026-05-19
- Deciders: 
- Tags: privacidad

## Contexto y Problema

Los datos de las personas (Jurídicas o Humanas) deben ser tratados según la ley de protección de datos, por lo que se deberá permitir a los usuarios eliminar aquella información sensible que el sistema haya almacenado si desean borrar su cuenta.

La ley establece el Derecho de Supresión (Art. 16). Si un usuario solicita borrar su información, el sistema debe seguir este procedimiento:

* Plazo de respuesta: El responsable del banco de datos tiene un máximo de 5 días hábiles para realizar la supresión desde que recibió el reclamo o advirtió el error.
* Gratuidad: El trámite de eliminación debe ser totalmente gratuito para el interesado (Art. 19).
* Notificación a terceros: Si esos datos fueron cedidos o transferidos anteriormente a otra empresa o entidad, el responsable debe avisarles de la supresión en un plazo de 5 días hábiles para que ellos también los borren (Art. 16.4).
* Bloqueo preventivo: Mientras se verifica si corresponde o no la supresión, el sistema debe "bloquear" el archivo o anotar que la información está bajo revisión (Art. 16.6).

[Fuente](https://servicios.infoleg.gob.ar/infolegInternet/anexos/60000-64999/64790/texact.htm)

## Atributos de Calidad y Drivers de Decisión
* Privacidad
* Extensibilidad

## Alternativas Consideradas
* Eliminar las instancias asociadas a la persona
* Desacoplar datos sensibles de entidades de dominio creando nuevas clases
* Definir interfaz Anonimizable para las clases con informacion sensible

## Resultado de la Decisión

Alternativa elegida: "Definir interfaz Anonimizable para las clases con informacion sensible", porque: Por ahora utilizaremos esta alternativa debido a la baja complejidad en la implementación, pero se deberá analizar nuevamente la decisión para la Entrega 2.

### Consecuencias
Bueno, porque: 

* Permitimos anonimizar a los usuarios que lo soliciten.

Malo, porque:

* La solución no parece muy confiable y podría traer problemas a futuro si alguna implementación de la interfaz se pasa por alto la anonimización todos los atributos sensibles.

### Validación

Se deberá testear con rigurosidad cada implementación de esta interfaz para no infringir las leyes de protección de datos. Luego esta decisión deberá de ser evaluada antes de la Entrega 2 ya que puede traer problemas a futuro la alternativa elegida.

## Pros y Contras de las Alternativas

### Eliminar las instancias asociadas a la persona

Se borrarían las instancias como Persona, sus referencias en las demás entidades

Bueno, porque: 

* No afectaría el modelado actual

Malo, porque: 

* Perderiamos el registro historico de aquellos eventos que realizó la persona en el sistema
* Podría traer problemas de integridad al eliminar FKs

### Desacoplar datos sensibles de entidades de dominio creando nuevas clases

La entidad persona tendría una relación de asociación con una entidad informacionSensible que podría ser eliminada, lo mismo para las demás clases con datos sensibles.

Bueno, porque: 

* Nos permitiría eliminar las entidades de la agregación sin perder la referencia a las entidades de dominio.

Malo, porque: 

* El modelado sería difícil de realizar correctamente ya que las clases con información sensible presentan herencias con más información sensible

### Definir interfaz Anonimizable para las clases con informacion sensible

Se crearía una interfaz con el método anonimizar que implementarían las clases, la cual reemplazaría los datos sensibles por valores NULL, "ANONIMIZADO", o algo parecido

Bueno, porque:

* La implementación es simple, ya que no requiere separar las entidades de dominio en nuevas clases
* Cada clase definiría como anonimizarse,

Malo, porque: 

* La implementar la interfaz anonimizable no aseguraría que se eliminen todos los datos sensibles ya que es un método void.
* El correcto funcionamiento depende completamente de la implementación propia a cada clase, lo cual disminuye la confiabilidad de esta alternativa.

## Links

