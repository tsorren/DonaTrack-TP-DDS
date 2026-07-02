const fs = require('fs');
const path = require('path');

function findPDFs(dir, baseDir = '') {
    let results = [];
    if (!fs.existsSync(dir)) return results;

    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const relPath = path.join(baseDir, file);
        const stat = fs.statSync(fullPath);

        if (stat.isDirectory()) {
            // Búsqueda recursiva para subcarpetas (ej. entregas/1/anexos/)
            results = results.concat(findPDFs(fullPath, relPath));
        } else if (file.toLowerCase().endsWith('.pdf')) {
            const posixRelPath = relPath.replace(/\\/g, '/');
            const parts = posixRelPath.split('/');
            // El grupo base (ej. "1", "2") es el primer directorio. Si está en raíz, es "General"
            const entregaFolder = parts.length > 1 ? parts[0] : 'General';
            const subFolders = parts.length > 2 ? parts.slice(1, -1) : [];
            
            results.push({
                path: posixRelPath, // Asegura compatibilidad web de rutas
                entregaFolder: entregaFolder,
                subFolders: subFolders,
                filename: file
            });
        }
    }
    return results;
}

const targetDir = path.join(process.cwd(), 'staging', 'docs', 'entregas');
fs.writeFileSync(path.join(targetDir, 'pdfs.json'), JSON.stringify(findPDFs(targetDir)));