package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Characterization test: fija el comportamiento de {@link ValidadorPersonaDuplicada} ya adelgazado
 * (RF Oleada 1 - Persona). Ahora la comparación par a par vive en {@code Persona.esDuplicadaDe};
 * esta clase solo recorre el repositorio.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorPersonaDuplicadaTest {

  @Mock private IPersonasRepository personasRepository;

  private ValidadorPersonaDuplicada validador;

  @BeforeEach
  void setUp() {
    validador = new ValidadorPersonaDuplicada(personasRepository);
  }

  private static Humana humanaConDocumento(String documento) {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, 1, 1));
    humana.actualizarDocumento(TipoDocumento.DNI, documento);
    return humana;
  }

  @Test
  void buscarDuplicado_conPersonaExistenteConMismoDocumento_deberiaRetornarla() {
    Persona personaExistente = humanaConDocumento("30111222");
    Persona personaAImportar = humanaConDocumento("30111222");
    when(personasRepository.findAll()).thenReturn(List.of(personaExistente));

    Optional<Persona> resultado = validador.buscarDuplicado(personaAImportar);

    assertTrue(resultado.isPresent());
    assertEquals(personaExistente, resultado.get());
  }

  @Test
  void buscarDuplicado_sinNingunaPersonaCoincidente_deberiaRetornarVacio() {
    Persona personaExistente = humanaConDocumento("40222333");
    Persona personaAImportar = humanaConDocumento("30111222");
    when(personasRepository.findAll()).thenReturn(List.of(personaExistente));

    Optional<Persona> resultado = validador.buscarDuplicado(personaAImportar);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void buscarDuplicado_conRepositorioVacio_deberiaRetornarVacio() {
    Persona personaAImportar = humanaConDocumento("30111222");
    when(personasRepository.findAll()).thenReturn(List.of());

    Optional<Persona> resultado = validador.buscarDuplicado(personaAImportar);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void buscarDuplicado_conVariasPersonasYSoloUnaCoincide_deberiaRetornarLaQueCoincide() {
    Persona otraPersona = humanaConDocumento("40222333");
    Persona personaExistente = humanaConDocumento("30111222");
    Persona personaAImportar = humanaConDocumento("30111222");
    when(personasRepository.findAll()).thenReturn(List.of(otraPersona, personaExistente));

    Optional<Persona> resultado = validador.buscarDuplicado(personaAImportar);

    assertTrue(resultado.isPresent());
    assertEquals(personaExistente, resultado.get());
  }
}
