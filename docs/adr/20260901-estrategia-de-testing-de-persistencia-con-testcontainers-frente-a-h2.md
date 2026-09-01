# Estrategia de Testing de Persistencia con Testcontainers frente a H2

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: testing, persistencia, testcontainers, postgresql, h2, calidad

## Contexto y Problema

Al planificar las pruebas automatizadas de la capa de persistencia relacional (JPA/Hibernate 6 con PostgreSQL 15+ para Fase 2), existe una disyuntiva clásica: utilizar una base de datos embebida en memoria como H2, o utilizar contenedores efímeros mediante la biblioteca **Testcontainers**. El motor H2 emula el dialecto SQL pero carece de soporte nativo o presenta divergencias críticas frente a PostgreSQL en características avanzadas que el proyecto planea utilizar: columnas `JSONB`, sintaxis de funciones de ventana complejas (`ROW_NUMBER() OVER` en rankings), índices parciales, y la cláusula de concurrencia `SELECT ... FOR UPDATE SKIP LOCKED` utilizada por el Transactional Outbox. Probar con H2 genera una falsa sensación de seguridad (*falsos verdes*), ocultando incompatibilidades de esquema que solo estallarían en producción.

## Atributos de Calidad y Drivers de Decisión

* **Fidelidad y Confiabilidad de Pruebas:** Los tests deben ejecutarse exactamente contra el mismo motor de base de datos que se utiliza en preproducción y producción.
* **Testeabilidad:** Capacidad de levantar y destruir instancias de base de datos aisladas y reproducibles para cada suite de integración sin ensuciar datos entre ejecuciones.
* **Mantenibilidad:** Evitar el mantenimiento de dialectos SQL duales (uno para H2 y otro para PostgreSQL).

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 y Oleada 11 de los planes de persistencia técnica ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/donaciones/decisiones_futuras_en_oleada_10.md) §6).
* **Hallazgo:** En el diseño de la persistencia de `incentivos-service`, se comprobó que el cómputo del ranking mensual se delega nativamente a PostgreSQL con window functions y que el Transactional Outbox requiere `SKIP LOCKED`. Probar estos métodos con H2 provocaría errores de sintaxis o emulaciones incompletas.

## Alternativas Consideradas

* **Testcontainers con PostgreSQL 15+ (`postgres:15-alpine`):** Utilizar Testcontainers en las pruebas `@DataJpaTest` / integración para levantar automáticamente un contenedor efímero de PostgreSQL idéntico al de producción.
* **Base de Datos Embebida H2 en Memoria:** Configurar H2 con modo de compatibilidad PostgreSQL (`MODE=PostgreSQL`).
* **Instancia Fija de PostgreSQL Compartida Localmente:** Requerir que cada desarrollador o agente mantenga un servicio PostgreSQL corriendo manualmente en su máquina.

## Resultado de la Decisión

Alternativa elegida: "Testcontainers con PostgreSQL 15+ (`postgres:15-alpine`)"

Justificación:
Testcontainers garantiza fidelidad absoluta al 100% con el motor PostgreSQL real. Elimina los falsos positivos/negativos de H2, permite probar DDLs nativos, índices parciales y transacciones concurrentes con `SKIP LOCKED`. Se integra nativamente con Spring Boot mediante `@DynamicPropertySource` y con el pipeline de GitHub Actions ya existente.

### Consecuencias Positivas

* Pruebas de persistencia verdaderamente fidedignas; lo que pasa en test pasa idénticamente en producción.
* Aislamiento total entre tests: cada suite o contexto obtiene un esquema limpio y determinista.
* Unificación del dialecto SQL en un único archivo DDL canónico de PostgreSQL.

### Consecuencias Negativas

* Requiere que el entorno de desarrollo tenga Docker disponible (en entornos sin Docker, la suite de persistencia se clasifica como `[DEFERRED_NO_DOCKER]`).
* Tiempo de arranque inicial ligeramente superior al de una base puramente en memoria Java.

### Validación

Se valida mediante:
1. Clases de prueba de persistencia que utilizan `@Testcontainers` y `@Container static PostgreSQLContainer<?> postgres`.
2. Verificación de ejecución exitosa de queries nativas con `JSONB` y `SKIP LOCKED` en CI/CD.

## Análisis de Alternativas

### Testcontainers con PostgreSQL 15+

#### Pros
* Paridad 1:1 con producción.
* Soporte nativo de todas las extensiones y funciones avanzadas de Postgres.
* Limpieza automática de recursos al finalizar los tests.

#### Contras
* Dependencia del demonio de Docker.

### Base de Datos Embebida H2

#### Pros
* Arranca en milisegundos sin requerir Docker.

#### Contras
* Modo de compatibilidad Postgres incompleto: no soporta `SKIP LOCKED` ni `JSONB` de forma nativa.
* Alto riesgo de aprobar tests que fallarán al conectar a PostgreSQL real.

### Instancia Fija Compartida

#### Pros
* Ejecuta sobre Postgres real sin overhead de Testcontainers.

#### Contras
* Colisión de datos entre suites de prueba concurrentes.
* Frágil y difícil de orquestar en pipelines de CI/CD.