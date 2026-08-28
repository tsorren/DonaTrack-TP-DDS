package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
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

  public NecesidadesService(
      INecesidadesRepository necesidadRepository,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      ISubcategoriasRepository subcategoriaRepository) {
    this.necesidadRepository = necesidadRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.subcategoriaRepository = subcategoriaRepository;
  }

  @Override
  public NecesidadDTO guardar(NecesidadDTO dto) {
    Necesidad necesidadDominio = convertirDTOANecesidad(dto);
    necesidadRepository.save(necesidadDominio);

    return necesidadDominio.toDTO();
  }

  @Override
  public NecesidadDTO obtenerPorId(UUID id) {
    return necesidadRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException(id))
        .toDTO();
  }

  public List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo) {
    return necesidadRepository.findAll().stream()
        .map(Necesidad::toDTO)
        .filter(dto -> entidadId == null || entidadId.equals(dto.idEntidad()))
        .filter(dto -> tipo == null || tipo.equalsIgnoreCase(dto.tipo()))
        .toList();
  }

  // MAPPER INPUT (dto -> dominio)
  private Necesidad convertirDTOANecesidad(NecesidadDTO dto) {

    subcategoriaRepository
        .findById(dto.idSubcategoria())
        .orElseThrow(() -> new RecursoNoEncontradoException(dto.idSubcategoria()));
    entidadesBeneficiariasRepository
        .findById(dto.idEntidad())
        .orElseThrow(() -> new RecursoNoEncontradoException(dto.idEntidad()));
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
