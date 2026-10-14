# Donante y DonacionEvento tienen representación propia en el servicio de incentivos
- Status: accepted
- Date: 2026-06-18
- Deciders: Decisión Grupal

## Contexto y Problema
El servicio de incentivos necesita procesar información de donantes y donaciones,
pero debe mantenerse desacoplado de los demás servicios del sistema.

## Alternativas Consideradas
* Clases propias de Donante y DonacionEvento en el servicio de incentivos
* El servicio de incentivos accede directamente a los otros servicios

## Resultado de la Decisión

Alternativa elegida: "Clases propias de Donante y DonacionEvento en el servicio de incentivos"

Justificación:
Mantener independencia entre servicios es prioritario para la mantenibilidad y resiliencia del sistema. El costo de sincronización es aceptable frente al acoplamiento que generaría la alternativa descartada.

### Consecuencias Positivas
* El servicio de incentivos puede operar y evolucionar de forma independiente sin verse afectado por cambios en otros servicios

### Consecuencias Negativas
* Al registrar un donante o procesar una donación se debe notificar al servicio de incentivos, agregando una responsabilidad extra al flujo

## Análisis de Alternativas

### Clases propias de Donante y DonacionEvento en el servicio de incentivos

El servicio de incentivos define sus propias representaciones de Donante y DonacionEvento, recibiendo la info desde los otros servicios al momento del registro/evento

#### Pros
* El servicio de incentivos es completamente autónomo
* Cambios en otros servicios no impactan directamente en incentivos

#### Contras
* Existe duplicación de datos entre servicios
* Requiere sincronización al registrar un donante o una donación

### El servicio de incentivos accede directamente a los otros servicios

Incentivos consulta al servicio de donaciones cuando necesita información

#### Pros
* Sin duplicación de datos

#### Contras
* Genera acoplamiento entre servicios
* Un fallo en otro servicio afecta directamente a incentivos
