**Respuestas a Consultas sobre Arquitectura y Git Flow**

*Documento de Consolidación Conceptual — Proyecto Anual DonaTrack (Grupo 5\)*

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Este documento proporciona respuestas estructuradas y detalladas a las consultas planteadas por el equipo de desarrollo en relación a los conceptos de diseño, controladores REST, uso de DTOs, comunicación entre servicios, visualización en diagramas de arquitectura y el flujo de integración continua (CI/CD) basado en Git Flow y GitHub Actions dentro del contexto del proyecto anual **DonaTrack**.

**1\. Creación de Controllers, Services y Repositories (DDD)**

**Consulta planteada:** *¿Para qué entidades creamos service, controller y repository? ¿Es cuando nos interesa la persistencia o qué criterios tengo que tener en cuenta?*

En una arquitectura limpia orientada a Domain-Driven Design (DDD), no se crean controladores, servicios y repositorios para cada entidad o tabla del sistema. El criterio rector no es simplemente la persistencia de datos, sino la delimitación de los límites de consistencia de los agregados.

**El concepto de Agregado y Raíz de Agregado (Aggregate Root)**

Un Agregado (Aggregate) es un conjunto de objetos de dominio (entidades internas y objetos de valor) que se tratan como una sola unidad para los cambios de estado. Cada agregado tiene una única entidad externa visible llamada Raíz del Agregado (Aggregate Root). Los objetos externos solo pueden interactuar con el agregado a través de su raíz, la cual tiene un identificador global (por ejemplo, un UUID).

**Criterios de creación de capas:**

* **Repositories (Persistencia):** Únicamente creamos interfaces Repository para las Raíces de Agregado (Aggregate Roots). Las entidades internas del agregado (ej. ItemDonacion) se persisten y se recuperan de forma indirecta, a través del repositorio de su raíz (DonacionRepository). Esto garantiza que el agregado se almacene y cargue como una estructura atómica, respetando sus invariantes de negocio.  
* **Services (Reglas de Negocio):** Los servicios orquestan los casos de uso principales de la aplicación y manejan las transacciones de base de datos. Se definen a nivel de Aggregate Roots o a nivel de servicios de dominio cuando se coordinan múltiples agregados. Las entidades internas no tienen servicios propios; toda la lógica de negocio se procesa a través del servicio de la raíz.  
* **Controllers (Entrada/Salida API REST):** Exponen recursos públicos de cara a los clientes externos. Se crean controladores únicamente para los agregados que representan recursos REST principales. La interacción con las entidades internas del agregado se realiza enviando las propiedades a los endpoints de la raíz.

| ★ Ejemplo Práctico en DonaTrack: *En donaciones-service, Donacion es la Raíz de Agregado e ItemDonacion es una entidad interna. Existe DonacionRepository, DonacionService y DonacionController. No existe ItemDonacionRepository ni ItemDonacionController. Un ítem de donación solo se crea o modifica consumiendo la API de Donación, lo que permite validar los límites del lote completo en una única transacción de base de datos.* |
| :---- |

**2\. Implementación Práctica de una API REST**

**Consulta planteada:** *Entendí la teoría respecto a API Rest, pero no mucho cómo se implementa.*

La implementación práctica de una API REST en Spring Boot consiste en mapear recursos lógicos a direcciones URL (URIs) y usar los verbos del protocolo HTTP para denotar la acción a realizar sobre dichos recursos.

**Mapeo en Spring Boot:**

* **@RestController y @RequestMapping:** La clase controladora se define con @RestController para automatizar la conversión de respuestas a JSON. La anotación @RequestMapping("/api/donaciones") define la ruta base para acceder al recurso.  
* **Verbo GET (Lectura):** Mapeado con @GetMapping para listar todos los registros, o @GetMapping("/{id}") para buscar un recurso específico. Usa @PathVariable UUID id para mapear dinámicamente la variable de la URL.  
* **Verbo POST (Creación):** Mapeado con @PostMapping. Recibe los datos de creación en el cuerpo del request mediante @RequestBody. Debe validar la entrada con @Valid y retornar el código de estado HTTP 201 (Created), devolviendo el objeto creado y la cabecera 'Location'.  
* **Verbo PUT (Modificación):** Mapeado con @PutMapping("/{id}") para reemplazar por completo el estado del recurso con el nuevo payload enviado.  
* **Verbo DELETE (Eliminación):** Mapeado con @DeleteMapping("/{id}") para borrar físicamente de la base de datos o realizar una baja lógica (cambio de estado a inactivo). Retorna comúnmente 204 No Content.

Para responder de manera formal y flexible, los métodos del controlador retornan instancias de ResponseEntity\<T\>. Esta clase representa la respuesta HTTP completa y permite setear dinámicamente códigos de estado (HttpStatus.CREATED, HttpStatus.NOT\_FOUND, etc.) y cabeceras personalizadas. Los errores de validación y de negocio se interceptan de manera unificada mediante un manejador global de excepciones (@RestControllerAdvice).

