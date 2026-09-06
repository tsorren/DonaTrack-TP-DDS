# Original User Request

## 2026-09-06T05:10:58Z

Ejecutar una auditoría crítica, adversaria e integral sobre los 173 documentos Markdown del repositorio DonaTrack (Java 21 / Spring Boot 3 multi-módulo), contrastando cada aseveración técnica, firma, entidad, contrato REST/AMQP y configuración contra el código fuente real mediante búsqueda semántica obligatoria con `grepai`, orquestando subagentes en paralelo y garantizando cero divergencias de documentación o enlaces rotos.

Working directory: c:\IdeaProjects\DonaTrack-TP-DDS
Integrity mode: development

## Requirements

### R1. Auditoría Adversarial Segmentada por Subdominios
El sistema debe segmentar la totalidad de los 173 archivos Markdown en frentes de trabajo concurrentes delegados a subagentes especializados, abarcando:
1. Arquitectura Núcleo y Shared Kernel (docs/arquitectura/*, common-lib/AGENTS.md, raíz).
2. Diseño, Bitácoras de Oleadas, CI/CD, DevOps y Testing (docs/arquitectura/diseno/*, docs/auditoria/*, docs/cicd/*, docs/testing/*, .github/scripts/*).
3. Arquitectura de Decisiones (ADRs) (docs/adr/* y subdirectorios de microservicios).
4. Guías de IA, Prompts, Evals y Gobernanza (docs/IA/*).

### R2. Uso Estricto de Búsqueda Semántica con grepai
Toda validación de entidades, Value Objects, interfaces, enums, endpoints REST, routing keys AMQP, interceptores y propiedades de configuración debe verificarse obligatoriamente mediante herramientas MCP de `grepai` (`grepai_search`) contra el código fuente Java real, prohibiendo inferencias o suposiciones no respaldadas por código.

### R3. Preservación Invariante de Registros Históricos y Gobernanza
Conforme a AGENTS.md, las justificaciones y decisiones históricas de cátedra y ADRs aprobados son inmutables (solo se permite la corrección de hipervínculos, rutas relativas y sintaxis de renderizado). Se debe mantener la taxonomía epistémica ([OBSERVED], [DOCUMENTED], [VERIFIED], etc.) en cada reporte.

## Acceptance Criteria

### Integridad Referencial y Sintáctica
- [ ] python scripts/validate_docs_links.py ejecuta con 0 enlaces rotos (0 broken links).
- [ ] Ningún archivo Markdown posee bloques de código (```) sin cerrar ni encabezados malformados.
- [ ] Todos los enlaces entre ADRs y la raíz (AGENTS.md) resuelven con rutas relativas correctas.

### Consistencia de Contratos y Código Fuente
- [ ] node scripts/validate-contracts.js aprueba el 100% de los checks (79/79 PASS, 0 FAIL).
- [ ] node scripts/agent-check.js y node scripts/tests/run-tests.js aprueban todos los checks de gobernanza (86 PASS, 0 FAIL).
- [ ] Ninguna discrepancia entre endpoints descritos en Markdown y anotaciones @RestController / @RequestMapping en Java.
- [ ] mvn spotless:check finaliza con BUILD SUCCESS en los 7 módulos.
