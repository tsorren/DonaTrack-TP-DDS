package grupo5.cliente.vistas;

/**
 * Mi perfil del donante (docs/interfaz/flujos/raices-mi-perfil.md y r11-p0-persona.md): quién es,
 * cómo lo ven las entidades, los datos de la cuenta y la constancia de los términos. Un dato que la
 * cuenta no tiene es null y su campo queda vacío.
 *
 * @param cuenta el renglón de debajo del nombre: "Empresa donante · contacto@…", "Persona donante ·
 *     …" o, sin tipo, "Donante · …"
 * @param anonimo lo que ve la entidad si se oculta el nombre: "Empresa donante · Sostenedor" o
 *     "Donante Sostenedor"
 * @param oculto true si hoy las entidades no ven el nombre
 * @param cuit el CUIT de la empresa; null en la persona
 * @param legales la constancia: "Aceptaste la versión 1 el 1 de noviembre de 2026."
 * @param nombrePila y {@code apellido}: los de la persona; null en la empresa
 * @param documento el de la persona, con su tipo: "DNI 12.345.678"; null en la empresa
 */
public record DonantePerfilVista(
    String nombre,
    String cuenta,
    String anonimo,
    boolean oculto,
    String rubro,
    String cuit,
    String calle,
    String altura,
    String piso,
    String codigoPostal,
    String localidad,
    String provincia,
    String correo,
    String whatsapp,
    String telefono,
    String legales,
    String nombrePila,
    String apellido,
    String documento) {}
