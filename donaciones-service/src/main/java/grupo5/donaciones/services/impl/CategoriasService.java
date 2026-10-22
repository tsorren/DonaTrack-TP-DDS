package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.ICategoriasService;
import grupo5.donaciones.services.mappers.CategoriaMapper;
import grupo5.donaciones.services.mappers.SubcategoriaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoriasService implements ICategoriasService {

  private final ICategoriasRepository categoriasRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final INecesidadesRepository necesidadesRepository;
  private final CategoriaMapper categoriaMapper;
  private final SubcategoriaMapper subcategoriaMapper;

  public CategoriasService(
      ICategoriasRepository categoriasRepository,
      ISubcategoriasRepository subcategoriasRepository,
      INecesidadesRepository necesidadesRepository,
      CategoriaMapper categoriaMapper,
      SubcategoriaMapper subcategoriaMapper) {
    this.categoriasRepository = categoriasRepository;
    this.subcategoriasRepository = subcategoriasRepository;
    this.necesidadesRepository = necesidadesRepository;
    this.categoriaMapper = categoriaMapper;
    this.subcategoriaMapper = subcategoriaMapper;
  }

  @Override
  public CategoriaOutputDTO crear(CategoriaInputDTO dto) {
    Categoria entity = categoriaMapper.toEntity(dto);
    categoriasRepository.save(entity);
    return getCategoriaOutputDTO(entity);
  }

  @Override
  public CategoriaOutputDTO eliminar(UUID id) {
    Categoria entity =
        categoriasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    List<Subcategoria> subcategories = getSubcategoriesFor(id);
    // Se valida todo antes de borrar nada, para no dejar la categoría a medio borrar
    if (subcategories.stream()
        .anyMatch(s -> necesidadesRepository.existsBySubcategoriaId(s.getId()))) {
      throw new BusinessStateException(ErrorCatalog.SUBCATEGORIA_EN_USO);
    }
    subcategories.forEach(subcategoriasRepository::delete);

    categoriasRepository.delete(entity);
    return categoriaMapper.toOutputDTO(entity);
  }

  @Override
  public CategoriaOutputDTO modificar(UUID id, CategoriaInputDTO dto) {
    Categoria entity =
        categoriasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    categoriaMapper.updateEntity(entity, dto);
    categoriasRepository.save(entity);
    return getCategoriaOutputDTO(entity);
  }

  @Override
  public List<CategoriaOutputDTO> obtenerTodas() {
    return categoriasRepository.findAll().stream().map(this::getCategoriaOutputDTO).toList();
  }

  @Override
  public CategoriaOutputDTO obtener(UUID id) {
    Categoria entity =
        categoriasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return getCategoriaOutputDTO(entity);
  }

  private CategoriaOutputDTO getCategoriaOutputDTO(Categoria entity) {
    List<SubcategoriaOutputDTO> subcatDtos =
        getSubcategoriesFor(entity.getId()).stream().map(subcategoriaMapper::toOutputDTO).toList();
    return categoriaMapper.toOutputDTO(entity, subcatDtos);
  }

  private List<Subcategoria> getSubcategoriesFor(UUID categoryId) {
    return subcategoriasRepository.findAll().stream()
        .filter(s -> categoryId.equals(s.getCategoriaId()))
        .toList();
  }
}
