const driversList = document.getElementById('driversList');
const optionsList = document.getElementById('optionsList');
const linksList = document.getElementById('linksList');
const optionChosen = document.getElementById('optionChosen');
const formStatus = document.getElementById('formStatus');
const adrForm = document.getElementById('adrForm');
const btnCopy = document.getElementById('btnCopy');

const btnModeCreate = document.getElementById('btnModeCreate');
const btnModeEdit = document.getElementById('btnModeEdit');
const modeTitle = document.getElementById('modeTitle');
const editorPanel = document.getElementById('editorPanel');
const btnWriteTab = document.getElementById('btnWriteTab');
const btnPreviewTab = document.getElementById('btnPreviewTab');
const markdownInput = document.getElementById('markdownInput');

const normalizeText = (value) => value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

const getValue = (el, placeholder) => {
    const value = (el.value || '').trim();
    return value || placeholder;
};

const getLines = (value) => value
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);

const setStatus = (message) => {
    formStatus.textContent = message || '';
};

const addDriver = (value = '') => {
    const wrapper = document.createElement('div');
    wrapper.className = 'list-item';

    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = 'Ej: Escalabilidad';
    input.className = 'driver-input';
    input.value = value;

    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'btn btn-ghost';
    remove.textContent = 'Quitar';
    remove.addEventListener('click', () => wrapper.remove());

    wrapper.appendChild(input);
    wrapper.appendChild(remove);
    driversList.appendChild(wrapper);
};

const buildOptionCard = (data = {}) => {
    const card = document.createElement('div');
    card.className = 'option-card';

    const header = document.createElement('div');
    header.className = 'option-header';

    const title = document.createElement('input');
    title.type = 'text';
    title.placeholder = 'Título de la opción';
    title.className = 'option-title';
    title.value = data.title || '';

    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'btn btn-danger';
    remove.textContent = 'Quitar opción';
    remove.addEventListener('click', () => {
        card.remove();
        syncChosenOptions();
    });

    header.appendChild(title);
    header.appendChild(remove);

    const descGroup = document.createElement('div');
    descGroup.className = 'input-group';
    descGroup.innerHTML = `
        <label>Descripción o referencia</label>
        <input type="text" class="option-desc" placeholder="Breve descripción o link" value="${data.desc || ''}">
    `;

    const triple = document.createElement('div');
    triple.className = 'grid-2';
    triple.innerHTML = `
        <div class="input-group">
            <label>Pros (una línea por ítem)</label>
            <textarea rows="3" class="option-pros" placeholder="Mejora X\nReduce Y">${data.pros || ''}</textarea>
        </div>
        <div class="input-group">
            <label>Contras (una línea por ítem)</label>
            <textarea rows="3" class="option-cons" placeholder="Aumenta complejidad">${data.cons || ''}</textarea>
        </div>
    `;

    card.appendChild(header);
    card.appendChild(descGroup);
    card.appendChild(triple);

    title.addEventListener('input', syncChosenOptions);
    optionsList.appendChild(card);
};

const addLink = (value = '') => {
    const wrapper = document.createElement('div');
    wrapper.className = 'list-item';

    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = 'Ej: [ADR anterior](20260512-xxx.md)';
    input.className = 'link-input';
    input.value = value;

    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'btn btn-ghost';
    remove.textContent = 'Quitar';
    remove.addEventListener('click', () => wrapper.remove());

    wrapper.appendChild(input);
    wrapper.appendChild(remove);
    linksList.appendChild(wrapper);
};

const syncChosenOptions = () => {
    const current = optionChosen.value;
    const titles = [...document.querySelectorAll('.option-title')]
        .map(input => input.value.trim())
        .filter(Boolean);

    optionChosen.innerHTML = '';
    
    const analyzeOpt = document.createElement('option');
    analyzeOpt.value = '__pending__';
    analyzeOpt.textContent = 'Decisión Pendiente';
    optionChosen.appendChild(analyzeOpt);
    
    if (titles.length === 0) {
        return;
    }

    titles.forEach(title => {
        const opt = document.createElement('option');
        opt.value = title;
        opt.textContent = title;
        optionChosen.appendChild(opt);
    });

    if (titles.includes(current)) {
        optionChosen.value = current;
    }
};

