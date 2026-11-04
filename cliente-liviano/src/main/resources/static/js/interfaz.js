// Los componentes de Alpine.js (D31): estado de interfaz que nunca sale del navegador. Con el build
// CSP, el HTML solo nombra propiedades y métodos (x-data="menu", @click="alternar"); la lógica va
// acá, en Alpine.data(). Se carga antes que Alpine (templates/interaccion.html) y registra los
// componentes cuando Alpine avisa con alpine:init.
// Prohibido acá: fetch o XMLHttpRequest, armar listas con datos del negocio, x-html y guardar
// tokens o datos personales en localStorage (cliente-liviano/AGENTS.md).
// Un componente nuevo se agrega con su entrada en docs/interfaz/componentes.md y en /estilos.
document.addEventListener("alpine:init", () => {
  // Alpine.data("nombre", () => ({ ... }));
});
