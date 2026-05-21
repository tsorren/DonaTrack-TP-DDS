# Stack de Tecnologías para el Desarrollo y Colaboración
- Status: proposed
- Date: 2026-04-26
- Deciders: Decisión Grupal
- Tags: stack-tecnologico, infraestructura, colaboracion, desarrollo

## Contexto y Problema
El desarrollo de DonaTrack involucra la coordinación de un equipo de 10 integrantes que deben trabajar de manera distribuida y asíncrona tanto en el diseño de arquitectura (diagramas UML, gestión de requerimientos, minutas) como en la construcción del software (microservicios, APIs, persistencia). Es imperativo formalizar y estandarizar un conjunto de herramientas de comunicación, documentación, diseño y desarrollo para evitar la dispersión de información, garantizar la compatibilidad del entorno de ejecución y maximizar la velocidad de entrega del equipo.

## Atributos de Calidad y Drivers de Decisión
* Mantenibilidad

## Alternativas Consideradas
* Ecosistema Integrado de Colaboración y de Desarrollo Java

## Resultado de la Decisión

Alternativa elegida: "Ecosistema Integrado de Colaboración y de Desarrollo Java"

Justificación:
Esta suite de tecnologías cubre de forma holística todas las fases del ciclo de vida del proyecto. En la dimensión organizacional y de diseño, las herramientas en la nube de Google Workspace (Docs, Sheets para gestión de roles, Drive compartido y Forms para encuestas), junto con Lucidchart, centralizan los artefactos gráficos y documentales con edición colaborativa en tiempo real. En la dimensión técnica, la combinación de Java 21 y Spring Boot proporciona una base robusta e industrial para microservicios, optimizada mediante Maven para la gestión de dependencias, IntelliJ IDEA como IDE unificado, Lombok para la eliminación de código boilerplate, Spotless para el formateo determinista y GitHub para el control de versiones y automatización de la integración.

### Consecuencias Positivas
* Centraliza toda la documentación, entregables y minutas en un Google Drive común, garantizando una única fuente de verdad accesible para todo el equipo.
* Estandariza el entorno de desarrollo y ejecución en Java JDK 21 y Spring Boot, permitiendo aprovechar características modernas del lenguaje (como registros y pattern matching) y un framework maduro para la lógica de negocio.
* Segmenta eficazmente la comunicación del equipo: Discord actúa como el espacio formal para sesiones de diseño, desarrollo y pizarras técnicas, mientras que WhatsApp se reserva para alertas inmediatas, avisos de bloqueo y coordinación logística de alta prioridad.
* Automatiza la uniformidad estética del código a través del plugin de Spotless en Maven y reduce drásticamente el código repetitivo de accesores/constructores mediante anotaciones de Lombok.

### Consecuencias Negativas
* Lucidchart presenta limitaciones en su plan gratuito en cuanto al número de objetos por documento, lo que podría obligar a segmentar los diagramas UML del modelo de dominio si la complejidad aumenta significativamente.
* Requiere que todo el equipo configure correctamente los plugins de Lombok y las reglas de Spotless en sus entornos locales de IntelliJ IDEA para evitar conflictos de compilación o fallos en los Git Hooks previos al commit.

## Análisis de Alternativas

### Ecosistema Integrado de Colaboración y de Desarrollo Java

Consiste en la adopción obligatoria de un conjunto unificado de aplicaciones comerciales, de código abierto y frameworks para estandarizar la comunicación del equipo, la co-creación de diagramas arquitectónicos y la construcción del backend bajo especificaciones técnicas homogéneas (Google Drive, Discord, WhatsApp, Lucidchart, Java 21, Spring Boot, Maven, IntelliJ, Lombok, Spotless, GitHub).

#### Pros
* La edición simultánea en la nube de Google Docs, Sheets y Lucidchart elimina la fricción de fusionar versiones de archivos de diseño manualmente.
* Spring Boot y Maven están fuertemente alineados con el marco académico y las expectativas de la cátedra de Diseño de Sistemas.
* GitHub centraliza de forma nativa la gestión de issues, el código fuente y las revisiones de diseño en un único entorno interconectado.
* Lombok simplifica la legibilidad de las clases de dominio al remover la necesidad física de escribir getters, setters, equals y hashcode.

## Links
[Lucidchart](https://lucid.app/lucidchart/daddd34f-aa51-4efa-b7e1-d5695e2d38b8/edit?invitationId=inv_23e12ccc-973c-4430-8fb3-e38d3b9faa8f&page=0_0#)