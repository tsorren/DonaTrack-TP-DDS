package ar.edu.utn.frba.ddsi.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SaludadorTest {
    @Test
    void saludarAlProfe() {
        Saludador saludador = new Saludador();

        Persona profe = new Persona("Profe", "ddsi");
        String resultado = saludador.saludar(profe.nombreCompleto());

        Assertions.assertEquals("👋 Mucho gusto: Profe ddsi", resultado);
    }
}
