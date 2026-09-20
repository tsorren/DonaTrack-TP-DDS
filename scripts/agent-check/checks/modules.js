'use strict';

const fs = require('fs');
const path = require('path');
const { ROUTING_MODULE_EXCLUSIONS } = require('../config');
const { passed, failed, warned } = require('../lib/findings');
const { parsePomModules } = require('../lib/pom');

// Extracts service names from the Service Context table in context-index.md.
// Looks for backtick-wrapped names in the first column of the Service Context section.
// Pure — no filesystem access.
function parseContextIndexServices(content) {
  const sections = content.split(/\n(?=##\s)/);
  const serviceSection = sections.find(s => /^##\s+Service Context/m.test(s));
  if (!serviceSection) return [];

  const services = [];
  const rowPattern = /^\|\s*`([^`]+)`\s*\|/gm;
  let match;
  while ((match = rowPattern.exec(serviceSection)) !== null) {
    services.push(match[1]);
  }
  return services;
}

function checkModuleRouting(repoRoot) {
  const findings = [];
  const pomPath = path.join(repoRoot, 'pom.xml');
  const indexPath = path.join(repoRoot, 'docs', 'context-index.md');

  if (!fs.existsSync(pomPath)) {
    return [warned('MODULE_ROUTING_COMPLETENESS', 'pom.xml not found at repo root — module routing check skipped')];
  }
  if (!fs.existsSync(indexPath)) {
    return [failed('ROUTING_DEAD_ENTRY', 'docs/context-index.md not found', 'docs/context-index.md')];
  }

  const mavenModules = parsePomModules(fs.readFileSync(pomPath, 'utf8'));
  const routedServices = new Set(parseContextIndexServices(fs.readFileSync(indexPath, 'utf8')));

  let anyFail = false;

  // Module in Maven but not routed (WARN — not every module needs Service Context)
  for (const mod of mavenModules) {
    if (ROUTING_MODULE_EXCLUSIONS.has(mod)) continue;
    if (!routedServices.has(mod)) {
      findings.push(warned('MODULE_WITHOUT_ROUTING',
        `Maven module "${mod}" has no Service Context entry in docs/context-index.md`));
    }
  }

  // Routed service with no corresponding Maven module (FAIL — dead reference)
  const mavenSet = new Set(mavenModules);
  for (const svc of routedServices) {
    if (!mavenSet.has(svc)) {
      findings.push(failed('ROUTING_DEAD_ENTRY',
        `Service Context entry "${svc}" does not match any Maven module in pom.xml`));
      anyFail = true;
    }
  }

  if (!anyFail) {
    const excluded = mavenModules.filter(m => ROUTING_MODULE_EXCLUSIONS.has(m));
    const checked = mavenModules.length - excluded.length;
    findings.push(passed('MODULE_ROUTING_COMPLETENESS',
      `${checked} modules × ${routedServices.size} routed services aligned` +
      (excluded.length > 0 ? ` (${excluded.join(', ')} excluded)` : '')));
  }

  return findings;
}

module.exports = { checkModuleRouting, parseContextIndexServices };
