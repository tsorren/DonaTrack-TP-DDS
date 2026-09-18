package grupo5.incentivos.models.entities.inactividad;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestorDeInactivosTest {

  private final GestorDeInactivos gestor = new GestorDeInactivos();

  private CriterioInactividad criterioQueDetecta(DonanteIncentivos donante, int diasInactivo) {
    return new CriterioInactividad() {
      @Override
      public List<DonanteInactivo> detectarInactivos(List<DonanteIncentivos> donantes) {
        return List.of(
            new DonanteInactivo(
                donante.getId(), donante.getIdPersona(), diasInactivo, LocalDate.now()));
      }
    };
  }

  @Test
  void procesarInactividad_conCriterioNuloEnLaLista_deberiaIgnorarloYSeguirConLosDemas() {
    UUID id = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(id, personaId);
    CriterioInactividad criterioValido = criterioQueDetecta(donante, 40);

    // Arrays.asList permite elementos nulos (a diferencia de List.of)
    List<CriterioInactividad> criterios = Arrays.asList(null, criterioValido);

    List<DonanteInactivo> resultado = gestor.procesarInactividad(criterios, List.of(donante));

    assertEquals(1, resultado.size());
    assertEquals(40, resultado.getFirst().diasInactivo());
  }

  @Test
  void procesarInactividad_conCriterioQueDevuelveNulo_deberiaIgnorarloSinFallar() {
    UUID id = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(id, personaId);

    CriterioInactividad criterioSinResultado =
        new CriterioInactividad() {
          @Override
          public List<DonanteInactivo> detectarInactivos(List<DonanteIncentivos> donantes) {
            return null;
          }
        };
    CriterioInactividad criterioValido = criterioQueDetecta(donante, 25);

    List<DonanteInactivo> resultado =
        gestor.procesarInactividad(List.of(criterioSinResultado, criterioValido), List.of(donante));

    assertEquals(1, resultado.size());
    assertEquals(25, resultado.getFirst().diasInactivo());
  }

  @Test
  void procesarInactividad_conDosCriteriosParaElMismoDonante_deberiaConservarElMayorNumeroDeDias() {
    UUID id = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(id, personaId);

    // El segundo criterio detecta MÁS días que el primero: debe reemplazar al existente.
    CriterioInactividad criterioConMenosDias = criterioQueDetecta(donante, 30);
    CriterioInactividad criterioConMasDias = criterioQueDetecta(donante, 90);

    List<DonanteInactivo> resultado =
        gestor.procesarInactividad(
            List.of(criterioConMenosDias, criterioConMasDias), List.of(donante));

    assertEquals(1, resultado.size());
    assertEquals(90, resultado.getFirst().diasInactivo());
  }

  @Test
  void
      procesarInactividad_conDosCriteriosParaElMismoDonanteEnOrdenInverso_deberiaConservarElMayorNumeroDeDias() {
    UUID id = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(id, personaId);

    // El segundo criterio detecta MENOS días que el existente: debe conservarse el existente.
    CriterioInactividad criterioConMasDias = criterioQueDetecta(donante, 90);
    CriterioInactividad criterioConMenosDias = criterioQueDetecta(donante, 30);

    List<DonanteInactivo> resultado =
        gestor.procesarInactividad(
            List.of(criterioConMasDias, criterioConMenosDias), List.of(donante));

    assertEquals(1, resultado.size());
    assertEquals(90, resultado.getFirst().diasInactivo());
  }
}
