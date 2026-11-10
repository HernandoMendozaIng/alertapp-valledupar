package co.edu.unad.alertapp.usuario;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.seguridad.JwtService;
import co.edu.unad.alertapp.seguridad.LimiteIntentos;
import co.edu.unad.alertapp.seguridad.Totp;
import co.edu.unad.alertapp.usuario.AuthDtos.CambioClave;
import co.edu.unad.alertapp.usuario.AuthDtos.CambioCorreo;
import co.edu.unad.alertapp.usuario.AuthDtos.ConfiguracionDosFactores;
import co.edu.unad.alertapp.usuario.AuthDtos.IngresoPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.PerfilActualizacion;
import co.edu.unad.alertapp.usuario.AuthDtos.Preferencias;
import co.edu.unad.alertapp.usuario.AuthDtos.RegistroPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.RestablecerPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.SesionRespuesta;
import co.edu.unad.alertapp.usuario.AuthDtos.UsuarioRespuesta;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU 01 (acceso y autenticación) y HU 10 (configuración y seguridad de la cuenta).
 */
@Service
public class AuthServicio {

    private static final String EMISOR_2FA = "AlertApp Valledupar";
    private static final String CLAVE_INCORRECTA = "La contraseña no es correcta";
    private static final String CODIGO_INCORRECTO = "El código no es correcto o ya venció";

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;
    private final LimiteIntentos limite;
    private final CodigosRecuperacion recuperacion;
    private final Mensajero mensajero;

