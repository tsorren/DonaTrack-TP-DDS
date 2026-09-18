'use strict';

const fs = require('fs');
const path = require('path');
const { passed, failed, warned } = require('../lib/findings');

const EXPECTED_SKILLS = [
  'define-spec',
  'design-spec',
  'design-review',
  'implement-task',
  'implementation-review',
  'engineering-loop',
  'explain-concept',
  'review-pr',
];

function checkSkillsIntegrity(repoRoot) {
  const findings = [];
  const skillsDirRel = '.agents/skills';
  const skillsDirFull = path.join(repoRoot, '.agents', 'skills');
  const gitignoreFull = path.join(repoRoot, '.gitignore');

  // 1. Check directory existence
  if (!fs.existsSync(skillsDirFull)) {
    findings.push(failed('SKILLS_DIR_EXISTS', `${skillsDirRel} directory missing`, skillsDirRel));
    return findings;
  }
  findings.push(passed('SKILLS_DIR_EXISTS', `${skillsDirRel} directory present`));

  // 2. Check gitignore un-ignore rule
  if (fs.existsSync(gitignoreFull)) {
    const gitignoreContent = fs.readFileSync(gitignoreFull, 'utf8');
    if (gitignoreContent.includes('!.agents/skills/')) {
      findings.push(passed('SKILLS_GITIGNORE_TRACKED', '.gitignore explicitly un-ignores !.agents/skills/ for Git tracking'));
    } else {
      findings.push(failed('SKILLS_GITIGNORE_TRACKED', '.gitignore must un-ignore !.agents/skills/ to prevent knowledge loss', '.gitignore'));
    }
  }

  // 3. Check individual skills
  for (const skillName of EXPECTED_SKILLS) {
    const skillRel = path.join(skillsDirRel, skillName, 'SKILL.md').replace(/\\/g, '/');
    const skillFull = path.join(skillsDirFull, skillName, 'SKILL.md');

    if (!fs.existsSync(skillFull)) {
      findings.push(failed('SKILL_FILE_EXISTS', `Missing skill file: ${skillRel}`, skillRel));
      continue;
    }

    const content = fs.readFileSync(skillFull, 'utf8');

    // 4. Validate YAML frontmatter
    const hasFrontmatter = content.startsWith('---') && content.includes('name:') && content.includes('description:');
    if (hasFrontmatter) {
      findings.push(passed('SKILL_FRONTMATTER', `${skillName} has valid YAML frontmatter`));
    } else {
      findings.push(failed('SKILL_FRONTMATTER', `${skillRel} missing valid YAML frontmatter with name and description`, skillRel));
    }

    // 5. Check AGENTS.md invariant anchor
    if (content.includes('AGENTS.md')) {
      findings.push(passed('SKILL_AGENTS_ANCHOR', `${skillName} explicitly anchors to AGENTS.md`));
    } else {
      findings.push(warned('SKILL_AGENTS_ANCHOR', `${skillRel} should reference AGENTS.md invariants`, skillRel));
    }

    // 6. Vector audits for reviewers
    if (skillName === 'design-review') {
      const hasAllD = ['D1', 'D2', 'D3', 'D4', 'D5', 'D6'].every(d => content.includes(d));
      if (hasAllD) {
        findings.push(passed('DESIGN_REVIEW_CONTRACT_VECTORS', `${skillName} defines all D1-D6 design vectors`));
      } else {
        findings.push(failed('DESIGN_REVIEW_CONTRACT_VECTORS', `${skillRel} missing one or more D1-D6 design vectors`, skillRel));
      }
    }

    if (skillName === 'implementation-review') {
      const hasAllV = ['V1', 'V2', 'V3', 'V4', 'V5', 'V6', 'V7', 'V8', 'V9'].every(v => content.includes(v));
      if (hasAllV) {
        findings.push(passed('IMPL_REVIEW_CONTRACT_VECTORS', `${skillName} defines all V1-V9 code vectors from evaluator.md`));
      } else {
        findings.push(failed('IMPL_REVIEW_CONTRACT_VECTORS', `${skillRel} missing one or more V1-V9 vectors`, skillRel));
      }
    }

    if (skillName === 'explain-concept') {
      const hasAllSteps = ['Paso 1:', 'Paso 2:', 'Paso 3:', 'Paso 4:'].every(step => content.includes(step));
      if (hasAllSteps) {
        findings.push(passed('EXPLAIN_CONCEPT_FRAMEWORK_STEPS', `${skillName} defines the 4 pedagogical framework steps`));
      } else {
        findings.push(failed('EXPLAIN_CONCEPT_FRAMEWORK_STEPS', `${skillRel} missing one or more of the 4 pedagogical framework steps`, skillRel));
      }
    }

    if (skillName === 'review-pr') {
      const vectors = [
        'Arquitectura',
        'Contratos',
        'Concurrencia',
        'Tests',
        'Seguridad',
        'Rendimiento',
        'Scope',
        'Simplicidad',
      ];
      const hasAllVectors = vectors.every(v => content.toLowerCase().includes(v.toLowerCase()));
      if (hasAllVectors) {
        findings.push(passed('REVIEW_PR_VECTORS', `${skillName} defines all 8 PR review vectors`));
      } else {
        findings.push(failed('REVIEW_PR_VECTORS', `${skillRel} missing one or more of the 8 PR review vectors`, skillRel));
      }

      const grepaiTools = ['grepai_search', 'grepai_trace_callers', 'grepai_trace_callees'];
      const hasGrepaiProtocol = grepaiTools.every(tool => content.includes(tool));
      if (hasGrepaiProtocol) {
        findings.push(passed('REVIEW_PR_GREPAI_PROTOCOL', `${skillName} defines the GrepAI-First inspection protocol`));
      } else {
        findings.push(failed('REVIEW_PR_GREPAI_PROTOCOL', `${skillRel} missing GrepAI-First tool references`, skillRel));
      }

      const reportSections = ['REVISIÓN CRÍTICA DE PR', 'Veredicto', 'Matriz de Evaluación Rápida', 'Hallazgos Bloqueantes'];
      const hasReportStructure = reportSections.every(s => content.includes(s));
      if (hasReportStructure) {
        findings.push(passed('REVIEW_PR_REPORT_FORMAT', `${skillName} defines canonical compact report template`));
      } else {
        findings.push(failed('REVIEW_PR_REPORT_FORMAT', `${skillRel} missing canonical compact report sections`, skillRel));
      }
    }
  }

  return findings;
}

module.exports = { checkSkillsIntegrity, EXPECTED_SKILLS };
