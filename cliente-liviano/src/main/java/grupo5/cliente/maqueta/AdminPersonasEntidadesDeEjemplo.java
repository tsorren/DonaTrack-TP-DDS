package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminEntidadesVista.Entidad;
import grupo5.cliente.vistas.AdminEntidadesVista.Necesidad;
import java.util.List;

/**
 * Las entidades de ejemplo del admin: 3 piden sumarse, 46 activas (se ven 8; 6 con necesidades
 * vigentes) y 3 inactivas, coherentes con la ficha y con las páginas públicas, ya en orden
 * alfabético. Hogar Ejemplo Norte está dos veces: pendiente y, después de aprobarla, activa (la
 * plantilla elige cuál se ve).
 */
final class AdminPersonasEntidadesDeEjemplo {

  private static final String ALIMENTOS = "alimentos";
  private static final String ROPA = "ropa";
  private static final String MUEBLES = "muebles";
  private static final String ESCOLARES = "escolares";
  private static final String LOCALIDAD = "Localidad Ejemplo";
  private static final String HOGAR = "Hogar";
  private static final String CLUB = "Club";
  private static final String COMEDOR = "Comedor";
  private static final String MERENDERO = "Merendero";
  private static final String ESCUELA = "Escuela";

  /** Cada dato de la ficha es un parámetro de su fábrica: se lee mejor así (S107). */
  private static final String MUCHOS_PARAMETROS = "java:S107";

