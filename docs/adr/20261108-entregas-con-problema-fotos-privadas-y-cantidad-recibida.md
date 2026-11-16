# Entregas con problema: fotos fuera de lo público y la necesidad cuenta lo que llegó

- Status: proposed
- Date: 2026-11-08
- Deciders: tsorren
- Tags: logistica, donaciones, entregas, privacidad, asignacion, entrega-6

> **Origen:** HB-17 ([DTI-29](./DEUDA_TECNICA.md#dti-29)). Ajusta dos puntos del
> [ADR del contrato de entregas para la entidad](./20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md)
> (`proposed`): una consecuencia que la ficha
> [Llegó con un problema](../interfaz/flujos/raices-llego-con-un-problema.md) contradice, y la regla
> que ese ADR dejó para después. No lo reemplaza: el remito, el reporte y la composición del pedido
> y del donante siguen como están.

## Contexto y Problema

El ADR del contrato de entregas decide que una entrega incompleta o dañada se confirma
(`ENTREGADA`, con fotos) y lleva un `reporte` opcional (`motivo`, `detalle`, `cantidadRecibida`).
Dejó dos cosas abiertas o en conflicto:

1. `[DOCUMENTED]` Entre sus consecuencias dice que "la foto llega igual a la portada y al mapa". El
   2026-11-07 el usuario decidió lo contrario en la ficha "Llegó con un problema": las fotos de una
   entrega con problema las ven quien donó y el equipo, no el público (§9 de la ficha).
2. `[DOCUMENTED]` DTI-29 deja pendiente "qué hace Donaciones con una cantidad recibida menor". El
   2026-11-08, al aprobar el plan de HB-17, el usuario decidió que la necesidad cuente lo que llegó.

`[OBSERVED]` Lo asignado a cada necesidad se calcula sumando las cantidades de los ítems de sus
donaciones independientes (`AcumuladosDeAsignacion`, `sumarCantidadAsignada`), y los algoritmos de
asignación lo usan para saber cuánto le falta. `[OBSERVED]` El mapa público
(`EntregaMapaPublicoResponseDTO`) ya no expone fotos; las destacadas de la portada (HB-01) todavía no
existen.

Problema: ¿qué pasa con las fotos y con la cantidad cuando la entidad cuenta un problema?

## Atributos de Calidad y Drivers de Decisión

Privacidad y trato justo con quien donó y con la entidad 35 · exactitud de lo asignado (los
algoritmos asignan de más si cuentan lo que no llegó) 30 · cambios aditivos 20 · costo 15.

## Alternativas Consideradas

Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`).

### Fotos de una entrega con reporte

| Alternativa | Privacidad | Exactitud | Aditivo | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **F1. Quedan privadas: las destacadas excluyen las entregas con reporte** | 5 | — | 5 | 5 | **5,0** |
| F2. Se publican igual (lo que dice el ADR del contrato) | 2 | — | 5 | 5 | 3,4 |

### Cantidad que cuenta para la necesidad

| Alternativa | Privacidad | Exactitud | Aditivo | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **C1. Cuenta lo recibido: la donación guarda lo que no llegó y las sumas lo descuentan** | — | 5 | 5 | 3 | **4,5** |
| C2. Cuenta lo enviado; el reporte queda como información | — | 2 | 5 | 5 | 3,6 |
| C3. Se reducen los ítems de la donación a lo recibido | — | 4 | 3 | 4 | 3,6 |

## Resultado de la Decisión

Se eligen **F1** y **C1**: los decidió el usuario (2026-11-07 y 2026-11-08).

1. **Fotos.** Una entrega con `reporte` no aporta fotos a lo público. El mapa ya no las muestra. Las
   destacadas de HB-01 tienen que excluir las donaciones cuya entrega llegó con reporte; HB-01 lo
   implementa al replicar las fotos de `entrega.exitosa`, que ya trae el `reporte`. Quien donó y el
   equipo siguen viendo las fotos.
2. **Cantidad.** `entrega.exitosa` trae `reporte.cantidadRecibida`. Donaciones la guarda en la
   donación independiente como `cantidadNoRecibida` (lo enviado menos lo recibido, redondeado hacia
   abajo porque la cantidad es entera), en la misma transacción idempotente que el paso a
   `ENTREGADA`. Lo asignado a la necesidad descuenta lo que no llegó. Si llegó igual o más, no se
   descuenta nada.
3. La alerta al administrador por lo que no llegó se suma cuando haya bandeja con autenticación;
   hasta entonces la pantalla lo marca (decisión del usuario, 2026-11-06).

## Consecuencias

### Positivas

- La entidad puede contar un problema sin exponer al público la foto de una entrega fallida.
- Los algoritmos de asignación ven lo que de verdad cubre la necesidad y siguen asignando lo que
  falta.
- La donación conserva lo enviado: lo que no llegó queda aparte, sin tocar los ítems.

### Negativas

- `sumarCantidadAsignada` hace una consulta más por llamada.
- Una cantidad recibida con decimales (14,5 kg) cuenta como entera (14), hasta que las cantidades
  admitan decimales.
- HB-01 suma una condición (excluir las entregas con reporte).

## Análisis por alternativa

- **F1.** Pro: respeta la decisión del usuario y no publica algo que salió mal. Contra: menos fotos
  en la portada.
- **F2.** Pro: no hay que filtrar. Contra: va contra la decisión del usuario de la ficha.
- **C1.** Pro: exacto y aditivo; lo enviado queda intacto. Contra: una consulta más y el redondeo.
- **C2.** Pro: no toca Donaciones. Contra: la necesidad parece cubierta aunque no llegó todo.
- **C3.** Pro: las sumas no cambian. Contra: borra lo que se envió y altera los ítems después de la
  entrega.

## Trabajo futuro

- HB-01: excluir de las destacadas las entregas con reporte.
- Alerta al administrador con la bandeja de Notificaciones, cuando haya autenticación (HB-13).
