package co.edu.unad.alertapp.usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegistroPeticion(
            @NotBlank(message = "Escribe tu nombre") @Size(max = 80) String nombre,
            @NotBlank(message = "Escribe tu correo") @Email(message = "El correo no es válido") @Size(max = 120) String correo,
            @NotBlank(message = "Crea una contraseña")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe combinar letras y números")
            String clave,
            @Pattern(regexp = "^$|^3\\d{9}$", message = "El celular debe tener 10 dígitos y empezar por 3") String telefono,
            @Min(value = 1, message = "Comuna no válida") @Max(value = 6, message = "Comuna no válida") Short comuna,
            @AssertTrue(message = "Debes aceptar la política de tratamiento de datos") boolean aceptoPolitica
    ) {}

    public record IngresoPeticion(
            @NotBlank(message = "Escribe tu correo") String correo,
            @NotBlank(message = "Escribe tu contraseña") String clave
    ) {}

    public record PerfilActualizacion(
            @NotBlank(message = "Escribe tu nombre") @Size(max = 80) String nombre,
            @Pattern(regexp = "^$|^3\\d{9}$", message = "El celular debe tener 10 dígitos y empezar por 3") String telefono,
            @Min(1) @Max(6) Short comuna
    ) {}

    public record UsuarioRespuesta(Long id, String nombre, String correo, String telefono, Rol rol, Short comuna,
                                   boolean notificaciones, int radioAlertas, boolean dosFactores) {
        public static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getRol(), u.getComuna(),
                    u.isNotificaciones(), u.getRadioAlertas(), u.isDosFactores());
        }
    }

    /**
     * Respuesta del ingreso. Si la cuenta tiene 2FA, no trae token de sesión sino un desafío corto que se
     * canjea en /api/auth/ingreso/2fa junto con el código de la app autenticadora.
     */
    public record SesionRespuesta(String token, UsuarioRespuesta usuario, boolean requiereSegundoFactor, String desafio) {
        public static SesionRespuesta completa(String token, UsuarioRespuesta usuario) {
            return new SesionRespuesta(token, usuario, false, null);
        }

        public static SesionRespuesta pendiente(String desafio) {
            return new SesionRespuesta(null, null, true, desafio);
        }
    }

    public record SegundoFactorPeticion(
            @NotBlank(message = "Falta el desafío de ingreso") String desafio,
            @NotBlank(message = "Escribe el código de 6 dígitos") @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo
    ) {}

    public record CodigoPeticion(
            @NotBlank(message = "Escribe el código de 6 dígitos") @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo
    ) {}

    public record DesactivarDosFactores(
            @NotBlank(message = "Escribe tu contraseña") String clave,
            @NotBlank(message = "Escribe el código de 6 dígitos") @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo
    ) {}

    public record ConfiguracionDosFactores(String secreto, String uri) {}

    public record CambioClave(
            @NotBlank(message = "Escribe tu contraseña actual") String actual,
            @NotBlank(message = "Crea una contraseña nueva")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe combinar letras y números")
            String nueva
    ) {}

    public record CambioCorreo(
            @NotBlank(message = "Escribe tu contraseña") String clave,
            @NotBlank(message = "Escribe el correo nuevo") @Email(message = "El correo no es válido") @Size(max = 120) String correo
    ) {}

    public record RecuperarPeticion(@NotBlank(message = "Escribe tu correo") @Email(message = "El correo no es válido") String correo) {}

    public record RestablecerPeticion(
            @NotBlank(message = "Escribe tu correo") String correo,
            @NotBlank(message = "Escribe el código que te llegó") @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo,
            @NotBlank(message = "Crea una contraseña nueva")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe combinar letras y números")
            String nueva
    ) {}

    /** HU 02: activar o desactivar las alertas y elegir el radio. */
    public record Preferencias(
            boolean notificaciones,
            @Min(value = 200, message = "El radio mínimo es 200 m") @Max(value = 2000, message = "El radio máximo es 2 km") int radioAlertas
    ) {}

    public record UbicacionPeticion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud
    ) {}
}