**3\. Criterios de Uso de Data Transfer Objects (DTOs)**

**Consulta planteada:** *¿Cuándo necesito DTOs?*

Un DTO (Data Transfer Object) es un objeto contenedor diseñado para transferir datos entre diferentes capas del sistema, o a través de la red entre cliente y servidor. No posee lógica de negocio ni comportamiento, únicamente atributos y sus métodos de acceso.

**Razones fundamentales para utilizar DTOs:**

* **Desacoplamiento de la base de datos:** Las entidades JPA (@Entity) están acopladas a la estructura de las tablas de base de datos. Si se retornan de forma directa, cualquier refactorización o cambio en el esquema SQL rompería la API del cliente de forma inmediata. Los DTOs actúan como un contrato estable e independiente.  
* **Seguridad y Privacidad:** Las entidades del modelo de negocio contienen datos confidenciales o de control interno (ej. contraseñas hasheadas, claves de auditoría interna como modificadoPor, o tokens). El DTO permite filtrar selectivamente los campos que serán visibles por el cliente.  
* **Optimización de red:** Una pantalla de listado de la UI solo necesita mostrar el ID, fecha y monto de una donación, mientras que la vista de detalle requiere 30 campos del donante y los bienes. Usar DTOs específicos (DonacionResumenDTO y DonacionDetalleDTO) optimiza el ancho de banda transmitiendo únicamente lo necesario.  
* **Evitar ciclos de serialización JSON:** Las relaciones bidireccionales de JPA (ej. @OneToMany y @ManyToOne) generan referencias circulares. Al serializar la entidad directamente a JSON, el serializador entra en un bucle infinito lanzando una excepción StackOverflowError. Los DTOs rompen este ciclo al estructurar la información de manera plana.  
* **Validación limpia en la capa HTTP:** Los DTOs sirven para declarar reglas de validación en la capa de entrada (ej: @NotBlank, @Min, @Size) sin ensuciar las clases de dominio con anotaciones de frameworks de serialización y validación HTTP.

**4\. Comunicación entre Microservicios (Sincrónica vs. Asincrónica)**

**Consulta planteada:** *No terminé de entender cómo implementar la comunicación entre servicios.*

En una arquitectura distribuida de microservicios como la de DonaTrack, los servicios residen en procesos independientes y se comunican de dos formas principales según el requerimiento de negocio:

**Tabla Comparativa de Comunicación:**

| Aspecto | Comunicación Sincrónica | Comunicación Asincrónica | Ejemplo en el Proyecto |
| :---- | :---- | :---- | :---- |
| **Mecanismo** | Llamadas HTTP directas (Request-Response) usando REST. | Publicación de eventos en un Broker de Mensajería (Colas). | FeignClient vs RabbitMQ. |
| **Acoplamiento** | Alto. El emisor requiere que el receptor esté activo en línea. | Bajo. Los servicios se desacoplan; pueden procesar de forma diferida. | donaciones-service no depende de notificaciones. |
| **Bloqueo** | Bloqueante. El hilo del emisor espera el retorno HTTP. | No Bloqueante. El emisor publica el evento y continúa de inmediato. | La API retorna 201 sin esperar el correo al donante. |
| **Tolerancia** | Baja. Si el receptor falla, toda la transacción falla. | Alta. Si el receptor cae, los mensajes quedan guardados en la cola. | Notificaciones se envían cuando el servicio se levante. |

**Implementación Práctica:**

En esta entrega usaremos comunicación sincronica directa con retries para manejo de errores. Para llamadas sincrónicas, se declara una interfaz Java anotada con @FeignClient en Spring Cloud; Spring Boot se encarga de implementar los detalles del cliente HTTP en tiempo de ejecución. 

**5\. Componentes de la Arquitectura de Microservicios**

**Consulta planteada:** *Tampoco terminé de entender cómo se definen los componentes en el diagrama (los servicios y gateway sí, me perdí un poco con el resto)*

Algunos componentes importantes (Para esta entrega solo tendremos Servicios en nuestro diagrama de componentes)

