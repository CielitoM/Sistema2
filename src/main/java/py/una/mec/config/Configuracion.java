package py.una.mec.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Configuracion {
    private static final Properties P = cargar();
    private Configuracion() { }
    private static Properties cargar() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            p.load(in);
        } catch (IOException ignorado) { }
        return p;
    }
    public static String get(String clave, String defecto) {
        String valor = System.getenv(clave.toUpperCase().replace('.', '_'));
        return valor != null ? valor : P.getProperty(clave, defecto);
    }
    public static int entero(String clave, int defecto) {
        try { return Integer.parseInt(get(clave, String.valueOf(defecto))); }
        catch (NumberFormatException e) { return defecto; }
    }
}
