// --- Capa de Datos (Data Access) ---
class PdfRepository {
    constructor(url) { this.url = url; }
    
    async getPdfs() {
        const response = await fetch(this.url);
        if (!response.ok) throw new Error("Archivo pdfs.json no encontrado.");
        return await response.json();
    }
}

// --- Capa de Utilidades (Domain/Logic) ---
class StringFormatter {
    static cleanName(filename) {
        let name = filename.replace(/\.[^/.]+$/, ""); // Quita la extensión
        name = name.replace(/[-_]/g, " "); // Reemplaza guiones y snake_case por espacios
        
        // Capitaliza la primera letra de cada palabra respetando tildes/unicode
        return name.split(' ')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    }

    static formatFolder(folderName, isRoot = false) {
        if (isRoot && !isNaN(folderName)) return `Entrega ${folderName}`;
        return this.cleanName(folderName);
    }
}

class TreeBuilder {
    static build(files) {
        const root = { name: 'root', isRoot: true, children: {}, files: [] };
        
        files.forEach(file => {
            let current = root;
            
            if (!current.children[file.entregaFolder]) {
                current.children[file.entregaFolder] = { name: file.entregaFolder, isRoot: true, children: {}, files: [] };
            }
            current = current.children[file.entregaFolder];
            
            if (file.subFolders && file.subFolders.length > 0) {
                file.subFolders.forEach(sub => {
                    if (!current.children[sub]) {
                        current.children[sub] = { name: sub, isRoot: false, children: {}, files: [] };
                    }
                    current = current.children[sub];
                });
            }
            
            current.files.push(file);
        });
        
        return root.children;
    }
}

// --- Capa de Presentación (View/UI) ---
class SidebarRenderer {
    constructor(containerElement, frameElement, basePath) {
        this.containerElement = containerElement;
        this.frameElement = frameElement;
        this.basePath = basePath;
        this.isFirstFileLoaded = false;
    }

    render(files) {
        this.containerElement.innerHTML = ""; 
        
        if (!files || files.length === 0) {
            this.containerElement.innerHTML = "<p>No hay PDFs de entregas disponibles.</p>";
            return;
        }

        const tree = TreeBuilder.build(files);
        this.renderNode(tree, this.containerElement, 0);
    }

    renderNode(nodes, containerElement, depth) {
        // Ordenamos las carpetas alfabéticamente para mantener consistencia
        const sortedKeys = Object.keys(nodes).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
        
        for (const key of sortedKeys) {
            const node = nodes[key];
            
            const groupContainer = document.createElement("div");
            groupContainer.className = "folder-group";
            groupContainer.style.marginLeft = depth > 0 ? "1rem" : "0"; // Efecto visual jerárquico

            const header = document.createElement("h3");
            header.className = `group-header ${depth > 0 ? "subfolder-header" : ""}`;
            header.textContent = StringFormatter.formatFolder(node.name, node.isRoot);
            groupContainer.appendChild(header);

            const ul = document.createElement("ul");
            ul.className = "group-list";
            
            header.addEventListener("click", () => {
                header.classList.toggle("collapsed");
                ul.classList.toggle("collapsed");
            });
            
            if (Object.keys(node.children).length > 0) {
                const subContainer = document.createElement("li");
                subContainer.style.listStyle = "none";
                this.renderNode(node.children, subContainer, depth + 1);
                ul.appendChild(subContainer);
            }
            
            node.files.forEach(file => {
                const li = document.createElement("li");
                const a = document.createElement("a");
                a.textContent = StringFormatter.cleanName(file.filename);
                a.className = "pdf-link";
                
                const fileUrl = `${this.basePath}/${file.path}`;

                if (!this.isFirstFileLoaded) {
                    a.classList.add("active");
                    this.frameElement.src = fileUrl;
                    this.isFirstFileLoaded = true;
                }
                
                a.addEventListener("click", (e) => {
                    e.preventDefault();
                    document.querySelectorAll(".pdf-link").forEach(link => link.classList.remove("active"));
                    a.classList.add("active");
                    this.frameElement.src = fileUrl;
                });
                
                li.appendChild(a);
                ul.appendChild(li);
            });
            
            groupContainer.appendChild(ul);
            containerElement.appendChild(groupContainer);
        }
    }
    
    showError() {
        this.containerElement.innerHTML = "<p>Error al cargar la lista de entregas.</p>";
    }
}

// --- Orquestador / Punto de Entrada ---
document.addEventListener("DOMContentLoaded", () => {
    const pdfContainer = document.getElementById("pdf-list");
    const pdfFrame = document.getElementById("pdf-frame");

    if (!pdfContainer || !pdfFrame) return;

    const repository = new PdfRepository("docs/entregas/pdfs.json");
    const renderer = new SidebarRenderer(pdfContainer, pdfFrame, "docs/entregas");

    repository.getPdfs()
        .then(files => renderer.render(files))
        .catch(error => {
            console.error("Error de inicialización:", error);
            renderer.showError();
        });
});