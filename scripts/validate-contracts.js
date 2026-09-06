'use strict';

/**
 * Script de Validación Mecánica de Contratos REST, OpenAPI y Schemas JSON
 * Run: node scripts/validate-contracts.js
 * 
 * Verifica:
 * 1. Integridad estructural y sintáctica de todos los JSON Schemas en docs/arquitectura/contratos/schemas/
 * 2. Existencia y formato de especificaciones OpenAPI 3.0 en docs/arquitectura/contratos/
 * 3. Validación de payloads válidos e inválidos contra los schemas canónicos (motor puramente nativo)
 */

const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const schemasDir = path.join(repoRoot, 'docs', 'arquitectura', 'contratos', 'schemas');
const contratosDir = path.join(repoRoot, 'docs', 'arquitectura', 'contratos');

let passed = 0;
let failed = 0;

function assert(name, condition, extraInfo) {
  if (condition) {
    console.log(`  [PASS] ${name}`);
    passed++;
  } else {
    console.error(`  [FAIL] ${name} ${extraInfo ? '— ' + extraInfo : ''}`);
    failed++;
  }
}

// ─── Motor liviano de validación de JSON Schema (Pure Node.js) ─────────────
const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const DATE_TIME_REGEX = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})?$/;

function validateSchemaObject(schema, data, pathName = 'root', rootSchema = null) {
  if (!rootSchema) rootSchema = schema;

  // Resolución de $ref (#/$defs/...)
  if (schema.$ref) {
    if (schema.$ref.startsWith('#/$defs/')) {
      const defName = schema.$ref.replace('#/$defs/', '');
      const target = rootSchema.$defs && rootSchema.$defs[defName];
      if (!target) return { valid: false, error: `${pathName}: unresolved $ref '${schema.$ref}'` };
      return validateSchemaObject(target, data, pathName, rootSchema);
    }
    return { valid: false, error: `${pathName}: unsupported $ref format '${schema.$ref}'` };
  }

  // Soporte para const
  if (schema.const !== undefined) {
    if (data !== schema.const) {
      return { valid: false, error: `${pathName}: expected const '${schema.const}', got '${data}'` };
    }
  }

  // Soporte para oneOf
  if (schema.oneOf) {
    const matches = [];
    let lastError = null;
    for (const sub of schema.oneOf) {
      const res = validateSchemaObject(sub, data, pathName, rootSchema);
      if (res.valid) {
        matches.push(sub);
      } else {
        lastError = res.error;
      }
    }
    if (matches.length === 1) return { valid: true };
    if (matches.length === 0) return { valid: false, error: `${pathName}: does not match any oneOf schema (${lastError})` };
    return { valid: false, error: `${pathName}: matched multiple oneOf schemas (${matches.length})` };
  }

  // Tipos permitidos (string o array de strings como ["string", "null"])
  const allowedTypes = Array.isArray(schema.type) ? schema.type : (schema.type ? [schema.type] : []);

  if (allowedTypes.includes('null') && data === null) {
    return { valid: true };
  }

  if (allowedTypes.includes('object') || schema.type === 'object' || schema.properties) {
    if (typeof data !== 'object' || data === null || Array.isArray(data)) {
      return { valid: false, error: `${pathName}: expected object, got ${data === null ? 'null' : Array.isArray(data) ? 'array' : typeof data}` };
    }
    if (schema.required) {
      for (const req of schema.required) {
        if (!(req in data) || data[req] === undefined) {
          return { valid: false, error: `${pathName}: missing required property '${req}'` };
        }
      }
    }
    if (schema.properties) {
      for (const [key, val] of Object.entries(data)) {
        if (schema.properties[key]) {
          const res = validateSchemaObject(schema.properties[key], val, `${pathName}.${key}`, rootSchema);
          if (!res.valid) return res;
        } else if (schema.additionalProperties === false) {
          return { valid: false, error: `${pathName}: additional property '${key}' not allowed` };
        }
      }
    }
    return { valid: true };
  }

  if (allowedTypes.includes('string')) {
    if (typeof data !== 'string') {
      return { valid: false, error: `${pathName}: expected string, got ${typeof data}` };
    }
    if (schema.enum && !schema.enum.includes(data)) {
      return { valid: false, error: `${pathName}: '${data}' is not in enum [${schema.enum.join(', ')}]` };
    }
    if (schema.minLength !== undefined && data.length < schema.minLength) {
      return { valid: false, error: `${pathName}: length ${data.length} < minLength ${schema.minLength}` };
    }
    if (schema.maxLength !== undefined && data.length > schema.maxLength) {
      return { valid: false, error: `${pathName}: length ${data.length} > maxLength ${schema.maxLength}` };
    }
    if (schema.format === 'uuid') {
      if (!UUID_REGEX.test(data)) {
        return { valid: false, error: `${pathName}: invalid UUID format '${data}'` };
      }
    }
    if (schema.format === 'date-time') {
      if (!DATE_TIME_REGEX.test(data) || isNaN(Date.parse(data))) {
        return { valid: false, error: `${pathName}: invalid date-time format '${data}'` };
      }
    }
    if (schema.format === 'uri') {
      try {
        new URL(data);
      } catch (e) {
        return { valid: false, error: `${pathName}: invalid URI format '${data}'` };
      }
    }
    return { valid: true };
  }

  if (allowedTypes.includes('number') || allowedTypes.includes('integer')) {
    if (typeof data !== 'number' || isNaN(data) || (allowedTypes.includes('integer') && !allowedTypes.includes('number') && !Number.isInteger(data))) {
      return { valid: false, error: `${pathName}: expected number/integer, got ${typeof data}` };
    }
    if (schema.exclusiveMinimum !== undefined && data <= schema.exclusiveMinimum) {
      return { valid: false, error: `${pathName}: ${data} <= exclusiveMinimum ${schema.exclusiveMinimum}` };
    }
    if (schema.minimum !== undefined && data < schema.minimum) {
      return { valid: false, error: `${pathName}: ${data} < minimum ${schema.minimum}` };
    }
    if (schema.maximum !== undefined && data > schema.maximum) {
      return { valid: false, error: `${pathName}: ${data} > maximum ${schema.maximum}` };
    }
    return { valid: true };
  }

  if (allowedTypes.includes('boolean')) {
    if (typeof data !== 'boolean') {
      return { valid: false, error: `${pathName}: expected boolean, got ${typeof data}` };
    }
    return { valid: true };
  }

  if (allowedTypes.includes('array')) {
    if (!Array.isArray(data)) {
      return { valid: false, error: `${pathName}: expected array, got ${typeof data}` };
    }
    if (schema.items) {
      for (let i = 0; i < data.length; i++) {
        const res = validateSchemaObject(schema.items, data[i], `${pathName}[${i}]`, rootSchema);
        if (!res.valid) return res;
      }
    }
    return { valid: true };
  }

  return { valid: true };
}

