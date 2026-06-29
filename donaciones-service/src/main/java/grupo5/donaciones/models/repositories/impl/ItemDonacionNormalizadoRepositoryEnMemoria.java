package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ItemDonacionNormalizadoRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<ItemDonacionNormalizado>
    implements IItemDonacionNormalizadoRepository {}
