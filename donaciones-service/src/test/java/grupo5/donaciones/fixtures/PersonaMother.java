package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import java.time.LocalDate;
import java.time.Month;

public final class PersonaMother {

  public static final LocalDate FECHA_DEFECTO = LocalDate.of(1990, Month.JANUARY, 1);

  private PersonaMother() {}

  public static Humana humanaValida() {
    return juanPerez();
  }

  public static Humana juanPerez() {
    Humana h = new Humana("Juan", "Pérez", FECHA_DEFECTO, Genero.HOMBRE);
    h.actualizarDocumento(TipoDocumento.DNI, "12345678");
    h.actualizarDireccion(direccionValida());
    return h;
  }

  public static Humana mariaGomez() {
    Humana h = new Humana("María", "Gómez", LocalDate.of(1992, Month.MAY, 15), Genero.MUJER);
    h.actualizarDocumento(TipoDocumento.DNI, "87654321");
    h.actualizarDireccion(direccionValida());
    return h;
  }

  public static Juridica empresaSA() {
    Humana representante = juanPerez();
    Juridica j = new Juridica(representante, "Empresa SA", TipoJuridico.EMPRESA, "Tecnología");
    j.actualizarDocumento(TipoDocumento.CUIT, "30-12345678-9");
    j.actualizarDireccion(direccionValida());
    return j;
  }

  public static Juridica fundacionEsperanza() {
    Humana representante = mariaGomez();
    Juridica j = new Juridica(representante, "Fundación Esperanza", TipoJuridico.ONG, "Social");
    j.actualizarDocumento(TipoDocumento.CUIT, "30-87654321-9");
    j.actualizarDireccion(direccionValida());
    return j;
  }

  public static Direccion direccionValida() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    return new Direccion("Av. Corrientes", 1234, 2, "A", "1043", localidad);
  }

  public static Correo correoValido() {
    Correo c = new Correo();
    c.setDireccionCorreo("contacto@ejemplo.com");
    c.setEsPredeterminado(true);
    return c;
  }

  public static Telefono telefonoValido() {
    Telefono t = new Telefono();
    t.setCaracteristica("+54");
    t.setCodigoArea("11");
    t.setNumero("44445555");
    t.setTipo(TipoTelefono.WHATSAPP);
    t.setEsPredeterminado(true);
    return t;
  }
}
