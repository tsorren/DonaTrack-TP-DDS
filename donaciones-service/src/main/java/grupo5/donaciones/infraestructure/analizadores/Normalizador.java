package grupo5.donaciones.infraestructure.analizadores;

// falta sacarle todos los simbolos, lo dejo pendiente
public interface Normalizador {
    public static String normalizar(String texto){
        return texto.trim().toLowerCase();
    }
}
