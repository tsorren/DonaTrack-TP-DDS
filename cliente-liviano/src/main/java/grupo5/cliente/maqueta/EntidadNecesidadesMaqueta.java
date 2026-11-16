package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.vistas.EntidadNecesidadesVista;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Donacion;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Entrega;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Grupo;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Necesidad;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Opcion;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Periodo;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Rubro;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Terminada;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las necesidades del Comedor Barrio Ejemplo el 15 de octubre de 2026, las del primer prototipo con
 * las decisiones del 2026-11-02. Fideos secos es la que se publica en {@code publicada}; después de
 * {@code cambiada}, Lentejas secas pide 45 kg. Quien ocultó su nombre figura con su categoría del
 * ranking. Publicar, cambiar y dar de baja no guardan nada: en la maqueta los formularios los
 * atiende SitioController.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadNecesidadesMaqueta implements EntidadNecesidadesPuerto {

  private static final String ALIMENTOS = "Alimentos";
  private static final String CAT_ALIMENTOS = "cat-alimentos";
  private static final String UNA_VEZ = "Una sola vez";
  private static final String CADA_MES = "Cada mes";
  private static final String FIN_DE_OCTUBRE = "Octubre, hasta el 31";
  private static final String FALTAN_16 = "faltan 16 días";
  private static final String KG = "kg";
  private static final String UNIDADES = "unidades";
  private static final String ALMUERZO = "Para el almuerzo de lunes a viernes.";
  private static final String DISTRIBUIDORA = "Distribuidora Ejemplo SA";
  private static final String TODO = "todo";
  private static final String LLEGO_TODO = "Llegó todo";
  private static final String PARTE = "parte";
  private static final String UNA_PARTE = "Llegó una parte";
  private static final String SEPTIEMBRE = "Septiembre";
  private static final String AGOSTO = "Agosto";
  private static final String CUARENTA_KG = "40 de 40 kg";

  @Override
  public EntidadNecesidadesVista necesidades(UUID entidad, String accion) {
    boolean cambiada = "cambiada".equals(accion);
    return new EntidadNecesidadesVista(
        "15 de octubre de 2026",
        "2026-10-15",
        "15 de octubre",
        List.of(
            new Necesidad(
                "aceite",
                null,
                null,
                "Aceite de girasol",
                CAT_ALIMENTOS,
                ALIMENTOS,
                "litros",
                "Hasta el 20 de octubre",
                "faltan 5 días",
                UNA_VEZ,
                0,
                20,
                null,
                null,
                "Para cocinar el almuerzo.",
                0,
                0,
                1,
                List.of(
                    new Entrega(
                        "10 litros",
                        "con-problema",
                        "El camión no pudo llegar el 14 de octubre. Te avisamos la nueva fecha.",
                        null)),
                List.of(),
                null,
                List.of()),
            new Necesidad(
                "leche",
                null,
                null,
                "Leche en polvo",
                CAT_ALIMENTOS,
                ALIMENTOS,
                UNIDADES,
                "Hasta el 25 de octubre",
                "faltan 10 días",
                UNA_VEZ,
                12,
                24,
                null,
                null,
                "Para la merienda de los más chicos.",
                0,
                1,
                0,
                List.of(
                    new Entrega(
                        "12 unidades", "pendiente", "Sale del depósito el 17 de octubre.", null)),
                List.of(
                    new Donacion("Lácteos Ejemplo SA", null, null, "12 unidades", "6 de octubre")),
                null,
                List.of()),
            new Necesidad(
                "fideos",
                null,
                null,
                "Fideos secos",
                CAT_ALIMENTOS,
                ALIMENTOS,
                KG,
                "Hasta el 30 de octubre",
                "faltan 15 días",
                UNA_VEZ,
                0,
                30,
                null,
                null,
                "Para la cena de los viernes.",
                0,
                0,
                0,
                List.of(),
                List.of(),
                null,
                List.of()),
            new Necesidad(
                "lentejas",
                null,
                null,
                "Lentejas secas",
                CAT_ALIMENTOS,
                ALIMENTOS,
                KG,
                FIN_DE_OCTUBRE,
                FALTAN_16,
                CADA_MES,
                10,
                cambiada ? 45 : 40,
                cambiada
                    ? "Hoy pasó de 40 a 45 kg por mes. Vale desde octubre: lo que ya llegó cuenta."
                    : null,
                cambiada ? "Cambiada hoy" : null,
                ALMUERZO,
                1,
                0,
                0,
                List.of(
                    new Entrega(
                        "15 kg",
                        "puerta",
                        "Llegaron a tu puerta hoy a las 18:20 y esperan tu confirmación.",
                        "lentejas")),
                List.of(new Donacion(DISTRIBUIDORA, null, null, "10 kg", "10 de octubre")),
                "Desde junio de 2026",
                List.of(
                    new Periodo(
                        "Octubre",
                        "del 1 al 31",
                        cambiada ? "10 de 45 kg" : "10 de 40 kg",
                        "curso",
                        "En curso",
                        10,
                        null),
                    new Periodo(SEPTIEMBRE, null, CUARENTA_KG, TODO, LLEGO_TODO, 40, 40),
                    new Periodo(AGOSTO, null, "22 de 40 kg", PARTE, UNA_PARTE, 22, 40),
                    new Periodo("Julio", null, "0 de 40 kg", "nada", "No llegó nada", 0, 40),
                    new Periodo("Junio", null, CUARENTA_KG, TODO, LLEGO_TODO, 40, 40))),
            new Necesidad(
                "arroz",
                null,
                null,
                "Arroz",
                CAT_ALIMENTOS,
                ALIMENTOS,
                KG,
                FIN_DE_OCTUBRE,
                FALTAN_16,
                CADA_MES,
                15,
                40,
                null,
                null,
                ALMUERZO,
                0,
                1,
                0,
                List.of(
                    new Entrega(
                        "20 kg",
                        "en-camino",
                        "Salió del depósito hoy a las 9:15. Llega mañana.",
                        null)),
                List.of(
                    new Donacion(DISTRIBUIDORA, null, null, "10 kg", "2 de octubre"),
                    new Donacion("Martina Ruiz", null, null, "5 kg", "9 de octubre")),
                "Desde agosto de 2026",
                List.of(
                    new Periodo(
                        "Octubre", "del 1 al 31", "15 de 40 kg", "curso", "En curso", 15, null),
                    new Periodo(SEPTIEMBRE, null, CUARENTA_KG, TODO, LLEGO_TODO, 40, 40),
                    new Periodo(AGOSTO, null, "31 de 40 kg", PARTE, UNA_PARTE, 31, null))),
            new Necesidad(
                "ropa",
                null,
                null,
                "Ropa de abrigo",
                "cat-ropa",
                "Ropa",
                UNIDADES,
                "Hasta el 15 de noviembre",
                "falta un mes",
                UNA_VEZ,
                5,
                30,
                null,
                null,
                "Para las familias del barrio que perdieron ropa en el temporal del 3 de octubre.",
                1,
                0,
                0,
                List.of(
                    new Entrega(
                        "10 unidades",
                        "puerta",
                        "Llegaron a tu puerta ayer a las 17:10 y esperan tu confirmación.",
                        "ropa")),
                List.of(new Donacion(null, "Sostenedor", null, "5 unidades", "8 de octubre")),
                null,
                List.of())),
        List.of(
            new Rubro(CAT_ALIMENTOS, ALIMENTOS, CAT_ALIMENTOS),
            new Rubro("cat-ropa", "Ropa", "cat-ropa"),
            new Rubro("cat-muebles", "Muebles", "cat-muebles"),
            new Rubro("cat-escolares", "Útiles escolares", "cat-escolares")),
        List.of(
            new Terminada(
                "Útiles escolares · 30 unidades", "Cumplida el 28 de septiembre: llegaron las 30."),
            new Terminada(
                "Pañales · 10 unidades", "Terminó el 10 de octubre con 6 de 10 recibidas."),
            new Terminada(
                "Frazadas · 20 unidades", "La diste de baja el 1 de octubre, con 8 recibidas.")),
        List.of(
            new Grupo(
                ALIMENTOS,
                List.of(
                    new Opcion(null, "Fideos secos (kg)", true),
                    new Opcion(null, "Lentejas secas (kg)", false),
                    new Opcion(null, "Arroz (kg)", false),
                    new Opcion(null, "Leche en polvo (unidades)", false),
                    new Opcion(null, "Aceite de girasol (litros)", false))),
            new Grupo(
                "Ropa",
                List.of(
                    new Opcion(null, "Ropa de abrigo (unidades)", false),
                    new Opcion(null, "Calzado (unidades)", false),
                    new Opcion(null, "Ropa de cama (unidades)", false))),
            new Grupo(
                "Muebles",
                List.of(
                    new Opcion(null, "Camas (unidades)", false),
                    new Opcion(null, "Mesas y sillas (unidades)", false)))),
        "lentejas",
        "aceite");
  }

  @Override
  public void publicar(
      UUID entidad,
      UUID subcategoria,
      int cantidad,
      String periodicidad,
      LocalDate hasta,
      String motivo) {
    // En la maqueta no se guarda nada.
  }

  @Override
  public void cambiar(UUID necesidad, long version, int cantidad, String motivo) {
    // En la maqueta no se guarda nada.
  }

  @Override
  public void darDeBaja(UUID necesidad) {
    // En la maqueta no se guarda nada.
  }
}
