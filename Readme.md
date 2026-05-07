# ddsi-tp-template

Plantilla base para el trabajo práctico de DDSI (UTN FRBA). Implementa una arquitectura de servicios con Spring Boot y una biblioteca compartida, usando un reactor de Maven multi-módulo.

---

## Requisitos previos

- JDK 21
- Maven 3.9+
- Docker (opcional, solo para construir y ejecutar contenedores)

---

## Estructura del repositorio

```
ddsi-tp-template/
├── pom.xml                    # POM padre: versiones y dependencyManagement
├── common-lib/                # Librería compartida (JAR), importada por los servicios
├── donaciones-service/        # Servicio de donaciones — puerto 8080
└── notificaciones-service/    # Servicio de notificaciones — puerto 8081
```

Cada servicio es una aplicación Spring Boot independiente que declara `common-lib` como dependencia local del reactor.

---

## Tecnologías

| Tecnología          | Versión       |
|---------------------|---------------|
| Java                | 21            |
| Spring Boot         | 4.0.5         |
| Spring Cloud BOM    | 2025.1.1      |
| Lombok              | 1.18.34       |
| Maven               | 3.9+          |

El BOM de Spring Cloud está declarado en el POM padre para que los módulos puedan incorporar dependencias de Spring Cloud sin especificar versión explícita.

---

## Desarrollo local (Maven)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

### Compilar todos los módulos

```bash
mvn clean install
```

Esto construye `common-lib` primero y luego los servicios que dependen de ella.

### Ejecutar un servicio

```bash
# Servicio de donaciones (puerto 8080)
mvn spring-boot:run -pl donaciones-service

# Servicio de notificaciones (puerto 8081)
mvn spring-boot:run -pl notificaciones-service
```

Maven resuelve `common-lib` directamente desde el reactor, por lo que no hace falta instalarla por separado si se ejecuta desde la raíz.

---

## Construcción de imágenes Docker

Este proyecto utiliza una arquitectura multi-módulo de Maven. Los microservicios dependen del `pom.xml` padre y de `common-lib`, por lo que **el contexto de construcción de Docker siempre debe ser la raíz del proyecto**. Si se limita el contexto a la carpeta del microservicio, Maven fallará al no encontrar el POM padre ni las dependencias comunes.

### Construcción manual (CLI)

Posicionarse en la carpeta raíz del proyecto y pasar el Dockerfile con `-f`, dejando `.` como contexto:

```bash
# donaciones-service (expone el puerto 8080)
docker build -t donaciones-img -f donaciones-service/Dockerfile .

# notificaciones-service (expone el puerto 8081)
docker build -t notificaciones-img -f notificaciones-service/Dockerfile .
```

### Ejecutar los contenedores

```bash
docker run -p 8080:8080 donaciones-img
docker run -p 8081:8081 notificaciones-img
```

### Nota sobre `ARG SERVICE_NAME`

Cada Dockerfile define un `ARG SERVICE_NAME` cuyo valor por defecto ya coincide con el nombre del servicio (p. ej. `donaciones-service`). Solo es necesario sobreescribirlo si se reutiliza un Dockerfile genérico para construir un servicio diferente:

```bash
docker build --build-arg SERVICE_NAME=otro-service -f otro-service/Dockerfile .
```

---

## Estado del proyecto

Los servicios son aplicaciones Spring Boot mínimas, listas para extender con controladores, repositorios y lógica de negocio. `common-lib` contiene el código compartido entre servicios.
