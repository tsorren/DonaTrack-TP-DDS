# Tema de Keycloak con la identidad Raíces: tokens copiados del sitio, sincronizados y sin modo oscuro

- Status: proposed
- Date: 2026-11-12
- Deciders: tsorren, al implementar E2-09 de SPEC-06; falta la aprobación del equipo en el PR
- Tags: auth-service, keycloak, tema, tokens, raices, identidad-visual, accesibilidad, entrega-6

> **Origen:** cierra E2-09 de [SPEC-06](../../specs/active/SPEC-06-auth-mvp-keycloak.md) y reemplaza el tema provisorio de A1-04. Sigue D20 de los [requerimientos de E6](../../requerimientos/requerimientos-e6.md): `tokens.css` es la fuente de verdad de los estilos.

## Contexto y Problema

El login de Keycloak tenía un tema provisorio (paleta y logo de relleno, "hasta que llegue `tokens.css` de la diseñadora") y el sitio ya tenía su identidad real: la piel Huerta, con `tokens.css`, tres tipografías y un logo. Las personas ven las dos cosas seguidas (el sitio y, al ingresar, Keycloak) y tenían que ser la misma.

Había tres problemas de diseño:

1. **D20 dice que `tokens.css` es la fuente de verdad, pero Keycloak corre en otra imagen** y no puede leer los archivos del sitio.
2. **El login de Keycloak cambia a modo oscuro según el sistema** y el sitio no tiene modo oscuro.
3. **El nombre del encabezado** es texto plano y la marca del sitio lleva "Track" en otro color.

## Alternativas Consideradas

| Alternativa | Resultado |
|---|---|
| **A. Copiar `tokens.css`, las fuentes y el logo al tema, con un script que los sincroniza y un test que falla si quedan viejos** | Elegida |
| B. Que Keycloak cargue los estilos desde el sitio por URL | Descartada: una caída o un cambio de dominio del sitio rompe el login; mezcla orígenes |
| C. Redefinir los colores en el CSS del tema | Descartada: una segunda fuente de verdad que se desalinea sola |
| D. Mantener el modo oscuro de Keycloak | Descartada: pasar de un sitio crema a una pantalla oscura |

## Resultado de la Decisión

**A.**

- El tema lleva una copia **idéntica** de `css/tokens.css`, de las tres tipografías con sus licencias OFL y del logo, en las mismas subcarpetas que el sitio (así los `@font-face` valen sin cambios). `python scripts/sincronizar_tema.py` copia y `--verificar` comprueba. `test_auth_theme.py` falla si la copia está vieja.
- `donatrack.css` usa **solo tokens de uso** (`--color-*`, `--fuente-*`, `--radio-*`, `--sombra-papel`), nunca los valores base `--p-*`, y no escribe colores a mano. Un test lo vigila.
- **Sin modo oscuro:** `darkMode=false`. Keycloak declara `color-scheme: light` y no agrega el script que cambia de modo. Se elimina el logo oscuro.
- **La marca:** `displayNameHtml: Dona<em>Track</em>` en el realm (el sanitizador de Keycloak deja pasar el `<em>`), con el ícono del sitio como `::before`. El nombre sigue siendo texto para los lectores de pantalla.
- Cubre **todas** las pantallas de Keycloak: ingreso, registro, recuperar la clave, modificar la contraseña, verificar el email, actualizar el perfil, cuenta desactivada, enlace inválido, cerrar sesión, página caducada y las del vínculo con Google, con sus errores.

## Consecuencias Positivas

- Una sola identidad: cambiar `tokens.css` en el sitio y volver a copiar actualiza el login.
- **Verificado en un navegador real** (Firefox 160 sobre Keycloak 26.8.1): 17 pantallas a 360 y 1280 px, colores y tipografías calculados por el navegador, sin desborde horizontal a 360 ni a 320 px.
- **Contraste WCAG AA en los 22 pares de colores** que usa el tema (el más justo, el texto del botón sobre el verde: 5,41:1, mínimo 4,5), calculados por un test a partir de `tokens.css`.

## Consecuencias Negativas

- **Hay que acordarse de volver a copiar** cuando el sitio cambia los tokens: si no, el test falla (es la intención, pero es un paso manual).
- `tokens.css` trae valores que el login no usa (el mapa, los frutos, el movimiento): pesa lo que pesa en el sitio (~11 KB) y las tres fuentes suman ~250 KB.
- El tema usa `:has()` para marcar los campos con error: necesita Chrome 105, Firefox 121 o Safari 15.4. Solo se probó en Firefox.
- **"La cuenta ya existe"** (vínculo con Google) sale como aviso rojo de error: es el tipo que Keycloak elige para esa pantalla y cambiarlo exige reemplazar su plantilla.
- La consola de cuenta y los correos **no usan este tema**: el sitio no enlaza a la consola y los correos de cuenta los manda `notificaciones-service`.
- La marca en dos colores y el tema vienen del realm, que se importa una sola vez: `docker compose down -v`.
- Firefox **cachea las hojas de Keycloak por URL** (el hash de la ruta no cambia entre reinicios): al ver un cambio de estilo hay que vaciar la caché.

## Validación

- `scripts/tests/test_auth_theme.py` (sincronización con el sitio, tokens, contraste, estilos, realm, documentación) y `RealmTokenContractIT` (Keycloak sirve las hojas, las tres fuentes, el logo y la marca, y no cambia a modo oscuro).
- Importación real del realm y del tema del repo, con `scripts/e2e_correo_de_cuenta.py` y `scripts/e2e_google_vinculacion.py` sin regresiones.

## Trabajo Futuro / Links

- Reemplazar la plantilla de "La cuenta ya existe" para que no salga como error.
- Probar el tema en Chrome y Safari.
- Un tema de la consola de cuenta, si el sitio llega a enlazarla.
