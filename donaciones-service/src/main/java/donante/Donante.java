package donante;

import ar.edu.utn.frba.ddsi.common.Persona;
import donacion.Donacion;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Donante {
    private Persona persona;
    private List<Donacion> historialDonaciones = new ArrayList<>();

    public void agregarMedioContacto(MedioDeContacto medioDeContacto) {
        contactos.add(medioDeContacto);
    }

    public void quitarMedioContacto(MedioDeContacto medioDeContacto) {
        contactos.remove(medioDeContacto);
    }
}