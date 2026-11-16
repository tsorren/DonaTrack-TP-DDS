package grupo5.donaciones.dto.personas;

import java.time.LocalDateTime;

/**
 * Constancia de los legales de una persona (HB-20). {@code versionAceptada} y {@code aceptadaEn}
 * son {@code null} si nunca aceptó; si {@code vigenteAceptada} es {@code false}, el cliente le pide
 * aceptar la versión vigente.
 */
public record LegalesDTO(
    Integer versionAceptada,
    LocalDateTime aceptadaEn,
    int versionVigente,
    boolean vigenteAceptada) {}