const clearList = (element) => {
    element.innerHTML = '';
};

const setMode = (mode) => {
    const isEdit = mode === 'edit';
    btnModeCreate.classList.toggle('is-active', !isEdit);
    btnModeEdit.classList.toggle('is-active', isEdit);
    editorPanel.classList.toggle('is-visible', isEdit);
    editorPanel.setAttribute('aria-hidden', (!isEdit).toString());
    modeTitle.textContent = isEdit ? 'Modo editar' : 'Modo crear';
    adrForm.classList.toggle('is-hidden', isEdit);
    if (isEdit) {
        setTab('write');
    }
};

const setTab = (tab) => {
    const isPreview = tab === 'preview';
    btnWriteTab.classList.toggle('is-active', !isPreview);
    btnPreviewTab.classList.toggle('is-active', isPreview);
    editorPanel.classList.toggle('is-preview', isPreview);
    adrForm.classList.toggle('is-hidden', !isPreview);

    if (isPreview) {
        if (!markdownInput.value.trim()) {
            setStatus('Pega el markdown antes de previsualizar.');
            return;
        }
        const parsed = parseMarkdownToForm(markdownInput.value);
        if (parsed) {
            setStatus('Markdown cargado. Puedes editar el formulario.');
        }
    }
};

const parseMarkdownToForm = (markdown) => {
    const lines = markdown.split(/\r?\n/);
    const normalizedLines = lines.map(line => normalizeText(line.trim()));

    const findMeta = (label) => {
        const target = `- ${label.toLowerCase()}:`;
        const index = normalizedLines.findIndex(line => line.startsWith(target));
        if (index === -1) {
            return '';
        }
        return lines[index].split(':').slice(1).join(':').trim();
    };

    const titleLineIndex = normalizedLines.findIndex(line => line.startsWith('# '));
    const title = titleLineIndex !== -1 ? lines[titleLineIndex].replace(/^#\s*/, '').trim() : '';
    const statusRaw = findMeta('status');
    const date = findMeta('date');
    const deciders = findMeta('deciders');
    const tags = findMeta('tags');

    const sections = {};
    let current = '';
    lines.forEach((line) => {
        const trimmed = line.trim();
        if (trimmed.startsWith('## ')) {
            current = normalizeText(trimmed.slice(3));
            sections[current] = [];
            return;
        }
        if (current) {
            sections[current].push(line);
        }
    });

    const context = (sections['contexto y problema'] || []).join('\n').trim();
    const drivers = (sections['atributos de calidad y drivers de decision'] || [])
        .map(line => line.trim())
        .filter(line => line.startsWith('* '))
        .map(line => line.replace(/^\*\s*/, '').trim());

    const optionsConsidered = (sections['alternativas consideradas'] || [])
        .map(line => line.trim())
        .filter(line => line.startsWith('* '))
        .map(line => line.replace(/^\*\s*/, '').trim());

    const resultLines = sections['resultado de la decision'] || sections['decision pendiente'] || [];
    let chosen = '';
    let justification = '';
    let consequenceGood = '';
    let consequenceBad = '';
    let validation = '';
    let currentSection = 'main';
    const justificationLines = [];
    const goodLines = [];
    const badLines = [];
    const validationLines = [];

    resultLines.forEach((line) => {
        const trimmed = line.trim();
        const normalized = normalizeText(trimmed);
        if (!trimmed) {
            return;
        }

        if (trimmed.startsWith('### ')) {
            currentSection = normalizeText(trimmed.slice(4));
            return;
        }

        if (currentSection === 'main') {
            if (normalized.includes('decision pendiente')) {
                chosen = '__pending__';
                return;
            }

            if (normalized.startsWith('alternativa elegida:')) {
                const explicitMatch = trimmed.match(/alternativa elegida:\s*"([^"]+)"/i);
                const looseMatch = trimmed.match(/alternativa elegida:\s*(.+)/i);
                chosen = explicitMatch ? explicitMatch[1].trim() : (looseMatch ? looseMatch[1].trim() : chosen);
                return;
            }

            const justMatch = trimmed.match(/^justificaci[oó]n:\s*(.*)$/i);
            if (justMatch) {
                currentSection = 'justificación';
                if (justMatch[1]) {
                    justificationLines.push(justMatch[1].trim());
                }
                return;
            }
        }

        if (currentSection === 'justificación') {
            justificationLines.push(trimmed);
            return;
        }

        if (currentSection === 'consecuencias positivas' && trimmed.startsWith('*')) {
            goodLines.push(trimmed.replace(/^\*\s*/, '').trim());
            return;
        }

        if (currentSection === 'consecuencias negativas' && trimmed.startsWith('*')) {
            badLines.push(trimmed.replace(/^\*\s*/, '').trim());
            return;
        }

        if (currentSection === 'validación' && trimmed) {
            validationLines.push(trimmed);
            return;
        }
    });

    justification = justificationLines.join('\n').trim();
    consequenceGood = goodLines.join('\n').trim();
    consequenceBad = badLines.join('\n').trim();
    validation = validationLines.join('\n').trim();

    const optionsSectionLines = sections['analisis de alternativas'] || [];
    const detailedOptions = [];
    let currentOption = null;
    let currentDesc = [];
    let currentPros = [];
    let currentCons = [];
    let optionSubSection = 'desc';

    const pushOption = () => {
        if (!currentOption) {
            return;
        }
        detailedOptions.push({
            title: currentOption,
            desc: currentDesc.join('\n').trim(),
            pros: currentPros.join('\n'),
            cons: currentCons.join('\n')
        });
    };

    optionsSectionLines.forEach((line) => {
        const trimmed = line.trim();
        const normalized = normalizeText(trimmed);

        if (trimmed.startsWith('### ')) {
            pushOption();
            currentOption = trimmed.slice(4).trim();
            currentDesc = [];
            currentPros = [];
            currentCons = [];
            optionSubSection = 'desc';
            return;
        }

        if (!currentOption) {
            return;
        }

        if (trimmed.startsWith('#### ')) {
            const sub = normalizeText(trimmed.slice(5).trim());
            if (sub === 'pros') optionSubSection = 'pros';
            else if (sub === 'contras') optionSubSection = 'contras';
            return;
        }

        if (trimmed.startsWith('*')) {
            const itemText = trimmed.replace(/^\*\s*/, '').trim();
            if (optionSubSection === 'pros') {
                currentPros.push(itemText);
            } else if (optionSubSection === 'contras') {
                currentCons.push(itemText);
            } else {
                currentDesc.push(trimmed);
            }
        } else if (trimmed) {
            if (optionSubSection === 'desc') {
                currentDesc.push(trimmed);
            }
        }
    });

    pushOption();

    const detailMap = new Map(
        detailedOptions.map(option => [normalizeText(option.title), option])
    );

    const mergedOptions = optionsConsidered.length
        ? optionsConsidered.map(title => detailMap.get(normalizeText(title)) || { title })
        : detailedOptions;

    if (chosen && !mergedOptions.some(option => option.title === chosen) && chosen !== '__pending__') {
        const matched = mergedOptions.find(option => normalizeText(option.title) === normalizeText(chosen));
        if (matched) {
            chosen = matched.title;
        } else {
            setStatus(`La opción elegida "${chosen}" no coincide con ninguna alternativa.`);
        }
    }

    const finalOptions = mergedOptions.length ? mergedOptions : [{ title: '' }];

    const links = (sections['links'] || [])
        .map(line => line.trim())
        .filter(line => line.startsWith('* '))
        .map(line => line.replace(/^\*\s*/, '').trim());

    document.getElementById('title').value = title;
    const statusNormalized = normalizeText(statusRaw || '');
    document.getElementById('status').value = statusNormalized.includes('superseded by')
        ? 'superseded'
        : (statusNormalized || 'draft');
    document.getElementById('supersededBy').value = statusNormalized.includes('superseded by')
        ? statusRaw.replace(/superseded by\s*/i, '').trim()
        : '';
    document.getElementById('deciders').value = deciders;
    document.getElementById('tags').value = tags;
    document.getElementById('context').value = context;
    document.getElementById('justification').value = justification;
    document.getElementById('consequenceGood').value = consequenceGood;
    document.getElementById('consequenceBad').value = consequenceBad;
    document.getElementById('validation').value = validation;

    clearList(driversList);
    (drivers.length ? drivers : ['']).forEach(driver => addDriver(driver));

    clearList(optionsList);
    finalOptions.forEach(option => buildOptionCard(option));
    syncChosenOptions();
    if (chosen) {
        if (chosen === '__pending__') {
            optionChosen.value = '__pending__';
        } else {
            const chosenMatch = finalOptions.find(option => normalizeText(option.title) === normalizeText(chosen));
            if (chosenMatch) {
                optionChosen.value = chosenMatch.title;
            }
        }
    }

    clearList(linksList);
    (links.length ? links : ['']).forEach(link => addLink(link));

    if (date) {
        setStatus(`Markdown cargado con fecha ${date}.`);
    }

    return true;
};

