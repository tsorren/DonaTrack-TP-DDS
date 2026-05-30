/**
 * Core Utilities Module
 * 
 * Funciones genéricas reutilizables por todas las plantillas.
 * Principio: Zero template-specific logic.
 */

const Utils = (() => {
  /**
   * Normaliza texto removiendo acentos y convirtiendo a lowercase.
   * @param {string} value - Texto a normalizar
   * @returns {string} Texto normalizado
   */
  const normalizeText = (value) => value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

  /**
   * Extrae valor de elemento DOM, retorna placeholder si vacío.
   * @param {HTMLElement} el - Elemento DOM
   * @param {string} placeholder - Valor por defecto
   * @returns {string} Valor o placeholder
   */
  const getValue = (el, placeholder = '') => {
    const value = (el?.value || '').trim();
    return value || placeholder;
  };

  /**
   * Convierte string multilínea en array de líneas no vacías y trimmed.
   * @param {string} value - Texto multilinea
   * @returns {array} Array de líneas procesadas
   */
  const getLines = (value) => value
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);

  /**
   * Establece mensaje de estado en elemento.
   * @param {string} message - Mensaje a mostrar
   * @param {string} type - Tipo de mensaje: 'info', 'success', 'error'
   */
  const setStatus = (message, type = 'info') => {
    // Compatibilidad para Screen Readers en caso de existir formStatus
    const el = document.getElementById('formStatus');
    if (el) {
      el.textContent = message || '';
    }

    if (!message) return;

    // Crear e inyectar Toast System dinámicamente
    let toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.id = 'toastContainer';
      document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toastContainer.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));

    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300); // Esperar que termine CSS transition
    }, 3500);
  };

  /**
   * Descarga archivo con contenido dado.
   * @param {string} content - Contenido del archivo
   * @param {string} filename - Nombre del archivo a descargar
   * @param {string} mimeType - MIME type (default: 'text/plain')
   */
  const downloadFile = (content, filename, mimeType = 'text/plain') => {
    const blob = new Blob([content], { type: mimeType });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  /**
   * Copia texto al portapapeles usando Clipboard API o fallback.
   * @param {string} text - Texto a copiar
   * @returns {Promise<boolean>} Resuelve true si éxito, false si error
   */
  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (error) {
      // Fallback para navegadores sin Clipboard API
      try {
        const helper = document.createElement('textarea');
        helper.value = text;
        document.body.appendChild(helper);
        helper.select();
        const success = document.execCommand('copy');
        document.body.removeChild(helper);
        return success;
      } catch (err) {
        console.error('Error copying to clipboard:', err);
        return false;
      }
    }
  };

  /**
   * Genera nombre de archivo con formato: yyyymmdd-slug.ext
   * @param {string} title - Título del documento
   * @param {string} extension - Extensión (default: 'md')
   * @returns {string} Nombre de archivo formateado
   */
  const generateFilename = (title, extension = 'md') => {
    const date = new Date().toISOString().split('T')[0];
    const dateCompact = date.replace(/-/g, '');
    const slug = title
      .toLowerCase()
      .trim()
      .replace(/[^\w\s-]/gu, '')
      .replace(/[\s_-]+/g, '-')
      .replace(/^-+|-+$/g, '');
    return `${dateCompact}-${slug}.${extension}`;
  };

  /**
   * Normaliza saltos de línea en markdown (elimina excesos).
   * @param {string} markdown - Contenido markdown
   * @returns {string} Markdown normalizado
   */
  const normalizeMarkdown = (markdown) => {
    return markdown
      .replace(/\n{3,}/g, '\n\n')
      .trim() + '\n';
  };

  /**
   * Obtiene elemento por ID con manejo de error.
   * @param {string} id - ID del elemento
   * @returns {HTMLElement|null} Elemento o null
   */
  const getElementSafe = (id) => {
    try {
      return document.getElementById(id);
    } catch (e) {
      console.warn(`Element with id "${id}" not found`);
      return null;
    }
  };

  /**
   * Verifica si elemento existe en DOM.
   * @param {string} id - ID del elemento
   * @returns {boolean} True si existe
   */
  const elementExists = (id) => {
    return document.getElementById(id) !== null;
  };

  /**
   * Recupera valor de localStorage con fallback.
   * @param {string} key - Clave
   * @param {any} defaultValue - Valor por defecto
   * @returns {any} Valor guardado o default
   */
  const getLocalStorage = (key, defaultValue = null) => {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : defaultValue;
    } catch (e) {
      console.warn(`Error reading localStorage key "${key}"`);
      return defaultValue;
    }
  };

  /**
   * Guarda valor en localStorage con error handling.
   * @param {string} key - Clave
   * @param {any} value - Valor a guardar
   * @returns {boolean} True si éxito
   */
  const setLocalStorage = (key, value) => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (e) {
      console.warn(`Error writing localStorage key "${key}"`);
      return false;
    }
  };

  /**
   * Limpia todos los hijos de un elemento.
   * @param {HTMLElement} element - Elemento a limpiar
   */
  const clearElement = (element) => {
    if (element) {
      element.innerHTML = '';
    }
  };

  return {
    normalizeText,
    getValue,
    getLines,
    setStatus,
    downloadFile,
    copyToClipboard,
    generateFilename,
    normalizeMarkdown,
    getElementSafe,
    elementExists,
    getLocalStorage,
    setLocalStorage,
    clearElement
  };
})();
