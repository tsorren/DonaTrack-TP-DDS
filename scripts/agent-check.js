'use strict';

// Agent Governance Check — CLI entry point
// Run: node scripts/agent-check.js
// Docs: AGENTS.md §14

const path = require('path');
const { runAllChecks, renderFindings } = require('./agent-check/index');

const repoRoot = path.resolve(__dirname, '..');
const findings = runAllChecks(repoRoot);
console.log(renderFindings(findings));
process.exit(findings.filter(f => f.severity === 'FAIL').length > 0 ? 1 : 0);
