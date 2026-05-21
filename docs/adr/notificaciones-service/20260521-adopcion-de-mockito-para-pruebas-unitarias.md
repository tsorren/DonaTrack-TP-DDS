# Adopción de Mockito para Pruebas Unitarias
- Status: proposed
- Date: 2026-05-21
- Deciders: Decisión Grupal
- Tags: testing

## Contexto y Problema
Tras refactorizar el componente de notificaciones para utilizar Double Dispatch y aislar la infraestructura en el `NotificacionRouter`, necesitamos validar exhaustivamente la lógica core del dominio. Esta lógica incluye: la correcta priorización del medio de contacto marcado como "predeterminado", el mecanismo de tolerancia a fallos (continuar con el siguiente medio si el primero lanza una excepción o retorna `false`) y la transición de estados de la `Notificacion` (`PENDIENTE`, `ENVIADA`, `FALLIDA`). Ejecutar estas pruebas usando adaptadores reales enviaría correos y mensajes de texto genuinos, lo cual es lento, costoso e impredecible para un entorno de Integración Continua (CI).

## Atributos de Calidad y Drivers de Decisión
* Testeabilidad
* Fiabilidad

## Alternativas Consideradas
* Creación Manual de Clases Stub/Fake
* Adopción del Framework Mockito

## Resultado de la Decisión

Alternativa elegida: "Adopción del Framework Mockito"

Justificación:
Mockito permite generar simulacros (mocks) de las interfaces de infraestructura (`CorreoAdapter`, `TelefonoAdapter`, `WhatsAppAdapter` o directamente de `NotificacionSender`) de manera dinámica. Con esto, podemos programar el comportamiento esperado de las APIs externas de forma 100% aislada y en memoria (por ejemplo, forzar que el correo falle para probar que el sistema hace fallback al teléfono).

### Consecuencias Positivas
* Permite validar la lógica orquestadora del dominio en milisegundos sin latencia de red.
* Ahorra la creación y mantenimiento de múltiples clases "Fake" o "Mock" manuales en el código de testing, reduciendo el boilerplate.
* Facilita la aserción de interacciones (ej. verificar que `enviarMail` se llamó exactamente una vez) garantizando que el Double Dispatch está ruteando correctamente.

### Consecuencias Negativas
* Añade una nueva dependencia externa al `pom.xml` del proyecto.
* Las pruebas que abusan de los mocks pueden acoplarse demasiado a la implementación técnica en lugar de probar el comportamiento, volviéndose frágiles ante refactorizaciones menores.

## Análisis de Alternativas

### Creación Manual de Clases Stub/Fake

Crear clases como `FakeCorreoAdapter` que implementan las interfaces y guardan el estado en variables internas para luego realizar asserts.

#### Pros
* No requiere añadir dependencias externas al proyecto.
* Los Fakes suelen ser muy explícitos y fáciles de leer sin necesitar un framework de mocking.

#### Contras
* Genera mucho código innecesario / boilerplate destinado únicamente a testing.
* Difícil de mantener si las interfaces de los adaptadores cambian frecuentemente.

### Adopción del Framework Mockito

Utilizar el framework Mockito para crear objetos simulados y programar su comportamiento mediante anotaciones (`@Mock`, `@InjectMocks`) y métodos estáticos (`when`, `verify`).

#### Pros
* El código es altamente declarativo, conciso y expresivo.
* Permite simular escenarios complejos fácilmente, como lanzar excepciones (`thenThrow`) en el primer intento y éxito en el segundo, validando los bloques `try/catch` del dominio.

#### Contras
* Tiene una curva de aprendizaje inicial respecto a su sintaxis y al ciclo de vida de los mocks frente a los contextos de JUnit.

## Links
* [Contexto - NotificacionRouter](../20260521-desacoplamiento-de-medios-de-contacto-mediante-double-dispatch-y-router)
