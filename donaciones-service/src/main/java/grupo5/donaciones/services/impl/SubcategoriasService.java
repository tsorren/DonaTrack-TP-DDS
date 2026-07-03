package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.ISubcategoriasService;
import grupo5.donaciones.services.mappers.SubcategoriaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubcategoriasService implements ISubcategoriasService {

  private final ISubcategoriasRepository subcategoriasRepository;
  private final ICategoriasRepository categoriasRepository;
  private final SubcategoriaMapper subcategoriaMapper;

  public SubcategoriasService(
      ISubcategoriasRepository subcategoriasRepository,
      ICategoriasRepository categoriasRepository,
      SubcategoriaMapper subcategoriaMapper) {
    this.subcategoriasRepository = subcategoriasRepository;
    this.categoriasRepository = categoriasRepository;
    this.subcategoriaMapper = subcategoriaMapper;
  }

  @Override
  public SubcategoriaOutputDTO crear(SubcategoriaInputDTO dto) {
    Categoria categoria =
        categoriasRepository
            .findById(dto.idCategoria())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idCategoria()));
    Subcategoria entity = subcategoriaMapper.toEntity(dto, categoria);
    subcategoriasRepository.save(entity);
    return subcategoriaMapper.toOutputDTO(entity);
  }

  @Override
  public SubcategoriaOutputDTO eliminar(UUID id) {
    Subcategoria entity =
        subcategoriasRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));
    subcategoriasRepository.delete(entity);
    return subcategoriaMapper.toOutputDTO(entity);
  }

  @Override
  public SubcategoriaOutputDTO modificar(UUID id, SubcategoriaInputDTO dto) {
    Subcategoria entity =
        subcategoriasRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));
    Categoria categoria =
        categoriasRepository
            .findById(dto.idCategoria())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idCategoria()));
    subcategoriaMapper.updateEntity(entity, dto, categoria);
    subcategoriasRepository.save(entity);
    return subcategoriaMapper.toOutputDTO(entity);
  }

  @Override
  public List<SubcategoriaOutputDTO> obtenerTodas() {
    return subcategoriasRepository.findAll().stream().map(subcategoriaMapper::toOutputDTO).toList();
  }

  @Override
  public SubcategoriaOutputDTO obtener(UUID id) {
    Subcategoria entity =
        subcategoriasRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));
    return subcategoriaMapper.toOutputDTO(entity);
  }

  @Override
  public SubcategoriaOutputDTO agregarAlias(UUID id, AliasSubcategoriaInputDTO dto) {
    Subcategoria entity =
        subcategoriasRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));
    entity.agregarAlias(dto.alias());
    subcategoriasRepository.save(entity);
    return subcategoriaMapper.toOutputDTO(entity);
  }

  @Override
  public SubcategoriaOutputDTO quitarAlias(UUID id, String alias) {
    Subcategoria entity =
        subcategoriasRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));

    entity.removerAlias(alias);
    subcategoriasRepository.save(entity);
    return subcategoriaMapper.toOutputDTO(entity);
  }
}
