# Contraseña provisoria cifrada en la sesión con AES-GCM y una clave compartida

- Status: proposed
- Date: 2026-11-13
- Deciders: el usuario (eligió la opción D, "cifrarla", entre cuatro, 2026-11-13); los detalles de la construcción, el agente. Pendiente de aprobación del equipo en el PR que lleve la rama de la etapa 2 de auth (`E6_auth_etapa2_merge`) a ENTREGA_6
- Tags: cliente-liviano, seguridad, sesion, spring-session-jdbc, entrega-6

## Contexto y Problema

`[OBSERVED]` Al dar de alta un administrador, o al generarle otra contraseña, el sitio muestra la
contraseña provisoria una sola vez: el POST la deja en un flash attribute y redirige a
`/admin-administradores-alta` (`AdminAdministradoresController`). Spring guarda los flash
attributes en la sesión.

`[DOCUMENTED]` Con SPEC-06 E2-02 (D5) la sesión se guarda en PostgreSQL con Spring Session JDBC, en
la base `cliente`, para que el sitio funcione con dos instancias (RNF-CL-04).

`[OBSERVED]` El record `Provisoria` no era `Serializable`: con la sesión en la base, el alta fallaba
al guardar la sesión. Se detectó al juntar la 969 (sesión JDBC) con la 981 (alta de
administradores).

Problema: ¿cómo llega la contraseña a la pantalla que la muestra, con la sesión en la base?

**Gate A:** es una decisión nueva sobre qué se guarda en la sesión y cómo. **Gate B:** tiene
implicancias de seguridad (una contraseña en la base) y suma un secreto a la configuración.

## Alternativas Consideradas

| Opción | Contraseña en la base | Con dos instancias | Costo |
|---|---|---|---|
| A. `Provisoria` serializable | En claro, milisegundos | Anda | 1 archivo |
| **D. Cifrarla con una clave compartida** (elegida) | Cifrada | Anda | 1 clase, la clave en `.env`, compose y generador |
| B. Mostrarla en la respuesta del POST, sin redirección | Nunca | Anda | Rompe POST-redirect-GET: recargar repite el alta |
| C. Guardarla en memoria con un código de un solo uso | Nunca | Falla si la redirección cae en la otra instancia | 2 archivos |

### Análisis por alternativa

- **A. Serializable en claro.** Pro: una línea, nada que configurar. Contra: la contraseña queda
  legible en la base, en sus registros borrados y en las copias, aunque sea por milisegundos.
- **D. Cifrarla.** Pro: nunca queda legible en la base; anda con dos instancias. Contra: un secreto
  más en `.env`, compose y generador; si la clave cambia en medio de un alta, esa contraseña se
  pierde y hay que generar otra.
- **B. Sin redirección.** Pro: la contraseña nunca pasa por la sesión. Contra: rompe
  POST-redirect-GET; recargar repite el alta o genera otra contraseña, y cambia un flujo aprobado.
- **C. Memoria local con un código.** Pro: nunca va a la base. Contra: si la redirección cae en la
  otra instancia, la contraseña no aparece (rompe RNF-CL-04).

## Decisión

`[OBSERVED]` Opción D. `CifradoDeLaSesion` cifra con AES-GCM del JDK, con un IV al azar por
mensaje: cifra y detecta si el texto se alteró. Lo que va al flash es un texto en Base64. Al
mostrarla, si no se puede descifrar (otra clave o texto alterado), la pantalla responde como si ya
se hubiera mostrado; en la maqueta, como sin flash, muestra la contraseña de ejemplo.

- **Clave:** `CLIENTE_SESIONES_CIFRADO_CLAVE`, 32 bytes en Base64 (`openssl rand -base64 32`), la
  misma en todas las instancias. La llevan `.env`, `.env.example`, `generar-env-db.sh` y los dos
  compose, que la exigen con `:?`.
- **Sin clave:** con la sesión en la base, el sitio no arranca. Con la sesión en memoria (la maqueta
  y los tests) usa una clave al azar por proceso.
- Sin dependencias nuevas: `javax.crypto` del JDK.

## Consecuencias

- Buena: la contraseña no queda legible en la base, en sus registros borrados ni en las copias.
- Buena: funciona con dos instancias que comparten la clave.
- Mala: hay un secreto más que administrar. Un `.env` viejo, como el de la demo, necesita la línea
  nueva o el compose no levanta.
- Mala: si la clave cambia entre el POST y la redirección, esa contraseña no se muestra y hay que
  generar otra.
- Neutra: la base `cliente` ya guarda los tokens de los usuarios conectados. El cifrado protege la
  contraseña provisoria, no la sesión entera.

## Validación

`[OBSERVED]` `CifradoDeLaSesionTest`, `AdministradoresConIngresoTest` (lo que va al flash es un
texto sin la contraseña), `SesionSerializableTest` y `test_sesiones_jdbc.py` (la clave en `.env`,
generador y compose; cada `addFlashAttribute` conocido).
