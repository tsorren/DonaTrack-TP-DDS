# 🛡️ Guía Viva de Errores Frecuentes de SonarCloud para Agentes de IA

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**  
> **Ámbito:** Guía viva y obligatoria de verificación pre-flight para agentes de IA y desarrolladores antes de dar por finalizada cualquier tarea o abrir una Pull Request.  
> **Fuente Epistémica:** Análisis empírico de 55 commits de saneamiento y Quality Gate oficial del proyecto.

---

## 1. Propósito y Carácter de Documento Vivo

Este documento recopila las trampas técnicas, *Code Smells*, vulnerabilidades y *bugs* recurrentes que históricamente forzaron oleadas masivas de correcciones en DonaTrack.

> [!IMPORTANT]
> **Mandato para Agentes de IA:** Los agentes tienden a introducir código sintácticamente correcto que compila y pasa tests simples, pero que **viola sistemáticamente las reglas del Quality Gate de SonarCloud**. Antes de emitir el reporte final o sugerir un commit/PR, todo agente debe contrastar sus cambios contra este catálogo.

Este documento es **vivo**: si una nueva regla de SonarCloud o SonarLint bloquea un PR en CI/CD, debe registrarse aquí con su código oficial (`java:SXXXX` o `github-actions:SXXXX`), el patrón erróneo y la solución idiomática adoptada por el equipo.

---

## 2. El Quality Gate de DonaTrack como Restricción Dura

El pipeline de integración continua aplica reglas inflexibles que provocan el rechazo automático ante la mínima desviación:

### 2.1 Condiciones sobre Nuevo Código (*Conditions on New Code* — PRs y Ramas)
* **Security Rating peor que A:** 0 vulnerabilidades toleradas.
* **Security Hotspots revisados menor a 100%:** Todo punto crítico debe estar auditado y mitigado.
* **Reliability Rating peor que A:** 0 bugs tolerados (cualquier riesgo de NPE o contrato roto falla el gate).
* **Maintainability Rating peor que A:** Deuda técnica relativa inferior al 5%.
* **Líneas Duplicadas mayor a 3.0%:** Prohibida la repetición de cadenas o lógica idéntica.
* **Coverage menor a 0.0%:** Todo código nuevo debe incluir cobertura de tests ejecutables.

### 2.2 Condiciones sobre Código Global (*Conditions on Overall Code* — Ramas de Entrega)
* **Condition Coverage menor a 80.0%:** Al menos el 80% de las ramas condicionales (`if`, `switch`, ternarios) deben estar cubiertas por tests.
* **Technical Debt mayor a 0:** **Tolerancia cero a deuda técnica residual** en ramas de entrega y `main`.

---

## 3. Catálogo de Errores Frecuentes y Patrones de Corrección

### 🔴 Categoría 1: Seguridad e Infraestructura CI/CD (*Blocker / Critical*)

#### SEC-01: Inyección de Scripts en GitHub Actions (`github-actions:S6893`)
* **Problema:** Expandir directamente expresiones de contexto de GitHub dentro de scripts en bloque `run: | bash`. Si el commit message, actor o payload contiene caracteres de escape shell (`;`, `&&`, comillas), permite ejecución remota de comandos no autorizada.
* **❌ Incorrecto:**
  ```yaml
  run: |
    COMMIT_MSG="${{ github.event.head_commit.message }}"
    echo "Commit: $COMMIT_MSG"
  ```
* **✅ Correcto (Patrón DonaTrack):** Inyectar como variable de entorno intermedia y consumir la variable de entorno nativa del shell:
  ```yaml
  env:
    RAW_COMMIT_MSG: ${{ github.event.head_commit.message }}
  run: |
    COMMIT_MSG="$RAW_COMMIT_MSG"
    echo "Commit: $COMMIT_MSG"
  ```

#### SEC-02: Tags Mutables en Acciones de Terceros (`github-actions:S6898`)
* **Problema:** Usar `@v4` o `@v4.8.0` en GitHub Actions externas expone la cadena de suministro si un tag es alterado remotamente.
* **❌ Incorrecto:**
  ```yaml
  uses: JamesIves/github-pages-deploy-action@v4
  ```
