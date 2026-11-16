package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las seis entidades de ejemplo con necesidades vigentes (ficha raices-publico-navegable.md, 6.3),
 * las que escribían {@code datos-publicos} y la plantilla; "hoy" es el 15 de octubre de 2026. La
 * búsqueda, los filtros y los estados ({@code ?escenario=}) los muestra la plantilla.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.publico",
    havingValue = "maqueta",
    matchIfMissing = true)
class PublicoEntidadesMaqueta implements PublicoEntidadesPuerto {

  private static final String ALIMENTOS = "alimentos";
  private static final String ROPA = "ropa";
  private static final String MUEBLES = "muebles";
  private static final String RECURRENTE = "recurrente";
  private static final String EXTRAORDINARIA = "extraordinaria";
  private static final String CADA_MES = "Se repite cada mes";
  private static final String LECHE_EN_POLVO = "Leche en polvo";
  private static final String SILLAS = "Sillas";
  private static final String HASTA_EL_15_DE_NOVIEMBRE = "Hasta el 15 de noviembre";
  private static final String HASTA_EL_30_DE_NOVIEMBRE = "Hasta el 30 de noviembre";

  private static final PublicoEntidadesVista ENTIDADES =
      new PublicoEntidadesVista(
          List.of(
              new Entidad(
                  "comedor",
                  "Comedor",
                  "Comedor Barrio Ejemplo",
                  ALIMENTOS,
                  1,
                  3,
                  "Damos el almuerzo de lunes a viernes a unas 120 personas del barrio, la mayoría chicos que van a la escuela de la vuelta y sus familias. Cocinamos entre vecinas y vecinos que se turnan cada semana, con lo que llega de las donaciones y de una huerta chica que armamos en el patio. Los sábados abrimos para la merienda y para el apoyo escolar, y una vez por mes hacemos una feria de ropa a precio simbólico para sostener los gastos de gas y de luz.",
                  "El 3 de octubre un temporal tiró abajo casas enteras del barrio. Esta colecta es para que las familias estén abrigadas mientras se reconstruyen sus casas.",
                  List.of(
                      new Pedido(
                          "Lentejas secas",
                          ALIMENTOS,
                          "Faltan 30 de 40 kg este mes",
                          CADA_MES,
                          30,
                          40,
                          RECURRENTE,
                          false),
                      new Pedido(
                          "Arroz",
                          ALIMENTOS,
                          "Faltan 25 de 40 kg este mes",
                          CADA_MES,
                          25,
                          40,
                          RECURRENTE,
                          false),
                      new Pedido(
                          "Aceite de girasol",
                          ALIMENTOS,
                          "Faltan 20 litros",
                          "Hasta el 20 de octubre",
                          20,
                          null,
                          EXTRAORDINARIA,
                          false),
                      new Pedido(
                          LECHE_EN_POLVO,
                          ALIMENTOS,
                          "Faltan 12 de 24 unidades",
                          "Hasta el 25 de octubre",
                          12,
                          24,
                          EXTRAORDINARIA,
                          false),
                      new Pedido(
                          "Ropa de abrigo",
                          ROPA,
                          "Faltan 25 de 30 unidades",
                          HASTA_EL_15_DE_NOVIEMBRE,
                          25,
                          30,
                          EXTRAORDINARIA,
                          true))),
              new Entidad(
                  "escuela",
                  "Escuela",
                  "Escuela Ejemplo N.º 12",
                  MUEBLES,
                  2,
                  1,
                  "Somos una escuela primaria pública con 380 alumnas y alumnos de jornada completa. Este año abrimos un aula más de primer grado y la estamos equipando con lo que nos hace falta para empezar el ciclo que viene. La cooperadora junta lo que llega y lo reparte entre los grados según lo que pide cada maestra.",
                  "",
                  List.of(
                      new Pedido(
                          SILLAS,
                          MUEBLES,
                          "Faltan 14 de 20 unidades",
                          HASTA_EL_30_DE_NOVIEMBRE,
                          14,
                          20,
                          EXTRAORDINARIA,
                          false),
                      new Pedido(
                          "Útiles escolares",
                          "escolares",
                          "Faltan 4 cajas",
                          HASTA_EL_30_DE_NOVIEMBRE,
                          4,
                          null,
                          EXTRAORDINARIA,
                          false))),
              new Entidad(
                  "merendero",
                  "Merendero",
                  "Merendero Ejemplo Sur",
                  ALIMENTOS,
                  3,
                  2,
                  "Damos la merienda de lunes a viernes a unos 60 chicos del barrio, después de la escuela. Además de la leche y algo para comer, hay un espacio de juegos y de lectura que coordinan dos maestras jubiladas, y los viernes una vecina enseña a tejer a quien quiera aprender.",
                  "",
                  List.of(
                      new Pedido(
                          "Galletitas",
                          ALIMENTOS,
                          "Faltan 10 de 20 kg este mes",
                          CADA_MES,
                          10,
                          20,
                          RECURRENTE,
                          false),
                      new Pedido(
                          LECHE_EN_POLVO,
                          ALIMENTOS,
                          "Faltan 20 unidades",
                          "Hasta el 31 de octubre",
                          20,
                          null,
                          EXTRAORDINARIA,
                          false))),
              new Entidad(
                  "hogar",
                  "Hogar",
                  "Hogar Casa Ejemplo",
                  ROPA,
                  4,
                  3,
                  "Somos un hogar convivencial para 14 adolescentes. Acompañamos su escolaridad, su salud y sus proyectos hasta que puedan vivir por su cuenta, junto con un equipo de educadores que está presente todos los días del año.",
                  "",
                  List.of(
                      new Pedido(
                          "Frazadas",
                          ROPA,
                          "Faltan 10 de 20 unidades",
                          HASTA_EL_15_DE_NOVIEMBRE,
                          10,
                          20,
                          EXTRAORDINARIA,
                          false))),
              new Entidad(
                  "club",
                  "Club",
                  "Club Ejemplo Norte",
                  ROPA,
                  null,
                  0,
                  "Somos un club de barrio con fútbol infantil, vóley y patín para más de 200 chicas y chicos. La cuota es simbólica y nadie se queda afuera por no poder pagarla; la sostenemos entre las familias, una rifa anual y lo que se dona.",
                  "",
                  List.of(
                      new Pedido(
                          "Camisetas deportivas",
                          ROPA,
                          "Faltan 15 unidades",
                          HASTA_EL_30_DE_NOVIEMBRE,
                          15,
                          null,
                          EXTRAORDINARIA,
                          false))),
              new Entidad(
                  "biblioteca",
                  "Biblioteca",
                  "Biblioteca Popular Ejemplo",
                  MUEBLES,
                  5,
                  2,
                  "Somos una biblioteca popular con más de 6.000 libros y una sala de lectura infantil. Hacemos talleres de lectura y escritura para chicos y grandes, prestamos libros sin cargo y abrimos la sala para que las escuelas de la zona vengan con sus grados.",
                  "",
                  List.of(
                      new Pedido(
                          SILLAS,
                          MUEBLES,
                          "Faltan 4 de 8 unidades",
                          "Hasta el 31 de diciembre",
                          4,
                          8,
                          EXTRAORDINARIA,
                          false)))),
          46,
          "15 de octubre de 2026",
          "octubre de 2026",
          true,
          true,
          null);

  @Override
  public PublicoEntidadesVista entidades() {
    return ENTIDADES;
  }
}
