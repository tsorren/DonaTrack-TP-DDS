package grupo5.logistica.models.entities.rutas.eventos;

import grupo5.common.events.EventoDeDominio;

public abstract sealed class EventoRuta extends EventoDeDominio
    permits EventoRutaAsignada, EventoRutaIniciada {}
