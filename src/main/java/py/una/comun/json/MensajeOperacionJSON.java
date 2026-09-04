package py.una.comun.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import py.una.comun.protocolo.MensajeOperacion;

public final class MensajeOperacionJSON {
    private MensajeOperacionJSON() { }

    public static String serializar(MensajeOperacion mensaje) {
        JSONObject objeto = new JSONObject();
        objeto.put("tipoOperacion", mensaje.getTipoOperacion());
        objeto.put("idSolicitud", mensaje.getIdSolicitud());
        objeto.put("datos", mensaje.getDatos());
        return objeto.toJSONString();
    }

    public static MensajeOperacion deserializar(String texto) throws Exception {
        JSONObject objeto = (JSONObject) new JSONParser().parse(texto.trim());
        return new MensajeOperacion(
            (String) objeto.get("tipoOperacion"),
            (String) objeto.get("idSolicitud"),
            (String) objeto.get("datos"));
    }
}
