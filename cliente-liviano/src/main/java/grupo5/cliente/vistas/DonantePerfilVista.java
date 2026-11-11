package grupo5.cliente.vistas;

/**
 * Mi perfil del donante (docs/interfaz/flujos/raices-mi-perfil.md): quién es, cómo lo ven las
 * entidades, los datos de la cuenta y la constancia de los términos. Un dato que la cuenta no tiene
 * es null y su campo queda vacío.
 *
 * @param cuenta el renglón de debajo del nombre: "Empresa donante · contacto@…"
 * @param anonimo lo que ve la entidad si se oculta el nombre: "Empresa donante · Sostenedor"
 * @param oculto true si hoy las entidades no ven el nombre
 * @param legales la constancia: "Aceptaste la versión 1 el 1 de noviembre de 2026."
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
    String legales) {}