* **✅ Correcto (Patrón DonaTrack):** Fijar obligatoriamente por el hash SHA completo del commit (40 caracteres):
  ```yaml
  uses: JamesIves/github-pages-deploy-action@d92aa235d04922e8f08b40ce78cc5442fcfbfa2f # v4.8.0
  ```

#### SEC-03: Descarga Insegura mediante `curl` (CWE-319)
* **Problema:** Descargar binarios o dependencias sin forzar protocolos HTTPS y redirecciones cifradas.
* **❌ Incorrecto:** `curl -L -s -o file.jar http://example.com/file.jar`
* **✅ Correcto (Patrón DonaTrack):** `curl --proto '=https' --proto-redir =https -L -s -o file.jar https://...`

---

### 🟠 Categoría 2: Confiabilidad y Dominio (*Major Bugs — Reliability Rating*)

#### REL-01: Ensombrecimiento de Variables (*Variable Shadowing* — `java:S1117`)
* **Problema:** Declarar una variable local o parámetro de método con el mismo nombre que un campo de la clase. Causa confusión y bugs sutiles en tests y entidades.
* **❌ Incorrecto:**
  ```java
  class DonacionTest {
    private DonacionIndependiente donacionIndependiente;
    @Test
    void testFragmentar() {
      DonacionIndependiente donacionIndependiente = new DonacionIndependiente(...); // Shadowing
    }
  }
  ```
* **✅ Correcto:** Renombrar la variable local para desambiguar (`donacionLocal` o un nombre descriptivo).

#### REL-02: Violación del Contrato `equals()` y `hashCode()` (`java:S1206`, `java:S2162`)
* **Problema:** Sobrescribir `equals()` sin implementar `hashCode()` (o viceversa) en entidades o Value Objects. Rompe colecciones basadas en hashing (`HashSet`, `HashMap`).
* **❌ Incorrecto:** Implementar solo `equals(Object o)` comparando IDs pero olvidar `hashCode()`.
* **✅ Correcto:** Implementar ambos basados en la misma clave de negocio o identidad inmutable (`UUID` o campos constitutivos del Value Object):
  ```java
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MiEntidad that)) return false;
    return Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id);
  }
  ```

#### REL-03: Cadenas de Llamadas con Riesgo de NPE y `Optional` (`java:S2789`, `java:S3655`)
* **Problema:** Invocar métodos en cascada asumiendo que el resultado intermedio nunca es nulo o invocar `.get()` en un `Optional` sin verificar `isPresent()`.
* **❌ Incorrecto:**
  ```java
  return obtenerPeriodoActual().donacionesAsignadas(); // Si obtenerPeriodoActual() da null -> NPE
  ```
* **✅ Correcto (Patrón DonaTrack):** Guardas de seguridad con asignación previa:
  ```java
  PeriodoNecesidad actual = obtenerPeriodoActual();
  return (actual != null && actual.donacionesAsignadas() != null)
      ? actual.donacionesAsignadas()
      : List.of();
  ```

---

### 🟡 Categoría 3: Mantenibilidad y Estructura (*Major / Minor Code Smells*)

#### MAN-01: Métodos Privados que no Usan Estado de Instancia (`java:S2325`)
* **Problema:** Métodos utilitarios o de validación interna que no acceden a ningún campo de la instancia (`this`), pero no están declarados como `static`. Fue el error más repetido en `logistica-service`.
* **❌ Incorrecto:**
  ```java
  public class Entrega {
    private void validarDestino(Direccion destino) { // Code Smell S2325
      if (destino == null) throw new ValidationException(...);
    }
  }
  ```
* **✅ Correcto:** Declarar explícitamente `private static`:
  ```java
  private static void validarDestino(Direccion destino) { ... }
  ```

#### MAN-02: Clases de Utilidad con Constructor Público por Defecto (`java:S1118`)
* **Problema:** Clases compuestas enteramente por métodos estáticos (validadores, mappers, parsers) que exponen un constructor público predeterminado.
* **❌ Incorrecto:** `public class PersonaFactory { public static Persona crear(...) { ... } }`
* **✅ Correcto:** Ocultar el constructor con visibilidad privada:
  ```java
  public class PersonaFactory {
    private PersonaFactory() {
      throw new UnsupportedOperationException("Clase de utilidad - no instanciable");
    }
    // métodos estáticos...
  }
  ```

