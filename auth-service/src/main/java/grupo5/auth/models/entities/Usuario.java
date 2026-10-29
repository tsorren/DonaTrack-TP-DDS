package grupo5.auth.models.entities;

import grupo5.auth.models.ports.PasswordHasherPort;
import lombok.Getter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta del IdP. Una cuenta = un email = un {@code sub} (D30). Dominio puro: sin JPA, sin DTO, sin
 * reloj propio (el instante siempre entra por parámetro para tests deterministas).
 */
@Getter
public final class Usuario {

  private final UUID id;
  private final Email email;
  private String passwordHash;
  private EstadoCuenta estado;
  private final Set<Rol> roles;
  private boolean emailVerificado;
  private Rol rolSolicitado;
  private int intentosFallidos;
  private Instant primerIntentoFallidoEn;
  private Instant bloqueadoHasta;
  private final Set<IdentidadSocial> identidadesSociales;
  private final Instant creadoEn;
  private Instant actualizadoEn;

  @SuppressWarnings("java:S107")
  private Usuario(
      UUID id,
      Email email,
      String passwordHash,
      EstadoCuenta estado,
      Set<Rol> roles,
      boolean emailVerificado,
      Rol rolSolicitado,
      int intentosFallidos,
      Instant primerIntentoFallidoEn,
      Instant bloqueadoHasta,
      Set<IdentidadSocial> identidadesSociales,
      Instant creadoEn,
      Instant actualizadoEn) {
    this.id = Objects.requireNonNull(id, "id");
    this.email = Objects.requireNonNull(email, "email");
    this.passwordHash = passwordHash;
    this.estado = Objects.requireNonNull(estado, "estado");
    this.roles = roles.isEmpty() ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(roles);
    if (this.roles.contains(Rol.SERVICIO)) {
      throw new DominioAuthException("SERVICIO no es asignable a una persona");
    }
    this.emailVerificado = emailVerificado;
    this.rolSolicitado = rolSolicitado;
    this.intentosFallidos = intentosFallidos;
    this.primerIntentoFallidoEn = primerIntentoFallidoEn;
    this.bloqueadoHasta = bloqueadoHasta;
    this.identidadesSociales = new HashSet<>(identidadesSociales);
    this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
    this.actualizadoEn = Objects.requireNonNull(actualizadoEn, "actualizadoEn");
  }

  // ---------------------------------------------------------------- fábricas

  /** RF-AUTH-05. Fase 1: queda ACTIVA con email sin verificar (la verificación es Fase 2). */
  public static Usuario registrarDonante(Email email, String passwordHash, Instant ahora) {
    return nuevo(
        email,
        requerirHash(passwordHash),
        EstadoCuenta.ACTIVA,
        EnumSet.of(Rol.DONANTE),
        false,
        null,
        ahora);
  }

  /** RF-AUTH-06. Sin rol ENTIDAD hasta que un admin la apruebe. */
  public static Usuario registrarEntidad(Email email, String passwordHash, Instant ahora) {
    return nuevo(
        email,
        requerirHash(passwordHash),
        EstadoCuenta.PENDIENTE_APROBACION,
        EnumSet.noneOf(Rol.class),
        false,
        Rol.ENTIDAD,
        ahora);
  }

  /** RF-AUTH-07 (a): primer admin, proveniente del seed por variables de entorno. */
  public static Usuario crearAdminInicial(Email email, String passwordHash, Instant ahora) {
    return nuevo(
        email,
        requerirHash(passwordHash),
        EstadoCuenta.ACTIVA,
        EnumSet.of(Rol.ADMIN),
        true,
        null,
        ahora);
  }

  /** RF-AUTH-07 (b): un admin solo lo crea otro admin activo. */
  public Usuario crearAdmin(Email nuevoEmail, String nuevoPasswordHash, Instant ahora) {
    if (estado != EstadoCuenta.ACTIVA || !roles.contains(Rol.ADMIN)) {
      throw new DominioAuthException("Solo un ADMIN activo puede crear otro ADMIN");
    }
    return crearAdminInicial(nuevoEmail, nuevoPasswordHash, ahora);
  }

  /** RF-AUTH-02: alta por Google. Rechaza si el proveedor no verificó el email. */
  public static Usuario registrarDesdeProveedorSocial(
      Email email,
      IdentidadSocial identidad,
      boolean emailVerificadoPorProveedor,
      boolean comoEntidad,
      Instant ahora) {
    if (!emailVerificadoPorProveedor) {
      throw new DominioAuthException("El proveedor no verificó el email");
    }
    Usuario u =
        comoEntidad
            ? nuevo(
                email,
                null,
                EstadoCuenta.PENDIENTE_APROBACION,
                EnumSet.noneOf(Rol.class),
                true,
                Rol.ENTIDAD,
                ahora)
            : nuevo(
                email, null, EstadoCuenta.ACTIVA, EnumSet.of(Rol.DONANTE), true, null, ahora);
    u.identidadesSociales.add(Objects.requireNonNull(identidad, "identidad"));
    return u;
  }

  /** RF-AUTH-14/15 (Fase 2): cuenta importada, sin contraseña, a la espera de activación. */
  public static Usuario crearPendienteActivacion(Email email, Instant ahora) {
    return nuevo(
        email,
        null,
        EstadoCuenta.PENDIENTE_ACTIVACION,
        EnumSet.of(Rol.DONANTE),
        false,
        null,
        ahora);
  }

