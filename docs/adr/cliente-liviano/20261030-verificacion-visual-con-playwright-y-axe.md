# Verificación visual del cliente liviano con Playwright y axe

- Status: proposed
- Date: 2026-10-30
- Deciders: tsorren, al aprobar el plan de directivas de diseño de la interfaz de E6 (decisión D-C del plan)
- Tags: cliente-liviano, testing, accesibilidad, playwright, axe-core, entrega-6

> **Origen:** plan de directivas de diseño del cliente liviano (E6), decisión D-C.
> `[OBSERVED]` Implementada en `cliente-liviano/src/test/java/grupo5/cliente/visual/` (rama
> `E6_directivas_diseno_interfaz`).

## Contexto y Problema

RNF-UI-07 a RNF-UI-10 piden una interfaz responsive, sin superposiciones en ningún tamaño, con
WCAG AA y áreas de toque de 48 × 48 px. El proceso de diseño ([`docs/interfaz/`](../../interfaz/README.md))
compara variantes A/B con capturas en los anchos de referencia (360, 1280 y 1920 px) y los
agentes de IA necesitan evidencia que puedan leer (imágenes y un reporte) para hacer su
autocrítica antes de mostrarle algo al usuario.

`InterfazFitnessTest` cubre lo que se ve en el código fuente (tokens, colores, quiebres, contraste
de los pares declarados). Lo que solo se ve con la página dibujada (desbordes, tamaño real de los
controles, reglas de axe, capturas) necesita un navegador.

Problema: ¿con qué herramienta se dibujan y comprueban las páginas, de forma reproducible para
cualquier integrante y para los agentes?

## Atributos de Calidad y Drivers de Decisión

Pesos de la decisión D-C del plan: reproducible por cualquiera 30 · sin toolchain nueva 25 ·
evidencia legible por el agente (PNG + JSON) 20 · costo de puesta en marcha 15 · independiente del
cliente de agente 10.

## Relaciones Arquitectónicas

1. **Complementa:** [20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md) (`proposed`): comprueba lo que produce ese stack.
2. **Respeta:** [20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad](../20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md): no cambia los gates del CI; la verificación visual queda fuera del CI por ahora ([DTI-28](../DEUDA_TECNICA.md#dti-28)).

## Alternativas Consideradas

| Alternativa | Reproducible | Sin toolchain nueva | Evidencia legible | Puesta en marcha | Independiente | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **A. Playwright Java + axe-core (Deque), perfil Maven `visual`** | 5 | 5 | 5 | 4 | 5 | **4,85** |
| B. Selenium + Chrome instalado + axe | 3 | 5 | 4 | 3 | 5 | 3,9 |
| C. Node: `npx playwright` y `@axe-core/cli` | 4 | 1 | 5 | 3 | 5 | 3,4 |
| D. Solo el navegador del agente (panel de Claude Code) | 1 | 5 | 3 | 5 | 1 | 3,0 |

## Resultado de la Decisión

Se elige **A**:

- `com.microsoft.playwright:playwright` 1.63.0 y `com.deque.html.axe-core:playwright` 4.14.0
  (la 4.14.0 depende de Playwright 1.63.0), en `test` y **solo dentro del perfil `visual`** de
  `cliente-liviano/pom.xml`. `[OBSERVED]` Versiones verificadas en Maven Central el 2026-10-30.
- El paquete `grupo5.cliente.visual` se excluye de la compilación de tests por defecto y el perfil
  lo vuelve a incluir. Así `mvn test` y el CI no bajan Playwright: su driver pesa unos 200 MB
  (`driver-bundle` 1.63.0: 203.821.698 bytes).
- `VerificacionVisualTest` levanta la app con el perfil `maqueta` y, por cada ruta de
  `src/test/resources/verificacion/paginas.txt`:
  - en los anchos de `AnchosDeReferencia.REFERENCIA` (360, 1280 y 1920): captura de página
    completa, axe con las reglas `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa` y `wcag22aa`,
    controles visibles de 48 × 48 px o más (sin contar los enlaces dentro de un texto, como admite
    WCAG 2.5.8) y sin desborde horizontal;
  - en los anchos `INTERMEDIOS` (incluye una tableta, 768): solo el desborde.
- Con `-Dverificacion.variantes=<tema>-a,<tema>-b` repite todo con cada variante A/B y arma
  `comparacion.html`. Deja en `target/capturas/` los PNG, `index.html` y `reporte.json`.
- El test falla con cualquier hallazgo: una variante que rompe el piso no se le muestra al usuario.

## Consecuencias

### Positivas

- Una sola orden (`mvn test -pl cliente-liviano -Pvisual`) en cualquier máquina con Java y Maven:
  Playwright baja su Chromium la primera vez.
- Los agentes leen capturas y un JSON; el usuario ve una comparación lado a lado.
- Agregar una página a comprobar es una línea en `paginas.txt`; cambiar los anchos, una constante.

### Negativas

- Fuera del CI: un PR puede romper la accesibilidad sin que el CI lo note hasta que alguien corra
  el perfil. Registrado como [DTI-28](../DEUDA_TECNICA.md#dti-28).
- La primera corrida baja unos 200 MB de driver y el navegador; necesita red.
- axe detecta parte de los problemas de accesibilidad, no todos: la revisión de `revision-ui` y la
  prueba con teclado siguen haciendo falta.
- Sonar ve las clases del paquete `visual` sin compilar en el análisis del CI (corre sin el
  perfil): puede avisar tipos sin resolver en esos archivos.

## Análisis por alternativa

- **B. Selenium + Chrome instalado:** sin toolchain nueva, pero depende del Chrome de cada
  máquina y de su versión; las capturas de página completa requieren CDP.
- **C. Node:** las herramientas de referencia de Playwright y axe, pero suman Node al módulo,
  justo lo que D17 y D20 evitaron.
- **D. Solo el navegador del agente:** útil para mirar y depurar, y se sigue usando así
  (`.claude/launch.json`), pero no es reproducible en la terminal ni para otro integrante.
- **A con las dependencias siempre en `test`:** más simple de configurar, pero cada build del CI
  bajaría el driver de 200 MB aunque no lo use. Descartada.
- **A con `@Tag("visual")` en lugar de excluir el paquete:** igual necesita las dependencias para
  compilar, así que tiene el mismo costo que la anterior. Descartada.

## Validación

- `[VERIFIED]` 2026-10-30: `mvn test -pl cliente-liviano -Pvisual` en verde sobre `/estilos`
  (`reporte.json` vacío).
- `[VERIFIED]` 2026-10-30: con una variante de estructura rota a propósito (control de 20 × 20 px,
  campo sin etiqueta y un bloque de 2400 px), el test falló con 17 hallazgos: axe `label`, toque y
  desborde en los 8 anchos.
