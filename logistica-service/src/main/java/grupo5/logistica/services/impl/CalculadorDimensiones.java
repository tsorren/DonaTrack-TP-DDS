package grupo5.logistica.services.impl;

import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemTransportable;
import grupo5.logistica.models.valueobjects.ConversionUnidad;
import grupo5.logistica.models.valueobjects.DimensionCarga;
import java.util.*;

public class CalculadorDimensiones {

  private final Map<UUID, ConversionUnidad> conversiones = new HashMap<>();

  public void registrarConversion(UUID subcategoriaId, ConversionUnidad conversion) {
    conversiones.put(subcategoriaId, conversion);
  }

  public DimensionCarga calcularDimensiones(List<? extends ItemTransportable> items) {
    float pesoTotal = 0;
    float volumenTotal = 0;

    for (ItemTransportable item : items) {
      Unidad unidad = item.getUnidad();
      UUID subcategoriaId = item.getSubcategoriaId();
      int cantidad = item.getCantidad();

      ConversionUnidad conv = obtenerConversion(subcategoriaId);

      switch (unidad) {
        case KILOGRAMO -> {
          pesoTotal += cantidad;
          volumenTotal += (cantidad * conv.volumenPorUnidad());
        }
        case METRO_CUBICO -> {
          volumenTotal += cantidad;
          pesoTotal += (cantidad * conv.pesoPorUnidad());
        }
        case LITROS -> {
          float volumenM3 = cantidad / 1000f;
          volumenTotal += volumenM3;
          pesoTotal += (cantidad * conv.pesoPorUnidad());
        }
        case UNIDADES, METRO, METRO_CUADRADO -> {
          pesoTotal += cantidad * conv.pesoPorUnidad();
          volumenTotal += cantidad * conv.volumenPorUnidad();
        }
      }
    }
    return new DimensionCarga(pesoTotal, volumenTotal);
  }

  private ConversionUnidad obtenerConversion(UUID subcategoriaId) {
    ConversionUnidad conv = conversiones.get(subcategoriaId);
    return Objects.requireNonNullElseGet(conv, () -> new ConversionUnidad(1.0f, 0.001f));
  }
}
