# Sistema 2 - MEC

Proyecto correspondiente al sistema MEC para la materia Sistemas Distribuidos.

## Descripción

Este sistema representa al MEC y funciona como servidor para recibir solicitudes provenientes de FPUNA.

La comunicación se realiza mediante los protocolos TCP y UDP.

## Servicios implementados

### TCP - Recepción de datos académicos

El servidor TCP recibe reportes académicos enviados desde FPUNA.

- Protocolo: TCP
- Puerto: 4444

El servidor puede atender múltiples conexiones utilizando un handler independiente para cada cliente.

### UDP - Validación de título

El servidor UDP recibe solicitudes de validación de títulos enviadas por FPUNA.

- Protocolo: UDP
- Puerto: 9876

## Tecnologías utilizadas

- Java 8
- Maven
- Sockets TCP
- Datagramas UDP
- JSON

## Compilación

`mvn clean package`

## PostgreSQL y configuración

Crear la base y cargar el esquema y datos de prueba:

```powershell
createdb -U postgres mec_db
psql -U postgres -d mec_db -f src/main/resources/db/schema.sql
psql -U postgres -d mec_db -f src/main/resources/db/seed.sql
```

Copiar `config.properties.example` como `config.properties` y ajustar las
credenciales de PostgreSQL.

## Ejecución

El sistema MEC debe iniciarse antes que FPUNA.

`java -jar target/sistema2-mec-1.0.0-jar-with-dependencies.jar`

Una vez iniciado, el sistema queda escuchando solicitudes en:

- TCP: puerto 4444
- UDP: puerto 9876

También se inicia un menú administrativo para registrar universidades,
carreras y títulos oficiales. El menú funciona en paralelo con los servidores.

## Comunicación

Los mensajes intercambiados entre MEC y FPUNA utilizan formato JSON.

Los mensajes contienen información como:

- tipoOperacion
- idSolicitud
- datos

TCP usa una línea JSON por mensaje y despacha según `tipoOperacion`. Soporta
`RECEPCION_DATOS_ACADEMICOS`, `CONSULTA_EGRESADOS` y `CONSULTA_MALLA`.
UDP atiende `VALIDACION_TITULO`. Las respuestas devuelven `estado`, `mensaje`
y `datos`; los reportes se insertan o actualizan por CI sin duplicarse.

## Contratos JSON

Todas las solicitudes TCP son una línea JSON:

```json
{"tipoOperacion":"RECEPCION_DATOS_ACADEMICOS","idSolicitud":"TCP-1","datos":"1000001|Ana|Gomez|Ingenieria Informatica|UNA|egresado|88.5"}
```

La respuesta es:

```json
{"tipoOperacion":"RESPUESTA_RECEPCION_DATOS_ACADEMICOS","idSolicitud":"TCP-1","estado":"OK","mensaje":"Reporte académico guardado","datos":""}
```

Operaciones TCP adicionales:

```json
{"tipoOperacion":"CONSULTA_EGRESADOS","idSolicitud":"TCP-2","datos":"Ingenieria Informatica"}
{"tipoOperacion":"CONSULTA_MALLA","idSolicitud":"TCP-3","datos":"Ingenieria Informatica"}
{"tipoOperacion":"CERTIFICADO_NACIONAL","idSolicitud":"TCP-4","datos":"EMITIR|1000001|Ingenieria Informatica"}
{"tipoOperacion":"CERTIFICADO_NACIONAL","idSolicitud":"TCP-5","datos":"CONSULTAR_NUMERO|CERT-ABC"}
{"tipoOperacion":"CERTIFICADO_NACIONAL","idSolicitud":"TCP-6","datos":"CONSULTAR_CI|1000001"}
{"tipoOperacion":"AUDITORIA_CONSULTA","idSolicitud":"TCP-7","datos":""}
```

Cada una responde con `tipoOperacion`, `idSolicitud`, `estado`, `mensaje` y
`datos`. Una validación UDP usa:

```json
{"tipoOperacion":"VALIDACION_TITULO","idSolicitud":"UDP-1","datos":"1000001|Ingenieria Informatica"}
```

La respuesta UDP repite el `idSolicitud`; los reintentos son idempotentes y
no generan auditorías duplicadas.

## Guía de prueba manual

1. Crear `mec_db` y `fpuna_db`, ejecutar `schema.sql`, las migraciones y ambos
   `seed.sql`; copiar y editar cada `config.properties`.
2. Iniciar MEC y elegir `4` para cargar una materia de malla, por ejemplo
   `Ingenieria Informatica`, `Programacion I`, semestre `1`, créditos `5`,
   horas `80`.
3. Iniciar FPUNA. Elegir `3` con CI `1000001` y después `6` para comprobar
   historial y promedio.
4. Elegir `7` para enviar el reporte. MEC debe responder `OK`.
5. Elegir `9` con CI `1000001` y carrera `Ingenieria Informatica`; debe
   responder que el título es válido.
6. Elegir `8` con los mismos datos; se crea un `.txt` en `certificados/`.
7. En MEC elegir `5` para revisar auditoría. Repetir el mismo datagrama
   UDP con el mismo ID no debe añadir otra fila.
8. Elegir `10` en FPUNA para obtener la malla persistida.
9. Caso de error: detener MEC y usar `9`; FPUNA debe informar timeout después
   de los reintentos. Usar una CI inexistente en FPUNA debe informar que no
   existe sin cerrar el menú.

## Materia

Sistemas Distribuidos