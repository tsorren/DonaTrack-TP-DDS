# DonaTrack

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-build-blue?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-blue?style=flat-square&logo=docker)](https://www.docker.com/)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-quality_gate-green?style=flat-square&logo=sonarcloud)](https://sonarcloud.io/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-blue?style=flat-square&logo=githubactions)](https://github.com/features/actions)
[![n8n](https://img.shields.io/badge/n8n-workflow-red?style=flat-square&logo=n8n)](https://n8n.io/)

Optimizacion logistica, trazabilidad transparente y fidelizacion de donantes para potenciar el impacto de las organizaciones sociales mediante una plataforma de software robusta, moderna y escalable.

---

## Indice

* [Propuesta de Valor y Desafios Resolutivos](#propuesta-de-valor-y-desafios-resolutivos)
* [Soluciones Operativas de la Plataforma](#soluciones-operativas-de-la-plataforma)
* [Arquitectura Distribuida y Resiliencia](#arquitectura-distribuida-y-resiliencia)
* [Estructura del Repositorio](#estructura-del-repositorio)
* [Ecosistema Tecnologico](#ecosistema-tecnologico)
* [Garantias de Calidad y Estandares de Desarrollo](#garantias-de-calidad-y-estandares-de-desarrollo)
* [Guia de Instalacion y Validacion Local](#guia-de-instalacion-y-validacion-local)
* [Equipo de Ingenieria](#equipo-de-ingenieria)

---

## Propuesta de Valor y Desafios Resolutivos

La gestion diaria de donaciones materiales en el tercer sector requiere de un control riguroso para asegurar que la ayuda llegue a su destino en tiempo y forma. DonaTrack ha sido diseñada para transformar y optimizar esta operatoria, resolviendo tres problematicas criticas del sector social:

* **Trazabilidad y Auditoria Clara:** Asegura un registro inmutable de la ubicacion y estado fisico de los bienes donados, aportando total transparencia ante donantes y auditores de la organizacion.
* **Matchmaking Inteligente de Recursos:** Distribuye el stock disponible cruzando las necesidades declaradas por los centros con el inventario fisico. El sistema asigna los recursos priorizando activamente a las entidades que han recibido menor asistencia en el ultimo trimestre para evitar la sobre-atencion y el desperdicio.
* **Engagement del Colaborador:** Incentiva la participacion constante mediante misiones de ayuda secuenciales (rachas de donacion consecutivas, completitud de rubros de bienes y volumenes de stock) y el otorgamiento de insignias de impacto social.

---

## Soluciones Operativas de la Plataforma

DonaTrack resuelve las necesidades logisticas complejas mediante modulos tecnicos robustos que aseguran consistencia y escalabilidad:

* **Control de Inventario ininterrumpido:** Cada recurso ingresado transiciona de manera estrictamente controlada por una maquina de estados logica (desde deposito hasta su entrega final o retorno justificado), previniendo desvios en el stock fisico.
* **Procesamiento Semantico Protegido:** Clasifica las descripciones coloquiales de los bienes donados relacionandolas con alias oficiales de la organizacion, al tiempo que preserva inalterado el registro original ingresado por el donante para auditorias posteriores.
* **Despacho Multicanal Inteligente:** Envia notificaciones en tiempo real (WhatsApp, SMS o Correo) resolviendo el canal y formato de forma automatica en base a las preferencias y perfil del destinatario.
* **Orquestacion de Procesos Automatizada:** Integra las alertas y reconocimientos del sistema con redes sociales y plataformas externas mediante flujos visuales independientes, permitiendo flexibilizar las campañas de difusion sin alterar la logica central de la organizacion.

---

## Arquitectura Distribuida y Resiliencia

La plataforma se organiza bajo una arquitectura modular de servicios independientes desacoplados para asegurar una alta disponibilidad operativa:

* **Modulo de Donaciones:** Gestiona de forma transaccional el catalogo de bienes, inventarios y necesidades operativas.
* **Modulo de Incentivos:** Registra las metricas historicas de ayuda, procesa el cumplimiento de misiones y administra los rankings.
* **Modulo de Notificaciones:** Sistema no bloqueante para el despacho de alertas que mantiene réplicas locales ligeras de los perfiles. Esto garantiza que las comunicaciones sigan funcionando de forma autonoma incluso ante eventuales caidas o demoras en la red de los otros modulos.
* **Kernel Compartido (common-lib):** Biblioteca que centraliza excepciones tecnicas y logging estructurado de manera transversal sin compartir logica ni entidades de negocio, resguardando la autonomia de desarrollo y despliegue de cada modulo.

---

## Estructura del Repositorio

Organizacion del reactor multi-modulo y de los directorios principales del proyecto:

```text
DonaTrack-TP-DDS/
├── .github/                      # Flujos de integracion continua y calidad automatizada
├── docs/                         # Documentacion del portal y registros de decisiones
├── n8n/                          # Workflows serializados para la integracion externa
├── integration-tests/            # Suite de pruebas funcionales y de extremo a extremo
├── common-lib/                   # shared kernel de utilidades de infraestructura y logging
├── cliente-liviano/              # Interfaz y portal web de administracion
├── auth-service/                 # Modulo de seguridad, usuarios y accesos
├── donaciones-service/           # Modulo central de inventario y matchmaking
├── incentivos-service/           # Modulo de gamificacion, rankings e incentivos
├── logistica-service/            # Modulo experimental de transporte y rutas
└── notificaciones-service/       # Modulo de despacho de comunicaciones y alertas
```

---

## Ecosistema Tecnologico

La seleccion de tecnologias responde a la necesidad de construir un producto confiable, mantenible a largo plazo y facilmente adaptable:

* **Java 21 y Spring Boot 3:** Plataforma robusta y tipada que ofrece un rendimiento estable y una infraestructura segura para el desarrollo de servicios web transaccionales.
* **Docker y Docker Compose:** Virtualizacion de entornos que garantiza la portabilidad absoluta del sistema, logrando que funcione de forma identica en cualquier computadora personal o servidor en la nube.
* **n8n Workflow Engine:** Agilidad logica para automatizar integraciones mediante flujos de trabajo independientes, liberando a los microservicios principales de las dependencias de APIs de terceros.
* **Maven Reactor:** Reactor de compilacion unificado que simplifica la compilacion incremental y la administracion de dependencias.

---

## Garantias de Calidad y Estandares de Desarrollo

La integridad y estabilidad de DonaTrack se respalda en procesos de ingenieria rigurosos aplicados a lo largo de todo el ciclo de desarrollo:

* **Integracion Continua (CI/CD):** Flujos de ejecucion en GitHub Actions que compilan, formatean (Spotless) y validan de manera automatica el codigo en cada actualizacion, optimizando tiempos mediante la deteccion de cambios por modulos.
* **Monitoreo de Deuda Tecnica:** Integracion con SonarCloud para evaluar de forma automatica la salud del codigo, previniendo vulnerabilidades y asegurando una cobertura robusta de pruebas.
* **Suite de Pruebas Automatizadas E2E:** Verificacion automatica de extremo a extremo que simula solicitudes HTTP reales y tolera retrasos de red temporales, asegurando la robustez de las integraciones antes de promover cambios a produccion.
* **Politica de Desarrollo Ordenado:** Uso de convenciones de ramas estrictas y hooks locales que previenen el registro de codigo que no cumpla con los estandares de formato y calidad exigidos.

---

## Guia de Instalacion y Validacion Local

### Prerrequisitos

* Java 21 (JDK)
* Maven 3.8 o superior
* Docker con Compose v2 (soporte para el parametro --wait)

### 1. Compilacion Manual de Componentes
Si se desea compilar y empaquetar de forma manual y limpia todos los modulos del reactor Maven:
```bash
mvn clean package -DskipTests
```

### 2. Despliegue del Entorno Local con Docker Compose
Para levantar manualmente el ecosistema virtualizado completo de microservicios y la plataforma n8n en segundo plano:
```bash
docker compose -f docker-compose.preprod.yml up --build -d
```
*Los contenedores utilizan healthchecks basados en Actuator (/actuator/health) para asegurar una secuencia de arranque ordenada y segura.*

### 3. Suite de Validacion y Pruebas de Integracion Automatizadas
El repositorio cuenta con dos scripts orquestadores en Bash diseñados para automatizar por completo el ciclo de construccion, ejecucion de infraestructura y pruebas de extremo a extremo:

* **run-preprod-tests.sh (Flujo Completo con Auto-cleanup):** Compila el proyecto, levanta la infraestructura esperando a que todos los servicios esten saludables, importa y activa de forma automatica los flujos de n8n, realiza un warm-up de las APIs HTTP para evitar timeouts iniciales, ejecuta los tests de integracion REST y destruye todos los contenedores al finalizar para liberar puertos y recursos de forma automatica.
* **run-preprod-tests-stay.sh (Flujo de Depuracion):** Realiza exactamente la misma secuencia de arranque y ejecucion de pruebas, pero mantiene el entorno Docker y los contenedores corriendo en segundo plano para permitir la depuracion o consulta manual de los endpoints, esperando la confirmacion del usuario antes de desmontar el stack.

#### Comandos de Ejecucion de la Suite:

```bash
# Ejecucion del flujo de pruebas estandarizado con limpieza automatica:
./run-preprod-tests.sh

# Ejecucion del flujo de pruebas manteniendo la infraestructura activa para depuracion:
./run-preprod-tests-stay.sh

# Saltar la fase de compilacion de Maven (util si ya se cuenta con los JARs actualizados en target/):
./run-preprod-tests.sh --skip-build

# Ejecutar una unica clase de test de integracion en particular:
./run-preprod-tests.sh --test NombreDeClaseIT
```
*Nota en Windows: Ambos scripts se pueden ejecutar en PowerShell mediante `powershell -File ./run-preprod-tests.sh`.*

---

## Equipo de Ingenieria

### UTN-FRBA - Diseñó de Sistemas (2026) - Grupo 5

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
