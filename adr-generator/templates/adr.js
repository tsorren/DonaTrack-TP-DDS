/**
 * ADR Template Module
 * 
 * Implementa la plantilla de Architecture Decision Record.
 * Define: estructura de formulario, parsing, generación, validación.
 * 
 * Interfaz requerida:
 * - id, name, getFormSpec()
 * - parseMarkdownToFormData(markdown)
 * - buildMarkdownFromFormData(data)
 * - validateFormData(data)
 */

function ADRTemplate() {
  // Properties
  this.id = 'adr';
  this.name = 'Architecture Decision Record';

  /**
   * Define la especificación del formulario para ADR.
   * Retorna array de FieldSpec que UIEngine utilizará para renderizar.
   * @returns {array} Especificación de campos
   */
  this.getFormSpec = function () {
    return [
      // Sección 1: Identificación
      {
        type: 'section',
        title: '1. Identificación',
        fields: [
          {
            type: 'text',
            id: 'title',
            label: 'Título de la decisión',
            placeholder: 'Ej: Uso de RabbitMQ para notificaciones',
            required: true,
            dataAttribute: 'title'
          },
          {
            type: 'section',
            columns: 2,
            fields: [
              {
                type: 'select',
                id: 'status',
                label: 'Estado',
                dataAttribute: 'status',
                options: [
                  { value: 'draft', label: 'Draft' },
                  { value: 'proposed', label: 'Proposed' },
                  { value: 'accepted', label: 'Accepted' },
                  { value: 'rejected', label: 'Rejected' },
                  { value: 'deprecated', label: 'Deprecated' },
                  { value: 'superseded', label: 'Superseded' }
                ]
              },
              {
                type: 'text',
                id: 'tags',
                label: 'Tags (opcional)',
                placeholder: 'arquitectura, backend (opcional)',
                dataAttribute: 'tags'
              }
            ]
          },
          {
            type: 'section',
            columns: 2,
            fields: [
              {
                type: 'text',
                id: 'deciders',
                label: 'Decisores (integrantes)',
                placeholder: 'Decisión Grupal (si vacío)',
                dataAttribute: 'deciders'
              },
              {
                type: 'text',
                id: 'supersededBy',
                label: 'Superseded by (opcional)',
                placeholder: '[xxx](yyyymmdd-xxx.md)',
                dataAttribute: 'supersededBy'
              }
            ]
          }
        ]
      },

      // Sección 2: Contexto y Drivers
      {
        type: 'section',
        title: '2. Contexto y Drivers',
        fields: [
          {
            type: 'textarea',
            id: 'context',
            label: 'Problema a resolver',
            placeholder: 'Qué requerimiento estamos resolviendo? (opcional)',
            rows: 4,
            dataAttribute: 'context'
          },
          {
            type: 'list',
            id: 'drivers',
            label: 'Atributos de Calidad y Drivers',
            hint: 'Agrega tantos como necesites.',
            addButtonLabel: 'Agregar driver',
            itemFieldType: 'text'
          }
        ]
      },

      // Sección 3: Alternativas
      {
        type: 'section',
        title: '3. Alternativas y Justificación',
        fields: [
          {
            type: 'adr-options',
            id: 'options',
            label: 'Alternativas Consideradas',
            hint: 'Agrega alternativas detalladas con pros y contras.',
            addButtonLabel: 'Agregar alternativa'
          },
          {
            type: 'section',
            columns: 2,
            fields: [
              {
                type: 'select',
                id: 'optionChosen',
                label: 'Alternativa elegida (opcional)',
                dataAttribute: 'optionChosen',
                options: [
                  { value: '__pending__', label: 'Decisión Pendiente' }
                ]
              },
              {
                type: 'textarea',
                id: 'justification',
                label: 'Justificación (opcional)',
                placeholder: 'Describe por qué elegiste la alternativa',
                rows: 2,
                dataAttribute: 'justification'
              }
            ]
          }
        ]
      },

      // Sección 4: Consecuencias
      {
        type: 'section',
        title: '4. Consecuencias y Validación',
        fields: [
          {
            type: 'section',
            columns: 2,
            fields: [
              {
                type: 'textarea',
                id: 'consequenceGood',
                label: 'Consecuencia positiva (opcional)',
                placeholder: 'Mejora de atributo de calidad...',
                rows: 2,
                dataAttribute: 'consequenceGood'
              },
              {
                type: 'textarea',
                id: 'consequenceBad',
                label: 'Consecuencia negativa (opcional)',
                placeholder: 'Complejidad, latencia, etc.',
                rows: 2,
                dataAttribute: 'consequenceBad'
              }
            ]
          },
          {
            type: 'textarea',
            id: 'validation',
            label: 'Validación (opcional)',
            placeholder: 'Cómo se validará esta decisión?',
            rows: 3,
            dataAttribute: 'validation'
          }
        ]
      },

      // Sección 5: Links
      {
        type: 'section',
        title: '5. Links',
        fields: [
          {
            type: 'list',
            id: 'links',
            label: 'Referencias o Links',
            hint: 'Pega links en formato Markdown o texto simple.',
            addButtonLabel: 'Agregar link',
            itemFieldType: 'text'
          }
        ]
      }
    ];
  };

  /**
   * Valida que los datos cumplan requisitos de ADR.
   * @param {object} formData - Datos del formulario
   * @returns {object} { isValid: boolean, errors: array }
   */
  this.validateFormData = function (formData) {
    const errors = [];

    if (!formData.title || !formData.title.trim()) {
      errors.push('Título es obligatorio');
    }

    const optionsArray = formData.options || [];
    if (!Array.isArray(optionsArray) || optionsArray.length === 0) {
      errors.push('Al menos una alternativa es requerida');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  };

  // ============================================
  // Métodos de Extensión para Componentes Propios
  // ============================================

  /**
   * Sincroniza las opciones escritas en las tarjetas con el <select> de elección.
   */
  const syncChosenOptions = () => {
    const optionChosen = document.getElementById('optionChosen');
    if (!optionChosen) return;

    const current = optionChosen.value;
    const titles = [...document.querySelectorAll('.option-title')]
      .map(input => input.value.trim())
      .filter(Boolean);

    optionChosen.innerHTML = '';

    const analyzeOpt = document.createElement('option');
    analyzeOpt.value = '__pending__';
    analyzeOpt.textContent = 'Decisión Pendiente';
    optionChosen.appendChild(analyzeOpt);

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

  this.getCustomFieldRenderers = function () {
    return {
      'adr-options': (fieldSpec) => {
        const card = document.createElement('div');
        card.className = 'card';

        const header = document.createElement('div');
        header.className = 'list-header';

        const info = document.createElement('div');
        const title = document.createElement('h3');
        title.textContent = fieldSpec.label;
        const hint = document.createElement('p');
        hint.className = 'hint';
        hint.textContent = fieldSpec.hint || '';
        info.appendChild(title);
        info.appendChild(hint);

        const btnAdd = document.createElement('button');
        btnAdd.type = 'button';
        btnAdd.className = 'btn btn-outline';
        btnAdd.textContent = `+ ${fieldSpec.addButtonLabel || 'Agregar'}`;

        header.appendChild(info);
        header.appendChild(btnAdd);

        const list = document.createElement('div');
        list.className = 'list';
        list.id = `${fieldSpec.id}Container`;

        card.appendChild(header);
        card.appendChild(list);

        const buildOptionCard = (data = {}) => {
          const optCard = document.createElement('div');
          optCard.className = 'option-card card';
          optCard.style.marginTop = '1rem';
          optCard.style.padding = '1rem';
          optCard.style.border = '1px solid var(--border-color)';
          optCard.style.borderRadius = 'var(--radius)';
          optCard.style.background = 'var(--bg-secondary)';

          const optHeader = document.createElement('div');
          optHeader.className = 'list-header';
          optHeader.style.marginBottom = '1rem';

          const titleInput = document.createElement('input');
          titleInput.type = 'text';
          titleInput.placeholder = 'Título de la opción';
          titleInput.className = 'option-title';
          titleInput.value = data.title || '';
          titleInput.style.flex = '1';

          const remove = document.createElement('button');
          remove.type = 'button';
          remove.className = 'btn btn-ghost';
          remove.textContent = 'Quitar';
          remove.addEventListener('click', () => {
            optCard.remove();
            syncChosenOptions();
          });

          optHeader.appendChild(titleInput);
          optHeader.appendChild(remove);

          const descGroup = document.createElement('div');
          descGroup.className = 'input-group';
          descGroup.innerHTML = `
            <label>Descripción o referencia</label>
            <input type="text" class="option-desc" placeholder="Breve descripción o link" value="${data.desc || ''}">
          `;

          const triple = document.createElement('div');
          triple.className = 'grid grid-2';
          triple.style.gap = '1rem';
          triple.style.marginTop = '1rem';
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

          optCard.appendChild(optHeader);
          optCard.appendChild(descGroup);
          optCard.appendChild(triple);

          titleInput.addEventListener('input', syncChosenOptions);
          list.appendChild(optCard);
        };

        btnAdd.addEventListener('click', () => {
          buildOptionCard();
          syncChosenOptions();
        });

        // Exponemos la función al contenedor para que el populator la use
        list._buildOptionCard = buildOptionCard;

        // Iniciar con una tarjeta por defecto si la lista está vacía
        setTimeout(() => {
          if (list.children.length === 0) {
            buildOptionCard();
            syncChosenOptions();
          }
        }, 0);

        return card;
      }
    };
  };

  this.getCustomFieldCollectors = function () {
    return {
      'adr-options': (fieldId, container) => {
        const listContainer = container.querySelector(`#${fieldId}Container`);
        if (!listContainer) return [];

        return [...listContainer.querySelectorAll('.option-card')].map(card => ({
          title: card.querySelector('.option-title').value.trim(),
          desc: card.querySelector('.option-desc').value.trim(),
          pros: card.querySelector('.option-pros').value.trim(),
          cons: card.querySelector('.option-cons').value.trim()
        })).filter(option => option.title);
      }
    };
  };

  this.getCustomFieldPopulators = function () {
    return {
      'adr-options': (fieldId, data, container) => {
        const listContainer = container.querySelector(`#${fieldId}Container`);
        if (!listContainer || !listContainer._buildOptionCard) return;

        Utils.clearElement(listContainer);
        if (Array.isArray(data)) {
          data.forEach(item => {
            const obj = typeof item === 'string' ? { title: item } : item;
            listContainer._buildOptionCard(obj);
          });
        }
        syncChosenOptions();
      }
    };
  };

  /**
   * Parsea markdown de ADR a objeto formData.
   * @param {string} markdown - Markdown de ADR
   * @returns {object} FormData para llenar el form
   */
  this.parseMarkdownToFormData = function (markdown) {
    const lines = markdown.split(/\r?\n/);
    const normalizedLines = lines.map(line => Utils.normalizeText(line.trim()));

    // Helper para encontrar meta línea
    const findMeta = (label) => {
      const target = `- ${label.toLowerCase()}`;
      const idx = normalizedLines.findIndex(line =>
        line.startsWith(target)
      );
      return idx !== -1 ? lines[idx].replace(new RegExp(`-\\s*${label}:?\\s*`, 'i'), '').trim() : '';
    };

    const titleLineIndex = normalizedLines.findIndex(line => line.startsWith('# '));
    const title = titleLineIndex !== -1 ? lines[titleLineIndex].replace(/^#\s*/, '').trim() : '';
    const statusRaw = findMeta('status');
    const date = findMeta('date');
    const deciders = findMeta('deciders');
    const tags = findMeta('tags');

    // Parse secciones
    const sections = {};
    let current = '';
    lines.forEach((line) => {
      const normalized = Utils.normalizeText(line.trim());
      if (normalized.startsWith('##')) {
        const header = line.replace(/^##\s*/, '').toLowerCase();
        current = header;
        sections[header] = [];
      } else if (current && line.trim()) {
        sections[current].push(line);
      }
    });

    const context = (sections['contexto y problema'] || []).join('\n').trim();
    const drivers = (sections['atributos de calidad y drivers de decision'] || [])
      .map(line => line.trim())
      .filter(line => line.startsWith('*'))
      .map(line => line.replace(/^\*\s*/, '').trim());

    const optionsConsidered = (sections['alternativas consideradas'] || [])
      .map(line => line.trim())
      .filter(line => line.startsWith('*'))
      .map(line => line.replace(/^\*\s*/, '').trim());

    const optionsSectionLines = sections['analisis de alternativas'] || [];
    const detailedOptions = [];
    let currentOption = null;
    let currentDesc = [];
    let currentPros = [];
    let currentCons = [];
    let optionSubSection = 'desc';

    const pushOption = () => {
      if (!currentOption) return;
      detailedOptions.push({
        title: currentOption,
        desc: currentDesc.join('\n').trim(),
        pros: currentPros.join('\n'),
        cons: currentCons.join('\n')
      });
    };

    optionsSectionLines.forEach((line) => {
      const trimmed = line.trim();
      const normalized = Utils.normalizeText(trimmed);

      if (trimmed.startsWith('### ')) {
        pushOption();
        currentOption = trimmed.slice(4).trim();
        currentDesc = [];
        currentPros = [];
        currentCons = [];
        optionSubSection = 'desc';
        return;
      }

      if (!currentOption) return;

      if (trimmed.startsWith('#### ')) {
        const sub = Utils.normalizeText(trimmed.slice(5).trim());
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
      detailedOptions.map(option => [Utils.normalizeText(option.title), option])
    );

    const mergedOptions = optionsConsidered.length
      ? optionsConsidered.map(title => detailMap.get(Utils.normalizeText(title)) || { title, desc: '', pros: '', cons: '' })
      : detailedOptions;

    // Parse resultado de la decisión
    const resultLines = sections['resultado de la decision'] || sections['decision pendiente'] || [];
    let chosen = '';
    let justification = '';
    let consequenceGood = '';
    let consequenceBad = '';
    let validation = '';

    const justificationLines = [];
    const goodLines = [];
    const badLines = [];
    const validationLines = [];
    let currentSubsection = 'main';

    resultLines.forEach((line) => {
      const normalized = Utils.normalizeText(line.trim());

      if (normalized.includes('alternativa elegida')) {
        const match = line.match(/"([^"]+)"/);
        if (match) chosen = match[1];
      } else if (normalized.startsWith('justificacion')) {
        currentSubsection = 'justification';
      } else if (normalized.startsWith('### consecuencias positivas')) {
        currentSubsection = 'good';
      } else if (normalized.startsWith('### consecuencias negativas')) {
        currentSubsection = 'bad';
      } else if (normalized.startsWith('### validacion')) {
        currentSubsection = 'validation';
      } else if (line.trim().startsWith('*')) {
        const item = line.replace(/^\*\s*/, '').trim();
        if (currentSubsection === 'good') goodLines.push(item);
        if (currentSubsection === 'bad') badLines.push(item);
      } else if (line.trim() && !normalized.startsWith('#')) {
        if (currentSubsection === 'justification') justificationLines.push(line);
        if (currentSubsection === 'validation') validationLines.push(line);
      }
    });

    justification = justificationLines.join('\n').trim();
    consequenceGood = goodLines.join('\n');
    consequenceBad = badLines.join('\n');
    validation = validationLines.join('\n').trim();

    const links = (sections['links'] || [])
      .map(line => line.trim())
      .filter(line => line.startsWith('*'))
      .map(line => line.replace(/^\*\s*/, '').trim());

    // Retornar en formato que UIEngine entienda
    return {
      title,
      status: Utils.normalizeText(statusRaw || 'draft'),
      deciders,
      tags,
      context,
      drivers: drivers.filter(Boolean),
      options: mergedOptions,
      optionChosen: chosen,
      justification,
      consequenceGood,
      consequenceBad,
      validation,
      links: links.filter(Boolean),
      supersededBy: Utils.normalizeText(statusRaw || '').includes('superseded by')
        ? statusRaw.replace(/superseded by\s*/i, '').trim()
        : ''
    };
  };

  /**
   * Genera markdown de ADR desde formData.
   * @param {object} formData - Datos del formulario recolectados
   * @returns {string} Markdown generado
   */
  this.buildMarkdownFromFormData = function (formData) {
    const title = (formData.title || '').trim();
    const status = formData.status || 'draft';
    const supersededBy = (formData.supersededBy || '').trim();
    const date = new Date().toISOString().split('T')[0];
    const decidersInput = (formData.deciders || '').trim();
    const deciders = decidersInput || 'Decisión Grupal';
    const tags = (formData.tags || '').trim();
    const context = (formData.context || '').trim();
    const justification = (formData.justification || '').trim();
    const consequenceGood = (formData.consequenceGood || '').trim();
    const consequenceBad = (formData.consequenceBad || '').trim();
    const validation = (formData.validation || '').trim();

    const drivers = Array.isArray(formData.drivers)
      ? formData.drivers.filter(Boolean)
      : [];

    const options = Array.isArray(formData.options)
      ? formData.options.filter(o => o && o.title && o.title.trim())
      : [];

    const links = Array.isArray(formData.links)
      ? formData.links.filter(Boolean)
      : [];

    let markdown = `# ${title}\n`;

    const statusLine = status === 'superseded' && supersededBy
      ? `superseded by ${supersededBy}`
      : status;

    if (statusLine) markdown += `- Status: ${statusLine}\n`;
    markdown += `- Date: ${date}\n`;
    markdown += `- Deciders: ${deciders}\n`;
    if (tags) markdown += `- Tags: ${tags}\n`;

    if (context) {
      markdown += `\n## Contexto y Problema\n${context}\n`;
    }

    if (drivers.length > 0) {
      markdown += `\n## Atributos de Calidad y Drivers de Decisión\n`;
      markdown += drivers.map(d => `* ${d}`).join('\n') + '\n';
    }

    if (options.length > 0) {
      markdown += `\n## Alternativas Consideradas\n`;
      markdown += options.map(o => `* ${o.title}`).join('\n') + '\n';
    }

    let chosen = formData.optionChosen || '';
    if (chosen === '__pending__' && status === 'draft') {
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
      const lines = Utils.getLines(consequenceGood);
      markdown += lines.map(l => `* ${l}`).join('\n') + '\n';
    }

    if (consequenceBad) {
      markdown += `\n### Consecuencias Negativas\n`;
      const lines = Utils.getLines(consequenceBad);
      markdown += lines.map(l => `* ${l}`).join('\n') + '\n';
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

        const prosLines = Utils.getLines(opt.pros || '');
        const consLines = Utils.getLines(opt.cons || '');

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
      markdown += links.map(l => `* ${l}`).join('\n') + '\n';
    }

    return Utils.normalizeMarkdown(markdown);
  };

  /**
   * Retorna patrón de nombre de archivo para esta plantilla.
   * @returns {string} Patrón (ej: 'yyyymmdd-titulo.md')
   */
  this.getFilenamePattern = function () {
    return 'yyyymmdd-titulo.md';
  };
}

// Export para que TemplateRegistry pueda acceder
window.ADRTemplate = ADRTemplate;
