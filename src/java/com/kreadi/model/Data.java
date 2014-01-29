package com.kreadi.model;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Almacena un Array de objetos serializables, ademas permite enviar y recibir datos desde un servidor
 */
public final class Data implements Serializable {

    private static final long serialVersionUID = 23478632487432834L;
    private static final Logger logger = Logger.getAnonymousLogger();
    /**
     * Objetos serializables que forman la data
     */
    public Serializable[] values;

    /**
     * Se instancia indicando los objetos de la data
     */
    public Data(Serializable... vals) {
        values = vals;
    }

    /**
     * Envia la data en un stream
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public void send2Stream(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
        oos.close();
    }

    /**
     * Envia un requerimiento de data al servidor, no muestra el log por defecto
     */
    public Data requestData(String url) throws Exception {
        return requestData(url,false);
    }

    /**
     * Envia un requerimiento de data al servidor.
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public Data requestData(String url,boolean log) throws Exception {
        if (log) {
            logger.log(Level.INFO, ">>> {0}\n", this.toString());
        }
        URL targetUrl = new URL(url);
        URLConnection conn = targetUrl.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        OutputStream os = conn.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
        Data responseData = null;
        try {
            responseData = (Data) ois.readObject();
            if (responseData != null) {
                if (responseData.values.length == 1 && responseData.values[0] instanceof Exception) {
                    throw new Exception(((Exception) responseData.values[0]).getMessage());
                } else {
                    if (log) {
                        logger.log(Level.INFO, "<<< {0}\n", responseData.toString());
                    }
                }
            }
        } catch (EOFException e) {
            logger.log(Level.SEVERE, "Error al procesar " + this, e);
        }
        oos.close();
        ois.close();
        os.close();
        return responseData;
    }

    @Override
    public String toString() {
        if (values == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean coma = false;
        if (values != null) {
            for (Object o : values) {
                if (o == null) {
                    sb.append("null,");
                } else {
                    sb.append(o.toString()).append(",");
                }
                coma = true;
            }
        }
        if (coma) {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append("}");
        return sb.toString();
    }
}