  /** Rehidratación desde persistencia (usada por el mapper; no valida transiciones). */
  @SuppressWarnings("java:S107")
  public static Usuario rehidratar(
      UUID id,
      Email email,
      String passwordHash,
      EstadoCuenta estado,
      Set<Rol> roles,
      boolean emailVerificado,
      Rol rolSolicitado,
      int intentosFallidos,
      Instant primerIntentoFallidoEn,
      Instant bloqueadoHasta,
      Set<IdentidadSocial> identidadesSociales,
      Instant creadoEn,
      Instant actualizadoEn) {
    return new Usuario(
        id,
        email,
        passwordHash,
        estado,
        roles,
        emailVerificado,
        rolSolicitado,
        intentosFallidos,
        primerIntentoFallidoEn,
        bloqueadoHasta,
        identidadesSociales,
        creadoEn,
        actualizadoEn);
  }

  private static Usuario nuevo(
      Email email,
      String hash,
      EstadoCuenta estado,
      Set<Rol> roles,
      boolean emailVerificado,
      Rol rolSolicitado,
      Instant ahora) {
    Objects.requireNonNull(ahora, "ahora");
    return new Usuario(
        UUID.randomUUID(),
        email,
        hash,
        estado,
        roles,
        emailVerificado,
        rolSolicitado,
        0,
        null,
        null,
        Set.of(),
        ahora,
        ahora);
  }

  private static String requerirHash(String hash) {
    if (hash == null || hash.isBlank()) {
      throw new DominioAuthException("Falta el hash de contraseña");
    }
    return hash;
  }

  // ------------------------------------------------------------- autenticación

  public boolean estaBloqueada(Instant ahora) {
    return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
  }

  /**
   * RF-AUTH-01 / RNF-AUTH-07. La cuenta DESHABILITADA, las que aún no se activaron/verificaron, las
   * bloqueadas y las sin contraseña no pueden iniciar sesión. El llamador debe responder igual en
   * todos los casos (RNF-AUTH-10).
   */
  public boolean puedeAutenticarse(Instant ahora) {
    boolean estadoPermitido =
        estado == EstadoCuenta.ACTIVA || estado == EstadoCuenta.PENDIENTE_APROBACION;
    return estadoPermitido && passwordHash != null && !estaBloqueada(ahora);
  }

  /** Cuenta un fallo; bloquea al alcanzar el máximo dentro de la ventana. */
  public void registrarIntentoFallido(Instant ahora, PoliticaBloqueo politica) {
    if (estaBloqueada(ahora)) {
      return; // no se extiende el bloqueo (evita DoS por reintentos)
    }
    if (bloqueadoHasta != null) {
      bloqueadoHasta = null;
      intentosFallidos = 0;
      primerIntentoFallidoEn = null;
    }
    if (primerIntentoFallidoEn == null
        || ahora.isAfter(primerIntentoFallidoEn.plus(politica.ventana()))) {
      intentosFallidos = 0;
      primerIntentoFallidoEn = ahora;
    }
    intentosFallidos++;
    if (intentosFallidos >= politica.maxIntentos()) {
      bloqueadoHasta = ahora.plus(politica.duracionBloqueo());
    }
    actualizadoEn = ahora;
  }

  /** El contador se resetea con un login exitoso. */
  public void registrarLoginExitoso(Instant ahora) {
    intentosFallidos = 0;
    primerIntentoFallidoEn = null;
    bloqueadoHasta = null;
    actualizadoEn = ahora;
  }

  // ------------------------------------------------------------- aprobaciones

  /** RI-AUTH-05 (lado IdP). Idempotente. ENTIDAD solo se asigna por aprobación. */
  public void aprobarEntidad(Instant ahora) {
    if (estado == EstadoCuenta.ACTIVA && roles.contains(Rol.ENTIDAD)) {
      return;
    }
    if (estado != EstadoCuenta.PENDIENTE_APROBACION || rolSolicitado != Rol.ENTIDAD) {
      throw new DominioAuthException("La cuenta no tiene una solicitud de entidad pendiente");
    }
    roles.add(Rol.ENTIDAD);
    estado = EstadoCuenta.ACTIVA;
    rolSolicitado = null;
    actualizadoEn = ahora;
  }

  /** Rechazar pasa la cuenta a DESHABILITADA. Idempotente. */
  public void rechazarEntidad(Instant ahora) {
    if (estado == EstadoCuenta.DESHABILITADA) {
      return;
    }
    if (estado != EstadoCuenta.PENDIENTE_APROBACION) {
      throw new DominioAuthException("La cuenta no tiene una solicitud de entidad pendiente");
    }
    estado = EstadoCuenta.DESHABILITADA;
    rolSolicitado = null;
    actualizadoEn = ahora;
  }

  // ------------------------------------------------------------- contraseña

  /** RF-AUTH-12: exige la actual, aplica la política y no permite repetir la misma. */
  public void cambiarPassword(
      String actual, String nueva, PasswordHasherPort hasher, Instant ahora) {
    if (passwordHash == null || actual == null || !hasher.coincide(actual, passwordHash)) {
      throw new DominioAuthException("Contraseña actual incorrecta");
    }
    PoliticaContrasenia.validar(nueva, email);
    if (nueva.equals(actual)) {
      throw new DominioAuthException("La nueva contraseña debe ser distinta de la actual");
    }
    passwordHash = hasher.codificar(nueva);
    actualizadoEn = ahora;
  }

  // ------------------------------------------------------------- social

  public void vincularIdentidadSocial(IdentidadSocial identidad, Instant ahora) {
    if (identidadesSociales.add(Objects.requireNonNull(identidad, "identidad"))) {
      actualizadoEn = ahora;
    }
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof Usuario otro && id.equals(otro.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Usuario[id=" + id + ", estado=" + estado + "]";
  }
}
