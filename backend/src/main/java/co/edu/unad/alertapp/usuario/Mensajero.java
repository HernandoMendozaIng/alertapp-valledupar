package co.edu.unad.alertapp.usuario;

/** Canal para enviar mensajes al usuario (correo o SMS). */
public interface Mensajero {

    void enviar(String destino, String asunto, String texto);
}
