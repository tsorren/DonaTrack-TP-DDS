package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.impl.PersonasRepositoryEnMemoria;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El validador busca candidatos por documento y por correo (consultas del repositorio) y deja que
 * {@code Persona.esDuplicadaDe} y {@code Persona.entraEnConflictoCon} decidan (Paso 3c).
 */
class ValidadorPersonaDuplicadaTest {

  private PersonasRepositoryEnMemoria personasRepository;
  private ValidadorPersonaDuplicada validador;

  @BeforeEach
  void setUp() {
    personasRepository = spy(new PersonasRepositoryEnMemoria());
    validador = new ValidadorPersonaDuplicada(personasRepository);
  }

  private static Humana humana(String documento, String correo) {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    humana.limpiarMediosDeContacto();
    if (documento != null) {
      humana.actualizarDocumento(TipoDocumento.DNI, documento);
    }
    if (correo != null) {
      humana.agregarMedioDeContacto(correo(correo));
    }
    return humana;
  }

  private static Juridica juridica(String cuit, String correo) {
    Humana representante = new Humana("Ana", "Gomez", LocalDate.of(1985, Month.MAY, 15));
    Juridica juridica = new Juridica(representante, "Empresa SA", TipoJuridico.EMPRESA, "Rubro");
    juridica.actualizarDocumento(TipoDocumento.CUIT, cuit);
    juridica.agregarMedioDeContacto(correo(correo));
    return juridica;
  }

  private static Correo correo(String direccion) {
    Correo correo = new Correo();
    correo.setDireccionCorreo(direccion);
    return correo;
  }

  @Test
  void buscarDuplicado_conMismoDocumento_laEncuentraSinCargarTodasLasPersonas() {
    Persona existente = personasRepository.save(humana("30111222", "juan@mail.com"));

    Optional<Persona> resultado = validador.buscarDuplicado(humana("30111222", "otro@mail.com"));

    assertEquals(Optional.of(existente.getId()), resultado.map(Persona::getId));
    verify(personasRepository, never()).findAll();
  }

  @Test
  void buscarDuplicado_conCorreoRegistradoPorAlguienSinDocumento_laActualiza() {
    Persona existente = personasRepository.save(humana(null, "Juan@Mail.com"));

    Optional<Persona> resultado = validador.buscarDuplicado(humana("30111222", " juan@mail.com "));

    assertEquals(Optional.of(existente.getId()), resultado.map(Persona::getId));
  }

  @Test
  void buscarDuplicado_conCorreoDeOtraJuridicaConOtroCuit_rechazaLaFilaPorConflicto() {
    personasRepository.save(juridica("30-11122233-4", "ana@estudio.com"));
    Juridica otra = juridica("30-99988877-6", "ana@estudio.com");

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> validador.buscarDuplicado(otra));

    assertEquals(ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO, ex.getError());
  }

  @Test
  void buscarDuplicado_siElDocumentoCoincide_ganaAunqueElCorreoSeaDeOtraPersona() {
    Persona porDocumento = personasRepository.save(humana("30111222", null));
    personasRepository.save(humana("40222333", "juan@mail.com"));

    Optional<Persona> resultado = validador.buscarDuplicado(humana("30111222", "juan@mail.com"));

    assertEquals(Optional.of(porDocumento.getId()), resultado.map(Persona::getId));
  }

  @Test
  void buscarDuplicado_sinCoincidencias_devuelveVacio() {
    personasRepository.save(humana("40222333", "otra@mail.com"));

    assertTrue(validador.buscarDuplicado(humana("30111222", "juan@mail.com")).isEmpty());
  }

  @Test
  void buscarDuplicado_filaSinTipoDeDocumento_laEncuentraPorNumeroSiEsUnico() {
    Persona existente = personasRepository.save(humana("30111222", null));
    Humana sinTipo = humana(null, null);
    sinTipo.actualizarDocumento(null, "30111222");

    Optional<Persona> resultado = validador.buscarDuplicado(sinTipo);

    assertEquals(Optional.of(existente.getId()), resultado.map(Persona::getId));
  }

  @Test
  void buscarDuplicado_filaSinTipoYElNumeroLoTienenDosPersonas_rechazaPorConflicto() {
    personasRepository.save(humana("30111222", null));
    Humana conPasaporte = humana(null, null);
    conPasaporte.actualizarDocumento(TipoDocumento.PASAPORTE, "30111222");
    personasRepository.save(conPasaporte);
    Humana sinTipo = humana(null, null);
    sinTipo.actualizarDocumento(null, "30111222");

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> validador.buscarDuplicado(sinTipo));

    assertEquals(ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO, ex.getError());
  }
}
