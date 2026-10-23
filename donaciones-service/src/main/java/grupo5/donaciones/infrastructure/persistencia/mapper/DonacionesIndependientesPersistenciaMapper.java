package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.CambioEstadoDonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.DonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.EstadoDonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.ItemDonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DatosDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.EstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DonacionesIndependientesPersistenciaMapper {

  private final ItemsNormalizadosPersistenciaMapper itemsNormalizados;
  private final SpringDataItemDonacionNormalizadoRepository itemsNormalizadosRepo;

  public DonacionesIndependientesPersistenciaMapper(
      ItemsNormalizadosPersistenciaMapper itemsNormalizados,
      SpringDataItemDonacionNormalizadoRepository itemsNormalizadosRepo) {
    this.itemsNormalizados = itemsNormalizados;
    this.itemsNormalizadosRepo = itemsNormalizadosRepo;
  }

  /** {@code existente}: la fila ya guardada, para agregar al historial solo los cambios nuevos. */
  public DonacionIndependienteEntity toEntity(
      DonacionIndependiente domain, DonacionIndependienteEntity existente) {
    DonacionIndependienteEntity entity =
        existente != null ? existente : new DonacionIndependienteEntity();
    entity.setId(domain.getId());
    entity.setDonacionId(domain.getDonacionOriginalId());
    entity.setEstadoActual(domain.getEstadoActual().getTipo());
    entity.setFechaRegistro(domain.getFechaRegistro());
    entity.setNecesidadId(domain.getNecesidadId());
    entity.setPeriodoId(domain.getPeriodoId());
    entity.setEntidadBeneficiariaId(domain.getEntidadBeneficiariaId());
    reemplazarItems(domain, entity);
    agregarCambiosNuevos(domain, entity);
    return entity;
  }

  public DonacionIndependiente toDomain(DonacionIndependienteEntity entity) {
    Map<UUID, EstadoDonacionIndependiente> estados = new HashMap<>();
    entity.getEstados().forEach(fila -> estados.put(fila.getId(), estadoToDomain(fila)));

    List<CambioEstado> historial = new ArrayList<>();
    for (CambioEstadoDonacionIndependienteEntity c : enOrden(entity.getHistorial())) {
      historial.add(
          new CambioEstado(
              c.getEstadoAnteriorId() != null ? estados.get(c.getEstadoAnteriorId()) : null,
              estados.get(c.getEstadoNuevoId()),
              c.getTimestamp(),
              c.getJustificacion(),
              c.getActor()));
    }

    // D2: manda la columna; si coincide con el último cambio se usa esa instancia (con su patente)
    EstadoDonacionIndependiente actual =
        !historial.isEmpty()
                && historial.getLast().getEstadoNuevo().getTipo() == entity.getEstadoActual()
            ? historial.getLast().getEstadoNuevo()
            : estadoDesde(entity.getEstadoActual(), null);

    DatosDonacionIndependiente datos =
        new DatosDonacionIndependiente(
            entity.getId(),
            entity.getDonacionId(),
            actual,
            entity.getFechaRegistro(),
            destino(entity));
    List<ItemDonacionIndependiente> items =
        entity.getItems().stream().map(this::itemToDomain).toList();
    return new DonacionIndependiente(datos, items, historial);
  }

  // Los ítems no tienen identidad en el dominio: se reemplazan y orphanRemoval borra los viejos
  private void reemplazarItems(DonacionIndependiente domain, DonacionIndependienteEntity entity) {
    entity.getItems().clear();
    for (ItemDonacionIndependiente item : domain.getItems()) {
      ItemDonacionIndependienteEntity fila = new ItemDonacionIndependienteEntity();
      fila.setDonacion(entity);
      fila.setItemNormalizado(itemsNormalizadosRepo.getReferenceById(item.itemNormalizadoId()));
      fila.setCantidad(item.cantidad().longValue());
      fila.setBienDescripcion(item.bien().bienOriginal().descripcion());
      entity.getItems().add(fila);
    }
  }

  /**
   * Historial append-only. Cada estado por el que pasa la donación es una fila (D-21), y cada
   * cambio apunta a la fila del estado anterior y a la del nuevo. La primera fila es el estado
   * inicial.
   */
  private static void agregarCambiosNuevos(
      DonacionIndependiente domain, DonacionIndependienteEntity entity) {
    List<CambioEstado> cambios = domain.getHistorial();
    List<CambioEstadoDonacionIndependienteEntity> guardados = enOrden(entity.getHistorial());
    UUID ultimo;
    if (!guardados.isEmpty()) {
      ultimo = guardados.getLast().getEstadoNuevoId();
    } else if (!entity.getEstados().isEmpty()) {
      ultimo = entity.getEstados().getFirst().getId();
    } else {
      EstadoDonacionIndependiente inicial =
          cambios.isEmpty() ? domain.getEstadoActual() : cambios.getFirst().getEstadoAnterior();
      ultimo = estadoFila(entity, inicial).getId();
    }
    for (int i = guardados.size(); i < cambios.size(); i++) {
      CambioEstado cambio = cambios.get(i);
      UUID nuevo = estadoFila(entity, cambio.getEstadoNuevo()).getId();
      CambioEstadoDonacionIndependienteEntity fila = new CambioEstadoDonacionIndependienteEntity();
      fila.setDonacion(entity);
      fila.setEstadoAnteriorId(ultimo);
      fila.setEstadoNuevoId(nuevo);
      fila.setTimestamp(cambio.getTimestamp());
      fila.setActor(cambio.getActor());
      fila.setJustificacion(cambio.getJustificacion());
      entity.getHistorial().add(fila);
      ultimo = nuevo;
    }
  }

  private static EstadoDonacionIndependienteEntity estadoFila(
      DonacionIndependienteEntity entity, EstadoDonacionIndependiente estado) {
    EstadoDonacionIndependienteEntity fila = new EstadoDonacionIndependienteEntity();
    fila.setDonacion(entity);
    fila.setTipo(estado.getTipo());
    if (estado instanceof Entregada entregada) {
      fila.setPatenteCamion(entregada.getPatenteCamion());
    }
    entity.getEstados().add(fila);
    return fila;
  }

  /**
   * Ordena el historial encadenando cada cambio con el siguiente (el nuevo de uno es el anterior
   * del otro). No alcanza con el timestamp: una falla replanificable registra dos cambios seguidos
   * que pueden quedar con el mismo instante.
   */
  private static List<CambioEstadoDonacionIndependienteEntity> enOrden(
      List<CambioEstadoDonacionIndependienteEntity> cambios) {
    Map<UUID, CambioEstadoDonacionIndependienteEntity> porAnterior = new HashMap<>();
    Set<UUID> nuevos = new HashSet<>();
    for (CambioEstadoDonacionIndependienteEntity c : cambios) {
      if (c.getEstadoAnteriorId() != null) {
        porAnterior.put(c.getEstadoAnteriorId(), c);
      }
      nuevos.add(c.getEstadoNuevoId());
    }
    CambioEstadoDonacionIndependienteEntity actual =
        cambios.stream()
            .filter(
                c -> c.getEstadoAnteriorId() == null || !nuevos.contains(c.getEstadoAnteriorId()))
            .findFirst()
            .orElse(null);
    List<CambioEstadoDonacionIndependienteEntity> ordenados = new ArrayList<>();
    while (actual != null && ordenados.size() < cambios.size()) {
      ordenados.add(actual);
      actual = porAnterior.get(actual.getEstadoNuevoId());
    }
    return ordenados.size() == cambios.size() ? ordenados : cambios;
  }

  private ItemDonacionIndependiente itemToDomain(ItemDonacionIndependienteEntity fila) {
    return new ItemDonacionIndependiente(
        fila.getItemNormalizado().getId(),
        itemsNormalizados.toDomain(fila.getItemNormalizado()).getBien(),
        fila.getCantidad().intValue());
  }

  private static DestinoAsignacion destino(DonacionIndependienteEntity entity) {
    if (entity.getNecesidadId() == null
        && entity.getPeriodoId() == null
        && entity.getEntidadBeneficiariaId() == null) {
      return null;
    }
    return new DestinoAsignacion(
        entity.getNecesidadId(), entity.getPeriodoId(), entity.getEntidadBeneficiariaId());
  }

  private static EstadoDonacionIndependiente estadoToDomain(
      EstadoDonacionIndependienteEntity fila) {
    return estadoDesde(fila.getTipo(), fila.getPatenteCamion());
  }

  private static EstadoDonacionIndependiente estadoDesde(
      TipoEstadoDonacion tipo, String patenteCamion) {
    return switch (tipo) {
      case EN_DEPOSITO -> new EnDeposito();
      case ASIGNACION_REALIZADA -> new AsignacionRealizada();
      case LISTA_PARA_ENTREGAR -> new ListaParaEntregar();
      case EN_TRASLADO -> new EnTraslado();
      case ENTREGADA -> new Entregada(patenteCamion);
      case ENTREGA_FALLIDA -> new EntregaFallida();
      case VENCIDA -> new Vencida();
    };
  }
}
