package grupo5.donaciones.infraestructure.analizadores;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ComparadorTexto {
    private String[] ListaDePalabras(String texto){
        return texto.split("\\s+");
    }


    private Set<String> textoASetUnico (String texto){
        String normalizado = Normalizador.normalizar(texto);
        String[] lista = ListaDePalabras(normalizado);
        return new HashSet<String>(Arrays.asList(lista));
    }

    public Integer contarPalabrasEnComun(String textoA, String textoB){
        if (textoA == null || textoB == null) {
            return 0;
        }

        Set<String> palabrasTextoA = textoASetUnico(textoA);
        Set<String> palabrasTextoB = textoASetUnico(textoB);

        int contador = 0;
        for (String palabraA : palabrasTextoA) {
            for(String palabraB : palabrasTextoB){
                // Si la palabra no está vacía y existe en el segundo conjunto, la contamos
                if (palabraA.equals(palabraB)) {
                    contador++;
                }
            }
        }

        return contador;
    }
}
