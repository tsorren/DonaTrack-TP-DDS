package grupo5.cliente.puertos;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.UUID;

/**
 * Mi perfil del donante y el tipo de su cuenta. Lo implementan la maqueta y el adaptador REST;
 * {@code cliente.datos.donante} elige cuál.
 */
public interface DonantePerfilPuerto {

  /**
   * El tipo de la cuenta, que elige la versión de las pantallas del donante
   * (docs/interfaz/flujos/r11-p0-persona.md): persona ({@code HUMANA}), empresa ({@code JURIDICA})
   * o sin tipo, que nunca se trata como empresa.
   */
  enum Tipo {
    PERSONA,
    EMPRESA,
    SIN_TIPO
  }

  DonantePerfilVista perfil(UUID donante);

  /** Qué tipo de cuenta es el donante, según Donaciones. */
  Tipo tipo(UUID donante);

  /**
   * La cuenta del menú y del saludo (R11-44): el nombre y apellido de la persona o la razón social,
   * y qué tipo de donante es ("Persona donante", "Empresa donante", "ONG donante"…). Nunca el rol.
   */
  default CuentaVista cuenta(UUID donante) {
    String tipo =
        switch (tipo(donante)) {
          case PERSONA -> "Persona donante";
          case EMPRESA -> "Empresa donante";
          case SIN_TIPO -> "Donante";
        };
    return new CuentaVista(perfil(donante).nombre(), tipo);
  }

  /** Si las entidades ven el nombre del donante en lo que reciben. */
  void mostrarNombre(UUID donante, boolean mostrar);
}
