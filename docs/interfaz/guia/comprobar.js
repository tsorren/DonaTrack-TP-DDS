// Capturas y comprobaciones del Artifact. Uso: node comprobar.js <ruta a playwright> [capturas|medir|todo]
const fs = require('fs'); const path = require('path');
const { chromium } = require(process.argv[2]);
const modo = process.argv[3] || 'todo';
const aqui = __dirname;
const ARTIFACT = path.join(aqui, 'guia-y-pantallas.html');
const VISTA = path.join(aqui, '_vista.html');
const CAP = path.join(aqui, 'capturas');
const URL = 'file:///' + VISTA.split(path.sep).join('/');
const PANTALLAS = ['portada', 'necesidades', 'necesidades-despues', 'bandeja', 'inicio-donante', 'donaciones', 'detalle-asignada', 'seguimiento', 'detalle-problema', 'inicio-organizacion', 'inicio-organizacion-despues', 'entregas', 'entregas-despues', 'entrega-problema', 'entrega-problema-despues', 'confirmar', 'confirmar-error', 'confirmada'];
const lineas = []; const decir = (...a) => { const t = a.join(' '); lineas.push(t); console.log(t); };
let fallas = 0; const falla = t => { fallas++; decir('  FALLA: ' + t); };

fs.writeFileSync(VISTA, '<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><style>:root{color-scheme:light;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0;font:14px system-ui}img{max-width:100%}[hidden]{display:none!important}</style></head><body>' + fs.readFileSync(ARTIFACT, 'utf8') + '</body></html>');

async function abrir(b, ancho, alto, opciones) {
  const c = await b.newContext(Object.assign({ viewport: { width: ancho, height: alto || 900 } }, opciones || {}));
  const p = await c.newPage(); p.errores = [];
  p.on('console', m => { if (m.type() === 'error') p.errores.push(m.text()); }); p.on('pageerror', e => p.errores.push(String(e)));
  return p;
}
async function ir(p, ancla) {
  await p.goto(URL + (ancla ? '#' + ancla : ''), { waitUntil: 'load' });
  await p.evaluate(() => document.fonts.ready); await p.waitForTimeout(350);
}
const desborde = p => p.evaluate(() => ({ doc: document.documentElement.scrollWidth, vista: innerWidth, quien: [...document.querySelectorAll('.vista.activa *')].filter(e => { const b = e.getBoundingClientRect(); if (!(b.width > 0 && b.right > innerWidth + 0.5)) return false; for (let a = e.parentElement; a; a = a.parentElement) { const o = getComputedStyle(a); if (/(auto|scroll|hidden|clip)/.test(o.overflowX)) return false; } return true; }).slice(0, 5).map(e => e.tagName.toLowerCase() + '.' + String(e.className).split(' ').join('.')) }));

