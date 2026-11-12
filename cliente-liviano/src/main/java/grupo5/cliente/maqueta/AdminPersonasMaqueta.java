package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminDonantesVista.Opcion;
import grupo5.cliente.vistas.AdminDonantesVista.Opciones;
import grupo5.cliente.vistas.AdminDonantesVista.Ultimo;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import grupo5.cliente.vistas.ConsultaDonantes;
import grupo5.cliente.vistas.ConsultaImportaciones;
import grupo5.cliente.vistas.Tramo;
import java.text.Collator;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Los donantes y las entidades de ejemplo del admin, coherentes con la ficha
 * (docs/interfaz/flujos/raices-admin-navegable.md) y con las páginas públicas. Los estados (vacío,
 * lento, error) los elige la plantilla con {@code ?escenario=}.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.admin-personas",
    havingValue = "maqueta",
    matchIfMissing = true)
class AdminPersonasMaqueta implements AdminPersonasPuerto {

  private static final String PERSONA = "persona";
  private static final String PERSONA_TEXTO = "Persona";
  private static final String EMPRESA = "empresa";
  private static final String EMPRESA_TEXTO = "Empresa";
  private static final String ORGANIZACION = "ong";
  private static final String ORGANIZACION_TEXTO = "ONG";
  private static final String ACTIVO = "activo";
  private static final String PENDIENTE = "pendiente";
  private static final String ALIMENTOS = "alimentos";
  private static final String ALIMENTOS_TEXTO = "Alimentos";
  private static final String ESCOLARES = "escolares";
  private static final String ESCOLARES_TEXTO = "Útiles escolares";
  private static final String MUEBLES = "muebles";
  private static final String ROPA = "ropa";
  private static final String AL = "15 de octubre de 2026, 18:30";
  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

  private static final List<Donante> DONANTES =
      List.of(
          new Donante(
              "lucia",
              "Lucía Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.004",
              "15 de octubre",
              PENDIENTE,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "ana",
              "Ana Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.001",
              "14 de octubre",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "club",
              "Asociación Vecinal Ejemplo",
              ORGANIZACION,
              ORGANIZACION_TEXTO,
              "CUIT 30-00000005-0",
              "12 de octubre",
              PENDIENTE,
              MUEBLES,
              "Muebles"),
          new Donante(
              "juan",
              "Juan Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.002",
              "3 de septiembre",
              ACTIVO,
              ESCOLARES,
              ESCOLARES_TEXTO),
          new Donante(
              "super",
              "Supermercado Ejemplo SRL",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000003-0",
              "28 de agosto",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "fundacion",
              "Fundación Ejemplo Sur",
              ORGANIZACION,
              ORGANIZACION_TEXTO,
              "CUIT 30-00000002-0",
              "21 de julio",
              ACTIVO,
              ROPA,
              "Ropa"),
          new Donante(
              "libreria",
              "Librería Ejemplo SA",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000004-0",
              "9 de junio",
              ACTIVO,
              ESCOLARES,
              ESCOLARES_TEXTO),
          new Donante(
              "pedro",
              "Pedro Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.003",
              "5 de mayo",
              "baja",
              ROPA,
              "Ropa"),
          new Donante(
              "carla",
              "Carla Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.005",
              "1 de abril",
              ACTIVO,
              MUEBLES,
              "Muebles"),
          new Donante(
              "distribuidora",
              "Distribuidora Ejemplo SA",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000001-0",
              "2 de marzo",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO));

  /**
   * Los mismos, de la A a la Z (la lista de partida ya va de los más nuevos a los más antiguos).
   */
  private static final List<Donante> POR_NOMBRE =
      DONANTES.stream()
          .sorted(Comparator.comparing(Donante::nombre, Collator.getInstance(ES_AR)))
          .toList();

  /**
   * De a 5 por página en la maqueta (en el sistema, de a 20): con sus 10 donantes de ejemplo, el
   * paginador provisorio se ve y se puede probar.
   */
  static final int POR_PAGINA = 5;

  private static final String IMPORTANDO = "importando";
  private static final String IMPORTADO = "importado";
  private static final String TERMINADA = "terminada";

  private static final Opciones OPCIONES =
      new Opciones(
          List.of(
              new Opcion("", "Todos los tipos"),
              new Opcion(PERSONA, "Personas"),
              new Opcion(EMPRESA, "Empresas"),
              new Opcion(ORGANIZACION, ORGANIZACION_TEXTO)),
          List.of(
              new Opcion("", "Todas las categorías"),
              new Opcion(ALIMENTOS, ALIMENTOS_TEXTO),
              new Opcion(ROPA, "Ropa"),
              new Opcion(ESCOLARES, ESCOLARES_TEXTO),
              new Opcion(MUEBLES, "Muebles")),
          List.of(
              new Opcion("", "Todos los estados"),
              new Opcion(ACTIVO, "Activos"),
              new Opcion(PENDIENTE, "Sin activar su cuenta"),
              new Opcion("baja", "Dados de baja")),
          List.of(
              new Opcion("", "Más nuevos primero"),
              new Opcion("antiguo", "Más antiguos primero"),
              new Opcion("nombre", "Por nombre, de la A a la Z")),
          true,
          true);

