INSERT INTO universidad(nombre) VALUES ('Universidad Nacional de Asuncion')
ON CONFLICT (nombre) DO NOTHING;
INSERT INTO carrera(nombre, universidad_id)
SELECT 'Ingenieria Informatica', id FROM universidad WHERE nombre='Universidad Nacional de Asuncion'
ON CONFLICT (nombre) DO NOTHING;
INSERT INTO estudiante_reportado(ci,nombres,apellidos,carrera,universidad,estado,promedio)
VALUES ('1000001','Ana','Gomez','Ingenieria Informatica','Universidad Nacional de Asuncion','egresado',88.50)
ON CONFLICT (ci) DO NOTHING;
INSERT INTO titulo(ci,carrera,universidad,numero,valido)
VALUES ('1000001','Ingenieria Informatica','Universidad Nacional de Asuncion','TIT-1000001',TRUE)
ON CONFLICT (numero) DO NOTHING;
