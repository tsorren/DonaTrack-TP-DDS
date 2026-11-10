package grupo5.cliente.cuentas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.cliente.cuentas.Perfil.DonanteEmpresa;
import grupo5.cliente.cuentas.Perfil.DonantePersona;
import grupo5.cliente.cuentas.Perfil.Entidad;
import grupo5.cliente.puertos.DirectorioDeCuentas;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.puertos.PersonaNueva.Humana;
import grupo5.cliente.puertos.PersonaNueva.Juridica;
import grupo5.cliente.puertos.PersonasDeDonaciones;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PerfilServiceTest {

  private static final String CUENTA = "cuenta-1";
  private static final String CORREO = "ana@ejemplo.test";
  private static final UUID PERSONA = UUID.randomUUID();
  private static final Entidad ENTIDAD =
      new Entidad(
          "Merendero Oeste",
          "Merendero",
          "institucion",
          "Calle 1",
          1234,
          "1708",
          "Morón",
          "Buenos Aires",
          "Rosa Gómez");

  private final PersonasDeDonaciones donaciones = mock(PersonasDeDonaciones.class);
  private final DirectorioDeCuentas cuentas = mock(DirectorioDeCuentas.class);
  private final PerfilService servicio = new PerfilService(donaciones, cuentas);

  PerfilServiceTest() {
    when(cuentas.correoDe(CUENTA)).thenReturn(CORREO);
    when(donaciones.crear(any())).thenReturn(PERSONA);
  }

  private PersonaNueva personaCreada() {
    ArgumentCaptor<PersonaNueva> persona = ArgumentCaptor.forClass(PersonaNueva.class);
    verify(donaciones).crear(persona.capture());
    return persona.getValue();
  }

  @Test
  void unaPersonaDonanteSeCreaConElCorreoDeLaCuentaYQuedaConSuPlantilla() {
    servicio.completar(CUENTA, new DonantePersona("Ana", "Pérez"));

    assertThat(personaCreada()).isEqualTo(new Humana("Ana", "Pérez", CORREO));
    verify(donaciones).registrarDonante(PERSONA);
    verify(cuentas)
        .vincular(CUENTA, Map.of("personaId", PERSONA.toString()), Optional.of("DONANTE_PERSONA"));
  }

  @Test
  void unaEmpresaDonanteSeCreaConSuRepresentanteYOperaLaJuridica() {
    servicio.completar(CUENTA, new DonanteEmpresa("Acme SA", "Alimentos", "Ana María Pérez"));

    Humana representante = new Humana("Ana María", "Pérez", CORREO);
    Juridica esperada =
        new Juridica("Acme SA", "EMPRESA", "Alimentos", null, representante, CORREO);
    assertThat(personaCreada()).isEqualTo(esperada);
    verify(donaciones).registrarDonante(PERSONA);
    verify(cuentas)
        .vincular(
            CUENTA, Map.of("personaId", PERSONA.toString()), Optional.of("RESPONSABLE_DONANTE"));
  }

  @Test
  void unaEntidadQuedaPendienteSinPlantillaNiAltaDeDonante() {
    servicio.completar(CUENTA, ENTIDAD);

    Juridica juridica = (Juridica) personaCreada();
    assertThat(juridica.tipoJuridico()).isEqualTo("INSTITUCION");
    assertThat(juridica.rubro()).isEqualTo("Merendero");
    assertThat(juridica.direccion().codigoPostal()).isEqualTo("1708");
    assertThat(juridica.direccion().pais()).isEqualTo("Argentina");
    assertThat(juridica.representantes()).containsExactly(new Humana("Rosa", "Gómez", CORREO));
    verify(donaciones, never()).registrarDonante(any());
    verify(cuentas)
        .vincular(
            CUENTA,
            Map.of("personaId", PERSONA.toString(), "solicitudEntidad", "pendiente"),
            Optional.empty());
  }

  @Test
  void siDonacionesFallaAlCrearNoSeTocaLaCuenta() {
    when(donaciones.crear(any())).thenThrow(new IllegalStateException("caído"));

    assertThatThrownBy(() -> servicio.completar(CUENTA, new DonantePersona("Ana", "Pérez")))
        .isInstanceOf(IllegalStateException.class);

    verify(cuentas, never()).vincular(any(), any(), any());
    verify(donaciones, never()).eliminar(any());
  }

  @Test
  void siFallaElAltaDelDonanteSeDaDeBajaLaPersona() {
    doThrow(new IllegalStateException("409")).when(donaciones).registrarDonante(PERSONA);

    assertThatThrownBy(() -> servicio.completar(CUENTA, new DonantePersona("Ana", "Pérez")))
        .isInstanceOf(IllegalStateException.class);

    verify(donaciones).eliminar(PERSONA);
    verify(cuentas, never()).vincular(any(), any(), any());
  }

  @Test
  void siFallaKeycloakSeDaDeBajaLaPersonaYGuardarDeNuevoNoDejaDuplicados() {
    doThrow(new IllegalStateException("Keycloak caído"))
        .doNothing()
        .when(cuentas)
        .vincular(any(), any(), any());
    Perfil perfil = new DonanteEmpresa("Acme SA", "Alimentos", "Ana Pérez");

    assertThatThrownBy(() -> servicio.completar(CUENTA, perfil))
        .isInstanceOf(IllegalStateException.class);
    servicio.completar(CUENTA, perfil);

    verify(donaciones, times(1)).eliminar(PERSONA);
    verify(donaciones, times(2)).crear(any());
    verify(cuentas, times(2)).vincular(any(), any(), any());
  }

  @Test
  void elNombreCompletoSeParteEnElUltimoApellido() {
    assertThat(Humana.deNombreCompleto("  Ana María   de la Pérez ", CORREO))
        .isEqualTo(new Humana("Ana María   de la", "Pérez", CORREO));
    assertThat(Humana.deNombreCompleto("Ana", CORREO)).isEqualTo(new Humana("Ana", "", CORREO));
  }
}
