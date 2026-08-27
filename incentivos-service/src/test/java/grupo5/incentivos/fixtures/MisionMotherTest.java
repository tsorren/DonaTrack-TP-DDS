package grupo5.incentivos.fixtures;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;

public final class MisionMotherTest {

  private MisionMotherTest() {}

  public static MisionRacha rachaColaborador(int meses) {
    return new MisionRacha(CategoriaDonante.COLABORADOR, meses);
  }

  public static MisionRacha rachaConInsignia(
      CategoriaDonante cat, int meses, String nombreInsignia) {
    MisionRacha m = new MisionRacha(cat, meses);
    m.setInsignia(new Insignia(nombreInsignia, "Descripcion de " + nombreInsignia, "/icon.png"));
    return m;
  }

  public static MisionDonacionesExitosas exitosas(CategoriaDonante cat, int n) {
    return new MisionDonacionesExitosas(cat, n);
  }

  public static MisionDonacionesExitosas exitosasConInsignia(
      CategoriaDonante cat, int n, String nombreInsignia) {
    MisionDonacionesExitosas m = new MisionDonacionesExitosas(cat, n);
    m.setInsignia(new Insignia(nombreInsignia, "Descripcion de " + nombreInsignia, "/icon.png"));
    return m;
  }

  public static MisionCompletitud completitud(CategoriaDonante cat, int subcategorias) {
    return new MisionCompletitud(cat, subcategorias);
  }

  public static MisionCompletitud completitudConInsignia(
      CategoriaDonante cat, int subcategorias, String nombreInsignia) {
    MisionCompletitud m = new MisionCompletitud(cat, subcategorias);
    m.setInsignia(new Insignia(nombreInsignia, "Descripcion de " + nombreInsignia, "/icon.png"));
    return m;
  }

  public static MisionHabilDonador habilDonador(CategoriaDonante cat, int bienes) {
    return new MisionHabilDonador(cat, bienes);
  }

  public static MisionHabilDonador habilDonadorConInsignia(
      CategoriaDonante cat, int bienes, String nombreInsignia) {
    MisionHabilDonador m = new MisionHabilDonador(cat, bienes);
    m.setInsignia(new Insignia(nombreInsignia, "Descripcion de " + nombreInsignia, "/icon.png"));
    return m;
  }
}
