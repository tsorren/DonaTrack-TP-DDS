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
    if (titles.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'Agrega al menos una opción';
        optionChosen.appendChild(opt);
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

const extractBecause = (line) => {
    const cleaned = line.replace(/^\*\s*/, '').trim();
    const match = cleaned.split(/porque:?/i);
    if (match.length > 1) {
        return match.slice(1).join('porque').trim();
    }
    const commaSplit = cleaned.split(',').slice(1).join(',').trim();
    return commaSplit || cleaned;
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

    const resultLines = sections['resultado de la decision'] || [];
    let chosen = '';
    let justification = '';
    let consequenceGood = '';
    let consequenceBad = '';
    let validation = '';
    let subSection = 'main';
    const validationLines = [];

    resultLines.forEach((line) => {
        const trimmed = line.trim();
        const normalized = normalizeText(trimmed);
        if (trimmed.startsWith('### ')) {
            subSection = normalizeText(trimmed.slice(4));
            return;
        }

        if (subSection === 'main' && (normalized.startsWith('opcion elegida:') || normalized.startsWith('alternativa elegida:'))) {
            const match = normalized.match(/(opcion|alternativa) elegida:\s*"?([^\"]+)"?,\s*porque:?\s*(.+)\.?/i);
            if (match) {
                chosen = match[2].trim();
                justification = match[3].trim();
            }
        }

        if (subSection === 'consecuencias' && normalized.startsWith('* bueno')) {
            consequenceGood = extractBecause(line);
        }

        if (subSection === 'consecuencias' && normalized.startsWith('* malo')) {
            consequenceBad = extractBecause(line);
        }

        if ((subSection === 'confirmacion' || subSection === 'validacion') && trimmed && !trimmed.startsWith('#')) {
            validationLines.push(trimmed);
        }
    });

    validation = validationLines.join('\n').trim();

    const optionsSectionLines = sections['pros y contras de las alternativas'] || [];
    const detailedOptions = [];
    let currentOption = null;
    let currentDesc = [];
    let currentPros = [];
    let currentCons = [];

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
            return;
        }

        if (!currentOption) {
            return;
        }

        if (trimmed.startsWith('*')) {
            const itemText = extractBecause(line);
            if (normalized.startsWith('* bueno')) {
                currentPros.push(itemText);
            } else if (normalized.startsWith('* malo')) {
                currentCons.push(itemText);
            }
        } else if (trimmed) {
            currentDesc.push(trimmed);
        }
    });

    pushOption();

    const detailMap = new Map(
        detailedOptions.map(option => [normalizeText(option.title), option])
    );

    const mergedOptions = optionsConsidered.length
        ? optionsConsidered.map(title => detailMap.get(normalizeText(title)) || { title })
        : detailedOptions;

    if (chosen && !mergedOptions.some(option => option.title === chosen)) {
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
        const chosenMatch = finalOptions.find(option => normalizeText(option.title) === normalizeText(chosen));
        if (chosenMatch) {
            optionChosen.value = chosenMatch.title;
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
    const deciders = getValue(document.getElementById('deciders'), '{lista de integrantes involucrados en la decisión}');
    const tags = getValue(document.getElementById('tags'), '{lista}');
    const context = getValue(document.getElementById('context'), '{contexto}');
    const justification = getValue(document.getElementById('justification'), '{justificación}');
    const consequenceGood = getValue(document.getElementById('consequenceGood'), '{consecuencia positiva}');
    const consequenceBad = getValue(document.getElementById('consequenceBad'), '{consecuencia negativa}');
    const validation = getValue(document.getElementById('validation'), '{validación}');

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
            pros: prosInput.value,
            cons: consInput.value
        };
    }).filter(option => option.title);

    if (options.length === 0) {
        alert('Agrega al menos una opción considerada');
        return null;
    }

    const chosen = optionChosen.value || options[0].title;
    const statusLine = status === 'superseded' && supersededBy
        ? `superseded by ${supersededBy}`
        : status;

    const driverLines = drivers.length
        ? drivers.map(driver => `* ${driver}`).join('\n')
        : '* {driver de decisión}';

    const optionLines = options
        .map(option => `* ${option.title}`)
        .join('\n');

    const formatQualityList = (items, label) => {
        if (items.length === 0) {
            return '';
        }
        return items.map(item => `* ${label}, porque: ${item}`).join('\n');
    };

    const formatOptionSection = (option) => {
        const pros = formatQualityList(getLines(option.pros), 'Bueno');
        const cons = formatQualityList(getLines(option.cons), 'Malo');
        const listBlocks = [pros, cons].filter(Boolean).join('\n');
        const desc = option.desc ? `${option.desc}\n\n` : '';
        const fallback = '* Bueno, porque: {argumento a}\n* Malo, porque: {argumento b}';

        return `### ${option.title}\n\n${desc}${listBlocks || fallback}`;
    };

    const optionsDetails = options.map(formatOptionSection).join('\n\n');

    const links = [...document.querySelectorAll('.link-input')]
        .map(input => input.value.trim())
        .filter(Boolean);

    const linkLines = links.length
        ? links.map(link => `* ${link}`).join('\n')
        : '';

    const markdown = `# ${title}
- Status: ${statusLine}
- Date: ${date}
- Deciders: ${deciders}
- Tags: ${tags}

## Contexto y Problema

${context}

## Atributos de Calidad y Drivers de Decisión
${driverLines}

## Alternativas Consideradas
${optionLines}

## Resultado de la Decisión

Alternativa elegida: "${chosen}", porque: ${justification}.

### Consecuencias
* Bueno, porque: ${consequenceGood}
* Malo, porque: ${consequenceBad}

### Validación

${validation}

## Pros y Contras de las Alternativas

${optionsDetails}

## Links
${linkLines}
`;

    const dateCompact = date.replace(/-/g, '');
    const slug = title.toLowerCase().trim()
        .replace(/[^\w\s-]/g, '')
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