package grupo5.donaciones.services;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private final IDonacionesIndependientesRepository repositorio;

  public DonacionesIndependientesService(IDonacionesIndependientesRepository repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public void asignar(Long donaIndepId, Long necesidadId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.asignar(actor);
    repositorio.save(donacion);
  }

  @Override
  public void vencer(Long donaIndepId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.vencer(actor);
    repositorio.save(donacion);
  }

  @Override
  public void planificarRuta(Long donaIndepId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.planificarRuta(actor);
    repositorio.save(donacion);
  }

  @Override
  public void iniciarRecorrido(Long donaIndepId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.iniciarRecorrido(actor);
    repositorio.save(donacion);
  }

  @Override
  public void confirmarEntrega(Long donaIndepId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.confirmarEntrega(actor);
    repositorio.save(donacion);
  }

  @Override
  public void registrarFalla(Long donaIndepId, String justificacion, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.registrarFalla(justificacion, actor);
    repositorio.save(donacion);
  }

  @Override
  public void retornar(Long donaIndepId, String actor) {
    DonacionIndependiente donacion = buscarOFallar(donaIndepId);
    donacion.retornar(actor);
    repositorio.save(donacion);
  }

  private DonacionIndependiente buscarOFallar(Long id) {
    return repositorio.findById(id)
        .orElseThrow(() -> new IllegalArgumentException(
            "No existe DonacionIndependiente con id: " + id));
  }
}
