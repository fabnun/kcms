package com.kreadi.model;

import com.google.appengine.api.datastore.Blob;
import java.io.*;
import javax.persistence.Id;

/**Almacena un objeto serializable asignado a un id*/
public class Serial implements Serializable {

    private static final long serialVersionUID =  -6214581437134474035L;
    @Id
    public String key;
    public Blob value;

    public static byte[] toBytes(Serializable ser) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(ser);
        out.close();
        bos.close();
        return bos.toByteArray();
    }

    public Serializable getValue() throws IOException, ClassNotFoundException {
        return fromBytes(value.getBytes());
    }

    public static Serializable fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bis);
        Object o = in.readObject();
        bis.close();
        in.close();
        return (Serializable) o;
    }

    public Serial() {
    }

    public Serial(String key) {
        this.key = key;
    }

    public Serial(String key, Serializable value) throws IOException {
        this.key = key;
        byte[] bytes = toBytes(value);
        this.value = new Blob(bytes);
    }


}
