package com.kreadi.servlets;

import bsh.EvalError;
import com.kreadi.compiler.Scriptlet;
import com.kreadi.model.Dao;
import com.kreadi.model.Data;
import com.kreadi.model.Table;
import eu.bitwalker.useragentutils.Browser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FiltroUrl implements Filter {

    Dao dao;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String uri = req.getRequestURI();
        String param = req.getQueryString();
        String server = req.getServerName();
        if (uri.startsWith("/kreadi/") || uri.startsWith("/_ah/")) {
            chain.doFilter(request, response);
        } else {
            if (uri.startsWith("/admin")) {
                resp.sendRedirect("/kreadi/admin.jsp");
            } else {
                String browser = "all";//Browser.parseUserAgentString(req.getHeader("User-Agent")).toString();
                try {
                    //KeyCodes guarda los datos de los archivos y scripts (Ver otra posibilad... usar el mapping de google para esto)
                    HashMap<String, HashMap<String, Serializable>> keyCodes = (HashMap<String, HashMap<String, Serializable>>) dao.getSerial("map:map:" + browser);
                    HashMap<String, Serializable> map = null;
                    if (keyCodes == null) {
                        keyCodes = new HashMap<>();
                    } else {
                        map = keyCodes.get(uri);
                    }
                    String filename;
                    boolean storeMaps = false;
                    if (map == null) {
                        int idxDiv = uri.lastIndexOf("/");
                        if (idxDiv == -1) {
                            uri = "/" + uri;
                            idxDiv = 0;
                        }
                        String tableId = idxDiv > 0 ? uri.substring(1, idxDiv) : uri.substring(0, idxDiv);

                        Table table = dao.loadTable(tableId);
                        if (table != null) {
                            filename = uri.substring(idxDiv + 1);
                            String col = req.getParameter("col");
                            String n = req.getParameter("n");
                            int num = n != null ? Integer.parseInt(n) : 1;
                            map = col != null ? table.getFileMap(Integer.parseInt(col), filename, num) : table.getFileMap(filename, num);
                            keyCodes.put(uri.equals("//") ? "/" : uri, map);
                            storeMaps = true;
                        } else {
                            chain.doFilter(request, response);
                            return;
                        }
                    } else {
                        filename = (String) map.get("name");
                    }
                    int index;
                    if (map != null) {
                        index = (Integer) map.get("#n");
                        String type = (String) map.get("type");
                        String expires = req.getParameter("expires");
                        if (!type.equals("Script")) {
                            String down = req.getParameter("down");
                            if (down != null) {
                                resp.setContentType("application/octet-stream");
                                resp.setContentLength((Integer) map.get("size"));
                                resp.setHeader("Content-Transfer-Encoding", "binary");
                            } else {
                                ServletContext sc = getFilterConfig().getServletContext();
                                int idxDot = filename.lastIndexOf(".");
                                String ext = idxDot > -1 ? filename.substring(idxDot + 1).toLowerCase() : "";
                                String mimeType = ("woff".equals(ext) ? "application/font-woff"
                                        : "ttf".equals(ext) ? "font/ttf"
                                        : "mp4".equals(ext) ? "video/mp4"
                                        : "ogv".equals(ext) ? "video/ogg"
                                        : "webm".equals(ext) ? "video/webm"
                                        : "js".equals(ext) ? "application/javascript"
                                        : sc.getMimeType(filename));

                                resp.setContentType(mimeType);
                                resp.setHeader("Accept-Ranges", "bytes");
                            }
                            resp.setHeader("content-disposition", "inline; filename=\"" + filename + "\"");

                            long now = System.currentTimeMillis();
                            long expireTime = expires != null ? (Long.parseLong(expires)) : 0;
                            resp.setDateHeader("Expires", now + expireTime);
                            resp.setHeader("Cache-Control", "public, max-age=" + expireTime);

                            try (OutputStream os = resp.getOutputStream()) {
                                byte[] bytes;
                                int idx = 0;
                                String subId = "";
                                try {
                                    int size = 0;
                                    do {
                                        String id = (String) map.get("key") + subId;
                                        bytes = (byte[]) dao.getSerial("file:" + id);
                                        if (bytes != null && bytes.length > 0) {
                                            os.write(bytes);
                                            idx++;
                                            subId = "." + idx;
                                            size = size + bytes.length;
                                        }
                                    } while (bytes != null);

                                    int idxDot = filename.lastIndexOf(".");
                                    String ext = idxDot > -1 ? filename.substring(idxDot + 1).toLowerCase() : "";
                                    ServletContext sc = getFilterConfig().getServletContext();

                                    String mimeType = ("woff".equals(ext) ? "application/font-woff"
                                            : "ttf".equals(ext) ? "font/ttf"
                                            : "mp4".equals(ext) ? "video/mp4"
                                            : "ogv".equals(ext) ? "video/ogg"
                                            : "webm".equals(ext) ? "video/webm"
                                            : "js".equals(ext) ? "application/javascript"
                                            : sc.getMimeType(filename));//Obtiene el mime type

                                    resp.setContentType(mimeType);
                                    resp.setHeader("ETag", uri + size);

                                } catch (ClassNotFoundException ex) {
                                    throw new ServletException(ex);
                                }
                            }
                        } else {
                            ServletContext sc = getFilterConfig().getServletContext();

                            int idxDot = filename.lastIndexOf(".");
                            String ext = idxDot > -1 ? filename.substring(idxDot + 1).toLowerCase() : "html";

                            String mimeType = ("woff".equals(ext) ? "application/font-woff"
                                    : "ttf".equals(ext) ? "font/ttf"
                                    : "mp4".equals(ext) ? "video/mp4"
                                    : "ogv".equals(ext) ? "video/ogg"
                                    : "webm".equals(ext) ? "video/webm"
                                    : "js".equals(ext) ? "application/javascript"
                                    : sc.getMimeType("a." + ext));//Obtiene el mime type

                            long now = System.currentTimeMillis();
                            long expireTime = expires != null ? (Long.parseLong(expires)) : 0;
                            resp.setDateHeader("Expires", now + expireTime);
                            resp.setHeader("Cache-Control", "public, max-age=" + expireTime);

                            resp.setContentType(mimeType);
                            resp.setHeader("Accept-Ranges", "bytes");

                            // Get an UserAgentStringParser and analyze the requesting client
                            String cacheKey = server + "." + filename + (param != null ? ("?" + param) : "") + "." + browser;
                            String cache = filename.startsWith("_") ? null : (String) map.get(cacheKey);

                            if (cache == null) {
                                ByteArrayOutputStream baos = new Set.Baos();
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
                                String code = baos.toString("UTF-8");
                                if (!filename.endsWith("_")) {
                                    String process = new Scriptlet(code).process(req, resp, dao, index);
                                    if (!filename.startsWith("_")) {
                                        map.put(cacheKey, process);
                                        storeMaps = true;
                                    }
                                    resp.getOutputStream().write(process.getBytes("UTF-8"));
                                } else if (req.getMethod().equals("POST")) {
                                    Data data;
                                    try (ObjectInputStream ois = new ObjectInputStream(req.getInputStream())) {
                                        data = (Data) ois.readObject();
                                        data = new Scriptlet(code).processData(req, resp, dao, data);
                                    }
                                    try (ObjectOutputStream oos = new ObjectOutputStream(resp.getOutputStream())) {
                                        oos.writeObject(data);
                                    }
                                }
                            } else {
                                resp.getOutputStream().write(cache.getBytes("UTF-8"));
                            }
                        }
                        if (storeMaps) {
                            HashSet<String> list = (HashSet<String>) dao.getSerial("map:agent");
                            list = (list != null) ? list : new HashSet<String>();
                            try {
                                dao.setSerial("map:map:" + browser, keyCodes);
                                if (!list.contains(browser)) {
                                    list.add(browser);
                                    dao.setSerial("map:agent", list);
                                }
                            } catch (IOException | ClassNotFoundException e) {
                                System.err.println("ERROR DE MAPA KEYCODES (Solucionar modificando el cache asi el api de cache de google): " + e);
                                dao.setSerial("map:map:" + browser, new HashMap<>());
                            }
                        }
                    } else {
                        chain.doFilter(request, response);
                    }
                } catch (IOException | ClassNotFoundException | NumberFormatException | ServletException ex) {
                    ex.printStackTrace();
                } catch (EvalError ex) {
                    resp.getWriter().print(ex.getMessage());
                }
            }
        }
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
        dao = new Dao();
        setFilterConfig(fc);
    }

    @Override
    public void destroy() {
        dao = null;
    }

    FilterConfig config;

    public void setFilterConfig(FilterConfig config) {
        this.config = config;
    }

    public FilterConfig getFilterConfig() {
        return config;
    }

}