console.log('\n════════════════════════════════════════════════════════════');
console.log('  DonaTrack — Suite de Validación de Contratos y Schemas   ');
console.log('════════════════════════════════════════════════════════════\n');

// 1. Validar existencia y sintaxis de JSON Schemas
console.log('[1] Validación Estructural de JSON Schemas:');
if (!fs.existsSync(schemasDir)) {
  assert('schemas_dir_exists', false, `Directorio no encontrado: ${schemasDir}`);
} else {
  const schemaFiles = fs.readdirSync(schemasDir).filter(f => f.endsWith('.schema.json'));
  assert('schema_files_count', schemaFiles.length >= 11, `Encontrados ${schemaFiles.length} schemas`);

  for (const file of schemaFiles) {
    const fullPath = path.join(schemasDir, file);
    try {
      const parsed = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
      assert(`schema_syntax_${file}`, true);
      assert(`schema_fields_${file}`, parsed.$schema && ((parsed.type && parsed.properties) || parsed.oneOf), 'Campos base o oneOf presentes');
      if (parsed.required && parsed.properties) {
        const allPresent = parsed.required.every(r => r in parsed.properties);
        assert(`schema_required_valid_${file}`, allPresent, 'Todos los campos required existen en properties');
      }
    } catch (err) {
      assert(`schema_syntax_${file}`, false, err.message);
    }
  }
}

// 2. Validar especificaciones OpenAPI 3.0 YAML
console.log('\n[2] Validación de Especificaciones OpenAPI 3.0:');
const expectedOpenApis = [
  'openapi-donaciones.yaml',
  'openapi-logistica.yaml',
  'openapi-incentivos.yaml',
  'openapi-notificaciones.yaml'
];

for (const oasFile of expectedOpenApis) {
  const fullPath = path.join(contratosDir, oasFile);
  const exists = fs.existsSync(fullPath);
  assert(`openapi_exists_${oasFile}`, exists);
  if (exists) {
    const content = fs.readFileSync(fullPath, 'utf8');
    assert(`openapi_version_${oasFile}`, content.includes('openapi: 3.0.'), 'Versión OpenAPI 3.0 declarada');
    assert(`openapi_paths_${oasFile}`, content.includes('paths:') && content.includes('/'), 'Rutas y endpoints declarados');
    assert(`openapi_info_${oasFile}`, content.includes('info:') && content.includes('title:'), 'Metadatos info presentes');
  }
}

// 3. Validación de Payloads Funcionales contra Schemas (Cobertura 11/11)
console.log('\n[3] Validación de Payloads Funcionales contra Schemas (11/11 Schemas):');

