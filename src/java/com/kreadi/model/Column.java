package com.kreadi.model;

import bsh.EvalError;
import com.kreadi.compiler.Scriptlet;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Column implements Serializable {

    private static final long serialVersionUID = 4564738687234642L;

    /**
     * Nombre de la columna
     */
    public String name;
    /**
     * Clase serializable y comparable de la columna
     */
    public String type;
    /**
     * Lista de datos
     */
    public List<Serializable> data;
    /**
     * Es editable
     */
    public boolean editable = true;
    /**
     * ancho
     */
    public int width = 0;
    /**
     * Reglas
     */
    public String rules = "";
    

    public Column(String nombre, String tipo) {
        this.name = nombre;
        this.type = tipo;
        this.data = new LinkedList<Serializable>();
    }

    public void ordenar() {
        ordenar(true);
    }

    public String value(int row, Dao dao) throws IOException, ClassNotFoundException {
        return value(row, dao, null, null);
    }

    public String value(int row, Dao dao, HttpServletRequest request, HttpServletResponse response) throws IOException, ClassNotFoundException {
        HashMap<String, Serializable> map = (HashMap<String, Serializable>) data.get(row);
        if (map != null) {
            String key = (String) map.get("key");
            int n = row;
            if (key != null) {
                byte[] bytes = (byte[]) dao.getSerial("file:" + key);
                String result = new String(bytes, "UTF-8");
                if ("Script".equals(map.get("type"))) {
                    try {
                        return new Scriptlet(result).process(request, response, dao, n, "");
                    } catch (EvalError e) {
                        return e.getMessage();
                    } catch (IOException | ClassNotFoundException e) {
                        return e.getMessage();
                    }
                }
                return result;
            }
        }
        return null;
    }

    public transient LinkedList<int[]> permutaciones = new LinkedList<int[]>();

    public void ordenar(boolean ascendente) {
        permutaciones.clear();
        ordenar(0, data.size() - 1, ascendente);
    }

    private void ordenar(int inicio, int fin, boolean ascendente) {
        if (inicio < fin) {
            int k = Particionar(inicio, fin, ascendente);
            ordenar(inicio, k, ascendente);
            ordenar(k + 1, fin, ascendente);
        }
    }

    private int Particionar(int izq, int der, boolean ascendente) {
        Comparable p = (Comparable) data.get(izq);
        int i = izq, j = der;
        while (i < j) {
            if (ascendente) {
                while (((Comparable) data.get(j)).compareTo(p) > 0) {
                    j--;
                }
                while (((Comparable) data.get(i)).compareTo(p) <= 0 && i < j) {
                    i++;
                }
            } else {
                while (((Comparable) data.get(j)).compareTo(p) < 0) {
                    j--;
                }
                while (((Comparable) data.get(i)).compareTo(p) >= 0 && i < j) {
                    i++;
                }
            }
            if (i < j) {
                Swap(i, j);
            }
        }
        if (j != izq) {
            Swap(j, izq);
        }
        return j;
    }

    private void Swap(int i, int j) {
        permutaciones.add(new int[]{i, j});
        Serializable aux = data.get(i);
        data.set(i, data.get(j));
        data.set(j, aux);
    }

    public void transformColumn(String oldValue, String value, Dao dao) throws IOException, ClassNotFoundException {
        if (oldValue.equals("File")) {
            for (int i = 0; i < data.size(); i++) {
                Serializable s = data.get(i);
                HashMap<String, Serializable> map = (HashMap<String, Serializable>) s;
                if (map != null) {
                    String key = (String) map.get("key");
                    String subIdx = "";
                    int count = 0;
                    while (dao.delSerial("file:" + key + subIdx)) {
                        count++;
                        subIdx = "." + count;
                    }
                }
                data.set(i, null);
            }
        }
        if (value.equals("Number")) {
            for (int i = 0; i < data.size(); i++) {
                Serializable s = data.get(i);
                if (s != null) {
                    try {
                        data.set(i, Double.parseDouble(s.toString()));
                    } catch (NumberFormatException e) {
                        data.set(i, null);
                    }
                }
            }
        } else if (value.equals("String")) {
            for (int i = 0; i < data.size(); i++) {
                Serializable s = data.get(i);
                if (s != null) {
                    data.set(i, s.toString());
                }
            }
        } else if (value.equals("File")) {
            for (int i = 0; i < data.size(); i++) {
                data.set(i, null);
            }
        } else if (value.equals("Boolean")) {
            for (int i = 0; i < data.size(); i++) {
                Serializable s = data.get(i);
                if (s != null) {
                    data.set(i, s.toString().equals("true"));
                }
            }
        }
    }

    public int getRows() {
        return data.size();
    }

}
