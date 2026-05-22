# Asignacion de Donaciones
- Status: proposed
- Date: 2026-05-21
- Deciders: Decisión Grupal

## Contexto y Problema
Decidir como gestionará el sistema los bienes donados para repartirlos eficazmente entre las entidades beneficiarias.

## Alternativas Consideradas
* Entidad StockBien
* ItemDonacion con cantidadDisponible

## Resultado de la Decisión

Alternativa elegida: "ItemDonacion con cantidadDisponible"

Justificación:
Facilita el cálculo de stock y nos permite obtener el donante si partimos de las donaciones y llegamos a el itemDonacion del que se descontó el stock

## Análisis de Alternativas

### Entidad StockBien

Crear clase que vincule subcategoría y la cantidad en stock correspondiente

#### Pros
* Facilita mucho más el proceso de asignación y gestión de stock disponible

#### Contras
* Deberíamos encontrar la manera de vincular el stock asignado con el donante

### ItemDonacion con cantidadDisponible

Agregar el atributo cantidadDisponible a la entidad ItemDonacion y referenciar la lista de donaciones independientes asociadas en las subcategorías

#### Pros
* Permite asociar al donante con la cantidad asignada a entidades partiendo de la donacion que tiene al donante

#### Contras
* Dificulta más el manejo de stock
