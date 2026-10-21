package py.una.mec.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import py.una.mec.dao.Conexion;

public class MenuMec implements Runnable {
    private final Scanner entrada = new Scanner(System.in);
    public void run() {
        boolean continuar=true;
        while(continuar) {
            System.out.println("\n=== ADMINISTRACIÓN MEC ===\n1. Registrar universidad\n2. Registrar carrera\n3. Registrar título");
            System.out.println("4. Cargar materia de malla\n5. Ver auditoría\n6. Estadísticas por universidad\n0. Salir del menú");
            String op=entrada.nextLine().trim();
            try {
                if("1".equals(op)) universidad();
                else if("2".equals(op)) carrera();
                else if("3".equals(op)) titulo();
                else if("4".equals(op)) materia();
                else if("5".equals(op)) auditoria();
                else if("6".equals(op)) estadisticas();
                else if("0".equals(op)) continuar=false;
                else System.out.println("Opción inválida.");
            } catch(Exception e) { System.out.println("Operación no realizada: "+e.getMessage()); }
        }
    }
    private String pedir(String s) { System.out.print(s+": "); return entrada.nextLine().trim().replace("|"," "); }
    private void universidad() throws Exception {
        try(Connection c=Conexion.abrir(); PreparedStatement p=c.prepareStatement(
            "INSERT INTO universidad(nombre) VALUES(?) ON CONFLICT(nombre) DO NOTHING")) {
            p.setString(1,pedir("Nombre")); p.executeUpdate(); System.out.println("Universidad guardada.");
        }
    }
    private void carrera() throws Exception {
        try(Connection c=Conexion.abrir(); PreparedStatement p=c.prepareStatement(
            "INSERT INTO carrera(nombre) VALUES(?) ON CONFLICT(nombre) DO NOTHING")) {
            p.setString(1,pedir("Nombre de carrera")); p.executeUpdate(); System.out.println("Carrera guardada.");
        }
    }
    private void titulo() throws Exception {
        try(Connection c=Conexion.abrir(); PreparedStatement p=c.prepareStatement(
            "INSERT INTO titulo(ci,carrera,universidad,numero) VALUES(?,?,?,?)")) {
            p.setString(1,pedir("CI")); p.setString(2,pedir("Carrera")); p.setString(3,pedir("Universidad"));
            p.setString(4,pedir("Número de título")); p.executeUpdate(); System.out.println("Título guardado.");
        }
    }
        private void materia() throws Exception {
            String carrera = pedir("Carrera"), nombre = pedir("Materia");
            int semestre = Integer.parseInt(pedir("Semestre"));
            String creditos = pedir("Créditos (puede ser 0)"), horas = pedir("Carga horaria (puede ser 0)");
            try (Connection c=Conexion.abrir();
                 PreparedStatement p=c.prepareStatement(
                     "INSERT INTO materia_malla(carrera_id,nombre,semestre,creditos,carga_horaria) "
                     + "SELECT id,?,?,?,? FROM carrera WHERE nombre=? "
                     + "ON CONFLICT(carrera_id,nombre,semestre) DO UPDATE SET creditos=EXCLUDED.creditos,carga_horaria=EXCLUDED.carga_horaria")) {
                p.setString(1,nombre); p.setInt(2,semestre); p.setBigDecimal(3,new java.math.BigDecimal(creditos));
                p.setInt(4,Integer.parseInt(horas)); p.setString(5,carrera);
                if (p.executeUpdate() == 0) throw new IllegalArgumentException("Carrera no encontrada");
                System.out.println("Materia guardada.");
            }
        }
        private void auditoria() throws Exception {
            try (Connection c=Conexion.abrir(); PreparedStatement p=c.prepareStatement(
                "SELECT fecha,origen,tipo_operacion,id_solicitud,resultado,universidad,detalle FROM auditoria ORDER BY fecha DESC LIMIT 200");
                ResultSet r=p.executeQuery()) {
                while(r.next()) System.out.println(r.getTimestamp(1)+" | "+r.getString(2)+" | "+r.getString(3)
                    +" | "+r.getString(4)+" | "+r.getString(5)+" | "+r.getString(6)+" | "+r.getString(7));
            }
        }
        private void estadisticas() throws Exception {
            try (Connection c=Conexion.abrir(); PreparedStatement p=c.prepareStatement(
                "SELECT COALESCE(universidad,'(sin universidad)'),tipo_operacion,resultado,COUNT(*) "
                + "FROM auditoria GROUP BY universidad,tipo_operacion,resultado ORDER BY universidad,tipo_operacion");
                ResultSet r=p.executeQuery()) {
                while(r.next()) System.out.println(r.getString(1)+" | "+r.getString(2)+" | "
                    +r.getString(3)+" | total="+r.getLong(4));
            }
        }
}
