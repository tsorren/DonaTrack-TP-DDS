package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.services.IncentivosService.DonanteIncentivosNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncentivosServiceTest {

  @Mock private NotificacionesClient notificacionesClient;

  private IncentivosService service;
  private DonanteIncentivosRepositorygit repository;
  private MisionFactory misionFactory;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    misionFactory = new MisionFactory();
    service = new IncentivosService(repository, misionFactory, notificacionesClient);
  }

  @Test
  void registrarDonante_deberiaCrearPerfilConMisiones() {
    DonanteIncentivos donante = service.registrarDonante(1L, "usuario1");

    assertNotNull(donante);
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());
    assertFalse(donante.getMisiones().isEmpty());
  }

  @Test
  void registrarDonante_deberiaSerIdempotente() {
    service.registrarDonante(1L, "usuario1");
    service.registrarDonante(1L, "usuario1"); // segunda llamada

    assertEquals(1, repository.listarTodos().size());
  }

  @Test
  void procesarDonacion_deberiaNotificarCuandoSeCompletaUnaMision() {

    DonanteIncentivos donante = new DonanteIncentivos(42L);
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    Insignia insignia = new Insignia("Primera Entrega", "Primera donación exitosa", "/img.png");
    mision.setInsignia(insignia);
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("fideos")
            .build();

    service.procesarDonacion(42L, "usuario42", evento);

    verify(notificacionesClient, atLeastOnce())
        .notificarMisionCumplida(anyLong(), anyString(), anyString());
  }

  @Test
  void procesarDonacion_noDeberiaNotificarCuandoLaMisionNoSeCompletaAun() {
    DonanteIncentivos donante = new DonanteIncentivos(43L);
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 5);
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("fideos")
            .build();

    service.procesarDonacion(43L, "usuario43", evento);

    verify(notificacionesClient, never()).notificarMisionCumplida(any(), any(), any());
  }

  @Test
  void procesarDonacion_deberiaNotificarAscensoAlSubirCategoria() {
    DonanteIncentivos donante = new DonanteIncentivos(44L);
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    donante.getMisiones().add(racha);
    repository.guardar(donante);

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(1L)
            .fecha(LocalDate.now())
            .exitosa(true)
            .cantidadBienes(1)
            .subcategoria("x")
            .build();

    service.procesarDonacion(44L, "usuario44", evento);

    verify(notificacionesClient, atLeastOnce()).notificarAscensoCategoria(anyLong(), anyString());
  }

  @Test
  void obtenerDonante_deberiaLanzarExcepcionSiNoExiste() {
    assertThrows(DonanteIncentivosNotFoundException.class, () -> service.obtenerDonante(999L));
  }
}
