# Modelado de Direcciones

- Status: proposed
- Date: 2026-05-21
- Deciders: Decisión Grupal

## Contexto y Problema

Decidir cómo modelar la ubicación geográfica dentro de las direcciones de personas y entidades beneficiarias.

## Atributos de Calidad y Drivers de Decisión

* Mantenibilidad
* Extensibilidad

## Alternativas Consideradas

* Dirección con datos geográficos como atributos simples
* Dirección compuesta por Localidad, Provincia y País

## Resultado de la Decisión

Alternativa elegida: "Dirección compuesta por Localidad, Provincia y País"

Justificación:
Inicialmente la dirección concentraba toda la información geográfica en una única clase. Se decidió separar el modelo en
una cadena de composición `Direccion -> Localidad -> Provincia -> Pais`, permitiendo representar mejor el dominio y
desacoplar la ubicación de los datos propios del domicilio.

### Consecuencias Positivas

* El modelo queda más expresivo.
* Permite reutilizar la estructura geográfica.

### Consecuencias Negativas

* Se agregan más clases y relaciones al modelo.

## Análisis de Alternativas

### Dirección con datos geográficos como atributos simples

La clase Dirección contiene localidad, provincia y país como atributos propios.

#### Pros

* Menor cantidad de clases.
* Implementación más simple.

#### Contras

* Mezcla información del domicilio con información geográfica.
* Dificulta extender el modelo.

### Dirección compuesta por Localidad, Provincia y País

La dirección referencia una Localidad, que referencia una Provincia y esta a un País.

#### Pros

* Separa responsabilidades.
* Representa mejor la estructura del dominio.

#### Contras

* Aumenta la cantidad de relaciones.