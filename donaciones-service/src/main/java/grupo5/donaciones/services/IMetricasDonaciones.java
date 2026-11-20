package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donaciones.Donacion;

/**
 * Hechos de negocio de Donaciones que se miden; quién los cuenta y cómo lo decide la
 * infraestructura.
 */
public interface IMetricasDonaciones {

  void donacionRegistrada(Donacion donacion);
}