* **API Gateway:** Punto de acceso único para todos los clientes. Enruta el tráfico, maneja CORS, gestiona la seguridad inicial, aplica políticas de limitación de tasa (rate limiting) y balancea la carga entre las instancias de los servicios.  
* **Bases de Datos por Servicio:** Cada microservicio tiene su base de datos aislada (PostgreSQL, MongoDB, etc.). Un servicio no puede acceder directamente a los datos de otro servicio. Esto garantiza que las modificaciones internas en un esquema no rompan a los demás componentes. Por ahora nos piden un schema por servicio, simulando multiples bases de datos en el mismo motor.  
* **Message Broker / Event Bus:** Componente encargado de enrutar los mensajes asíncronos (ej: RabbitMQ) entre productores (quienes emiten eventos) y consumidores (quienes leen y ejecutan lógica en base a ellos).  
* **Service Discovery / Registry:** Directorio donde los microservicios se registran dinámicamente al levantarse ( Eureka o Consul). El API Gateway y los clientes consultan al Service Discovery para resolver la dirección IP y el puerto físico al que deben enviar la petición HTTP.  
* **Config Server:** Servidor centralizado que almacena las propiedades y variables de entorno de todos los microservicios, permitiendo gestionarlas desde un repositorio común de Git sin tener que recompilar cada microservicio para cambiar una propiedad.  
* **Cliente Liviano (Frontend):** Componente de interfaz gráfica que se ejecuta en el navegador web del usuario y se comunica exclusivamente con el API Gateway para consumir la lógica del sistema.  
* **Librería Compartida (common-lib):** No es un servicio que se ejecute en el servidor, sino un artefacto JAR compilado que es importado como dependencia local. Contiene código común, manejadores globales de excepciones y utilidades de auditoría/logging compartidas.

**6\. Diagnóstico de Fallas en GitHub Actions**

**Consulta planteada:** *Me confunden un poco las github actions, más que nada porque estaba viendo q varios checks fallaron.*

Las GitHub Actions del repositorio DonaTrack ejecutan un pipeline de validación automática basado en la política de 'Fallo Temprano'. Los motivos por los cuales los checks fallan y bloquean la integración son los siguientes:

* **validate-git-flow (Formato de Ramas):** El pipeline valida por expresión regular el nombre de la rama. Por ejemplo, si intentas hacer una PR hacia la rama ENTREGA\_N desde una rama que no empiece con el prefijo E{N}\_ (ej. E1\_registro-donaciones), el check fallará.  
* **lint-and-format (Estilo de Código con Spotless):** El compilador Maven valida el formato del código ejecutando 'mvn spotless:check'. Si dejaste espacios adicionales, importaciones sin usar o un orden de llaves incorrecto, el check fallará para mantener la consistencia visual.

| ★ Cómo corregir fallos de Spotless: *Si falla el check lint-and-format, abre una terminal en la raíz de tu proyecto local y ejecuta el comando:  mvn spotless:applyEste comando reformatea automáticamente los archivos Java. Luego haz commit y push de las correcciones.* |
| :---- |

* **build-and-test (Pruebas y Compilación):** Este check fallará si hay un error de sintaxis en Java que impida la compilación o si cualquiera de las pruebas unitarias (JUnit 5 y Mockito) falla al ser ejecutada.  
* **sonar-analysis (Quality Gate de SonarCloud):** Este paso analiza la calidad general. Fallará si se detectan fallos de seguridad (vulnerabilidades), bugs críticos, o si la cobertura de código calculada por JaCoCo no llega al porcentaje mínimo exigido.

* **semantic-diff (Herramienta propia):** Genera diagramas de clases de las dos branch de la PR (base y target) y compara semanticamente los diagramas para detectar creación, eliminación y modificacion de entidades y relaciones. Facilita la revisión de PRs al representar graficamente los cambios, también calcula un nivel estimado de complejidad de los cambios con una fórmula determinística.

**7\. Flujo de Trabajo (Git Flow) y Stacked PRs**

**Consulta planteada:** *Tampoco tengo claro como es el git flow.*

El Git Flow en DonaTrack está adaptado al contexto académico y diseñado para optimizar las revisiones. En lugar de subir código directamente o abrir Pull Requests gigantescas antes de cada entrega, se trabaja bajo un esquema de PRs Apiladas (Stacked PRs):

* **Ramas de Entrega (ENTREGA\_N):** Son las ramas base oficiales para cada hito académico. La rama 'main' representa la estabilidad histórica acumulada.  
* **Creación de requerimientos y tareas:** Usar plantillas para generar las Issues complejas, luego asignarlas, crear las ramas, crear Drafts de Pull Requests y linkear a la issue. Las PRs deben contener la menor cantidad de cambios posibles (separar en sub-issues) para facilitar la revisión.  
* **Desarrollo en Cascada:** Cada programador trabaja en su rama de tarea correspondiente. Por ejemplo, el desarrollador de task2 trabaja basándose en los cambios de la task1. Cuando finalizas tu tarea, cambias la PR de borrador a lista para revisión (ready\_for\_review).  
* **Revisión Secuencial y Asignación Automática:** El sistema valida que no haya tareas previas en borrador. Luego, asigna un revisor de forma balanceada (Round-Robin) basándose en la menor carga de revisiones e inactividad, notificándole por Discord. Una vez aprobada y fusionada la PR de task1, se habilita la revisión de task2 y así en adelante hasta consolidarse en la rama base del requerimiento (Por ahora solo está habilitado el notificador automático, la revisión secuencial todavía requiere mejoras)

