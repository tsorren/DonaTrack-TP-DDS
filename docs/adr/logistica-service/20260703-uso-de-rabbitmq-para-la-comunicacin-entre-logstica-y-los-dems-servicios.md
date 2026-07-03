# Uso de RabbitMQ para la comunicación entre Logística y los demás servicios
- Status: proposed
- Date: 2026-07-03
- Deciders: Decisión Grupal

## Contexto y Problema
La consigna establece que el servicio de Logística no debe invocar directamente a Donaciones ni a Incentivos, sino dejar disponible la información para que otros servicios reaccionen. Además, Logística no se comunica con Notificaciones, pero sus eventos deben disparar notificaciones igualmente, por lo que algún otro servicio debe actuar como puente. Se necesita definir un mecanismo de comunicación entre servicios que respete este desacoplamiento.

## Alternativas Consideradas
* Polling (Donaciones consulta periódicamente a Logística)
* Patrón Outbox
* Colas asincrónicas con Message Broker (RabbitMQ)

## Resultado de la Decisión

Alternativa elegida: "Colas asincrónicas con Message Broker (RabbitMQ)"

Justificación:
La restricción de que Logística no invoque a otros servicios describe naturalmente el modelo productor/consumidor de un message broker: el emisor publica sin saber quién escucha, y los receptores reaccionan de forma independiente. RabbitMQ permite además reacción casi en tiempo real, tolerancia a caídas de los consumidores (los mensajes se acumulan en la cola) y escalabilidad, ya que agregar nuevos consumidores no requiere modificar Logística.

## Análisis de Alternativas

### Polling (Donaciones consulta periódicamente a Logística)

Donaciones llama a un endpoint de Logística cada cierto tiempo para preguntar si hubo cambios de estado.

#### Pros
* Simple de implementar, sin infraestructura extra
* Logística solo expone REST, no necesita saber nada de nadie

#### Contras
* Introduce latencia: los cambios no se reflejan de inmediato
* Genera tráfico innecesario aunque no haya cambios
* Escala mal si varios servicios necesitan enterarse

### Patrón Outbox

Logística persiste los eventos en un registro propio (almacenamiento local de eventos_logistica). Donaciones tiene un proceso programado (@Scheduled) que consulta ese registro y procesa los eventos nuevos.

#### Pros
* Logística no conoce a nadie: solo escribe en su propio almacenamiento local
* Garantiza que el evento no se pierde aunque el sistema falle
* Compatible con @Scheduled ya usado en Entrega 2

#### Contras
* Sigue siendo un esquema de polling, con latencia asociada
* Requiere un proceso adicional que lea y procese el registro
* Acoplamiento implícito a través del esquema de almacenamiento

### Colas asincrónicas con Message Broker (RabbitMQ)

Logística publica eventos en un broker de mensajería. Los servicios interesados (Donaciones, potencialmente Incentivos) se suscriben a esos eventos y reaccionan de forma independiente.

#### Pros
* Desacoplamiento total: Logística no conoce a ningún consumidor
* Reacción en tiempo casi real, sin latencia de polling
* Si un servicio está caído, los mensajes se acumulan en la cola y se procesan cuando vuelve
* Escala fácilmente: agregar nuevos consumidores no requiere modificar Logística

#### Contras
* Requiere un contenedor adicional (RabbitMQ) en Docker Compose
* Mayor complejidad operativa y de debugging