  static final AdminEntidadesVista ENTIDADES =
      new AdminEntidadesVista(
          46,
          "15 de octubre de 2026, 18:30",
          List.of(
              activa(
                  "biblioteca",
                  "Biblioteca Popular Ejemplo",
                  "Biblioteca",
                  MUEBLES,
                  "Villa Ejemplo",
                  "12 de septiembre",
                  0,
                  1,
                  "Pablo Ejemplo",
                  "biblioteca@ejemplo.org",
                  "Prestamos libros y abrimos la sala de lectura por la tarde para que los chicos"
                      + " hagan la tarea.",
                  new Necesidad(
                      "Sillas", MUEBLES, "Faltan 4 de 8 unidades, hasta el 31 de diciembre")),
              activa(
                  "centro",
                  "Centro Vecinal Ejemplo",
                  "Centro comunitario",
                  ALIMENTOS,
                  LOCALIDAD,
                  "21 de abril",
                  2,
                  11,
                  "Sara Ejemplo",
                  "centro@ejemplo.org",
                  "Organizamos una olla popular los sábados y acompañamos a las familias del barrio"
                      + " con trámites."),
              activa(
                  "club",
                  "Club Ejemplo Norte",
                  CLUB,
                  ROPA,
                  "Ejemplo Norte",
                  "9 de agosto",
                  0,
                  2,
                  "Diego Ejemplo",
                  "club.norte@ejemplo.org",
                  "Damos fútbol y básquet gratis a chicos de 6 a 16 años, con merienda después del"
                      + " entrenamiento.",
                  new Necesidad(
                      "Camisetas deportivas",
                      ROPA,
                      "Faltan 15 unidades, hasta el 30 de noviembre")),
              inactiva(
                  "club-sur",
                  "Club Ejemplo Sur",
                  CLUB,
                  ROPA,
                  "Ejemplo Sur",
                  "5 de abril",
                  "20 de agosto",
                  "Última entrega confirmada el 2 de mayo",
                  "La dimos de baja: no confirmó sus datos en tres meses.",
                  "Ana Ejemplo",
                  "club.sur@ejemplo.org",
                  "Acompañamos a los chicos del barrio con deporte los fines de semana."),
              activa(
                  "comedor",
                  "Comedor Barrio Ejemplo",
                  COMEDOR,
                  ALIMENTOS,
                  "Barrio Ejemplo",
                  "4 de febrero",
                  5,
                  32,
                  "Carmen Ejemplo",
                  "comedor.barrio@ejemplo.org",
                  "Damos almuerzo de lunes a viernes a unas 120 personas del barrio, la mayoría"
                      + " chicos y personas mayores.",
                  new Necesidad("Lentejas secas", ALIMENTOS, "Faltan 30 de 40 kg este mes"),
                  new Necesidad(
                      "Aceite de girasol", ALIMENTOS, "Faltan 20 litros, hasta el 20 de octubre"),
                  new Necesidad("Arroz", ALIMENTOS, "Faltan 25 de 40 kg este mes"),
                  new Necesidad(
                      "Leche en polvo",
                      ALIMENTOS,
                      "Faltan 12 de 24 unidades, hasta el 25 de octubre")),
              pendiente(
                  "comedor-oeste",
                  "Comedor Ejemplo Oeste",
                  COMEDOR,
                  ALIMENTOS,
                  "14 de octubre",
                  "Ayer",
                  null,
                  "Comedor Comunitario Ejemplo Oeste",
                  "30-00000013-0",
                  "Avenida Ejemplo 789, Localidad Ejemplo, Buenos Aires",
                  "Marta Ejemplo",
                  "comedor@ejemplo.org",
                  "constancia.pdf",
                  "Cocinamos la cena para las familias del barrio tres veces por semana."),
              activa(
                  "escuela",
                  "Escuela Ejemplo N.º 12",
                  ESCUELA,
                  ESCOLARES,
                  "Centro Ejemplo",
                  "30 de junio",
                  1,
                  6,
                  "Inés Ejemplo",
                  "escuela12@ejemplo.org",
                  "Escuela primaria con comedor escolar: 340 chicos en dos turnos.",
                  new Necesidad(
                      "Sillas", MUEBLES, "Faltan 14 de 20 unidades, hasta el 30 de noviembre"),
                  new Necesidad(
                      "Útiles escolares", ESCOLARES, "Faltan 4 cajas, hasta el 30 de noviembre")),
              activa(
                  "escuela3",
                  "Escuela Ejemplo N.º 3",
                  ESCUELA,
                  ESCOLARES,
                  LOCALIDAD,
                  "30 de junio",
                  1,
                  4,
                  "Tomás Ejemplo",
                  "escuela3@ejemplo.org",
                  "Escuela secundaria con talleres de oficios a la tarde."),
              activa(
                  "hogar-casa",
                  "Hogar Casa Ejemplo",
                  HOGAR,
                  ROPA,
                  "Parque Ejemplo",
                  "2 de mayo",
                  0,
                  3,
                  "Elena Ejemplo",
                  "hogar.casa@ejemplo.org",
                  "Damos un lugar para vivir a 18 personas mayores sin familia que las acompañe.",
                  new Necesidad(
                      "Frazadas", ROPA, "Faltan 10 de 20 unidades, hasta el 15 de noviembre")),
              pendiente(
                  "hogar",
                  "Hogar Ejemplo Norte",
                  HOGAR,
                  ROPA,
                  "9 de octubre",
                  "Hace 6 días",
                  null,
                  "Asociación Civil Hogar Ejemplo Norte",
                  "30-00000011-0",
                  "Calle Ejemplo 1234, Localidad Ejemplo, Buenos Aires",
                  "Rosa Ejemplo",
                  "hogar@ejemplo.org",
                  "constancia-domicilio.pdf",
                  "Recibimos a chicos y chicas de 4 a 12 años mientras sus familias trabajan."),
              activa(
                  "hogar",
                  "Hogar Ejemplo Norte",
                  HOGAR,
                  ROPA,
                  LOCALIDAD,
                  "15 de octubre",
                  0,
                  0,
                  "Rosa Ejemplo",
                  "hogar@ejemplo.org",
                  "Recibimos a chicos y chicas de 4 a 12 años mientras sus familias trabajan."),
              inactiva(
                  "hogar-oeste",
                  "Hogar Ejemplo Oeste",
                  HOGAR,
                  ROPA,
                  "Ejemplo Oeste",
                  "11 de enero",
                  "1 de octubre",
                  "Última entrega confirmada el 18 de septiembre",
                  "La entidad pidió la baja: se mudó a otra localidad.",
                  "Julio Ejemplo",
                  "hogar.oeste@ejemplo.org",
                  "Dábamos alojamiento transitorio a mujeres con sus hijos."),
              pendiente(
                  "merendero-este",
                  "Merendero Ejemplo Este",
                  MERENDERO,
                  ALIMENTOS,
                  "12 de octubre",
                  "Hace 3 días",
                  "Le falta la constancia de domicilio",
                  "Merendero Ejemplo Este Asociación Civil",
                  "30-00000012-0",
                  "Pasaje Ejemplo 56, Localidad Ejemplo, Buenos Aires",
                  "Luis Ejemplo",
                  "merendero@ejemplo.org",
                  null,
                  "Damos la merienda a unos 60 chicos de lunes a viernes."),
              inactiva(
                  "merendero-norte",
                  "Merendero Ejemplo Norte",
                  MERENDERO,
                  ALIMENTOS,
                  "Ejemplo Norte",
                  "3 de marzo",
                  "3 de julio",
                  "Última entrega confirmada el 21 de junio",
                  "La entidad pidió la baja: cerró el merendero.",
                  "Nora Ejemplo",
                  "merendero.norte@ejemplo.org",
                  "Dábamos la merienda a los chicos del barrio los días de semana."),
              activa(
                  "merendero",
                  "Merendero Ejemplo Sur",
                  MERENDERO,
                  ALIMENTOS,
                  "Ejemplo Sur",
                  "18 de marzo",
                  3,
                  19,
                  "Raúl Ejemplo",
                  "merendero.sur@ejemplo.org",
                  "Damos desayuno y merienda a 80 chicos antes y después de la escuela.",
                  new Necesidad("Galletitas", ALIMENTOS, "Faltan 10 de 20 kg este mes"),
                  new Necesidad(
                      "Leche en polvo", ALIMENTOS, "Faltan 20 unidades, hasta el 31 de octubre"))));

