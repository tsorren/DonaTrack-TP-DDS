package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import grupo5.cliente.vistas.AdminNotificacionesVista;
import grupo5.cliente.vistas.AdminNotificacionesVista.Aviso;
import grupo5.cliente.vistas.AdminNotificacionesVista.Tema;
import grupo5.cliente.vistas.ConsultaBandeja;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Los avisos de ejemplo de la Administración (R11-30, ficha r11-p2-campanita §4 y §6): solo los que
 * hoy genera algún servicio, importaciones y mensajes de contacto, más uno viejo sin tipo. Las 3
 * sin leer son las de la campanita de la maqueta (CampanitaDeEjemplo). Los avisos de importación no
 * llevan el nombre del archivo (R1, H8). "Ahora" es el 15 de octubre de 2026, 18:45. Filtra y
 * pagina en memoria lo que en el sistema resuelve Notificaciones (BandejasDeEjemplo). Marcar no
 * guarda nada: en la maqueta el formulario lo atiende SitioController.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.avisos",
    havingValue = "maqueta",
    matchIfMissing = true)
class AdminNotificacionesMaqueta implements AdminNotificacionesPuerto {

  private static final String IMPORTACIONES = "importaciones";
  private static final String CONTACTO = "contacto";
  private static final String TERMINO = "Terminó una importación";
  private static final String CONTACTO_NUEVO = "Nuevo mensaje de contacto";
  private static final String VER_RESULTADO = "Ver el resultado";
  private static final String AHORA = "2026-10-15T18:45";

  private static final List<Aviso> AVISOS =
      List.of(
          new Aviso(
              "a1",
              IMPORTACIONES,
              "2026-10-15T18:40",
              false,
              TERMINO,
              "La importación de donantes que se subió el 15/10 a las 18:02 terminó. Se leyeron"
                  + " 19.980 filas: 19.100 donantes nuevos, 754 actualizados y 126 con error.",
              VER_RESULTADO,
              "/admin-donantes-importado?archivo=7d1a2b3c-0000-4000-8000-000000000001"),
          new Aviso(
              "a2",
              CONTACTO,
              "2026-10-14T16:10",
              false,
              CONTACTO_NUEVO,
              "Nuevo mensaje de contacto Nº 12, sobre una entrega.",
              null,
              null),
          new Aviso(
              "a3",
              IMPORTACIONES,
              "2026-10-12T09:00",
              false,
              "Una importación se cortó",
              "La importación de donantes que se subió el 12/10 a las 08:55 se cortó después de"
                  + " leer 1.234 filas. Esas filas quedaron guardadas.",
              "Ver qué pasó",
              "/admin-importaciones"),
          new Aviso(
              "a4",
              CONTACTO,
              "2026-10-06T10:00",
              true,
              CONTACTO_NUEVO,
              "Nuevo mensaje de contacto Nº 11, sobre el sitio.",
              null,
              null),
          new Aviso(
              "a5",
              IMPORTACIONES,
              "2026-09-12T10:00",
              true,
              TERMINO,
              "La importación de donantes que se subió el 12/09 a las 09:40 terminó. Se leyeron"
                  + " 3.241 filas: 3.200 donantes nuevos y 41 actualizados. Todas las filas se"
                  + " importaron sin errores.",
              VER_RESULTADO,
              "/admin-donantes-importado"),
          new Aviso(
              "a6",
              "otras",
              "2026-09-01T09:00",
              true,
              "Novedad",
              "Un aviso de antes, sin categoría.",
              null,
              null));

  /** Las categorías del menú: todas las del rol. */
  private static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema(IMPORTACIONES, "Importaciones", "archivo"),
          new Tema(CONTACTO, "Mensajes de contacto", "sobre"),
          new Tema("otras", "Otras novedades", "reloj"));

  @Override
  public AdminNotificacionesVista bandeja(ConsultaBandeja consulta) {
    return new AdminNotificacionesVista(
        BandejasDeEjemplo.pagina(
            AVISOS,
            consulta,
            Aviso::leida,
            Aviso::tema,
            n -> n.titulo() + " " + n.texto(),
            Aviso::fecha),
        AVISOS.stream().filter(n -> !n.leida()).count(),
        TEMAS,
        AHORA,
        "15 de octubre de 2026, 18:45");
  }

  @Override
  public void marcarLeidas(List<UUID> ids) {
    // En la maqueta no se guarda nada.
  }

  @Override
  public void dejarSinLeer(UUID id) {
    // En la maqueta no se guarda nada.
  }
}
