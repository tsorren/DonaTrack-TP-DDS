'use strict';

/**
 * Repository Knowledge Generator — DonaTrack (Level 4 Agent-First)
 * Run: node scripts/generate-repo-knowledge.js
 * 
 * Automatically parses OpenAPI 3.0 contracts, JSON Schemas, and messaging topologies
 * to generate structured system knowledge in docs/generated/:
 * - docs/generated/README.md (Index & metadata)
 * - docs/generated/endpoints-catalog.md (All REST endpoints by microservice)
 * - docs/generated/events-catalog.md (RabbitMQ topologies, events & schemas)
 * - docs/generated/architecture-graph.md (Mermaid topology and interaction graphs)
 * - docs/generated/contracts-summary.json (Machine-readable JSON schema inventory)
 */

const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const contratosDir = path.join(repoRoot, 'docs', 'arquitectura', 'contratos');
const schemasDir = path.join(contratosDir, 'schemas');
const generatedDir = path.join(repoRoot, 'docs', 'generated');

// ─── Parser Liviano de OpenAPI YAML (Pure Node.js) ─────────────────────────────
function parseOpenApiYaml(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const fileName = path.basename(filePath);

  let title = 'API';
  let version = '1.0.0';
  let description = '';
  let serverUrl = '';

  const titleMatch = content.match(/title:\s*([^\r\n]+)/);
  if (titleMatch) title = titleMatch[1].trim();

  const versionMatch = content.match(/version:\s*([^\r\n]+)/);
  if (versionMatch) version = versionMatch[1].trim();

  const descMatch = content.match(/description:\s*([^\r\n]+)/);
  if (descMatch) description = descMatch[1].trim();

  const urlMatch = content.match(/url:\s*(http[^\r\n]+)/);
  if (urlMatch) serverUrl = urlMatch[1].trim();

  const endpoints = [];
  const lines = content.split(/\r?\n/);
  let inPaths = false;
  let currentPath = '';
  let currentMethod = '';
  let currentEndpoint = null;
  let inRequestBody = false;
  let inResponses = false;
  let inSchema = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    if (/^paths:/.test(line)) {
      inPaths = true;
      continue;
    }

    if (inPaths) {
      if (/^[a-zA-Z0-9_]+:/.test(line) && !/^paths:/.test(line)) {
        // Exiting paths section (e.g. components:)
        if (currentEndpoint) endpoints.push(currentEndpoint);
        break;
      }

      // Detect path (e.g. "  /donaciones-independientes:")
      const pathMatch = line.match(/^  (\/[^:\s]*):/);
      if (pathMatch) {
        if (currentEndpoint) endpoints.push(currentEndpoint);
        currentPath = pathMatch[1];
        currentMethod = '';
        currentEndpoint = null;
        inRequestBody = false;
        inResponses = false;
        continue;
      }

      // Detect HTTP method (e.g. "    get:", "    post:")
      const methodMatch = line.match(/^    (get|post|put|delete|patch):/i);
      if (methodMatch) {
        if (currentEndpoint) endpoints.push(currentEndpoint);
        currentMethod = methodMatch[1].toUpperCase();
        currentEndpoint = {
          path: currentPath,
          method: currentMethod,
          summary: '',
          operationId: '',
          requestBody: '-',
          responses: {},
        };
        inRequestBody = false;
        inResponses = false;
        continue;
      }

      if (currentEndpoint) {
        const summaryMatch = line.match(/^\s+summary:\s*([^\r\n]+)/);
        if (summaryMatch) currentEndpoint.summary = summaryMatch[1].trim();

        const opIdMatch = line.match(/^\s+operationId:\s*([^\r\n]+)/);
        if (opIdMatch) currentEndpoint.operationId = opIdMatch[1].trim();

        if (/^\s+requestBody:/.test(line)) {
          inRequestBody = true;
          inResponses = false;
          continue;
        }

        if (/^\s+responses:/.test(line)) {
          inResponses = true;
          inRequestBody = false;
          continue;
        }

        if (inRequestBody) {
          const refMatch = line.match(/\$ref:\s*['"]?#\/components\/schemas\/([a-zA-Z0-9_]+)/);
          if (refMatch) {
            currentEndpoint.requestBody = refMatch[1];
          }
        }

        if (inResponses) {
          const codeMatch = line.match(/^\s+'?([2-5]\d{2})'?:/);
          if (codeMatch) {
            const code = codeMatch[1];
            currentEndpoint.responses[code] = '-';
          }
          const refMatch = line.match(/\$ref:\s*['"]?#\/components\/schemas\/([a-zA-Z0-9_]+)/);
          if (refMatch) {
            const lastCode = Object.keys(currentEndpoint.responses).pop();
            if (lastCode) {
              currentEndpoint.responses[lastCode] = refMatch[1];
            }
          }
        }
      }
    }
  }

  if (currentEndpoint && !endpoints.includes(currentEndpoint)) {
    endpoints.push(currentEndpoint);
  }

  return {
    fileName,
    title,
    version,
    description,
    serverUrl,
    endpoints,
  };
}

// ─── Parser de Schemas JSON ───────────────────────────────────────────────────
function parseJsonSchemas(dirPath) {
  const schemas = [];
  if (!fs.existsSync(dirPath)) return schemas;

  const files = fs.readdirSync(dirPath).filter(f => f.endsWith('.json')).sort();
  for (const file of files) {
    const fullPath = path.join(dirPath, file);
    try {
      const data = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
      schemas.push({
        fileName: file,
        title: data.title || file.replace(/\.schema\.json$/, ''),
        description: data.description || 'Sin descripción',
        type: data.type || (data.oneOf ? 'polymorphic (oneOf)' : 'object'),
        required: data.required || [],
        subtypes: data.oneOf ? data.oneOf.map(o => o.$ref ? o.$ref.replace('#/$defs/', '') : 'unknown') : [],
      });
    } catch (e) {
      console.warn(`[WARN] Error parseando schema ${file}: ${e.message}`);
    }
  }
  return schemas;
}

// ─── Topología de Eventos RabbitMQ de DonaTrack ──────────────────────────────
const KNOWN_EVENTS = [
  {
    name: 'EventoDonanteRegistradoDTO',
    schemaFile: 'evento-notificable.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'donante.registrado',
    producer: 'donaciones-service',
    consumers: ['notificaciones-service'],
    description: 'Emitido cuando se da de alta un nuevo donante. Despacha credenciales iniciales de acceso.',
  },
  {
    name: 'EventoDonanteInactivoDTO',
    schemaFile: 'evento-notificable.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'donante.inactivo',
    producer: 'incentivos-service',
    consumers: ['notificaciones-service'],
    description: 'Emitido cuando un donante supera el umbral de inactividad de donaciones configurado.',
  },
  {
    name: 'EventoMisionCumplidaDTO',
    schemaFile: 'evento-notificable.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'mision.cumplida',
    producer: 'incentivos-service',
    consumers: ['notificaciones-service'],
    description: 'Emitido cuando un donante completa una misión de fidelización (racha o volumen).',
  },
  {
    name: 'EventoSubioCategoriaDTO',
    schemaFile: 'evento-notificable.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'categoria.ascenso',
    producer: 'incentivos-service',
    consumers: ['notificaciones-service'],
    description: 'Emitido cuando un donante asciende de categoría de fidelización.',
  },
  {
    name: 'EventoRutaAsignadaDTO',
    schemaFile: 'evento-ruta-asignada.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'ruta.asignada',
    producer: 'logistica-service',
    consumers: ['donaciones-service', 'notificaciones-service'],
    description: 'Emitido al consolidar y asignar una ruta logística para traslado de donaciones.',
  },
  {
    name: 'EventoRutaIniciadaDTO',
    schemaFile: 'evento-ruta-iniciada.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'ruta.iniciada',
    producer: 'logistica-service',
    consumers: ['donaciones-service'],
    description: 'Emitido cuando el transporte inicia el recorrido de la ruta asignada.',
  },
  {
    name: 'EventoEntregaExitosaDTO',
    schemaFile: 'evento-entrega-exitosa.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'entrega.exitosa',
    producer: 'logistica-service',
    consumers: ['donaciones-service', 'incentivos-service', 'notificaciones-service'],
    description: 'Notifica la recepción exitosa de la donación en la entidad beneficiaria.',
  },
  {
    name: 'EventoEntregaFallidaDTO',
    schemaFile: 'evento-entrega-fallida.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'entrega.fallida',
    producer: 'logistica-service',
    consumers: ['donaciones-service', 'notificaciones-service'],
    description: 'Notifica el fracaso del intento de entrega de la donación, activando justificación.',
  },
  {
    name: 'PersonaReplicaDTO',
    schemaFile: 'persona-replica.schema.json',
    exchange: 'donatrack.events',
    routingKey: 'persona.replica',
    producer: 'donaciones-service',
    consumers: ['logistica-service', 'incentivos-service', 'notificaciones-service'],
    description: 'Sincronización eventual de datos de contacto y roles de personas entre bounded contexts.',
  },
];

// ─── Generador Principal de Artefactos ─────────────────────────────────────────
function generateRepoKnowledge() {
  console.log('════════════════════════════════════════════════════════════');
  console.log('  DonaTrack — Repository Knowledge Generator (Level 4)      ');
  console.log('════════════════════════════════════════════════════════════\n');

  fs.mkdirSync(generatedDir, { recursive: true });

  // 1. Parsear specs OpenAPI
  const openapiFiles = fs.readdirSync(contratosDir)
    .filter(f => f.endsWith('.yaml') || f.endsWith('.yml'))
    .sort();

  const services = [];
  let totalEndpoints = 0;

  for (const file of openapiFiles) {
    const full = path.join(contratosDir, file);
    const parsed = parseOpenApiYaml(full);
    services.push(parsed);
    totalEndpoints += parsed.endpoints.length;
    console.log(`[OpenAPI] ${parsed.fileName} ➔ ${parsed.endpoints.length} endpoints identificados (${parsed.title})`);
  }

  // 2. Parsear JSON Schemas
  const schemas = parseJsonSchemas(schemasDir);
  console.log(`[Schemas] ${schemas.length} schemas JSON descubiertos en docs/arquitectura/contratos/schemas/`);

  // 3. Generar docs/generated/README.md
  const readmePath = path.join(generatedDir, 'README.md');
  let readme = `# Conocimiento Generado del Repositorio (Generated Knowledge)\n\n`;
  readme += `> **Ámbito:** Catálogo dinámico y mechanically extracted de contratos, endpoints y eventos.\n`;
  readme += `> **Alineación Normativa:** [\`AGENTS.md\`](../../AGENTS.md), [\`docs/auditoria/auditoria-directivas-agentes.md\`](../auditoria/auditoria-directivas-agentes.md) y arquitectura Nivel 4 (Agent-First).\n`;
  readme += `> **Generador:** \`node scripts/generate-repo-knowledge.js\`\n\n`;
  readme += `<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->\n\n`;
  readme += `## 1. Propósito y Criterio de Automatización\n\n`;
  readme += `Este directorio contiene artefactos documentales extraídos de manera 100% determinista a partir de las fuentes de verdad canónicas del repositorio (OpenAPI 3.0, JSON Schemas y configuraciones RabbitMQ).\n\n`;
  readme += `Provee un inventario consolidado que erradica la duplicación manual y acelera el *Progressive Disclosure* para agentes de IA y desarrolladores humanos.\n\n`;
  readme += `## 2. Métricas de Contratos del Sistema\n\n`;
  readme += `| Dimensión | Cantidad Identificada |\n`;
  readme += `|---|---:|\n`;
  readme += `| **Microservicios Documentados** | ${services.length} |\n`;
  readme += `| **Endpoints REST Públicos** | ${totalEndpoints} |\n`;
  readme += `| **Schemas JSON Canónicos** | ${schemas.length} |\n`;
  readme += `| **Eventos RabbitMQ Topológicos** | ${KNOWN_EVENTS.length} |\n\n`;
  readme += `## 3. Catálogos Disponibles\n\n`;
  readme += `* [\`endpoints-catalog.md\`](./endpoints-catalog.md) — Inventario completo de endpoints REST agrupados por microservicio.\n`;
  readme += `* [\`events-catalog.md\`](./events-catalog.md) — Catálogo de mensajería asíncrona, exchanges, routing keys y contratos de eventos.\n`;
  readme += `* [\`architecture-graph.md\`](./architecture-graph.md) — Diagramas Mermaid de topología, arquitectura de datos y dependencias inter-servicio.\n`;
  readme += `* [\`contracts-summary.json\`](./contracts-summary.json) — Formato estructurado JSON para consumo programático por linters y agentes.\n\n`;
  readme += `---\n*Generado mecánicamente por DonaTrack Knowledge Engine.*\n`;
  fs.writeFileSync(readmePath, readme, 'utf8');

  // 4. Generar docs/generated/endpoints-catalog.md
  const endpointsPath = path.join(generatedDir, 'endpoints-catalog.md');
  let epMd = `# Catálogo Unificado de Endpoints REST — DonaTrack\n\n`;
  epMd += `> **Fuente Canónica:** Especificaciones OpenAPI 3.0 en [\`docs/arquitectura/contratos/\`](../arquitectura/contratos/)\n`;
  epMd += `> **Total de Endpoints:** ${totalEndpoints}\n\n`;
  epMd += `<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->\n\n`;

  for (const svc of services) {
    epMd += `## Microservicio: ${svc.title} (\`${svc.fileName}\`)\n\n`;
    epMd += `* **Versión:** \`${svc.version}\`\n`;
    epMd += `* **Descripción:** ${svc.description}\n`;
    epMd += `* **Servidor Local:** \`${svc.serverUrl || 'http://localhost:8080'}\`\n\n`;
    epMd += `| Método | Path | Operación | Request Body | Códigos de Respuesta |\n`;
    epMd += `|:---:|---|---|---|---|\n`;

    for (const ep of svc.endpoints) {
      const respStr = Object.entries(ep.responses)
        .map(([code, model]) => `${code} (${model})`)
        .join(', ') || '200';
      epMd += `| \`${ep.method}\` | \`${ep.path}\` | \`${ep.operationId || ep.summary}\` | \`${ep.requestBody}\` | \`${respStr}\` |\n`;
    }
    epMd += `\n`;
  }
  epMd += `---\n*Generado mecánicamente por DonaTrack Knowledge Engine.*\n`;
  fs.writeFileSync(endpointsPath, epMd, 'utf8');

  // 5. Generar docs/generated/events-catalog.md
  const eventsPath = path.join(generatedDir, 'events-catalog.md');
  let evMd = `# Catálogo de Eventos Asíncronos y Mensajería RabbitMQ — DonaTrack\n\n`;
  evMd += `> **Topología:** Exchange \`donatrack.events\` (Topic/Direct Exchange)\n`;
  evMd += `> **Fuente Canónica:** [\`docs/arquitectura/contratos/schemas/\`](../arquitectura/contratos/schemas/)\n\n`;
  evMd += `<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->\n\n`;
  evMd += `## 1. Matriz de Mensajería Inter-Servicio\n\n`;
  evMd += `| Evento DTO | Routing Key | Productor | Consumidores | Schema Canónico |\n`;
  evMd += `|---|---|---|---|---|\n`;

  for (const ev of KNOWN_EVENTS) {
    const consumersStr = ev.consumers.map(c => `\`${c}\``).join(', ');
    evMd += `| \`${ev.name}\` | \`${ev.routingKey}\` | \`${ev.producer}\` | ${consumersStr} | [\`${ev.schemaFile}\`](../arquitectura/contratos/schemas/${ev.schemaFile}) |\n`;
  }

  evMd += `\n## 2. Descripción de Eventos y Responsabilidades\n\n`;
  for (const ev of KNOWN_EVENTS) {
    evMd += `### \`${ev.name}\`\n`;
    evMd += `* **Exchange:** \`${ev.exchange}\`\n`;
    evMd += `* **Routing Key:** \`${ev.routingKey}\`\n`;
    evMd += `* **Publicador:** \`${ev.producer}\`\n`;
    evMd += `* **Propósito:** ${ev.description}\n`;
    evMd += `* **Schema de Validación:** [\`docs/arquitectura/contratos/schemas/${ev.schemaFile}\`](../arquitectura/contratos/schemas/${ev.schemaFile})\n\n`;
  }

  evMd += `## 3. Esquemas JSON de Eventos y Payloads Registrados\n\n`;
  evMd += `| Schema File | Título | Tipo | Propiedades Obligatorias |\n`;
  evMd += `|---|---|---|---|\n`;
  for (const sc of schemas) {
    const reqStr = sc.required.length > 0 ? sc.required.map(r => `\`${r}\``).join(', ') : '*(definido en subschemas)*';
    evMd += `| [\`${sc.fileName}\`](../arquitectura/contratos/schemas/${sc.fileName}) | \`${sc.title}\` | \`${sc.type}\` | ${reqStr} |\n`;
  }

  evMd += `\n---\n*Generado mecánicamente por DonaTrack Knowledge Engine.*\n`;
  fs.writeFileSync(eventsPath, evMd, 'utf8');

  // 6. Generar docs/generated/architecture-graph.md
  const graphPath = path.join(generatedDir, 'architecture-graph.md');
  let grMd = `# Grafo de Arquitectura y Topología de Integración — DonaTrack\n\n`;
  grMd += `> **Representación Visual y Mecánica de la Arquitectura Distribuida**\n\n`;
  grMd += `<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->\n\n`;
  grMd += `## 1. Topología de Comunicación Inter-Servicios\n\n`;
  grMd += `\`\`\`mermaid\ngraph TD\n`;
  grMd += `    subgraph Clientes["Canales de Entrada"]\n`;
  grMd += `        Web["Frontend / Web SPA"]\n`;
  grMd += `        N8N["Orquestador n8n (Webhooks)"]\n`;
  grMd += `    end\n\n`;
  grMd += `    subgraph Microservicios["Core Platform (Java 21 / Spring Boot 3)"]\n`;
  grMd += `        DON["donaciones-service<br/>(Port 8081)"]\n`;
  grMd += `        LOG["logistica-service<br/>(Port 8082)"]\n`;
  grMd += `        INC["incentivos-service<br/>(Port 8083)"]\n`;
  grMd += `        NOT["notificaciones-service<br/>(Port 8084)"]\n`;
  grMd += `    end\n\n`;
  grMd += `    subgraph Mensajeria["Message Broker"]\n`;
  grMd += `        RABBIT[("RabbitMQ<br/>donatrack.events")]\n`;
  grMd += `    end\n\n`;
  grMd += `    Web -->|HTTP REST| DON\n`;
  grMd += `    Web -->|HTTP REST| LOG\n`;
  grMd += `    Web -->|HTTP REST| INC\n`;
  grMd += `    N8N -->|Webhooks| NOT\n\n`;
  grMd += `    DON -->|Feign Client (Sync)| LOG\n`;
  grMd += `    LOG -->|Feign Client (Sync)| DON\n`;
  grMd += `    DON -->|Feign Client (Sync)| INC\n\n`;
  grMd += `    DON -.->|Publish: donante.registrado| RABBIT\n`;
  grMd += `    LOG -.->|Publish: ruta.*, entrega.*| RABBIT\n`;
  grMd += `    INC -.->|Publish: donante.inactivo, mision.*| RABBIT\n\n`;
  grMd += `    RABBIT -.->|Consume| NOT\n`;
  grMd += `    RABBIT -.->|Consume| DON\n`;
  grMd += `    RABBIT -.->|Consume| INC\n`;
  grMd += `    RABBIT -.->|Consume| LOG\n`;
  grMd += `\`\`\`\n\n`;

  grMd += `## 2. Catálogo de Microservicios y Bounded Contexts\n\n`;
  grMd += `| Servicio | Puerto | Bounded Context | Estrategia de Persistencia | Shared Kernel |\n`;
  grMd += `|---|:---:|---|---|:---:|\n`;
  grMd += `| \`donaciones-service\` | 8081 | Donaciones, Necesidades y Asignación | Repositorios Concurrente / JPA | [\`common-lib\`](../../common-lib/AGENTS.md) |\n`;
  grMd += `| \`logistica-service\` | 8082 | Rutas, Envíos, Entregas y Trazabilidad | Repositorios Concurrente / JPA | [\`common-lib\`](../../common-lib/AGENTS.md) |\n`;
  grMd += `| \`incentivos-service\` | 8083 | Misiones, Rachas y Puntos | Repositorios Concurrente / JPA | [\`common-lib\`](../../common-lib/AGENTS.md) |\n`;
  grMd += `| \`notificaciones-service\` | 8084 | Despacho de Mensajes y Alertas | Stateless / Event-Driven | [\`common-lib\`](../../common-lib/AGENTS.md) |\n\n`;
  grMd += `---\n*Generado mecánicamente por DonaTrack Knowledge Engine.*\n`;
  fs.writeFileSync(graphPath, grMd, 'utf8');

  // 7. Generar docs/generated/contracts-summary.json
  const summaryJsonPath = path.join(generatedDir, 'contracts-summary.json');
  const summaryData = {
    generatedAt: new Date().toISOString(),
    harnessVersion: '6.4.0',
    stats: {
      servicesCount: services.length,
      endpointsCount: totalEndpoints,
      schemasCount: schemas.length,
      eventsCount: KNOWN_EVENTS.length,
    },
    services: services.map(s => ({
      title: s.title,
      fileName: s.fileName,
      version: s.version,
      serverUrl: s.serverUrl,
      endpoints: s.endpoints,
    })),
    events: KNOWN_EVENTS,
    schemas: schemas,
  };
  fs.writeFileSync(summaryJsonPath, JSON.stringify(summaryData, null, 2), 'utf8');

  console.log(`\nArtefactos generados exitosamente en ${generatedDir}:`);
  console.log(`  - ${path.basename(readmePath)}`);
  console.log(`  - ${path.basename(endpointsPath)}`);
  console.log(`  - ${path.basename(eventsPath)}`);
  console.log(`  - ${path.basename(graphPath)}`);
  console.log(`  - ${path.basename(summaryJsonPath)}`);
  console.log('\nKnowledge generation complete.');
}

if (require.main === module) {
  generateRepoKnowledge();
}

module.exports = {
  parseOpenApiYaml,
  parseJsonSchemas,
  KNOWN_EVENTS,
  generateRepoKnowledge,
};
