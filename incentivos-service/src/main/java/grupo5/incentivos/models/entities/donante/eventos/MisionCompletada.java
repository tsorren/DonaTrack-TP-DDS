package grupo5.incentivos.models.entities.donante.eventos;

import grupo5.incentivos.models.entities.insignias.Insignia;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MisionCompletada extends EventoDonanteIncentivos {
  private final String nombreDonante;
  private final String nombreMision;
  private final Insignia insignia;

  public MisionCompletada(
      UUID donanteId,
      UUID idPersona,
      String nombreDonante,
      String nombreMision,
      Insignia insignia) {
    super(donanteId, idPersona);
    this.nombreDonante = nombreDonante;
    this.nombreMision = nombreMision;
    this.insignia = insignia;
  }

  public String nombreDonante() {
    return nombreDonante;
  }

  public String nombreMision() {
    return nombreMision;
  }

  public Insignia insignia() {
    return insignia;
  }
}
