package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.impl.DonacionesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.DonantesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.ItemDonacionNormalizadoRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.PersonasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.SubcategoriasRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.CriterioPersonas;
import grupo5.donaciones.models.repositories.listados.OrdenDonantes;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Búsqueda, filtros, orden y tramo de los listados en memoria (R11-01): la misma semántica que las
 * consultas de la base, que se prueban aparte contra Postgres.
 */
class ListadosEnMemoriaTest {

  private static final Paginado PRIMERA = new Paginado(0, 20);

  private final PersonasRepositoryEnMemoria personas = new PersonasRepositoryEnMemoria();
  private final DonacionesRepositoryEnMemoria donaciones = new DonacionesRepositoryEnMemoria();
  private final DonantesRepositoryEnMemoria donantes =
      new DonantesRepositoryEnMemoria(
          personas,
          donaciones,
          new ItemDonacionNormalizadoRepositoryEnMemoria(),
          new SubcategoriasRepositoryEnMemoria());

  private static CriterioDonantes donantesQue(String q, String canal, OrdenDonantes orden) {
    return new CriterioDonantes(q, null, null, null, null, canal, orden, PRIMERA);
  }

  private Donante donanteDe(Persona persona, boolean activo, LocalDateTime alta) {
    personas.save(persona);
    return donantes.save(new Donante(persona.getId(), activo, true, alta));
  }

  private static List<UUID> ids(Tramo<Donante> tramo) {
    return tramo.elementos().stream().map(Donante::getId).toList();
  }

  // ===================== donantes =====================

  @Test
  void donantes_q_ignoraTildesYMayusculas() {
    Donante gomez = donanteDe(PersonaMother.mariaGomez(), true, LocalDateTime.now());
    donanteDe(PersonaMother.juanPerez(), true, LocalDateTime.now());

    assertEquals(
        List.of(gomez.getId()),
        ids(donantes.buscar(donantesQue("GOMEZ", null, OrdenDonantes.NUEVOS))));
  }

