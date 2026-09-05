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

## Ejecución

El sistema MEC debe iniciarse antes que FPUNA.

`java -jar target/sistema2-mec-1.0.0-jar-with-dependencies.jar`

Una vez iniciado, el sistema queda escuchando solicitudes en:

- TCP: puerto 4444
- UDP: puerto 9876

## Comunicación

Los mensajes intercambiados entre MEC y FPUNA utilizan formato JSON.

Los mensajes contienen información como:

- tipoOperacion
- idSolicitud
- datos

## Materia

Sistemas Distribuidos