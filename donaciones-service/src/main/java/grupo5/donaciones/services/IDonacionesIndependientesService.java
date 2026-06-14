package grupo5.donaciones.services;

public interface IDonacionesIndependientesService {

  void asignar(Long donaIndepId, Long necesidadId, String actor);
  void vencer(Long donaIndepId, String actor);
  void planificarRuta(Long donaIndepId, String actor);
  void iniciarRecorrido(Long donaIndepId, String actor);
  void confirmarEntrega(Long donaIndepId, String actor);
  void registrarFalla(Long donaIndepId, String justificacion, String actor);
  void retornar(Long donaIndepId, String actor);
}
