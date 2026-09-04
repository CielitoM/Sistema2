package py.una.comun.protocolo;

public class MensajeOperacion {
    private String tipoOperacion;
    private String idSolicitud;
    private String datos;

    public MensajeOperacion() { }

    public MensajeOperacion(String tipoOperacion, String idSolicitud, String datos) {
        this.tipoOperacion = tipoOperacion;
        this.idSolicitud = idSolicitud;
        this.datos = datos;
    }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(String idSolicitud) { this.idSolicitud = idSolicitud; }
    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }
}
