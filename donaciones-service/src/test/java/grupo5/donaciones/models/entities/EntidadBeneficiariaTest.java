package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTest {

  private Juridica juridica;

  @BeforeEach
  void setUp() {
    juridica = PersonaMother.empresaSA();
  }

  @Test
  void crearEntidadBeneficiaria_conJuridicaValida_usaElIdDeLaJuridica() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());

    assertEquals(juridica.getId(), entidad.getId());
    assertEquals(juridica.getId(), entidad.juridicaId());
  }

  @Test
  void crearEntidadBeneficiaria_conJuridicaNula_deberiaLanzarValidationException() {
    assertThrows(ValidationException.class, () -> new EntidadBeneficiaria(null));
  }

  @Test
  void entidadNueva_estaActiva() {
    assertTrue(new EntidadBeneficiaria(juridica.getId()).estaActivo());
  }

  @Test
  void darDeBajaYReactivar_avisanSoloCuandoHayTransicion() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());

    assertFalse(entidad.reactivar());
    assertTrue(entidad.darDeBaja());
    assertFalse(entidad.darDeBaja());
    assertFalse(entidad.estaActivo());
    assertTrue(entidad.reactivar());
    assertTrue(entidad.estaActivo());
  }

  @Test
  void validarApta_conOng_devuelveLaJuridica() {
    Juridica ong = PersonaMother.fundacionEsperanza();

    assertSame(ong, EntidadBeneficiaria.validarApta(ong));
  }

  @Test
  void validarApta_conInstitucion_esValido() {
    Juridica institucion = juridicaDeTipo(TipoJuridico.INSTITUCION);

    assertEquals(institucion.getId(), EntidadBeneficiaria.validarApta(institucion).getId());
  }

  @Test
  void validarApta_conGubernamental_esValido() {
    Juridica gubernamental = juridicaDeTipo(TipoJuridico.GUBERNAMENTAL);

    assertEquals(gubernamental.getId(), EntidadBeneficiaria.validarApta(gubernamental).getId());
  }

  @Test
  void validarApta_conEmpresa_lanzaTipoInvalido() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> EntidadBeneficiaria.validarApta(juridica));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO, ex.getError());
  }

  @Test
  void validarApta_sinDireccion_lanzaSinDireccion() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    ong.actualizarDireccion(null);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> EntidadBeneficiaria.validarApta(ong));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_DIRECCION, ex.getError());
  }

  @Test
  void validarApta_conPersonaHumana_lanzaSinPersonaJuridica() {
    Humana humana = PersonaMother.juanPerez();

    ValidationException ex =
        assertThrows(ValidationException.class, () -> EntidadBeneficiaria.validarApta(humana));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA, ex.getError());
  }

  @Test
  void validarApta_conPersonaNula_lanzaSinPersonaJuridica() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> EntidadBeneficiaria.validarApta(null));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA, ex.getError());
  }

  @Test
  void validarRequisitos_conTipoYDireccionValidos_noLanza() {
    EntidadBeneficiaria.validarRequisitos(TipoJuridico.ONG, true);
    EntidadBeneficiaria.validarRequisitos(TipoJuridico.INSTITUCION, true);
    EntidadBeneficiaria.validarRequisitos(TipoJuridico.GUBERNAMENTAL, true);
  }

  @Test
  void validarRequisitos_conEmpresa_lanzaTipoInvalido() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> EntidadBeneficiaria.validarRequisitos(TipoJuridico.EMPRESA, true));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO, ex.getError());
  }

  @Test
  void validarRequisitos_sinDireccion_lanzaSinDireccion() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> EntidadBeneficiaria.validarRequisitos(TipoJuridico.ONG, false));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_DIRECCION, ex.getError());
  }

  private static Juridica juridicaDeTipo(TipoJuridico tipo) {
    Juridica j = new Juridica(PersonaMother.mariaGomez(), "Organización", tipo, "Social");
    j.actualizarDireccion(PersonaMother.direccionValida());
    return j;
  }
}
