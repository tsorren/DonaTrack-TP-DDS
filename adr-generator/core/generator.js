/**
 * Generator Service Module
 * 
 * Orquestador central de operaciones. Coordina:
 * - Manejo de modos (create/edit)
 * - Manejo de tabs (write/preview)
 * - Operaciones: copiar, descargar, parsear, generar
 * - Integración entre UIEngine y Template
 * 
 * Principio: Orchestration pattern. No acoplado a plantilla específica.
 */

const GeneratorService = (() => {
  // Private state
  let currentTemplate = null;
  let currentMode = 'create'; // 'create' | 'edit'
  let currentTab = 'write';   // 'write' | 'preview'
  let editorPanel = null;
  let formContainer = null;
  let markdownInput = null;

  /**
   * Inicializa el servicio con referencias a elementos DOM y plantilla.
   * @param {object} template - Instancia de plantilla (debe tener métodos requeridos)
   * @param {HTMLElement} formContainerEl - Elemento contenedor del formulario
   * @param {HTMLElement} editorPanelEl - Elemento panel editor
   * @param {HTMLElement} markdownInputEl - Elemento textarea para markdown
   * @returns {boolean} True si éxito
   */
  const initialize = (template, formContainerEl, editorPanelEl, markdownInputEl) => {
    if (!template) {
      console.error('GeneratorService: No template provided');
      return false;
    }

    // Validar que template tenga interfaz requerida
    const requiredMethods = [
      'getFormSpec',
      'buildMarkdownFromFormData',
      'parseMarkdownToFormData',
      'validateFormData',
      'getFilenamePattern'
    ];

    for (const method of requiredMethods) {
      if (typeof template[method] !== 'function') {
        console.error(`GeneratorService: Template missing method "${method}"`);
        return false;
      }
    }

    currentTemplate = template;
    formContainer = formContainerEl;
    editorPanel = editorPanelEl;
    markdownInput = markdownInputEl;

    return true;
  };

  /**
   * Renderiza el formulario para la plantilla actual.
   */
  const renderForm = () => {
    if (!currentTemplate || !formContainer) {
      console.error('GeneratorService: Not initialized');
      return;
    }

    UIEngine.initialize(formContainer, currentTemplate);
    UIEngine.render();

    // Agregar botones de acción
    const actionSection = document.getElementById('actionSection');
    if (actionSection) {
      Utils.clearElement(actionSection);

      const btnCopy = document.createElement('button');
      btnCopy.type = 'button';
      btnCopy.className = 'btn btn-outline';
      btnCopy.textContent = 'Copiar a portapapeles';
      btnCopy.id = 'btnCopy';
      btnCopy.addEventListener('click', handleCopy);

      const btnDownload = document.createElement('button');
      btnDownload.type = 'button';
      btnDownload.className = 'btn btn-primary';
      btnDownload.textContent = `Descargar ${currentTemplate.name || 'Documento'} (.md)`;
      btnDownload.id = 'btnGenerate';
      btnDownload.addEventListener('click', handleDownload);

      actionSection.appendChild(btnCopy);
      actionSection.appendChild(btnDownload);
    }

    // Agregar status element si no existe
    if (!document.getElementById('formStatus')) {
      const status = document.createElement('div');
      status.id = 'formStatus';
      status.className = 'form-status';
      status.setAttribute('aria-live', 'polite');
      if (formContainer.parentElement) {
        formContainer.parentElement.appendChild(status);
      }
    }
  };

  /**
   * Cambia el modo actual (create ↔ edit).
   * @param {string} newMode - 'create' o 'edit'
   */
  const setMode = (newMode) => {
    if (!['create', 'edit'].includes(newMode)) {
      console.warn(`Invalid mode: ${newMode}`);
      return;
    }

    currentMode = newMode;
    const isEdit = newMode === 'edit';

    // Actualizar UI buttons
    const btnModeCreate = document.getElementById('btnModeCreate');
    const btnModeEdit = document.getElementById('btnModeEdit');
    if (btnModeCreate) {
      btnModeCreate.classList.toggle('is-active', !isEdit);
    }
    if (btnModeEdit) {
      btnModeEdit.classList.toggle('is-active', isEdit);
    }

    // Actualizar titulo modo
    const modeTitle = document.getElementById('modeTitle');
    if (modeTitle) {
      modeTitle.textContent = isEdit ? 'Modo editar' : 'Modo crear';
    }

    // Toggle editor panel
    if (editorPanel) {
      editorPanel.classList.toggle('is-visible', isEdit);
      editorPanel.setAttribute('aria-hidden', (!isEdit).toString());
    }

    // Toggle form
    if (formContainer) {
      formContainer.classList.toggle('is-hidden', isEdit);
    }

    // Si entramos en edit, limpiar markdown input
    if (isEdit && markdownInput) {
      markdownInput.value = '';
      setTab('write');
    }
  };

  /**
   * Cambia el tab actual (write ↔ preview) en modo edit.
   * @param {string} newTab - 'write' o 'preview'
   */
  const setTab = (newTab) => {
    if (!['write', 'preview'].includes(newTab)) {
      console.warn(`Invalid tab: ${newTab}`);
      return;
    }

    currentTab = newTab;
    const isPreview = newTab === 'preview';

    // Actualizar UI buttons
    const btnWriteTab = document.getElementById('btnWriteTab');
    const btnPreviewTab = document.getElementById('btnPreviewTab');
    if (btnWriteTab) {
      btnWriteTab.classList.toggle('is-active', !isPreview);
    }
    if (btnPreviewTab) {
      btnPreviewTab.classList.toggle('is-active', isPreview);
    }

    // Toggle editor panel preview mode
    if (editorPanel) {
      editorPanel.classList.toggle('is-preview', isPreview);
    }

    // Si preview, parsear markdown
    if (isPreview && markdownInput) {
      const markdown = markdownInput.value;
      if (markdown.trim()) {
        parseMarkdown(markdown);
      } else {
        Utils.setStatus('Ingresa markdown en la pestaña "Escribir"');
      }
    }
  };

  /**
   * Parsea markdown y carga datos en el formulario.
   * @param {string} markdown - Contenido markdown a parsear
   * @returns {boolean} True si éxito
   */
  const parseMarkdown = (markdown) => {
    if (!currentTemplate) {
      console.error('No template set');
      return false;
    }

    try {
      const formData = currentTemplate.parseMarkdownToFormData(markdown);
      if (!formData) {
        Utils.setStatus('Error al parsear markdown');
        return false;
      }

      // Cambiar a create mode y renderizar
      setMode('create');
      renderForm();
      UIEngine.populateFormData(formData);
      Utils.setStatus('Markdown cargado correctamente');
      return true;
    } catch (error) {
      console.error('Parse error:', error);
      Utils.setStatus(`Error: ${error.message}`);
      return false;
    }
  };

  /**
   * Recolecta datos del formulario y genera markdown.
   * @returns {object|null} { markdown, filename } o null si error
   */
  const buildMarkdown = () => {
    if (!currentTemplate) {
      console.error('No template set');
      return null;
    }

    try {
      const formData = UIEngine.collectFormData();

      // Validar
      const validation = currentTemplate.validateFormData(formData);
      if (!validation.isValid) {
        Utils.setStatus(`Validación: ${validation.errors.join(', ')}`);
        return null;
      }

      // Generar markdown
      const markdown = currentTemplate.buildMarkdownFromFormData(formData);
      const filename = Utils.generateFilename(
        formData.title || 'documento',
        'md'
      );

      return { markdown, filename };
    } catch (error) {
      console.error('Build markdown error:', error);
      Utils.setStatus(`Error: ${error.message}`);
      return null;
    }
  };

  /**
   * Maneja descarga de archivo.
   */
  const handleDownload = () => {
    const result = buildMarkdown();
    if (!result) return;

    Utils.downloadFile(result.markdown, result.filename, 'text/markdown');
    Utils.setStatus(`Archivo descargado: ${result.filename}`);
  };

  /**
   * Maneja copia a portapapeles.
   */
  const handleCopy = async () => {
    const result = buildMarkdown();
    if (!result) return;

    const success = await Utils.copyToClipboard(result.markdown);
    if (success) {
      Utils.setStatus('Markdown copiado al portapapeles');
    } else {
      Utils.setStatus('Error al copiar (verifica permisos)');
    }
  };

  /**
   * Cambia la plantilla actual y reinicializa.
   * @param {object} template - Nueva plantilla
   */
  const setTemplate = (template) => {
    if (!initialize(template, formContainer, editorPanel, markdownInput)) {
      console.error('Failed to set template');
      return false;
    }

    // Establecer modo create por defecto
    setMode('create');

    // Renderizar nueva forma
    renderForm();

    // Guardar en localStorage
    Utils.setLocalStorage('lastTemplateId', template.id);

    return true;
  };

  /**
   * Obtiene estado actual.
   * @returns {object} Estado del servicio
   */
  const getState = () => ({
    template: currentTemplate?.id || null,
    mode: currentMode,
    tab: currentTab
  });

  return {
    initialize,
    renderForm,
    setMode,
    setTab,
    parseMarkdown,
    buildMarkdown,
    handleDownload,
    handleCopy,
    setTemplate,
    getState
  };
})();
