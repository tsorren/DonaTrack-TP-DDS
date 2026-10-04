package grupo5.logistica.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.infrastructure.ComunicadorEventosLogisticaRabbit;
import grupo5.logistica.infrastructure.GeneradorDeURLSeguimiento;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.infrastructure.persistencia.adapters.CamionRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.ChoferesRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.EntregasRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.EventosEntregaRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.RutasRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.SolicitudPlanificacionRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.adapters.SolicitudesTransicionEntregaRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.LocalidadEntity;
import grupo5.logistica.infrastructure.persistencia.entities.PaisEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ProvinciaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.CamionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.ChoferPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.EntregaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.RutaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.SolicitudPlanificacionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataCamionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataChoferRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataDireccionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEventoEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataLocalidadRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataPaisRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataProvinciaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataRutaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataSolicitudPlanificacionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataSolicitudTransicionEntregaRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
import grupo5.logistica.models.entities.entregas.RevisionEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.impl.EventosEntregaRepository;
import grupo5.logistica.models.repositories.impl.SolicitudesTransicionEntregaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

class RepositoriosJpaAdaptersTest {

  @Test
  void camionAdapter_deberiaEjecutarOperacionesCrudYConsultasConRutaDerivada() {
    SpringDataCamionRepository springDataRepo = mock(SpringDataCamionRepository.class);
    SpringDataRutaRepository rutaRepo = mock(SpringDataRutaRepository.class);
    CamionPersistenciaMapper mapper = new CamionPersistenciaMapper();
    CamionRepositoryJpaAdapter adapter =
        new CamionRepositoryJpaAdapter(springDataRepo, rutaRepo, mapper);

    Camion camion = new Camion("AB123CD", 50.0f, 2000.0f, 3.0f);
    when(springDataRepo.findById(camion.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Camion saved = adapter.save(camion);
    assertEquals(camion.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(camion)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
    assertTrue(adapter.findByEstado(null).isEmpty());
    assertTrue(adapter.findByPatente(null).isEmpty());
    assertTrue(adapter.findByPatente("   ").isEmpty());

    when(springDataRepo.findByEstadoCamion(EstadoCamion.DISPONIBLE))
        .thenReturn(List.of(mapper.toEntity(camion)));
    when(springDataRepo.findByEstadoCamionNot(EstadoCamion.DESHABILITADO))
        .thenReturn(List.of(mapper.toEntity(camion)));
    when(springDataRepo.findByPatenteIgnoreCase("AB123CD"))
        .thenReturn(Optional.of(mapper.toEntity(camion)));
    when(rutaRepo.findFirstByIdCamionAndEstado(camion.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.empty());

    assertEquals(1, adapter.findByEstado(EstadoCamion.DISPONIBLE).size());
    assertEquals(1, adapter.findActivos().size());
    assertEquals(1, adapter.findDisponibles().size());
    assertTrue(adapter.findByPatente("ab-123-cd").isPresent());

    UUID rutaActivaId = UUID.randomUUID();
    camion.asignarARuta(rutaActivaId);
    RutaEntity rutaEntity = new RutaEntity();
    rutaEntity.setIdRuta(rutaActivaId);
    when(springDataRepo.findByEstadoCamion(EstadoCamion.EN_RUTA))
        .thenReturn(List.of(mapper.toEntity(camion)));
    when(rutaRepo.findFirstByIdCamionAndEstado(camion.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.of(rutaEntity));
    assertEquals(rutaActivaId, adapter.findByEstado(EstadoCamion.EN_RUTA).getFirst().getRutaId());
  }

  @Test
  void choferAdapter_deberiaEjecutarOperacionesCrudYConsultasConRutaDerivada() {
    SpringDataChoferRepository springDataRepo = mock(SpringDataChoferRepository.class);
    SpringDataRutaRepository rutaRepo = mock(SpringDataRutaRepository.class);
    ChoferPersistenciaMapper mapper = new ChoferPersistenciaMapper();
    ChoferesRepositoryJpaAdapter adapter =
        new ChoferesRepositoryJpaAdapter(springDataRepo, rutaRepo, mapper);

    Chofer chofer = new Chofer("Luis", "Paz", "LIC-50", "1122334455");
    when(springDataRepo.findById(chofer.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Chofer saved = adapter.save(chofer);
    assertEquals(chofer.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(chofer)).size());
    assertTrue(adapter.saveAll(null).isEmpty());

    when(springDataRepo.findByEstadoChoferNot(EstadoChofer.DESHABILITADO))
        .thenReturn(List.of(mapper.toEntity(chofer)));
    when(springDataRepo.findByEstadoChofer(EstadoChofer.DISPONIBLE))
        .thenReturn(List.of(mapper.toEntity(chofer)));
    when(rutaRepo.findFirstByIdChoferAndEstado(chofer.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.empty());

    assertEquals(1, adapter.findActivos().size());
    assertEquals(1, adapter.findDisponibles().size());

    UUID rutaActivaId = UUID.randomUUID();
    chofer.asignarARuta(rutaActivaId);
    RutaEntity rutaEntity = new RutaEntity();
    rutaEntity.setIdRuta(rutaActivaId);
    when(springDataRepo.findByEstadoChoferNot(EstadoChofer.DESHABILITADO))
        .thenReturn(List.of(mapper.toEntity(chofer)));
    when(rutaRepo.findFirstByIdChoferAndEstado(chofer.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.of(rutaEntity));
    assertEquals(rutaActivaId, adapter.findActivos().getFirst().getRutaId());
  }

  @Test
  void rutasAdapter_deberiaGuardarYConsultarRutas() {
    SpringDataRutaRepository springDataRepo = mock(SpringDataRutaRepository.class);
    SpringDataEntregaRepository entregaRepo = mock(SpringDataEntregaRepository.class);
    RutaPersistenciaMapper mapper = new RutaPersistenciaMapper();
    RutasRepositoryJpaAdapter adapter =
        new RutasRepositoryJpaAdapter(springDataRepo, entregaRepo, mapper);

    UUID entregaId = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 12), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(entregaId);

    when(springDataRepo.findById(ruta.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entregaRepo.findByIdRutaOrderByIdEntregaAsc(ruta.getId())).thenReturn(List.of());

    Ruta saved = adapter.save(ruta);
    assertEquals(ruta.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(ruta)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
    assertTrue(adapter.findByFecha(null).isEmpty());
    assertTrue(adapter.findByCamionId(null).isEmpty());
    assertTrue(adapter.findByCamionIdAndFecha(null, LocalDate.now()).isEmpty());
    assertTrue(adapter.findByCamionIdAndFecha(UUID.randomUUID(), null).isEmpty());

    RutaEntity entity = mapper.toEntity(ruta);
    when(springDataRepo.findByFecha(ruta.getFecha())).thenReturn(List.of(entity));
    when(springDataRepo.findByIdCamion(ruta.getCamionId())).thenReturn(List.of(entity));
    when(springDataRepo.findByIdCamionAndFecha(ruta.getCamionId(), ruta.getFecha()))
        .thenReturn(List.of(entity));

    assertEquals(1, adapter.findByFecha(ruta.getFecha()).size());
    assertEquals(1, adapter.findByCamionId(ruta.getCamionId()).size());
    assertEquals(1, adapter.findByCamionIdAndFecha(ruta.getCamionId(), ruta.getFecha()).size());
  }

  @Test
  void entregasAdapter_deberiaPersistirGeografiaInmutableYConsultarEntregas() {
    SpringDataEntregaRepository springDataRepo = mock(SpringDataEntregaRepository.class);
    SpringDataPaisRepository paisRepo = mock(SpringDataPaisRepository.class);
    SpringDataProvinciaRepository provinciaRepo = mock(SpringDataProvinciaRepository.class);
    SpringDataLocalidadRepository localidadRepo = mock(SpringDataLocalidadRepository.class);
    SpringDataDireccionRepository direccionRepo = mock(SpringDataDireccionRepository.class);
    EntregaPersistenciaMapper mapper = new EntregaPersistenciaMapper();

    EntregasRepositoryJpaAdapter adapter =
        new EntregasRepositoryJpaAdapter(
            springDataRepo, paisRepo, provinciaRepo, localidadRepo, direccionRepo, mapper);

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Callao", 500, null, null, "1022", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 10.0f, 1.0f);

    when(springDataRepo.findById(entrega.getId())).thenReturn(Optional.empty());
    when(paisRepo.findByNombreIgnoreCase("Argentina")).thenReturn(Optional.empty());
    when(paisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(provinciaRepo.findByPais_IdPaisAndNombreIgnoreCase(any(), any()))
        .thenReturn(Optional.empty());
    when(provinciaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(localidadRepo.findByProvincia_IdProvinciaAndNombreIgnoreCase(any(), any()))
        .thenReturn(Optional.empty());
    when(localidadRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(direccionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(springDataRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Entrega saved = adapter.save(entrega);
    assertEquals(entrega.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(entrega)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
    assertTrue(adapter.findByEstado(null).isEmpty());
    assertTrue(adapter.findByRutaId(null).isEmpty());
    assertFalse(adapter.existsByIdDonacion(null));

    PaisEntity pais = new PaisEntity(UUID.randomUUID(), "Argentina");
    ProvinciaEntity prov = new ProvinciaEntity(UUID.randomUUID(), pais, "Buenos Aires");
    LocalidadEntity loc = new LocalidadEntity(UUID.randomUUID(), prov, "CABA");
    var entity = mapper.toEntity(entrega, null, mapper.construirDireccionNueva(dir, loc));

    when(springDataRepo.findByEstado(EstadoEntrega.PENDIENTE)).thenReturn(List.of(entity));
    when(springDataRepo.findByIdRutaOrderByIdEntregaAsc(any())).thenReturn(List.of(entity));
    when(springDataRepo.findByIdRutaIsNull()).thenReturn(List.of(entity));
    when(springDataRepo.existsByIdDonacion(entrega.getIdDonacion())).thenReturn(true);

    assertEquals(1, adapter.findByEstado(EstadoEntrega.PENDIENTE).size());
    assertEquals(1, adapter.findByRutaId(UUID.randomUUID()).size());
    assertEquals(1, adapter.findSinRuta().size());
    assertTrue(adapter.existsByIdDonacion(entrega.getIdDonacion()));
  }

  @Test
  void solicitudPlanificacionAdapter_deberiaGuardarYSincronizar() {
    SpringDataSolicitudPlanificacionRepository springDataRepo =
        mock(SpringDataSolicitudPlanificacionRepository.class);
    SolicitudPlanificacionPersistenciaMapper mapper =
        new SolicitudPlanificacionPersistenciaMapper();
    SolicitudPlanificacionRepositoryJpaAdapter adapter =
        new SolicitudPlanificacionRepositoryJpaAdapter(springDataRepo, mapper);

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(), LocalDate.of(2026, 10, 15), 2, "http://localhost/cb");

    when(springDataRepo.findById(solicitud.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    SolicitudPlanificacion saved = adapter.save(solicitud);
    assertEquals(solicitud.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(solicitud)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
  }

  @Test
  void repositoriosAuditoriaJpaYMemoria_deberianRegistrarTransicionesYEventos() {
    SpringDataSolicitudTransicionEntregaRepository transicionSpringRepo =
        mock(SpringDataSolicitudTransicionEntregaRepository.class);
    SolicitudesTransicionEntregaRepositoryJpaAdapter transicionJpaAdapter =
        new SolicitudesTransicionEntregaRepositoryJpaAdapter(transicionSpringRepo);

    SpringDataEventoEntregaRepository eventoSpringRepo =
        mock(SpringDataEventoEntregaRepository.class);
    EventosEntregaRepositoryJpaAdapter eventoJpaAdapter =
        new EventosEntregaRepositoryJpaAdapter(eventoSpringRepo);

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Salta", 100, null, null, "1074", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 5.0f, 0.5f);

    transicionJpaAdapter.registrar(null);
    transicionJpaAdapter.registrar(new ConfirmacionRecepcion(entrega, "Actor", "http://foto"));
    transicionJpaAdapter.registrar(new NoRecepcion(entrega, "Actor", "Cerrado", true));
    transicionJpaAdapter.registrar(new RevisionEntrega(entrega, "Actor"));
    transicionJpaAdapter.registrar(new RegresoDeposito(entrega, "Actor"));

    eventoJpaAdapter.registrarRutaAsignada(null, entrega);
    eventoJpaAdapter.registrarEntregaExitosa(null);
    eventoJpaAdapter.registrarEntregaFallida(null);

    EventoRutaAsignada evRuta = new EventoRutaAsignada(UUID.randomUUID(), entrega.getId());
    EntregaConfirmada evExito =
        new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), UUID.randomUUID());
    EntregaFallida evFallo =
        new EntregaFallida(entrega.getId(), entrega.getIdDonacion(), "Fallo", false);

    eventoJpaAdapter.registrarRutaAsignada(evRuta, entrega);
    eventoJpaAdapter.registrarEntregaExitosa(evExito);
    eventoJpaAdapter.registrarEntregaFallida(evFallo);

    SolicitudesTransicionEntregaRepository memTransicionRepo =
        new SolicitudesTransicionEntregaRepository();
    memTransicionRepo.registrar(null);
    memTransicionRepo.registrar(new ConfirmacionRecepcion(entrega, "Actor", null));
    assertEquals(1, memTransicionRepo.findAll().size());
    memTransicionRepo.deleteAll();
    assertTrue(memTransicionRepo.findAll().isEmpty());

    EventosEntregaRepository memEventoRepo = new EventosEntregaRepository();
    memEventoRepo.registrarRutaAsignada(null, entrega);
    memEventoRepo.registrarRutaAsignada(evRuta, entrega);
    memEventoRepo.registrarEntregaExitosa(evExito);
    memEventoRepo.registrarEntregaFallida(evFallo);
    assertEquals(3, memEventoRepo.findAll().size());
    memEventoRepo.deleteAll();
    assertTrue(memEventoRepo.findAll().isEmpty());
  }

  @Test
  void logisticaEventPublisherYComunicador_deberianSoportarAfterCommitYAuditoria() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
    LogisticaEventPublisher publisherConSpring =
        new LogisticaEventPublisher(rabbitTemplate, appPublisher);
    LogisticaEventPublisher publisherDirecto = new LogisticaEventPublisher(rabbitTemplate);

    var evRutaAsignada =
        new grupo5.logistica.dto.eventos.EventoRutaAsignada(
            UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());
    var evRutaIniciada =
        new grupo5.logistica.dto.eventos.EventoRutaIniciada(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "AB123CD",
            List.of(UUID.randomUUID()),
            LocalDateTime.now(),
            "http://mapa");
    var evExitosa =
        new EventoEntregaExitosa(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "AB123CD",
            LocalDateTime.now());
    var evFallida =
        new EventoEntregaFallida(
            UUID.randomUUID(), UUID.randomUUID(), "Ausente", LocalDateTime.now(), true);

    publisherConSpring.publicarRutaAsignada(evRutaAsignada);
    publisherConSpring.publicarRutaIniciada(evRutaIniciada);
    publisherConSpring.publicarEntregaExitosa(evExitosa);
    publisherConSpring.publicarEntregaFallida(evFallida);
    verify(appPublisher).publishEvent(evRutaAsignada);
    verify(appPublisher).publishEvent(evRutaIniciada);
    verify(appPublisher).publishEvent(evExitosa);
    verify(appPublisher).publishEvent(evFallida);

    publisherDirecto.publicarRutaAsignada(evRutaAsignada);
    publisherDirecto.publicarRutaIniciada(evRutaIniciada);
    publisherDirecto.publicarEntregaExitosa(evExitosa);
    publisherDirecto.publicarEntregaFallida(evFallida);

    EventosEntregaRepository memEventoRepo = new EventosEntregaRepository();
    ComunicadorEventosLogisticaRabbit comunicador =
        new ComunicadorEventosLogisticaRabbit(
            publisherConSpring, mock(GeneradorDeURLSeguimiento.class), memEventoRepo);

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Salta", 100, null, null, "1074", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 5.0f, 0.5f);
    Camion camion = new Camion("AB123CD", 50.0f, 2000.0f, 3.0f);

    comunicador.comunicarRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), entrega.getId()), entrega);
    comunicador.comunicarEntregaExitosa(
        new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), UUID.randomUUID()), camion);
    comunicador.comunicarEntregaFallida(
        new EntregaFallida(entrega.getId(), entrega.getIdDonacion(), "Motivo", true));

    assertEquals(3, memEventoRepo.findAll().size());
  }
}
