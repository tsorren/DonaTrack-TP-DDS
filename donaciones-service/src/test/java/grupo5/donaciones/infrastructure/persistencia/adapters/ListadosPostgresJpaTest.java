package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.dto.entidadBeneficiaria.FiltroOrganizaciones;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonantesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.EntidadesBeneficiariasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.PersonasPersistenciaMapper;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.ConteoAsignadas;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.CriterioDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.CriterioPersonas;
import grupo5.donaciones.models.repositories.listados.OrdenDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.OrdenDonantes;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Los listados de R11-01 resueltos en SQL, contra Postgres con la migración V21 (unaccent,
 * fecha_alta e índices): búsqueda sin tildes, filtros, orden, tramo, total y conteos.
 */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  DireccionesPersistenciaMapper.class,
  PersonasPersistenciaMapper.class,
  PersonaRepositoryJpaAdapter.class,
  DonantesPersistenciaMapper.class,
  DonanteRepositoryJpaAdapter.class,
  EntidadesBeneficiariasPersistenciaMapper.class,
  EntidadBeneficiariaRepositoryJpaAdapter.class,
  DonacionesPersistenciaMapper.class,
  DonacionRepositoryJpaAdapter.class,
  ItemsNormalizadosPersistenciaMapper.class,
  ItemDonacionNormalizadoRepositoryJpaAdapter.class,
  DonacionesIndependientesPersistenciaMapper.class,
  DonacionIndependienteRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class ListadosPostgresJpaTest {

  private static final Paginado PRIMERA = new Paginado(0, 20);
  private static final String ACTOR = "admin";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IPersonasRepository personas;
  @Autowired private IDonantesRepository donantes;
  @Autowired private IEntidadesBeneficiariasRepository entidades;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    independientes.deleteAll();
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    jdbc.update("DELETE FROM donaciones.necesidad");
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.entidad_beneficiaria");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");
  }

  // Datos sintéticos: cada persona con su documento, porque (tipo, número) es único en la base
  private int documento = 20_000_000;

  private Humana humana(String nombre, String apellido) {
    Humana humana = new Humana(nombre, apellido, LocalDate.of(1990, 1, 1), Genero.NO_BINARIO);
    humana.actualizarDocumento(TipoDocumento.DNI, String.valueOf(documento++));
    return humana;
  }

  private Juridica juridica(String razonSocial, TipoJuridico tipo) {
    Juridica juridica = new Juridica(humana("Rep", "Legal"), razonSocial, tipo, "Social");
    juridica.actualizarDocumento(TipoDocumento.CUIT, "30-" + (documento++) + "-9");
    return juridica;
  }

  private Donante donanteDe(Persona persona, boolean activo, LocalDateTime alta) {
    personas.save(persona);
    return donantes.save(new Donante(persona.getId(), activo, true, alta));
  }

  private static List<UUID> ids(Tramo<Donante> tramo) {
    return tramo.elementos().stream().map(Donante::getId).toList();
  }

  private static CriterioDonantes donantesQue(String q, String canal, OrdenDonantes orden) {
    return new CriterioDonantes(q, null, null, null, null, canal, orden, PRIMERA);
  }

  private UUID crearSubcategoria() {
    UUID categoriaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.categoria (id, nombre, con_uso, con_vencimiento, tipo_unidad)"
            + " VALUES (?, 'Alimentos', false, true, 'UNIDADES')",
        categoriaId);
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.subcategoria (id, id_categoria, nombre) VALUES (?, ?, 'Fideos')",
        id,
        categoriaId);
    return id;
  }

  // ===================== donantes =====================

  @Test
  void donantes_q_ignoraTildesYMayusculas_enNombreDocumentoYRazonSocial() {
    Donante maria = donanteDe(humana("María", "Gómez"), true, LocalDateTime.now());
    Humana perez = humana("Juan", "Pérez");
    Donante juan = donanteDe(perez, true, LocalDateTime.now());
    Donante fundacion =
        donanteDe(juridica("Fundación Esperanza", TipoJuridico.ONG), true, LocalDateTime.now());

    assertEquals(
        List.of(maria.getId()),
        ids(donantes.buscar(donantesQue("GOMEZ", null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(juan.getId()),
        ids(donantes.buscar(donantesQue("pérez", null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(juan.getId()),
        ids(donantes.buscar(donantesQue(perez.getDocumento(), null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(fundacion.getId()),
        ids(donantes.buscar(donantesQue("fundacion", null, OrdenDonantes.NUEVOS))));
  }

  @Test
  void donantes_q_conComodinesLosBuscaLiteralmente() {
    donanteDe(humana("Juan", "Pérez"), true, LocalDateTime.now());

    assertEquals(0, donantes.buscar(donantesQue("%", null, OrdenDonantes.NUEVOS)).total());
    assertEquals(0, donantes.buscar(donantesQue("_", null, OrdenDonantes.NUEVOS)).total());
  }

  @Test
  void donantes_porCanal_excluyeLosDeBajaAnonimizadosYSinEseMedio() {
    Humana activa = humana("Juan", "Pérez");
    activa.agregarMedioDeContacto(PersonaMother.correoValido());
    Humana deBaja = humana("María", "Gómez");
    deBaja.agregarMedioDeContacto(PersonaMother.correoValido());
    Humana conTelefono = humana("Luis", "Diaz");
    conTelefono.agregarMedioDeContacto(PersonaMother.telefonoValido());
    Donante esperado = donanteDe(activa, true, LocalDateTime.now());
    donanteDe(deBaja, false, LocalDateTime.now());
    Donante porTelefono = donanteDe(conTelefono, true, LocalDateTime.now());

    assertEquals(
        List.of(esperado.getId()),
        ids(donantes.buscar(donantesQue(null, "CORREO", OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(porTelefono.getId()),
        ids(donantes.buscar(donantesQue(null, "WHATSAPP", OrdenDonantes.NUEVOS))));
    // El constructor de Persona agrega un teléfono estándar vacío: cuenta como TELEFONO (igual que
    // antes de R11-01); el donante de baja sigue afuera
    assertEquals(
        Set.of(esperado.getId(), porTelefono.getId()),
        Set.copyOf(ids(donantes.buscar(donantesQue(null, "TELEFONO", OrdenDonantes.NUEVOS)))));
    assertEquals(0, donantes.buscar(donantesQue(null, "PALOMA", OrdenDonantes.NUEVOS)).total());
  }

  @Test
  void donantes_ordenPorAltaYPorNombre_yTramoConElTotal() {
    LocalDateTime hoy = LocalDateTime.of(2026, 11, 11, 10, 0);
    Donante maria = donanteDe(humana("María", "Gómez"), true, hoy.minusDays(10));
    Donante juan = donanteDe(humana("Juan", "Pérez"), true, hoy);
    Donante empresa =
        donanteDe(juridica("Empresa SA", TipoJuridico.EMPRESA), true, hoy.minusDays(5));

    assertEquals(
        List.of(juan.getId(), empresa.getId(), maria.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.NUEVOS))));
    assertEquals(
        List.of(maria.getId(), empresa.getId(), juan.getId()),
        ids(donantes.buscar(donantesQue(null, null, OrdenDonantes.ANTIGUOS))));
    // "empresa sa" < "gomez maria" < "perez juan"
    Tramo<Donante> segunda =
        donantes.buscar(
            new CriterioDonantes(
                null, null, null, null, null, null, OrdenDonantes.NOMBRE, new Paginado(1, 2)));
    assertEquals(List.of(juan.getId()), ids(segunda));
    assertEquals(3, segunda.total());
  }

  @Test
  void donantes_contarActivosYActivosConDonaciones() {
    Donante conDonacion = donanteDe(humana("Juan", "Pérez"), true, LocalDateTime.now());
    donanteDe(humana("María", "Gómez"), true, LocalDateTime.now());
    Donante deBajaConDonacion =
        donanteDe(juridica("Empresa SA", TipoJuridico.EMPRESA), false, LocalDateTime.now());
    donaciones.save(DonacionMother.simple(conDonacion.getId()));
    donaciones.save(DonacionMother.simple(deBajaConDonacion.getId()));

    assertEquals(2, donantes.contarActivos());
    assertEquals(1, donantes.contarActivosConDonaciones());
    assertEquals(
        Set.of(conDonacion.getId(), deBajaConDonacion.getId()),
        Set.copyOf(
            donantes.findAllById(List.of(conDonacion.getId(), deBajaConDonacion.getId())).stream()
                .map(Donante::getId)
                .toList()));
  }

  // ===================== personas =====================

  @Test
  void personas_porIdsTipoYTexto_ordenadasPorNombre() {
    Humana juan = humana("Juan", "Pérez");
    Humana maria = humana("María", "Gómez");
    Juridica fundacion = juridica("Fundación Esperanza", TipoJuridico.ONG);
    personas.save(juan);
    personas.save(maria);
    personas.save(fundacion);

    Tramo<Persona> porIds =
        personas.buscar(
            new CriterioPersonas(null, null, null, Set.of(juan.getId(), maria.getId()), PRIMERA));
    Tramo<Persona> porTexto =
        personas.buscar(new CriterioPersonas("GÓMEZ", null, null, Set.of(), PRIMERA));
    Tramo<Persona> juridicas =
        personas.buscar(new CriterioPersonas(null, TipoPersona.JURIDICA, null, Set.of(), PRIMERA));

    assertEquals(
        List.of(maria.getId(), juan.getId()),
        porIds.elementos().stream().map(Persona::getId).toList());
    assertEquals(
        List.of(maria.getId()), porTexto.elementos().stream().map(Persona::getId).toList());
    assertEquals(
        List.of(fundacion.getId()), juridicas.elementos().stream().map(Persona::getId).toList());
  }

  // ===================== entidades =====================

  @Test
  void entidades_porNombreSinTildes_soloActivas_yConteo() {
    Juridica fundacion = juridica("Fundación Esperanza", TipoJuridico.ONG);
    Juridica empresa = juridica("Empresa SA", TipoJuridico.EMPRESA);
    personas.save(fundacion);
    personas.save(empresa);
    entidades.save(new EntidadBeneficiaria(fundacion.getId()));
    EntidadBeneficiaria deBaja = new EntidadBeneficiaria(empresa.getId());
    deBaja.darDeBaja();
    entidades.save(deBaja);

    Tramo<EntidadBeneficiaria> porNombre =
        entidades.buscar(new CriterioEntidades("FUNDACION", false, PRIMERA));
    Tramo<EntidadBeneficiaria> todas =
        entidades.buscar(new CriterioEntidades(null, false, PRIMERA));
    Tramo<EntidadBeneficiaria> activas =
        entidades.buscar(new CriterioEntidades(null, true, PRIMERA));

    assertEquals(
        List.of(fundacion.getId()),
        porNombre.elementos().stream().map(EntidadBeneficiaria::getId).toList());
    // Por razón social: "Empresa SA" antes que "Fundación Esperanza"
    assertEquals(
        List.of(empresa.getId(), fundacion.getId()),
        todas.elementos().stream().map(EntidadBeneficiaria::getId).toList());
    assertEquals(1, activas.total());
    assertEquals(1, entidades.contarActivas());
  }

  // R11-12 (A1 a A5 y A8): categoría y "ya doné" en SQL, las mismas reglas que en memoria
  @Test
  void entidades_porCategoriaYSoloDonadas_yLaMarcaDeLaPagina() {
    UUID subcategoriaId = crearSubcategoria();
    UUID categoriaId =
        jdbc.queryForObject(
            "SELECT id_categoria FROM donaciones.subcategoria WHERE id = ?",
            UUID.class,
            subcategoriaId);
    LocalDate hoy = LocalDate.now();
    UUID activa = entidadLlamada("Comedor Activo");
    UUID deBaja = entidadLlamada("Comedor De Baja");
    UUID vencida = entidadLlamada("Comedor Vencido");
    UUID donada = entidadLlamada("Hogar Donado");
    necesidad(activa, subcategoriaId, true, hoy);
    necesidad(deBaja, subcategoriaId, false, null);
    necesidad(vencida, subcategoriaId, true, hoy.minusDays(1));
    Donante donante = donanteDe(humana("Ana", "Díaz"), true, LocalDateTime.now());
    asignarA(donante, subcategoriaId, donada);
    asignarA(donante, subcategoriaId, activa);
    Donante otro = donanteDe(humana("Otro", "Donante"), true, LocalDateTime.now());
    asignarA(otro, subcategoriaId, vencida);

    Tramo<EntidadBeneficiaria> deLaCategoria =
        entidades.buscar(new CriterioEntidades(null, true, categoriaId, hoy, null, PRIMERA));
    Tramo<EntidadBeneficiaria> deOtraCategoria =
        entidades.buscar(new CriterioEntidades(null, true, UUID.randomUUID(), hoy, null, PRIMERA));
    Tramo<EntidadBeneficiaria> donadas =
        entidades.buscar(new CriterioEntidades(null, true, null, hoy, donante.getId(), PRIMERA));
    Tramo<EntidadBeneficiaria> combinadas =
        entidades.buscar(
            new CriterioEntidades(
                "comedor", true, categoriaId, hoy, donante.getId(), new Paginado(0, 1)));
    Tramo<EntidadBeneficiaria> sinFiltros =
        entidades.buscar(new CriterioEntidades(null, true, PRIMERA));

    assertEquals(List.of(activa), idsDe(deLaCategoria));
    assertEquals(1, deLaCategoria.total());
    assertEquals(0, deOtraCategoria.total());
    // Por razón social: "Comedor Activo" antes que "Hogar Donado"
    assertEquals(List.of(activa, donada), idsDe(donadas));
    assertEquals(2, donadas.total());
    assertEquals(List.of(activa), idsDe(combinadas));
    assertEquals(1, combinadas.total());
    assertEquals(4, sinFiltros.total());
    assertEquals(
        Set.of(activa, donada),
        entidades.recibieronDe(donante.getId(), List.of(activa, deBaja, vencida, donada)));
    assertEquals(Set.of(), entidades.recibieronDe(donante.getId(), List.of()));
  }

  // R11-43 (C5): estado, necesidades vigentes y rubro en SQL; el total respeta los filtros
  @Test
  void entidades_porEstadoNecesidadesYRubro_sinTildesNiMayusculas() {
    UUID subcategoriaId = crearSubcategoria();
    LocalDate hoy = LocalDate.now();
    UUID centro = entidadDelRubro("Barrio Norte", "Centro Comunitario");
    UUID comedor = entidadDelRubro("Comedor Sur", "Comedor");
    UUID sinRubro = entidadDelRubro("Hogar Sin Necesidad", "Hogar");
    UUID deBaja = entidadDelRubro("Merendero De Baja", "Merendero");
    jdbc.update("UPDATE donaciones.entidad_beneficiaria SET activo = false WHERE id = ?", deBaja);
    necesidad(centro, subcategoriaId, true, null);
    necesidad(comedor, subcategoriaId, true, hoy.minusDays(1));
    necesidad(sinRubro, subcategoriaId, false, null);

    Tramo<EntidadBeneficiaria> bajas = conFiltro(new FiltroOrganizaciones(false, null, null), hoy);
    Tramo<EntidadBeneficiaria> activas = conFiltro(new FiltroOrganizaciones(true, null, null), hoy);
    Tramo<EntidadBeneficiaria> conNecesidades =
        conFiltro(new FiltroOrganizaciones(null, true, null), hoy);
    Tramo<EntidadBeneficiaria> sinNecesidades =
        conFiltro(new FiltroOrganizaciones(null, false, null), hoy);
    Tramo<EntidadBeneficiaria> delRubro =
        conFiltro(new FiltroOrganizaciones(null, null, "centro comunitário"), hoy);
    Tramo<EntidadBeneficiaria> activasConNecesidadDelRubro =
        conFiltro(new FiltroOrganizaciones(true, true, "Comedor"), hoy);

    assertEquals(List.of(deBaja), idsDe(bajas));
    assertEquals(1, bajas.total());
    assertEquals(3, activas.total());
    // La vencida ayer y la inactiva no cuentan como vigentes
    assertEquals(List.of(centro), idsDe(conNecesidades));
    assertEquals(1, conNecesidades.total());
    assertEquals(List.of(comedor, sinRubro, deBaja), idsDe(sinNecesidades));
    assertEquals(3, sinNecesidades.total());
    assertEquals(List.of(centro), idsDe(delRubro));
    assertEquals(0, activasConNecesidadDelRubro.total());
  }

  private Tramo<EntidadBeneficiaria> conFiltro(FiltroOrganizaciones filtro, LocalDate hoy) {
    return entidades.buscar(new CriterioEntidades(null, false, null, hoy, null, filtro, PRIMERA));
  }

  private UUID entidadDelRubro(String razonSocial, String rubro) {
    Juridica juridica = juridica(razonSocial, TipoJuridico.ONG);
    personas.save(juridica);
    UUID id = entidades.save(new EntidadBeneficiaria(juridica.getId())).getId();
    jdbc.update("UPDATE donaciones.juridica SET rubro = ? WHERE id = ?", rubro, id);
    return id;
  }

  private UUID entidadLlamada(String razonSocial) {
    Juridica juridica = juridica(razonSocial, TipoJuridico.ONG);
    personas.save(juridica);
    return entidades.save(new EntidadBeneficiaria(juridica.getId())).getId();
  }

  private void necesidad(UUID entidadId, UUID subcategoriaId, boolean activa, LocalDate hasta) {
    jdbc.update(
        "INSERT INTO donaciones.necesidad (id, tipo_necesidad, subcategoria_id,"
            + " entidad_beneficiaria_id, cantidad_necesitada, descripcion, fecha_inicio, activa,"
            + " fecha_limite) VALUES (?, 'EXTRAORDINARIA', ?, ?, 10, 'Arroz', now(), ?, ?)",
        UUID.randomUUID(),
        subcategoriaId,
        entidadId,
        activa,
        hasta);
  }

  private void asignarA(Donante donante, UUID subcategoriaId, UUID entidadId) {
    Bien arroz = BienMother.alimentoPerecedero("Arroz");
    Donacion donacion = DonacionMother.segmentada(donante.getId(), arroz, 10);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(arroz, subcategoriaId), 10));
    DonacionIndependiente asignada = independiente(normalizado);
    asignada.asignar(ACTOR, new DestinoAsignacion(null, null, entidadId));
    independientes.save(asignada);
  }

  private static List<UUID> idsDe(Tramo<EntidadBeneficiaria> tramo) {
    return tramo.elementos().stream().map(EntidadBeneficiaria::getId).toList();
  }

  // ===================== donaciones e independientes =====================

  @Test
  void donaciones_porDonanteTextoYFechas() {
    Donante donante = donanteDe(humana("Juan", "Pérez"), true, LocalDateTime.now());
    Bien campera = BienMother.alimentoPerecedero("Campera de abrigo");
    Donacion conCampera = DonacionMother.segmentada(donante.getId(), campera, 1);
    donaciones.save(conCampera);
    donaciones.save(DonacionMother.simple(donante.getId()));

    Tramo<Donacion> delDonante =
        donaciones.buscar(
            new CriterioDonaciones(
                donante.getId(), null, null, null, OrdenPorFecha.RECIENTES, PRIMERA));
    Tramo<Donacion> porTexto =
        donaciones.buscar(
            new CriterioDonaciones(null, null, null, "CAMPERA", OrdenPorFecha.RECIENTES, PRIMERA));
    Tramo<Donacion> fueraDeRango =
        donaciones.buscar(
            new CriterioDonaciones(
                null,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 2),
                null,
                OrdenPorFecha.RECIENTES,
                PRIMERA));

    assertEquals(2, delDonante.total());
    assertEquals(
        List.of(conCampera.getId()), porTexto.elementos().stream().map(Donacion::getId).toList());
    assertEquals(0, fueraDeRango.total());
  }

  @Test
  void independientes_filtrosVariosEstadosTextoYConteos() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Juan", "Pérez"), true, LocalDateTime.now());
    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion donacion = DonacionMother.segmentada(donante.getId(), fideos, 30);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
    DonacionIndependiente enDeposito = independiente(normalizado);
    Juridica comedor = juridica("Comedor", TipoJuridico.ONG);
    personas.save(comedor);
    entidades.save(new EntidadBeneficiaria(comedor.getId()));
    DonacionIndependiente asignada = independiente(normalizado);
    asignada.asignar(ACTOR, new DestinoAsignacion(null, null, comedor.getId()));
    independientes.save(enDeposito);
    independientes.save(asignada);

    Tramo<DonacionIndependiente> delDonanteEnDeposito =
        independientes.buscar(
            new CriterioDonacionesIndependientes(
                Set.of(TipoEstadoDonacion.EN_DEPOSITO),
                subcategoriaId,
                null,
                donante.getId(),
                null,
                null,
                null,
                null,
                "FIDEOS",
                OrdenDonacionesIndependientes.RECIENTES,
                PRIMERA));
    Tramo<DonacionIndependiente> dosEstados =
        independientes.buscar(
            new CriterioDonacionesIndependientes(
                Set.of(TipoEstadoDonacion.EN_DEPOSITO, TipoEstadoDonacion.ASIGNACION_REALIZADA),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OrdenDonacionesIndependientes.ANTIGUAS,
                new Paginado(0, 1)));
    Tramo<DonacionIndependiente> otroTexto =
        independientes.buscar(
            new CriterioDonacionesIndependientes(
                Set.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "arroz",
                OrdenDonacionesIndependientes.RECIENTES,
                PRIMERA));
    Map<TipoEstadoDonacion, Long> porEstado = independientes.contarPorEstado();

    assertEquals(
        List.of(enDeposito.getId()),
        delDonanteEnDeposito.elementos().stream().map(DonacionIndependiente::getId).toList());
    assertEquals(2, dosEstados.total());
    assertEquals(1, dosEstados.elementos().size());
    assertEquals(0, otroTexto.total());
    assertEquals(1L, porEstado.get(TipoEstadoDonacion.EN_DEPOSITO));
    assertEquals(1L, porEstado.get(TipoEstadoDonacion.ASIGNACION_REALIZADA));
    assertEquals(
        1,
        independientes.contarQuePasaronA(
            TipoEstadoDonacion.ASIGNACION_REALIZADA,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1)));
    // El extremo superior es exclusivo y una ventana pasada no cuenta el cambio de hoy
    assertEquals(
        0,
        independientes.contarQuePasaronA(
            TipoEstadoDonacion.ASIGNACION_REALIZADA,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(5)));
  }

  // R11-43 (C7): lo que hay que atender primero, y dentro de cada grupo la más reciente
  @Test
  void independientes_ordenAtencion_porGrupoYDespuesLaMasReciente() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Ana", "Orden"), true, LocalDateTime.now());
    UUID comedor = entidadLlamada("Comedor Atencion");
    LocalDateTime base = LocalDateTime.of(2026, 11, 1, 10, 0);
    UUID recibida =
        asignadaEn(donante, subcategoriaId, comedor, null, "ENTREGADA", base.plusDays(9));
    UUID porSalir =
        asignadaEn(donante, subcategoriaId, comedor, null, "ASIGNACION_REALIZADA", base);
    UUID enDeposito =
        asignadaEn(donante, subcategoriaId, comedor, null, "EN_DEPOSITO", base.plusDays(2));
    UUID enCamino = asignadaEn(donante, subcategoriaId, comedor, null, "EN_TRASLADO", base);
    UUID vencida = asignadaEn(donante, subcategoriaId, comedor, null, "VENCIDA", base);
    UUID fallida =
        asignadaEn(donante, subcategoriaId, comedor, null, "ENTREGA_FALLIDA", base.plusDays(1));
    UUID enPuertaVieja = asignadaEn(donante, subcategoriaId, comedor, null, "EN_PUERTA", base);
    UUID enPuertaNueva =
        asignadaEn(donante, subcategoriaId, comedor, null, "EN_PUERTA", base.plusDays(3));

    Tramo<DonacionIndependiente> atencion =
        independientes.buscar(
            new CriterioDonacionesIndependientes(
                Set.of(),
                null,
                null,
                null,
                comedor,
                null,
                null,
                null,
                null,
                OrdenDonacionesIndependientes.ATENCION,
                PRIMERA));
    Tramo<DonacionIndependiente> soloLasQueEsperan =
        independientes.buscar(
            new CriterioDonacionesIndependientes(
                Set.of(TipoEstadoDonacion.EN_PUERTA, TipoEstadoDonacion.EN_TRASLADO),
                null,
                null,
                null,
                comedor,
                null,
                null,
                null,
                null,
                OrdenDonacionesIndependientes.ATENCION,
                new Paginado(0, 2)));

    assertEquals(
        List.of(
            enPuertaNueva,
            enPuertaVieja,
            fallida,
            vencida,
            enCamino,
            enDeposito,
            porSalir,
            recibida),
        atencion.elementos().stream().map(DonacionIndependiente::getId).toList());
    assertEquals(
        List.of(enPuertaNueva, enPuertaVieja),
        soloLasQueEsperan.elementos().stream().map(DonacionIndependiente::getId).toList());
    assertEquals(3, soloLasQueEsperan.total());
  }

  // R11-43: el acta de Confirmar recepción lee solo la donación pedida, de esa organización
  @Test
  void independientes_porIdDeLaOrganizacion_soloEsaYNuncaLaDeOtra() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Ana", "Acta"), true, LocalDateTime.now());
    UUID comedor = entidadLlamada("Comedor Acta");
    UUID otroComedor = entidadLlamada("Comedor Vecino");
    LocalDateTime base = LocalDateTime.of(2026, 11, 1, 10, 0);
    UUID pedida = asignadaEn(donante, subcategoriaId, comedor, null, "EN_PUERTA", base);
    asignadaEn(donante, subcategoriaId, comedor, null, "EN_PUERTA", base.plusDays(1));
    UUID ajena = asignadaEn(donante, subcategoriaId, otroComedor, null, "EN_PUERTA", base);

    Tramo<DonacionIndependiente> laPedida = independientes.buscar(porId(comedor, pedida));
    Tramo<DonacionIndependiente> laAjena = independientes.buscar(porId(comedor, ajena));

    assertEquals(
        List.of(pedida), laPedida.elementos().stream().map(DonacionIndependiente::getId).toList());
    assertEquals(1, laPedida.total());
    assertEquals(0, laAjena.total());
  }

  private static CriterioDonacionesIndependientes porId(UUID entidadId, UUID id) {
    return new CriterioDonacionesIndependientes(
        Set.of(TipoEstadoDonacion.EN_PUERTA, TipoEstadoDonacion.EN_TRASLADO),
        null,
        null,
        null,
        entidadId,
        null,
        null,
        null,
        null,
        OrdenDonacionesIndependientes.ATENCION,
        new Paginado(0, 1),
        id);
  }

  // R11-43 (C6): las cifras de una organización, por estado y por necesidad, en una consulta
  @Test
  void independientes_conteosDeUnaOrganizacion_porNecesidadYEstado() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Ana", "Conteo"), true, LocalDateTime.now());
    UUID comedor = entidadLlamada("Comedor Conteos");
    UUID otra = entidadLlamada("Otra Organizacion");
    UUID arroz = necesidadDe(comedor, subcategoriaId);
    LocalDateTime hoy = LocalDateTime.now();
    asignadaEn(donante, subcategoriaId, comedor, arroz, "EN_PUERTA", hoy);
    asignadaEn(donante, subcategoriaId, comedor, arroz, "EN_PUERTA", hoy);
    asignadaEn(donante, subcategoriaId, comedor, arroz, "ENTREGADA", hoy);
    asignadaEn(donante, subcategoriaId, comedor, null, "EN_TRASLADO", hoy);
    asignadaEn(donante, subcategoriaId, otra, null, "EN_PUERTA", hoy);

    Set<ConteoAsignadas> todas = Set.copyOf(independientes.contarAsignadas(comedor, null));
    Set<ConteoAsignadas> deArroz = Set.copyOf(independientes.contarAsignadas(comedor, arroz));

    assertEquals(
        Set.of(
            new ConteoAsignadas(arroz, TipoEstadoDonacion.EN_PUERTA, 2),
            new ConteoAsignadas(arroz, TipoEstadoDonacion.ENTREGADA, 1),
            new ConteoAsignadas(null, TipoEstadoDonacion.EN_TRASLADO, 1)),
        todas);
    assertEquals(
        Set.of(
            new ConteoAsignadas(arroz, TipoEstadoDonacion.EN_PUERTA, 2),
            new ConteoAsignadas(arroz, TipoEstadoDonacion.ENTREGADA, 1)),
        deArroz);
    // Las cuatro del comedor pasaron hoy por ASIGNACION_REALIZADA; tres son de la necesidad
    assertEquals(
        4,
        independientes.contarQuePasaronA(
            TipoEstadoDonacion.ASIGNACION_REALIZADA,
            hoy.minusDays(1),
            hoy.plusDays(1),
            comedor,
            null));
    assertEquals(
        3,
        independientes.contarQuePasaronA(
            TipoEstadoDonacion.ASIGNACION_REALIZADA,
            hoy.minusDays(1),
            hoy.plusDays(1),
            comedor,
            arroz));
    assertEquals(
        0,
        independientes.contarQuePasaronA(
            TipoEstadoDonacion.ENTREGADA, hoy.minusDays(1), hoy.plusDays(1), comedor, null));
  }

  // R11-42: las series del inicio del admin, contadas con GROUP BY en la base
  @Test
  void independientes_seriesPorMesYPorCategoria_delTramoPedido() {
    UUID subcategoriaId = crearSubcategoria();
    UUID categoriaId =
        jdbc.queryForObject(
            "SELECT id_categoria FROM donaciones.subcategoria WHERE id = ?",
            UUID.class,
            subcategoriaId);
    Donante donante = donanteDe(humana("Ana", "Series"), true, LocalDateTime.now());
    UUID comedor = entidadLlamada("Comedor Series");
    // Fechas a mitad de mes: la base guarda en UTC y el JVM puede estar en otra zona
    UUID deSeptiembre =
        asignadaEn(
            donante,
            subcategoriaId,
            comedor,
            null,
            "EN_DEPOSITO",
            LocalDateTime.of(2026, 9, 3, 12, 0));
    asignadaEn(
        donante,
        subcategoriaId,
        comedor,
        null,
        "EN_DEPOSITO",
        LocalDateTime.of(2026, 10, 2, 12, 0));
    asignadaEn(
        donante,
        subcategoriaId,
        comedor,
        null,
        "EN_DEPOSITO",
        LocalDateTime.of(2026, 10, 20, 8, 0));
    // Fuera de los meses pedidos
    asignadaEn(
        donante,
        subcategoriaId,
        comedor,
        null,
        "EN_DEPOSITO",
        LocalDateTime.of(2026, 12, 10, 9, 0));
    jdbc.update(
        "UPDATE donaciones.cambio_estado_donacion_independiente SET \"timestamp\" = ?"
            + " WHERE id_donacion_independiente = ?",
        LocalDateTime.of(2026, 10, 5, 12, 0),
        deSeptiembre);
    YearMonth septiembre = YearMonth.of(2026, 9);
    YearMonth octubre = YearMonth.of(2026, 10);

    assertEquals(
        Map.of(septiembre, 1L, octubre, 2L),
        independientes.contarRegistradasPorMes(septiembre, octubre));
    assertEquals(
        Map.of(septiembre, 0L, octubre, 1L),
        independientes.contarQuePasaronAPorMes(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, septiembre, octubre));
    LocalDateTime desde = septiembre.atDay(1).atStartOfDay();
    LocalDateTime hasta = octubre.plusMonths(1).atDay(1).atStartOfDay();
    assertEquals(
        Map.of(categoriaId, 3L), independientes.contarRegistradasPorCategoria(desde, hasta));
    assertEquals(
        Map.of(categoriaId, 1L),
        independientes.contarQuePasaronAPorCategoria(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, desde, hasta));
    // La fecha se escribió sin la conversión de zona de Hibernate: alcanza con el día
    assertEquals(
        Optional.of(LocalDate.of(2026, 9, 3)),
        independientes.primeraFechaRegistro().map(LocalDateTime::toLocalDate));
  }

  @Test
  void independientes_sinNingunaNoHayPrimeraFecha() {
    assertEquals(Optional.empty(), independientes.primeraFechaRegistro());
  }

  /**
   * Una asignada a la organización, con el estado y la fecha de registro puestos en la base: el
   * orden y los conteos leen esas columnas.
   */
  // R11-34: el filtro "vence a más tardar" y el orden por el vencimiento más próximo
  @Test
  void independientes_venceHastaYOrdenVence_laMasProximaPrimeroYSinVencimientoAlFinal() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Ana", "Vence"), true, LocalDateTime.now());
    LocalDate dia = LocalDate.of(2027, 3, 10);
    UUID sinVencimiento = queVence(donante, subcategoriaId, null);
    UUID tarde = queVence(donante, subcategoriaId, dia.plusDays(30));
    UUID justo = queVence(donante, subcategoriaId, dia);
    UUID antes = queVence(donante, subcategoriaId, dia.minusDays(3));

    Tramo<DonacionIndependiente> hastaElDia = independientes.buscar(porVencimiento(dia));
    Tramo<DonacionIndependiente> todas = independientes.buscar(porVencimiento(null));

    assertEquals(
        List.of(antes, justo),
        hastaElDia.elementos().stream().map(DonacionIndependiente::getId).toList());
    assertEquals(2, hastaElDia.total());
    assertEquals(
        List.of(antes, justo, tarde, sinVencimiento),
        todas.elementos().stream().map(DonacionIndependiente::getId).toList());
  }

  private static CriterioDonacionesIndependientes porVencimiento(LocalDate venceHasta) {
    return new CriterioDonacionesIndependientes(
        Set.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        OrdenDonacionesIndependientes.VENCE,
        PRIMERA,
        null,
        venceHasta);
  }

  /** Una en el depósito con un bien que vence {@code fecha} (null: no vence). */
  private UUID queVence(Donante donante, UUID subcategoriaId, LocalDate fecha) {
    Bien leche = new Bien("Leche en polvo", null, fecha, null, 1.0, 0.01);
    Donacion donacion = DonacionMother.segmentada(donante.getId(), leche, 10);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(leche, subcategoriaId), 10));
    DonacionIndependiente independiente = independiente(normalizado);
    independientes.save(independiente);
    return independiente.getId();
  }

  // R11-34: el período de alta, con días inclusivos
  @Test
  void donantes_porPeriodoDeAlta_diasInclusivos() {
    LocalDateTime dia = LocalDateTime.of(2026, 11, 10, 23, 59);
    donanteDe(humana("Ana", "Antes"), true, dia.minusDays(5));
    Donante enElDia = donanteDe(humana("Ana", "Justo"), true, dia);
    donanteDe(humana("Ana", "Despues"), true, dia.plusDays(1));

    Tramo<Donante> delDia =
        donantes.buscar(
            new CriterioDonantes(
                null,
                null,
                null,
                null,
                null,
                null,
                OrdenDonantes.NUEVOS,
                PRIMERA,
                dia.toLocalDate(),
                dia.toLocalDate()));

    assertEquals(List.of(enElDia.getId()), ids(delDia));
    assertEquals(1, delDia.total());
  }

  private UUID asignadaEn(
      Donante donante,
      UUID subcategoriaId,
      UUID entidadId,
      UUID necesidadId,
      String estado,
      LocalDateTime registro) {
    Bien arroz = BienMother.alimentoPerecedero("Arroz");
    Donacion donacion = DonacionMother.segmentada(donante.getId(), arroz, 10);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(arroz, subcategoriaId), 10));
    DonacionIndependiente asignada = independiente(normalizado);
    asignada.asignar(ACTOR, new DestinoAsignacion(necesidadId, null, entidadId));
    independientes.save(asignada);
    jdbc.update(
        "UPDATE donaciones.donacion_independiente SET estado_actual = ?, fecha_registro = ?"
            + " WHERE id = ?",
        estado,
        registro,
        asignada.getId());
    return asignada.getId();
  }

  private UUID necesidadDe(UUID entidadId, UUID subcategoriaId) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.necesidad (id, tipo_necesidad, subcategoria_id,"
            + " entidad_beneficiaria_id, cantidad_necesitada, descripcion, fecha_inicio, activa)"
            + " VALUES (?, 'EXTRAORDINARIA', ?, ?, 10, 'Arroz', now(), true)",
        id,
        subcategoriaId,
        entidadId);
    return id;
  }

  @Test
  void independientes_porVencer_cuentaLasDelDepositoEnLaVentana_yDaLaMasProxima() {
    UUID subcategoriaId = crearSubcategoria();
    Donante donante = donanteDe(humana("Ana", "Prueba"), true, LocalDateTime.now());
    LocalDate hoy = LocalDate.of(2027, 12, 1);
    DonacionIndependiente venceEn2 =
        independiente(normalizadoQueVence(donante, subcategoriaId, hoy.plusDays(2)));
    DonacionIndependiente venceEn5 =
        independiente(normalizadoQueVence(donante, subcategoriaId, hoy.plusDays(5)));
    DonacionIndependiente fueraDeLaVentana =
        independiente(normalizadoQueVence(donante, subcategoriaId, hoy.plusDays(20)));
    DonacionIndependiente asignada =
        independiente(normalizadoQueVence(donante, subcategoriaId, hoy.plusDays(1)));
    Juridica comedor = juridica("Comedor", TipoJuridico.ONG);
    personas.save(comedor);
    entidades.save(new EntidadBeneficiaria(comedor.getId()));
    asignada.asignar(ACTOR, new DestinoAsignacion(null, null, comedor.getId()));
    independientes.save(venceEn2);
    independientes.save(venceEn5);
    independientes.save(fueraDeLaVentana);
    independientes.save(asignada);

    PorVencer porVencer = independientes.buscarPorVencer(hoy, hoy.plusDays(7));

    assertEquals(2, porVencer.cantidad());
    assertEquals(venceEn2.getId(), porVencer.proximaId());
    assertEquals(hoy.plusDays(2), porVencer.proximaVence());
  }

  private ItemDonacionNormalizado normalizadoQueVence(
      Donante donante, UUID subcategoriaId, LocalDate vence) {
    Bien leche = new Bien("Leche larga vida", null, vence, null, 1.0, 0.002);
    Donacion donacion = DonacionMother.segmentada(donante.getId(), leche, 10);
    donaciones.save(donacion);
    return itemsNormalizados.save(
        new ItemDonacionNormalizado(
            donacion.getId(), BienMother.aceptado(leche, subcategoriaId), 10));
  }

  @Test
  void independientes_porVencer_sinNingunaDevuelveCero() {
    PorVencer porVencer =
        independientes.buscarPorVencer(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 8));

    assertEquals(0, porVencer.cantidad());
    assertNull(porVencer.proximaId());
  }

  private static DonacionIndependiente independiente(ItemDonacionNormalizado normalizado) {
    return new DonacionIndependiente(
        normalizado.getDonacionOriginalId(),
        List.of(new ItemDonacionIndependiente(normalizado.getId(), normalizado.getBien(), 5)));
  }
}
