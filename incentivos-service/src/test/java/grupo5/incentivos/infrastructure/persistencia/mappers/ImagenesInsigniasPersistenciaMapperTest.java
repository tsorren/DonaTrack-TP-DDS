package grupo5.incentivos.infrastructure.persistencia.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.incentivos.fixtures.MisionMother;
import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.InsigniaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionEntity;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.storage.IImagenesInsignias;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class ImagenesInsigniasPersistenciaMapperTest {

  private static final String REFERENCIA = "/insignias/explorador.png";
  private static final String URL = "http://localhost:9000/insignias/explorador.png";

  private final IImagenesInsignias imagenes = mock(IImagenesInsignias.class);
  private MisionPersistenciaMapper misionMapper;
  private DonanteIncentivosPersistenciaMapper donanteMapper;

  @BeforeEach
  void preparar() {
    when(imagenes.urlPublica(REFERENCIA)).thenReturn(URL);
    when(imagenes.urlPublica(URL)).thenReturn(URL);
    when(imagenes.referencia(URL)).thenReturn(REFERENCIA);
    when(imagenes.referencia(REFERENCIA)).thenReturn(REFERENCIA);
    misionMapper = new MisionPersistenciaMapper(imagenes);
    donanteMapper = new DonanteIncentivosPersistenciaMapper(misionMapper);
  }

  @Test
  void toEntity_deberiaGuardarLaReferenciaYNoLaUrlAbsolutaDeLaInsigniaDeLaMision() {
    Mision mision = misionConInsignia(URL);

    MisionEntity entity = misionMapper.toEntity(mision);

    assertEquals(REFERENCIA, entity.getInsignia().getImagenUrl());
    assertEquals("Explorador", entity.getInsignia().getNombre());
    verify(imagenes).referencia(URL);
  }

  @Test
  void toDomain_deberiaResolverLasReferenciasViejasQueQuedaronEnLaBd() {
    MisionEntity entity = misionMapper.toEntity(misionConInsignia(URL));
    entity.setInsignia(new InsigniaEmbeddable("Explorador", "desc", REFERENCIA));

    Mision mision = misionMapper.toDomain(entity);

    assertEquals(URL, mision.getInsignia().imagenUrl());
  }

  @Test
  void deberiaMapearSinInsigniaSinConsultarAlPuerto() {
    MisionEntity entity = misionMapper.toEntity(MisionMother.rachaColaborador(3));

    assertNull(entity.getInsignia());
    assertNull(misionMapper.toDomain(entity).getInsignia());
    verifyNoInteractions(imagenes);
  }

  @Test
  void deberiaGuardarLaReferenciaYLeerLaUrlPublicaDeLasInsigniasGanadas() {
    DonanteIncentivos donante =
        new DonanteIncentivos(UUID.randomUUID(), UUID.randomUUID(), "Ana", List.of());
    donante.otorgarInsignia(new Insignia("Explorador", "desc", URL));

    DonanteIncentivosEntity entity = donanteMapper.toEntity(donante);
    assertEquals(REFERENCIA, entity.getInsignias().get(0).getImagenUrl());

    DonanteIncentivos leido = donanteMapper.toDomain(entity);
    assertEquals(URL, leido.getInsignias().get(0).imagenUrl());
  }

  @Test
  void sinPuertoRegistradoDeberiaUsarLaIdentidad() {
    ObjectProvider<IImagenesInsignias> sinBean =
        new StaticListableBeanFactory().getBeanProvider(IImagenesInsignias.class);

    MisionPersistenciaMapper mapper = new MisionPersistenciaMapper(sinBean);

    assertEquals(REFERENCIA, mapper.resolverImagenUrl(REFERENCIA));
    assertNull(mapper.resolverImagenUrl(null));
  }

  @Test
  void conPuertoRegistradoDeberiaUsarlo() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    factory.addBean("imagenesInsignias", imagenes);

    MisionPersistenciaMapper mapper =
        new MisionPersistenciaMapper(factory.getBeanProvider(IImagenesInsignias.class));

    assertEquals(URL, mapper.resolverImagenUrl(REFERENCIA));
  }

  private static Mision misionConInsignia(String imagenUrl) {
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 3);
    mision.setNumeroMision(2);
    mision.setInsignia(new Insignia("Explorador", "desc", imagenUrl));
    return mision;
  }
}
