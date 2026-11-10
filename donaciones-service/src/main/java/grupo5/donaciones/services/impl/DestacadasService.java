package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.PeriodoDestacadas;
import grupo5.donaciones.dto.donaciones.outputs.DonacionDestacadaDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Donaciones destacadas de la landing (HB-01, D28): entregadas en el período y con foto. */
@Service
public class DestacadasService {

  static final int LIMITE_POR_DEFECTO = 10;
  static final int LIMITE_MAXIMO = 50;

  private final IDonacionesIndependientesRepository donaciones;
  private final ISubcategoriasRepository subcategorias;
  private final ICategoriasRepository categorias;
  private final IPersonasRepository personas;
  private final Clock reloj;

  @Autowired
  public DestacadasService(
      IDonacionesIndependientesRepository donaciones,
      ISubcategoriasRepository subcategorias,
      ICategoriasRepository categorias,
      IPersonasRepository personas) {
    // UTC: la fecha de entrega llega en UTC en entrega.exitosa
    this(donaciones, subcategorias, categorias, personas, Clock.systemUTC());
  }

  DestacadasService(
      IDonacionesIndependientesRepository donaciones,
      ISubcategoriasRepository subcategorias,
      ICategoriasRepository categorias,
      IPersonasRepository personas,
      Clock reloj) {
    this.donaciones = donaciones;
    this.subcategorias = subcategorias;
    this.categorias = categorias;
    this.personas = personas;
    this.reloj = reloj;
  }

  /** {@code periodo} nulo es el mes (D28); {@code limite} va de 1 a 50, 10 si es nulo. */
  public Pagina<DonacionDestacadaDTO> listar(PeriodoDestacadas periodo, Integer limite) {
    if (limite != null && (limite < 1 || limite > LIMITE_MAXIMO)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    PeriodoDestacadas ventana = periodo == null ? PeriodoDestacadas.MES : periodo;
    LocalDateTime desde = LocalDateTime.now(reloj).minusDays(ventana.dias());
    // ponytail: busca categoría, subcategoría y entidad una por destacada (hasta 50); pasar a una
    // consulta con join si la portada lo pide
    List<DonacionDestacadaDTO> destacadas =
        donaciones.findDestacadasDesde(desde, limite == null ? LIMITE_POR_DEFECTO : limite).stream()
            .map(this::aDTO)
            .toList();
    return new Pagina<>(destacadas, Math.toIntExact(donaciones.contarDestacadasDesde(desde)));
  }

  private DonacionDestacadaDTO aDTO(DonacionIndependiente d) {
    Optional<Subcategoria> subcategoria =
        Optional.ofNullable(d.getSubcategoriaId()).flatMap(subcategorias::findById);
    Optional<Categoria> categoria =
        subcategoria.map(Subcategoria::getCategoriaId).flatMap(categorias::findById);
    String entidad =
        Optional.ofNullable(d.getEntidadBeneficiariaId())
            .flatMap(personas::findById)
            .filter(Juridica.class::isInstance)
            .map(p -> ((Juridica) p).getRazonSocial())
            .orElse(null);
    return new DonacionDestacadaDTO(
        d.getId(),
        categoria.map(Categoria::getNombre).orElse(null),
        subcategoria.map(Subcategoria::getNombre).orElse(null),
        d.getCantidad(),
        categoria
            .map(c -> c.getTipoUnidad() == null ? null : c.getTipoUnidad().name())
            .orElse(null),
        d.getEntidadBeneficiariaId(),
        entidad,
        d.getFechaEntrega(),
        d.getFotosRecepcion());
  }
}
