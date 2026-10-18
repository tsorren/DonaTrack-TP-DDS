# n8n — Workflows de DonaTrack

n8n es el motor de automatización que DonaTrack usa para difundir eventos de insignias y rankings en redes sociales (actualmente en modo mock). Corre como contenedor Docker y el `incentivos-service` se comunica con él vía webhooks HTTP.

---

## Configuración general

| Parámetro | Valor |
|-----------|-------|
| Puerto | `5678` |
| URL base | `http://localhost:5678` |
| Health check | `http://localhost:5678/healthz` |
| Modo de respuesta | Síncrono (`responseNode`) |

---

## Workflows disponibles

### 1. DonaTrack - Difusión de Insignias (`workflow-insignias.json`)

**ID:** `1` | **Estado:** `active`

**Webhook:**
- Método: `POST`
- Path: `insignia-ganada`
- URL completa: `http://localhost:5678/webhook/insignia-ganada`

**Payload esperado:**

```json
{
  "donanteId": "<UUID>",
  "user": "Nombre del donante",
  "badge": "Nombre de la insignia",
  "description": "Descripción de la insignia"
}
```

> Los campos `user` y `description` son opcionales: el workflow los defaultea a `"Donante #<donanteId>"` e `"Insignia obtenida en DonaTrack"` respectivamente.

**Pipeline de nodos:**

```
Webhook - Insignia Ganada
  → Construir Texto Publicable   (Code: formatea postText con hashtags)
  → Generar Imagen (Mock)        (Code: genera URL placeholder 800x400)
  → Publicar en Red Social (Mock)(HTTP Request → httpbin.org/post)
  → Confirmar Publicación        (Code: valida respuesta, genera status)
  → Responder al Backend         (RespondToWebhook: HTTP 200 + JSON)
```

**Respuesta al backend:**

```json
{
  "status": "ok",
  "publicado": true,
  "mensaje": "Insignia '<nombre>' publicada correctamente (mock)",
  "timestamp": "<ISO-8601>"
}
```

---

### 2. DonaTrack - Ranking Mensual (`workflow-ranking-mensual.json`)

**ID:** `2` | **Estado:** `active`

**Webhook:**
- Método: `POST`
- Path: `ranking-calculado`
- URL completa: `http://localhost:5678/webhook/ranking-calculado`

**Payload esperado:**

```json
{
  "periodo": "2026-05",
  "top3": [
    {
      "posicion": 1,
      "donanteId": "<UUID>",
      "nombre": "Nombre del donante",
      "misionesCompletadas": 42
    }
  ]
}
```

**Pipeline de nodos:**

```
Webhook - Ranking Calculado
  → Procesar Top 3               (Code: formatea entradas con medallas 🥇🥈🥉)
  → Construir Mensaje del Ranking(Code: genera postText con ranking formateado)
  → Publicar Ranking (Mock)      (HTTP Request → httpbin.org/post)
  → Registrar Resultado          (Code: valida resultado, genera status)
  → Responder al Backend         (RespondToWebhook: HTTP 200 + JSON)
```

**Respuesta al backend:**

```json
{
  "status": "ok",
  "periodo": "2026-05",
  "publicado": true,
  "top3": [...],
  "mensaje": "Ranking mensual 2026-05 publicado correctamente (mock)",
  "timestamp": "<ISO-8601>"
}
```

---

## Variables de entorno requeridas

Configuradas en `incentivos-service/src/main/resources/application.properties`:

| Propiedad Spring | Variable de entorno | Valor por defecto |
|-----------------|---------------------|-------------------|
| `n8n.webhook.insignia-url` | `N8N_INSIGNIA_WEBHOOK_URL` | `http://localhost:5678/webhook/insignia-ganada` |
| `n8n.webhook.ranking-url` | `N8N_RANKING_WEBHOOK_URL` | `http://localhost:5678/webhook/ranking-calculado` |

---

## Relación con `incentivos-service`

El adaptador [`N8nClientAdapter`](../incentivos-service/src/main/java/grupo5/incentivos/infrastructure/adapters/N8nClientAdapter.java) implementa la interfaz [`IN8nClient`](../incentivos-service/src/main/java/grupo5/incentivos/infrastructure/IN8nClient.java) y realiza llamadas **asíncronas** (reactivas con `WebClient`) a los webhooks de n8n.

```java
// Interface (IN8nClient.java)
void publicarInsigniaGanada(UUID donanteId, String nombreDonante,
                             String nombreInsignia, String descripcionInsignia);
void notificarRankingCalculado(String periodo, List<Map<String, Object>> top3);
```

- Las llamadas son **fire-and-forget**: los errores se loguean como `WARN` pero no propagan excepción al caller.
- El payload que envía el adaptador para insignias usa los campos `donanteId`, `user`, `badge`, `description`.
- El payload para ranking usa `periodo` y `top3` (lista de mapas con la estructura del `EntradaRanking`).

---

## Cómo importar los workflows

### Opción A — Interfaz web de n8n

1. Abrir `http://localhost:5678` en el navegador.
2. Ir a **Workflows → Import from file**.
3. Seleccionar `workflow-insignias.json` o `workflow-ranking-mensual.json`.
4. Activar el workflow con el toggle **Active**.

### Opción B — CLI (usado en CI/CD)

```bash
# Publicar workflows desde dentro del contenedor
docker compose exec n8n n8n publish:workflow --id=1
docker compose exec n8n n8n publish:workflow --id=2

# Reiniciar para aplicar los webhooks
docker compose restart n8n

# Verificar que n8n esté listo
curl -s -f http://localhost:5678/healthz
```

> Los IDs `1` y `2` corresponden a los campos `"id"` definidos en cada archivo JSON.

---

## Notas de producción

- **Red social (mock):** Actualmente ambos workflows envían a `https://httpbin.org/post` como simulación. En producción reemplazar el nodo `Publicar en Red Social (Mock)` / `Publicar Ranking (Mock)` por nodos de Twitter, LinkedIn o Instagram con credenciales reales.
- **Generación de imagen (mock):** El nodo `Generar Imagen (Mock)` en el workflow de insignias usa URLs de `placeholder.com`. En producción integrar DALL-E, Stable Diffusion u otra API de generación de imágenes.
