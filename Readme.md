# DonaTrack

DonaTrack es el proyecto anual de la cátedra de Diseño de Sistemas de Información (2026). El sistema tiene como objetivo optimizar la gestión, trazabilidad y distribución de donaciones de bienes materiales para organizaciones sin fines de lucro, buscando mejorar la transparencia y eficiencia en la asistencia a entidades beneficiarias.

## Contexto del Proyecto

El sistema está concebido bajo una arquitectura distribuida de microservicios, permitiendo un crecimiento incremental, modular y escalable.
La plataforma permite la gestión integral de donantes (humanos y jurídicos), la recepción y asignación de donaciones, el registro de necesidades de entidades beneficiarias y la logística de entrega de bienes.

## Documentación y Recursos

- **[Hub de Documentación](https://tsorren.github.io/DonaTrack-TP-DDS/)**: Página principal del proyecto que actúa como índice consolidado de documentación, incluyendo la indexación dinámica de las entregas (PDFs).
- **[ADR Preview](https://tsorren.github.io/DonaTrack-TP-DDS/adr-preview)**: Visualización interactiva de los Registros de Decisiones de Arquitectura (ADRs) publicada en GitHub Pages.
- **[Documentador](https://tsorren.github.io/DonaTrack-TP-DDS/documentador)**: Documentador de los Registros de Decisiones de Arquitectura (ADRs) publicada en GitHub Pages.

## Tecnologías y Herramientas

El desarrollo de DonaTrack se fundamenta en un ecosistema robusto y moderno, alineado con estándares de la industria:

### Desarrollo y Backend
- Lenguaje: Java 21 (enfocado en las últimas funcionalidades del lenguaje).
- Framework: Spring Boot (base para el desarrollo de microservicios).
- Gestión de dependencias: Maven.
- Simplificación de código: Lombok (reducción de boilerplate).

### Calidad y Testing
- Análisis de código: SonarQube.
- Cobertura de tests: JaCoCo.
- Framework de Testing: JUnit 5 y Mockito.
- Formateo y estilo: Spotless (aplicado mediante Git Hooks para garantizar determinismo).

### DevOps y Gestión
- Control de versiones y flujo: GitHub (Actions para CI/CD, Issues para seguimiento, Projects para gestión ágil).
- Documentación de arquitectura: LucidChart y ADRs (Architecture Decision Records) visualizados con Log4brains.
- IDE: IntelliJ IDEA.
## Estructura del Proyecto

El proyecto sigue una arquitectura de microservicios. A continuación se detalla la organización de los directorios principales:

### Raíz del Proyecto
- `.github/`: Flujos de trabajo de CI/CD (GitHub Actions) y plantillas para Issues.
- `.log4brains/`: Configuración para la visualización de los Registros de Decisiones de Arquitectura (ADR).
- `docs/`: Documentación técnica, diagramas de arquitectura y archivos ADR en formato Markdown.
  - `docs/hub/`: Código fuente de la página central del ecosistema desplegado en GitHub Pages.
  - `docs/entregas/`: Directorio destinado al almacenamiento de documentos (PDFs) de entregas académicas, indexado automáticamente para su visualización.
- `documentador/`: Herramienta interna para la generación estandarizada de nuevas decisiones de arquitectura.

### Microservicios
Cada servicio contiene su propia lógica de dominio siguiendo los principios de Clean Architecture:
- `auth-service/`: Gestión de autenticación y seguridad.
- `donaciones-service/`: Microservicio principal de gestión de donantes, donaciones y necesidades de entidades beneficiarias.
- `notificaciones-service/`: Orquestación y envío de eventos y notificaciones (WhatsApp, Email, etc.).
- `incentivos-service/`: Cálculo de analíticas y recompensas para donantes.
- `logistica-service/`: Gestión de rutas de entrega y seguimiento de bienes.

### Componentes Compartidos y Utilidades
- `common-lib/`: Dependencia compartida que incluirá frameworks de logging, tracing, etc.
- `cliente-liviano/`: Renderizado de vistas desde el servidor.


## Decisiones de Diseño Clave

Para garantizar la calidad y mantenibilidad, hemos adoptado las siguientes decisiones estratégicas:

- Pipeline CI/CD: Implementación de un flujo unificado basado en el enfoque de "Fallo Temprano", automatizando validaciones de calidad, Git Flow y despliegue de documentación.
- Privacidad: Anonimización de usuarios para cumplir con las normativas de protección de datos sensibles.
- Comunicación Asincrónica: Implementación de mensajería basada en eventos para desacoplar los servicios, mejorando la escalabilidad y tolerancia a fallos.
- Pruebas Unitarias: Uso intensivo de JUnit y Mockito, garantizando el aislamiento y determinismo en los tests de lógica de negocio.


## Grupo 5 - Integrantes:
|                **Nombre**                | **Legajo** |           **Correo**          |
|:----------------------------------------:|:----------:|:-----------------------------:|
|            Camila Aylen Suarez           |  214.170-0 |    casuarez@frba.utn.edu.ar   |
|           Camila Belén Lencina           |  215.042-6 |   calencina@frba.utn.edu.ar   |
|            Lara Anush Eriakian           |  213.678-8 |   leriakian@frba.utn.edu.ar   |
|          Martín Ilán Zajdenberg          |  209.501-4 |  mzajdenberg@frba.utn.edu.ar  |
| Milton Christopher Bernardo Estigarribia |  208.731-5 | mestigarribia@frba.utn.edu.ar |
|               Miranda Rossi              |  208.818-6 |    mirossi@frba.utn.edu.ar    |
|              Nicolás Delorte             |  172.817-9 |    ndelorte@frba.utn.edu.ar   |
|             Sofia Maria Deane            |  210.350-3 |     sdeane@frba.utn.edu.ar    |
|             Tadeo Sorrentino             |  214.153-0 |  tsorrentino@frba.utn.edu.ar  |
|            Valentina Thiwissen           |  209.210-4 |   vthiwissen@frba.utn.edu.ar  |