  private static final String ARCHIVO = "donantes-octubre.csv";
  private static final String HORA = "18:34:12";
  private static final String AVANCE = "/admin-donantes-importado";

  private static final List<Ultimo> ULTIMOS =
      List.of(
          new Ultimo("Ana Ejemplo", "Nuevo"),
          new Ultimo("Juan Ejemplo", "Actualizado"),
          new Ultimo("Supermercado Ejemplo SRL", "Nuevo"),
          new Ultimo("Carla Ejemplo", "Nuevo"),
          new Ultimo("Fundación Ejemplo Sur", "Actualizado"));

  /** Las 126 filas con error de la ficha: las tres de ejemplo, repetidas a lo largo del archivo. */
  private static final List<FilaConError> FILAS_CON_ERROR =
      IntStream.range(0, 126)
          .mapToObj(
              i ->
                  switch (i % 3) {
                    case 0 -> new FilaConError(fila(15 + i * 150), "Falta el apellido.");
                    case 1 ->
                        new FilaConError(
                            fila(202 + i * 150),
                            "El correo ya es de otra persona, con otro documento.");
                    default ->
                        new FilaConError(
                            fila(3410 + i * 150), "Tiene 6 columnas y el encabezado tiene 8.");
                  })
          .toList();

  private static String fila(int numero) {
    return "Fila " + NumberFormat.getIntegerInstance(ES_AR).format(numero);
  }

  @Override
  public AdminDonantesVista donantes(ConsultaDonantes pedida) {
    ConsultaDonantes consulta =
        new ConsultaDonantes(
            pedida.buscar(),
            Opciones.elegido(pedida.tipo(), OPCIONES.tipos()),
            Opciones.elegido(pedida.dona(), OPCIONES.categorias()),
            Opciones.elegido(pedida.estado(), OPCIONES.estados()),
            Opciones.elegido(pedida.orden(), OPCIONES.ordenes()),
            pedida.pagina());
    List<Donante> orden =
        switch (consulta.orden()) {
          case "nombre" -> POR_NOMBRE;
          case "antiguo" -> DONANTES.reversed();
          default -> DONANTES;
        };
    String buscado = sinTildes(consulta.buscar());
    List<Donante> encontrados =
        orden.stream()
            .filter(d -> consulta.tipo().isEmpty() || consulta.tipo().equals(d.tipo()))
            .filter(d -> consulta.dona().isEmpty() || consulta.dona().equals(d.dona()))
            .filter(d -> consulta.estado().isEmpty() || consulta.estado().equals(d.estado()))
            .filter(
                d ->
                    buscado.isEmpty()
                        || sinTildes(d.nombre()).contains(buscado)
                        || sinTildes(d.documento()).contains(buscado))
            .toList();
    return new AdminDonantesVista(
        86, AL, Tramo.de(encontrados, consulta.pagina(), POR_PAGINA), consulta, OPCIONES, null);
  }