  private AdminPersonasEntidadesDeEjemplo() {}

  @SuppressWarnings(MUCHOS_PARAMETROS)
  private static Entidad activa(
      String clave,
      String nombre,
      String tipo,
      String cat,
      String localidad,
      String alta,
      int entregas,
      int total,
      String responsable,
      String correo,
      String mision,
      Necesidad... necesidades) {
    return new Entidad(
        clave,
        "activa",
        nombre,
        tipo,
        cat,
        localidad,
        responsable,
        correo,
        mision,
        List.of(necesidades),
        alta,
        entregas,
        total,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @SuppressWarnings(MUCHOS_PARAMETROS)
  private static Entidad pendiente(
      String clave,
      String nombre,
      String tipo,
      String cat,
      String cuando,
      String espera,
      String falta,
      String razon,
      String cuit,
      String direccion,
      String responsable,
      String correo,
      String constancia,
      String mision) {
    return new Entidad(
        clave,
        "pendiente",
        nombre,
        tipo,
        cat,
        LOCALIDAD,
        responsable,
        correo,
        mision,
        List.of(),
        null,
        null,
        null,
        cuando,
        espera,
        falta,
        razon,
        cuit,
        direccion,
        constancia,
        null,
        null,
        null);
  }

  @SuppressWarnings(MUCHOS_PARAMETROS)
  private static Entidad inactiva(
      String clave,
      String nombre,
      String tipo,
      String cat,
      String localidad,
      String alta,
      String baja,
      String ultima,
      String motivo,
      String responsable,
      String correo,
      String mision) {
    return new Entidad(
        clave,
        "inactiva",
        nombre,
        tipo,
        cat,
        localidad,
        responsable,
        correo,
        mision,
        List.of(),
        alta,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        baja,
        ultima,
        motivo);
  }
}
