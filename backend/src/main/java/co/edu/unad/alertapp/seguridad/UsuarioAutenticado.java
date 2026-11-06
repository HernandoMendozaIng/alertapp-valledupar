package co.edu.unad.alertapp.seguridad;

import co.edu.unad.alertapp.usuario.Rol;

/**
 * Datos mínimos del usuario que viajan en el token y quedan en el contexto de seguridad.
 */
public record UsuarioAutenticado(Long id, String nombre, Rol rol) {

    public boolean esAutoridad() {
        return rol == Rol.AUTORIDAD;
    }
}
