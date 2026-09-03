'use strict';

// Parses an ADR file's content and extracts governance metadata.
// Pure — no filesystem access.
// Returns { status: string|null, supersededRef: string|null }
//   status: lowercase first word of the Status: field value
//   supersededRef: link target from "superseded by [text](path)" format, or null
function parseAdrEntry(content) {
  const statusLine = content.match(/^-\s*Status:\s*(.+)$/m);
  if (!statusLine) return { status: null, supersededRef: null };

  const raw = statusLine[1].trim();

  // Handle: "superseded by [text](path)" format
  const supersededMatch = raw.match(/^superseded\s+by\s+\[([^\]]*)\]\(([^)]+)\)/i);
  if (supersededMatch) {
    return { status: 'superseded', supersededRef: supersededMatch[2].trim() };
  }

  // Plain status value (proposed / accepted / rejected / superseded without ref)
  return { status: raw.toLowerCase().split(/\s+/)[0], supersededRef: null };
}

// Convenience wrapper: returns only the status string (used by deuda-tecnica check).
function parseAdrStatus(content) {
  return parseAdrEntry(content).status;
}

module.exports = { parseAdrEntry, parseAdrStatus };
