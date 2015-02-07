package com.kreadi.servlets;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
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
    private static final HashMap<InputStream, StringBuilder> readTextMap = new HashMap<>();
    /**
     * Mapa de codigos separadores en peticiones
     */
    private static final HashMap<InputStream, String> codeMap = new HashMap<>();

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

    @SuppressWarnings("empty-statement")
    private static long newEmptyData(Dao dao) throws IOException, ClassNotFoundException {
        HashSet<Long> fileKeys = (HashSet<Long>) dao.getSerial("fileKeys");//Obtiene los keys actuales
        if (fileKeys == null) {//Si no existe 
            fileKeys = new HashSet<>();//crea un set vacio para los keys de los archivos
        }
        long key;
        Random rand = new Random();
        String subIndex = "";

        while (fileKeys.contains(key = rand.nextLong()));//Busca un key no usado
        fileKeys.add(key);//lo agrega al set de keys
        dao.setSerial("fileKeys", fileKeys);
        dao.setSerial("file:" + key + subIndex, new byte[0]);
        return key;
    }

    /**
     * Almacena un stream en una secuencia de objetos serializable que tienen un array de bytes, retorna del primero
     */
    @SuppressWarnings("empty-statement")
    private static long storeData(InputStream is, Dao dao, byte[] boundaryCode) throws IOException, ClassNotFoundException {
        int boundarySize = boundaryCode.length;
        HashSet<Long> fileKeys = (HashSet<Long>) dao.getSerial("fileKeys");//Obtiene los keys actuales
        if (fileKeys == null) {//Si no existe 
            fileKeys = new HashSet<>();//crea un set vacio para los keys de los archivos
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
        int pos = 0;
        buffer[0] = new byte[1024 * 16];//Buffer de lectura
        buffer[1] = new byte[BUFFER_SIZE];//Buffer de almacenado
        do {
            r = is.read(buffer[0]);//Lee los bytes
            if (r > -1) {//Si ha leido algo
                if (pos + r < BUFFER_SIZE) {//Si es menor al tamaño maximo
                    System.arraycopy(buffer[0], 0, buffer[1], pos, r);
                    pos = pos + r;
                } else {
                    byte[] bytes = new byte[pos];
                    System.arraycopy(buffer[1], 0, bytes, 0, pos);
                    dao.setSerial("file:" + key + subIndex, bytes);
                    index++;
                    subIndex = "." + index;
                    buffer[1] = new byte[BUFFER_SIZE];
                    System.arraycopy(buffer[0], 0, buffer[1], 0, r);
                    pos = r;
                }
                boolean match = true;
                for (int i = 0; match && i < boundarySize; i++) {
                    if (r - i - 1 >= 0 && buffer[0][r - i - 1] != boundaryCode[boundarySize - i - 1]) {
                        match = false;
                    } else if (r - i - 1 < 0 && buffer[1][pos - i - 1] != boundaryCode[boundarySize - i - 1]) {
                        match = false;
                    }
                }
                if (match) {
                    if (pos > boundarySize) {
                        byte[] bytes = new byte[pos - boundarySize];
                        System.arraycopy(buffer[1], 0, bytes, 0, pos - boundarySize);

                        dao.setSerial("file:" + key + subIndex, bytes);
                        index++;
                        subIndex = "." + index;

                    } else if (r < boundarySize) {
                        byte[] EndBuff = new byte[buffer[1].length - (r - boundarySize)];
                        System.arraycopy(buffer[1], 0, EndBuff, 0, EndBuff.length);
                        buffer[1] = EndBuff;
                        index--;
                        subIndex = "." + index;
                        dao.setSerial("file:" + key + subIndex, buffer[1]);
                    }
                }
            }
        } while (r > -1);
        return key;
    }

    private static HashMap<String, Object> readParams(InputStream is) throws IOException {
        HashMap<String, Object> map = new HashMap<>();//mapa de parametros a retornar, debe incluir el codigo
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
                switch (col.type) {
                    case "Script":
                    case "File":
                    case "Html":
                        for (Serializable row : col.data) {
                            if (row != null) {
                                String key = (String) ((HashMap<String, Serializable>) row).get("key");
                                Serial fileSerial = dao.getObject(Serial.class, "file:" + key);
                                int count = 0;
                                while (fileSerial != null) {
                                    byte[] dat = Serial.toBytes(fileSerial.getValue());
                                    ZipEntry ee = new ZipEntry(fileSerial.key);
                                    try {
                                        zipStream.putNextEntry(ee);
                                        zipStream.write(dat, 0, dat.length);
                                        zipStream.closeEntry();
                                        count++;
                                        fileSerial = dao.getObject(Serial.class, "file:" + key + "." + count);
                                    } catch (Exception e) {
                                        System.err.println(e.getMessage());
                                        fileSerial = null;
                                    }

                                }
                            }
                        }
                        break;
                    case "SubTable":
                        for (int j = 0; j < col.getRows(); j++) {
                            Table sub = (Table) col.data.get(j);
                            table2Zip(dao, sub, zipStream, serList);
                        }
                        break;
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
            if (s != null) {
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
            } else {
                System.out.println("now!!! aca paso algo!!!");
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
            LinkedList<String> superAdmins = new LinkedList();
            superAdmins.add("test@example.com");
            superAdmins.add("fabnun");

            try {
                String rls = (String) new Dao().getSerial("user:rol");
                if (rls != null) {
                    rls = rls == null ? "" : rls;
                    String[] role = rls.split(" ");
                    for (int i = 0; i < role.length; i = i + 2) {
                        if ("_super_".equals(role[i + 1])) {
                            superAdmins.add(role[i]);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

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
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                resp.sendRedirect("/admin");
            }
        } else if (req.getParameter("backup") != null) {

            UserService userService = UserServiceFactory.getUserService();
            User user = userService.getCurrentUser();
            LinkedList<String> superAdmins = new LinkedList();
            superAdmins.add("test@example.com");
            superAdmins.add("fabnun");

            try {
                String rls = (String) new Dao().getSerial("user:rol");
                if (rls != null) {
                    rls = rls == null ? "" : rls;
                    String[] role = rls.split(" ");
                    for (int i = 0; i < role.length; i = i + 2) {
                        if ("_super_".equals(role[i + 1])) {
                            superAdmins.add(role[i]);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

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

                resp.setContentType("application/force-download");
                resp.setHeader("Content-Transfer-Encoding", "binary");
                SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd.HH-mm");
                resp.setHeader("content-disposition", "inline; filename=\"" + req.getServerName() + "." + sdf.format(new Date()) + ".zip\"");
                try (OutputStream os = resp.getOutputStream()) {
                    Dao dao = new Dao();
                    try (ZipOutputStream out = new ZipOutputStream(os)) {
                        LinkedList<Serial> serList = new LinkedList<>();
                        try {
                            serList.add(new Serial("fileKeys", dao.getSerial("fileKeys")));
                            serList.add(new Serial("user:rol", dao.getSerial("user:rol")));
                            serList.add(dao.getObject(Serial.class, "TABLE.ROOT"));
                            serial2Zip(dao, out, serList);
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } else {
            String id = req.getParameter("id");
            String resize = req.getParameter("resize");
            int[] resi = null;
            if (resize != null) {
                try {
                    resize = resize.trim().toLowerCase();
                    int idx = resize.indexOf("x");
                    resi = new int[]{Integer.parseInt(resize.substring(0, idx)), Integer.parseInt(resize.substring(idx + 1))};
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
                                    :"woff2".equals(ext) ? "application/font-woff2"
                                    : "ttf".equals(ext) ? "font/ttf"
                                            : "mp4".equals(ext) ? "video/mp4"
                                                    : "ogv".equals(ext) ? "video/ogg"
                                                            : "webm".equals(ext) ? "video/webm"
                                                                    : "js".equals(ext) ? "application/javascript"
                                                                            : "appcache".equals(ext) ? "text/cache-manifest"
                                                                                    : sc.getMimeType(name));

                resp.setContentType(mimeType);

                resp.setHeader("Accept-Ranges", "bytes");
            }
            resp.setHeader("content-disposition", "inline; filename=\"" + name + "\"");

            long time = 2592000000l;//por defecto expiran en 1 mes los recursos estaticos
            long now = System.currentTimeMillis() + time;
            resp.setDateHeader("Expires", now);
            resp.setHeader("Cache-Control", "public, max-age=" + time);
            resp.setHeader("ETag", id);
            byte[] bytes;
            int idx = 0;
            String subId = "";
            if (resi != null) {
                Dao dao = new Dao();

                try {
                    byte[] newImageData;
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        do {
                            bytes = (byte[]) dao.getSerial("file:" + id + subId);
                            if (bytes != null && bytes.length > 0) {
                                os.write(bytes);
                                idx++;
                                subId = "." + idx;
                            }
                        } while (bytes != null);
                        bytes = os.toByteArray();
                        ImagesService imagesService = ImagesServiceFactory.getImagesService();
                        Image oldImage = ImagesServiceFactory.makeImage(bytes);
                        Transform res = ImagesServiceFactory.makeResize(resi[0], resi[1]);
                        Image newImage = imagesService.applyTransform(res, oldImage);
                        newImageData = newImage.getImageData();
                    }
                    resp.getOutputStream().write(newImageData);

                } catch (ClassNotFoundException ex) {
                    throw new ServletException(ex);
                }
            } else {
                try (OutputStream os = resp.getOutputStream()) {

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
                }
            }
        }
    }

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch", "empty-statement"})
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        LinkedList<String> superAdmins = new LinkedList();
        superAdmins.add("test@example.com");
        superAdmins.add("fabnun");

        try {
            String rls = (String) new Dao().getSerial("user:rol");
            if (rls != null) {
                rls = rls == null ? "" : rls;
                String[] role = rls.split(" ");
                for (int i = 0; i < role.length; i = i + 2) {
                    if ("_super_".equals(role[i + 1])) {
                        superAdmins.add(role[i]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        InputStream is = req.getInputStream();
        res.setContentType("text/plain");
        String resp = null;
        HashMap<String, Object> paramMap = readParams(is);

        String command = (String) paramMap.get("command");//OBTIENE EL COMANDO
        if (command.equals("mail")) {//SI SOLICITA ENVIAR UN CORREO
            String mensaje = (String) paramMap.get("msg");
            String to = (String) paramMap.get("to");
            String from = (String) paramMap.get("from");
            String subject = (String) paramMap.get("subject");

            MailService mail = MailServiceFactory.getMailService();
            MailService.Message msg = new MailService.Message(from, to, subject, mensaje);
            msg.setReplyTo(to);
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
                        && (command.equals("newTable") || command.equals("setTable") || command.equals("delTable") || command.equals("movTable") || command.equals("agentes")
                        || command.equals("setCol") || command.equals("leftCol") || command.equals("rightCol")
                        || command.equals("delCol") || command.equals("newCol") || command.equals("backup") || command.equals("addUser") || command.equals("delUser")
                        || command.equals("serialdelete") || command.equals("serial") || command.equals("serial2"))) {
                    switch (command) {
                        case "newTable": {
                            //NUEVA TABLA (SUPERUSER)
                            String key = (String) paramMap.get("key");
                            String value = (String) paramMap.get("value");
                            String parent = (String) paramMap.get("parent");
                            System.out.println(key + " " + value + " " + parent);
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(parent);
                            if (tabla == null) {
                                tabla = new Table("ROOT", "Configuración");
                                dao.saveTable(tabla);
                            }
                            if (dao.loadTable(key) != null) {
                                resp = "Ya ocupo ese id de tabla, elija otro";
                            } else {
                                tabla.subTableMap.put(key, value);
                                Table newTable = new Table(key, value);
                                newTable.parentId = parent;
                                dao.saveTable(newTable);
                                dao.saveTable(tabla);
                            }
                            break;
                        }
                        case "setTable": {
                            //ESTABLECE UNA PROPIEDAD DE LA TABLA (SUPERUSER)
                            Dao dao = new Dao();
                            String tableId = (String) paramMap.get("table");
                            String key = (String) paramMap.get("key");
                            String value = (String) paramMap.get("value");
                            Table tabla = dao.loadTable(tableId);
                            switch (key) {
                                case "id":
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
                                    break;
                                case "name":
                                    tabla.name = value;
                                    if (tabla.parentId != null) {
                                        Table tablaParent = dao.loadTable(tabla.parentId);
                                        tablaParent.subTableMap.put(tabla.id, value);
                                        dao.saveTable(tablaParent);
                                    }
                                    break;
                                case "admins":
                                    tabla.admins = value;
                                    break;
                                case "allowAdd":
                                    tabla.allowAdd = value.equals("true");
                                    break;
                            }
                            if (resp == null) {
                                dao.saveTable(tabla);
                            }
                            break;
                        }
                        case "serialdelete": {
                            Dao dao = new Dao();
                            List<Serial> seriales = (List<Serial>) dao.query(Serial.class);
                            for (Serial s : seriales) {
                                dao.delSerial(s.key);
                            }
                            break;
                        }
                        case "addUser": {
                            Dao dao = new Dao();
                            String name = (String) paramMap.get("userName");
                            name = name.trim();
                            String rol = (String) paramMap.get("userRol");
                            rol = rol.trim();
                            String roles = (String) dao.getSerial("user:rol");
                            roles = (roles == null ? "" : roles);
                            if (name.length() > 0 && !name.contains(" ") && rol.length() > 0 && !rol.contains(" ")) {
                                roles = roles + " " + name + " " + rol;
                                roles = roles.trim();
                                dao.setSerial("user:rol", roles);
                            }
                            resp = roles.trim();
                            break;
                        }
                        case "delUser": {
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
                            break;
                        }
                        case "serial": {
                            Dao dao = new Dao();
                            byte[] bytes = readByteStream(is);
                            Serializable ser = Serial.fromBytes(bytes);
                            String key = (String) paramMap.get("id");
                            dao.setSerial(key, ser);
                            break;
                        }
                        case "serial2": {
                            Dao dao = new Dao();
                            String key = "/" + paramMap.get("id");//Obtiene el id
                            if (!key.endsWith("/")) {//Ignora los directorios
                                byte[] bytesrc = readByteStream(is);//Obtiene los bytes enviados
                                int size = Integer.parseInt((String) paramMap.get("size"));//Obtiene el tamaño real del archivo
                                byte[] bytes = new byte[size];//Crea el buffer destino del tamaño correcto
                                System.arraycopy(bytesrc, 0, bytes, 0, size);//copia al buffer destino
                                bytesrc = null;//borra los bytes leidos
                                int idx = key.lastIndexOf("/");//Obtiene la ultima posicion del separador de carpetas
                                String folder = key.substring(0, idx);//Obtiene la carpeta
                                key = key.substring(idx + 1);//obtiene el archivo de la carpeta
                                Table table = null, parentTable;
                                String subFolders[] = folder.split("/");//Obtiene las carpetas
                                String ffolder = "";
                                for (String f : subFolders) {//recorre las carpetas
                                    parentTable = table;
                                    ffolder = ffolder + f;
                                    table = dao.loadTable(ffolder);
                                    if (table == null) {
                                        table = new Table(ffolder, f);
                                        table.addCol("", "File");
                                        if (parentTable == null) {
                                            table.parentId = "ROOT";
                                            Table troot = dao.loadTable("ROOT");
                                            if (troot == null) {
                                                troot = new Table("ROOT", "");
                                            }
                                            troot.subTableMap.put(ffolder, ffolder);
                                            dao.saveTable(troot);
                                        } else {
                                            parentTable.subTableMap.put(ffolder, ffolder);
                                            dao.saveTable(parentTable);
                                            table.parentId = parentTable.id;
                                        }
                                        dao.saveTable(table);
                                    }
                                    if (ffolder.length() > 0) {
                                        ffolder = ffolder + "/";
                                    }
                                }

                                Column col = table.columns.get(0);
                                HashSet<Long> fileKeys = (HashSet<Long>) dao.getSerial("fileKeys");
                                if (fileKeys == null) {
                                    fileKeys = new HashSet<>();
                                }
                                long rkey;
                                Random rand = new Random();
                                while (fileKeys.contains(rkey = rand.nextLong()));
                                fileKeys.add(rkey);

                                dao.setSerial("file:" + rkey, bytes);
                                dao.setSerial("fileKeys", fileKeys);
                                String name = key;
                                name = name == null ? ("" + rkey) : name;

                                col.data.add(map("type", "File", "name", name, "size", bytes.length, "time", System.currentTimeMillis(), "key", "" + rkey));
                                dao.saveTable(table);
                            }
                            break;
                        }
                        case "movTable": {
                            //MUEVE UNA TABLA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            String parentId = ((String) paramMap.get("parentId")).trim();
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            Table parentTabla = dao.loadTable(parentId);

                            if (tabla != null && parentTabla != null && !parentId.equals(tabla.parentId)) {
                                String s = tabla.parentId;
                                tabla.parentId = parentId;
                                dao.saveTable(tabla);

                                parentTabla.subTableMap.put(tabla.id, tabla.name);
                                dao.saveTable(parentTabla);

                                tabla = dao.loadTable(s);
                                tabla.subTableMap.remove(id);
                                dao.saveTable(tabla);

                            }
                            break;
                        }
                        case "delTable": {
                            //ELIMINA UNA TABLA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            if (tabla != null) {
                                if (tabla.columns != null) {
                                    for (int i = 0; i < tabla.columns.size(); i++) {
                                        if (tabla.columns.get(i).type.equals("File")) {
                                            //delColumn(tabla, i, dao);//TODO implementar... sin eliminar dobles referencias
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
                            break;
                        }
                        case "setCol": {
                            //MODIFICA UNA PROPIEDAD DE UNA COLUMNA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            int idx = Integer.parseInt((String) paramMap.get("idx"));
                            String param = (String) paramMap.get("param");
                            String value = (String) paramMap.get("value");
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            Column col = tabla.columns.get(idx);
                            switch (param) {
                                case "colname":
                                    col.name = value;
                                    break;
                                case "colwidth":
                                    col.width = Integer.parseInt(value);
                                    break;
                                case "coltype":
                                    //TODO implementar el type Select
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
                                    break;
                                case "coleditable":
                                    col.editable = value.equals("true");
                                    break;
                                case "colrules":
                                    col.rules = value.trim();
                                    break;
                            }
                            dao.saveTable(tabla);
                            break;
                        }
                        case "leftCol": {
                            //MUEVE UNA COLUMNA A LA IZQUIERDA (SUPERUSER)
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
                            break;
                        }
                        case "rightCol": {
                            //MUEVE UNA COLUMNA A LA DERECHA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            int idx = Integer.parseInt((String) paramMap.get("idx"));
                            int idx0 = (idx + 1) % tabla.columns.size();
                            Column col = tabla.columns.get(idx);
                            tabla.columns.set(idx, tabla.columns.get(idx0));
                            tabla.columns.set(idx0, col);
                            dao.saveTable(tabla);
                            break;
                        }
                        case "delCol": {
                            //ELIMINA UNA COLUMNA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            int idx = Integer.parseInt((String) paramMap.get("idx"));
                            //delColumn(tabla, idx, dao);//TODO eliminar sin eliminar doble referencia
                            tabla.columns.remove(idx);
                            dao.saveTable(tabla);
                            break;
                        }
                        case "newCol": {
                            //CREA UNA NUEVA COLUMNA (SUPERUSER)
                            String id = (String) paramMap.get("id");
                            Dao dao = new Dao();
                            Table tabla = dao.loadTable(id);
                            if (tabla == null) {
                                tabla = new Table("ROOT", "Configuración");
                            }
                            Column col = new Column((String) paramMap.get("name"), (String) paramMap.get("type"));
                            col.width = Integer.parseInt((String) paramMap.get("width"));
                            col.rules = (String) paramMap.get("rules");
                            col.editable = paramMap.get("editable").equals("true");
                            int size = 0;
                            if (tabla.columns != null) {
                                if (tabla.columns.size() > 0) {
                                    size = tabla.columns.get(0).data.size();
                                }
                            } else {
                                tabla.columns = new LinkedList<>();
                            }
                            for (int i = 0; i < size; i++) {
                                col.data.add(null);
                            }
                            tabla.columns.add(col);
                            dao.saveTable(tabla);
                            break;
                        }
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
                            if (usuario.equals(username)) {
                                havePermision = true;
                                break;
                            }
                        }
                    }
                    if (havePermision) {
                        switch (command) {
                            case "setTableVal": {
                                //ESTABLECE UN VALOR DE LA TABLA
                                dao.resetCache();
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
                                        fileKeys = new HashSet<>();
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
                                break;
                            }
                            case "delrow": {
                                dao.resetCache();
                                //ELIMINA UN REGISTRO
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
                                break;
                            }
                            case "drag":
                                //TODO hacer el drag

                                String tid = (String) paramMap.get("tid");
                                String fid = (String) paramMap.get("fid");
                                String from0 = (String) paramMap.get("from");
                                String to0 = (String) paramMap.get("to");
                                int idxFrom = from0.indexOf("-");
                                int idxTo = to0.indexOf("-");
                                String from1 = from0.substring(idxFrom + 1);
                                from0 = from0.substring(0, idxFrom);
                                String to1 = to0.substring(idxTo + 1);
                                to0 = to0.substring(0, idxTo);

                                Table tableFrom = dao.loadTable(fid);
                                Table tableTo = dao.loadTable(tid);

                                String tipoFrom = null;
                                Serializable elementFrom = null;

                                String tipoTo = null;

                                if (from0.endsWith("data")) {
                                    idxFrom = from1.indexOf(".");
                                    int row = Integer.parseInt(from1.substring(0, idxFrom));
                                    int col = Integer.parseInt(from1.substring(idxFrom + 1));
                                    tipoFrom = tableFrom.columns.get(col).type;
                                    elementFrom = tableFrom.columns.get(col).data.get(row);
                                } else {
                                    from0 = from0.substring(8);
                                    idxFrom = from0.indexOf(".");
                                    int col = Integer.parseInt(from0.substring(0, idxFrom));
                                    int row = Integer.parseInt(from0.substring(idxFrom + 1));
                                    Table subTable = (Table) tableFrom.columns.get(col).data.get(row);
                                    idxFrom = from1.indexOf(".");
                                    row = Integer.parseInt(from1.substring(0, idxFrom));
                                    col = Integer.parseInt(from1.substring(idxFrom + 1));
                                    tipoFrom = subTable.columns.get(col).type;
                                    elementFrom = subTable.columns.get(col).data.get(row);
                                }

                                if (to0.endsWith("data")) {
                                    idxFrom = to1.indexOf(".");
                                    int row = Integer.parseInt(to1.substring(0, idxFrom));
                                    int col = Integer.parseInt(to1.substring(idxFrom + 1));
                                    tipoTo = tableTo.columns.get(col).type;
                                    if (tipoTo.equals(tipoFrom)) {
                                        tableTo.columns.get(col).data.set(row, elementFrom);
                                        dao.saveTable(tableTo);
                                        resp = elementFrom.toString();
                                    } else {
                                        resp = "";
                                    }
                                } else {
                                    to0 = to0.substring(8);
                                    idxTo = to0.indexOf(".");
                                    int col = Integer.parseInt(to0.substring(0, idxTo));
                                    int row = Integer.parseInt(to0.substring(idxTo + 1));
                                    Table subTable = (Table) tableTo.columns.get(col).data.get(row);
                                    idxTo = to1.indexOf(".");
                                    row = Integer.parseInt(to1.substring(0, idxTo));
                                    col = Integer.parseInt(to1.substring(idxTo + 1));
                                    tipoTo = subTable.columns.get(col).type;
                                    if (tipoTo.equals(tipoFrom)) {
                                        subTable.columns.get(col).data.set(row, elementFrom);
                                        dao.saveTable(tableTo);
                                        resp = elementFrom.toString();
                                    } else {
                                        resp = "";
                                    }
                                }

                                break;
                            case "addrow":
                                //CREA UN NUEVO REGISTRO
                                int before = Integer.parseInt((String) paramMap.get("before"));
                                for (Column col : tabla.columns) {
                                    if (before == -1) {
                                        col.data.add(null);
                                    } else {
                                        col.data.add(before, null);
                                    }
                                }
                                dao.saveTable(tabla);
                                dao.resetCache();
                                break;
                            case "rename": {
                                dao.resetCache();
                                //RENOMBRA UN ARCHIVO
                                int colIdx = Integer.parseInt((String) paramMap.get("col"));
                                int rowIdx = Integer.parseInt((String) paramMap.get("row"));
                                String name = (String) paramMap.get("name");
                                HashMap<String, Serializable> map;
                                try {
                                    map = (HashMap<String, Serializable>) tabla.columns.get(colIdx).data.get(rowIdx);
                                    if (map == null) {
                                        long key = newEmptyData(dao);
                                        map = map("type", "File", "name", name, "size", 0, "time", System.currentTimeMillis(), "key", "" + key);
                                        tabla.columns.get(colIdx).data.set(rowIdx, map);
                                        Gson gson = new Gson();
                                        resp = gson.toJson(map);
                                    } else {
                                        map.put("name", name);
                                        resp = "OK";
                                    }
                                    dao.saveTable(tabla);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "getData":
                                //OBTIENE EL JSON DE LA TABLA
                                resp = tabla.toJSON();
                                break;
                            case "getText": {
                                String suid = (String) paramMap.get("subId");
                                if (suid != null && !"undefined".equals(suid)) {
                                    suid = suid.substring(8);
                                    int idx = suid.indexOf(".");
                                    int colIdx = Integer.parseInt(suid.substring(0, idx));
                                    int rowIdx = Integer.parseInt(suid.substring(idx + 1));
                                    try {
                                        tabla = (Table) tabla.columns.get(colIdx).data.get(rowIdx);
                                    } catch (Exception e) {
                                    }
                                }
                                //OBTIENE EL TEXTO ASOCIADO A UN KEY DE UN FILE (SOLO UN KEY... HASTA 1024 KB)
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
                                break;
                            }
                            case "downrow": {
                                dao.resetCache();
                                //MUEVO UN REGISTRO ABAJO
                                String[] rows = ((String) paramMap.get("rows")).split(",");
                                for (int j = rows.length - 1; j >= 0; j--) {
                                    int index = Integer.parseInt(rows[j]);
                                    for (Column column : tabla.columns) {
                                        Serializable val0 = column.data.get(index);
                                        Serializable val1 = column.data.get(index + 1);
                                        column.data.set(index, val1);
                                        column.data.set(index + 1, val0);
                                    }
                                }
                                dao.saveTable(tabla);
                                break;
                            }
                            case "uprow": {
                                dao.resetCache();
                                //MUEVE UN REGISTRO ARRIVA
                                String[] rows = ((String) paramMap.get("rows")).split(",");
                                for (String row : rows) {
                                    int index = Integer.parseInt(row);
                                    for (Column column : tabla.columns) {
                                        Serializable val0 = column.data.get(index);
                                        Serializable val1 = column.data.get(index - 1);
                                        column.data.set(index, val1);
                                        column.data.set(index - 1, val0);
                                    }
                                }
                                dao.saveTable(tabla);
                                break;
                            }
                            case "sortrow": {
                                dao.resetCache();
                                //MUEVE UN REGISTRO ARRIVA
                                String[] rows = ((String) paramMap.get("sort")).split(",");
                                int col = 0, colIdx = -1;

                                for (Column column : tabla.columns) {
                                    if (column.type.equals("File")) {
                                        colIdx = col;
                                        break;
                                    }
                                    col++;
                                }
                                LinkedList<Integer> sortResult = new LinkedList<>();
                                boolean ok = true;
                                for (String s : rows) {
                                    int idx = tabla.getFileMapIndex(colIdx, s);
                                    if (idx == -1) {
                                        resp = "ERROR: " + s + " no encontrado";
                                        ok = false;
                                        break;
                                    }
                                    sortResult.add(idx);
                                }
                                if (ok) {
                                    resp = "";
                                    Table tabla2 = dao.loadTable(id);
                                    for (Column col2 : tabla.columns) {
                                        col2.data.clear();
                                    }
                                    for (int i : sortResult) {
                                        int colId = 0;
                                        for (Column col2 : tabla.columns) {
                                            tabla.columns.get(colId).data.add(tabla2.value(colId, i));
                                            colId++;
                                        }
                                    }
                                    dao.saveTable(tabla);
                                }

                                break;
                            }
                            case "upload": {
                                dao.resetCache();
                                //REALIZA UN UPLOAD
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

                                break;
                            }
                            case "restore":
                                //REALIZA UN RESTORE
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
                                break;
                            case "upRow2": {
                                dao.resetCache();
                                //EN UNA SUBTABLA SUBE LOS REGISTROS SELECCIONADOS
                                int col = Integer.parseInt((String) paramMap.get("col"));
                                int rw = Integer.parseInt((String) paramMap.get("row"));
                                Table subTabla = (Table) tabla.columns.get(col).data.get(rw);
                                if (subTabla != null) {
                                    String[] rows = ((String) paramMap.get("rows")).split(",");
                                    for (String row : rows) {
                                        int index = Integer.parseInt(row);
                                        for (Column column : subTabla.columns) {
                                            Serializable val0 = column.data.get(index);
                                            Serializable val1 = column.data.get(index - 1);
                                            column.data.set(index, val1);
                                            column.data.set(index - 1, val0);
                                        }
                                    }
                                    dao.saveTable(tabla);
                                }
                                break;
                            }
                            case "downRow2": {
                                dao.resetCache();
                                //EN UNA SUBTABLA BAJA LOS REGISTROS SELECCIONADOS
                                int col = Integer.parseInt((String) paramMap.get("col"));
                                int row = Integer.parseInt((String) paramMap.get("row"));
                                Table subTabla = (Table) tabla.columns.get(col).data.get(row);
                                if (subTabla != null) {
                                    String[] rows = ((String) paramMap.get("rows")).split(",");
                                    for (int j = rows.length - 1; j >= 0; j--) {
                                        int index = Integer.parseInt(rows[j]);
                                        for (Column column : subTabla.columns) {
                                            Serializable val0 = column.data.get(index);
                                            Serializable val1 = column.data.get(index + 1);
                                            column.data.set(index, val1);
                                            column.data.set(index + 1, val0);
                                        }
                                    }
                                    dao.saveTable(tabla);
                                }
                                break;
                            }
                            case "addRow2": {
                                dao.resetCache();
                                //EN UNA SUBTABLA CREA UN REGISTRO AL FINAL////////////////////////////
                                int col = Integer.parseInt((String) paramMap.get("col"));
                                int row = Integer.parseInt((String) paramMap.get("row"));
                                before = Integer.parseInt((String) paramMap.get("before"));
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
                                            subTabla.columns = new LinkedList<>();
                                        }
                                        subTabla.columns.add(column);
                                    }
                                }
                                for (Column subcol : subTabla.columns) {
                                    if (before == -1) {
                                        subcol.data.add(null);
                                    } else {
                                        subcol.data.add(before, null);
                                    }
                                }
                                tabla.columns.get(col).data.set(row, subTabla);
                                resp = subTabla.toJSON();
                                dao.saveTable(tabla);
                                break;
                            }
                            case "delRow2": {
                                dao.resetCache();
                                //EN UNA SUBTABLA ELIMINA LOS REGISTROS SELECCIONADOS
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
                                break;
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
                if (tipo.equals("File") || tipo.equals("Script") || tipo.equals("Html")) {
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
        HashMap<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < obj.length; i += 2) {
            map.put((String) obj[i], obj[i + 1]);
        }
        return map;
    }

}
