package py.una.comun.protocolo;

public class MensajeOperacion {
    private String tipoOperacion;
    private String idSolicitud;
    private String datos;
    private String estado;
    private String mensaje;

    public MensajeOperacion() { }

    public MensajeOperacion(String tipoOperacion, String idSolicitud, String datos) {
        this.tipoOperacion = tipoOperacion;
        this.idSolicitud = idSolicitud;
        this.datos = datos;
    }

    public MensajeOperacion(String tipoOperacion, String idSolicitud, String estado, String mensaje, String datos) {
        this(tipoOperacion, idSolicitud, datos);
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(String idSolicitud) { this.idSolicitud = idSolicitud; }
    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
