/**
 * Template Registry Module
 * 
 * Patrón Registry: carga dinámicamente plantillas desde templates.json.
 * Proporciona un punto centralizado para acceder a todas las plantillas.
 * 
 * Principios SOLID aplicados:
 * - Open/Closed: Agregar plantilla = solo agregar en templates.json + nuevo archivo
 * - Dependency Inversion: Generator depende de este registro, no de templates específicas
 */

const TemplateRegistry = (() => {
  // Private state
  let templates = new Map(); // id → template instance
  let config = null;
  let isInitialized = false;

  /**
   * Carga el archivo de configuración templates.json
   * @returns {Promise<object>} Configuración cargada
   * @throws {Error} Si falla la carga
   */
  const loadConfig = async () => {
    // Si estamos ejecutando desde file://, no hacemos fetch para evitar el error de CORS
    if (window.location.protocol === 'file:') {
      console.log('📂 Entorno local (file://) detectado. Usando configuración interna de plantillas...');
      return getFallbackConfig();
    }

    try {
      const response = await fetch('./templates.json');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status} loading templates.json`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.warn('⚠️ No se pudo cargar templates.json por red. Usando configuración de respaldo...');
      return getFallbackConfig();
    }
  };

  const getFallbackConfig = () => {
    return {
      "templates": [
        {
          "id": "adr",
          "name": "Architecture Decision Record",
          "file": "./templates/adr.js",
          "className": "ADRTemplate",
          "description": "Registro de decisión arquitectónica con alternativas y justificación"
        },
        {
          "id": "minuta",
          "name": "Minuta de Reunión",
          "file": "./templates/minuta.js",
          "className": "MinutaTemplate",
          "description": "Registro de reunión con participantes, temas y tareas asignadas"
        }
      ],
      "defaultTemplate": "adr"
    };
  };

  /**
   * Carga dinámicamente un módulo de plantilla.
   * Espera que el módulo exporte una función constructora o factory.
   * 
   * @param {string} scriptPath - Ruta del archivo JS de la plantilla
   * @param {string} className - Nombre de la clase/función en el módulo
   * @returns {Promise<object>} Instancia de la plantilla
   */
  const loadTemplate = async (scriptPath, className) => {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = scriptPath;
      script.onload = () => {
        // El módulo debe existir en el global scope después de cargar el script
        if (window[className]) {
          const TemplateClass = window[className];
          const instance = new TemplateClass();
          resolve(instance);
        } else {
          reject(new Error(`Template class "${className}" not found after loading ${scriptPath}`));
        }
      };
      script.onerror = () => {
        reject(new Error(`Failed to load template script: ${scriptPath}`));
      };
      document.head.appendChild(script);
    });
  };

  /**
   * Inicializa el registry: carga templates.json y todas las plantillas listadas.
   * Debe llamarse al inicio de la aplicación.
   * 
   * @returns {Promise<boolean>} True si éxito, false si error
   */
  const initialize = async () => {
    try {
      config = await loadConfig();
      
      if (!config.templates || !Array.isArray(config.templates)) {
        throw new Error('Invalid templates.json structure: missing "templates" array');
      }

      // Cargar todas las plantillas en paralelo
      const templatePromises = config.templates.map(async (spec) => {
        try {
          const instance = await loadTemplate(spec.file, spec.className);
          templates.set(spec.id, instance);
          console.log(`✓ Template loaded: ${spec.id}`);
        } catch (error) {
          console.error(`✗ Error loading template ${spec.id}:`, error);
          throw error;
        }
      });

      await Promise.all(templatePromises);
      isInitialized = true;
      console.log(`✓ Registry initialized with ${templates.size} templates`);
      return true;
    } catch (error) {
      console.error('Error initializing TemplateRegistry:', error);
      isInitialized = false;
      return false;
    }
  };

  /**
   * Obtiene una plantilla por ID.
   * @param {string} id - ID de la plantilla (ej: "adr", "minuta")
   * @returns {object|null} Instancia de la plantilla o null si no existe
   */
  const get = (id) => {
    if (!isInitialized) {
      console.warn('TemplateRegistry not initialized yet');
      return null;
    }
    return templates.get(id) || null;
  };

  /**
   * Obtiene todas las plantillas registradas.
   * @returns {array} Array de [ {id, name, instance}, ... ]
   */
  const getAll = () => {
    if (!isInitialized) return [];
    
    const result = [];
    templates.forEach((instance, id) => {
      result.push({
        id,
        name: instance.name || id,
        instance
      });
    });
    return result;
  };

  /**
   * Obtiene metadatos de todas las plantillas desde config.
   * Útil para popullar UI sin necesitar la instancia.
   * @returns {array} Array de specs de templates desde templates.json
   */
  const getAllSpecs = () => {
    return config?.templates || [];
  };

  /**
   * Obtiene la plantilla por defecto según config.
   * @returns {object|null} Instancia de la plantilla por defecto
   */
  const getDefault = () => {
    if (!isInitialized || !config) return null;
    const defaultId = config.defaultTemplate;
    return get(defaultId);
  };

  /**
   * Obtiene el ID de la plantilla por defecto.
   * @returns {string|null} ID de la plantilla por defecto
   */
  const getDefaultId = () => {
    return config?.defaultTemplate || null;
  };

  /**
   * Verifica si una plantilla existe.
   * @param {string} id - ID a verificar
   * @returns {boolean} True si existe
   */
  const exists = (id) => {
    return templates.has(id);
  };

  /**
   * Devuelve estado de inicialización.
   * @returns {boolean} True si ya fue inicializado
   */
  const isReady = () => {
    return isInitialized;
  };

  return {
    initialize,
    get,
    getAll,
    getAllSpecs,
    getDefault,
    getDefaultId,
    exists,
    isReady
  };
})();