// 1. cambio-estado-donacion-request
const cambioEstadoDonacionSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'cambio-estado-donacion-request.schema.json'), 'utf8'));
assert('cambio_estado_donacion_valido', validateSchemaObject(cambioEstadoDonacionSchema, {
  estado: 'EN_TRASLADO',
  justificacion: 'Ruta 1 asignada',
  replanificable: false
}).valid);
assert('cambio_estado_donacion_invalido_enum', !validateSchemaObject(cambioEstadoDonacionSchema, {
  estado: 'ESTADO_INVENTADO'
}).valid);
assert('cambio_estado_donacion_invalido_missing', !validateSchemaObject(cambioEstadoDonacionSchema, {
  justificacion: 'Sin estado'
}).valid);

// 2. cambio-estado-entrega-request
const cambioEstadoEntregaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'cambio-estado-entrega-request.schema.json'), 'utf8'));
assert('cambio_estado_entrega_valido', validateSchemaObject(cambioEstadoEntregaSchema, {
  estado: 'EN_TRASLADO',
  actor: 'chofer-1',
  replanificable: false
}).valid);
assert('cambio_estado_entrega_invalido_enum', !validateSchemaObject(cambioEstadoEntregaSchema, {
  estado: 'NO_EXISTE'
}).valid);
assert('cambio_estado_entrega_invalido_missing', !validateSchemaObject(cambioEstadoEntregaSchema, {
  actor: 'chofer-1'
}).valid);

// 3. crear-entrega-request
const crearEntregaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'crear-entrega-request.schema.json'), 'utf8'));
assert('crear_entrega_valido', validateSchemaObject(crearEntregaSchema, {
  idDonacion: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  idBeneficiaria: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  destino: {
    calle: 'Av. Medrano',
    altura: 951,
    codigoPostal: 'C1179AAQ',
    localidad: 'CABA',
    provincia: 'Buenos Aires',
    pais: 'Argentina'
  },
  volumenTotalM3: 2.5
}).valid);
assert('crear_entrega_invalido_volumen', !validateSchemaObject(crearEntregaSchema, {
  idDonacion: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  idBeneficiaria: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  destino: { calle: 'Av. Medrano', altura: 951, codigoPostal: 'C1179AAQ', localidad: 'CABA', provincia: 'Buenos Aires', pais: 'Argentina' },
  volumenTotalM3: -1.0
}).valid);

// 4. donacion-independiente-response
const donacionIndependienteResponseSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'donacion-independiente-response.schema.json'), 'utf8'));
assert('donacion_independiente_response_valido', validateSchemaObject(donacionIndependienteResponseSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  donacionOriginalId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  estadoActual: 'EN_DEPOSITO',
  fechaRegistro: '2026-09-05T10:00:00Z',
  cantidad: 5
}).valid);
assert('donacion_independiente_response_invalido_missing', !validateSchemaObject(donacionIndependienteResponseSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
}).valid);

// 5. entrega-response
const entregaResponseSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'entrega-response.schema.json'), 'utf8'));
assert('entrega_response_valido', validateSchemaObject(entregaResponseSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  idDonacion: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  idBeneficiaria: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
  estadoActual: 'PENDIENTE',
  volumenTotalM3: 1.2
}).valid);
assert('entrega_response_invalido_missing', !validateSchemaObject(entregaResponseSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
}).valid);

// 6. evento-entrega-exitosa
const eventoEntregaExitosaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-entrega-exitosa.schema.json'), 'utf8'));
assert('evento_entrega_exitosa_valido', validateSchemaObject(eventoEntregaExitosaSchema, {
  entregaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  donacionIndependienteId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  fechaEntrega: '2026-09-05T14:30:00Z'
}).valid);
assert('evento_entrega_exitosa_invalido_missing', !validateSchemaObject(eventoEntregaExitosaSchema, {
  entregaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
}).valid);

// 7. evento-entrega-fallida
const eventoEntregaFallidaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-entrega-fallida.schema.json'), 'utf8'));
assert('evento_entrega_fallida_valido', validateSchemaObject(eventoEntregaFallidaSchema, {
  entregaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  donacionIndependienteId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  fechaFalla: '2026-09-05T15:00:00Z',
  replanificable: true
}).valid);
assert('evento_entrega_fallida_invalido_missing', !validateSchemaObject(eventoEntregaFallidaSchema, {
  entregaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
}).valid);

// 8. evento-notificable (Polimórfico - B03)
const eventoNotificableSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-notificable.schema.json'), 'utf8'));
assert('evento_notificable_donante_registrado_valido', validateSchemaObject(eventoNotificableSchema, {
  tipo: 'DONANTE_REGISTRADO',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z',
  credencialesDeAcceso: 'clave-inicial-123'
}).valid);
assert('evento_notificable_donante_inactivo_valido', validateSchemaObject(eventoNotificableSchema, {
  tipo: 'DONANTE_INACTIVO',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z',
  diasInactivo: 30
}).valid);
assert('evento_notificable_entrega_fallida_valido', validateSchemaObject(eventoNotificableSchema, {
  tipo: 'ENTREGA_FALLIDA',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z',
  idPersonaBeneficiaria: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  detalleDonacion: 'Caja de leche en polvo',
  idPersonaAdmin: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
  motivo: 'Dirección inaccesible',
  replanificable: true
}).valid);

