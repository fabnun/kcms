package com.kreadi.servlets;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.kreadi.model.Column;
import com.kreadi.model.Dao;
import com.kreadi.model.Serial;
import com.kreadi.model.Table;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Set extends HttpServlet {

    /**
     * Tamaño en bytes del buffer de lectura de data en peticiones
     */
    private static final int BUFFER_SIZE = 1024 * 1023;

    /**
     * Mapa de StringBuilders de lectura de data
     */
    private static final HashMap<InputStream, StringBuilder> readTextMap = new HashMap<InputStream, StringBuilder>();
    /**
     * Mapa de codigos separadores en peticiones
     */
    private static final HashMap<InputStream, String> codeMap = new HashMap<InputStream, String>();

    /**
     * Extiende el stream y le agrega la obtencion de los bytes en un rango
     */
    static class Baos extends ByteArrayOutputStream {

        public synchronized byte[] getBytes(int from, int to) {
            return Arrays.copyOfRange(buf, from, to);
        }
    }

    /**
     * Retorna los bytes de un stream
     */
    private static byte[] readByteStream(InputStream is) throws IOException {
        Baos baos = new Baos();

        int r;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((r = is.read(buffer)) > -1) {
            baos.write(buffer, 0, (int) r);
        }
        baos.close();
        if (baos.size() == 0) {
            return null;
        } else {
            return baos.toByteArray();
        }
    }

    /**
     * Almacena un stream en una secuencia de objetos serializable que tienen un array de bytes, retorna del primero
     */
    @SuppressWarnings("empty-statement")
    private static long storeData(InputStream is, Dao dao, byte[] boundaryCode) throws IOException, ClassNotFoundException {
        int boundarySize = boundaryCode.length;
        HashSet<Long> fileKeys = (HashSet<Long>) dao.getSerial("fileKeys");//Obtiene los keys actuales
        if (fileKeys == null) {//Si no existe 
            fileKeys = new HashSet<Long>();//crea un set vacio para los keys de los archivos
        }
        long key;
        Random rand = new Random();
        String subIndex = "";
        int index = 0;
        while (fileKeys.contains(key = rand.nextLong()));//Busca un key no usado
        fileKeys.add(key);//lo agrega al set de keys
        dao.setSerial("fileKeys", fileKeys);

        byte[][] buffer = new byte[2][];
        int r;
        do {
            buffer[1] = buffer[0];//Le asigna la lectura anterior
            buffer[0] = new byte[BUFFER_SIZE];//inicializa el buffer en su tamaño maximo
            r = is.read(buffer[0]);//Lee los bytes
            if (r > -1) {//Si ha leido algo
                if (r < BUFFER_SIZE) {//Si es menor al tamaño
                    byte[] EndBuff = new byte[r];
                    System.arraycopy(buffer[0], 0, EndBuff, 0, r);
                    buffer[0] = EndBuff;
                }
                boolean match = true;
                for (int i = 0; match && i < boundarySize; i++) {
                    if (r - i - 1 >= 0 && buffer[0][r - i - 1] != boundaryCode[boundarySize - i - 1]) {
                        match = false;
                    } else if (r - i - 1 < 0 && buffer[1][buffer[1].length + r - i - 1] != boundaryCode[boundarySize - i - 1]) {
                        match = false;
                    }
                }
                if (match) {
                    if (r > boundarySize) {
                        byte[] EndBuff = new byte[r - boundarySize];
                        System.arraycopy(buffer[0], 0, EndBuff, 0, r - boundarySize);
                        buffer[0] = EndBuff;
                        dao.setSerial("file:" + key + "" + subIndex, buffer[0]);
                    } else if (r < boundarySize) {
                        byte[] EndBuff = new byte[buffer[1].length - (r - boundarySize)];
                        System.arraycopy(buffer[1], 0, EndBuff, 0, EndBuff.length);
                        buffer[1] = EndBuff;
                        index--;
                        subIndex = "." + index;
                        dao.setSerial("file:" + key + "" + subIndex, buffer[1]);
                    }
                } else {
                    dao.setSerial("file:" + key + "" + subIndex, buffer[0]);
                    index++;
                    subIndex = "." + index;
                }
            }
        } while (r > -1);
        return key;
    }

    private static HashMap<String, Object> readParams(InputStream is) throws IOException {
        HashMap<String, Object> map = new HashMap<String, Object>();//mapa de parametros a retornar, debe incluir el codigo
        boolean readByte;//inicialmente tiene que leer mas bytes
        Baos baos = new Baos();//Se crea un stream de bytes para almacenar los bytes leidos
        int byte0 = 0, byte1, byte2 = 0, byte3;//ultimos bytes leidos
        do {
            byte1 = byte0;
            byte0 = is.read();
            readByte = !(byte0 == 10 && byte1 == 13);
            baos.write(byte0);//Lee byte por byte
        } while (readByte);//Repite si tiene que leer mas bytes
        byte[] boundaryCode = baos.getBytes(0, baos.size() - 2);//Obtiene el codigo de separacion

        boolean readParam;
        do {
            baos = new Baos();
            do {
                byte3 = byte2;
                byte2 = byte1;
                byte1 = byte0;
                byte0 = is.read();
                readByte = !(byte0 == 10 && byte1 == 13 && byte2 == 10 && byte3 == 13);
                baos.write(byte0);//Lee byte por byte
            } while (readByte);
            byte[] bytes = baos.getBytes(0, baos.size() - 4);
            String name = new String(bytes, "UTF-8");
            int idxName = name.indexOf("name=\"") + 6;
            name = name.substring(idxName, name.indexOf("\"", idxName));
            readParam = !name.equals("data");

            if (readParam) {
                baos = new Baos();
                int boundaryMatch = 0;//cantidad de caracteres que hacen match con los bytes de separacion
                readByte = true;
                do {
                    byte0 = is.read();
                    if (boundaryCode[boundaryMatch] == byte0) {
                        boundaryMatch++;
                        if (boundaryMatch == boundaryCode.length) {
                            readByte = false;
                        }
                    } else {
                        boundaryMatch = 0;
                    }
                    baos.write(byte0);//Lee byte por byte
                } while (readByte);
                String value = new String(baos.getBytes(0, baos.size() - boundaryCode.length - 2), "UTF-8");
                map.put(name, value);
            }

        } while (readParam);
        byte[] tmpBoundary = new byte[boundaryCode.length + 6];
        System.arraycopy(boundaryCode, 0, tmpBoundary, 2, boundaryCode.length);
        tmpBoundary[0] = 13;
        tmpBoundary[1] = 10;
        tmpBoundary[boundaryCode.length + 2] = '-';
        tmpBoundary[boundaryCode.length + 3] = '-';
        tmpBoundary[boundaryCode.length + 4] = 13;
        tmpBoundary[boundaryCode.length + 5] = 10;
        map.put("_boundaryCode_", tmpBoundary);
        return map;
    }

    private static void table2Zip(Dao dao, Table table, ZipOutputStream zipStream, LinkedList<Serial> serList) throws IOException, ClassNotFoundException {

        if (table != null && table.columns != null) {
            for (Column col : table.columns) {
                if (col.type.equals("Script") || col.type.equals("File") || col.type.equals("Html")) {
                    for (Serializable row : col.data) {
                        if (row != null) {
                            String key = (String) ((HashMap<String, Serializable>) row).get("key");
                            Serial fileSerial = dao.getObject(Serial.class, "file:" + key);
                            int count = 0;
                            while (fileSerial != null) {
                                byte[] dat = Serial.toBytes(fileSerial.getValue());
                                ZipEntry ee = new ZipEntry(fileSerial.key);
                                zipStream.putNextEntry(ee);
                                zipStream.write(dat, 0, dat.length);
                                zipStream.closeEntry();
                                count++;
                                fileSerial = dao.getObject(Serial.class, "file:" + key + "." + count);
                            }
                        }
                    }
                } else if (col.type.equals("SubTable")) {
                    for (int j = 0; j < col.getRows(); j++) {
                        Table sub = (Table) col.data.get(j);
                        table2Zip(dao, sub, zipStream, serList);
                    }
                }
            }
        }
        if (table != null && table.subTableMap != null) {
            for (String sub : table.subTableMap.keySet()) {
                serList.add(dao.getObject(Serial.class, "TABLE." + sub));
            }
        }
    }

    private static void serial2Zip(Dao dao, ZipOutputStream zipStream, LinkedList<Serial> serList) throws IOException, ClassNotFoundException {
        if (serList.size() > 0) {
            Serial s = serList.pollFirst();
            Serializable ser = s.getValue();
            try {
                byte[] data = Serial.toBytes(ser);
                ZipEntry e = new ZipEntry(s.key);
                zipStream.putNextEntry(e);
                zipStream.write(data, 0, data.length);
                zipStream.closeEntry();
                if (ser instanceof Table) {
                    table2Zip(dao, (Table) ser, zipStream, serList);
                }
                serial2Zip(dao, zipStream, serList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Procesa la peticion get
     *
     * @param req
     * @param resp
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (req.getParameter("reset") != null) {

            UserService userService = UserServiceFactory.getUserService();
            User user = userService.getCurrentUser();
            String[] superAdmins = new String[]{"test@example.com", "fabnun", "mariajose@kreadi.com"};

            boolean isSuperAdmin = false;
            if (user != null) {//SI ES OTRO COMANDO Y EL USUARIO ESTA LOGEADO
                String username = user.getNickname();
                for (String sa : superAdmins) {
                    if (sa.equals(username)) {
                        isSuperAdmin = true;
                        break;
                    }
                }
            }
            if (isSuperAdmin) {
                Dao dao = new Dao();
                try {
                    List<Serial> seriales = (List<Serial>) dao.query(Serial.class);
                    for (Serial s : seriales) {
                        dao.delSerial(s.key);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                resp.sendRedirect("/admin");
            }
        } else if (req.getParameter("backup") != null) {
            resp.setContentType("application/force-download");
            resp.setHeader("Content-Transfer-Encoding", "binary");
            SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd.HH-mm");
            resp.setHeader("content-disposition", "inline; filename=\"" + req.getServerName() + "." + sdf.format(new Date()) + ".zip\"");
            OutputStream os = resp.getOutputStream();
            Dao dao = new Dao();
            ZipOutputStream out = new ZipOutputStream(os);
            LinkedList<Serial> serList = new LinkedList<Serial>();

            try {
                serList.add(new Serial("FileKeys", dao.getSerial("fileKeys")));
                serList.add(new Serial("user:rol", dao.getSerial("user:rol")));
                serList.add(dao.getObject(Serial.class, "TABLE.ROOT"));
                serial2Zip(dao, out, serList);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            out.close();
            os.close();
        } else {
            String id = req.getParameter("id");
            String name = req.getParameter("name");
            String down = req.getParameter("download");
            if (down != null) {
                resp.setContentType("application/octet-stream");
                resp.setContentLength(Integer.parseInt(down));
                resp.setHeader("Content-Transfer-Encoding", "binary");
            } else {
                ServletContext sc = getServletContext();

                int idxDot = name.lastIndexOf(".");
                String ext = idxDot > -1 ? name.substring(idxDot + 1).toLowerCase() : "";

                String mimeType = ("woff".equals(ext) ? "application/font-woff"
                        : "ttf".equals(ext) ? "font/ttf"
                        : "mp4".equals(ext) ? "video/mp4"
                        : "ogv".equals(ext) ? "video/ogg"
                        : "webm".equals(ext) ? "video/webm"
                        : "js".equals(ext) ? "application/javascript"
                        : sc.getMimeType(name));//Obtiene el mime type

                resp.setContentType(mimeType);

                resp.setHeader("Accept-Ranges", "bytes");
            }
            resp.setHeader("content-disposition", "inline; filename=\"" + name + "\"");

            long time = 2592000000l;
            long now = System.currentTimeMillis() + time;
            resp.setDateHeader("Expires", now);
            resp.setHeader("Cache-Control", "public, max-age=" + time);
            resp.setHeader("ETag", id);
            OutputStream os = resp.getOutputStream();
            byte[] bytes;
            int idx = 0;
            String subId = "";
            Dao dao = new Dao();
            try {
                do {
                    bytes = (byte[]) dao.getSerial("file:" + id + subId);
                    if (bytes != null && bytes.length > 0) {
                        os.write(bytes);
                        idx++;
                        subId = "." + idx;
                    }
                } while (bytes != null);
            } catch (ClassNotFoundException ex) {
                throw new ServletException(ex);
            }
            os.close();
        }
    }

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch", "empty-statement"})
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        String[] superAdmins = new String[]{"test@example.com", "fabnun", "mariajose@kreadi.com"};

        InputStream is = req.getInputStream();
        res.setContentType("text/plain");
        String resp = null;
        HashMap<String, Object> paramMap = readParams(is);

        String command = (String) paramMap.get("command");//OBTIENE EL COMANDO
        if (command.equals("mail")) {//SI SOLICITA ENVIAR UN CORREO
            String mensaje = (String) paramMap.get("msg");
            String correo = (String) paramMap.get("correo");

            MailService mail = MailServiceFactory.getMailService();
            MailService.Message msg = new MailService.Message("contacto@verdeoriginal.cl", "contacto@verdeoriginal.cl", "Formulario de Contacto", mensaje);
            msg.setReplyTo(correo);
            mail.send(msg);

        } else if (user != null) {//SI ES OTRO COMANDO Y EL USUARIO ESTA LOGEADO
            boolean isSuperAdmin = false;
            String username = user.getNickname();
            for (String sa : superAdmins) {
                if (sa.equals(username)) {
                    isSuperAdmin = true;
                    break;
                }
            }
            try {
                if (isSuperAdmin //CHEQUEA COMANDOS DE SUPERUSUARIO
                        && (command.equals("newTable") || command.equals("setTable") || command.equals("delTable")
                        || command.equals("setCol") || command.equals("leftCol") || command.equals("rightCol")
                        || command.equals("delCol") || command.equals("newCol") || command.equals("backup") || command.equals("addUser") || command.equals("delUser")
                        || command.equals("serialdelete") || command.equals("serial"))) {
                    if (command.equals("newTable")) {//NUEVA TABLA (SUPERUSER)
                        String key = (String) paramMap.get("key");
                        String value = (String) paramMap.get("value");
                        String parent = (String) paramMap.get("parent");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(parent);
                        if (dao.loadTable(key) != null) {
                            resp = "Ya ocupo ese id de tabla, elija otro";
                        } else {
                            tabla.subTableMap.put(key, value);
                            Table newTable = new Table(key, value);
                            newTable.parentId = parent;
                            dao.saveTable(newTable);
                            dao.saveTable(tabla);
                        }
                    } else if (command.equals("setTable")) {//ESTABLECE UNA PROPIEDAD DE LA TABLA (SUPERUSER)
                        Dao dao = new Dao();
                        String tableId = (String) paramMap.get("table");
                        String key = (String) paramMap.get("key");
                        String value = (String) paramMap.get("value");
                        Table tabla = dao.loadTable(tableId);
                        if (key.equals("id")) {
                            if (tabla.id.equals("ROOT")) {
                                resp = "No puede modificar el id de la tabla raiz";
                            } else if (!tabla.id.equals(value)) {
                                Table tabla2 = dao.loadTable(value);
                                if (tabla2 != null) {
                                    resp = "Ya ocupo ese id de tabla, elija otro";
                                } else {
                                    if (tabla.parentId != null) {
                                        Table tablaParent = dao.loadTable(tabla.parentId);
                                        String name = tablaParent.subTableMap.get(tabla.id);
                                        tablaParent.subTableMap.remove(tabla.id);
                                        tablaParent.subTableMap.put(value, name);
                                        dao.saveTable(tablaParent);
                                    }

                                    dao.delTable("" + tableId);
                                    tabla.id = value;
                                }
                            }
                        } else if (key.equals("name")) {
                            tabla.name = value;
                            if (tabla.parentId != null) {
                                Table tablaParent = dao.loadTable(tabla.parentId);
                                tablaParent.subTableMap.put(tabla.id, value);
                                dao.saveTable(tablaParent);
                            }
                        } else if (key.equals("admins")) {
                            tabla.admins = value;
                        } else if (key.equals("allowAdd")) {
                            tabla.allowAdd = value.equals("true");
                        }
                        if (resp == null) {
                            dao.saveTable(tabla);
                        }
                    } else if (command.equals("serialdelete")) {
                        Dao dao = new Dao();
                        List<Serial> seriales = (List<Serial>) dao.query(Serial.class);
                        for (Serial s : seriales) {
                            dao.delSerial(s.key);
                        }
                    } else if (command.equals("addUser")) {
                        Dao dao = new Dao();
                        String name = (String) paramMap.get("userName");
                        name = name.trim();
                        String rol = (String) paramMap.get("userRol");
                        rol = rol.trim();
                        String roles = (String) dao.getSerial("user:rol");
                        roles = (roles == null ? "" : roles);
                        if (name.length() > 0 && name.indexOf(" ") == -1 && rol.length() > 0 && rol.indexOf(" ") == -1) {
                            roles = roles + " " + name + " " + rol;
                            roles = roles.trim();
                            dao.setSerial("user:rol", roles);
                        }
                        resp = roles.trim();
                    } else if (command.equals("delUser")) {
                        Dao dao = new Dao();
                        int idx = Integer.parseInt((String) paramMap.get("idx"));
                        String roles = (String) dao.getSerial("user:rol");
                        roles = roles == null ? "" : roles;
                        String[] rolArray = roles.split(" ");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < rolArray.length; i = i + 2) {
                            if (i / 2 != idx) {
                                sb.append(" ").append(rolArray[i]).append(" ").append(rolArray[i + 1]);
                            }
                        }
                        roles = sb.toString().trim();
                        dao.setSerial("user:rol", roles);
                        resp = roles;
                    } else if (command.equals("serial")) {
                        Dao dao = new Dao();
                        byte[] bytes = readByteStream(is);
                        Serializable ser = Serial.fromBytes(bytes);
                        String key = (String) paramMap.get("id");
                        dao.setSerial(key, ser);
                    } else if (command.equals("delTable")) {//ELIMINA UNA TABLA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        if (tabla != null) {
                            if (tabla.columns != null) {
                                for (int i = 0; i < tabla.columns.size(); i++) {
                                    if (tabla.columns.get(i).type.equals("File")) {
                                        delColumn(tabla, i, dao);
                                    }
                                }
                            }
                            if (tabla.parentId != null) {
                                tabla = dao.loadTable(tabla.parentId);
                                tabla.subTableMap.remove(id);
                                dao.saveTable(tabla);
                            }
                            dao.delTable(id);
                        }
                        if ("ROOT".equals(id)) {
                            dao.saveTable(new Table("ROOT", "Configuración"));
                        }
                    } else if (command.equals("setCol")) {//MODIFICA UNA PROPIEDAD DE UNA COLUMNA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        int idx = Integer.parseInt((String) paramMap.get("idx"));
                        String param = (String) paramMap.get("param");
                        String value = (String) paramMap.get("value");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        Column col = tabla.columns.get(idx);
                        if (param.equals("colname")) {
                            col.name = value;
                        } else if (param.equals("colwidth")) {
                            col.width = Integer.parseInt(value);
                        } else if (param.equals("coltype")) {//TODO implementar type Select
                            if (value.equals("Id")) {
                                boolean found = false;
                                for (Column c : tabla.columns) {
                                    if (c.type.equals("Id")) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    resp = "Solo puede tener una columna tipo Id.";
                                }
                            }
                            if (resp == null) {
                                String oldValue = col.type;
                                col.type = value;
                                col.transformColumn(oldValue, value, dao);
                            }
                        } else if (param.equals("coleditable")) {
                            col.editable = value.equals("true");
                        } else if (param.equals("colrules")) {
                            col.rules = value.trim();
                        }
                        dao.saveTable(tabla);
                    } else if (command.equals("leftCol")) {//MUEVE UNA COLUMNA A LA IZQUIERDA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        int idx = Integer.parseInt((String) paramMap.get("idx"));
                        int idx0 = idx - 1;
                        if (idx0 < 0) {
                            idx0 = tabla.columns.size() - 1;
                        }
                        Column col = tabla.columns.get(idx);
                        tabla.columns.set(idx, tabla.columns.get(idx0));
                        tabla.columns.set(idx0, col);
                        dao.saveTable(tabla);
                    } else if (command.equals("rightCol")) {//MUEVE UNA COLUMNA A LA DERECHA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        int idx = Integer.parseInt((String) paramMap.get("idx"));
                        int idx0 = (idx + 1) % tabla.columns.size();
                        Column col = tabla.columns.get(idx);
                        tabla.columns.set(idx, tabla.columns.get(idx0));
                        tabla.columns.set(idx0, col);
                        dao.saveTable(tabla);
                    } else if (command.equals("delCol")) {//ELIMINA UNA COLUMNA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        int idx = Integer.parseInt((String) paramMap.get("idx"));
                        delColumn(tabla, idx, dao);
                        tabla.columns.remove(idx);
                        dao.saveTable(tabla);
                    } else if (command.equals("newCol")) {//CREA UNA NUEVA COLUMNA (SUPERUSER)
                        String id = (String) paramMap.get("id");
                        Dao dao = new Dao();
                        Table tabla = dao.loadTable(id);
                        Column col = new Column((String) paramMap.get("name"), (String) paramMap.get("type"));
                        col.width = Integer.parseInt((String) paramMap.get("width"));
                        col.rules = (String) paramMap.get("rules");
                        col.editable = paramMap.get("editable").equals("true");
                        int size = 0;
                        if (tabla.columns.size() > 0) {
                            size = tabla.columns.get(0).data.size();
                        }
                        for (int i = 0; i < size; i++) {
                            col.data.add(null);
                        }
                        tabla.columns.add(col);
                        dao.saveTable(tabla);
                    }
                } else {//CHEQUEA LOS COMANDOS QUE NO SON DE SUPERUSUARIO
                    String id = (String) paramMap.get("id");
                    Dao dao = new Dao();
                    Table tabla = dao.loadTable(id);
                    boolean havePermision = isSuperAdmin;

                    String rol = (String) dao.getSerial("user:rol");
                    rol = rol == null ? "" : rol;
                    String[] roles = rol.split(" ");

                    String usr = username;
                    username = null;

                    for (int i = 0; i < roles.length; i = i + 2) {
                        if (usr.equals(roles[i])) {
                            username = roles[i + 1];
                            break;
                        }
                    }
                    if (!havePermision) {
                        String[] usuarios = tabla.admins.split(",");
                        for (String usuario : usuarios) {
                            if (usuario.equals(user.getNickname())) {
                                havePermision = true;
                                break;
                            }
                        }
                    }
                    if (havePermision) {
                        if (command.equals("setTableVal")) {//ESTABLECE UN VALOR DE LA TABLA
                            dao.delSerial("map:map");//Elimina el mapa del cache
                            String subId = (String) paramMap.get("subId");
                            Table parentTable = null;
                            if (subId != null && !"undefined".equals(subId)) {
                                subId = subId.substring(8);
                                int idx = subId.indexOf(".");
                                int scol = Integer.parseInt(subId.substring(0, idx));
                                int srow = Integer.parseInt(subId.substring(idx + 1));
                                parentTable = tabla;
                                tabla = (Table) tabla.columns.get(scol).data.get(srow);
                            };
                            String value = (String) paramMap.get("value");
                            int colIdx = Integer.parseInt((String) paramMap.get("col"));
                            int rowIdx = Integer.parseInt((String) paramMap.get("row"));
                            String type = (String) paramMap.get("type");
                            Column col = tabla.columns.get(colIdx);

                            Serializable val = value;

                            if (col.type.equals("Id")) {
                                int index = tabla.columns.get(colIdx).data.indexOf(value);
                                if (index > -1 && index != rowIdx) {
                                    resp = "Ya ocupo ese valor, elija otro.";
                                }
                            } else if (col.type.equals("Number")) {
                                try {
                                    val = Double.parseDouble(value);
                                } catch (Exception e) {
                                    val = null;
                                }
                            } else if (col.type.equals("Boolean")) {
                                val = Boolean.parseBoolean(value);
                            } else if ((type != null && type.equals("Script")) || col.type.equals("ScriptC") || col.type.equals("Html")) {
                                HashMap<String, Serializable> map;
                                try {
                                    map = (HashMap<String, Serializable>) col.data.get(rowIdx);
                                    if (map != null) {
                                        dao.delSerial("file:" + map.get("key"));//ELIMINAR VARIOS
                                    }
                                } catch (Exception e) {
                                }
                                HashSet<Long> fileKeys = (HashSet<Long>) dao.getSerial("fileKeys");
                                if (fileKeys == null) {
                                    fileKeys = new HashSet<Long>();
                                }
                                long key;
                                Random rand = new Random();
                                while (fileKeys.contains(key = rand.nextLong()));
                                fileKeys.add(key);
                                dao.setSerial("file:" + key, value.getBytes("UTF-8"));
                                dao.setSerial("fileKeys", fileKeys);
                                String name = (String) paramMap.get("name");
                                name = name == null ? ("" + key) : name;
                                val = map("type", (type != null && type.equals("Script")) ? "Script" : col.type, "name", name, "size", value.length(), "time", System.currentTimeMillis(), "key", "" + key);
                            }
                            if (resp == null) {
                                tabla.columns.get(colIdx).data.set(rowIdx, val);
                                if (parentTable != null) {
                                    dao.saveTable(parentTable);
                                } else {
                                    dao.saveTable(tabla);
                                }
                                if (val instanceof HashMap) {
                                    Gson gson = new Gson();
                                    resp = gson.toJson(val);
                                }
                            }
                        } else if (command.equals("delrow")) {//ELIMINA UN REGISTRO
                            String[] rows = ((String) paramMap.get("rows")).split(",");
                            for (int i = rows.length - 1; i >= 0; i--) {
                                for (Column col : tabla.columns) {
                                    if (col.type.equals("File")) {
                                        if (col.data.get(i) instanceof HashMap) {
                                            HashMap<String, Serializable> map = (HashMap<String, Serializable>) col.data.get(Integer.parseInt(rows[i]));
                                            if (map != null) {
                                                String key = (String) map.get("key");
                                                String subIdx = "";
                                                int count = 0;
                                                while (dao.delSerial("file:" + key + subIdx)) {
                                                    count++;
                                                    subIdx = "." + count;
                                                }
                                            }
                                        }
                                    }
                                    col.data.remove(Integer.parseInt(rows[i]));
                                }
                            }
                            dao.saveTable(tabla);
                        } else if (command.equals("addrow")) {//CREA UN NUEVO REGISTRO
                            for (Column col : tabla.columns) {
                                col.data.add(null);
                            }
                            dao.saveTable(tabla);
                        } else if (command.equals("rename")) {//RENOMBRA UN ARCHIVO
                            int colIdx = Integer.parseInt((String) paramMap.get("col"));
                            int rowIdx = Integer.parseInt((String) paramMap.get("row"));
                            HashMap<String, Serializable> map = null;
                            try {
                                map = (HashMap<String, Serializable>) tabla.columns.get(colIdx).data.get(rowIdx);
                            } catch (Exception e) {
                            }
                            map.put("name", (String) paramMap.get("name"));
                            dao.saveTable(tabla);
                        } else if (command.equals("getData")) {//OBTIENE EL JSON DE LA TABLA
                            resp = tabla.toJSON();
                        } else if (command.equals("getText")) {//OBTIENE EL TEXTO ASOCIADO A UN KEY DE UN FILE (SOLO UN KEY... HASTA 1024 KB)
                            int colIdx = Integer.parseInt((String) paramMap.get("col"));
                            int rowIdx = Integer.parseInt((String) paramMap.get("row"));
                            HashMap<String, Serializable> map = null;
                            try {
                                map = (HashMap<String, Serializable>) tabla.columns.get(colIdx).data.get(rowIdx);
                            } catch (Exception e) {
                            }
                            if (map == null) {
                                resp = "";
                            } else {
                                ByteArrayOutputStream baos = new Baos();
                                byte[] bytes;
                                String subId = "";
                                String key = (String) map.get("key");
                                int idx = 0;
                                do {
                                    bytes = (byte[]) dao.getSerial("file:" + key + subId);
                                    if (bytes != null && bytes.length > 0) {
                                        baos.write(bytes);
                                        idx++;
                                        subId = "." + idx;
                                    }
                                } while (bytes != null);
                                resp = baos.toString("UTF-8");
                            }
                        } else if (command.equals("downrow")) {//MUEVO UN REGISTRO ABAJO
                            String[] rows = ((String) paramMap.get("rows")).split(",");
                            for (int j = rows.length - 1; j >= 0; j--) {
                                int index = Integer.parseInt(rows[j]);
                                for (int i = 0; i < tabla.columns.size(); i++) {
                                    Serializable val0 = tabla.columns.get(i).data.get(index);
                                    Serializable val1 = tabla.columns.get(i).data.get(index + 1);
                                    tabla.columns.get(i).data.set(index, val1);
                                    tabla.columns.get(i).data.set(index + 1, val0);
                                }
                            }
                            dao.saveTable(tabla);
                        } else if (command.equals("uprow")) {//MUEVE UN REGISTRO ARRIVA
                            String[] rows = ((String) paramMap.get("rows")).split(",");
                            for (String row : rows) {
                                int index = Integer.parseInt(row);
                                for (int i = 0; i < tabla.columns.size(); i++) {
                                    Serializable val0 = tabla.columns.get(i).data.get(index);
                                    Serializable val1 = tabla.columns.get(i).data.get(index - 1);
                                    tabla.columns.get(i).data.set(index, val1);
                                    tabla.columns.get(i).data.set(index - 1, val0);
                                }
                            }
                            dao.saveTable(tabla);
                        } else if (command.equals("upload")) {//REALIZA UN UPLOAD

                            String subId = (String) paramMap.get("sid");
                            Table parentTable = null;
                            if (subId != null && !"undefined".equals(subId)) {
                                subId = subId.substring(8);
                                int idx = subId.indexOf(".");
                                int scol = Integer.parseInt(subId.substring(0, idx));
                                int srow = Integer.parseInt(subId.substring(idx + 1));
                                parentTable = tabla;
                                tabla = (Table) tabla.columns.get(scol).data.get(srow);
                            };

                            String name = (String) paramMap.get("name");
                            int size = Integer.parseInt((String) paramMap.get("size"));
                            int colIdx = Integer.parseInt((String) paramMap.get("col"));
                            int rowIdx = Integer.parseInt((String) paramMap.get("row"));
                            Long key = null;
                            try {
                                //byte[] bytes = readByteStream(is);
                                //key=storeFile(bytes, dao);
                                key = storeData(is, dao, (byte[]) paramMap.get("_boundaryCode_"));
                                HashMap<String, Serializable> map = map("type", "File", "name", name, "size", size, "time", System.currentTimeMillis(), "key", "" + key);
                                tabla.columns.get(colIdx).data.set(rowIdx, map);
                                readTextMap.remove(is);
                                codeMap.remove(is);
                                Gson gson = new Gson();
                                resp = gson.toJson(map);
                            } catch (Exception e) {
                                resp = "Error:" + e.getMessage();
                                if (key != null) {
                                    String subIdx = "";
                                    int count = 0;
                                    while (dao.delSerial("file:" + key + subIdx)) {
                                        count++;
                                        subIdx = "." + count;
                                    }
                                }
                            }
                            if (parentTable == null) {
                                dao.saveTable(tabla);
                            } else {
                                dao.saveTable(parentTable);
                            }
                        } else if (command.equals("restore")) {//REALIZA UN RESTORE
                            try {
                                byte[] bytes = readByteStream(is);
                                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                                ZipInputStream stream = new ZipInputStream(bais);
                                List<Serial> seriales = (List<Serial>) dao.query(Serial.class);
                                for (Serial s : seriales) {
                                    dao.delSerial(s.key);
                                }
                                ZipEntry entry;
                                while ((entry = stream.getNextEntry()) != null) {
                                    String key = entry.getName();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] buffer = new byte[BUFFER_SIZE];
                                    int len;
                                    while ((len = stream.read(buffer)) > 0) {
                                        baos.write(buffer, 0, len);
                                    }
                                    baos.close();
                                    Serializable ser = Serial.fromBytes(baos.toByteArray());
                                    dao.setSerial(key, ser);
                                }
                            } catch (Exception e) {
                                resp = "Error:" + e.getMessage();
                            }
                        } else if (command.equals("upRow2")) {//EN UNA SUBTABLA SUBE LOS REGISTROS SELECCIONADOS
                            int col = Integer.parseInt((String) paramMap.get("col"));
                            int row = Integer.parseInt((String) paramMap.get("row"));
                            dao.saveTable(tabla);
                        } else if (command.equals("downRow2")) {//EN UNA SUBTABLA BAJA LOS REGISTROS SELECCIONADOS
                            int col = Integer.parseInt((String) paramMap.get("col"));
                            int row = Integer.parseInt((String) paramMap.get("row"));
                            dao.saveTable(tabla);
                        } else if (command.equals("addRow2")) {//EN UNA SUBTABLA CREA UN REGISTRO AL FINAL////////////////////////////
                            int col = Integer.parseInt((String) paramMap.get("col"));
                            int row = Integer.parseInt((String) paramMap.get("row"));
                            Table subTabla = (Table) tabla.columns.get(col).data.get(row);
                            if (subTabla == null) {
                                subTabla = new Table(null, null);
                                String rules = tabla.columns.get(col).rules;
                                String rexType = "String|Number|Boolean|Select|Script|Html|File|Id|SubTable";
                                String[] types = rexType.split("\\|");
                                String rexCol = "(" + rexType + ")\\d*(\\([^\\)]+\\))?\\s*";
                                Pattern pattern = Pattern.compile(rexCol);
                                Matcher match = pattern.matcher(rules);

                                while (match.find()) {
                                    String colDef = match.group().trim();
                                    String type = null;
                                    for (String tp : types) {
                                        if (colDef.startsWith(tp)) {
                                            type = tp;
                                            break;
                                        }
                                    }
                                    colDef = colDef.substring(type.length());

                                    String rule = null;

                                    if (colDef.endsWith(")")) {
                                        int idx = colDef.indexOf("(");
                                        rule = colDef.substring(idx + 1, colDef.length() - 1);
                                        colDef = colDef.substring(0, idx);
                                    }

                                    int size = 0;
                                    if (colDef.length() > 0) {

                                        size = Integer.parseInt(colDef);
                                    }

                                    Column column = new Column(null, type);
                                    column.rules = rule;
                                    column.width = size;
                                    if (subTabla.columns == null) {
                                        subTabla.columns = new LinkedList<Column>();
                                    }
                                    subTabla.columns.add(column);
                                }
                            }
                            for (Column subcol : subTabla.columns) {
                                subcol.data.add(null);
                            }
                            tabla.columns.get(col).data.set(row, subTabla);
                            resp = subTabla.toJSON();
                            dao.saveTable(tabla);
                        } else if (command.equals("delRow2")) {//EN UNA SUBTABLA ELIMINA LOS REGISTROS SELECCIONADOS
                            int col = Integer.parseInt((String) paramMap.get("col"));
                            int row = Integer.parseInt((String) paramMap.get("row"));
                            Table subTabla = (Table) tabla.columns.get(col).data.get(row);
                            if (subTabla != null) {
                                String[] checks = ((String) paramMap.get("checks")).split(",");

                                for (int i = checks.length - 1; i >= 0; i--) {
                                    String scheck = checks[i];
                                    int icheck = Integer.parseInt(scheck);
                                    for (Column subcol : subTabla.columns) {
                                        subcol.data.remove(icheck);
                                        //Si tiene archivos o recursos externos debe eliminarlos del data store
                                    }
                                }
                                if (subTabla.getRows() == 0) {
                                    subTabla = null;
                                }
                                tabla.columns.get(col).data.set(row, subTabla);
                                if (subTabla != null) {
                                    resp = subTabla.toJSON();
                                }
                                dao.saveTable(tabla);
                            }
                        }
                    } else {
                        codeMap.remove(is);
                        readTextMap.remove(is);
                        res.getWriter().print("USER NOT HAVE PERMISSION");
                    }
                }
            } catch (Exception e) {
                resp = e.getClass().getSimpleName() + ": " + e.getMessage();
                e.printStackTrace();
            }
            if (resp != null) {
                res.getWriter().print(resp);
            }
        } else {
            res.getWriter().print("USER IS NOT LOGGED");
        }
        readTextMap.remove(is);
        codeMap.remove(is);
    }

    private void delColumn(Table tabla, int idx, Dao dao) throws IOException, ClassNotFoundException {
        Column col = tabla.columns.get(idx);
        for (Serializable ser : col.data) {
            if (ser instanceof HashMap) {
                HashMap<String, Serializable> map = (HashMap) ser;
                String tipo = (String) map.get("type");
                if (tipo.equals("File")) {
                    String key = (String) map.get("key");
                    String subIdx = "";
                    int count = 0;
                    while (dao.delSerial("file:" + key + subIdx)) {
                        count++;
                        subIdx = "." + count;
                    }
                }
            }
        }
    }

    public HashMap<String, Serializable> map(Serializable... obj) {
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        for (int i = 0; i < obj.length; i += 2) {
            map.put((String) obj[i], obj[i + 1]);
        }
        return map;
    }

}
