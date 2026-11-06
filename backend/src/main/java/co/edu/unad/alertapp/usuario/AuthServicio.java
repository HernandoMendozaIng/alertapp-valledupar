package co.edu.unad.alertapp.usuario;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.seguridad.JwtService;
import co.edu.unad.alertapp.seguridad.LimiteIntentos;
import co.edu.unad.alertapp.usuario.AuthDtos.IngresoPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.PerfilActualizacion;
import co.edu.unad.alertapp.usuario.AuthDtos.RegistroPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.SesionRespuesta;
import co.edu.unad.alertapp.usuario.AuthDtos.UsuarioRespuesta;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServicio {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;
    private final LimiteIntentos limite;

    public AuthServicio(UsuarioRepositorio usuarios, PasswordEncoder codificador, JwtService jwt, LimiteIntentos limite) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.limite = limite;
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
        return new SesionRespuesta(jwt.generar(u), UsuarioRespuesta.de(u));
    }

    @Transactional(readOnly = true)
    public SesionRespuesta ingresar(IngresoPeticion p) {
        if (limite.bloqueado(p.correo())) {
            throw new ErrorNegocio(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera 15 minutos e intenta de nuevo.");
        }
        Usuario u = usuarios.findByCorreoIgnoreCase(p.correo().trim())
                .filter(x -> codificador.matches(p.clave(), x.getClaveHash()))
                .orElse(null);
        if (u == null) {
            limite.fallo(p.correo());
            throw new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }
        limite.exito(p.correo());
        return new SesionRespuesta(jwt.generar(u), UsuarioRespuesta.de(u));
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

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
