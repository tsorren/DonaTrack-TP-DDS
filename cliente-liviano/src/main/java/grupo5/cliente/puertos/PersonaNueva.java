package grupo5.cliente.puertos;

import java.util.List;

/** Los cuerpos con que el sitio da de alta una persona en Donaciones. */
public sealed interface PersonaNueva {

  record Contacto(String tipo, String direccionCorreo, boolean esPredeterminado) {

    static List<Contacto> correo(String direccion) {
      return List.of(new Contacto("CORREO", direccion, true));
    }
  }

  record Direccion(
      String calle,
      Integer altura,
      String codigoPostal,
      String localidad,
      String provincia,
      String pais) {}

  record Humana(String tipo, String nombre, String apellido, List<Contacto> mediosDeContacto)
      implements PersonaNueva {

    public Humana(String nombre, String apellido, String correo) {
      this("HUMANA", nombre, apellido, Contacto.correo(correo));
    }

    /** Parte "Ana María Pérez" en el nombre y el apellido, que es lo último que se escribe. */
    public static Humana deNombreCompleto(String completo, String correo) {
      String[] partes = completo.trim().split("\\s+(?=\\S+$)", 2);
      return new Humana(partes[0], partes.length > 1 ? partes[1] : "", correo);
    }
  }

  record Juridica(
      String tipo,
      String razonSocial,
      String tipoJuridico,
      String rubro,
      Direccion direccion,
      List<Contacto> mediosDeContacto,
      List<Humana> representantes)
      implements PersonaNueva {

    public Juridica(
        String razonSocial,
        String tipoJuridico,
        String rubro,
        Direccion direccion,
        Humana representante,
        String correo) {
      this(
          "JURIDICA",
          razonSocial,
          tipoJuridico,
          rubro,
          direccion,
          Contacto.correo(correo),
          List.of(representante));
    }
  }
}
