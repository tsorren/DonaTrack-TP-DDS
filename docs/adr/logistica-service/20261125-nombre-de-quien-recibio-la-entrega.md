# Nombre de quien recibió la entrega: dato personal opcional y privado en Logística

- Status: proposed
- Date: 2026-11-25
- Deciders: pendiente de aprobación humana; redactado por el agente en HG-03. DA-10 (en lo público, la organización) quedó delegada en la opción recomendada por el relevamiento del 2026-11-24
- Tags: logistica, entregas, privacidad, datos-personales, contrato, entrega-7

> **Origen:** hueco HG-03 de la guía al código (backlog `docs/interfaz/backlog-guia-al-codigo.md` de
> `E7_guia_ui`, §7) y [SPEC-HG03](../../specs/active/SPEC-HG03-nombre-de-quien-recibio.md). La
> etiqueta privada de una entrega confirmada tiene que decir «Recibió Marta el 5 de octubre»; la
> pública, «Recibió: Comedor Barrio Ejemplo».

## Contexto y Problema

- `[OBSERVED]` Al confirmar, el cliente manda como `actor` el identificador de la organización
  (`cliente-liviano/.../rest/EntidadConfirmarRest.java`, método `confirmar`). Nadie guarda el
  nombre de la persona que recibió.
- `[DOCUMENTED]` El [ADR de réplicas por evento](../20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md)
  dice que Logística no guarda datos personales del donante y que por los eventos viajan
  identificadores y URLs, nunca datos personales.
- `[DOCUMENTED]` La [estrategia de crypto-shredding](../20260901-estrategia-de-crypto-shredding-para-supresion-de-datos-personales.md)
  (`proposed`) cifra los datos de una persona con una clave por `persona_id`. `[OBSERVED]` No está
  implementada en ningún servicio (no hay `AttributeConverter` de cifrado en el código).
- `[INFERRED]` Quien recibe no es una persona registrada: no tiene `personaId` ni cuenta. Es texto
  libre que escribe la organización.

Problema: ¿dónde vive el nombre de quien recibió, quién lo puede leer y cómo se evita que llegue a
lo público?

## Atributos de Calidad y Drivers de Decisión

- **Privacidad (DA-10):** lo público muestra la organización, nunca la persona.
- **Compatibilidad:** cambio aditivo (AGENTS.md §8.2); ningún consumidor actual se rompe.
- **Cohesión:** el dato se escribe en el mismo comando que confirma la entrega.
- **Costo:** sin canal nuevo entre servicios ni dependencias.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`). Pesos: privacidad 0,35 ·
compatibilidad 0,25 · cohesión 0,25 · costo 0,15.

| Opción | Privacidad | Compatibilidad | Cohesión | Costo | Total |
| --- | --- | --- | --- | --- | --- |
| **A. Campo opcional en la entrega de Logística, solo en lecturas privadas** | 4 | 5 | 5 | 5 | **4,65** |
| B. Campo en la entrega y réplica en Donaciones por `entrega.exitosa` | 2 | 4 | 3 | 3 | 2,95 |
| C. Ficha de la persona en Donaciones (una «persona receptora») | 4 | 3 | 2 | 1 | 2,80 |
| D. No guardar nombre: la etiqueta privada dice la organización | 5 | 5 | 5 | 5 | 5,00 (no cumple HG-03) |

- **B, descartada:** pone un dato personal en un evento y en otra base; contradice el ADR de
  réplicas. Si Donaciones necesitara el nombre, lo lee de Logística.
- **C, descartada:** crea un agregado de persona para un texto que no identifica a nadie
  registrado; costo alto y otro contrato.
- **D, descartada:** es lo que hay hoy y es lo que HG-03 pide cambiar. Queda como respaldo si el
  revisor humano decide que el dato no vale el riesgo.

## Resultado de la Decisión

Opción elegida: **A**.

1. `PATCH /api/entregas/{id}/estado` a `ENTREGADA` acepta `recibidoPor` opcional (hasta 80
   caracteres sin los espacios de los bordes; en blanco queda nulo). `actor` no cambia de sentido.
2. La entrega lo guarda (`entrega.recibido_por`, migración `V12`; campo `recibidoPor` en Mongo).
3. Lo devuelven `GET /api/entregas/{id}`, la respuesta del cambio de estado y los tres seguimientos
   (por donación, por entidad y por donante), que son lecturas autenticadas.
4. No lo llevan `GET /api/entregas/mapa` (la única ruta pública de Logística en el gateway), el
   evento `entrega.exitosa` ni los logs.

## Consecuencias

### Positivas

- La etiqueta privada puede nombrar a la persona sin tocar Donaciones ni los eventos.
- El cambio es aditivo: OpenAPI 1.12.0, campo `nullable` en request y response.

### Negativas

- Logística guarda por primera vez un dato personal. No hay supresión a pedido del titular: el
  crypto-shredding no está implementado y quien recibe no tiene `persona_id`. Queda como deuda
  ([DTI-55](../DEUDA_TECNICA.md#dti-55)).
- Quien dona ve el nombre en su seguimiento. DA-10 solo fija lo público; si el revisor quiere
  limitarlo a la organización, es un filtro en la lectura por donante.

## Relaciones Arquitectónicas

1. **Afina:** [réplicas por evento](../20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md)
   (`proposed`): el nombre no viaja por eventos; Logística sí guarda este dato personal.
2. **Depende de:** [crypto-shredding](../20260901-estrategia-de-crypto-shredding-para-supresion-de-datos-personales.md)
   (`proposed`) para la supresión, cuando exista.
3. **Respeta:** [contrato de entregas para la entidad](../20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md)
   (`proposed`): mismo recorrido que el remito.

## Validación

`QuienRecibioTest`, `EntregaServiceTest`, los tests de mappers JPA y Mongo,
`RepositoriosLogisticaJpaTest#deberiaPersistirYRecuperarQuienRecibio` y
`OpenApiContractDriftTest#quienRecibio_estaEnLasLecturasPrivadasYNuncaEnElMapaPublico`.
