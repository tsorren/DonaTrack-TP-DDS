/**
 * UI Engine Module
 * 
 * Motor de renderizado dinámico. Transforma especificaciones de formulario
 * (FieldSpec) en HTML, y recolecta datos en formato normalizado.
 * 
 * Principios SOLID:
 * - Single Responsibility: Solo renderizado y recolección de datos
 * - Open/Closed: Agregar field type = solo extender render logic
 */

const UIEngine = (() => {
  let currentContainer = null;
  let currentFormSpec = null;
  let currentTemplate = null;
  let customRenderers = {}; // Para campos especiales por template
  let customCollectors = {}; // Para recolección de datos de campos especiales
  let customPopulators = {}; // Para poblar campos especiales

  /**
   * Inicializa el engine con una plantilla.
   * @param {HTMLElement} container - Contenedor donde renderizar
   * @param {object} template - Instancia de plantilla con getFormSpec()
   */
  const initialize = (container, template) => {
    currentContainer = container;
    currentTemplate = template;
    customRenderers = {};
    customCollectors = {};
    customPopulators = {};

    if (container && template) {
      currentFormSpec = template.getFormSpec();
      // Si la plantilla tiene custom renderers, registrarse
      if (typeof template.getCustomFieldRenderers === 'function') {
        customRenderers = template.getCustomFieldRenderers();
      }
      if (typeof template.getCustomFieldCollectors === 'function') {
        customCollectors = template.getCustomFieldCollectors();
      }
      if (typeof template.getCustomFieldPopulators === 'function') {
        customPopulators = template.getCustomFieldPopulators();
      }
    }
  };

  /**
   * Renderiza un campo de tipo "text".
   * @param {object} fieldSpec - Especificación del campo
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderTextField = (fieldSpec) => {
    const group = document.createElement('div');
    group.className = 'input-group';

    const label = document.createElement('label');
    label.htmlFor = fieldSpec.id;
    label.textContent = fieldSpec.label;
    if (fieldSpec.required) {
      label.textContent += ' *';
    }

    const input = document.createElement('input');
    input.type = fieldSpec.inputType || 'text';
    input.id = fieldSpec.id;
    input.className = fieldSpec.className || '';
    input.placeholder = fieldSpec.placeholder || '';
    input.required = fieldSpec.required || false;
    if (fieldSpec.dataAttribute) {
      input.setAttribute('data-field', fieldSpec.dataAttribute);
    }

    group.appendChild(label);
    group.appendChild(input);

    if (fieldSpec.hint) {
      const hint = document.createElement('p');
      hint.className = 'hint';
      hint.textContent = fieldSpec.hint;
      group.appendChild(hint);
    }

    return group;
  };

  /**
   * Renderiza un campo de tipo "textarea".
   * @param {object} fieldSpec - Especificación del campo
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderTextareaField = (fieldSpec) => {
    const group = document.createElement('div');
    group.className = 'input-group';

    const label = document.createElement('label');
    label.htmlFor = fieldSpec.id;
    label.textContent = fieldSpec.label;
    if (fieldSpec.required) {
      label.textContent += ' *';
    }

    const textarea = document.createElement('textarea');
    textarea.id = fieldSpec.id;
    textarea.className = fieldSpec.className || '';
    textarea.placeholder = fieldSpec.placeholder || '';
    textarea.rows = fieldSpec.rows || 4;
    textarea.required = fieldSpec.required || false;
    if (fieldSpec.dataAttribute) {
      textarea.setAttribute('data-field', fieldSpec.dataAttribute);
    }

    group.appendChild(label);
    group.appendChild(textarea);

    if (fieldSpec.hint) {
      const hint = document.createElement('p');
      hint.className = 'hint';
      hint.textContent = fieldSpec.hint;
      group.appendChild(hint);
    }

    return group;
  };

  /**
   * Renderiza un campo de tipo "select".
   * @param {object} fieldSpec - Especificación del campo
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderSelectField = (fieldSpec) => {
    const group = document.createElement('div');
    group.className = 'input-group';

    const label = document.createElement('label');
    label.htmlFor = fieldSpec.id;
    label.textContent = fieldSpec.label;
    if (fieldSpec.required) {
      label.textContent += ' *';
    }

    const select = document.createElement('select');
    select.id = fieldSpec.id;
    select.className = fieldSpec.className || '';
    select.required = fieldSpec.required || false;
    if (fieldSpec.dataAttribute) {
      select.setAttribute('data-field', fieldSpec.dataAttribute);
    }

    if (fieldSpec.options && Array.isArray(fieldSpec.options)) {
      fieldSpec.options.forEach(opt => {
        const option = document.createElement('option');
        option.value = opt.value;
        option.textContent = opt.label;
        select.appendChild(option);
      });
    }

    group.appendChild(label);
    group.appendChild(select);

    return group;
  };

  /**
   * Renderiza un campo de tipo "list" (repetible, 1-D).
   * Ej: drivers, links, participantes.
   * @param {object} fieldSpec - Especificación del campo
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderListField = (fieldSpec) => {
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
    btnAdd.id = `btn${fieldSpec.id.charAt(0).toUpperCase() + fieldSpec.id.slice(1)}Add`;
    btnAdd.addEventListener('click', (e) => {
      e.preventDefault();
      addListItem(fieldSpec.id, fieldSpec.itemFieldType || 'text');
    });

    header.appendChild(info);
    header.appendChild(btnAdd);

    const list = document.createElement('div');
    list.className = 'list';
    list.id = `${fieldSpec.id}Container`;
    list.setAttribute('data-field-list', fieldSpec.id);

    card.appendChild(header);
    card.appendChild(list);

    return card;
  };

  /**
   * Agrega un item a una lista.
   * @param {string} listId - ID de la lista
   * @param {string} itemType - Tipo de campo del item (text, textarea)
   */
  const addListItem = (listId, itemType = 'text') => {
    const container = document.getElementById(`${listId}Container`);
    if (!container) return;

    const item = document.createElement('div');
    item.className = 'list-item';

    const input = document.createElement(itemType === 'textarea' ? 'textarea' : 'input');
    input.type = itemType === 'textarea' ? 'textarea' : 'text';
    input.className = `field-${listId}`;
    input.setAttribute('data-list-item', listId);
    if (itemType === 'textarea') {
      input.rows = 2;
    }

    const btnRemove = document.createElement('button');
    btnRemove.type = 'button';
    btnRemove.className = 'btn btn-ghost';
    btnRemove.textContent = 'Quitar';
    btnRemove.addEventListener('click', () => item.remove());

    item.appendChild(input);
    item.appendChild(btnRemove);
    container.appendChild(item);
  };

  /**
   * Renderiza una sección (agrupa múltiples campos).
   * @param {object} sectionSpec - Especificación de sección
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderSection = (sectionSpec) => {
    const section = document.createElement('section');
    section.className = 'card';

    if (sectionSpec.title) {
      const heading = document.createElement('h2');
      heading.textContent = sectionSpec.title;
      section.appendChild(heading);
    }

    if (sectionSpec.fields && Array.isArray(sectionSpec.fields)) {
      // Renderizar fields dentro de la sección
      // Si hay múltiples columns, usar grid
      if (sectionSpec.fields.length > 1 && sectionSpec.columns) {
        const wrapper = document.createElement('div');
        wrapper.className = `grid grid-${sectionSpec.columns}`;
        sectionSpec.fields.forEach(field => {
          wrapper.appendChild(renderField(field));
        });
        section.appendChild(wrapper);
      } else {
        sectionSpec.fields.forEach(field => {
          section.appendChild(renderField(field));
        });
      }
    }

    return section;
  };

  /**
   * Renderiza un campo genérico según su tipo.
   * Punto central de dispatch. Primero chequea renderers personalizados.
   * @param {object} fieldSpec - Especificación del campo
   * @returns {HTMLElement} Elemento renderizado
   */
  const renderField = (fieldSpec) => {
    const type = fieldSpec.type || 'text';

    // Chequear si hay renderer personalizado
    if (customRenderers[type]) {
      return customRenderers[type](fieldSpec);
    }

    switch (type) {
      case 'text':
        return renderTextField(fieldSpec);
      case 'textarea':
        return renderTextareaField(fieldSpec);
      case 'select':
        return renderSelectField(fieldSpec);
      case 'list':
        return renderListField(fieldSpec);
      case 'section':
        return renderSection(fieldSpec);
      default:
        console.warn(`Unknown field type: ${type}`);
        return renderTextField(fieldSpec);
    }
  };

  /**
   * Limpia el contenedor actual.
   */
  const clear = () => {
    if (currentContainer) {
      Utils.clearElement(currentContainer);
    }
  };

  /**
   * Renderiza el formulario completo según currentFormSpec.
   * @param {HTMLElement} container - Contenedor destino (opcional, usa el registrado)
   */
  const render = (container = null) => {
    const target = container || currentContainer;
    if (!target) {
      console.error('UIEngine: No container specified');
      return;
    }

    if (!currentFormSpec) {
      console.error('UIEngine: No form spec available');
      return;
    }

    clear();

    currentFormSpec.forEach(spec => {
      const element = renderField(spec);
      target.appendChild(element);
    });

    // Agregar sección de acciones al final
    const actionSection = document.createElement('div');
    actionSection.className = 'action-row';
    actionSection.id = 'actionSection';
    target.appendChild(actionSection);

    console.log('✓ Form rendered');
  };

  /**
   * Recolecta todos los datos del formulario en un objeto.
   * @returns {object} Datos del formulario { fieldId: value, ... }
   */
  const collectFormData = () => {
    const data = {};

    if (!currentContainer) {
      console.error('UIEngine: No container');
      return data;
    }

    // Procesar recolectores personalizados primero
    if (currentFormSpec) {
      const processSpecForCollection = (fields) => {
        fields.forEach(field => {
          if (customCollectors[field.type]) {
            data[field.id] = customCollectors[field.type](field.id, currentContainer);
          } else if (field.type === 'section' && field.fields) {
            processSpecForCollection(field.fields);
          }
        });
      };
      processSpecForCollection(currentFormSpec);
    }

    // Text inputs and textareas (ignorando los ya recolectados)
    currentContainer.querySelectorAll('input[type="text"], textarea, select').forEach(el => {
      if (el.id && !el.id.startsWith('btn') && data[el.id] === undefined) {
        data[el.id] = el.value;
      }
    });

    // Lists (1-D): agrupa items por listId
    currentContainer.querySelectorAll('[data-list-item]').forEach(el => {
      const listId = el.getAttribute('data-list-item');
      if (data[listId] === undefined || Array.isArray(data[listId])) {
        if (!data[listId]) {
          data[listId] = [];
        }
        data[listId].push(el.value);
      }
    });

    return data;
  };

  /**
   * Rellena el formulario con datos existentes.
   * @param {object} data - Datos a completar { fieldId: value, ... }
   */
  const populateFormData = (data) => {
    if (!currentContainer || !data) return;

    Object.entries(data).forEach(([key, value]) => {
      // Buscar el tipo de campo en la especificación
      let fieldSpec = null;
      if (currentFormSpec) {
        const findFieldSpec = (fields, targetId) => {
          for (const field of fields) {
            if (field.id === targetId) return field;
            if (field.type === 'section' && field.fields) {
              const spec = findFieldSpec(field.fields, targetId);
              if (spec) return spec;
            }
          }
          return null;
        };
        fieldSpec = findFieldSpec(currentFormSpec, key);
      }

      const fieldType = fieldSpec ? fieldSpec.type : null;

      if (fieldType && customPopulators[fieldType]) {
        customPopulators[fieldType](key, value, currentContainer);
      } else if (Array.isArray(value)) {
        // Es una lista genérica (1-D): agregar items
        const container = document.getElementById(`${key}Container`);
        if (container) {
          Utils.clearElement(container);
          const itemType = fieldSpec ? (fieldSpec.itemFieldType || 'text') : 'text';
          value.forEach(item => {
            addListItem(key, itemType);
            const inputs = container.querySelectorAll(`[data-list-item="${key}"]`);
            if (inputs.length > 0) {
              inputs[inputs.length - 1].value = item;
            }
          });
        }
      } else {
        // Es un campo singular
        const el = document.getElementById(key);
        if (el) {
          el.value = value;
          // Disparar eventos por si hay lógica atada a ellos
          el.dispatchEvent(new Event('input', { bubbles: true }));
          el.dispatchEvent(new Event('change', { bubbles: true }));
        }
      }
    });
  };

  /**
   * Obtiene el engine para acceso avanzado (si necesario).
   * @returns {object} Estado actual del engine
   */
  const getState = () => ({
    container: currentContainer,
    template: currentTemplate,
    formSpec: currentFormSpec
  });

  return {
    initialize,
    render,
    clear,
    collectFormData,
    populateFormData,
    addListItem,
    getState
  };
})();
