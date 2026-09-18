# Herramientas y Utilidades Locales — DonaTrack

> **Portal de Aplicaciones Web y Utilidades de Desarrollo**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §2`](../../AGENTS.md)

---

## 1. Propósito

Este directorio contiene las aplicaciones web interactivas y herramientas de desarrollo local diseñadas para facilitar la gestión documental, la redacción de decisiones arquitectónicas (ADRs) y la navegación unificada del material académico de la cátedra.

```text
docs/herramientas/
├── README.md              # Este índice de navegación
├── documentador/          # 📝 Generador web interactivo de ADRs y minutas de diseño
└── hub/                   # 🌐 Visor web del portal de entregas y documentación
```

---

## 2. Herramientas Disponibles

* [**Documentador Web (`documentador/`)**](documentador/README.md):  
  Aplicación web estática (`index.html`) que asiste a los integrantes del equipo en la redacción asistida de:
  - Registros de Decisión de Arquitectura siguiendo el estándar MADR ([`plantilla_adr.md`](documentador/plantilla_adr.md)).
  - Minutas de reuniones y diseño técnico ([`plantilla_minuta.md`](documentador/plantilla_minuta.md)).
  Permite exportar directamente a Markdown listo para incorporar a `docs/adr/`.
* [**Hub de Entregas y Documentación (`hub/`)**](hub/):  
  Portal web accesible localmente (`index.html`) o mediante GitHub Pages que proporciona una interfaz visual moderna para:
  - Visualizar los enunciados en PDF de las Entregas 1 a 4.
  - Explorar diagramas de clases, paquetes y secuencias.
  - Enlazar directamente al visor interactivo de ADRs (Log4brains).

---

## 3. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
