package grupo5.tests.builders;

import grupo5.tests.dto.DireccionTestDTO;
import grupo5.tests.dto.MedioContactoTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.TestIdGenerator;
import java.util.ArrayList;
import java.util.List;

public class PersonaTestDataBuilder {
  private String tipo = "HUMANA";
  private String tipoDocumento = "DNI";
  private String documento = TestIdGenerator.randomDni();
  private String nombre = "Juan";
  private String apellido = "Perez";
  private String genero = "HOMBRE";
  private String fechaNacimiento = "1995-05-15";
  private String razonSocial = null;
  private String tipoJuridico = "ONG";
  private String rubro = null;
  private List<PersonaTestDTO> representantes = null;
  private final List<MedioContactoTestDTO> medios = new ArrayList<>();
  private DireccionTestDTO direccion = DireccionTestDTO.defaultMedrano();

  public static PersonaTestDataBuilder humana() {
    PersonaTestDataBuilder b = new PersonaTestDataBuilder();
    b.tipo = "HUMANA";
    b.tipoDocumento = "DNI";
    b.tipoJuridico = null;
    b.medios.add(MedioContactoTestDTO.email(TestIdGenerator.randomEmail("juan")));
    return b;
  }

  public static PersonaTestDataBuilder juridica() {
    PersonaTestDataBuilder b = new PersonaTestDataBuilder();
    b.tipo = "JURIDICA";
    b.tipoDocumento = "CUIT";
    b.documento = TestIdGenerator.randomCuit();
    b.nombre = null;
    b.apellido = null;
    b.genero = null;
    b.fechaNacimiento = null;
    b.razonSocial = TestIdGenerator.uniqueName("Comedor Solidario");
    b.tipoJuridico = "ONG";
    b.rubro = "Comedor";
    b.medios.add(MedioContactoTestDTO.email(TestIdGenerator.randomEmail("comedor")));

    // Persona Juridica requiere al menos un representante inicial
    PersonaTestDTO rep =
        PersonaTestDataBuilder.humana()
            .conNombre("Representante")
            .conApellido("Legal")
            .conDocumento(TestIdGenerator.randomDni())
            .build();
    b.representantes = List.of(rep);
    return b;
  }

  public PersonaTestDataBuilder conDocumento(String doc) {
    this.documento = doc;
    return this;
  }

  public PersonaTestDataBuilder conNombre(String nom) {
    this.nombre = nom;
    return this;
  }

  public PersonaTestDataBuilder conApellido(String ape) {
    this.apellido = ape;
    return this;
  }

  public PersonaTestDataBuilder conRazonSocial(String rs) {
    this.razonSocial = rs;
    return this;
  }

  public PersonaTestDataBuilder conRepresentantes(List<PersonaTestDTO> reps) {
    this.representantes = reps;
    return this;
  }

  public PersonaTestDataBuilder conEmail(String email) {
    this.medios.clear();
    this.medios.add(MedioContactoTestDTO.email(email));
    return this;
  }

  public PersonaTestDTO build() {
    return new PersonaTestDTO(
        tipo,
        tipoDocumento,
        documento,
        nombre,
        apellido,
        genero,
        fechaNacimiento,
        razonSocial,
        tipoJuridico,
        rubro,
        representantes,
        medios,
        direccion);
  }
}
