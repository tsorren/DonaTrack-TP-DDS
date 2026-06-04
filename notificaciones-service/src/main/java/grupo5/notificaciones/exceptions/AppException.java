package grupo5.notificaciones.exceptions;

public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }
}
/* Ejemplo de una excepción personalizada que extiende AppException:
* public class PersonaNoEncontradaException extends AppException {

    public PersonaNoEncontradaException(Long id) {
        super("No existe la persona con id " + id);
    }
}
*
* Y para llamarlo:
*
* public Persona buscarPersona(Long id) {

    return personaRepository.findById(id)
            .orElseThrow(() ->
                    new PersonaNoEncontradaException(id));
}
*
* y asi se van generando más clases que extienden de AppException
*
* */