    public AuthServicio(UsuarioRepositorio usuarios, PasswordEncoder codificador, JwtService jwt, LimiteIntentos limite,
                        CodigosRecuperacion recuperacion, Mensajero mensajero) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.limite = limite;
        this.recuperacion = recuperacion;
        this.mensajero = mensajero;
    }

    @Transactional
    public SesionRespuesta registrar(RegistroPeticion p) {
        String correo = p.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        String telefono = p.telefono() == null || p.telefono().isBlank() ? null : p.telefono();
        Usuario u = usuarios.save(new Usuario(p.nombre().trim(), correo, codificador.encode(p.clave()),
                telefono, Rol.CIUDADANO, p.comuna()));
        return SesionRespuesta.completa(jwt.generar(u), UsuarioRespuesta.de(u));
    }

    @Transactional(readOnly = true)
    public SesionRespuesta ingresar(IngresoPeticion p) {
        exigirNoBloqueado(p.correo());
        Usuario u = usuarios.findByCorreoIgnoreCase(p.correo().trim())
                .filter(x -> codificador.matches(p.clave(), x.getClaveHash()))
                .orElse(null);
        if (u == null) {
            limite.fallo(p.correo());
            throw new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }
        if (u.isDosFactores()) {
            // La contraseña fue correcta, pero el contador de intentos no se limpia hasta pasar el segundo factor.
            return SesionRespuesta.pendiente(jwt.generarDesafio(u));
        }
        limite.exito(p.correo());
        return SesionRespuesta.completa(jwt.generar(u), UsuarioRespuesta.de(u));
    }

    /** Segundo paso del ingreso con 2FA: canjea el desafío y el código de la app autenticadora por la sesión. */
    @Transactional(readOnly = true)
    public SesionRespuesta segundoFactor(String desafio, String codigo) {
        Long id = jwt.validarDesafio(desafio)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "El ingreso venció. Vuelve a escribir tu contraseña."));
        Usuario u = buscar(id);
        exigirNoBloqueado(u.getCorreo());
        if (!u.isDosFactores() || !Totp.verificar(u.getTotpSecreto(), codigo)) {
            limite.fallo(u.getCorreo());
            throw new ErrorNegocio(HttpStatus.UNAUTHORIZED, CODIGO_INCORRECTO);
        }
        limite.exito(u.getCorreo());
        return SesionRespuesta.completa(jwt.generar(u), UsuarioRespuesta.de(u));
    }

    @Transactional(readOnly = true)
    public UsuarioRespuesta perfil(Long id) {
        return UsuarioRespuesta.de(buscar(id));
    }

    @Transactional
    public UsuarioRespuesta actualizar(Long id, PerfilActualizacion p) {
        Usuario u = buscar(id);
        u.setNombre(p.nombre().trim());
        u.setTelefono(p.telefono() == null || p.telefono().isBlank() ? null : p.telefono());
        u.setComuna(p.comuna());
        return UsuarioRespuesta.de(u);
    }

    @Transactional
    public UsuarioRespuesta preferencias(Long id, Preferencias p) {
        Usuario u = buscar(id);
        u.setNotificaciones(p.notificaciones());
        u.setRadioAlertas(p.radioAlertas());
        return UsuarioRespuesta.de(u);
    }

    @Transactional
    public void ubicar(Long id, double lat, double lng) {
        buscar(id).ubicar(lat, lng);
    }

    @Transactional
    public void cambiarClave(Long id, CambioClave p) {
        Usuario u = buscar(id);
        exigirClave(u, p.actual());
        if (codificador.matches(p.nueva(), u.getClaveHash())) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "La contraseña nueva debe ser distinta de la actual");
        }
        u.setClaveHash(codificador.encode(p.nueva()));
        mensajero.enviar(u.getCorreo(), "Tu contraseña cambió",
                "La contraseña de tu cuenta de AlertApp se cambió. Si no fuiste tú, recupérala de inmediato.");
    }

    @Transactional
    public UsuarioRespuesta cambiarCorreo(Long id, CambioCorreo p) {
        Usuario u = buscar(id);
        exigirClave(u, p.clave());
        String nuevo = p.correo().trim().toLowerCase();
        if (nuevo.equals(u.getCorreo())) return UsuarioRespuesta.de(u);
        if (usuarios.existsByCorreoIgnoreCase(nuevo)) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        String anterior = u.getCorreo();
        u.setCorreo(nuevo);
        mensajero.enviar(anterior, "Tu correo cambió", "El correo de tu cuenta de AlertApp ahora es " + nuevo + ".");
        return UsuarioRespuesta.de(u);
    }

    /** Paso 1 de la activación de 2FA: genera el secreto que el usuario escanea con su app autenticadora. */
    @Transactional
    public ConfiguracionDosFactores prepararDosFactores(Long id) {
        Usuario u = buscar(id);
        if (u.isDosFactores()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "La verificación en dos pasos ya está activa");
        }
        String secreto = Totp.nuevoSecreto();
        u.setTotpSecreto(secreto);
        return new ConfiguracionDosFactores(secreto, Totp.uri(EMISOR_2FA, u.getCorreo(), secreto));
    }

    /** Paso 2: solo se activa si el usuario demuestra que su app ya genera códigos válidos. */
    @Transactional
    public UsuarioRespuesta activarDosFactores(Long id, String codigo) {
        Usuario u = buscar(id);
        if (u.isDosFactores()) return UsuarioRespuesta.de(u);
        if (u.getTotpSecreto() == null) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Primero genera el código QR");
        }
        if (!Totp.verificar(u.getTotpSecreto(), codigo)) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, CODIGO_INCORRECTO);
        }
        u.setDosFactores(true);
        mensajero.enviar(u.getCorreo(), "Verificación en dos pasos activada",
                "Desde ahora AlertApp te pedirá un código de tu app autenticadora al ingresar.");
        return UsuarioRespuesta.de(u);
    }

    @Transactional
    public UsuarioRespuesta desactivarDosFactores(Long id, String clave, String codigo) {
        Usuario u = buscar(id);
        exigirClave(u, clave);
        if (u.isDosFactores() && !Totp.verificar(u.getTotpSecreto(), codigo)) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, CODIGO_INCORRECTO);
        }
        u.setDosFactores(false);
        u.setTotpSecreto(null);
        return UsuarioRespuesta.de(u);
    }

    /**
     * Envía un código de recuperación. Responde igual exista o no la cuenta, para que nadie pueda usar
     * este formulario para averiguar qué correos están registrados.
     */
    public void solicitarRecuperacion(String correo) {
        exigirNoBloqueado(correo);
        usuarios.findByCorreoIgnoreCase(correo.trim()).ifPresent(u -> mensajero.enviar(u.getCorreo(),
                "Código para recuperar tu contraseña",
                "Tu código es " + recuperacion.generar(u.getCorreo()) + ". Vence en 15 minutos. Si no lo pediste, ignora este mensaje."));
    }

    @Transactional
    public void restablecer(RestablecerPeticion p) {
        exigirNoBloqueado(p.correo());
        Usuario u = usuarios.findByCorreoIgnoreCase(p.correo().trim()).orElse(null);
        if (u == null || !recuperacion.canjear(u.getCorreo(), p.codigo())) {
            limite.fallo(p.correo());
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, CODIGO_INCORRECTO);
        }
        u.setClaveHash(codificador.encode(p.nueva()));
        limite.exito(p.correo());
    }

    private void exigirClave(Usuario u, String clave) {
        if (!codificador.matches(clave, u.getClaveHash())) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, CLAVE_INCORRECTA);
        }
    }

    private void exigirNoBloqueado(String correo) {
        if (limite.bloqueado(correo)) {
            throw new ErrorNegocio(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera 15 minutos e intenta de nuevo.");
        }
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