(async () => {
  const b = await chromium.launch();

  // ---------- Capturas ----------
  if (modo !== 'medir') {
    for (const [ancho, tema] of [[1280, 'light'], [360, 'light'], [1280, 'dark']]) {
      const p = await abrir(b, ancho, 900, { colorScheme: tema });
      for (const v of ['inicio', 'guia']) {
        await ir(p, v); const suf = `${ancho}${tema === 'dark' ? '-oscuro' : ''}`;
        const n = await p.locator('#' + v + ' .seccion').count();
        for (let i = 0; i < n; i++) { const s = p.locator('#' + v + ' .seccion').nth(i); const id = (await s.getAttribute('id')) || 'cabeza'; if (tema === 'dark' && !['cabeza', 'g-botones', 'analisis-portada'].includes(id)) continue; await s.screenshot({ path: path.join(CAP, `lamina-${v}-${id}-${suf}.png`) }); }
      }
      await p.context().close();
    }
    let p = await abrir(b, 1400, 900);
    for (const v of PANTALLAS) {
      await p.context().close(); p = await abrir(b, 1400, 900);
      await ir(p, v);
      await p.check('#ancho-1280'); await p.waitForTimeout(400);
      await p.locator('.vista.activa .marco-lienzo').screenshot({ path: path.join(CAP, `${v}-1280.png`) });
      await p.check('#ancho-360'); await p.evaluate(() => document.documentElement.classList.add('captura')); await p.waitForTimeout(150);
      await p.locator('.vista.activa .marco-lienzo').screenshot({ path: path.join(CAP, `${v}-360.png`) });
      await p.evaluate(() => document.documentElement.classList.add('captura-640')); await p.waitForTimeout(100);
      await p.locator('.vista.activa .marco-lienzo').screenshot({ path: path.join(CAP, `${v}-360x640.png`) });
      await p.evaluate(() => document.documentElement.classList.remove('captura', 'captura-640'));
    }
    await p.screenshot({ path: path.join(CAP, 'lamina-pantalla-1400.png') });
    await p.context().close();
    const q = await abrir(b, 390, 800); await ir(q, 'necesidades'); await q.screenshot({ path: path.join(CAP, 'lamina-pantalla-390.png') }); await q.context().close();
    decir('Capturas en ' + CAP);
  }

  // ---------- Medidas ----------
  if (modo !== 'capturas') {
    decir('== Comprobación del Artifact «Guía de estilo y pantallas» ==');
    // 1. Desborde de la página (lámina) a 320, 360, 1280 y 1920, en todas las vistas
    decir('\n1. Desplazamiento horizontal de la página, por vista y ancho');
    for (const ancho of [320, 360, 1280, 1920]) {
      const p = await abrir(b, ancho, 900); const res = [];
      for (const v of ['inicio', 'guia'].concat(PANTALLAS)) { await ir(p, v); const d = await desborde(p); res.push(v + (d.doc > d.vista ? ' DESBORDA(' + d.doc + ': ' + d.quien.join(' ') + ')' : ' ok')); if (d.doc > d.vista) falla(`la página desborda a ${ancho} en ${v}`); }
      decir(`  ${ancho} px: ` + res.join(', ')); if (p.errores.length) falla('errores de consola: ' + p.errores.slice(0, 3).join(' | '));
      await p.context().close();
    }

    // 2. Cada pantalla dentro de su marco, a 320, 360, 1280 y 1920
    decir('\n2. Cada pantalla en su marco: desborde, toque de 48 px, acción llena, primera pantalla del celular, renglones');
    const p = await abrir(b, 2000, 1000);
    for (const v of PANTALLAS) {
      for (const ancho of [320, 360, 1280, 1920]) {
        await ir(p, v);
        await p.addStyleTag({ content: `.marco-fila{max-width:none!important}.marco-lienzo{width:${ancho}px!important;height:auto!important;overflow:clip!important;zoom:1!important}` });
        await p.waitForTimeout(120);
        const r = await p.evaluate((ancho) => {
          const lienzo = document.querySelector('.vista.activa .marco-lienzo'); const pant = lienzo.querySelector('.dt.pantalla');
          const L = lienzo.getBoundingClientRect(); const visible = e => { const c = getComputedStyle(e); const b = e.getBoundingClientRect(); return c.display !== 'none' && c.visibility !== 'hidden' && b.width > 0 && b.height > 0 && !e.closest('.oculto-visual') && !e.classList.contains('saltar'); };
          const fuera = [...pant.querySelectorAll('*')].filter(e => visible(e) && !e.closest('svg') && (e.getBoundingClientRect().right > L.right + 0.5 || e.getBoundingClientRect().left < L.left - 0.5)).map(e => e.tagName.toLowerCase() + '.' + e.className).slice(0, 4);
          const enOracion = a => a.tagName === 'A' && getComputedStyle(a).display === 'inline' && a.parentElement && [...a.parentElement.childNodes].some(n => n !== a && n.textContent.trim().length > 3);
          // toque
          const chicos = [];
          pant.querySelectorAll('a[href], button, select, textarea, input, summary').forEach(e => {
            let blanco = e;
            if (e.matches('input[type=radio], input[type=checkbox]')) blanco = e.closest('label');
            if (e.matches('input[type=file]')) blanco = pant.querySelector('label[for="' + e.id + '"]');
            if (!blanco || !visible(blanco)) return;
            const c = getComputedStyle(blanco);
            if (enOracion(blanco)) return; // enlace dentro de una oración, con más texto alrededor: exceptuado (WCAG 2.5.8)
            const bb = blanco.getBoundingClientRect();
            if (bb.width < 47.5 || bb.height < 47.5) chicos.push((blanco.textContent.trim().slice(0, 24) || blanco.getAttribute('aria-label') || blanco.tagName) + ' ' + Math.round(bb.width) + '×' + Math.round(bb.height));
          });
          const llenos = [...pant.querySelectorAll('.boton--lleno')].filter(visible);
          const h1 = pant.querySelector('main h1'); const tope = L.top;
          const enlacesEnLinea = [...pant.querySelectorAll('a[href]')].filter(e => visible(e) && enOracion(e)).length;
          const signosGrandes = [...pant.querySelectorAll('.signo')].filter(e => visible(e) && e.getBoundingClientRect().width > 24).length;
          // largo de renglón en texto corrido
          const largos = [];
          const lienzoMedida = document.createElement('canvas').getContext('2d'); const corridos = [];
          pant.querySelectorAll('main p').forEach(e => { if (!visible(e)) return; const c = getComputedStyle(e); const lh = parseFloat(c.lineHeight); const n = Math.round(e.getBoundingClientRect().height / lh); const txt = e.textContent.trim().replace(/\s+/g, ' '); if (n < 2) return; lienzoMedida.font = c.fontWeight + ' ' + c.fontSize + ' ' + c.fontFamily; const anchoTexto = lienzoMedida.measureText(txt).width; const porRenglon = Math.round(txt.length * (e.clientWidth - parseFloat(c.paddingLeft) - parseFloat(c.paddingRight)) / anchoTexto); largos.push(porRenglon); if (n >= 3) corridos.push(porRenglon); });
          const menu = [...pant.querySelectorAll('.menu-celular a')].filter(visible).filter(a => a.scrollWidth > a.clientWidth + 1 || a.getBoundingClientRect().height > 70).map(a => a.textContent.trim());
          const menuFijo = [...pant.querySelectorAll('.menu-celular')].filter(visible)[0]; const altoMenu = menuFijo ? Math.round(menuFijo.getBoundingClientRect().height) : 0;
          return { altoMenu, signosGrandes, desb: lienzo.scrollWidth > lienzo.clientWidth + 1, fuera, chicos, llenos: llenos.length, h1: h1 ? Math.round(h1.getBoundingClientRect().bottom - tope) : null, lleno: llenos[0] ? Math.round(llenos[0].getBoundingClientRect().bottom - tope) : null, enlacesEnLinea, largoMax: largos.length ? Math.max(...largos) : 0, largoMin: largos.length ? Math.min(...largos) : 0, corridoMin: corridos.length ? Math.min(...corridos) : 0, corridoMax: corridos.length ? Math.max(...corridos) : 0, menu, alto: Math.round(L.height) };
        }, ancho);
        let linea = `  ${v} a ${ancho}: desborde ${r.desb || r.fuera.length ? 'SÍ ' + r.fuera.join(' ') : 'no'}; controles menores a 48: ${r.chicos.length ? r.chicos.join(' | ') : 'ninguno'}; acciones llenas: ${r.llenos}; enlaces en oración exceptuados: ${r.enlacesEnLinea}`;
        if (ancho === 360) linea += `; título termina a ${r.h1} px y la acción llena a ${r.lleno} px; en un celular de 640 px de alto el menú fijo empieza a los ${640 - r.altoMenu} px`;
        if (ancho === 1280) linea += `; párrafos de 2 o más renglones: entre ${r.largoMin} y ${r.largoMax} caracteres por renglón` + (r.corridoMax ? `; texto corrido (3 o más renglones): entre ${r.corridoMin} y ${r.corridoMax}` : '; sin párrafos de 3 o más renglones');
        decir(linea);
        if (r.desb || r.fuera.length) falla(`desborde en ${v} a ${ancho}`);
        if (r.chicos.length) falla(`controles menores a 48 px en ${v} a ${ancho}`);
        if (r.llenos > 1) falla(`más de una acción llena en ${v}`);
        if (r.signosGrandes) falla(`${r.signosGrandes} signos de más de 24 px en ${v} a ${ancho}`);
        if (r.menu.length) falla(`rótulos del menú del celular partidos en ${v}: ${r.menu.join(', ')}`);
        if (ancho === 360 && !v.startsWith('confirmar') && ((r.h1 || 0) > 640 || (r.lleno || 0) > 640)) falla(`título o acción llena fuera de los primeros 640 px en ${v}`);
        if (ancho === 360 && !v.startsWith('confirmar') && r.lleno && r.lleno > 640 - r.altoMenu) falla(`la acción llena queda debajo del menú fijo en ${v}`);
        if (ancho === 1280 && r.largoMax > 75) falla(`renglón de ${r.largoMax} caracteres en ${v}`);
        if (ancho === 1280 && r.corridoMax && r.corridoMin < 50) falla(`texto corrido con renglones de ${r.corridoMin} caracteres en ${v}`);
      }
    }
    if (p.errores.length) falla('errores de consola: ' + p.errores.slice(0, 3).join(' | '));

    // 2b. Las muestras de la guía: toque de 48 px y tamaño de los signos
    decir('\n2b. Muestras de la guía: controles de menos de 48 px y signos de más de 24 px');
    for (const ancho of [360, 1280]) {
      const g = await abrir(b, ancho, 900); await ir(g, 'guia');
      const r = await g.evaluate(() => {
        const enOracion = a => a.tagName === 'A' && getComputedStyle(a).display === 'inline' && a.parentElement && [...a.parentElement.childNodes].some(n => n !== a && n.textContent.trim().length > 3);
        const visible = e => { const c = getComputedStyle(e); const b = e.getBoundingClientRect(); return c.display !== 'none' && c.visibility !== 'hidden' && b.width > 0 && b.height > 0 && !e.closest('.oculto-visual'); };
        const chicos = []; let medidos = 0;
        document.querySelectorAll('#guia .dt a[href], #guia .dt button, #guia .dt select, #guia .dt textarea, #guia .dt input').forEach(e => {
          let blanco = e; if (e.matches('input[type=radio], input[type=checkbox]')) blanco = e.closest('label');
          if (!blanco || !visible(blanco) || enOracion(blanco)) return; medidos++;
          const bb = blanco.getBoundingClientRect(); if (bb.width < 47.5 || bb.height < 47.5) chicos.push((blanco.textContent.trim().slice(0, 24) || blanco.getAttribute('aria-label') || blanco.tagName) + ' ' + Math.round(bb.width) + '×' + Math.round(bb.height));
        });
        const signos = [...document.querySelectorAll('#guia .dt .signo')].filter(e => visible(e) && e.getBoundingClientRect().width > 24).length;
        return { chicos, medidos, signos };
      });
      decir(`  guía a ${ancho}: ${r.medidos} controles medidos; menores a 48: ${r.chicos.length ? r.chicos.join(' | ') : 'ninguno'}; signos grandes: ${r.signos}`);
      if (r.chicos.length) falla(`controles menores a 48 px en la guía a ${ancho}`); if (r.signos) falla('signos de más de 24 px en la guía');
      await g.context().close();
    }

    // 3. Detalles gráficos
    decir('\n3. Detalles gráficos por pantalla (atributo data-d)');
    await ir(p, 'portada');
    const dg = await p.evaluate(() => [...document.querySelectorAll('.dt.pantalla[data-pantalla]')].map(x => { const usados = {}; x.querySelectorAll('[data-d]').forEach(e => e.dataset.d.split(' ').forEach(d => { usados[d] = (usados[d] || 0) + 1; })); return { pantalla: x.dataset.pantalla, esqueleto: x.dataset.esqueleto, usados, frutos: x.querySelectorAll('.portada-dibujo figcaption .frutos li').length }; }));
    const frutosDibujo = await p.evaluate(() => document.querySelectorAll('#ilustracion-recorrido .f-fruto circle').length);
    dg.forEach(d => {
      decir(`  ${d.pantalla} (${d.esqueleto}): ` + (Object.keys(d.usados).sort().map(k => k + '×' + d.usados[k]).join(', ') || 'ninguno'));
      if (d.usados.D6) falla('se usa D6 en ' + d.pantalla);
      if ((d.usados.D7 || 0) > 1) falla('D7 más de una vez en ' + d.pantalla);
      if (d.esqueleto === 'galpon' && Object.keys(d.usados).some(k => k !== 'D1')) falla('el galpón usa un detalle que no es D1');
    });
    decir(`  Frutos en la ilustración: ${frutosDibujo}; frutos al pie: ${dg[0].frutos}; cifra del mes: 9`);
    if (frutosDibujo !== 9 || dg[0].frutos !== 9) falla('los frutos no son 9');
    for (const [ancla, esperados, conPaja] of [['inicio-organizacion', 4, true], ['inicio-organizacion-despues', 5, false], ['confirmada', 5, true]]) {
      await ir(p, ancla);
      const f = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const ve = e => { for (let a = e; a && a !== pant; a = a.parentElement) { if (getComputedStyle(a).display === 'none') return false; } return true; }; const fig = pant.querySelector('.puerta-dibujo'); const cifra = fig.querySelector('figcaption strong'); const numero = [...cifra.querySelectorAll('span')].filter(ve).map(s => s.textContent).join('') || cifra.textContent; const lam = pant.querySelector('.lamina--protagonista'); return { dibujo: [...fig.querySelectorAll('.pta-fruto')].filter(ve).length, pie: [...fig.querySelectorAll('figcaption .frutos li')].filter(ve).length, cifra: parseInt(numero, 10), bultos: [...fig.querySelectorAll('.pta-bultos')].filter(ve).length, falta: [...fig.querySelectorAll('.pta-tramo-falta')].filter(ve).length, hecho: [...fig.querySelectorAll('.pta-hecho')].filter(ve).length, sombra: getComputedStyle(lam).boxShadow, llenos: [...pant.querySelectorAll('.boton--lleno')].filter(ve).length }; });
      decir(`  La puerta en ${ancla}: ${f.dibujo} frutos en el árbol, ${f.pie} al pie y la cifra dice ${f.cifra}; bultos en la puerta: ${f.bultos}; tramo lleno hasta el quinto nudo: ${f.hecho ? 'sí' : 'no'}; sombra de paja: ${f.sombra === 'none' ? 'no' : 'sí'}; acciones llenas: ${f.llenos}`);
      if (f.dibujo !== esperados || f.pie !== esperados || f.cifra !== esperados) falla(`los frutos de la puerta no son ${esperados} en ${ancla}`);
      if ((f.sombra !== 'none') !== conPaja) falla(`la sombra de paja no corresponde en ${ancla}`);
      if (ancla === 'inicio-organizacion' && (f.bultos !== 1 || f.hecho !== 0 || f.llenos !== 1)) falla('el estado con la entrega en la puerta no se dibuja completo');
      if (ancla !== 'inicio-organizacion' && (f.bultos !== 0 || f.hecho !== 1 || f.llenos !== 0)) falla(`el estado posterior no se dibuja completo en ${ancla}`);
    }

    // 3b. Las pantallas de la versión 9: la tabla, el paso que no pudo ser y los dos estados de Entregas
    await ir(p, 'donaciones');
    const tb = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const n = s => pant.querySelectorAll(s).length; const cuentas = [...pant.querySelectorAll('.pestanas .cuenta-chica')].map(c => parseInt(c.textContent, 10)); return { filas: n('tbody tr'), raices: n('tbody .raiz'), sellos: n('tbody .sello'), enlaces: n('tbody .tabla-enlace'), rotulos: [...pant.querySelectorAll('tbody td')].filter(td => !td.dataset.rotulo).length, problema: n('tbody .raiz-paso[data-paso="problema"] .signo'), cuentas, llenos: n('.boton--lleno'), tabla: pant.querySelector('table').getAttribute('role'), encabezados: n('thead th[scope="col"]') + n('tbody th[scope="row"]') }; });
    decir(`  Donaciones: ${tb.filas} filas, ${tb.raices} raíces, ${tb.sellos} sellos y ${tb.enlaces} enlaces al detalle; celdas sin rótulo: ${tb.rotulos}; pestañas: ${tb.cuentas.join(', ')}; nudos «no pudo llegar» con signo: ${tb.problema}; acciones llenas: ${tb.llenos}; encabezados con scope: ${tb.encabezados}`);
    if (tb.filas !== 6 || tb.raices !== 6 || tb.sellos !== 6 || tb.enlaces !== 6 || tb.rotulos !== 0 || tb.problema !== 1 || tb.llenos !== 0 || tb.tabla !== 'table' || tb.encabezados !== 10) falla('la tabla de donaciones no está completa');
    if (tb.cuentas[0] !== 6 || tb.cuentas[1] + tb.cuentas[2] + tb.cuentas[3] !== 6) falla('las pestañas de Donaciones no suman 6');
    for (const ancho of [360, 1280]) {
      await ir(p, 'donaciones'); await p.addStyleTag({ content: `.marco-fila{max-width:none!important}.marco-lienzo{width:${ancho}px!important;height:auto!important;overflow:clip!important;zoom:1!important}` }); await p.waitForTimeout(120);
      const ap = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const c = e => getComputedStyle(e); const td = pant.querySelector('tbody td'); return { fila: c(pant.querySelector('tbody tr')).display, rotulo: getComputedStyle(td, '::before').content, cabeza: Math.round(pant.querySelector('thead').getBoundingClientRect().height), ocultas: [...pant.querySelectorAll('tbody td, tbody th')].filter(x => c(x).display === 'none' || x.getBoundingClientRect().width === 0).length, corte: [...pant.querySelectorAll('tbody td > *, tbody th > *')].filter(x => x.scrollWidth > x.clientWidth + 1).length }; });
      decir(`  Donaciones a ${ancho}: cada fila se dibuja como «${ap.fila}»; el rótulo de la primera celda es ${ap.rotulo}; la cabecera mide ${ap.cabeza} px de alto; celdas escondidas: ${ap.ocultas}; celdas con texto cortado: ${ap.corte}`);
      if (ap.ocultas || ap.corte) falla(`la tabla esconde o corta datos a ${ancho}`);
      if (ancho === 360 && (ap.fila !== 'grid' || !/Para/.test(ap.rotulo))) falla('la tabla no se apila a 360');
      if (ancho === 1280 && (ap.fila !== 'table-row' || ap.cabeza < 20)) falla('la tabla no es tabla a 1280');
    }
    await ir(p, 'detalle-problema');
    const dp = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const n = s => pant.querySelectorAll(s).length; return { avisos: n('.aviso'), paja: n('.lamina--protagonista'), llenos: n('.boton--lleno'), pasos: [...pant.querySelectorAll('.raiz > li')].map(l => l.dataset.paso).join(','), signo: n('.raiz-paso[data-paso="problema"] .signo'), mapa: n('.mapa') }; });
    decir(`  Donación con problema: avisos ${dp.avisos}; láminas protagonistas ${dp.paja}; acciones llenas ${dp.llenos}; mapas ${dp.mapa}; pasos de la raíz: ${dp.pasos}; el nudo del problema lleva signo: ${dp.signo ? 'sí' : 'no'}`);
    if (dp.avisos !== 1 || dp.paja !== 0 || dp.llenos !== 0 || dp.mapa !== 0 || dp.pasos !== 'hecho,hecho,hecho,problema,falta' || dp.signo !== 1) falla('la donación con problema no se dibuja como dice el criterio 23');
    for (const [ancla, esperados, conTarea] of [['entregas', 4, true], ['entregas-despues', 5, false]]) {
      await ir(p, ancla);
      const f = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const ve = e => { for (let a = e; a && a !== pant; a = a.parentElement) { if (getComputedStyle(a).display === 'none') return false; } return true; }; const caja = pant.querySelector('.frutos-cuenta'); const numero = [...caja.querySelectorAll('strong span')].filter(ve).map(s => s.textContent).join(''); const fotos = [...pant.querySelectorAll('.etiqueta--fila')].filter(ve).map(e => e.querySelector('use').getAttribute('href')); return { frutos: [...caja.querySelectorAll('.frutos li')].filter(ve).length, cifra: parseInt(numero, 10), etiquetas: fotos.length, distintas: new Set(fotos).size, paja: [...pant.querySelectorAll('.lamina--protagonista')].filter(ve).length, llenos: [...pant.querySelectorAll('.boton--lleno')].filter(ve).length, curso: [...pant.querySelectorAll('.grilla-2 > .lamina h3')].filter(ve).map(h => h.textContent).join(' | '), estado: pant.dataset.estado }; });
      decir(`  Entregas en ${ancla} (estado «${f.estado}»): ${f.frutos} frutos, la cifra dice ${f.cifra} y hay ${f.etiquetas} etiquetas con ${f.distintas} fotos distintas; láminas protagonistas: ${f.paja}; acciones llenas: ${f.llenos}; por llegar: ${f.curso}`);
      if (f.frutos !== esperados || f.cifra !== esperados || f.etiquetas !== esperados || f.distintas !== esperados) falla(`los frutos, la cifra y las etiquetas de Entregas no son ${esperados} en ${ancla}`);
      if ((f.paja === 1) !== conTarea || (f.llenos === 1) !== conTarea) falla(`la lámina protagonista o la acción llena no corresponden en ${ancla}`);
      if (!/^Aceite de girasol/.test(f.curso)) falla('en «Por llegar», la entrega con problema no va primera');
    }
    const fotosTodas = await p.evaluate(() => { const m = {}; document.querySelectorAll('.vista--pantalla .foto > svg > use').forEach(u => { const et = u.closest('.etiqueta, .foto'); const titulo = (u.closest('.etiqueta') ? u.closest('.etiqueta').querySelector('h3').textContent : 'formulario'); const pie = (u.closest('.etiqueta') && u.closest('.etiqueta').querySelector('.etiqueta-pie') ? u.closest('.etiqueta').querySelector('.etiqueta-pie').textContent.replace(/hoy, |, 18:45/g, '').replace(/^Recibió \S+ el /, '').replace(/^Recibió \S+ /, '') : ''); const h = u.getAttribute('href'); (m[h] = m[h] || new Set()).add(titulo + ' · ' + pie); }); return Object.keys(m).map(h => h + ': ' + [...m[h]].join(' / ')); });
    decir('  Qué entrega muestra cada foto de ejemplo, en las trece pantallas:'); fotosTodas.forEach(x => decir('    ' + x));

    // 3d. Las pantallas de la versión 10: la donación asignada, la entrega con problema a sus dos horas y los dos estados de Necesidades
    await ir(p, 'detalle-asignada');
    const da = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const n = s => pant.querySelectorAll(s).length; const av = pant.querySelector('progress.avance'); return { avisos: n('.aviso'), paja: n('.lamina--protagonista'), llenos: n('.boton--lleno'), mapa: n('.mapa'), pasos: [...pant.querySelectorAll('.raiz > li')].map(l => l.dataset.paso).join(','), textos: [...pant.querySelectorAll('.raiz > li')].filter(l => l.querySelector('.suave')).length, actual: n('.raiz > li[aria-current="step"]'), avance: av.value + ' de ' + av.max, cuenta: pant.querySelector('progress.avance + p').textContent, sello: pant.querySelector('.cabecera-pantalla .sello').textContent.trim(), horas: (pant.textContent.match(/\d{1,2}:\d{2}/g) || []).join(' ') }; });
    decir(`  Donación asignada: sello «${da.sello}»; avisos ${da.avisos}; láminas protagonistas ${da.paja}; acciones llenas ${da.llenos}; mapas ${da.mapa}; pasos de la raíz: ${da.pasos}, los 5 con su texto: ${da.textos === 5 ? 'sí' : 'NO'}; la necesidad: ${da.avance} · «${da.cuenta}»; horas que nombra: ${da.horas}`);
    if (da.avisos !== 0 || da.paja !== 0 || da.llenos !== 0 || da.mapa !== 0 || da.pasos !== 'hecho,actual,falta,falta,falta' || da.textos !== 5 || da.actual !== 1 || da.sello !== 'Asignada') falla('la donación asignada no se dibuja como dice el criterio 26');
    if (da.avance !== '12 de 24' || !/12 de 24 unidades/.test(da.cuenta) || !/Tus 6/.test(da.cuenta) || !/Quedan 6/.test(da.cuenta)) falla('la necesidad de la donación asignada no dice 12, 6 y 6 de 24');
    if (da.horas !== '16:25 12:15 16:25 09:20 12:15') falla('la donación asignada nombra una hora que la hoja no tiene: ' + da.horas);
    const textoDe = {};
    for (const [ancla, sufijo, hora] of [['entrega-problema', '', '18:30'], ['entrega-problema-despues', '-despues', '18:50']]) {
      await ir(p, ancla);
      const f = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const ve = e => { for (let a = e; a && a !== pant; a = a.parentElement) { if (getComputedStyle(a).display === 'none') return false; } return true; }; const n = s => [...pant.querySelectorAll(s)].filter(ve).length; const texto = []; const w = document.createTreeWalker(pant.querySelector('main'), NodeFilter.SHOW_TEXT); while (w.nextNode()) { const t = w.currentNode.textContent.trim(); if (t && ve(w.currentNode.parentElement)) texto.push(t); } return { estado: pant.dataset.estado, avisos: n('.aviso'), paja: n('.lamina--protagonista'), llenos: n('.boton--lleno'), mapa: n('.mapa'), contacto: [...pant.querySelectorAll('main a, main button')].filter(ve).filter(a => /escrib|contact|llam|mensaje/i.test(a.textContent)).length, pasos: [...pant.querySelectorAll('.raiz > li')].map(l => l.dataset.paso).join(','), signo: n('.raiz-paso[data-paso="problema"] .signo'), vueltas: [...pant.querySelectorAll('a[href^="#inicio-organizacion"], a[href^="#necesidades"], a[href^="#entregas"]')].filter(ve).map(a => a.getAttribute('href')), texto: texto.join(' ') }; });
      textoDe[ancla] = f.texto;
      const malas = f.vueltas.filter(h => h.endsWith('-despues') !== (sufijo === '-despues'));
      decir(`  Entrega con problema en ${ancla} (estado «${f.estado}»): avisos ${f.avisos}; láminas protagonistas ${f.paja}; acciones llenas ${f.llenos}; mapas ${f.mapa}; formas de contacto ${f.contacto}; pasos de la raíz: ${f.pasos}; el nudo del problema lleva signo: ${f.signo ? 'sí' : 'no'}; ${f.vueltas.length} enlaces de vuelta a la vista en escritorio (la marca, tres secciones, las migas y «Volver a Entregas»), ${malas.length} a otra hora`);
      if (f.avisos !== 1 || f.paja !== 0 || f.llenos !== 0 || f.mapa !== 0 || f.contacto !== 0 || f.pasos !== 'hecho,hecho,hecho,problema,falta' || f.signo !== 1) falla(`la entrega con problema no se dibuja como dice el criterio 27 en ${ancla}`);
      if (f.vueltas.length !== 6 || malas.length) falla(`los enlaces de vuelta de ${ancla} no abren todos el estado de las ${hora}: ${f.vueltas.join(' ')}`);
      if (!f.texto.includes(hora) || f.texto.includes(hora === '18:30' ? '18:50' : '18:30')) falla(`la hora de ${ancla} no es ${hora}`);
    }
    const igualSalvoHora = textoDe['entrega-problema'].replace(/18:30/g, 'HH') === textoDe['entrega-problema-despues'].replace(/18:50/g, 'HH');
    decir(`  Entrega con problema: el texto de las dos horas es el mismo salvo la hora: ${igualSalvoHora ? 'sí' : 'NO'}`); if (!igualSalvoHora) falla('la entrega con problema no dice lo mismo a las dos horas');
    for (const [ancla, conTarea, llegaron] of [['necesidades', true, 10], ['necesidades-despues', false, 25]]) {
      await ir(p, ancla);
      const f = await p.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); const ve = e => { for (let a = e; a && a !== pant; a = a.parentElement) { if (getComputedStyle(a).display === 'none') return false; } return true; }; const n = s => [...pant.querySelectorAll(s)].filter(ve).length; const tarjeta = pant.querySelector('.grilla-2 > li'); const av = [...tarjeta.querySelectorAll('progress')].filter(ve); return { estado: pant.dataset.estado, paja: n('.lamina--protagonista'), llenos: n('.boton--lleno'), pedidos: n('.pedido'), sellos: n('.sello'), tarjetas: n('.grilla-2 > li'), avances: av.length, llegaron: av[0] ? av[0].value : null, frase: [...tarjeta.querySelectorAll('p')].filter(ve).map(x => x.textContent).join(' · '), vigentes: n('.pestanas a[aria-current="page"]'), subtitulo: [...pant.querySelectorAll('main header p')].filter(ve)[0].innerText }; });
      decir(`  Necesidades en ${ancla} (estado «${f.estado}»): láminas protagonistas ${f.paja}; acciones llenas ${f.llenos}; chapas «Espera tu confirmación» ${f.pedidos}; sellos ${f.sellos}; tarjetas ${f.tarjetas}; lentejas: ${f.llegaron} de 40 kg · «${f.frase}»; pestaña elegida a la vista: ${f.vigentes}`);
      if ((f.paja === 1) !== conTarea || (f.llenos === 1) !== conTarea || f.pedidos !== (conTarea ? 2 : 0)) falla(`la lámina protagonista, la acción llena o la chapa no corresponden en ${ancla}`);
      if (f.sellos !== 2 || f.tarjetas !== 4 || f.avances !== 1 || f.llegaron !== llegaron || f.vigentes !== 1) falla(`las tarjetas de Necesidades no están completas en ${ancla}`);
      if (conTarea ? !/18:30/.test(f.subtitulo) || /18:50/.test(f.subtitulo) : !/18:50/.test(f.subtitulo) || !/18:45/.test(f.subtitulo)) falla(`el subtítulo de ${ancla} no dice su hora`);
    }

    // 3c. El largo de las pantallas nuevas en un celular de 360 × 640, y los frutos del inicio de quien dona
    for (const [ancla, sinFila] of [['donaciones', false], ['detalle-problema', false], ['entregas', false], ['entregas-despues', false], ['entregas-despues', true], ['detalle-asignada', false], ['entrega-problema', false], ['necesidades', false], ['necesidades-despues', false]]) {
      await ir(p, ancla); await p.addStyleTag({ content: '.marco-fila{max-width:none!important}.marco-lienzo{width:360px!important;height:auto!important;overflow:clip!important;zoom:1!important}' });
      if (sinFila) await p.evaluate(() => document.querySelectorAll('.vista.activa .etiqueta--fila').forEach(e => e.classList.remove('etiqueta--fila')));
      await p.waitForTimeout(150);
      const alto = await p.evaluate(() => Math.round(document.querySelector('.vista.activa .marco-lienzo').getBoundingClientRect().height));
      decir(`  ${ancla} a 360${sinFila ? ', con la etiqueta común, que se apila' : ''}: ${alto} px de alto, que son ${(alto / 640).toFixed(1).replace('.', ',')} pantallas de 640 px`);
    }
    await ir(p, 'inicio-donante');
    const fd = await p.evaluate(() => { const c = document.querySelector('.vista.activa .dt.pantalla .frutos-cuenta'); return { frutos: c.querySelectorAll('.frutos li').length, cifra: parseInt(c.querySelector('strong').textContent, 10) }; });
    decir(`  Inicio de quien dona: ${fd.frutos} frutos y la cifra dice ${fd.cifra}`); if (fd.frutos !== 2 || fd.cifra !== 2) falla('los frutos del inicio de quien dona no son 2');

    // 4. Contraste
    decir('\n4. Contraste, calculado desde los tokens');
    await ir(p, 'guia');
    const con = await p.evaluate(() => [...document.querySelectorAll('#tabla-contraste tr')].map(tr => ({ par: tr.dataset.par, razon: tr.dataset.razon, pasa: tr.dataset.pasa, nombre: tr.firstChild.textContent, piso: tr.children[3].textContent })));
    con.forEach(c => { decir(`  ${c.razon} (piso ${c.piso}) ${c.pasa === 'si' ? 'pasa' : 'NO PASA'}: ${c.nombre} [${c.par}]`); if (c.pasa !== 'si') falla('contraste: ' + c.nombre); });

    // 5. La letra: el eje de ancho de Archivo responde
    const letra = await p.evaluate(() => { const s = document.createElement('span'); s.textContent = 'Seguí tu donación'; s.style.cssText = 'position:absolute;font:850 40px Archivo;white-space:nowrap'; document.body.appendChild(s); s.style.fontStretch = '62%'; const a = s.getBoundingClientRect().width; s.style.fontStretch = '122%'; const z = s.getBoundingClientRect().width; s.remove(); return { a: Math.round(a), z: Math.round(z), archivo: document.fonts.check('850 40px Archivo'), dm: document.fonts.check("17px 'DM Sans'") }; });
    decir(`\n5. Letras: Archivo cargada ${letra.archivo}, DM Sans cargada ${letra.dm}; un título mide ${letra.a} px a 62 % de ancho y ${letra.z} px a 122 %`);
    if (!(letra.z > letra.a * 1.3)) falla('el eje de ancho de Archivo no responde');
    await p.context().close();

    // 6. Foco: visible y nunca tapado, con Tab, a 360 (marco de 640 de alto) y a 1280
    decir('\n6. Foco con Tab: visible y no tapado por lo fijo');
    let paradasTotales = 0;
    for (const [ancho, radio] of [[360, '#ancho-360'], [1280, '#ancho-1280']]) {
      const q = await abrir(b, 1400, 760);
      for (const v of PANTALLAS) {
        await ir(q, v); await q.check(radio); await q.waitForTimeout(120);
        const n = await q.evaluate(() => { const pant = document.querySelector('.vista.activa .dt.pantalla'); return [...pant.querySelectorAll('a[href], button, input, select, textarea')].filter(e => { const c = getComputedStyle(e); return c.display !== 'none' && c.visibility !== 'hidden' && !e.disabled; }).length; });
        await q.evaluate(() => { window.scrollTo(0, 0); const f = [...document.querySelectorAll('.lam-herramientas a, .lam-herramientas input, .vista.activa .ficha a')].filter(e => e.offsetParent); f[f.length - 1].focus(); }); await q.keyboard.press('Tab');
        const malos = []; let vistos = 0;
        for (let i = 0; i < n + 2; i++) {
          const r = await q.evaluate(() => {
            const e = document.activeElement; const pant = document.querySelector('.vista.activa .dt.pantalla');
            if (!e || !pant.contains(e)) return null;
            let blanco = e; if (e.matches('input[type=radio], input[type=checkbox]')) blanco = e.closest('label'); if (e.matches('input[type=file]')) blanco = pant.querySelector('label[for="' + e.id + '"]');
            const hayAro = x => { const c = getComputedStyle(x); return c.outlineStyle !== 'none' && parseFloat(c.outlineWidth) >= 2; }; const boton = blanco.matches('label.boton') ? blanco : blanco.querySelector(':scope > .boton'); const aro = hayAro(e) || hayAro(blanco) || (boton ? hayAro(boton) : false);
            const bb = blanco.getBoundingClientRect(); const cx = bb.left + bb.width / 2, cy = bb.top + bb.height / 2;
            const arriba = document.elementFromPoint(cx, cy); const tapado = !(arriba && (blanco.contains(arriba) || arriba.contains(blanco) || arriba.closest('label') === blanco || arriba === e || (e.id && arriba.closest('.subir-boton') && arriba.closest('.subir-boton').contains(e))));
            const enVista = bb.top >= -1 && bb.bottom <= innerHeight + 1;
            return { nombre: (blanco.textContent.trim().slice(0, 28) || blanco.getAttribute('aria-label') || e.id || e.tagName), aro, tapado, enVista, quien: arriba ? arriba.tagName + '.' + arriba.className : '' };
          });
          if (r) { vistos++; paradasTotales++; if (!r.aro) malos.push(r.nombre + ' sin aro'); if (r.tapado || !r.enVista) malos.push(r.nombre + ' tapado por ' + r.quien); }
          await q.keyboard.press('Tab');
        }
        decir(`  ${v} a ${ancho}: ${vistos} paradas; ` + (malos.length ? 'PROBLEMAS: ' + [...new Set(malos)].join(' | ') : 'todas con aro y a la vista'));
        if (malos.length) falla(`foco en ${v} a ${ancho}`);
      }
      await q.context().close();
    }

    decir(`  En total: ${paradasTotales} paradas`);

    // 7. Sin JavaScript
    decir('\n7. Sin JavaScript');
    const s = await abrir(b, 1280, 900, { javaScriptEnabled: false });
    for (const [ancla, visible, oculto] of [['', '#inicio', '#guia'], ['necesidades', '#necesidades', '#inicio'], ['inicio-donante', '#inicio-donante', '#inicio'], ['donaciones', '#donaciones', '#inicio'], ['detalle-problema', '#detalle-problema', '#inicio'], ['detalle-asignada', '#detalle-asignada', '#inicio'], ['entrega-problema', '#entrega-problema', '#inicio'], ['entregas', '#entregas', '#inicio'], ['inicio-organizacion', '#inicio-organizacion', '#inicio'], ['confirmada', '#confirmada', '#inicio'], ['g-botones', '#guia', '#inicio'], ['confirmar-error', '#confirmar-resumen', '#inicio']]) {
      await s.goto(URL + (ancla ? '#' + ancla : ''), { waitUntil: 'load' }); await s.waitForTimeout(250);
      const a = await s.locator(visible).isVisible(); const o = await s.locator(oculto).isVisible();
      decir(`  #${ancla || '(sin ancla)'}: ${visible} ${a ? 'se ve' : 'NO SE VE'}; ${oculto} ${o ? 'SE VE' : 'no se ve'}`); if (!a || o) falla('sin JavaScript: ' + ancla);
    }
    await s.goto(URL + '#confirmar', { waitUntil: 'load' }); const sinError = await s.locator('#confirmar-resumen').isVisible(); decir(`  #confirmar: el resumen de errores ${sinError ? 'SE VE' : 'no se ve'}`); if (sinError) falla('el error se ve en el estado normal');
    for (const [ancla, seVe, noSeVe] of [['inicio-organizacion', '#p-inicio-organizacion .boton--lleno', '#p-inicio-organizacion a[href="#confirmada"]'], ['inicio-organizacion-despues', '#p-inicio-organizacion a[href="#confirmada"]', '#p-inicio-organizacion .boton--lleno'], ['entregas', '#p-entregas .boton--lleno', '#p-entregas a[href="#confirmada"]'], ['entregas-despues', '#p-entregas a[href="#confirmada"]', '#p-entregas .boton--lleno']]) {
      await s.goto(URL + '#' + ancla, { waitUntil: 'load' }); await s.waitForTimeout(250);
      const a = await s.locator(seVe).isVisible(); const o = await s.locator(noSeVe).isVisible();
      decir(`  #${ancla}: «${ancla.endsWith('despues') ? 'Ver lo que quedó asentado' : 'Confirmar recepción'}» ${a ? 'se ve' : 'NO SE VE'}; lo del otro estado ${o ? 'SE VE' : 'no se ve'}`); if (!a || o) falla('sin JavaScript: el estado de ' + ancla);
    }
    for (const [ancla, seVe, noSeVe, rotulo] of [['necesidades', '#p-necesidades .boton--lleno', '#p-necesidades progress[value="25"]', 'la lámina de la entrega por confirmar'], ['necesidades-despues', '#p-necesidades progress[value="25"]', '#p-necesidades .boton--lleno', 'la necesidad de lentejas en 25 de 40 kg'], ['entrega-problema', '#p-entrega-problema .migas a[href="#entregas"]', '#p-entrega-problema .migas a[href="#entregas-despues"]', 'las migas a Entregas de las 18:30'], ['entrega-problema-despues', '#p-entrega-problema .migas a[href="#entregas-despues"]', '#p-entrega-problema .migas a[href="#entregas"]', 'las migas a Entregas de las 18:50']]) {
      await s.goto(URL + '#' + ancla, { waitUntil: 'load' }); await s.waitForTimeout(250);
      const a = await s.locator(seVe).isVisible(); const o = await s.locator(noSeVe).isVisible();
      decir(`  #${ancla}: ${rotulo} ${a ? 'se ve' : 'NO SE VE'}; lo del otro estado ${o ? 'SE VE' : 'no se ve'}`); if (!a || o) falla('sin JavaScript: el estado de ' + ancla);
    }
    const BARRA = (pant, a) => `#${pant} .barra-secciones a[href^="#${a}"]`;
    for (const [origen, selector, destino, rotulo] of [
      ['necesidades', BARRA('p-necesidades', 'entregas'), '#entregas', 'Entregas'], ['necesidades-despues', BARRA('p-necesidades', 'entregas'), '#entregas-despues', 'Entregas'],
      ['inicio-organizacion', BARRA('p-inicio-organizacion', 'entregas'), '#entregas', 'Entregas'], ['inicio-organizacion-despues', BARRA('p-inicio-organizacion', 'entregas'), '#entregas-despues', 'Entregas'],
      ['confirmar', BARRA('p-confirmar', 'entregas'), '#entregas', 'Entregas'], ['confirmada', BARRA('p-confirmada', 'entregas'), '#entregas-despues', 'Entregas'],
      ['inicio-organizacion', BARRA('p-inicio-organizacion', 'necesidades'), '#necesidades', 'Necesidades'], ['inicio-organizacion-despues', BARRA('p-inicio-organizacion', 'necesidades'), '#necesidades-despues', 'Necesidades'],
      ['entregas', BARRA('p-entregas', 'necesidades'), '#necesidades', 'Necesidades'], ['entregas-despues', BARRA('p-entregas', 'necesidades'), '#necesidades-despues', 'Necesidades'],
      ['entrega-problema', BARRA('p-entrega-problema', 'necesidades'), '#necesidades', 'Necesidades'], ['entrega-problema-despues', BARRA('p-entrega-problema', 'necesidades'), '#necesidades-despues', 'Necesidades'],
      ['confirmar', BARRA('p-confirmar', 'necesidades'), '#necesidades', 'Necesidades'], ['confirmada', BARRA('p-confirmada', 'necesidades'), '#necesidades-despues', 'Necesidades'],
      ['necesidades-despues', BARRA('p-necesidades', 'inicio-organizacion'), '#inicio-organizacion-despues', 'Inicio'], ['entrega-problema-despues', BARRA('p-entrega-problema', 'inicio-organizacion'), '#inicio-organizacion-despues', 'Inicio'],
      ['inicio-organizacion', '#p-inicio-organizacion .aviso-titulo a', '#entrega-problema', 'el aviso del aceite'], ['inicio-organizacion-despues', '#p-inicio-organizacion .aviso-titulo a', '#entrega-problema-despues', 'el aviso del aceite'],
      ['entregas', '#p-entregas a[href^="#entrega-problema"]', '#entrega-problema', '«Ver el detalle» del aceite'], ['entregas-despues', '#p-entregas a[href^="#entrega-problema"]', '#entrega-problema-despues', '«Ver el detalle» del aceite']]) {
      await s.goto(URL + '#' + origen, { waitUntil: 'load' }); await s.waitForTimeout(150);
      const visibles = await s.locator(selector + ':visible').evaluateAll(as => as.map(a => a.getAttribute('href')));
      decir(`  #${origen}: ${rotulo} lleva a ${visibles.join(', ') || 'NINGÚN LADO'}`); if (visibles.length !== 1 || visibles[0] !== destino) falla(`sin JavaScript, ${rotulo}, desde ${origen}, no abre ${destino}`);
    }
    await s.context().close();

    // 8. Movimiento reducido y tema oscuro
    const m = await abrir(b, 1280, 900, { reducedMotion: 'reduce', colorScheme: 'dark' }); await ir(m, 'guia');
    const mr = await m.evaluate(() => { const giro = getComputedStyle(document.querySelector('.boton[aria-busy="true"]'), '::before').animationName; const late = getComputedStyle(document.querySelector('.esqueleto i')).animationName; const dt = document.querySelector('.dt.muestra'); const c = getComputedStyle(dt); const campo = getComputedStyle(document.querySelector('.dt .entrada')); return { giro, late, fondo: c.backgroundColor, esquema: c.colorScheme, campo: campo.backgroundColor, lamina: getComputedStyle(document.body).backgroundColor }; });
    decir(`\n8. Movimiento reducido: el giro del botón es «${mr.giro}» y el latido del esqueleto es «${mr.late}». Tema oscuro: la lámina queda en ${mr.lamina}; el producto sigue en ${mr.fondo}, con color-scheme «${mr.esquema}» y campos en ${mr.campo}`);
    if (mr.giro !== 'none' || mr.late !== 'none') falla('movimiento reducido'); if (mr.esquema !== 'light') falla('el producto no fija color-scheme light');
    await m.context().close();
    const trazoDe = async (reducido, desdeElFormulario) => {
      const t = await abrir(b, 1280, 900, reducido ? { reducedMotion: 'reduce' } : {});
      if (desdeElFormulario) { await ir(t, 'confirmar-error'); await t.evaluate(() => document.querySelector('#form-confirmar').requestSubmit()); await t.waitForTimeout(150); } else { await ir(t, 'confirmada'); }
      const r = await t.evaluate(() => { const q = s => getComputedStyle(document.querySelector('#p-confirmada ' + s)).animationName; return { ancla: location.hash, activa: document.querySelector('.vista.activa').id, tramo: q('.pta-tramo'), nudo: q('.pta-nudo-5'), fruto: q('.pta-fruto-nuevo') }; });
      await t.context().close(); return r;
    };
    const tz1 = await trazoDe(false, true), tz2 = await trazoDe(false, false), tz3 = await trazoDe(true, true);
    decir(`   El trazo de Recepción confirmada: desde el formulario (queda en ${tz1.ancla}) las animaciones son «${tz1.tramo}», «${tz1.nudo}» y «${tz1.fruto}»; por su enlace, «${tz2.tramo}»; desde el formulario con movimiento reducido, «${tz3.tramo}», «${tz3.nudo}» y «${tz3.fruto}»`);
    if (tz1.activa !== 'confirmada' || tz1.tramo !== 'dt-trazo' || tz1.nudo !== 'dt-asoma' || tz1.fruto !== 'dt-brota') falla('el trazo no se dibuja al llegar desde el formulario');
    if (tz2.tramo !== 'none') falla('el trazo se dibuja al abrir la página por su enlace');
    if (tz3.tramo !== 'none' || tz3.nudo !== 'none' || tz3.fruto !== 'none') falla('el trazo no respeta el movimiento reducido');

    // 9. Lo que se dibuja dentro del producto sale solo de sus hojas: igual con y sin la hoja de la lámina, y en claro y en oscuro
    decir('\n9. El producto se dibuja solo con sus tokens: estilos calculados dentro de .dt, con y sin la hoja de la lámina, en tema claro y oscuro');
    const firmas = {};
    for (const tema of ['light', 'dark']) {
      const t = await abrir(b, 1400, 900, { colorScheme: tema }); await ir(t, 'portada');
      await t.addStyleTag({ content: '.vista{display:block!important}.marco-lienzo{width:1280px!important;zoom:1!important}' }); await t.waitForTimeout(150);
      const leer = () => t.evaluate(() => {
        const PROPS = ['color', 'backgroundColor', 'borderTopColor', 'fontFamily', 'fontWeight', 'fontStretch', 'lineHeight', 'letterSpacing', 'textDecorationColor', 'fill', 'stroke'];
        const salida = []; const mira = []; let enfocables = 0;
        document.querySelectorAll('.dt, .dt *').forEach(e => {
          const c = getComputedStyle(e); const fila = PROPS.map(p => c[p]);
          fila.push(e.closest('.vista--pantalla') ? c.fontSize : '');
          if (e.matches('a[href], button, input, select, textarea') && !e.disabled) { e.focus({ preventScroll: true }); if (document.activeElement === e) { const f = getComputedStyle(e); fila.push(f.outlineColor + ' ' + f.outlineStyle + ' ' + f.outlineWidth + ' ' + f.boxShadow); enfocables++; } }
          salida.push(e.tagName.toLowerCase() + (e.className && typeof e.className === 'string' ? '.' + e.className.split(' ')[0] : '') + '|' + fila.join('|'));
          mira.push(e.closest('.vista--pantalla') ? 'pantalla' : (e.matches('h1, h2, h3, h4, a, button, input, select, textarea, summary') && !e.closest('.muestra-angosta') ? 'muestra' : ''));
        });
        if (document.activeElement) document.activeElement.blur();
        return { salida, enfocables, mira };
      });
      const con = await leer();
      await t.evaluate(() => { document.getElementById('lamina').disabled = true; }); await t.waitForTimeout(100);
      const sin = await leer();
      const distintos = con.salida.map((x, i) => (con.mira[i] && x !== sin.salida[i] ? con.mira[i] + ' ' + x + '  ≠  ' + sin.salida[i] : null)).filter(Boolean);
      const enPantallas = con.mira.filter(m => m === 'pantalla').length, enMuestras = con.mira.filter(m => m === 'muestra').length;
      decir(`  tema ${tema === 'dark' ? 'oscuro' : 'claro'}: ${enPantallas} elementos de las trece pantallas, ${enMuestras} títulos, enlaces y controles de las muestras de la guía y ${con.enfocables} aros de foco; distintos con y sin la hoja de la lámina: ${distintos.length}` + (distintos.length ? ' · ' + distintos.slice(0, 4).join(' ;; ') : ''));
      if (distintos.length) falla(`la lámina cambia ${distintos.length} elementos del producto en tema ${tema}`);
      firmas[tema] = con.salida; await t.context().close();
    }
    const cambiaTema = firmas.light.filter((x, i) => x !== firmas.dark[i]);
    decir(`  entre tema claro y oscuro: de ${firmas.light.length} elementos del producto (pantallas y muestras), cambian ${cambiaTema.length}` + (cambiaTema.length ? ' · ' + cambiaTema.slice(0, 3).join(' ;; ') : ''));
    if (cambiaTema.length) falla('el producto cambia con el tema de quien mira');

    decir(`\nResultado: ${fallas === 0 ? 'sin fallas' : fallas + ' fallas'}`);
    fs.writeFileSync(path.join(aqui, 'verificacion.txt'), lineas.join('\n') + '\n');
  }
  await b.close();
  process.exit(fallas ? 1 : 0);
})().catch(e => { console.error('ERROR', e); process.exit(2); });
