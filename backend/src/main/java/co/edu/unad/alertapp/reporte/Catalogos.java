package co.edu.unad.alertapp.reporte;

/**
 * Catálogos del formulario guiado de reporte (HU 05). Los valores salen del diseño
 * de pantallas aprobado en Justinmind: tipo de hecho, arma, medio del agresor y elemento hurtado.
 */
public final class Catalogos {

    private Catalogos() {
    }

    public enum TipoDelito { ATRACO, HURTO, SOSPECHOSO, OTRO }

    public enum Arma { ARMA_FUEGO, CORTOPUNZANTE, SIN_ARMA, NO_IDENTIFICADA }

    public enum Medio { A_PIE, BICICLETA, MOTOCICLETA, VEHICULO, NO_IDENTIFICADO }

    public enum Elemento { CELULAR, DINERO, JOYAS, PRENDA, BICICLETA, MOTOCICLETA, VEHICULO, PARTE_VEHICULO, NINGUNO, OTRO }

    public enum Severidad { BAJA, MEDIA, ALTA }

    /**
     * PENDIENTE: recién creado. VALIDADO: la comunidad lo confirmó.
     * EN_REVISION / CERRADO: lo gestiona la autoridad. DESCARTADO: anulado por votos falsos o por la autoridad.
     */
    public enum Estado { PENDIENTE, VALIDADO, EN_REVISION, CERRADO, DESCARTADO }
}
