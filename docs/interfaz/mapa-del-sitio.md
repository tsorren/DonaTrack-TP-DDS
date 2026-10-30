# Mapa del sitio

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · une el mapa de E1 con las pantallas de E6**

Fuentes: [`Mapa del Sitio.pdf`](../entregas/1/interfaz/Mapa%20del%20Sitio.pdf) de E1 y la tabla de pantallas de
[`requerimientos-e6.md` §6.2](../requerimientos/requerimientos-e6.md). Regla: ninguna función clave
queda a más de **3 niveles** (RNF-UI-02). El nivel 1 es el inicio de cada rol.

## Mapa

```mermaid
flowchart LR
  subgraph Publico["Público"]
    L[Landing] --> DR[Donaciones realizadas]
    L --> MP[Mapa de entregadas]
    L --> EN[Entidades y necesidades]
    L --> NO[Nosotros]
    L --> LD[Landing donante]
    L --> LB[Landing beneficiario]
    L --> LG[Legales y privacidad]
    L --> IS[Iniciar sesión]
    L --> RE[Registrarse: elegir]
    RE --> RD[Registro de donante]
    RE --> RN[Registro de entidad]
    IS --> RC[Recuperar contraseña]
    IS --> AC[Activar cuenta]
  end
  subgraph Donante
    ID[Inicio] --> MD[Mis donaciones] --> DD[Detalle de donación]
    ID --> BE[Beneficiarios]
    ID --> MI[Misiones e insignias]
    ID --> SD[Seguir entregas]
    ID --> ND[Notificaciones]
    ID --> PD[Mi perfil]
  end
  subgraph Entidad
    IE[Inicio] --> NE[Necesidades] --> DN[Detalle de necesidad]
    IE --> DRc[Donaciones recibidas]
    IE --> SE[Seguir entregas] --> CR[Confirmar recepción]
    IE --> NN[Notificaciones]
    IE --> PE[Mi perfil]
  end
  subgraph Admin
    IA[Inicio] --> RDo[Registrar donantes]
    IA --> REn[Registrar entidades]
    IA --> AE[Aprobar entidades]
    IA --> RDn[Registrar donaciones]
    IA --> DO[Donaciones] --> DV[Detalle y vencimiento]
    IA --> AS[Asignaciones] --> PR[Propuesta]
    IA --> CA[Camiones]
    IA --> RK[Ranking]
    IA --> CSV[Importar CSV] --> RES[Resultado por fila]
  end
```

## Pantallas

| Pantalla | Rol | Nivel | Requerimiento | Boceto E1 | Origen |
|---|---|:-:|---|---|---|
| Landing | Público | 1 | RF-01 | pág. 1 | E1 |
| Donaciones realizadas | Público | 2 | RF-01 | pág. 2 | E1 |
| Mapa de entregadas | Público | 2 | RF-02 | págs. 2 y 23 | E1 |
| Entidades y necesidades | Público | 2 | RF-22 | — | E6 (D26) |
| Nosotros | Público | 2 | RF-01 | pág. 3 | E1 |
| Landing donante / beneficiario | Público | 2 | RF-01 | — | E1 (sin boceto) |
| Legales y privacidad | Público | 2 | RF-01 | pie de página | E6 |
| Iniciar sesión | Público | 2 | RF-04 | pág. 4 | E1 |
| Registrarse: elegir | Público | 2 | RF-03 | pág. 5 | E1 |
| Registro de donante / entidad | Público | 3 | RF-03 | pág. 11 (entidad) | E1 |
| Recuperar contraseña / Activar cuenta | Público | 3 | RF-AUTH | — | E6 |
| Inicio del donante | Donante | 1 | — | pág. 15 | E1 |
| Mis donaciones → detalle | Donante | 2 → 3 | RF-05 | pág. 16 | E1 |
| Beneficiarios | Donante | 2 | RF-06 | pág. 17 | E1 |
| Misiones e insignias | Donante humano | 2 | RF-07 | pág. 18 | E1 |
| Seguir entregas | Donante | 2 | RF-09 | pág. 19 | E1 |
| Notificaciones | Donante | 2 | RF-08 | pág. 6 | E1 |
| Mi perfil | Donante y entidad | 2 | RI-AUTH-02 | menú de usuario (pág. 14) | E6 |
| Inicio de la entidad | Entidad | 1 | — | pág. 24 | E1 |
| Necesidades → detalle | Entidad | 2 → 3 | RF-10 | págs. 26 y 29–31 | E1 |
| Donaciones recibidas | Entidad | 2 | RF-11 | pág. 27 | E1 |
| Seguir entregas → **Confirmar recepción** | Entidad | 2 → 3 | RF-14, **RF-12** | pág. 28 | E1 · **flujo piloto** |
| Notificaciones | Entidad | 2 | RF-13 | — | E1 |
| Inicio del admin | Admin | 1 | — | pág. 13 | E1 |
| Registrar donantes / entidades / donaciones | Admin | 2 | RF-15 | pág. 13 (menú) | E1 |
| Aprobar entidades | Admin | 2 | RF-21 | — | E6 |
| Donaciones → detalle y vencimiento | Admin | 2 → 3 | RF-16 | pág. 13 (menú) | E1 |
| Asignaciones → propuesta | Admin | 2 → 3 | RF-17 | — | E6 |
| Camiones | Admin | 2 | RF-18 | pág. 13 (menú) | E1 |
| Ranking | Admin | 2 | RF-19 | — | E6 |
| Importar CSV → resultado | Admin | 2 → 3 | RF-20 | — | E6 |

## Observaciones para el diseño

- El admin tiene 9 secciones de nivel 2. Una fila de píldoras con 9 opciones es mucha carga:
  conviene agruparlas (por ejemplo, Altas · Donaciones · Logística · Reportes). Se decide con un
  A/B del tema `layout:navegacion-admin` ([catálogo de temas](temas-ab.md)).
- "Mapa de entregadas" y "Donaciones realizadas" comparten el mapa en los bocetos. Se decide al
  diseñar ese flujo si van juntas o separadas.
