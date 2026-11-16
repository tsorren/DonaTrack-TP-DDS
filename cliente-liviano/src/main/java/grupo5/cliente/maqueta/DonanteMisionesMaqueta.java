package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteMisionesPuerto;
import grupo5.cliente.vistas.DonanteMisionesVista;
import grupo5.cliente.vistas.DonanteMisionesVista.Categoria;
import grupo5.cliente.vistas.DonanteMisionesVista.Historia;
import grupo5.cliente.vistas.DonanteMisionesVista.Hito;
import grupo5.cliente.vistas.DonanteMisionesVista.Mision;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las misiones de Martina, la persona de ejemplo (ficha raices-misiones.md): las seis de {@code
 * MisionFactory} (incentivos-service), con dos completas, y, con {@code ?escenario=vacio}, las de
 * quien recién empieza. Los datos son los que escribían la plantilla y su variante (la huerta);
 * "hoy" es el 15 de octubre de 2026. Que la cuenta sea una persona lo decide {@link
 * CuentaDeEjemplo}.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.misiones",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteMisionesMaqueta implements DonanteMisionesPuerto {

  private static final String COLABORADOR = "Colaborador";
  private static final String SOSTENEDOR = "Sostenedor";
  private static final String TRANSFORMADOR = "Transformador";
  private static final String RACHA_INICIAL = "Racha Inicial";
  private static final String EXPLORADOR = "Explorador";
  private static final String GRAN_APORTE = "Gran Aporte";
  private static final String TU_CATEGORIA = "tu categoría";
  private static final String SE_ABRE_DESPUES = "se abre después";
  private static final String COMPLETA = "completa";
  private static final String CERRADA = "cerrada";
  private static final String ALIMENTOS = "cat-alimentos";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String ESCUELA = "Escuela Ejemplo N.º 12";
  private static final String TE_SUMASTE = "Te sumaste a DonaTrack";
  private static final String HOY = "15 de octubre de 2026, 16:25";

  private static final Mision CONSTANCIA =
      new Mision(
          SOSTENEDOR,
          "Constancia",
          "6 meses seguidos donando",
          CERRADA,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
  private static final Mision IMPACTO_REAL =
      new Mision(
          TRANSFORMADOR,
          "Impacto Real",
          "10 donaciones recibidas, confirmadas por la organización",
          CERRADA,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
  private static final Mision DIVERSIDAD =
      new Mision(
          TRANSFORMADOR,
          "Diversidad Solidaria",
          "Donaciones en 8 categorías distintas",
          CERRADA,
          null,
          null,
          null,
          null,
          null,
          null,
          null);

  private static final DonanteMisionesVista CON_DATOS =
      new DonanteMisionesVista(
          null,
          false,
          "Martina",
          SOSTENEDOR,
          "2 de septiembre de 2026",
          2,
          HOY,
          List.of(
              completa(
                  RACHA_INICIAL,
                  "3 meses seguidos donando",
                  "3 de agosto de 2026",
                  new Historia(
                      "Donaste 3 meses seguidos: junio, julio y agosto de 2026. Tus galletitas completaron la misión.",
                      List.of(
                          new Hito(
                              "Llevaste tus galletitas al depósito", "3 de agosto de 2026", true),
                          new Hito("Llegaron a " + MERENDERO, "7 de agosto de 2026, 10:30", true),
                          new Hito(
                              MERENDERO + " confirmó que las recibió",
                              "7 de agosto de 2026, 17:40",
                              true)),
                      "galletitas",
                      MERENDERO,
                      "Las galletitas acompañaron la merienda del viernes. ¡Gracias por acordarte de nosotros!",
                      ALIMENTOS)),
              completa(
                  EXPLORADOR,
                  "Donaciones en 3 categorías distintas",
                  "2 de septiembre de 2026",
                  new Historia(
                      "Donaste en 3 categorías distintas: alimentos, ropa y útiles escolares. Los útiles completaron la misión.",
                      List.of(
                          new Hito(
                              "Llevaste tus útiles escolares al depósito",
                              "2 de septiembre de 2026",
                              true),
                          new Hito("Llegaron a " + ESCUELA, "8 de septiembre de 2026, 9:00", true),
                          new Hito(
                              ESCUELA + " confirmó que los recibió",
                              "8 de septiembre de 2026, 12:30",
                              true)),
                      "utiles",
                      ESCUELA,
                      "Los útiles ya están en las mochilas de segundo grado. ¡Gracias!",
                      "cat-escolares")),
              new Mision(
                  SOSTENEDOR,
                  GRAN_APORTE,
                  "Una donación con al menos 50 bienes",
                  "curso",
                  null,
                  12,
                  50,
                  "12 de 50 bienes",
                  "Tu donación más grande desde el 2 de septiembre.",
                  null,
                  null),
              new Mision(
                  SOSTENEDOR,
                  "Constancia",
                  "6 meses seguidos donando",
                  "espera",
                  null,
                  null,
                  null,
                  null,
                  null,
                  GRAN_APORTE,
                  null),
              IMPACTO_REAL,
              DIVERSIDAD),
          List.of(
              new Categoria(COLABORADOR, COMPLETA, null, null),
              new Categoria(SOSTENEDOR, TU_CATEGORIA, TRANSFORMADOR, null),
              new Categoria(TRANSFORMADOR, SE_ABRE_DESPUES, null, SOSTENEDOR)),
          new Historia(
              "Te sumaste a DonaTrack y donaste por primera vez: lentejas secas, 4 kg, para "
                  + COMEDOR
                  + ".",
              List.of(
                  new Hito(TE_SUMASTE, "1 de junio de 2026", true),
                  new Hito("Llevaste tus lentejas al depósito", "5 de junio de 2026", true),
                  new Hito(COMEDOR + " confirmó que las recibió", "9 de junio de 2026", true)),
              "lentejas",
              COMEDOR,
              "Gracias por las lentejas, Martina: alcanzaron para el guiso de toda la semana.",
              ALIMENTOS),
          null);

  private static final DonanteMisionesVista NUEVA =
      new DonanteMisionesVista(
          null,
          true,
          "Martina",
          COLABORADOR,
          null,
          0,
          HOY,
          List.of(
              new Mision(
                  COLABORADOR,
                  RACHA_INICIAL,
                  "3 meses seguidos donando",
                  "curso",
                  null,
                  0,
                  3,
                  "0 de 3 meses",
                  null,
                  null,
                  null),
              new Mision(
                  COLABORADOR,
                  EXPLORADOR,
                  "Donaciones en 3 categorías distintas",
                  "espera",
                  null,
                  null,
                  null,
                  null,
                  null,
                  RACHA_INICIAL,
                  null),
              new Mision(
                  SOSTENEDOR,
                  GRAN_APORTE,
                  "Una donación con al menos 50 bienes",
                  CERRADA,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null),
              CONSTANCIA,
              IMPACTO_REAL,
              DIVERSIDAD),
          List.of(
              new Categoria(COLABORADOR, TU_CATEGORIA, SOSTENEDOR, null),
              new Categoria(SOSTENEDOR, SE_ABRE_DESPUES, null, COLABORADOR),
              new Categoria(TRANSFORMADOR, SE_ABRE_DESPUES, null, SOSTENEDOR)),
          new Historia(
              "Te sumaste a DonaTrack. Esta planta llega con tu cuenta.",
              List.of(
                  new Hito(TE_SUMASTE, "15 de octubre de 2026", true),
                  new Hito("Tu primera donación", "Cuando quieras", false),
                  new Hito(
                      "La organización confirma que la recibió",
                      "Después de tu primera donación",
                      false)),
              null,
              COMEDOR,
              null,
              ALIMENTOS),
          null);

  private static Mision completa(String nombre, String pide, String fecha, Historia historia) {
    return new Mision(
        COLABORADOR, nombre, pide, COMPLETA, fecha, null, null, null, null, null, historia);
  }

  @Override
  public DonanteMisionesVista misiones(UUID donante) {
    return Escenario.actual() == Escenario.VACIO ? NUEVA : CON_DATOS;
  }
}
