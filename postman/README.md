# DonaTrack — Colecciones Postman

> Formato: Postman Collection v2.1.0  
> Todas las colecciones son **autocontenidas** (crean su propio setup de datos) salvo que se indique lo contrario.

---

## Mapa de servicios

| Servicio              | Puerto      | Variable de colección |
|-----------------------|-------------|-----------------------|
| `donaciones-service`  | `8080` [OBSERVED] | `baseUrl` / `donacionesUrl` |
| `notificaciones-service` | `8081` [OBSERVED] | `baseUrl` / `notificacionesUrl` |
| `incentivos-service`  | `8082` [OBSERVED] | `baseUrl` / `incentivosUrl` |
| `logistica-service`   | `8083` [OBSERVED] | `baseUrl` / `logisticaUrl` |

---

## Colecciones disponibles

| # | Archivo | Requests | Servicio(s) | Descripción |
|---|---------|----------|-------------|-------------|
| 1 | `flujo-1-catalogo-personas.json` | 8 | donaciones (8080) | Crea Categoría → Subcategoría → Persona Humana (donante) → Donante → Persona Jurídica → Entidad Beneficiaria |
| 2 | `flujo-2-necesidades.json` | 11 | donaciones (8080) | Crea Necesidad Extraordinaria y Recurrente; documenta bug conocido (RECURRENTE sin fechas → 500 en lugar de 400) |
| 3 | `flujo-3-donacion-normalizacion-segmentacion.json` | 12 | donaciones (8080) | Normalización auto-semántica vs. resolución manual; normalización y segmentación son **asíncronas** |
| 4 | `flujo-4-matching-asignacion-estados.json` | 30 | donaciones (8080) | Matching, aprobación de propuesta y **7 transiciones** completas de la máquina de estados de `DonacionIndependiente` |
| 5 | `flujo-5-misiones-insignias-ranking.json` | 40 | incentivos (8082) | 6 misiones/insignias en orden obligatorio; progresión COLABORADOR → SOSTENEDOR → TRANSFORMADOR; IDs con `{{$guid}}` |
| 6 | `flujo-6-logistica-completo.json` | 26 | logistica (8083) | Ciclo de vida de Camión y Chofer; creación de Entrega; rutas solo nacen del scheduler (2 AM por defecto) |
| 7 | `flujo-7-notificaciones-eventos.json` | 22 | notificaciones (8081) | Sincroniza 3 réplicas de persona; dispara los 8 tipos de evento reales; cierra con anonimización |
| 8 | `flujo-8-e2e-distribuido.json` | 21 | **todos (8080–8083)** | Smoke test E2E: Donación → Matching → Logística → Incentivos → Notificaciones |

**Total: 170 requests** distribuidas en 8 colecciones.

---

## Variables de colección

Cada colección declara sus propias variables (`variable` en la raíz del JSON). **No se requiere un environment global** para los flujos 1–7.  
El flujo 8 usa cuatro variables de URL separadas:

```
donacionesUrl     = http://localhost:8080
notificacionesUrl = http://localhost:8081
incentivosUrl     = http://localhost:8082
logisticaUrl      = http://localhost:8083
```

### IDs generados automáticamente (flujos 5 y 7)

Los flujos 5 y 7 tienen un pre-request script global que genera IDs con `{{$guid}}` si aún no existen, permitiendo correr la colección varias veces sin colisiones:

```javascript
// flujo-5 [OBSERVED]
if (!pm.collectionVariables.get('donanteId')) {
  pm.collectionVariables.set('donanteId', pm.variables.replaceIn('{{$guid}}'));
}
```

### IDs propagados por test scripts

Los flujos 1–4, 6 y 8 capturan IDs de respuesta en test scripts y los pasan a variables de colección:

```javascript
// ejemplo flujo-1 [OBSERVED]
var json = pm.response.json();
pm.collectionVariables.set('categoriaId', json.id);
pm.test('Status 201 Created', function () { pm.response.to.have.status(201); });
```

---

## Autenticación

Ninguna colección usa autenticación. Todos los endpoints son públicos (sin cabecera `Authorization`). [OBSERVED]

---

## Orden de ejecución recomendado

### Tests unitarios por dominio (paralelo posible)

