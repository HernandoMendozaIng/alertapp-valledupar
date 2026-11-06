package co.edu.unad.alertapp.usuario;

import jakarta.validation.constraints.AssertTrue;
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

    public record UsuarioRespuesta(Long id, String nombre, String correo, String telefono, Rol rol, Short comuna) {
        public static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getRol(), u.getComuna());
        }
    }

    public record SesionRespuesta(String token, UsuarioRespuesta usuario) {}
}