// 9. evento-ruta-asignada
const eventoRutaAsignadaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-ruta-asignada.schema.json'), 'utf8'));
assert('evento_ruta_asignada_valido', validateSchemaObject(eventoRutaAsignadaSchema, {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  donacionIndependienteId: '98765432-abcd-ef01-2345-6789abcdef01',
  fechaAsignacion: '2026-09-05T13:30:00Z'
}).valid);
assert('evento_ruta_asignada_invalido_missing', !validateSchemaObject(eventoRutaAsignadaSchema, {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
}).valid);

// 10. evento-ruta-iniciada
const eventoRutaIniciadaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-ruta-iniciada.schema.json'), 'utf8'));
assert('evento_ruta_iniciada_valido', validateSchemaObject(eventoRutaIniciadaSchema, {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  camionId: 'b1b2c3d4-e5f6-7890-abcd-ef1234567890',
  patenteCamion: 'AA123BB',
  donacionesIndependientesIds: ['98765432-abcd-ef01-2345-6789abcdef01'],
  fechaInicio: '2026-09-05T08:00:00Z'
}).valid);
assert('evento_ruta_iniciada_invalido_missing', !validateSchemaObject(eventoRutaIniciadaSchema, {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
}).valid);

// 11. persona-replica
const personaReplicaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'persona-replica.schema.json'), 'utf8'));
assert('persona_replica_valido', validateSchemaObject(personaReplicaSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  denominacion: 'Juan Perez',
  tipoPersona: 'HUMANA',
  mediosDeContacto: [{ tipo: 'CORREO', direccionCorreo: 'juan@example.com', esPredeterminado: true }]
}).valid);
assert('persona_replica_invalido_tipo', !validateSchemaObject(personaReplicaSchema, {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  denominacion: 'Juan Perez',
  tipoPersona: 'EXTRATERRESTRE'
}).valid);

// 4. Pruebas Adversarias de Detección de Falsos Positivos (A03, B03)
console.log('\n[4] Pruebas Adversarias de Detección de Falsos Positivos:');

// A03 — Validación de Formatos Estrictos (UUID y date-time)
assert('adversarial_rechazo_uuid_invalido', !validateSchemaObject(eventoRutaAsignadaSchema, {
  rutaId: 'not-a-valid-uuid',
  donacionIndependienteId: '98765432-abcd-ef01-2345-6789abcdef01',
  fechaAsignacion: '2026-09-05T13:30:00Z'
}).valid, 'UUID malformado debe ser rechazado');

assert('adversarial_rechazo_datetime_invalido', !validateSchemaObject(eventoRutaAsignadaSchema, {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  donacionIndependienteId: '98765432-abcd-ef01-2345-6789abcdef01',
  fechaAsignacion: '2026-13-99'
}).valid, 'date-time malformado debe ser rechazado');

// B03 — Falsos Positivos en Polimorfismo de EventoNotificableDTO
const payloadFalsoPositivoB03 = {
  tipo: 'DONANTE_REGISTRADO',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z'
  // falta credencialesDeAcceso
};
assert('adversarial_rechazo_b03_subtipo_incompleto', !validateSchemaObject(eventoNotificableSchema, payloadFalsoPositivoB03).valid, 'Payload sin credencialesDeAcceso debe ser RECHAZADO');

const payloadSubtipoInvalido = {
  tipo: 'DONANTE_INACTIVO',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z',
  diasInactivo: 0 // debe ser minimum: 1
};
assert('adversarial_rechazo_subtipo_dias_inactivos_invalidos', !validateSchemaObject(eventoNotificableSchema, payloadSubtipoInvalido).valid, 'diasInactivo <= 0 debe ser RECHAZADO');

const payloadTipoInexistente = {
  tipo: 'TIPO_INEXISTENTE',
  idPersonaDonante: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  fecha: '2026-09-05T12:00:00Z'
};
assert('adversarial_rechazo_tipo_inexistente', !validateSchemaObject(eventoNotificableSchema, payloadTipoInexistente).valid, 'Tipo polimórfico desconocido debe ser RECHAZADO');

console.log('\n────────────────────────────────────────────────────────────');
console.log(`RESULTADOS: PASS: ${passed}  │  FAIL: ${failed}`);
console.log('────────────────────────────────────────────────────────────\n');

process.exit(failed > 0 ? 1 : 0);
