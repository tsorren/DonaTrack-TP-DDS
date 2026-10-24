package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.INecesidadesService;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NecesidadesService implements INecesidadesService {

  private final INecesidadesRepository necesidadRepository;
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final ISubcategoriasRepository subcategoriaRepository;
  private final AcumuladosDeAsignacion acumulados;

  public NecesidadesService(
      INecesidadesRepository necesidadRepository,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      ISubcategoriasRepository subcategoriaRepository,
      AcumuladosDeAsignacion acumulados) {
    this.necesidadRepository = necesidadRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.subcategoriaRepository = subcategoriaRepository;
    this.acumulados = acumulados;
  }

  private NecesidadDTO toDTO(Necesidad necesidad) {
    return necesidad.toDTO(acumulados.asignadoA(necesidad));
  }

  @Override
  public NecesidadDTO guardar(NecesidadDTO dto) {
    Necesidad necesidadDominio = convertirDTOANecesidad(dto);
    necesidadRepository.save(necesidadDominio);

    return toDTO(necesidadDominio);
  }

  @Override
  public NecesidadDTO obtenerPorId(UUID id) {
    return toDTO(
        necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id)));
  }

  public List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo) {
    return necesidadRepository.findAll().stream()
        .map(this::toDTO)
        .filter(dto -> entidadId == null || entidadId.equals(dto.idEntidad()))
        .filter(dto -> tipo == null || tipo.equalsIgnoreCase(dto.tipo()))
        .toList();
  }

  @Override
  public NecesidadDTO actualizar(UUID id, NecesidadDTO dto) {
    Necesidad necesidad =
        necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    necesidad.actualizar(dto.descripcion(), dto.cantidadNecesitada());
    necesidadRepository.save(necesidad);
    return toDTO(necesidad);
  }

  @Override
  public void eliminar(UUID id) {
    Necesidad necesidad =
        necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    necesidad.desactivar();
    necesidadRepository.save(necesidad);
  }

  // MAPPER INPUT (dto -> dominio)
  private Necesidad convertirDTOANecesidad(NecesidadDTO dto) {

    subcategoriaRepository
        .findById(dto.idSubcategoria())
        .orElseThrow(() -> new RecursoNoEncontradoException(dto.idSubcategoria()));
    EntidadBeneficiaria entidad =
        entidadesBeneficiariasRepository
            .findById(dto.idEntidad())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idEntidad()));
    if (!entidad.estaActivo()) {
      throw new BusinessStateException(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA);
    }
    Necesidad necesidad =
        switch (dto.tipo()) {
          case "RECURRENTE" -> {
            Period periodo = Period.between(dto.fechaInicio(), dto.fechaFin());
            yield new NecesidadRecurrente(
                dto.idSubcategoria(),
                dto.cantidadNecesitada(),
                dto.descripcion(),
                periodo,
                dto.fechaInicio());
          }
          case "EXTRAORDINARIA" ->
              new NecesidadExtraordinaria(
                  dto.idSubcategoria(), dto.cantidadNecesitada(), dto.descripcion());
          default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        };
    if (dto.idEntidad() != null) {
      necesidad.asociarAEntidad(dto.idEntidad());
    }

    return necesidad;
  }
}
