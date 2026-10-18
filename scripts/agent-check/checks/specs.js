'use strict';

// Agent Governance Check — Spec-Driven Development (SDD) linter
// Verifies structural integrity of specifications in docs/specs/active/ and docs/specs/completed/
// Rule: AGENTS.md §2, docs/specs/README.md, .agents/skills/define-spec, .agents/skills/design-spec

const fs = require('fs');
const path = require('path');
const { passed, failed, warned } = require('../lib/findings');

function parseSpecFile(content) {
  const hasCanonicalTitle = /^#\s*Spec:\s*.+/im.test(content);
  const hasStatus = />\s*\*\*(Estado|Status):\*\*/i.test(content);
  const hasLevel = />\s*\*\*(Nivel|Level):\*\*/i.test(content);
  const isArchitectural = />\s*\*\*(Nivel|Level):\*\*\s*ARCHITECTURAL/i.test(content);
  const hasGoal = /##\s*(1\.\s*)?(Objetivo Funcional|Goal)/i.test(content);
  const hasScope = /##\s*(2\.\s*)?(Alcance Delimitado|Scope Boundaries)/i.test(content) &&
                   /###\s*In-Scope/i.test(content) &&
                   /###\s*Out-of-Scope/i.test(content);
  const hasTradeoffs = /##\s*(3\.\s*)?(Decisiones Acordadas|Trade-offs)/i.test(content) &&
                       /(5D|Trade-off|Alternativa|Tradeoff)/i.test(content);
  const hasInvariants = /##\s*(4\.\s*)?(Invariantes|Invariants)/i.test(content);
  const hasGherkin = /##\s*(5\.\s*)?(Criterios de Aceptación|Acceptance Criteria)/i.test(content) &&
                     /(Dado|Given)/i.test(content) &&
                     /(Cuando|When)/i.test(content) &&
                     /(Entonces|Then)/i.test(content);
  const hasTechnicalDesign = /##\s*(7\.\s*)?(Especificación Técnica de Diseño|Technical Design)/i.test(content) &&
                             /(Two-Gate|ADR)/i.test(content) &&
                             /(TDD|Plan TDD|Test)/i.test(content);
  const hasReviewContract = /===\s*(DESIGN REVIEW CONTRACT|REVIEW CONTRACT)\s*===/i.test(content);

  return {
    hasCanonicalTitle,
    hasStatus,
    hasLevel,
    isArchitectural,
    hasGoal,
    hasScope,
    hasTradeoffs,
    hasInvariants,
    hasGherkin,
    hasTechnicalDesign,
    hasReviewContract,
  };
}

