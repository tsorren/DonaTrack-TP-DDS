package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class MisionesDonacionService implements IMisionesDonacionService {

  private final IDonanteIncentivosRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public MisionesDonacionService(
      IDonanteIncentivosRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void procesarDonacion(NuevaDonacionRequest request) {
    EventoDonacion evento =
        EventoDonacion.builder()
            .categorias(request.categorias())
            .cantidadBienes(request.cantidadBienes())
            .fecha(request.fecha())
            .build();

    DonanteIncentivos donante = obtenerDonante(request.donanteId());

    donante.registrarDonacion(evento);
    despacharEventosYGuardar(donante);
  }

  @Override
  public void procesarDonacionExitosa(DonacionExitosaRequest request) {
    DonanteIncentivos donante = obtenerDonante(request.donanteId());
    donante.registrarDonacionExitosa(request.organizacionId());
    despacharEventosYGuardar(donante);
  }

  @Override
  public List<MisionDTO> obtenerMisiones(UUID donanteId) {
    return obtenerDonante(donanteId).getMisiones().stream()
        .sorted(
            Comparator.comparing(
                Mision::getNumeroMision, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(MisionDTO::desde)
        .toList();
  }

  @Override
  public void verificarRachasVencidas(YearMonth mesActual) {
    List<DonanteIncentivos> todos = repository.findAll();
    todos.forEach(donante -> donante.verificarRachas(mesActual));
    repository.saveAll(todos);
  }

  private DonanteIncentivos obtenerDonante(UUID donanteId) {
    return repository
        .findById(donanteId)
        .orElseThrow(
            () -> new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO));
  }

  private void despacharEventosYGuardar(DonanteIncentivos donante) {
    repository.save(donante);
    donante.getDomainEvents().forEach(eventPublisher::publishEvent);
    donante.clearDomainEvents();
  }
}
