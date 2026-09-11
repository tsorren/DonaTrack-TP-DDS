/**
 * Entry Point - Main Orchestration Script
 * 
 * Responsabilidades:
 * 1. Cargar y inicializar TemplateRegistry
 * 2. Restaurar última plantilla de localStorage o usar default
 * 3. Inicializar GeneratorService
 * 4. Enganchar event listeners
 * 5. Renderizar formulario inicial
 */

(async function () {
    console.log('🚀 Initializing DonaTrack Document Generator...');

    // ============================================
    // 1. Esperar DOM ready
    // ============================================
    if (document.readyState === 'loading') {
        await new Promise(resolve => {
            document.addEventListener('DOMContentLoaded', resolve);
        });
    }

    // ============================================
    // 2. Obtener referencias DOM críticas
    // ============================================
    const formContainer = document.getElementById('formContainer');
    const editorPanel = document.getElementById('editorPanel');
    const markdownInput = document.getElementById('markdownInput');
    const templateSelect = document.getElementById('templateSelect');
    const btnModeCreate = document.getElementById('btnModeCreate');
    const btnModeEdit = document.getElementById('btnModeEdit');
    const btnWriteTab = document.getElementById('btnWriteTab');
    const btnPreviewTab = document.getElementById('btnPreviewTab');

    if (!formContainer || !editorPanel || !markdownInput || !templateSelect) {
        console.error('❌ Critical DOM elements missing');
        return;
    }

    // ============================================
    // 3. Inicializar TemplateRegistry
    // ============================================
    console.log('📦 Loading template registry...');
    const registryReady = await TemplateRegistry.initialize();

    if (!registryReady) {
        console.error('❌ Failed to initialize TemplateRegistry');
        Utils.setStatus('Error loading templates. Refresh the page.', 'error');
        return;
    }

    console.log('✓ Registry initialized');

    // ============================================
    // 4. Poblar selector de plantillas
    // ============================================
    console.log('📋 Populating template selector...');
    const specs = TemplateRegistry.getAllSpecs();
    specs.forEach(spec => {
        const option = document.createElement('option');
        option.value = spec.id;
        option.textContent = spec.name;
        templateSelect.appendChild(option);
    });

    // ============================================
    // 5. Restaurar última plantilla usada
    // ============================================
    const lastTemplateId = Utils.getLocalStorage('lastTemplateId');
    const defaultTemplateId = TemplateRegistry.getDefaultId();
    const initialTemplateId = lastTemplateId || defaultTemplateId;

    console.log(`🎯 Initial template: ${initialTemplateId}`);
    templateSelect.value = initialTemplateId;

    // ============================================
    // 6. Obtener plantilla inicial
    // ============================================
    let currentTemplate = TemplateRegistry.get(initialTemplateId);
    if (!currentTemplate) {
        console.error(`❌ Template not found: ${initialTemplateId}`);
        currentTemplate = TemplateRegistry.getDefault();
    }

    // ============================================
    // 7. Inicializar GeneratorService
    // ============================================
    console.log(`📄 Initializing generator for: ${currentTemplate.id}`);
    const success = GeneratorService.initialize(
        currentTemplate,
        formContainer,
        editorPanel,
        markdownInput
    );

    if (!success) {
        console.error('❌ Failed to initialize GeneratorService');
        return;
    }

    // Renderizar formulario inicial
    GeneratorService.renderForm();
    console.log('✓ Form rendered');

    // ============================================
    // 8. Enganchar Event Listeners - Modes
    // ============================================
    btnModeCreate.addEventListener('click', () => {
        GeneratorService.setMode('create');
    });

    btnModeEdit.addEventListener('click', () => {
        GeneratorService.setMode('edit');
    });

    // ============================================
    // 9. Enganchar Event Listeners - Tabs (Edit Mode)
    // ============================================
    btnWriteTab.addEventListener('click', () => {
        GeneratorService.setTab('write');
    });

    btnPreviewTab.addEventListener('click', () => {
        GeneratorService.setTab('preview');
    });

    // ============================================
    // 10. Enganchar Event Listener - Selector Plantilla
    // ============================================
    templateSelect.addEventListener('change', async (e) => {
        const templateId = e.target.value;
        console.log(`🔄 Switching to template: ${templateId}`);

        const template = TemplateRegistry.get(templateId);
        if (!template) {
            console.error(`Template not found: ${templateId}`);
            return;
        }

        // Cambiar plantilla en GeneratorService
        const setSuccess = GeneratorService.setTemplate(template);
        if (setSuccess) {
            console.log(`✓ Template switched to: ${templateId}`);
        } else {
            console.error(`Failed to switch template to: ${templateId}`);
        }
    });

    // ============================================
    // 11. Delegación de eventos para campos dinámicos
    // ============================================
    // Los eventos de click para botones dinámicos ya están attached en UIEngine.render()

    console.log('✅ Generator fully initialized and ready!');
})();
