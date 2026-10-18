'use strict';

// Parses the <modules> section of a Maven pom.xml and returns module names.
// Pure — no filesystem access. Uses regex on the known format; no XML library.
function parsePomModules(content) {
  const block = content.match(/<modules>([\s\S]*?)<\/modules>/);
  if (!block) return [];
  const modules = [];
  const pattern = /<module>([^<]+)<\/module>/g;
  let match;
  while ((match = pattern.exec(block[1])) !== null) {
    modules.push(match[1].trim());
  }
  return modules;
}

module.exports = { parsePomModules };
