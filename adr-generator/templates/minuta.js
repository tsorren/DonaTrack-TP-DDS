/**
 * Minuta Template Module
 * 
 * Implementa la plantilla de Minuta de Reunión.
 * Estructura 2-D: Integrante → Tareas
 */

function MinutaTemplate() {
  this.id = 'minuta';
  this.name = 'Minuta de Reunión';

  /**
   * Define la especificación del formulario para Minuta.
   * @returns {array} Especificación de campos
   */
  this.getFormSpec = function () {
    return [
      // Sección 1: Información General
      {
        type: 'section',
        title: '1. Información General',
        fields: [
          {
            type: 'section',
            columns: 3,
            fields: [
              {
                type: 'text',
                inputType: 'date',
                id: 'fecha',
                label: 'Fecha',
                placeholder: 'dd/mm/yyyy',
                required: true,
                dataAttribute: 'fecha'
              },
              {
                type: 'text',
                id: 'horaInicio',
                label: 'Hora Inicio',
                placeholder: 'HH:MM',
                dataAttribute: 'horaInicio'
              },
              {
                type: 'text',
                id: 'horaFin',
                label: 'Hora Fin',
                placeholder: 'HH:MM',
                dataAttribute: 'horaFin'
              }
            ]
          }
        ]
      },

      // Sección 2: Participantes
      {
        type: 'section',
        title: '2. Participantes',
        fields: [
          {
            type: 'list',
            id: 'participantes',
            label: 'Participantes',
            hint: 'Agrega los integrantes asistentes.',
            addButtonLabel: 'Agregar participante',
            itemFieldType: 'text'
          }
        ]
      },

      // Sección 3: Temas
      {
        type: 'section',
        title: '3. Temas / Dificultades Abordadas',
        fields: [
          {
            type: 'list',
            id: 'temas',
            label: 'Temas Abordados',
            hint: 'Temas o dificultades discutidas en la reunión.',
            addButtonLabel: 'Agregar tema',
            itemFieldType: 'text'
          }
        ]
      },

      // Sección 4: Resultados
      {
        type: 'section',
        title: '4. Resultados',
        fields: [
          {
            type: 'list',
            id: 'resultados',
            label: 'Resultados Obtenidos',
            hint: 'Decisiones, conclusiones o resultados de la reunión.',
            addButtonLabel: 'Agregar resultado',
            itemFieldType: 'text'
          }
        ]
      },

      // Sección 5: Tareas Asignadas
      {
        type: 'section',
        title: '5. Tareas Asignadas',
        fields: [
          {
            type: 'custom-tasks',
            id: 'tareas',
            label: 'Tareas por Integrante',
            hint: 'Ingresa integrantes y sus tareas asociadas.'
          }
        ]
      }
    ];
  };

  /**
   * Valida datos de Minuta.
   * @param {object} formData
   * @returns {object} { isValid, errors }
   */
  this.validateFormData = function (formData) {
    const errors = [];

    if (!formData.fecha || !formData.fecha.trim()) {
      errors.push('Fecha es obligatoria');
    }

    const participantes = Array.isArray(formData.participantes)
      ? formData.participantes.filter(p => p && p.trim())
      : [];

    if (participantes.length === 0) {
      errors.push('Al menos un participante es requerido');
    }

    // Tareas es un objeto { integrante: [{ desc, fechaLimite }, ...] }
    const tareas = formData.tareas || {};
    let totalTasks = 0;
    Object.values(tareas).forEach(tasks => {
      if (Array.isArray(tasks)) {
        totalTasks += tasks.filter(t => t && t.descripcion && t.descripcion.trim()).length;
      }
    });

    return {
      isValid: errors.length === 0,
      errors
    };
  };

  // ============================================
  // Métodos de Extensión para Componentes Propios
  // ============================================

  this.getCustomFieldRenderers = function () {
    return {
      'custom-tasks': (fieldSpec) => {
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

        const btnAddIntegrante = document.createElement('button');
        btnAddIntegrante.type = 'button';
        btnAddIntegrante.className = 'btn btn-outline';
        btnAddIntegrante.textContent = '+ Agregar Integrante';

        header.appendChild(info);
        header.appendChild(btnAddIntegrante);

        const list = document.createElement('div');
        list.className = 'list';
        list.id = `${fieldSpec.id}Container`;

        card.appendChild(header);
        card.appendChild(list);

        const buildIntegranteCard = (integranteName = '') => {
          const intCard = document.createElement('div');
          intCard.className = 'integrante-card card';
          intCard.style.marginTop = '1rem';
          intCard.style.padding = '1rem';
          intCard.style.border = '1px solid var(--border-color)';
          intCard.style.borderRadius = 'var(--radius)';
          intCard.style.background = 'var(--bg-secondary)';

          const intHeader = document.createElement('div');
          intHeader.className = 'list-header';
          intHeader.style.marginBottom = '1rem';

          const nameInput = document.createElement('select');
          nameInput.className = 'integrante-name';
          nameInput.style.flex = '1';

          const initOpt = document.createElement('option');
          initOpt.value = integranteName;
          initOpt.textContent = integranteName || '-- Seleccionar Integrante --';
          nameInput.appendChild(initOpt);
          nameInput.value = integranteName;

          const updateOptions = () => {
            const currentVal = nameInput.value;
            nameInput.innerHTML = '';
            
            const container = document.getElementById('participantesContainer');
            const inputs = container ? container.querySelectorAll('input') : [];
            const participantes = Array.from(inputs).map(i => i.value.trim()).filter(Boolean);
            
            participantes.sort((a, b) => a.localeCompare(b));
            const unique = [...new Set(participantes)];

            // Obtener participantes ya seleccionados en otros selectores
            const allSelects = document.querySelectorAll('.integrante-name');
            const selectedElsewhere = Array.from(allSelects)
              .map(s => s.value)
              .filter(v => v && v !== currentVal);

            const defaultOpt = document.createElement('option');
            defaultOpt.value = '';
            defaultOpt.textContent = '-- Seleccionar Integrante --';
            nameInput.appendChild(defaultOpt);

            let found = false;
            unique.forEach(p => {
              if (selectedElsewhere.includes(p)) return; // No mostrar si ya fue asignado

              const opt = document.createElement('option');
              opt.value = p;
              opt.textContent = p;
              if (p === currentVal) found = true;
              nameInput.appendChild(opt);
            });

            if (currentVal && !found) {
              const opt = document.createElement('option');
              opt.value = currentVal;
              opt.textContent = currentVal;
              nameInput.appendChild(opt);
            }
            nameInput.value = currentVal;
          };

          nameInput.addEventListener('focus', updateOptions);
          nameInput.addEventListener('mouseenter', updateOptions);

          const removeInt = document.createElement('button');
          removeInt.type = 'button';
          removeInt.className = 'btn btn-ghost';
          removeInt.textContent = 'Quitar Integrante';
          removeInt.addEventListener('click', () => intCard.remove());

          intHeader.appendChild(nameInput);
          intHeader.appendChild(removeInt);

          const tasksList = document.createElement('div');
          tasksList.className = 'tasks-list';
          
          const btnAddTask = document.createElement('button');
          btnAddTask.type = 'button';
          btnAddTask.className = 'btn btn-outline btn-sm';
          btnAddTask.textContent = '+ Agregar Tarea';
          btnAddTask.style.marginTop = '1rem';

          intCard.appendChild(intHeader);
          intCard.appendChild(tasksList);
          intCard.appendChild(btnAddTask);

          const buildTaskCard = (taskData = {}) => {
            const taskCard = document.createElement('div');
            taskCard.className = 'task-card';
            taskCard.style.marginTop = '0.5rem';
            taskCard.style.padding = '0.75rem';
            taskCard.style.border = '1px dashed var(--border-color)';
            taskCard.style.borderRadius = 'var(--radius)';
            taskCard.style.background = 'var(--bg-primary)';

            const taskHeader = document.createElement('div');
            taskHeader.className = 'list-header';
            taskHeader.style.marginBottom = '0.5rem';

            const titleInput = document.createElement('input');
            titleInput.type = 'text';
            titleInput.placeholder = 'Título de la tarea';
            titleInput.className = 'task-title';
            titleInput.value = taskData.titulo || '';
            titleInput.style.flex = '1';

            const removeTask = document.createElement('button');
            removeTask.type = 'button';
            removeTask.className = 'btn btn-ghost btn-sm';
            removeTask.textContent = 'Quitar Tarea';
            removeTask.addEventListener('click', () => taskCard.remove());

            taskHeader.appendChild(titleInput);
            taskHeader.appendChild(removeTask);

            const extraGroup = document.createElement('div');
            extraGroup.className = 'grid grid-2';
            extraGroup.style.gap = '1rem';
            extraGroup.innerHTML = `
              <div class="input-group">
                <label>Fecha Límite</label>
                <input type="date" class="task-date" value="${taskData.fechaLimite || ''}">
              </div>
              <div class="input-group">
                <label>Descripción</label>
                <textarea rows="2" class="task-desc" placeholder="Detalle de la tarea">${taskData.descripcion || ''}</textarea>
              </div>
            `;

            taskCard.appendChild(taskHeader);
            taskCard.appendChild(extraGroup);
            tasksList.appendChild(taskCard);
          };

          btnAddTask.addEventListener('click', () => buildTaskCard());

          // Exponer para el populator
          intCard._buildTaskCard = buildTaskCard;

          list.appendChild(intCard);
          return intCard;
        };

        btnAddIntegrante.addEventListener('click', () => {
          const intCard = buildIntegranteCard();
        });

        // Exponer para el populator
        list._buildIntegranteCard = buildIntegranteCard;

        // Iniciar con un integrante y una tarea por defecto
        setTimeout(() => {
          if (list.children.length === 0) {
            const intCard = buildIntegranteCard();
          }
        }, 0);

        return card;
      }
    };
  };

  this.getCustomFieldCollectors = function () {
    return {
      'custom-tasks': (fieldId, container) => {
        const listContainer = container.querySelector(`#${fieldId}Container`);
        if (!listContainer) return {};

        const result = {};
        const intCards = listContainer.querySelectorAll('.integrante-card');
        
        intCards.forEach(intCard => {
          const name = intCard.querySelector('.integrante-name').value.trim();
          if (!name) return; // Omitir si no tiene nombre el integrante

          const tasks = [];
          const taskCards = intCard.querySelectorAll('.task-card');
          taskCards.forEach(taskCard => {
            const titulo = taskCard.querySelector('.task-title').value.trim();
            const fechaLimite = taskCard.querySelector('.task-date').value.trim();
            const descripcion = taskCard.querySelector('.task-desc').value.trim();

            if (titulo || descripcion) {
              tasks.push({ titulo, fechaLimite, descripcion });
            }
          });

          result[name] = tasks;
        });

        return result;
      }
    };
  };

  this.getCustomFieldPopulators = function () {
    return {
      'custom-tasks': (fieldId, data, container) => {
        const listContainer = container.querySelector(`#${fieldId}Container`);
        if (!listContainer || !listContainer._buildIntegranteCard) return;

        Utils.clearElement(listContainer);

        if (data && typeof data === 'object') {
          Object.entries(data).forEach(([integranteName, tasks]) => {
            const intCard = listContainer._buildIntegranteCard(integranteName);
            if (Array.isArray(tasks) && intCard._buildTaskCard) {
              tasks.forEach(task => {
                intCard._buildTaskCard(task);
              });
            }
          });
        }
      }
    };
  };

  /**
   * Parsea markdown de minuta a formData.
   * @param {string} markdown
   * @returns {object} FormData
   */
  this.parseMarkdownToFormData = function (markdown) {
    const lines = markdown.split(/\r?\n/);
    const sections = {};
    let current = '';

    // Parse secciones
    lines.forEach((line) => {
      const normalized = Utils.normalizeText(line.trim());
      if (normalized.startsWith('##')) {
        const header = Utils.normalizeText(line.replace(/^##\s*/, '').trim());
        current = header;
        sections[header] = [];
      } else if (current && line.trim()) {
        sections[current].push(line);
      }
    });

    // Parse metadatos
    const titleLineIndex = lines.findIndex(l => Utils.normalizeText(l.trim()).startsWith('# '));
    const title = titleLineIndex !== -1 ? lines[titleLineIndex].replace(/^#\s*/, '').trim() : '';

    // Información General
    let fecha = '';
    let horaInicio = '';
    let horaFin = '';

    const infoGeneral = sections['informacion general'] || [];
    infoGeneral.forEach(line => {
      const normalized = Utils.normalizeText(line);
      if (normalized.includes('fecha')) {
        const match = line.match(/(\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{4})/);
        if (match) {
          let parsedDate = match[1];
          if (parsedDate.includes('/')) {
            const [d, m, y] = parsedDate.split('/');
            parsedDate = `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
          }
          fecha = parsedDate;
        }
      }
      if (normalized.includes('bloque horario')) {
        const match = line.match(/(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})/);
        if (match) {
          horaInicio = match[1];
          horaFin = match[2];
        }
      }
    });

    // Extraer participantes (es una línea CSV en Información General)
    let participantes = [];
    infoGeneral.forEach(line => {
      const normalized = Utils.normalizeText(line);
      if (normalized.includes('participantes:')) {
        const parts = line.split(/participantes/i);
        if (parts.length > 1) {
          const partsStr = parts[1].replace(/^[:\*\s]+/, '').trim();
          if (partsStr) {
            participantes = partsStr.split(',').map(p => p.trim()).filter(Boolean);
          }
        }
      }
    });

    // Temas, Resultados
    const temas = this._parseList(sections['temas'] || sections['temas / dificultades abordadas'] || []);
    const resultados = this._parseList(sections['resultados'] || []);

    // Tareas (estructura anidada por integrante)
    const tareas = {};
    const tareasSection = sections['tareas asignadas'] || [];
    let currentIntegrante = '';
    let isParsingTaskDesc = false;

    tareasSection.forEach(line => {
      const normalized = Utils.normalizeText(line.trim());

      // Detectar integrante (H4: ####)
      if (line.trim().startsWith('####')) {
        currentIntegrante = line.replace(/^####\s*/, '').replace(/:$/, '').trim();
        tareas[currentIntegrante] = [];
        isParsingTaskDesc = false;
        return;
      }

      // Detectar tarea (H6: ###### o estructura de tarea)
      if (currentIntegrante && line.trim().startsWith('######')) {
        const taskTitle = line.replace(/^######\s*/, '').replace(/:$/, '').trim();
        tareas[currentIntegrante].push({
          titulo: taskTitle,
          fechaLimite: '',
          descripcion: ''
        });
        isParsingTaskDesc = false;
        return;
      }

      // Detectar fecha límite y descripción
      if (currentIntegrante && tareas[currentIntegrante].length > 0) {
        const lastTask = tareas[currentIntegrante][tareas[currentIntegrante].length - 1];

        if (normalized.includes('fecha limite')) {
          const match = line.match(/(\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{4})/);
          if (match) {
            let parsedDate = match[1];
            if (parsedDate.includes('/')) {
              const [d, m, y] = parsedDate.split('/');
              parsedDate = `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
            }
            lastTask.fechaLimite = parsedDate;
            return;
          }
        }

        if (normalized.startsWith('descripcion:')) {
          lastTask.descripcion = line.replace(/^descripci[oó]n:\s*/i, '').trim();
          isParsingTaskDesc = true;
          return;
        }

        if (isParsingTaskDesc) {
          if (lastTask.descripcion) {
            lastTask.descripcion += '\n' + line;
          } else {
            lastTask.descripcion = line;
          }
        }
      }
    });

    return {
      fecha,
      horaInicio,
      horaFin,
      participantes,
      temas,
      resultados,
      tareas
    };
  };

  /**
   * Helper: parsea lista de items de markdown (bullets).
   * @param {array} lines - Líneas de la sección
   * @returns {array} Items de lista
   */
  this._parseList = function (lines) {
    return lines
      .map(line => line.trim())
      .filter(line => line.startsWith('*') || line.startsWith('-'))
      .map(line => line.replace(/^[\*\-]\s*/, '').trim())
      .filter(Boolean);
  };

  /**
   * Genera markdown de minuta desde formData.
   * @param {object} formData
   * @returns {string} Markdown
   */
  this.buildMarkdownFromFormData = function (formData) {
    const fecha = (formData.fecha || '').trim();
    const horaInicio = (formData.horaInicio || '').trim();
    const horaFin = (formData.horaFin || '').trim();

    let markdown = '# Minuta de Reunión\n\n';

    // Información General
    markdown += '## Información General\n';
    markdown += `- **Fecha:** ${fecha || 'dd/mm/yyyy'}\n`;
    if (horaInicio && horaFin) {
      markdown += `- **Bloque Horario:** ${horaInicio} hs - ${horaFin} hs\n`;
    }

    const participantes = Array.isArray(formData.participantes)
      ? formData.participantes.filter(p => p && p.trim())
      : [];
    if (participantes.length > 0) {
      participantes.sort((a, b) => a.localeCompare(b));
      markdown += `- **Participantes:** ${participantes.join(', ')}\n`;
    }

    // Temas
    const temas = Array.isArray(formData.temas)
      ? formData.temas.filter(t => t && t.trim())
      : [];
    if (temas.length > 0) {
      markdown += `\n## Temas / Dificultades Abordadas\n`;
      markdown += temas.map(t => `- ${t}`).join('\n') + '\n';
    }

    // Resultados
    const resultados = Array.isArray(formData.resultados)
      ? formData.resultados.filter(r => r && r.trim())
      : [];
    if (resultados.length > 0) {
      markdown += `\n## Resultados\n`;
      markdown += resultados.map(r => `- ${r}`).join('\n') + '\n';
    }

    // Tareas Asignadas
    const tareas = formData.tareas || {};
    if (Object.keys(tareas).length > 0) {
      markdown += `\n## Tareas Asignadas\n`;

      const sortedIntegrantes = Object.keys(tareas).sort((a, b) => a.localeCompare(b));
      sortedIntegrantes.forEach(integrante => {
        markdown += `\n#### ${integrante}:\n`;
        const tasksList = tareas[integrante];

        if (Array.isArray(tasksList) && tasksList.length > 0) {
          tasksList.forEach((task, idx) => {
            if (task && task.descripcion && task.descripcion.trim()) {
              markdown += `\n###### ${task.titulo || `Tarea ${idx + 1}`}:\n`;
              if (task.fechaLimite) {
                markdown += `Fecha Límite: ${task.fechaLimite}\n`;
              }
              markdown += `Descripción: ${task.descripcion}\n`;
            }
          });
        } else {
          markdown += `Sin tareas asignadas.\n`;
        }
      });
    }

    return Utils.normalizeMarkdown(markdown);
  };

  /**
   * Retorna patrón de nombre de archivo.
   * @returns {string}
   */
  this.getFilenamePattern = function () {
    return 'yyyymmdd-minuta.md';
  };
}

// Export
window.MinutaTemplate = MinutaTemplate;