  @Test
  void donantes_q_buscaTambienPorDocumentoYRazonSocial() {
    Donante juan = donanteDe(PersonaMother.juanPerez(), true, LocalDateTime.now());
    Donante fundacion = donanteDe(PersonaMother.fundacionEsperanza(), true, LocalDateTime.now());

    assertEquals(
        List.of(juan.getId()),
        ids(donantes.buscar(donantesQue("12345678", null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(fundacion.getId()),
        ids(donantes.buscar(donantesQue("fundacion", null, OrdenDonantes.NUEVOS))));
  }

  @Test
  void donantes_porCanal_excluyeDonantesDeBajaYPersonasAnonimizadas() {
    Humana activa = PersonaMother.juanPerez();
    activa.agregarMedioDeContacto(PersonaMother.correoValido());
    Humana deBaja = PersonaMother.mariaGomez();
    deBaja.agregarMedioDeContacto(PersonaMother.correoValido());
    Humana anonimizada = new Humana("Ana", "Lopez", LocalDate.of(1992, 3, 3));
    anonimizada.agregarMedioDeContacto(PersonaMother.correoValido());
    anonimizada.anonimizar();
    Humana sinCorreo = new Humana("Luis", "Diaz", LocalDate.of(1980, 1, 1));
    Donante esperado = donanteDe(activa, true, LocalDateTime.now());
    donanteDe(deBaja, false, LocalDateTime.now());
    donanteDe(anonimizada, true, LocalDateTime.now());
    donanteDe(sinCorreo, true, LocalDateTime.now());

    assertEquals(
        List.of(esperado.getId()),
        ids(donantes.buscar(donantesQue(null, "CORREO", OrdenDonantes.NUEVOS))));
  }

  @Test
  void donantes_sinCanal_traeTambienLosDeBaja() {
    Donante deBaja = donanteDe(PersonaMother.juanPerez(), false, LocalDateTime.now());

    assertEquals(
        List.of(deBaja.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.NUEVOS))));
  }

  @Test
  void donantes_ordenPorAltaYPorNombre() {
    LocalDateTime hoy = LocalDateTime.of(2026, 11, 11, 10, 0);
    Donante viejo = donanteDe(PersonaMother.mariaGomez(), true, hoy.minusDays(10));
    Donante nuevo = donanteDe(PersonaMother.juanPerez(), true, hoy);

    assertEquals(
        List.of(nuevo.getId(), viejo.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(viejo.getId(), nuevo.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.ANTIGUOS))));
    // "Gómez María" antes que "Pérez Juan": por apellido y sin tildes
    assertEquals(
        List.of(viejo.getId(), nuevo.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.NOMBRE))));
  }

  @Test
  void donantes_porTipoJuridico_yTramoConElTotal() {
    donanteDe(PersonaMother.juanPerez(), true, LocalDateTime.now());
    Donante empresa = donanteDe(PersonaMother.empresaSA(), true, LocalDateTime.now());
    donanteDe(PersonaMother.fundacionEsperanza(), true, LocalDateTime.now());

    Tramo<Donante> empresas =
        donantes.buscar(
            new CriterioDonantes(
                null,
                TipoPersona.JURIDICA,
                TipoJuridico.EMPRESA,
                null,
                null,
                null,
                OrdenDonantes.NUEVOS,
                PRIMERA));
    Tramo<Donante> segunda =
        donantes.buscar(
            new CriterioDonantes(
                null, null, null, null, null, null, OrdenDonantes.NOMBRE, new Paginado(1, 2)));

    assertEquals(List.of(empresa.getId()), ids(empresas));
    assertEquals(1, empresas.total());
    assertEquals(1, segunda.elementos().size());
    assertEquals(3, segunda.total());
  }

  // ===================== personas =====================

  @Test
  void personas_porIdsYPorTexto() {
    Humana juan = PersonaMother.juanPerez();
    Humana maria = PersonaMother.mariaGomez();
    Juridica fundacion = PersonaMother.fundacionEsperanza();
    personas.save(juan);
    personas.save(maria);
    personas.save(fundacion);

    Tramo<Persona> porIds =
        personas.buscar(
            new CriterioPersonas(null, null, null, Set.of(juan.getId(), maria.getId()), PRIMERA));
    Tramo<Persona> porTexto =
        personas.buscar(new CriterioPersonas("pérez", null, null, Set.of(), PRIMERA));
    Tramo<Persona> juridicas =
        personas.buscar(new CriterioPersonas(null, TipoPersona.JURIDICA, null, Set.of(), PRIMERA));

    assertEquals(
        List.of(maria.getId(), juan.getId()),
        porIds.elementos().stream().map(Persona::getId).toList());
    assertEquals(List.of(juan.getId()), porTexto.elementos().stream().map(Persona::getId).toList());
    assertEquals(
        List.of(fundacion.getId()), juridicas.elementos().stream().map(Persona::getId).toList());
  }

  // ===================== donaciones =====================

  @Test
  void donaciones_porDonanteYPorTextoDeLosBienes() {
    UUID donanteId = UUID.randomUUID();
    Donacion delDonante =
        DonacionMother.conItem(
            donanteId, new Bien("Campera de abrigo", null, null, Estado.NUEVO, 1.0, 1.0), 1);
    donaciones.save(delDonante);
    donaciones.save(DonacionMother.simple(donanteId));
    donaciones.save(DonacionMother.simple(UUID.randomUUID()));

    Tramo<Donacion> porDonante =
        donaciones.buscar(
            new CriterioDonaciones(donanteId, null, null, null, OrdenPorFecha.RECIENTES, PRIMERA));
    Tramo<Donacion> porTexto =
        donaciones.buscar(
            new CriterioDonaciones(
                donanteId, null, null, "CAMPERA", OrdenPorFecha.RECIENTES, PRIMERA));
    Tramo<Donacion> fueraDeRango =
        donaciones.buscar(
            new CriterioDonaciones(
                donanteId,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 2),
                null,
                OrdenPorFecha.RECIENTES,
                PRIMERA));

    assertEquals(2, porDonante.total());
    assertEquals(
        List.of(delDonante.getId()), porTexto.elementos().stream().map(Donacion::getId).toList());
    assertEquals(0, fueraDeRango.total());
  }
}
