# Base de conocimientos de arquitectura

Bienvenido a la base de conocimientos de arquitectura de DonaTrack. En este espacio se centralizan todas las decisiones
tecnicas que definen la estructura de nuestro sistema.

[Volver al Hub](..)

## Proposito

Un Registro de Decision de Arquitectura (ADR) es un documento que describe una eleccion de diseño importante para
resolver un problema especifico. Estos registros son fundamentales para:

- Mantener un historial transparente de por que se eligio una tecnologia o estrategia sobre otra.
- Evitar repetir errores del pasado al entender el contexto en el que se tomo cada decision.
- Facilitar la incorporacion de nuevos integrantes al equipo.

Los ADR son documentos permanentes. Si una decision cambia en el futuro, no se borra el registro original, sino que se
crea uno nuevo que lo reemplaza, manteniendo asi la trazabilidad de la evolucion del software.

## Herramientas de Documentacion

Para gestionar este conocimiento, el equipo utiliza dos herramientas principales:

1. [Documentador](../documentador/): Una aplicacion web personalizada que facilita la redaccion de estos
   documentos siguiendo el formato requerido.
2. [Display de Decisiones (Log4brains)](../adr-preview/): Esta interfaz interactiva que permite navegar por todos los
   registros de forma organizada.

## Flujo de Trabajo

El proceso para documentar una decision es el siguiente:

1. Identificacion: Se detecta una necesidad tecnica o un problema de diseño a traves de un Issue de Requerimiento en
   GitHub.
2. Propuesta y Analisis: Se utiliza el Documentador para evaluar alternativas, ventajas y desventajas.
3. Revision: La decision se sube al repositorio y el equipo la valida.
4. Publicacion: Una vez aceptada, el sistema actualiza automaticamente este sitio mediante GitHub Actions.

## Uso del Sitio

Este sitio se actualiza automaticamente cada vez que se suben cambios a las ramas principales o de entrega del proyecto.
Podes utilizar el buscador o el menu lateral para explorar las decisiones filtradas por cada servicio del sistema como
Donaciones, Logistica o Notificaciones.

## Enlaces Utiles

- [Repositorio del proyecto en GitHub](https://github.com/tsorren/DonaTrack-TP-DDS)
- [Documentacion oficial de Log4brains](https://github.com/thomvaill/log4brains)
- [Formatos de registro de decisiones (MADR)](https://adr.github.io/madr/)