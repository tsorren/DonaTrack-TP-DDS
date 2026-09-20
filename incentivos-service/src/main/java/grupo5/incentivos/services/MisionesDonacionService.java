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
import grupo5.incentivos.services.mappers.MisionMapper;
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
  private final MisionMapper misionMapper;

  public MisionesDonacionService(
      IDonanteIncentivosRepository repository,
      ApplicationEventPublisher eventPublisher,
      MisionMapper misionMapper) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.misionMapper = misionMapper;
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
    DonanteIncentivos donante = obtenerDonante(donanteId);
    return donante.getMisiones().stream()
        .sorted(
            Comparator.comparing(
                Mision::getNumeroMision, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(mision -> misionMapper.toResponseDTO(mision, donante))
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
