package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Telefono extends MedioDeContacto {
  private String caracteristica;
  private String codigoArea;
  private String numero;
  private TipoTelefono tipo = TipoTelefono.ESTANDAR;

  public String obtenerNumeroCompleto() {
    return this.caracteristica + this.codigoArea + this.numero;
  }

  @Override
  public void anonimizar() {
    this.caracteristica = Anonimizable.VALOR_STRING;
    this.codigoArea = Anonimizable.VALOR_STRING;
    this.numero = Anonimizable.VALOR_STRING;
  }
}