const buildMarkdown = () => {
    const title = document.getElementById('title').value.trim();
    if (!title) {
        alert('Por favor, ingresa un título');
        return null;
    }

    const status = document.getElementById('status').value;
    const supersededBy = document.getElementById('supersededBy').value.trim();
    const date = new Date().toISOString().split('T')[0];
    const decidersInput = document.getElementById('deciders').value.trim();
    const deciders = decidersInput || 'Decisión Grupal';
    const tags = document.getElementById('tags').value.trim();
    const context = document.getElementById('context').value.trim();
    const justification = document.getElementById('justification').value.trim();
    const consequenceGood = document.getElementById('consequenceGood').value.trim();
    const consequenceBad = document.getElementById('consequenceBad').value.trim();
    const validation = document.getElementById('validation').value.trim();

    const drivers = [...document.querySelectorAll('.driver-input')]
        .map(input => input.value.trim())
        .filter(Boolean);

    const options = [...document.querySelectorAll('.option-card')].map(card => {
        const titleInput = card.querySelector('.option-title');
        const descInput = card.querySelector('.option-desc');
        const prosInput = card.querySelector('.option-pros');
        const consInput = card.querySelector('.option-cons');

        return {
            title: titleInput.value.trim(),
            desc: descInput.value.trim(),
            pros: prosInput.value.trim(),
            cons: consInput.value.trim()
        };
    }).filter(option => option.title);

    if (options.length === 0) {
        alert('Agrega al menos una opción considerada');
        return null;
    }

    let chosen = optionChosen.value;
    if (!chosen || (chosen === '__pending__' && status !== 'draft')) {
        chosen = options[0].title;
    }
    const statusLine = status === 'superseded' && supersededBy
        ? `superseded by ${supersededBy}`
        : status;

    const links = [...document.querySelectorAll('.link-input')]
        .map(input => input.value.trim())
        .filter(Boolean);

    let markdown = `# ${title}\n`;
    if (statusLine) markdown += `- Status: ${statusLine}\n`;
    markdown += `- Date: ${date}\n`;
    markdown += `- Deciders: ${deciders}\n`;
    if (tags) markdown += `- Tags: ${tags}\n`;

    if (context) {
        markdown += `\n## Contexto y Problema\n${context}\n`;
    }

    if (drivers.length > 0) {
        markdown += `\n## Atributos de Calidad y Drivers de Decisión\n`;
        markdown += drivers.map(driver => `* ${driver}`).join('\n') + '\n';
    }

    if (options.length > 0) {
        markdown += `\n## Alternativas Consideradas\n`;
        markdown += options.map(option => `* ${option.title}`).join('\n') + '\n';
    }

    const isPending = chosen === '__pending__' && status === 'draft';
    
    if (isPending) {
        markdown += `\n## Decisión Pendiente\n`;
    } else if ((chosen && chosen !== '__pending__') || justification || consequenceGood || consequenceBad || validation) {
        markdown += `\n## Resultado de la Decisión\n`;
        if (chosen && chosen !== '__pending__') {
            markdown += `\nAlternativa elegida: "${chosen}"\n`;
        }
    }

    if (justification) {
        markdown += `\nJustificación:\n${justification}\n`;
    }

    if (consequenceGood) {
        markdown += `\n### Consecuencias Positivas\n`;
        const lines = getLines(consequenceGood);
        markdown += lines.map(line => `* ${line}`).join('\n') + '\n';
    }

    if (consequenceBad) {
        markdown += `\n### Consecuencias Negativas\n`;
        const lines = getLines(consequenceBad);
        markdown += lines.map(line => `* ${line}`).join('\n') + '\n';
    }

    if (validation) {
        markdown += `\n### Validación\n\n${validation}\n`;
    }

    if (options.length > 0) {
        let hasOptionDetails = false;
        let optionsMarkdown = '';
        
        options.forEach(opt => {
            let optBlock = `\n### ${opt.title}\n`;
            let hasContent = false;

            if (opt.desc) {
                optBlock += `\n${opt.desc}\n`;
                hasContent = true;
            }
            
            const prosLines = getLines(opt.pros);
            const consLines = getLines(opt.cons);
            
            if (prosLines.length > 0) {
                optBlock += `\n#### Pros\n` + prosLines.map(p => `* ${p}`).join('\n') + '\n';
                hasContent = true;
            }
            if (consLines.length > 0) {
                optBlock += `\n#### Contras\n` + consLines.map(c => `* ${c}`).join('\n') + '\n';
                hasContent = true;
            }
            
            if (hasContent) {
                optionsMarkdown += optBlock;
                hasOptionDetails = true;
            }
        });

        if (hasOptionDetails) {
            markdown += `\n## Análisis de Alternativas\n` + optionsMarkdown;
        }
    }

    if (links.length > 0) {
        markdown += `\n## Links\n`;
        markdown += links.map(link => `* ${link}`).join('\n') + '\n';
    }

    // Normaliza saltos de línea (elimina los excesos para mantener el archivo limpio)
    markdown = markdown.replace(/\n{3,}/g, '\n\n').trim() + '\n';

    const dateCompact = date.replace(/-/g, '');
    const slug = title.toLowerCase().trim()
        .replace(/[^\w\s-]/gu, '')  
        .replace(/[\s_-]+/g, '-')    
        .replace(/^-+|-+$/g, '');    
    const filename = `${dateCompact}-${slug}.md`;

    return { markdown, filename };
};

