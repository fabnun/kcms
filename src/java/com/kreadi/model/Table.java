package com.kreadi.model;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Implementa una tabla de objetos serializables
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 7436873683476784L;

    /**
     * ID unico de la tabla
     */
    public String id;
    /**
     * Nombre o descripcion de la tabla
     */
    public String name;
    /**
     * Lista de correos de administracion de la tabla
     */
    public String admins = "";
    /**
     * Soporta agregar/eliminar/reordenar los registros
     */
    public boolean allowAdd = false;
    /**
     * Id de la tabla padre
     */
    public String parentId;

    /**
     * Lista de columnas de la tabla
     */
    public LinkedList<Column> columns = new LinkedList<>();

    /**
     * Mapa de Id y nombres de las subtablas
     */
    public HashMap<String, String> subTableMap = new HashMap<>();

    /**
     * Instancia una nueva tabla indicando su id y su nombre
     *
     * @param id
     * @param nombre
     */
    public Table(String id, String nombre) {
        this.id = id;
        this.name = nombre;
    }

    /**
     * Obtiene la cantidad de registros de la tabla
     *
     * @return
     */
    @Deprecated
    public int getRows() {
        if (columns.size() > 0) {
            return columns.get(0).data.size();
        } else {
            return 0;
        }
    }

    /**
     * Obtiene la cantidad de registros de la tabla
     *
     * @return
     */
    public int rows() {
        if (columns.size() > 0) {
            return columns.get(0).data.size();
        } else {
            return 0;
        }
    }

    /**
     * Retorna la cantidad de columnas
     *
     * @return
     */
    public int cols() {
        return columns == null ? 0 : columns.size();
    }

    /**
     * Agrega una registro al final
     *
     * @param row
     */
    public void addRow(Serializable[] row) {
        addRow(row, 0);
    }

    /**
     * Agrega un registro en una posicion especifica
     *
     * @param row
     * @param idx
     */
    public void addRow(Serializable[] row, int idx) {
        int cols = cols();
        for (int i = 0; i < cols; i++) {
            columns.get(i).data.add(idx, row[i]);
        }
    }

    /**
     * @param col
     * @param row
     * @param dao
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public String value(int col, int row, Dao dao) throws IOException, ClassNotFoundException {
        return columns.get(col).value(row, dao);
    }

    /**
     *
     * @param col
     * @param row
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Serializable value(int col, int row) throws IOException, ClassNotFoundException {
        Serializable val = columns.get(col).data.get(row);
        return val;
    }

    /**
     * Obtiene la url del registro de la primera columna tipo File
     *
     * @param row
     * @return
     */
    public String getURLParam(int row) {
        int colIdx = 0;
        while (columns.size() > colIdx && !columns.get(colIdx).type.equals("File")) {
            colIdx++;
        }
        if (columns.size() > colIdx && columns.get(colIdx).type.equals("File")) {
            return getURLParam(colIdx, row);
        }
        return null;
    }

    /**
     * Obtiene la url de un registro file de una columna especifica
     *
     * @param col
     * @param row
     * @return
     */
    public String getURLParam(int col, int row) {
        try {
            Serializable ser = columns.get(col).data.get(row);
            if (ser != null) {
                HashMap<String, Serializable> map = (HashMap<String, Serializable>) ser;
                return "/kreadi/set?id=" + map.get("key") + "&name=" + map.get("name");
            } else {
                return "";
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Obtiene el tamaño de una imagen
     *
     * @param col
     * @param row
     * @param dao
     * @return
     */
    public String getSize(int col, int row, Dao dao, int w, int h) {
        try {
            Serializable ser = columns.get(col).data.get(row);
            if (ser != null) {
                HashMap<String, Serializable> map = (HashMap<String, Serializable>) ser;
                String key0 = (String) map.get("key");
                Object o=dao.getSerial("size:" + key0);
                if (o!=null){
                    int[] size = (int[]) o;
                    return size != null ? "w:" + size[0] + ", h:" + size[1] : "w:" + w + ", h:" + h;
                } else {
                    return "w:" + w + ", h:" + h;
                }
            } else {
                return "w:" + w + ", h:" + h;
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Obtiene la url del registro de la primera columna tipo File
     *
     * @param row
     * @return
     */
    public String getURL(int row) {
        int colIdx = 0;
        while (columns.size() > colIdx && !columns.get(colIdx).type.equals("File")) {
            colIdx++;
        }
        if (columns.size() > colIdx && columns.get(colIdx).type.equals("File")) {
            return getURL(colIdx, row);
        }
        return null;
    }

    /**
     * Obtiene la url de un registro file de una columna especifica
     *
     * @param col
     * @param row
     * @return
     */
    public String getURL(int col, int row) {
        try {
            Column column = columns.get(col);
            Serializable ser = column.data.get(row);
            if (ser != null) {
                HashMap<String, Serializable> map2, map = (HashMap<String, Serializable>) ser;
                String nam = (String) map.get("name");
                int count = 0;
                for (int i = 0; i < column.getRows() && i < row; i++) {
                    map2 = (HashMap<String, Serializable>) column.data.get(i);
                    if (map2 != null && map2.get("name").equals(nam)) {
                        count++;
                    }
                }
                return id + "/" + map.get("name")
                        + (count > 0 ? ("?n=" + (count + 1)) : "");
            } else {
                return "";
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     *
     * @param col
     * @param filename
     * @return
     */
    public HashMap<String, Serializable> getFileMap(int col, String filename) {
        try {
            for (Serializable ser : columns.get(col).data) {
                HashMap<String, Serializable> map = null;
                try {
                    map = (HashMap<String, Serializable>) ser;
                } catch (Exception e) {
                }
                if (map != null && filename.matches((String) map.get("name")) || (filename.length() == 0 && "index.html".matches((String) map.get("name")))) {
                    return map;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public int getFileMapIndex(int col, String filename) {
        try {
            int idx = 0;
            for (Serializable ser : columns.get(col).data) {
                HashMap<String, Serializable> map = null;
                try {
                    map = (HashMap<String, Serializable>) ser;
                } catch (Exception e) {
                }
                if (map != null && filename.matches((String) map.get("name")) || (filename.length() == 0 && "index.html".matches((String) map.get("name")))) {
                    return idx;
                }
                idx++;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     *
     * @param filename
     * @return
     */
    public HashMap<String, Serializable> getFileMap(String filename) {
        int colIdx = 0;
        while (columns.size() > colIdx && !(columns.get(colIdx).type.equals("File") || columns.get(colIdx).type.equals("Html") || columns.get(colIdx).type.equals("Script"))) {
            colIdx++;
        }
        if (columns.size() > colIdx && (columns.get(colIdx).type.equals("File") || columns.get(colIdx).type.equals("Html") || columns.get(colIdx).type.equals("Script"))) {
            return getFileMap(colIdx, filename);
        }
        return null;
    }

    /**
     * Agrega una nueva columna al fina
     *
     * @param nombre
     * @param tipo
     * @return
     */
    public Column addCol(String nombre, String tipo) {
        Column columna = new Column(nombre, tipo);
        columns.add(columna);
        return columna;
    }

    /**
     * Agrega una nueva columna al fina
     *
     * @param nombre
     * @param tipo
     * @param rules
     * @return
     */
    public Column addCol(String nombre, String tipo, String rules) {
        Column columna = new Column(nombre, tipo);
        columns.add(columna);
        columna.rules = rules;
        return columna;
    }

    /**
     * Obtiene la representacion json de la tabl
     *
     * @return
     */
    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     *
     * @param username
     * @param dao
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public String subTables(String username, Dao dao) throws ClassNotFoundException, IOException {
        username = username.trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        Set<String> set = subTableMap.keySet();
        Table table;
        String rol = (String) dao.getSerial("user:rol");
        rol = rol == null ? "" : rol;
        String[] roles = rol.split(" ");
        String user = username;
        username = null;

        for (int i = 0; i < roles.length; i = i + 2) {
            if (user.equals(roles[i])) {
                username = roles[i + 1];
                break;
            }
        }
        if (username != null) {
            HashSet<String> del = new HashSet<>();
            for (String idSubTable : set) {
                table = dao.loadTable(idSubTable);
                if (table == null) {
                    del.add(idSubTable);
                } else {
                    String[] users = table.admins.split(",");
                    for (String usr : users) {
                        usr = usr.trim().toLowerCase();
                        if (usr.equals(username)) {
                            sb.append(table.id).append("\",\"");
                        }
                    }
                }
            }
            for (String s : del) {
                set.remove(s);
            }
            dao.saveTable(this);
        }
        int size = sb.length();
        if (size >= 2) {
            sb.insert(0, "\"");
            sb.delete(size - 1, size + 1);
        }
        return sb.toString();
    }

}
