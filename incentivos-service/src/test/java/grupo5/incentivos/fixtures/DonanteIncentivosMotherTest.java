package grupo5.incentivos.fixtures;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DonanteIncentivosMotherTest {

  public static final UUID ID_DEFAULT = new UUID(0L, 1L);

  private DonanteIncentivosMotherTest() {}

  public static DonanteIncentivos colaboradorSinMisiones() {
    return new DonanteIncentivos(ID_DEFAULT, ID_DEFAULT, "Test", List.of());
  }

  public static DonanteIncentivos colaboradorSinMisiones(UUID id) {
    return new DonanteIncentivos(id, id, "Test", List.of());
  }

  public static DonanteIncentivos colaboradorSinMisiones(UUID id, UUID personaId) {
    return new DonanteIncentivos(id, personaId, "Test", List.of());
  }

  public static DonanteIncentivos colaboradorSinMisiones(UUID id, String nombre) {
    return new DonanteIncentivos(id, id, nombre, List.of());
  }

  public static DonanteIncentivos colaboradorSinMisiones(UUID id, UUID personaId, String nombre) {
    return new DonanteIncentivos(id, personaId, nombre, List.of());
  }

  public static DonanteIncentivos colaboradorConMisionRacha(int mesesObjetivo) {
    return new DonanteIncentivos(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test",
        List.of(MisionMotherTest.rachaColaborador(mesesObjetivo)));
  }

  public static DonanteIncentivos conMisiones(List<Mision> misiones) {
    return new DonanteIncentivos(UUID.randomUUID(), UUID.randomUUID(), "Test", misiones);
  }

  public static DonanteIncentivos conMisiones(UUID id, List<Mision> misiones) {
    return new DonanteIncentivos(id, id, "Test", misiones);
  }

  public static DonanteIncentivos conMisiones(
      UUID id, UUID personaId, String nombre, List<Mision> misiones) {
    return new DonanteIncentivos(id, personaId, nombre, misiones);
  }

  public static DonanteIncentivos conDonacionEnFecha(LocalDate fecha) {
    DonanteIncentivos d = colaboradorSinMisiones();
    d.registrarDonacion(EventoDonacionMotherTest.enFecha(fecha));
    return d;
  }

  public static DonanteIncentivos conDonacion(UUID id, EventoDonacion evento) {
    DonanteIncentivos d = colaboradorSinMisiones(id);
    d.registrarDonacion(evento);
    return d;
  }

  public static DonanteIncentivos conMisionesCompletadasEnMes(
      UUID id, String nombre, YearMonth periodo, int cantidadMisiones) {
    List<Mision> misiones = new ArrayList<>();
    DonanteIncentivos donante = new DonanteIncentivos(id, id, nombre, List.of());
    for (int i = 0; i < cantidadMisiones; i++) {
      MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
      EventoDonacion evento =
          EventoDonacion.builder()
              .donacionId(new UUID(0L, (long) i + 1))
              .fecha(LocalDate.of(periodo.getYear(), periodo.getMonthValue(), 15))
              .cantidadBienes(1)
              .categorias(List.of("alimentos"))
              .build();
      mision.evaluarProgreso(donante, evento);
      misiones.add(mision);
    }
    return new DonanteIncentivos(id, id, nombre, misiones);
  }
}
