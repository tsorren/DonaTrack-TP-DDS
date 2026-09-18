package grupo5.logistica.models.entities.entregas.eventos;

import grupo5.common.events.EventoDeDominio;

public abstract sealed class EventoEntrega extends EventoDeDominio
    permits EntregaConfirmada, EntregaFallida {}
