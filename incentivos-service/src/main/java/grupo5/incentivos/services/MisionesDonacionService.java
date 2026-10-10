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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

@Service
public class MisionesDonacionService implements IMisionesDonacionService {

  private static final Logger log = LoggerFactory.getLogger(MisionesDonacionService.class);

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

    ReintentoPorConcurrencia.ejecutar(
        "donación del donante " + request.donanteId(),
        () -> {
          DonanteIncentivos donante = obtenerDonante(request.donanteId());
          donante.registrarDonacion(evento);
          despacharEventosYGuardar(donante);
        });
  }

  @Override
  public void procesarDonacionExitosa(DonacionExitosaRequest request) {
    ReintentoPorConcurrencia.ejecutar(
        "donación exitosa del donante " + request.donanteId(),
        () -> {
          DonanteIncentivos donante = obtenerDonante(request.donanteId());
          donante.registrarDonacionExitosa(request.organizacionId());
          despacharEventosYGuardar(donante);
        });
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
    // ponytail: relee cada donante para poder reintentar solo; con muchos donantes, usar el ya
    // cargado en el primer intento y releer únicamente ante un choque.
    for (DonanteIncentivos cargado : repository.findAll()) {
      UUID donanteId = cargado.getId();
      try {
        ReintentoPorConcurrencia.ejecutar(
            "rachas del donante " + donanteId,
            () ->
                repository
                    .findById(donanteId)
                    .ifPresent(
                        donante -> {
                          donante.verificarRachas(mesActual);
                          repository.save(donante);
                        }));
      } catch (ConcurrencyFailureException e) {
        // El job es periódico: el próximo ciclo vuelve a verificar a este donante.
        log.error("[RACHAS] No se pudieron verificar las rachas del donante {}", donanteId, e);
      }
    }
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