const handleDownload = () => {
    const result = buildMarkdown();
    if (!result) {
        return;
    }
    const blob = new Blob([result.markdown], { type: 'text/markdown' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = result.filename;
    link.click();
    setStatus('Archivo descargado.');
};

const handleCopy = async () => {
    const result = buildMarkdown();
    if (!result) {
        return;
    }
    try {
        await navigator.clipboard.writeText(result.markdown);
        setStatus('Markdown copiado al portapapeles.');
    } catch (error) {
        const helper = document.createElement('textarea');
        helper.value = result.markdown;
        document.body.appendChild(helper);
        helper.select();
        document.execCommand('copy');
        document.body.removeChild(helper);
        setStatus('Markdown copiado al portapapeles.');
    }
};

document.getElementById('btnAddDriver').addEventListener('click', () => addDriver());
document.getElementById('btnAddOption').addEventListener('click', () => {
    buildOptionCard();
    syncChosenOptions();
});
document.getElementById('btnAddLink').addEventListener('click', () => addLink());

btnModeCreate.addEventListener('click', () => setMode('create'));
btnModeEdit.addEventListener('click', () => setMode('edit'));
btnWriteTab.addEventListener('click', () => setTab('write'));
btnPreviewTab.addEventListener('click', () => setTab('preview'));

document.getElementById('btnGenerate').addEventListener('click', handleDownload);
btnCopy.addEventListener('click', handleCopy);

addDriver();
addDriver();
buildOptionCard();
addLink();
syncChosenOptions();
setMode('create');