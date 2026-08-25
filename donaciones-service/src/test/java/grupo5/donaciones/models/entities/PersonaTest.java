package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Characterization test de {@code Persona.esDuplicadaDe}: fija el comportamiento que antes vivía en
 * {@code CriterioPorDocumento} y {@code CriterioPorMedioDeContacto} (RF Oleada 1 - Persona).
 */
class PersonaTest {

  private static Humana humana() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, 1, 1));
    humana.limpiarMediosDeContacto();
    return humana;
  }

  private static Correo correo(String direccion) {
    Correo correo = new Correo();
    correo.setDireccionCorreo(direccion);
    return correo;
  }

  private static Telefono telefono(
      String caracteristica, String codigoArea, String numero, TipoTelefono tipo) {
    Telefono telefono = new Telefono();
    telefono.setCaracteristica(caracteristica);
    telefono.setCodigoArea(codigoArea);
    telefono.setNumero(numero);
    telefono.setTipo(tipo);
    return telefono;
  }

  @Test
  void esDuplicadaDe_conOtraPersonaNula_deberiaRetornarFalso() {
    Persona persona = humana();

    assertFalse(persona.esDuplicadaDe(null));
  }

  @Test
  void esDuplicadaDe_conMismoDocumento_deberiaRetornarTrue() {
    Persona persona = humana();
    persona.actualizarDocumento(TipoDocumento.DNI, "30111222");
    Persona otra = humana();
    otra.actualizarDocumento(TipoDocumento.DNI, "30111222");

    assertTrue(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conDocumentoDistinto_deberiaRetornarFalso() {
    Persona persona = humana();
    persona.actualizarDocumento(TipoDocumento.DNI, "30111222");
    Persona otra = humana();
    otra.actualizarDocumento(TipoDocumento.DNI, "40222333");

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conDocumentoPropioNulo_deberiaRetornarFalsoAunqueCompartanContacto() {
    Persona persona = humana(); // sin documento (null)
    Persona otra = humana();
    otra.actualizarDocumento(TipoDocumento.DNI, "30111222");

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conDocumentoPropioBlanco_deberiaRetornarFalso() {
    Persona persona = humana();
    persona.actualizarDocumento(TipoDocumento.DNI, "   ");
    Persona otra = humana();
    otra.actualizarDocumento(TipoDocumento.DNI, "   ");

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conCorreoIgualIgnorandoMayusculasYEspacios_deberiaRetornarTrue() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(correo("juan.perez@mail.com"));
    Persona otra = humana();
    otra.agregarMedioDeContacto(correo("  JUAN.PEREZ@MAIL.COM  "));

    assertTrue(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conCorreoDistinto_deberiaRetornarFalso() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(correo("juan.perez@mail.com"));
    Persona otra = humana();
    otra.agregarMedioDeContacto(correo("otro@mail.com"));

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conTelefonoConMismosDigitosFinales_deberiaRetornarTrue() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(telefono("54", "011", "12345678", TipoTelefono.ESTANDAR));
    Persona otra = humana();
    // Mismos dígitos pero sin el código de país: debe matchear por sufijo.
    otra.agregarMedioDeContacto(telefono("", "011", "12345678", TipoTelefono.ESTANDAR));

    assertTrue(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conTelefonoWhatsappYEstandarConMismoNumero_deberiaRetornarTrue() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(telefono("54", "011", "12345678", TipoTelefono.ESTANDAR));
    Persona otra = humana();
    otra.agregarMedioDeContacto(telefono("54", "011", "12345678", TipoTelefono.WHATSAPP));

    assertTrue(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conTelefonoDistinto_deberiaRetornarFalso() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(telefono("54", "011", "12345678", TipoTelefono.ESTANDAR));
    Persona otra = humana();
    otra.agregarMedioDeContacto(telefono("54", "011", "87654321", TipoTelefono.ESTANDAR));

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_conCorreoVsTelefono_deberiaRetornarFalso() {
    Persona persona = humana();
    persona.agregarMedioDeContacto(correo("juan.perez@mail.com"));
    Persona otra = humana();
    otra.agregarMedioDeContacto(telefono("54", "011", "12345678", TipoTelefono.ESTANDAR));

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_sinDocumentoNiMediosDeContactoEnComun_deberiaRetornarFalso() {
    Persona persona = humana();
    Persona otra = humana();

    assertFalse(persona.esDuplicadaDe(otra));
  }

  @Test
  void esDuplicadaDe_enJuridica_deberiaDelegarEnLaMismaLogicaQuePersona() {
    Humana representante = new Humana("Ana", "Gomez", LocalDate.of(1985, 5, 15));
    Juridica juridica = new Juridica(representante, "Empresa SA", TipoJuridico.EMPRESA, "Rubro");
    juridica.actualizarDocumento(TipoDocumento.CUIT, "30-11122233-4");
    Juridica otra = new Juridica(representante, "Otra SA", TipoJuridico.EMPRESA, "Rubro");
    otra.actualizarDocumento(TipoDocumento.CUIT, "30-11122233-4");

    assertTrue(juridica.esDuplicadaDe(otra));
  }
}
