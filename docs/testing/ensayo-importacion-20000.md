# Ensayo de importación de 20.000 donantes

> Origen: ítem R11-03 del [backlog de la revisión del 2026-11-11](../interfaz/backlog-revision-2026-11-11.md).
> Generador: [`demo/ensayo-importacion/`](../../demo/ensayo-importacion/). Importador:
> `ImportadorService` en Donaciones.

La demo importa un CSV de 20.000 filas en vivo (RF-20). Nadie midió cuánto tarda ni cuánto frena al
resto del sitio. `[OBSERVED]` El importador lee el archivo entero, trabaja fila a fila y publica dos
eventos por donante nuevo. `[INFERRED]` Eso puede cargar a Donaciones y a Notificaciones, pero hay
que medirlo. Esta guía dice cómo hacerlo y qué resultado alcanza. El ensayo lo corre el cierre de la
oleada sobre el stack propio; el generador y la guía quedan listos antes.

## Qué se prueba

| Pregunta | Qué se mira |
|---|---|
| ¿Cuánto tarda la importación? | Segundos desde que se sube el archivo hasta que el estado deja de ser `PENDIENTE` o `PROCESANDO`. |
| ¿Cuenta bien? | Total, creadas, actualizadas y filas con error contra lo que el generador anuncia. |
| ¿El resto del sitio responde mientras importa? | Tiempo de respuesta de páginas y consultas de otras pantallas, medido cada segundo durante la importación. |
| ¿Aguanta la memoria? | Memoria de los contenedores de Donaciones y Notificaciones, que no se reinicien ni se queden sin memoria. |
| ¿Notificaciones se satura? | Mensajes pendientes en RabbitMQ y cuánto tardan en vaciarse. |

## Qué trae el archivo

El generador arma un CSV con 20.000 filas de datos, todas ficticias. Con los valores por defecto:

| Clase de fila | Cantidad | Qué debería pasar |
|---|---|---|
| Donantes nuevos (85 % humanos, 15 % empresas) | 19.400 | Se crean. |
| Duplicados: mismo documento y mismo correo que una fila anterior, con otro teléfono | 400 | Se cuentan como actualizados. |
| Con error de formato: una columna de menos o de más | 133 | Se cuentan con error, motivo `FORMATO_INVALIDO`. |
| Con error de correo: el correo de otro donante y un documento distinto | 67 | Se cuentan con error, motivo `PERSONA_DUPLICADA_EN_CONFLICTO`. |

Los datos son sintéticos: correos `@ensayo.donatrack.test`, DNI que empiezan con 88 y CUIT con 3088,
teléfonos `+54 11 5550-…`. La serie 90 es la del [seeder de demo](demo-seeder.md), así que el ensayo
no se cruza con la historia cargada. El archivo trae la marca BOM de UTF-8, como los que baja Excel.

## Antes de empezar

1. Stack propio levantado y sano (`docker compose ps`), con el seeder cargado si se quiere ensayar
   en las condiciones de la demo.
2. Ninguna importación en curso. En RabbitMQ (`http://localhost:15672`, pestaña Queues) las colas
   están en 0 pendientes.
3. Si la base ya tiene donantes de la serie 88 (un ensayo anterior), generar el archivo con otra
   `--serie`. Si no, todas las filas contarían como actualizadas y el resultado no sería comparable.
4. Si la seguridad está prendida, un token de administración para la API (se pide por el mismo
   camino que usa el cliente liviano; no se anota en ningún archivo).

## Cómo se corre

### 1. Generar el archivo

```bash
cd demo/ensayo-importacion
python3 generar_csv.py --salida /tmp/donantes-ensayo-20000.csv
```

Solo usa la biblioteca estándar de Python 3.9 o más nuevo. Con la misma `--semilla` (2026 por
defecto) sale el mismo archivo, byte a byte. Al terminar imprime los números esperados. Otros
valores: `--filas`, `--duplicados`, `--con-error`, `--serie`, `--sin-bom`. El archivo pesa unos
2 MB, muy por debajo del tope de 50 MB de Donaciones.

### 2. Dejar la medición corriendo

En una terminal aparte, antes de subir el archivo. Mide cada segundo si el resto del sitio
responde y guarda el resultado en un archivo para leerlo después:

```bash
while true; do
  for url in http://localhost:8090/ http://localhost:8086/api/necesidades/publicas \
             http://localhost:8080/actuator/health; do
    curl -s -o /dev/null --max-time 10 -w "$(date +%H:%M:%S) %{http_code} %{time_total} $url\n" "$url"
  done
  sleep 1
done | tee /tmp/ensayo-respuesta.log
```

Las tres direcciones son la portada del cliente liviano (8090), una consulta pública que pasa por
el gateway (8086) y la salud de Donaciones (8080). Si el cliente liviano corre en otro puerto,
cambiar la primera. En otra terminal, la memoria:

```bash
while true; do
  docker stats --no-stream --format "$(date +%H:%M:%S) {{.Name}} {{.MemUsage}} {{.CPUPerc}}" \
    | grep -E "donaciones-service|notificaciones-service|incentivos-service|rabbitmq"
  sleep 5
done | tee /tmp/ensayo-memoria.log
```

