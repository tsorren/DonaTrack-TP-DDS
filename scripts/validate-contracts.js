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
function validateSchemaObject(schema, data, pathName = 'root') {
  if (schema.type === 'object') {
    if (typeof data !== 'object' || data === null || Array.isArray(data)) {
      return { valid: false, error: `${pathName}: expected object, got ${typeof data}` };
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
          const res = validateSchemaObject(schema.properties[key], val, `${pathName}.${key}`);
          if (!res.valid) return res;
        } else if (schema.additionalProperties === false) {
          return { valid: false, error: `${pathName}: additional property '${key}' not allowed` };
        }
      }
    }
  } else if (schema.type === 'string') {
    if (typeof data !== 'string') {
      return { valid: false, error: `${pathName}: expected string, got ${typeof data}` };
    }
    if (schema.enum && !schema.enum.includes(data)) {
      return { valid: false, error: `${pathName}: '${data}' is not in enum [${schema.enum.join(', ')}]` };
    }
    if (schema.minLength && data.length < schema.minLength) {
      return { valid: false, error: `${pathName}: length ${data.length} < minLength ${schema.minLength}` };
    }
  } else if (schema.type === 'number' || schema.type === 'integer') {
    if (typeof data !== 'number' || isNaN(data) || (schema.type === 'integer' && !Number.isInteger(data))) {
      return { valid: false, error: `${pathName}: expected ${schema.type}, got ${typeof data}` };
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
  } else if (schema.type === 'boolean') {
    if (typeof data !== 'boolean') {
      return { valid: false, error: `${pathName}: expected boolean, got ${typeof data}` };
    }
  } else if (schema.type === 'array') {
    if (!Array.isArray(data)) {
      return { valid: false, error: `${pathName}: expected array, got ${typeof data}` };
    }
    if (schema.items) {
      for (let i = 0; i < data.length; i++) {
        const res = validateSchemaObject(schema.items, data[i], `${pathName}[${i}]`);
        if (!res.valid) return res;
      }
    }
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
  assert('schema_files_count', schemaFiles.length >= 8, `Encontrados ${schemaFiles.length} schemas`);

  for (const file of schemaFiles) {
    const fullPath = path.join(schemasDir, file);
    try {
      const parsed = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
      assert(`schema_syntax_${file}`, true);
      assert(`schema_fields_${file}`, parsed.$schema && parsed.type && parsed.properties, 'Campos base presentes');
      if (parsed.required) {
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

// 3. Validación de Payloads Funcionales contra Schemas
console.log('\n[3] Validación de Payloads de Prueba contra Schemas:');

const cambioEstadoSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'cambio-estado-donacion-request.schema.json'), 'utf8'));
const validCambioEstado = { estado: 'EN_TRASLADO', justificacion: 'Ruta 1 asignada', replanificable: false };
const invalidCambioEstadoEnum = { estado: 'ESTADO_INVENTADO' };
const invalidCambioEstadoMissing = { justificacion: 'Sin estado' };

assert('cambio_estado_payload_valido', validateSchemaObject(cambioEstadoSchema, validCambioEstado).valid);
assert('cambio_estado_payload_invalido_enum', !validateSchemaObject(cambioEstadoSchema, invalidCambioEstadoEnum).valid);
assert('cambio_estado_payload_invalido_missing', !validateSchemaObject(cambioEstadoSchema, invalidCambioEstadoMissing).valid);

const entregaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'crear-entrega-request.schema.json'), 'utf8'));
const validEntrega = {
  idDonacion: '12345678-1234-1234-1234-123456789abc',
  idBeneficiaria: 'abcdefab-abcd-abcd-abcd-abcdefabcdef',
  destino: {
    calle: 'Av. Medrano',
    altura: 951,
    codigoPostal: 'C1179AAQ',
    localidad: 'CABA',
    provincia: 'Buenos Aires',
    pais: 'Argentina'
  },
  volumenTotalM3: 2.5
};
const invalidEntregaVolumen = {
  idDonacion: '12345678-1234-1234-1234-123456789abc',
  idBeneficiaria: 'abcdefab-abcd-abcd-abcd-abcdefabcdef',
  destino: {
    calle: 'Av. Medrano',
    altura: 951,
    codigoPostal: 'C1179AAQ',
    localidad: 'CABA',
    provincia: 'Buenos Aires',
    pais: 'Argentina'
  },
  volumenTotalM3: -1.0 // Debe ser positivo
};
assert('crear_entrega_payload_valido', validateSchemaObject(entregaSchema, validEntrega).valid);
assert('crear_entrega_payload_invalido_volumen', !validateSchemaObject(entregaSchema, invalidEntregaVolumen).valid);

const personaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'persona-replica.schema.json'), 'utf8'));
const validPersona = {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  denominacion: 'Juan Perez',
  tipoPersona: 'HUMANA',
  mediosDeContacto: [{ tipo: 'CORREO', direccionCorreo: 'juan@example.com', esPredeterminado: true }]
};
const invalidPersonaTipo = {
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  denominacion: 'Juan Perez',
  tipoPersona: 'EXTRATERRESTRE'
};
assert('persona_replica_payload_valido', validateSchemaObject(personaSchema, validPersona).valid);
assert('persona_replica_payload_invalido_tipo', !validateSchemaObject(personaSchema, invalidPersonaTipo).valid);

const amqpRutaAsignadaSchema = JSON.parse(fs.readFileSync(path.join(schemasDir, 'evento-ruta-asignada.schema.json'), 'utf8'));
const validAmqpRuta = {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  donacionIndependienteId: '98765432-abcd-ef01-2345-6789abcdef01',
  fechaAsignacion: '2026-09-05T13:30:00'
};
const invalidAmqpRutaMissing = {
  rutaId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890' // Falta donacionIndependienteId
};
assert('amqp_ruta_asignada_valido', validateSchemaObject(amqpRutaAsignadaSchema, validAmqpRuta).valid);
assert('amqp_ruta_asignada_invalido_missing', !validateSchemaObject(amqpRutaAsignadaSchema, invalidAmqpRutaMissing).valid);

console.log('\n────────────────────────────────────────────────────────────');
console.log(`RESULTADOS: PASS: ${passed}  │  FAIL: ${failed}`);
console.log('────────────────────────────────────────────────────────────\n');

process.exit(failed > 0 ? 1 : 0);
