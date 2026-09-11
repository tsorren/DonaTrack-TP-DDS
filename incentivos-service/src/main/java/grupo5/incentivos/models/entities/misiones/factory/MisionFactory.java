package grupo5.incentivos.models.entities.misiones.factory;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.util.ArrayList;
import java.util.List;

public final class MisionFactory {

  private MisionFactory() {}

  public static List<Mision> crearMisionesEstandar() {
    List<Mision> misiones = new ArrayList<>();

    // === CATEGORÍA COLABORADOR ===
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 3);
    racha.setNumeroMision(1);
    racha.setInsignia(
        new Insignia(
            "Racha Inicial", "3 meses consecutivos donando", "/insignias/racha-colaborador.png"));
    misiones.add(racha);

    MisionCompletitud completitud = new MisionCompletitud(CategoriaDonante.COLABORADOR, 3);
    completitud.setNumeroMision(2);
    completitud.setInsignia(
        new Insignia(
            "Explorador", "Donaciones en 3 categorías distintas", "/insignias/explorador.png"));
    misiones.add(completitud);

    // === CATEGORÍA SOSTENEDOR ===
    MisionHabilDonador habil = new MisionHabilDonador(CategoriaDonante.SOSTENEDOR, 50);
    habil.setNumeroMision(3);
    habil.setInsignia(
        new Insignia(
            "Gran Aporte", "Una donación con al menos 50 bienes", "/insignias/gran-aporte.png"));
    misiones.add(habil);

    MisionRacha rachaSostenedor = new MisionRacha(CategoriaDonante.SOSTENEDOR, 6);
    rachaSostenedor.setNumeroMision(4);
    rachaSostenedor.setInsignia(
        new Insignia("Constancia", "6 meses consecutivos donando", "/insignias/constancia.png"));
    misiones.add(rachaSostenedor);

    // === CATEGORÍA TRANSFORMADOR ===
    MisionDonacionesExitosas exitosas =
        new MisionDonacionesExitosas(CategoriaDonante.TRANSFORMADOR, 10);
    exitosas.setNumeroMision(5);
    exitosas.setInsignia(
        new Insignia(
            "Impacto Real", "10 donaciones confirmadas recibidas", "/insignias/impacto-real.png"));
    misiones.add(exitosas);

    MisionCompletitud completitudTransformador =
        new MisionCompletitud(CategoriaDonante.TRANSFORMADOR, 8);
    completitudTransformador.setNumeroMision(6);
    completitudTransformador.setInsignia(
        new Insignia(
            "Diversidad Solidaria",
            "Donaciones en 8 categorías distintas",
            "/insignias/diversidad.png"));
    misiones.add(completitudTransformador);

    return misiones;
  }
}
