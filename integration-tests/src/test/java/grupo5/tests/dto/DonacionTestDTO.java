package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DonacionTestDTO(
    UUID idDonante,
    String descripcion,
    List<ItemDonacionTestDTO> items,
    String nombreDeposito,
    DireccionTestDTO direccion,
    String fecha) {}
