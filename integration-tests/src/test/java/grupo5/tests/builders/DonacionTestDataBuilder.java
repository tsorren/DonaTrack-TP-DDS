package grupo5.tests.builders;

import grupo5.tests.dto.DireccionTestDTO;
import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.ItemDonacionTestDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DonacionTestDataBuilder {
  private UUID idDonante;
  private String descripcion = "Donación estándar";
  private final List<ItemDonacionTestDTO> items = new ArrayList<>();
  private String nombreDeposito = "Deposito Central";
  private DireccionTestDTO direccion = DireccionTestDTO.defaultMedrano();
  private String fecha = null;

  public static DonacionTestDataBuilder deAlimento(String descripcionBien, int cantidad) {
    DonacionTestDataBuilder b = new DonacionTestDataBuilder();
    b.items.add(ItemDonacionTestDTO.simple(descripcionBien, cantidad));
    return b;
  }

  public static DonacionTestDataBuilder deRopa(String descripcionBien, int cantidad) {
    DonacionTestDataBuilder b = new DonacionTestDataBuilder();
    b.items.add(
        new ItemDonacionTestDTO(descripcionBien, null, null, "NUEVO", 0.5, 0.005, cantidad));
    return b;
  }

  public DonacionTestDataBuilder conDonante(UUID donanteId) {
    this.idDonante = donanteId;
    return this;
  }

  public DonacionTestDataBuilder conDescripcion(String desc) {
    this.descripcion = desc;
    return this;
  }

  public DonacionTestDataBuilder conItem(String bien, int cant, double peso, double vol) {
    this.items.add(new ItemDonacionTestDTO(bien, null, "2027-12-31", "NUEVO", peso, vol, cant));
    return this;
  }

  public DonacionTestDataBuilder conFecha(String fecha) {
    this.fecha = fecha;
    return this;
  }

  public DonacionTestDTO build() {
    return new DonacionTestDTO(idDonante, descripcion, items, nombreDeposito, direccion, fecha);
  }
}