### 3. Subir el archivo y medir la importación

Por la API, que es lo que mide el importador sin la pantalla en el medio. Sin seguridad:

```bash
inicio=$(date +%s)
id=$(curl -s -F "archivo=@/tmp/donantes-ensayo-20000.csv" http://localhost:8086/api/donantes/archivos \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
while true; do
  curl -s "http://localhost:8086/api/donantes/archivos/$id" | tee /tmp/ensayo-estado.json
  echo
  grep -q '"estado":"PROCESADO\|"estado":"ERROR"' /tmp/ensayo-estado.json && break
  sleep 2
done
echo "Duró $(( $(date +%s) - inicio )) segundos"
```

Con la seguridad prendida se agrega `-H "Authorization: Bearer $TOKEN"` a las dos llamadas de
`curl` de la API. Mientras corre, el estado trae `totalFilas`, `procesadas`, `creadas`,
`actualizadas` y `conError`. El avance se guarda cada 500 filas.

Para mirar el sitio con ojos humanos mientras importa, abrir en el navegador el inicio del
administrador y la lista de necesidades y navegar. Anotar si algo tarda o se traba.

### 4. Después de terminar

- Mensajes pendientes en RabbitMQ (`http://localhost:15672`, pestaña Queues): anotar el pico de
  *Ready* y *Unacked*, y cuánto tarda cada cola en volver a 0.
- Reinicios y memoria agotada: `docker inspect -f '{{.Name}} reinicios={{.RestartCount}} sin_memoria={{.State.OOMKilled}}' $(docker ps -q)`.
- Errores en los registros de Donaciones y Notificaciones:
  `docker compose logs --since 30m donaciones-service notificaciones-service | grep -ci "OutOfMemory\|Exception"`
  (anotar la cantidad y mirar las que no sean las 200 filas con error esperadas).
- Las filas con error: `GET /api/donantes/archivos/{id}/filas-con-error?page=0&size=50` (se pide de
  a una página). Tienen que ser 200 en total (4 páginas de 50), con los dos motivos de la tabla.

## Qué resultado se considera suficiente

`[PROPOSED]` Criterio de esta guía, no fijado por la cátedra. Si falla alguno, se anota cuál y se
sube R11-B08 (importador por lotes) a la entrega 6.

| # | Qué | Suficiente cuando |
|---|---|---|
| 1 | Cuenta | Termina en `PROCESADO_CON_ERRORES` (con filas con error, como acá) con `totalFilas` 20.000, `creadas` 19.400, `actualizadas` 400 y `conError` 200 (con los valores por defecto). |
| 2 | Duración | Hasta 5 minutos. Entre 5 y 10 minutos sirve si el avance se ve moviéndose en pantalla. Más de 10 minutos no sirve para una demo en vivo. |
| 3 | El sitio responde | Durante la importación, ninguna respuesta con error ni de más de 5 segundos, y el 95 % de las respuestas en menos de 1 segundo. |
| 4 | Memoria | Ningún contenedor se reinicia ni queda sin memoria, no aparece `OutOfMemoryError` en los registros y el pico de Donaciones se mantiene por debajo de 700 MiB (hoy su montón tiene tope de 384 MiB). |
| 5 | Notificaciones | Las colas de RabbitMQ vuelven a 0 sin intervención, nada queda en colas de mensajes muertos y Notificaciones sigue respondiendo su salud. |

Cómo sacar los números 2 y 3 del registro de respuestas, tomando solo las líneas entre el inicio y
el fin de la importación (las horas están en la primera columna):

```bash
awk '$1 >= "HH:MM:SS" && $1 <= "HH:MM:SS"' /tmp/ensayo-respuesta.log \
  | awk '{ print $3 }' | sort -n | awk '{ t[NR] = $1 } END { print "pedidos:", NR, " p95:", t[int(NR*0.95)], " máximo:", t[NR] }'
awk '$2 != 200' /tmp/ensayo-respuesta.log   # respuestas con error
```

## Qué se anota

Una tabla con una fila por corrida, en el cierre de la oleada:

| Dato | Valor |
|---|---|
| Fecha, rama y commit | |
| Valores del generador (filas, duplicados, errores, semilla, serie) | |
| Duración de la importación | |
| Total, creadas, actualizadas y con error | |
| Tiempo de respuesta: p95 y máximo durante la importación | |
| Pico de memoria de Donaciones y de Notificaciones | |
| Pico de mensajes pendientes en RabbitMQ y tiempo hasta vaciarse | |
| Cuáles de los cinco criterios se cumplieron | |
| Decisión: alcanza, o sube R11-B08 a la entrega 6 | |

Si no alcanza, el ítem R11-B08 trae el arreglo: importador por lotes, lectura del CSV sin cargarlo
entero en memoria y avisos de bienvenida que no saturen Notificaciones.

## Para repetir o ajustar

- Otra carga: `--filas 5000 --duplicados 100 --con-error 50` deja un ensayo corto para probar la
  medición antes del de 20.000.
- Mismo archivo para comparar antes y después de un cambio: misma `--semilla` y misma `--serie`, con
  la base de donantes de esa serie limpia.
- Pruebas del generador: `cd demo/ensayo-importacion && python3 -m unittest discover -s .`
