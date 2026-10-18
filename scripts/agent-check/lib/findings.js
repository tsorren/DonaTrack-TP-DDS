'use strict';

function passed(id, message) {
  return { id, severity: 'PASS', message };
}

function failed(id, message, file) {
  const f = { id, severity: 'FAIL', message };
  if (file !== undefined) f.file = file;
  return f;
}

function warned(id, message, file) {
  const f = { id, severity: 'WARN', message };
  if (file !== undefined) f.file = file;
  return f;
}

module.exports = { passed, failed, warned };
