package grupo5.donaciones.controllers;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.controllers.impl.CategoriasController;
import grupo5.donaciones.controllers.impl.DonacionesController;
import grupo5.donaciones.controllers.impl.DonacionesIndependientesController;
import grupo5.donaciones.controllers.impl.DonantesController;
import grupo5.donaciones.controllers.impl.EntidadBeneficiariaController;
import grupo5.donaciones.controllers.impl.ItemDonacionNormalizadoController;
import grupo5.donaciones.controllers.impl.NecesidadesController;
import grupo5.donaciones.controllers.impl.PersonasController;
import grupo5.donaciones.controllers.impl.PropuestaDeAsignacionController;
import grupo5.donaciones.controllers.impl.SubcategoriasController;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.ICategoriasService;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.IItemDonacionNormalizadoService;
import grupo5.donaciones.services.INecesidadesService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import grupo5.donaciones.services.ISubcategoriasService;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  CategoriasController.class,
  DonacionesController.class,
  DonacionesIndependientesController.class,
  DonantesController.class,
  EntidadBeneficiariaController.class,
  ItemDonacionNormalizadoController.class,
  NecesidadesController.class,
  PersonasController.class,
  PropuestaDeAsignacionController.class,
  SubcategoriasController.class
})
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("donaciones-webmvc-context")
public abstract class AbstractDonacionesWebMvcTest {

  @Autowired protected MockMvc mockMvc;

  @MockitoBean protected ICategoriasService categoriasService;

  @MockitoBean protected IDonacionesService donacionesService;

  @MockitoBean protected IDonacionesIndependientesService donacionesIndependientesService;

  @MockitoBean protected IDonantesService donantesService;

  @MockitoBean protected IArchivoDonantesService archivoDonantesService;

  @MockitoBean protected IEntidadBeneficiariaService entidadBeneficiariaService;

  @MockitoBean protected IItemDonacionNormalizadoService itemDonacionNormalizadoService;

  @MockitoBean protected INecesidadesService necesidadesService;

  @MockitoBean protected IPersonasService personasService;

  @MockitoBean protected IPropuestaDeAsignacionService propuestaDeAsignacionService;

  @MockitoBean protected ISubcategoriasService subcategoriasService;
}
