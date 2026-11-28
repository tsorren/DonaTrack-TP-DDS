package grupo5.donaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.CancelacionDeEntregas;
import grupo5.donaciones.services.impl.EntidadBeneficiariaService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La lectura de entidades por lista de ids (SPEC-11, G2, lote {@code g2-relaciones}): la carga por
 * lotes de {@code Necesidad.entidad} y {@code Donacion.entidad}. Una lectura de entidades y una de
 * jurídicas para todos los ids, con la vista pública (D26), también de las dadas de baja.
 */
class EntidadesPublicasPorIdsServiceTest {

  private IEntidadesBeneficiariasRepository repositorio;
  private IPersonasRepository personas;
  private EntidadBeneficiariaService servicio;

  @BeforeEach
  void setUp() {
    repositorio = mock(IEntidadesBeneficiariasRepository.class);
    personas = mock(IPersonasRepository.class);
    servicio =
        new EntidadBeneficiariaService(
            repositorio,
            personas,
            mock(INecesidadesRepository.class),
            new EntidadBeneficiariaMapper(
                new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper()), personas),
            mock(CancelacionDeEntregas.class),
            mock(IDonantesRepository.class));
  }

  private static Juridica juridica(String razonSocial, String rubro) {
    return new Juridica(PersonaMother.juanPerez(), razonSocial, TipoJuridico.ONG, rubro);
  }

  @Test
  void leeTodasJuntasYDevuelveSoloLaVistaPublica_tambienDeLasDadasDeBaja() {
    Juridica comedor = juridica("Comedor de ejemplo", "Comedor");
    Juridica hogar = juridica("Hogar de ejemplo", "Hogar");
    EntidadBeneficiaria activa = new EntidadBeneficiaria(comedor.getId());
    EntidadBeneficiaria deBaja = new EntidadBeneficiaria(hogar.getId());
    deBaja.darDeBaja();
    Set<UUID> ids = Set.of(activa.getId(), deBaja.getId());
    when(repositorio.findAllById(any())).thenReturn(List.of(activa, deBaja));
    when(personas.findAllById(any())).thenReturn(List.of(comedor, hogar));

    List<EntidadPublicaDTO> publicas = servicio.buscarPublicasPorIds(ids);

    verify(repositorio, times(1)).findAllById(ids);
    verify(personas, times(1)).findAllById(any());
    verify(personas, never()).findById(any());
    assertThat(publicas)
        .containsExactlyInAnyOrder(
            new EntidadPublicaDTO(comedor.getId(), "Comedor de ejemplo", "Comedor"),
            new EntidadPublicaDTO(hogar.getId(), "Hogar de ejemplo", "Hogar"));
  }

  @Test
  void lasActivas_dejanAfueraLasDadasDeBajaConUnaSolaLectura() {
    Juridica comedor = juridica("Comedor de ejemplo", "Comedor");
    Juridica hogar = juridica("Hogar de ejemplo", "Hogar");
    EntidadBeneficiaria activa = new EntidadBeneficiaria(comedor.getId());
    EntidadBeneficiaria deBaja = new EntidadBeneficiaria(hogar.getId());
    deBaja.darDeBaja();
    Set<UUID> ids = Set.of(activa.getId(), deBaja.getId());
    when(repositorio.findAllById(any())).thenReturn(List.of(activa, deBaja));
    when(personas.findAllById(any())).thenReturn(List.of(comedor, hogar));

    assertThat(servicio.buscarActivasPorIds(ids))
        .containsExactly(new EntidadPublicaDTO(comedor.getId(), "Comedor de ejemplo", "Comedor"));
    verify(repositorio, times(1)).findAllById(ids);
  }

  @Test
  void lasActivas_sinIds_noLeen() {
    assertThat(servicio.buscarActivasPorIds(Set.of())).isEmpty();

    verify(repositorio, never()).findAllById(any());
  }

  @Test
  void unaEntidadSinJuridicaNoAparece() {
    EntidadBeneficiaria huerfana = new EntidadBeneficiaria(UUID.randomUUID());
    when(repositorio.findAllById(any())).thenReturn(List.of(huerfana));
    when(personas.findAllById(any())).thenReturn(List.of());

    assertThat(servicio.buscarPublicasPorIds(Set.of(huerfana.getId()))).isEmpty();
  }

  @Test
  void sinIds_noLeeLosRepositorios() {
    assertThat(servicio.buscarPublicasPorIds(Set.of())).isEmpty();

    verify(repositorio, never()).findAllById(any());
    verify(personas, never()).findAllById(any());
  }
}
