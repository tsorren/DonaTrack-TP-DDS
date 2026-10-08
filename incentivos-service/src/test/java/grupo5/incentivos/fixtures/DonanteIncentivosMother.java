package grupo5.incentivos.fixtures;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DonanteIncentivosMother {

  public static final UUID ID_DEFAULT = new UUID(0L, 1L);

  private DonanteIncentivosMother() {}

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

  public static DonanteIncentivos colaboradorRegistradoEn(UUID id, LocalDate fechaRegistro) {
    return new DonanteIncentivos(id, id, "Test", fechaRegistro);
  }

  public static DonanteIncentivos colaboradorRegistradoEn(
      UUID id, UUID personaId, String nombre, LocalDate fechaRegistro) {
    return new DonanteIncentivos(id, personaId, nombre, fechaRegistro);
  }

  public static DonanteIncentivos colaboradorConMisionRacha(int mesesObjetivo) {
    return new DonanteIncentivos(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test",
        List.of(MisionMother.rachaColaborador(mesesObjetivo)));
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
    d.registrarDonacion(EventoDonacionMother.enFecha(fecha));
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

  /**
   * Donante con filas en todas las tablas hijas (misiones de los cuatro tipos, categorías donadas,
   * insignias, organizaciones ayudadas y donaciones por período). Insignias: "Gran Aporte Test"
   * (visible) y "Extra" (oculta).
   */
  public static DonanteIncentivos conEstadoCompleto(UUID id) {
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 3, "Racha Test");
    racha.setNumeroMision(1);
    MisionCompletitud completitud =
        MisionMother.completitudConInsignia(CategoriaDonante.COLABORADOR, 3, "Explorador Test");
    completitud.setNumeroMision(2);
    MisionHabilDonador habil =
        MisionMother.habilDonadorConInsignia(CategoriaDonante.SOSTENEDOR, 5, "Gran Aporte Test");
    habil.setNumeroMision(3);
    MisionDonacionesExitosas exitosas =
        MisionMother.exitosasConInsignia(CategoriaDonante.TRANSFORMADOR, 3, "Impacto Test");
    exitosas.setNumeroMision(4);

    DonanteIncentivos donante =
        new DonanteIncentivos(
            id, UUID.randomUUID(), "Ana", List.of(racha, completitud, habil, exitosas));

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 5, 10));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 6, 10));
    completitud.evaluarProgreso(
        donante,
        EventoDonacionMother.conCategorias(
            LocalDate.of(2026, 6, 10), List.of("Alimentos", "Ropa")));
    habil.evaluarProgreso(
        donante, EventoDonacionMother.conCantidadBienes(LocalDate.of(2026, 6, 11), 5));
    exitosas.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 6, 12));
    exitosas.evaluarProgresoExitoso(donante);

    donante.getMetricas().registrarDonacion(EventoDonacionMother.enFecha(2026, 5, 10));
    donante.getMetricas().registrarDonacion(EventoDonacionMother.enFecha(2026, 6, 11));
    donante.getMetricas().registrarDonacionExitosa(UUID.randomUUID());

    donante.otorgarInsignia(
        new Insignia("Extra", "Descripcion", "/extra.png"), LocalDate.of(2026, 6, 1));
    donante.configurarVisibilidadInsignia("Extra", false);
    return donante;
  }
}
