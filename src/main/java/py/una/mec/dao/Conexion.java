package py.una.mec.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import py.una.mec.config.Configuracion;

public final class Conexion {
    private Conexion() { }
    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(
            Configuracion.get("db.url", "jdbc:postgresql://localhost:5432/mec_db"),
            Configuracion.get("db.user", "postgres"),
            Configuracion.get("db.password", "postgres"));
    }
}