#### MAN-03: Duplicación de Cadenas Literales (`java:S1192`)
* **Problema:** Repetir literales de texto $\ge 3$ veces (ej. mensajes de error, delimitadores, nombres de roles). Supera el umbral de duplicación del 3.0%.
* **❌ Incorrecto:** Repetir `". Motivo: "` o `"ROLE_DONANTE"` inline en múltiples métodos.
* **✅ Correcto:** Extraer a constantes estáticas reutilizables:
  ```java
  private static final String SEPARADOR_MOTIVO = ". Motivo: ";
  ```

#### MAN-04: Uso Directo de `System.out` o `printStackTrace()` (`java:S106`, `java:S4507`)
* **Problema:** Escribir en la consola estándar en lugar de emplear el logger parametrizado.
* **❌ Incorrecto:** `e.printStackTrace();` o `System.out.println("Error procesando...");`
* **✅ Correcto:**
  ```java
  private static final Logger log = LoggerFactory.getLogger(MiClase.class);
  // o @Slf4j si está permitido por el módulo
  if (log.isErrorEnabled()) {
    log.error("[ERROR-CONTEXTO] Mensaje parametrizado: {}", detalle, e);
  }
  ```

#### MAN-05: Omisión de la Anotación `@Override` (`java:S1161`)
* **Problema:** Sobrescribir un método de una interfaz o superclase sin colocar la anotación `@Override`. Dificulta refactors futuros y viola las reglas de legibilidad.

#### MAN-06: Números Mágicos en Fechas y Algoritmos (`java:S109`)
* **Problema:** Escribir literales numéricos sin contexto explicativo (ej. meses o umbrales).
* **❌ Incorrecto:** `YearMonth.of(2026, 5)` o `LocalDate.now().minusDays(60)` sin constante.
* **✅ Correcto:** `YearMonth.of(2026, Month.MAY)` o definir `private static final int DIAS_INACTIVIDAD_UMBRAL = 60;`.

---

### 🟢 Categoría 4: Testing y Suites de Prueba (*Test Smells*)

#### TST-01: Tests sin Aserciones (`java:S2699`)
* **Problema:** Escribir un método anotado con `@Test` que ejecuta una invocación pero no contiene ninguna aserción, creando falsa sensación de cobertura.
* **❌ Incorrecto:**
  ```java
  @Test
  void testCargarArchivo() {
    lector.cargar("ruta/archivo.csv"); // No valida nada
  }
  ```
* **✅ Correcto:** Comprobar siempre el estado resultante, o validar que no lanza excepción:
  ```java
  assertDoesNotThrow(() -> lector.cargar("ruta/archivo.csv"));
  // O preferentemente:
  List<Donante> resultado = lector.cargar("ruta/archivo.csv");
  assertFalse(resultado.isEmpty());
  ```

#### TST-02: Modificadores `public` Redundantes en JUnit 5 (`java:S5786`)
* **Problema:** Declarar clases y métodos de test con visibilidad `public`. JUnit 5 no requiere modificadores públicos.
* **❌ Incorrecto:** `public class MiServicioTest { public void testCalcular() { ... } }`
* **✅ Correcto:** Usar visibilidad de paquete por defecto (*package-private*):
  ```java
  class MiServicioTest {
    @Test
    void calcular_conParametrosValidos_debeRetornarResultado() { ... }
  }
  ```

#### TST-03: Cobertura de Ramas Condicionales (*Condition Coverage < 80%*)
* **Problema:** Probar únicamente el "happy path" dejando sin testear las ramas `else`, casos con colecciones vacías o parámetros nulos.
* **✅ Mandato:** Todo `if`, `switch` o filtro `stream()` debe contar con al menos un caso de prueba para la rama afirmativa y otro para la negativa/alternativa.

---

