package py.una.mec.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LogUtil {
    private LogUtil() { }
    public static Logger get(Class<?> tipo) {
        Logger l=Logger.getLogger(tipo.getName());
        if(l.getHandlers().length==0) {
            try {
                FileHandler h=new FileHandler("mec.log",true);
                h.setFormatter(new SimpleFormatter()); l.addHandler(h); l.setUseParentHandlers(false);
            } catch(IOException e) { l.log(Level.WARNING,"No se pudo abrir mec.log",e); }
        }
        return l;
    }
}
