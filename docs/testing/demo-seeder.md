# Seeder de demo

> Especificación: [SPEC-10](../specs/active/SPEC-10-seeder-de-demo.md). Código: [`demo/seeder/`](../../demo/seeder/).
> Overlay: [`docker-compose.seed.yml`](../../docker-compose.seed.yml).

Carga seis meses de uso en todos los servicios (donantes, entidades, necesidades, donaciones
entregadas con fotos, rutas cerradas, misiones, insignias, rankings y avisos). También arma lo que
pasa hoy: un camión en ruta, una entrega en la puerta, una entrega fallida y pendientes para el
administrador. Todos los datos son sintéticos: correos `@demo.donatrack.test`, documentos de la
serie 90…, teléfonos 11 5555 … y fotos dibujadas por el seeder.

## Cómo se usa

```bash
# 1. Base vacía y stack completo levantado (la seguridad puede estar prendida o apagada)
docker compose down -v
docker compose up -d --build

# 2. Seis meses de historia más lo que pasa hoy (~3 minutos)
DEMO_PASSWORD='una-clave-de-10+' docker compose -f docker-compose.yml -f docker-compose.seed.yml run --rm seeder

# 3. Justo antes de presentar: vuelve a sacar un camión a la calle (hasta dos veces después de
#    `todo`, una por camión libre; cada vez quedan menos necesidades abiertas)
DEMO_PASSWORD='una-clave-de-10+' docker compose -f docker-compose.yml -f docker-compose.seed.yml run --rm seeder vivo
```

`DEMO_PASSWORD` es la clave común de las cuentas de la demo: de 10 a 128 caracteres y sin `@`.
Las demás credenciales salen del `.env` del stack: `KC_BOOTSTRAP_ADMIN_*`, `AUTH_ADMIN_*`,
`POSTGRES_PASSWORD`, `LOGISTICA_MONGO_*` y `LOGISTICA_MINIO_PASSWORD`.

Al final se imprime la tabla de cuentas. Los protagonistas son:

| Rol | Cuenta | Para mostrar |
|---|---|---|
| Donante estrella | `lucia.fernandez@demo.donatrack.test` | Categoría SOSTENEDOR, insignias, constancia 5 de 6, historial completo |
| Donante constante | `martin.gomez@demo.donatrack.test` | Donaciones mes a mes |
| Empresa donante | `donaciones@distribuidora-sur.demo.donatrack.test` | Donaciones sin mostrar el nombre |
| Donante nuevo | `sofia.ruiz@demo.donatrack.test` | Alta de hoy, primera donación con propuestas pendientes |
| Donante inactivo | `carlos.pereyra@demo.donatrack.test` | Aviso "Te extrañamos" |
| Entidad protagonista | `comedor.lospibes@demo.donatrack.test` | Necesidades, entregas con fotos y reporte, el camión en la puerta |
| Entidad | `escuela12@demo.donatrack.test` | La entrega fallida de hoy |
| Entidad pendiente | `hogar.esperanza@demo.donatrack.test` | Solicitud para que la apruebe el administrador |
| Entidad dada de baja | `fundacion.horizonte@demo.donatrack.test` | Baja con su historia |
| Administración | `AUTH_ADMIN_EMAIL` | Propuestas, bien sin reconocer, mensajes de contacto, flota |

## Qué hace, en orden

1. Espera a que estén sanos Keycloak y los cuatro servicios.
2. Crea en Keycloak (en el realm que ya corre, no en el del repositorio) un cliente temporal
   `demo-seeder` con clave aleatoria. Lo usa para pedir tokens de cada cuenta con usuario y clave,
   y lo borra al terminar, aunque el seeder falle.
3. Si la base ya tiene la demo, se detiene: `todo` necesita la base vacía.
4. **Por API**, igual que el sitio: completa el catálogo (8 categorías), crea personas, donantes,
   entidades, legales y cuentas, y da de alta la flota. Así Incentivos y Notificaciones reciben sus
   eventos y crean sus propias copias.
5. **Directo en las bases**, porque ninguna API acepta fechas pasadas, escribe la historia de seis
   meses con el formato que deja cada servicio:
   - Donaciones (Postgres): donaciones, segmentación, necesidades con sus períodos, estados encadenados, propuestas y fotos.
   - Logística (Mongo): rutas cerradas, entregas y eventos.
   - Incentivos: misiones, insignias, ascensos y rankings mensuales.
   - Notificaciones: avisos leídos y sin leer, y mensajes de contacto.
   - MinIO: las fotos de recepción.
6. **Por API**, lo de hoy: la donación de Lucía pasa por el algoritmo y el administrador la aprueba.
   Con eso se arma una ruta que sale en el camión, la segunda parada queda como entrega fallida y
   la primera queda EN_PUERTA para confirmarla en vivo como el comedor. También deja la donación
   de Sofía con propuestas sin revisar y un bien que no se reconoce, la baja de una necesidad y la
   de una entidad, un CSV de donantes y dos mensajes de contacto.

## Lo que el seeder suple, y por qué

- **Coordenadas:** Logística no geocodifica ([DTI-15](../adr/DEUDA_TECNICA.md#dti-15)). El seeder
  pone en cada entrega el punto de la sede, que es lo que haría el geocodificador.
- **Ruta:** no hay alta de rutas por API (la planificación corre a las 02:00). El seeder crea la ruta
  vacía en Mongo y le suma las entregas por la API, que es la que valida.
- **Propuestas que llegan a la ruta:** Donaciones corre dos algoritmos y se queda con las
  propuestas en las que coinciden. Cada uno arma como máximo 10, y el segundo pone primero a las
  entidades menos atendidas. Por eso el guion reserva tres subcategorías para la ruta de hoy
  (`RESERVADAS_VIVO`) y deja en el depósito stock que compite por pocas necesidades
  (`STOCK_CON_CANDIDATAS`).
- **Orden de los eventos:** el seeder espera a que Donaciones marque cada donación como lista para
  entregar antes de arrancar la ruta (ver el hallazgo 5 de SPEC-10).

## Desarrollo

```bash
cd demo/seeder
python3 -m unittest discover -s tests -t .   # guion determinista, catálogo y coherencia
```

El guion (`seeder/guion.py`) es puro: con la misma fecha y semilla (`SEEDER_SEMILLA`, 2026 por defecto)
arma el mismo mundo. Los escritores viven en `historia.py` y lo de hoy, en `vivo.py`.
