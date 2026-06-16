package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteEstadosTest {

  private static final String ACTOR = "SISTEMA";
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private DonacionIndependiente donacion;

  private NecesidadExtraordinaria receptor;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    Donacion donacionOriginal = new Donacion(donante);

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Ropa de Invierno");
    Bien bien = new Bien("Abrigo", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bien, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacionOriginal, bienNormalizado, 5);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado.getBien(), 5);

    donacion = new DonacionIndependiente(donacionOriginal, List.of(item));
  }

  @Test
  void nuevaDonacionIndependiente_comienzaEnEstadoEnDeposito() {
    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
  }

  @Test
  void asignar_desdeEnDeposito_transicionaCorrectamente() {
    donacion.asignar(ACTOR, receptor);
    assertTrue(donacion.getHistorial().size() == 1);
  }

  @Test
  void planificarRuta_luegoDe_asignar_transicionaAEnTraslado() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertInstanceOf(EnTraslado.class, donacion.getEstadoActual());
  }

  @Test
  void iniciarRecorrido_luegoDePlanificarRuta_transicionaAListaParaEntregar() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    assertInstanceOf(ListaParaEntregar.class, donacion.getEstadoActual());
  }

  @Test
  void registrarFalla_desdeListaParaEntregar_transicionaAEntregaFallida() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("No había nadie", ACTOR);
    assertInstanceOf(EntregaFallida.class, donacion.getEstadoActual());
  }

  @Test
  void retornar_desdeEntregaFallida_vuelveAEnDeposito() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.registrarFalla("No había nadie", ACTOR);
    donacion.retornar(ACTOR);
    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
  }

  @Test
  void historial_registraCadaTransicion() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertEquals(3, donacion.getHistorial().size());
  }

  @Test
  void historial_registraElActorEnCadaCambio() {
    donacion.asignar("admin1", receptor);
    assertEquals("admin1", donacion.getHistorial().getFirst().getActor());
  }

  @Test
  void vencer_desdeEnDeposito_transicionaCorrectamente() {
    donacion.vencer(ACTOR);
    assertFalse(donacion.getHistorial().isEmpty());
  }

  @Test
  void transicionInvalida_desdeEnDeposito_lanzaExcepcion() {
    assertThrows(BusinessStateException.class, () -> donacion.confirmarEntrega(ACTOR));
  }

  @Test
  void registrarFalla_sinJustificacion_lanzaExcepcion() {
    donacion.asignar(ACTOR, receptor);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    assertThrows(Exception.class, () -> donacion.registrarFalla(null, ACTOR));
  }
}