function checkSpecsIntegrity(repoRoot) {
  const findings = [];
  const specsDirRel = 'docs/specs';
  const specsDirFull = path.join(repoRoot, 'docs', 'specs');
  const activeDirFull = path.join(specsDirFull, 'active');
  const completedDirFull = path.join(specsDirFull, 'completed');

  if (!fs.existsSync(specsDirFull)) {
    findings.push(failed('SPECS_DIR_EXISTS', `${specsDirRel} directory missing`, specsDirRel));
    return findings;
  }
  findings.push(passed('SPECS_DIR_EXISTS', `${specsDirRel} directory present`));

  const specFiles = [];

  function collectSpecs(dirFull, dirRel, isCompleted) {
    if (!fs.existsSync(dirFull)) return;
    const entries = fs.readdirSync(dirFull);
    for (const entry of entries) {
      if (entry.endsWith('.md') && entry !== 'README.md') {
        specFiles.push({
          relPath: path.join(dirRel, entry).replace(/\\/g, '/'),
          fullPath: path.join(dirFull, entry),
          isCompleted,
        });
      }
    }
  }

  collectSpecs(activeDirFull, 'docs/specs/active', false);
  collectSpecs(completedDirFull, 'docs/specs/completed', true);

  if (specFiles.length === 0) {
    findings.push(passed('SPECS_INTEGRITY', 'no active or completed specs present (clean state)'));
    return findings;
  }

  let totalSpecErrors = 0;

  for (const spec of specFiles) {
    const content = fs.readFileSync(spec.fullPath, 'utf8');
    const parsed = parseSpecFile(content);

    if (parsed.hasCanonicalTitle) {
      findings.push(passed('SPEC_CANONICAL_TITLE', `${spec.relPath} has canonical '# Spec:' title`));
    } else {
      findings.push(failed('SPEC_CANONICAL_TITLE', `${spec.relPath} must start with '# Spec: <ID> — <Title>'`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasStatus) {
      findings.push(passed('SPEC_METADATA_STATUS', `${spec.relPath} defines status metadata`));
    } else {
      findings.push(failed('SPEC_METADATA_STATUS', `${spec.relPath} missing '> **Estado:**' or '> **Status:**'`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasLevel) {
      findings.push(passed('SPEC_METADATA_LEVEL', `${spec.relPath} defines level metadata`));
    } else {
      findings.push(failed('SPEC_METADATA_LEVEL', `${spec.relPath} missing '> **Nivel:** STANDARD|ARCHITECTURAL'`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasGoal) {
      findings.push(passed('SPEC_SECTION_GOAL', `${spec.relPath} defines Objetivo Funcional (Goal)`));
    } else {
      findings.push(failed('SPEC_SECTION_GOAL', `${spec.relPath} missing '## 1. Objetivo Funcional (Goal)' section`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasScope) {
      findings.push(passed('SPEC_SECTION_SCOPE', `${spec.relPath} defines Scope Boundaries (In-Scope and Out-of-Scope)`));
    } else {
      findings.push(failed('SPEC_SECTION_SCOPE', `${spec.relPath} missing Scope Boundaries with In-Scope and Out-of-Scope subsections`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasTradeoffs) {
      findings.push(passed('SPEC_SECTION_TRADEOFFS', `${spec.relPath} documents Trade-offs and Decision Rationale (5D)`));
    } else {
      findings.push(failed('SPEC_SECTION_TRADEOFFS', `${spec.relPath} missing Trade-offs section referencing 5D criteria`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasInvariants) {
      findings.push(passed('SPEC_SECTION_INVARIANTS', `${spec.relPath} defines Business Invariants`));
    } else {
      findings.push(failed('SPEC_SECTION_INVARIANTS', `${spec.relPath} missing '## 4. Invariantes' section`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.hasGherkin) {
      findings.push(passed('SPEC_SECTION_GHERKIN', `${spec.relPath} defines Given-When-Then Acceptance Criteria`));
    } else {
      findings.push(failed('SPEC_SECTION_GHERKIN', `${spec.relPath} missing Given-When-Then acceptance criteria in Gherkin format`, spec.relPath));
      totalSpecErrors++;
    }

    if (parsed.isArchitectural) {
      if (parsed.hasTechnicalDesign) {
        findings.push(passed('SPEC_ARCH_TECHNICAL_DESIGN', `${spec.relPath} includes Technical Design (Two-Gate ADR + TDD plan)`));
      } else {
        findings.push(failed('SPEC_ARCH_TECHNICAL_DESIGN', `${spec.relPath} classified as ARCHITECTURAL but missing Section 7 Technical Design with Two-Gate ADR and TDD plan`, spec.relPath));
        totalSpecErrors++;
      }
    }

    if (spec.isCompleted) {
      if (parsed.hasReviewContract) {
        findings.push(passed('SPEC_COMPLETED_REVIEW_CONTRACT', `${spec.relPath} includes verified Review Contract`));
      } else {
        findings.push(failed('SPEC_COMPLETED_REVIEW_CONTRACT', `${spec.relPath} in completed/ must include === REVIEW CONTRACT === or === DESIGN REVIEW CONTRACT ===`, spec.relPath));
        totalSpecErrors++;
      }
    }
  }

  if (totalSpecErrors === 0) {
    findings.push(passed('SPECS_INTEGRITY', `all ${specFiles.length} specification(s) compliant with canonical SDD format`));
  }

  return findings;
}

module.exports = { checkSpecsIntegrity, parseSpecFile };
