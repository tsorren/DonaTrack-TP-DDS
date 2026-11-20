package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.services.logistica.MotivoDescarteEvento;

/**
 * Hechos de negocio de Donaciones que se miden; quién los cuenta y cómo lo decide la
 * infraestructura.
 */
public interface IMetricasDonaciones {

  void donacionRegistrada(Donacion donacion);

  void entregaFallida(boolean replanificable);

  void eventoDeProveedorDescartado(MotivoDescarteEvento motivo);
}
