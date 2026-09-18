'use strict';

// Extracts all [text](target) links from Markdown content.
// Pure — no filesystem access. Limitation: anchor targets not validated.
function extractMarkdownLinks(content) {
  const links = [];
  const pattern = /\[([^\]]*)\]\(([^)]+)\)/g;
  let match;
  while ((match = pattern.exec(content)) !== null) {
    let raw = match[2].trim();
    raw = raw.replace(/\s+"[^"]*"$/, '').replace(/\s+'[^']*'$/, '').trim();
    links.push({ text: match[1], raw });
  }
  return links;
}

// Extracts backtick code spans that look like concrete docs/ filesystem paths.
// Skips: spans without /; spans not starting with docs/; spans with <placeholder>.
// Pure — no filesystem access.
function extractCodespanPaths(content) {
  const paths = [];
  const pattern = /`([^`]+)`/g;
  let match;
  while ((match = pattern.exec(content)) !== null) {
    const span = match[1];
    if (!span.startsWith('docs/')) continue;
    if (/<[^>]+>/.test(span)) continue;
    paths.push(span);
  }
  return paths;
}

module.exports = { extractMarkdownLinks, extractCodespanPaths };
