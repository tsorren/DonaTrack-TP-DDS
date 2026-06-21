# DonaTrack

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-build-blue?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-blue?style=flat-square&logo=docker)](https://www.docker.com/)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-quality_gate-green?style=flat-square&logo=sonarcloud)](https://sonarcloud.io/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-blue?style=flat-square&logo=githubactions)](https://github.com/features/actions)
[![n8n](https://img.shields.io/badge/n8n-workflow-red?style=flat-square&logo=n8n)](https://n8n.io/)

Plataforma distribuida para la trazabilidad integral, asignación inteligente de recursos y fidelización en la gestión de donaciones para organizaciones sin fines de lucro.

---

## Índice

* [El Desafío y la Motivación](#1-el-desafío-y-la-motivación)
* [Arquitectura de Sistemas y Componentes](#2-arquitectura-de-sistemas-y-componentes)
* [Estructura del Proyecto](#3-estructura-del-proyecto)
* [Ecosistema Tecnológico](#4-ecosistema-tecnológico)
* [Capacidades y Diseño de Dominio (Hitos de Entrega)](#5-capacidades-y-diseño-de-dominio-hitos-de-entrega)
* [Ingeniería de Infraestructura, Calidad y CI/CD](#6-ingeniería-de-infraestructura-calidad-y-cicd)
* [Configuración y Automatización de Entornos](#7-configuración-y-automatización-de-entornos)
* [Recursos y Documentación Anexa](#8-recursos-y-documentación-anexa)
* [Equipo de Ingeniería](#9-equipo-de-ingeniería)

---

## 1. El Desafío y la Motivación

La gestión eficiente de donaciones de bienes materiales en organizaciones sin fines de lucro enfrenta tres desafíos operacionales críticos:
1. **Transparencia y Trazabilidad:** Garantizar la procedencia, el estado físico y el destino final de cada bien para asegurar auditorías limpias y generar confianza en los donantes.
2. **Optimización de Recursos (Matchmaking):** Conectar de forma inteligente el stock de donaciones disponibles con las necesidades reales (recurrentes o de emergencia) de las entidades beneficiarias, evitando el desperdicio y la sobre-atención de ciertos sectores en detrimento de otros.
3. **Fidelización y Participación (Donor Engagement):** Incentivar a los colaboradores mediante métricas de impacto claras, misiones secuenciales y reconocimiento público de su participación para mantener una comunidad activa y comprometida.

**DonaTrack** resuelve estas problemáticas mediante una arquitectura modular y desacoplada que automatiza el ciclo de vida de las donaciones, optimiza la logística de asignación y gamifica la experiencia del donante.

---

## 2. Arquitectura de Sistemas y Componentes

El ecosistema de **DonaTrack** se organiza como una solución modular de microservicios. Cada componente asume una responsabilidad de negocio o técnica delimitada para garantizar el desacoplamiento y facilitar la evolución independiente en futuras entregas:

*   **`donaciones-service` (Núcleo Transaccional):** Administra el catálogo de bienes, donantes, necesidades y la logística física de inventario. Aloja el motor interno de matchmaking para consolidar y sugerir asignaciones de stock.
*   **`notificaciones-service` (Despacho Autónomo):** Orquesta el envío asíncrono de alertas multicanal (Email, SMS y WhatsApp). Cuenta con réplicas locales ligeras del perfil de las personas para funcionar de manera independiente, eliminando dependencias síncronas críticas.
*   **`incentivos-service` (Motor de Gamificación):** Procesa eventos de finalización de donaciones para computar el progreso de misiones, otorgar insignias y calcular rankings mensuales de participación.
*   **`cliente-liviano` (Interfaz de Usuario):** Capa de presentación que sirve como portal de interacción directa para los donantes, beneficiarios y administradores del sistema.
*   **`n8n` (Orquestador de Procesos Externos):** Workflow Engine externo que automatiza la difusión publicitaria e integraciones low-code (como publicaciones en redes sociales tras la obtención de insignias).
*   **`common-lib` (Kernel Técnico Compartido):** Biblioteca que centraliza concerns transversales como el control global de excepciones, formateo de trazas y utilidades de testing, evitando duplicar código de infraestructura entre módulos.

---

## 3. Estructura del Proyecto

A continuación se detalla la organización física de directorios y componentes principales del repositorio multi-módulo:

```text
DonaTrack-TP-DDS/
├── .github/                      # Flujos de trabajo de CI/CD (GitHub Actions)
├── docs/                         # Documentación general y entregas
│   ├── documentador/             # Editor reactivo de ADRs y minutas de reuniones
│   ├── entregas/                 # Índice de entregables en PDF
│   └── hub/                      # Código fuente del portal de documentación
├── n8n/                          # Workflows serializados en JSON para el motor low-code
├── integration-tests/            # Módulo de Maven para tests funcionales y volumétricos E2E
├── common-lib/                   # Módulo de Maven con utilidades compartidas del Kernel Técnico
├── cliente-liviano/              # Módulo de Maven para el renderizado web en servidor
│
│   # Microservicios Core (Clean Architecture / Hexagonal)
├── auth-service/                 # Gestión de autenticación, usuarios y seguridad
├── donaciones-service/           # Dominio central de donantes, donaciones y matchmaking
├── incentivos-service/           # Lógica de gamificación, misiones y rankings
├── logistica-service/            # Planificación de rutas y tracking de transportes
└── notificaciones-service/       # Despacho multicanal asíncrono y réplicas de contactos
```

---

## 4. Ecosistema Tecnológico

### Capas Lógicas y Justificaciones Arquitectónicas

| Componente / Tecnología | Rol en el Sistema | Justificación Arquitectónica |
| :--- | :--- | :--- |
| **Java 21 & Spring Boot 3** | Motor de Servicios Core | Plataforma base para el desarrollo de microservicios robustos, aprovechando características modernas del lenguaje (records, pattern matching) y un contenedor de inyección de dependencias estandarizado. |
| **OpenFeign** | Cliente HTTP Declarativo | Abstrae la comunicación síncrona entre microservicios core (`donaciones` ➔ `notificaciones` / `incentivos`), integrando de forma nativa políticas de resiliencia y propagación de trazas distribuidas. |
| **n8n Workflow Engine** | Orquestador Low-Code Externo | Permite integrar lógica de difusión externa (redes sociales y generación de piezas gráficas de insignias) de manera ágil y visual, desacoplando los servicios core de APIs externas de terceros. |
| **Docker & Docker Compose** | Virtualización y Orquestación Local | Unifica el comportamiento del entorno de pre-producción mediante imágenes Docker multi-etapa y validación en cascada de salud de componentes (Healthchecks). |
| **Maven** | Reactor Multi-Módulo | Gestiona de forma unificada las dependencias y la compilación coordinada de todos los servicios del proyecto desde la raíz. |

---

## 5. Capacidades y Diseño de Dominio (Hitos de Entrega)

El diseño del dominio está estructurado para evolucionar incrementalmente en cada entrega académica. A continuación se presentan las capacidades activas agrupadas conceptualmente:

### Hito 1: Cimiento del Dominio y CI/CD
*   Definición inicial del modelo de datos de personas, donantes y bienes.
*   Configuración del pipeline de calidad de código estático (Spotless + Google Java Format y SonarCloud).

### Hito 2: Distribución, Gamificación y Trazabilidad (Hito Activo)
*   **Trazabilidad Física de Bienes (State Machine):** Ciclo de vida estricto para cada donación (`En depósito` ➔ `Asignación realizada` ➔ `Lista para entregar` ➔ `En traslado` ➔ `Entregada` / `Entrega fallida` o `Vencida`), garantizando un historial de auditoría inmutable.
*   **Matchmaking de Stock:** Algoritmo estructurado que prioriza entidades sub-atendidas y calcula compatibilidad semántica de necesidades en horarios programados de baja carga.
*   **Motor de Recompensas (Incentivos):** Progresión secuencial de misiones por categorías del donante (rachas consecutivas, variedad de bienes y volúmenes) y entrega de insignias digitales.
*   **Alertas Multicanal Desacopladas (Double Dispatch):** Envío polimórfico de notificaciones basadas en perfiles preferidos del usuario, implementando consistencia eventual para soportar caídas de red temporales.

---

## 6. Ingeniería de Infraestructura, Calidad y CI/CD

El proyecto prioriza la resiliencia en red, la observabilidad y la automatización del ciclo de desarrollo:

*   **Resiliencia y Tolerancia a Fallos:** Las llamadas inter-servicio configuran políticas de reintento exponencial automático para resistir caídas de red transitorias. El procesamiento lento de notificaciones o difusión externa se delega a pools de hilos asíncronos (`@Async`), liberando de inmediato el hilo HTTP de respuesta al cliente.
*   **Observabilidad y Trazabilidad Distribuidas:** Un esquema de logging estructurado unifica la visibilidad de los logs del ecosistema. Mediante *Micrometer Tracing* y aspectos AOP, se genera y propaga un Correlation ID (`traceId`) a través de llamadas de red HTTP y tareas en segundo plano (`@Scheduled`), facilitando el rastreo transaccional de extremo a extremo.
*   **Pipeline de Integración Continua (CI/CD):** Flujo automatizado en GitHub Actions que valida:
    - Convención de nombres en ramas (Git Flow UTN).
    - Compilación incremental modular (`Dorny Filter`) para optimizar tiempos del pipeline.
    - Calidad y cobertura estática de código (SonarCloud & JaCoCo).
    - Flujo de PRs Apiladas (*Stacked PRs*) para subdividir requerimientos complejos.
    - Auditoría arquitectónica automatizada (*Semantic UML Diff*) que detecta cambios estructurales en el grafo de clases de dominio de la PR frente a la rama base.

---

## 7. Configuración y Automatización de Entornos

### Prerrequisitos
* Java 21 (JDK)
* Maven 3.8+
* Docker & Docker Compose

### 1. Compilación del Proyecto
Para compilar y empaquetar de forma limpia todos los módulos del reactor Maven:
```bash
mvn clean package -DskipTests
```

### 2. Ejecución del Entorno de Pre-producción
El proyecto dispone de un entorno virtualizado local idéntico a pre-producción que levanta los microservicios core (`donaciones`, `notificaciones`, `incentivos`) y el motor de `n8n` con sus dependencias correspondientes.
```bash
docker compose -f docker-compose.preprod.yml up --build -d
```
*Nota: Los servicios cuentan con Healthchecks configurados contra el endpoint `/actuator/health` de Spring Boot, asegurando un arranque en cascada ordenado.*

### 3. Suite de Pruebas de Integración y Smoke Tests
Para automatizar la verificación del stack (incluyendo compilación limpia, importación desatendida de workflows de insignias en n8n mediante CLI, y calentamiento de Tomcat para evitar timeouts):
```bash
# En sistemas basados en Linux/Bash
./run-preprod-tests.sh

# En sistemas Windows (PowerShell)
powershell -File ./run-preprod-tests.sh
```
*Este script utiliza un manejador `trap EXIT` en Bash para remover automáticamente los contenedores temporales, liberar puertos del host y borrar volúmenes residuales al finalizar las pruebas (hayan pasado o fallado).*

---

## 8. Recursos y Documentación Anexa

*   **[Hub de Documentación Principal](https://tsorren.github.io/DonaTrack-TP-DDS/)**: Portal estático centralizado que actúa como índice dinámico de todas las entregas académicas (PDFs) e hitos del proyecto.
*   **[ADR Preview (Log4brains)](https://tsorren.github.io/DonaTrack-TP-DDS/adr-preview)**: Visualización interactiva y searchable de los Registros de Decisiones de Arquitectura (ADRs) del sistema.
*   **[DonaTrack Document Generator](https://tsorren.github.io/DonaTrack-TP-DDS/documentador)**: Herramienta reactiva basada en formularios web interactivos para la creación estandarizada y edición bidireccional de ADRs y minutas de reuniones del equipo.

---

## 9. Equipo de Ingeniería

### UTN-FRBA - Diseño de Sistemas (2026) - Grupo 5

| Nombre | Legajo | Correo |
| :--- | :---: | :--- |
| **Camila Aylen Suarez** | 214.170-0 | casuarez@frba.utn.edu.ar |
| **Camila Belén Lencina** | 215.042-6 | calencina@frba.utn.edu.ar |
| **Lara Anush Eriakian** | 213.678-8 | leriakian@frba.utn.edu.ar |
| **Martín Ilán Zajdenberg** | 209.501-4 | mzajdenberg@frba.utn.edu.ar |
| **Milton Christopher B. Estigarribia** | 208.731-5 | mestigarribia@frba.utn.edu.ar |
| **Miranda Rossi** | 208.818-6 | mirossi@frba.utn.edu.ar |
| **Nicolás Delorte** | 172.817-9 | ndelorte@frba.utn.edu.ar |
| **Sofia Maria Deane** | 210.350-3 | sdeane@frba.utn.edu.ar |
| **Tadeo Sorrentino** | 214.153-0 | tsorrentino@frba.utn.edu.ar |
| **Valentina Thiwissen** | 209.210-4 | vthiwissen@frba.utn.edu.ar |
