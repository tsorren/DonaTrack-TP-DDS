package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.CambioEstadoDonacionEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DepositoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DonacionEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.ItemDonacionEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDepositoRepository;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.CambioEstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.DatosDonacion;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class DonacionesPersistenciaMapper {

  private final DireccionesPersistenciaMapper direcciones;
  private final SpringDataDepositoRepository depositos;

  public DonacionesPersistenciaMapper(
      DireccionesPersistenciaMapper direcciones, SpringDataDepositoRepository depositos) {
    this.direcciones = direcciones;
    this.depositos = depositos;
  }

  /** {@code existente}: la fila ya guardada, para agregar al historial solo los cambios nuevos. */
  public DonacionEntity toEntity(Donacion domain, DonacionEntity existente) {
    DonacionEntity entity = existente != null ? existente : new DonacionEntity();
    entity.setId(domain.getId());
    entity.setDonanteId(domain.getDonanteId());
    entity.setDescripcion(domain.getDescripcion());
    entity.setFecha(domain.getFecha());
    entity.setEstado(domain.getEstadoActual());
    entity.setDeposito(
        domain.getDepositoRecepcion() != null ? resolver(domain.getDepositoRecepcion()) : null);
    reemplazarItems(domain, entity);
    agregarCambiosNuevos(domain, entity);
    return entity;
  }

  public Donacion toDomain(DonacionEntity entity) {
    DatosDonacion datos =
        new DatosDonacion(
            entity.getId(),
            entity.getDonanteId(),
            entity.getDescripcion(),
            entity.getFecha(),
            entity.getDeposito() != null ? depositoToDomain(entity.getDeposito()) : null,
            entity.getEstado());
    List<ItemDonacion> items =
        entity.getItems().stream().map(DonacionesPersistenciaMapper::itemToDomain).toList();
    List<CambioEstadoDonacion> historial =
        entity.getHistorial().stream()
            .map(
                c ->
                    new CambioEstadoDonacion(
                        c.getEstadoAnterior(), c.getEstadoNuevo(), c.getTimestamp()))
            .toList();
    return new Donacion(datos, items, historial);
  }

  // P-2 y D-9: el depósito se comparte; se reutiliza la fila con el mismo nombre y dirección
  private DepositoEntity resolver(Deposito deposito) {
    DireccionEntity direccion = direcciones.resolver(deposito.direccion());
    return depositos
        .findByNombreAndDireccion(deposito.nombre(), direccion)
        .orElseGet(
            () -> {
              DepositoEntity nuevo = new DepositoEntity();
              nuevo.setId(UUID.randomUUID());
              nuevo.setNombre(deposito.nombre());
              nuevo.setDireccion(direccion);
              return depositos.save(nuevo);
            });
  }

  private Deposito depositoToDomain(DepositoEntity entity) {
    return new Deposito(entity.getNombre(), direcciones.toDomain(entity.getDireccion()));
  }

  // Los ítems no tienen identidad en el dominio: se reemplazan y orphanRemoval borra los viejos
  private static void reemplazarItems(Donacion domain, DonacionEntity entity) {
    entity.getItems().clear();
    for (ItemDonacion item : domain.getItems()) {
      Bien bien = item.bien();
      ItemDonacionEntity fila = new ItemDonacionEntity();
      fila.setDonacion(entity);
      fila.setDescripcion(bien.descripcion());
      fila.setFotoUrl(bien.fotoUrl());
      fila.setFechaVencimiento(bien.fechaVencimiento());
      fila.setEstado(bien.estado());
      fila.setPesoUnitario(MedidasPersistencia.aColumna(bien.pesoUnitario()));
      fila.setVolumenUnitario(MedidasPersistencia.aColumna(bien.volumenUnitario()));
      fila.setCantidad(item.cantidad().longValue());
      entity.getItems().add(fila);
    }
  }

  // Historial append-only: los cambios ya guardados no se tocan, solo se agregan los nuevos
  private static void agregarCambiosNuevos(Donacion domain, DonacionEntity entity) {
    List<CambioEstadoDonacion> cambios = domain.getHistorialEstados();
    for (int i = entity.getHistorial().size(); i < cambios.size(); i++) {
      CambioEstadoDonacion cambio = cambios.get(i);
      CambioEstadoDonacionEntity fila = new CambioEstadoDonacionEntity();
      fila.setDonacion(entity);
      fila.setEstadoAnterior(cambio.getEstadoAnterior());
      fila.setEstadoNuevo(cambio.getEstadoNuevo());
      fila.setTimestamp(cambio.getTimestamp());
      entity.getHistorial().add(fila);
    }
  }

  private static ItemDonacion itemToDomain(ItemDonacionEntity fila) {
    Bien bien =
        new Bien(
            fila.getDescripcion(),
            fila.getFotoUrl(),
            fila.getFechaVencimiento(),
            fila.getEstado(),
            MedidasPersistencia.aDominio(fila.getPesoUnitario()),
            MedidasPersistencia.aDominio(fila.getVolumenUnitario()));
    return new ItemDonacion(bien, fila.getCantidad().intValue());
  }
}
