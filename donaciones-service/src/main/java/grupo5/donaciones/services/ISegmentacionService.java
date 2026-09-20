package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;

public interface ISegmentacionService {
  void procesarDonacionNormalizada(DonacionNormalizada event);
}
