package com.kreadi.model;

import bsh.EvalError;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.DAOBase;
import com.kreadi.compiler.Scriptlet;
import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Dao extends DAOBase {

    //Obtiene acceso al cache
    private static final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

    static {
        //syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        //Registra las clases que se persistiran
        ObjectifyService.register(Serial.class);
    }

    /**
     * Persiste un objeto
     *
     * @param objeto
     */
    public void store(Object objeto) {
        ofy().put(objeto);
    }

    /**
     * Elimina el objeto
     *
     * @param objeto
     */
    public void del(Object objeto) {
        if (objeto != null) {
            ofy().delete(objeto);
        }
    }

    /**
     * Elimina todos los objetos de una clase
     *
     * @param clase
     */
    public void delClass(Class clase) {
        Objectify ofy = ofy();
        List l = ofy.query(clase).list();
        for (Object o : l) {
            ofy.delete(o);
        }
    }

    /**
     * Obtiene la lista de objetos de una clase
     *
     * @param clase
     * @return
     */
    public List<?> query(Class<?> clase) {
        return ofy().query(clase).list();
    }

    public Serializable getSerial(String id) throws IOException, ClassNotFoundException {
        Serializable s = (Serializable) syncCache.get("s:" + id);
        if (s == null) {
            Serial ser = getObject(Serial.class, id);
            if (ser != null) {
                s = ser.getValue();
                if (s != null) {
                    syncCache.put("s:" + id, s);
                }
            }
        }
        return s;
    }

    public void setSerial(String id, Serializable serial) throws IOException, ClassNotFoundException {
        syncCache.put("s:" + id, serial);
        Serial ser = new Serial(id, serial);
        store(ser);
    }

    public boolean delSerial(String id) throws IOException, ClassNotFoundException {
        syncCache.delete("s:" + id);
        Serial ser = getObject(Serial.class, id);
        if (ser != null) {
            del(ser);
        }
        return ser != null;
    }

    /**
     * Obtiene un objecto mediante su clase e id
     *
     * @param <T>
     * @param clase
     * @param id
     * @return
     */
    public <T> T getObject(Class<T> clase, String id) {
        return ofy().find(clase, id);
    }

    /**
     * Obtiene un objecto mediante su clase e id
     *
     * @param <T>
     * @param clase
     * @param id
     * @return
     */
    public <T> T getObject(Class<T> clase, long id) {
        return ofy().find(clase, id);
    }

    public void clearCache(String key) {
        syncCache.delete(key);
    }

    public void clearAllCache() {
        syncCache.clearAll();
    }

    public String getSHA512(String pass) throws Exception {
        InputStream is = new ByteArrayInputStream(pass.getBytes("UTF-8"));
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] dataBytes = new byte[1024];
        int nread;
        while ((nread = is.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        byte[] mdbytes = md.digest();
        //convert the byte to hex format method 1
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public void saveTable(Table table) throws IOException, ClassNotFoundException {
        setSerial("TABLE." + table.id, table);
    }

    public void delTable(String id) throws IOException, ClassNotFoundException {
        id = "TABLE." + id;
        Table table = (Table) getSerial(id);
        for (String child : table.subTableMap.keySet()) {
            delTable(child);
        }
        delSerial(id);
    }

    public Table loadTable(String id) throws ClassNotFoundException, IOException {
        return (Table) getSerial("TABLE." + id);
    }
    
      public void delMapMap() throws IOException, ClassNotFoundException {
        HashSet<String> set = (HashSet<String>) getSerial("map:agent");
        if (set != null) {
            for (String browser : set) {
                delSerial("map:map:" + browser);
                System.out.println(">>> DELMAP BROWSER " + browser);
            }
        }
        delSerial("map:agent");
    }
    
    public String getValue(String url, HttpServletRequest request, HttpServletResponse response) throws ClassNotFoundException, IOException, EvalError {
        int idx = url.lastIndexOf("/");
        Table t = loadTable(url.substring(0, idx));
        HashMap<String, Serializable> map = t.getFileMap(url.substring(idx + 1));
        int n = (Integer) map.get("#n");
        if ("Script".equals(map.get("type"))) {
            String code = new String((byte[]) getSerial("file:" + map.get("key")), "UTF-8");
            String result = new Scriptlet(code).process(request, response, this, n, url);
            return result;
        } else {
            return new String((byte[]) getSerial("file:" + map.get("key")));
        }
    }

}