### 🧹 Categoría 5: Limpieza y Modern Java (*Minor / Style*)

#### CLN-01: Imports No Utilizados (`java:S1128`)
* Todo import sobrante debe ser purgado. El comando `mvn spotless:apply` resuelve discrepancias de formateo, pero los imports no usados deben eliminarse activamente.

#### CLN-02: Colecciones Modernas e Inmutabilidad (`java:S6204`, `java:S1168`)
* Reemplazar `stream().collect(Collectors.toList())` por el idiomático `.toList()`.
* Retornar siempre colecciones vacías inmutables (`List.of()`, `Collections.emptyList()`) en lugar de `null`.

---

## 4. Checklist Pre-Flight para Agentes de IA

Antes de considerar concluida una tarea de desarrollo o mantenimiento, el agente **debe auto-auditar su entrega contra esta lista**:

```markdown
### 📋 Pre-Flight SonarCloud Checklist (Comprobación Obligatoria)

#### 1. Seguridad y Workflows CI/CD
- [ ] ¿Se evitó el uso de `${{ github.event... }}` en bloques `run: | bash`, usando variables de entorno intermedias (`env:`)? (SEC-01)
- [ ] ¿Todas las GitHub Actions de terceros están fijadas por hash SHA de 40 caracteres inmutable? (SEC-02)
- [ ] ¿Las llamadas a `curl` en scripts fuerzan `--proto '=https' --proto-redir =https`? (SEC-03)

#### 2. Confiabilidad y Dominio
- [ ] ¿No hay colisiones de nombres ni variable shadowing en variables locales o tests? (REL-01)
- [ ] Si se sobreescribió `equals()`, ¿se sobreescribió también `hashCode()` coherentemente? (REL-02)
- [ ] ¿Se protegieron contra `NullPointerException` todas las cadenas de invocación o llamadas a `Optional`? (REL-03)
- [ ] ¿Se devuelven listas vacías inmutables (`List.of()`) en lugar de `null`? (CLN-02)

#### 3. Mantenibilidad y Diseño
- [ ] ¿Todos los métodos privados de ayuda/validación que no usan `this` están declarados como `private static`? (MAN-01)
- [ ] ¿Las clases de utilidades o factories tienen un constructor `private` explícito? (MAN-02)
- [ ] ¿Se extrajeron los textos duplicados (>= 3 veces) a constantes `private static final String`? (MAN-03)
- [ ] ¿Se eliminó cualquier llamada a `System.out.println` o `e.printStackTrace()`, usando Logger SLF4J? (MAN-04)
- [ ] ¿Todos los métodos que sobreescriben tienen su anotación `@Override`? (MAN-05)

#### 4. Calidad de Pruebas (Test Smells)
- [ ] ¿Todos los métodos `@Test` incluyen al menos una aserción explícita (`assert...`)? (TST-01)
- [ ] ¿Las clases y métodos de test JUnit 5 son package-private (sin modificador `public`)? (TST-02)
- [ ] ¿Se incluyeron pruebas para ramas condicionales alternativas y casos vacíos/nulos para asegurar Condition Coverage >= 80%? (TST-03)

#### 5. Limpieza y Quality Gates
- [ ] ¿Se eliminaron todos los imports no utilizados? (CLN-01)
- [ ] ¿Se ejecutó `mvn spotless:check` (y `mvn spotless:apply` si correspondía)?
- [ ] ¿Se ejecutaron los tests unitarios del módulo afectado (`mvn test -pl <modulo>`) resultando en verde?
```

---

## 5. Gobernanza y Evolución Continua

1. **Adición de Nuevos Errores:** Si en una ejecución de CI/CD SonarCloud reporta una nueva regla violada, el agente o desarrollador que la corrija debe documentarla en este archivo siguiendo la estructura:
   * Código de la regla (`java:SXXXX`).
   * Ejemplo de código incorrecto.
   * Ejemplo de código corregido bajo los patrones de DonaTrack.
2. **Inmutabilidad de Reglas Históricas:** Ningún ítem de este catálogo puede ser flexibilizado o removido si eso implica reducir las exigencias del Quality Gate establecido para la cátedra.
