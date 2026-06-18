package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import grupo5.donaciones.services.INecesidadesService;
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

    return dto;
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
        .filter(dto -> entidadId == null || entidadId.equals(dto.getIdEntidad()))
        .filter(dto -> tipo == null || tipo.equalsIgnoreCase(dto.getTipo()))
        .toList();
  }

  // MAPPER INPUT (dto -> dominio)
  private Necesidad convertirDTOANecesidad(NecesidadDTO dto) {

    Subcategoria subcategoria =
        subcategoriaRepository
            .findById(dto.getIdSubcategoria())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.getIdSubcategoria()));
    EntidadBeneficiaria entidadBeneficiaria =
        entidadesBeneficiariasRepository
            .findById(dto.getIdEntidad())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.getIdEntidad()));
    Necesidad necesidad;
    switch (dto.getTipo()) {
      case "RECURRENTE" -> {
        Period periodo = Period.between(dto.getFechaInicio(), dto.getFechaFin());
        necesidad =
            new NecesidadRecurrente(
                subcategoria,
                dto.getCantidadNecesitada(),
                dto.getDescripcion(),
                periodo,
                dto.getFechaInicio());
      }
      case "EXTRAORDINARIA" -> {
        necesidad =
            new NecesidadExtraordinaria(
                subcategoria, dto.getCantidadNecesitada(), dto.getDescripcion());
      }
      default -> throw (new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));
    }

    necesidad.setEntidad(
        entidadBeneficiaria); // TODO: Anaalizar si esto debería ir en el constructor
    return necesidad;
  }
}
