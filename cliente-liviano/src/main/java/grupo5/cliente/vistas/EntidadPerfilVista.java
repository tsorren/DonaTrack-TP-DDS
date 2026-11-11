package grupo5.cliente.vistas;

/**
 * Mi perfil de la entidad. {@code cuentaLinea} es el renglón de debajo del nombre ("Comedor ·
 * Marta, comedor@ejemplo.org"); {@code queEs}, el rubro; {@code tipo}, la opción de Tipo de
 * organización ({@code asociacion}, {@code institucion} u {@code organismo}), o null si no es
 * ninguna; {@code medio}, por dónde se avisa ({@code correo}, {@code whatsapp} o {@code telefono});
 * {@code legales}, la constancia en palabras. Lo que no se sabe, null.
 */
public record EntidadPerfilVista(
    String nombre,
    String cuentaLinea,
    String queEs,
    String tipo,
    String direccion,
    String representante,
    String correoRepresentante,
    String correo,
    String whatsapp,
    String telefono,
    String medio,
    String legales) {}
