# Catálogo de componentes y capas CUBE

> Índice para los agentes y las personas: lo primero que se lee antes de construir o revisar una
> pantalla (skills `vista-ui` y `revision-ui`). Lo pide el
> [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`). La versión visual es `/estilos` (RF-CL-05).
>
> **Estado:** vacío hasta cerrar B0-03 y B0-06 del [backlog de migración](backlog-migracion-componentes.md).
> Mientras tanto, los componentes y su patrón están en [patrones y microcopy](patrones-y-microcopy.md#componentes-y-su-patrón).

## Cómo se usa

- Si lo que necesitás está acá, se usa como está. No se crea una clase parecida.
- Si falta, se agrega primero acá (y en `/estilos`, cuando exista con B0-06), y después se usa.
  Desde B0-07, el test del cerco rechazará las clases que no figuren en este catálogo; hasta
  entonces, lo controla la revisión.
- Los nombres van en español. La columna "Equivalente" da el nombre conocido (Every Layout, Material
  o HIG) para orientarse.

## Composición

| Clase | Equivalente | Cuándo | Parámetros (custom properties) |
|---|---|---|---|

## Utilidades

| Clase o atributo | Token | Cuándo |
|---|---|---|

## Movimiento

| `data-movimiento` | Tokens | Cuándo | Con "reducir movimiento" |
|---|---|---|---|

## Componentes (bloques)

| Componente | Fragmento | Parámetros | Slots | Estados y excepciones | Movimiento | Patrón |
|---|---|---|---|---|---|---|
