# Medios de Contacto

- Status: proposed
- Date: 2026-05-20
- Deciders: Decisión Grupal

## Contexto y Problema

Debemos permitir que los usuarios reciban notificaciones a través de los medios de contacto que especifiquen (correo,
teléfono o whatsapp), seleccionando cuál es su medio predeterminado.

## Atributos de Calidad y Drivers de Decisión

* Extensibilidad

## Alternativas Consideradas

* Enum MedioDeContacto
* Clase abstracta MedioDeContacto

## Resultado de la Decisión

Alternativa elegida: "Clase abstracta MedioDeContacto"

Justificación:
Esta alternativa aplica correctamente los principios SOLID

## Análisis de Alternativas

### Enum MedioDeContacto

El medio de contacto se modelaría como un enum con los valores CORREO, TELÉFONO y WHATSAPP, definiendo esos atributos
para las personas los cuales podrían ser nulleables

#### Contras

* Los datos están desacoplados de la lógica
* Utilizar un Enum es poco flexible
* El requerimiento parece requerir mayores abstracciones que esta

### Clase abstracta MedioDeContacto

Se define la clase abstracta MedioDeContacto con el atributo esPredeterminado: Boolean y el método abstracto void
enviarMensaje con los parametros de tipo String y NotificacionSender (interfaz con el contrato enviarMensaje(String):
void). Las clases Correo y Teléfono heredan de MedioDeContacto, definiendo sus atributos especificos para implementar la
lógica de enviarMensaje, luego la clase Whatsapp hereda de Teléfono reemplazando únicamente el método para enviar
mensajes.

#### Pros

* Modela correctamente la abstracción de los medios de contacto, permitiendo utilizarlos de manera polimórfica
* Delega la responsabilidad de enviar el mensaje a las clases que implementen la interfaz NotificacionSender (tendrán la
  lógica de manejo de APIs)
* Permite extender facilmente el dominio a nuevos medios de contacto en el futuro si se requiere

#### Contras

* Si la lógica de negocio solo permite tener un medio de contacto predeterminado, se deberá validar desde la persona que
  no haya un estado invalido en los medios de contacto, ya que no se conocen entre si para determinar si son el único
  medio de contacto predeterminado