  /** Sin tildes ni mayúsculas, como busca Donaciones: "gomez" encuentra "Gómez". */
  private static String sinTildes(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  /**
   * La importación de la ficha (20.000 filas) en la pantalla pedida. Escenarios del avance: {@code
   * esperando}, {@code leyendo} y {@code cortada}; del resultado, {@code sin-errores}.
   */
  /** Las importaciones de ejemplo de la ficha r11-p1-importaciones; el escenario elige cuáles. */
  @Override
  public AdminImportacionesVista importaciones(ConsultaImportaciones consulta, String ejemplo) {
    return AdminImportacionesDeEjemplo.importaciones(consulta, ejemplo);
  }

  /** Las 126 filas con error de ejemplo. */
  @Override
  public DescargaDeFilas filasConError(UUID archivo) {
    return AdminImportacionesDeEjemplo.filasConError(FILAS_CON_ERROR);
  }

  @Override
  public Importacion importacion(UUID archivo, String nombre, String ejemplo, int pagina) {
    return conSubida(ejemplo(ejemplo, pagina));
  }

  /** Con la fecha de subida de la ficha ("Subido el 15 de octubre de 2026, 18:02."). */
  private static Importacion conSubida(Importacion i) {
    return new Importacion(
        i.accion(),
        i.etapa(),
        i.archivo(),
        i.total(),
        i.procesadas(),
        i.creadas(),
        i.actualizadas(),
        i.conError(),
        i.leidas(),
        i.totalFilas(),
        i.resultado(),
        i.ultimos(),
        i.filas(),
        i.avance(),
        i.hora(),
        "15 de octubre de 2026, 18:02",
        "2026-10-15T18:02:00",
        "00000000-0000-4000-8000-000000000001");
  }

  private Importacion ejemplo(String ejemplo, int pagina) {
    return switch (ejemplo == null ? "" : ejemplo) {
      case "importando:esperando" ->
          new Importacion(
              IMPORTANDO,
              "esperando",
              ARCHIVO,
              null,
              "0",
              "0",
              "0",
              "0",
              0,
              null,
              null,
              List.of(),
              null,
              AVANCE,
              HORA);
      case "importando:leyendo" ->
          new Importacion(
              IMPORTANDO,
              "leyendo",
              ARCHIVO,
              null,
              "500",
              "480",
              "15",
              "5",
              500,
              null,
              null,
              ULTIMOS,
              null,
              AVANCE,
              HORA);
      case "importando:cortada", "importado:cortada" ->
          new Importacion(
              IMPORTADO,
              "cortada",
              ARCHIVO,
              "20.000",
              "8.400",
              "7.950",
              "410",
              "40",
              8400,
              20000,
              null,
              List.of(),
              Tramo.de(FILAS_CON_ERROR.subList(0, 40), pagina, 20),
              AVANCE,
              HORA);
      case "importado:sin-errores" ->
          new Importacion(
              IMPORTADO,
              TERMINADA,
              ARCHIVO,
              "20.000",
              "20.000",
              "19.874",
              "126",
              "0",
              20000,
              20000,
              "20.000 filas leídas: 19.874 donantes nuevos y 126 actualizados.",
              List.of(),
              Tramo.de(List.of(), 1, 20),
              AVANCE,
              HORA);
      case String e when e.startsWith(IMPORTADO) ->
          new Importacion(
              IMPORTADO,
              TERMINADA,
              ARCHIVO,
              "20.000",
              "20.000",
              "19.100",
              "774",
              "126",
              20000,
              20000,
              "20.000 filas leídas: 19.100 donantes nuevos, 774 actualizados y 126 filas con"
                  + " error.",
              List.of(),
              Tramo.de(FILAS_CON_ERROR, pagina, 20),
              AVANCE,
              HORA);
      case String e when e.startsWith("importar-error") ->
          new Importacion(
              "importar-error",
              "error",
              ARCHIVO,
              null,
              "0",
              "0",
              "0",
              "0",
              0,
              null,
              null,
              List.of(),
              null,
              AVANCE,
              HORA);
      default ->
          new Importacion(
              IMPORTANDO,
              IMPORTANDO,
              ARCHIVO,
              "20.000",
              "8.400",
              "7.950",
              "410",
              "40",
              8400,
              20000,
              null,
              ULTIMOS,
              null,
              AVANCE,
              HORA);
    };
  }

  /** En la maqueta el formulario lo atiende SitioController y no sube nada. */
  @Override
  public UUID importar(MultipartFile archivo) {
    throw new UnsupportedOperationException("La maqueta no importa archivos");
  }

  /** En la maqueta el formulario lo atiende SitioController y no guarda nada. */
  @Override
  public void registrarDonante(PersonaNueva persona) {
    throw new UnsupportedOperationException("La maqueta no registra donantes");
  }

  /** En la maqueta el formulario lo atiende SitioController y no guarda nada. */
  @Override
  public void registrarEntidad(PersonaNueva.Juridica juridica) {
    throw new UnsupportedOperationException("La maqueta no registra organizaciones");
  }

  /** La lista entera de ejemplo: la pantalla busca y filtra sobre ella. */
  @Override
  public AdminEntidadesVista entidades(
      String buscar, AdminEntidadesVista.Filtro filtro, int pagina) {
    return AdminPersonasEntidadesDeEjemplo.ENTIDADES;
  }

  /** En la maqueta el formulario lo atiende SitioController y no aprueba nada. */
  @Override
  public String aprobarEntidad(String clave) {
    throw new UnsupportedOperationException("La maqueta no aprueba organizaciones");
  }

  /**
   * El inicio de la ficha (R11-21): 3 organizaciones, 7 donaciones sin asignar (4 vencen) y 2
   * propuestas; las cifras por tema, con 1 que no se pudo entregar, 2 vencidas y 1.284 entregas; y
   * los gráficos de los últimos 12 meses (R11-42).
   */
  @Override
  public AdminInicioVista inicio() {
    return new AdminInicioVista(
        "Jueves 15 de octubre de 2026",
        "Hoy esperan tu decisión 3 organizaciones y 2 propuestas de asignación. Hay 7 donaciones"
            + " sin asignar en el depósito; 4 vencen en los próximos 7 días.",
        new Decisiones(
            3,
            List.of("Hogar Ejemplo Norte", "Merendero Ejemplo Este", "Comedor Ejemplo Oeste"),
            0,
            7,
            4,
            "La primera, Leche en polvo, vence el 17 de octubre, en 2 días.",
            2),
        AL,
        new Cifras(
            7, 4, 1, 1, 2, 9, "en octubre, del 1 al 15", 1_284, 46, 86, 92, new Camiones(1, 3)),
        AdminInicioGraficosDeEjemplo.evolucion());
  }
}