```
flujo-1  → base de datos mínima para reutilizar manualmente con flujos 2-4
flujo-2  → autocontenido, corre en paralelo con flujo-3
flujo-3  → autocontenido, corre en paralelo con flujo-2
flujo-4  → autocontenido, más largo (30 requests)
flujo-5  → independiente, solo toca incentivos-service
flujo-6  → independiente, solo toca logistica-service
flujo-7  → independiente, solo toca notificaciones-service
```

### Smoke test completo (secuencial)

```
flujo-8  → requiere los 4 servicios corriendo simultáneamente
```

> **Nota:** Si querés IDs frescos para testear flujos 2–4 a mano, corré **flujo-1 primero** y reutilizá los IDs. Los flujos 2–4 son autocontenidos pero crean sus propios datos independientes.

---

## Comportamientos asíncronos conocidos

| Flujo | Operación asíncrona | Acción requerida |
|-------|--------------------|--------------------|
| 3 | Normalización y segmentación de ítems de donación | Reintentar el GET de estado manualmente si aún no cambió |
| 6 | Creación de Ruta (solo vía scheduler `PlanificadorDeEntregas`, cron 2 AM) | No existe `POST /api/rutas`; las transiciones `EN_TRASLADO`/`ENTREGADA` de Entrega requieren una Ruta real preexistente |

---

## Notas de comportamiento por servicio

- **donaciones-service (8080):** errores de negocio devuelven `400` con body descriptivo. Bug documentado en flujo-2: `RECURRENTE` sin `fechaInicio`/`fechaFin` devuelve `500` (NPE no controlada). [OBSERVED]
- **notificaciones-service (8081):** "recurso no encontrado" siempre es `400`, nunca `404`. [OBSERVED]
- **incentivos-service (8082):** categoría inicial del donante es `COLABORADOR`; ascensos a `SOSTENEDOR` y `TRANSFORMADOR` se disparan por cumplimiento de misiones en orden estricto. [OBSERVED]
- **logistica-service (8083):** Camión nace en estado `DISPONIBLE`. Estados camión: `DISPONIBLE | EN_RUTA | DESHABILITADO` [OBSERVED]. Estados entrega: `PENDIENTE | EN_TRASLADO | ENTREGADA | NO_RECIBIDA | REVISION` [OBSERVED].

---

## Ejecutar con Newman CLI

### Requisito

```bash
npm install -g newman
```

### Correr una colección individual

```bash
newman run flujo-1-catalogo-personas.json
```

### Correr con timeout y reporte HTML

```bash
newman run flujo-4-matching-asignacion-estados.json \
  --timeout-request 10000 \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export results/flujo-4.html
```

### Correr el E2E con URLs personalizadas

```bash
newman run flujo-8-e2e-distribuido.json \
  --env-var "donacionesUrl=http://localhost:8080" \
  --env-var "notificacionesUrl=http://localhost:8081" \
  --env-var "incentivosUrl=http://localhost:8082" \
  --env-var "logisticaUrl=http://localhost:8083"
```

### Correr todos los flujos en secuencia (bash)

```bash
for f in flujo-{1..8}-*.json; do
  echo "=== Corriendo $f ===";
  newman run "$f" --timeout-request 10000;
done
```

### Correr todos los flujos en secuencia (PowerShell)

```powershell
Get-ChildItem flujo-*.json | ForEach-Object {
  Write-Host "=== Corriendo $($_.Name) ===" -ForegroundColor Cyan
  newman run $_.FullName --timeout-request 10000
}
```

---

## Estructura de archivos

```
postman/
├── README.md
├── flujo-1-catalogo-personas.json                    #  8 requests — donaciones-service
├── flujo-2-necesidades.json                          # 11 requests — donaciones-service
├── flujo-3-donacion-normalizacion-segmentacion.json  # 12 requests — donaciones-service
├── flujo-4-matching-asignacion-estados.json          # 30 requests — donaciones-service
├── flujo-5-misiones-insignias-ranking.json           # 40 requests — incentivos-service
├── flujo-6-logistica-completo.json                   # 26 requests — logistica-service
├── flujo-7-notificaciones-eventos.json               # 22 requests — notificaciones-service
└── flujo-8-e2e-distribuido.json                      # 21 requests — todos los servicios
```
