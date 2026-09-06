'use strict';

const fs = require('fs');
const path = require('path');
const { TEMPORAL_DRIFT_SKIP_MODULES } = require('../config');
const { passed, warned } = require('../lib/findings');
const { walkFiles, toForwardSlash } = require('../lib/paths');
const { parsePomModules } = require('../lib/pom');

// Drift signals as declared in docs/context-index.md Temporal Constraints section.
// Do not expand this list without a corresponding update to context-index.md.
const JPA_DRIFT_SIGNAL = 'spring-boot-starter-data-jpa';
const ENTITY_DRIFT_SIGNALS = ['@Entity', '@Column'];
const ENTITY_PATH_FRAGMENT = 'models/entities/';

function checkTemporalDrift(repoRoot) {
  const findings = [];
  const pomPath = path.join(repoRoot, 'pom.xml');

  if (!fs.existsSync(pomPath)) return [];

  const allModules = parsePomModules(fs.readFileSync(pomPath, 'utf8'));
  const services = allModules.filter(m => !TEMPORAL_DRIFT_SKIP_MODULES.has(m));

  let driftFound = false;

  for (const service of services) {
    // Signal A: spring-boot-starter-data-jpa in service pom.xml
    const servicePom = path.join(repoRoot, service, 'pom.xml');
    if (fs.existsSync(servicePom)) {
      const content = fs.readFileSync(servicePom, 'utf8');
      if (content.includes(JPA_DRIFT_SIGNAL)) {
        findings.push(warned('TEMPORAL_DRIFT',
          `${service} — \`${JPA_DRIFT_SIGNAL}\` detected in pom.xml. ` +
          `Review whether the temporal constraint in docs/context-index.md is still current.`,
          `${service}/pom.xml`
        ));
        driftFound = true;
      }
    }

    // Signal B: @Entity or @Column in files under models/entities/ path
    const srcDir = path.join(repoRoot, service, 'src');
    if (fs.existsSync(srcDir)) {
      const javaFiles = walkFiles(srcDir, repoRoot, name => name.endsWith('.java'));
      for (const { rel, full } of javaFiles) {
        if (!rel.includes(ENTITY_PATH_FRAGMENT)) continue;
        let content;
        try { content = fs.readFileSync(full, 'utf8'); } catch { continue; }
        for (const signal of ENTITY_DRIFT_SIGNALS) {
          if (content.includes(signal)) {
            findings.push(warned('TEMPORAL_DRIFT',
              `${service} — \`${signal}\` detected in models/entities/. ` +
              `Review whether the domain purity constraint in docs/context-index.md is still current.`,
              toForwardSlash(rel)
            ));
            driftFound = true;
            break; // one warning per file is enough
          }
        }
      }
    }
  }

  if (!driftFound) {
    findings.push(passed('TEMPORAL_DRIFT', `no drift signals detected across ${services.length} services`));
  }

  return findings;
}

module.exports = { checkTemporalDrift };
