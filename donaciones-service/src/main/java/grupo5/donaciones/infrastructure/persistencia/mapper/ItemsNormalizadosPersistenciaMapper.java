package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.itemsNormalizados.ItemDonacionNormalizadoEntity;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import org.springframework.stereotype.Component;

@Component
public class ItemsNormalizadosPersistenciaMapper {

  public ItemDonacionNormalizadoEntity toEntity(ItemDonacionNormalizado domain) {
    BienNormalizado normalizado = domain.getBien();
    Bien bien = normalizado.bienOriginal();
    ItemDonacionNormalizadoEntity entity = new ItemDonacionNormalizadoEntity();
    entity.setId(domain.getId());
    entity.setDonacionId(domain.getDonacionOriginalId());
    entity.setCantidad(domain.getCantidad().longValue());
    entity.setSegmentado(domain.isSegmentado());
    entity.setBienDescripcion(bien.descripcion());
    entity.setFotoUrl(bien.fotoUrl());
    entity.setFechaVencimiento(bien.fechaVencimiento());
    entity.setEstado(bien.estado());
    entity.setPesoUnitario(MedidasPersistencia.aColumna(bien.pesoUnitario()));
    entity.setVolumenUnitario(MedidasPersistencia.aColumna(bien.volumenUnitario()));
    entity.setSubcategoriaId(normalizado.subcategoriaId());
    entity.setConfianza(normalizado.confianza());
    entity.setEstadoNormalizacion(normalizado.estadoNormalizacion());
    return entity;
  }

  /** Usa el constructor canónico de {@code BienNormalizado}: reconstituye sin revalidar. */
  public ItemDonacionNormalizado toDomain(ItemDonacionNormalizadoEntity entity) {
    Bien bien =
        new Bien(
            entity.getBienDescripcion(),
            entity.getFotoUrl(),
            entity.getFechaVencimiento(),
            entity.getEstado(),
            MedidasPersistencia.aDominio(entity.getPesoUnitario()),
            MedidasPersistencia.aDominio(entity.getVolumenUnitario()));
    BienNormalizado normalizado =
        new BienNormalizado(
            bien,
            entity.getSubcategoriaId(),
            entity.getConfianza(),
            entity.getEstadoNormalizacion());
    return new ItemDonacionNormalizado(
        entity.getId(),
        entity.getDonacionId(),
        normalizado,
        entity.getCantidad().intValue(),
        entity.isSegmentado());
  }
}
