'use strict';

const fs = require('fs');
const path = require('path');
const { SKIP_DIRS, HISTORY_PREFIXES } = require('../config');

function toForwardSlash(p) {
  return p.replace(/\\/g, '/');
}

function isUnderHistory(relPath) {
  const normalized = toForwardSlash(relPath);
  return HISTORY_PREFIXES.some(
    prefix => normalized === prefix || normalized.startsWith(prefix + '/')
  );
}

// Returns [{ rel, full }] for files matching filterFn(filename).
// rel is forward-slash normalized, relative to repoRoot.
function walkFiles(startDir, repoRoot, filterFn) {
  const results = [];
  function recurse(current) {
    let entries;
    try { entries = fs.readdirSync(current, { withFileTypes: true }); } catch { return; }
    for (const entry of entries) {
      if (SKIP_DIRS.has(entry.name)) continue;
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        recurse(full);
      } else if (entry.isFile() && filterFn(entry.name)) {
        const rel = toForwardSlash(path.relative(repoRoot, full));
        results.push({ rel, full });
      }
    }
  }
  recurse(startDir);
  return results;
}

// Resolves a Markdown link target to an absolute path for existence checking.
// Returns { skip: true } for external, anchor-only, or empty targets.
// Returns { skip: false, resolved: string } otherwise.
function resolveInternalLink(sourceRel, rawTarget, repoRoot) {
  if (/^https?:\/\/|^mailto:|^ftp:\/\//.test(rawTarget)) return { skip: true };
  if (rawTarget.startsWith('#')) return { skip: true };
  const withoutAnchor = rawTarget.split('#')[0];
  if (!withoutAnchor) return { skip: true };
  let resolved;
  if (withoutAnchor.startsWith('/')) {
    resolved = path.join(repoRoot, withoutAnchor.slice(1));
  } else {
    const sourceDir = path.dirname(path.join(repoRoot, sourceRel));
    resolved = path.join(sourceDir, withoutAnchor);
  }
  return { skip: false, resolved };
}

module.exports = { toForwardSlash, isUnderHistory, walkFiles, resolveInternalLink };
