package com.kreadi.servlets;

import bsh.EvalError;
import com.kreadi.compiler.Scriptlet;
import com.kreadi.model.Dao;
import com.kreadi.model.Data;
import com.kreadi.model.Table;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
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
        if (uri.startsWith("/kreadi/") || uri.startsWith("/_ah/")) {
            chain.doFilter(request, response);
        } else {
            if (uri.startsWith("/admin")) {
                resp.sendRedirect("/kreadi/admin.jsp");
            } else {
                String filename;
                int idxDiv = uri.lastIndexOf("/");
                if (idxDiv == -1) {
                    uri = "/" + uri;
                    idxDiv = 0;
                }
                filename = uri.substring(idxDiv + 1);
                String cacheKey = "fmap:" + uri + ":" + param;
                HashMap<String, Serializable> map = filename.startsWith("_") ? null : (HashMap<String, Serializable>) dao.getCache(cacheKey);

                if (map == null) {
                    try {

                        String tableId = idxDiv > 0 ? uri.substring(1, idxDiv) : uri.substring(0, idxDiv);
                        Table table = dao.loadTable(tableId);
                        if (table != null) {
                            String col = req.getParameter("col");
                            map = col == null ? table.getFileMap(filename) : table.getFileMap(Integer.parseInt(col), filename);
                        } else {
                            chain.doFilter(request, response);
                            return;
                        }
                    } catch (ClassNotFoundException | IOException | NumberFormatException | ServletException ex) {
                        ex.printStackTrace();
                        resp.getWriter().print(ex.getMessage());
                    }
                }
                String code = null;
                try {
                    Integer index = (Integer) map.get("#n");
                    index = index == null ? 0 : index;
                    String type = (String) map.get("type");
                    String expires = req.getParameter("expires");
                    if (!type.equals("Script")) {//Si no es script
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
                    } else {//Es un script
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

                        String cacheKey2 = "fmap:" + uri + ":" + param;
                        byte[] cacheBytes = !filename.startsWith("_") ? null : (byte[]) dao.getCache(cacheKey2);
                        if (cacheBytes != null) {
                            resp.getOutputStream().write(cacheBytes);
                        } else {
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
                            code=baos.toString("UTF-8");
                            if (!filename.endsWith("_")) {//Si no termina en _
                                Scriptlet script = new Scriptlet(code);
                                String process = script.process(req, resp, dao, index, uri);
                                cacheBytes=process.getBytes("UTF-8");
                                resp.getOutputStream().write(cacheBytes);
                                if (!filename.startsWith("_")){
                                        dao.setCache(cacheKey2, cacheBytes);
                                }
                            } else if (req.getMethod().equals("POST")) {//Si termina en _ y es un metodo post retorna data
                                Data data;
                                try (ObjectInputStream ois = new ObjectInputStream(req.getInputStream())) {
                                    data = (Data) ois.readObject();
                                    data = new Scriptlet(code).processData(req, resp, dao, data);
                                }
                                try (ObjectOutputStream oos = new ObjectOutputStream(resp.getOutputStream())) {
                                    oos.writeObject(data);
                                }
                            }
                        }
                        //TODO Guardar resultado del script
                    }
                } catch (ClassNotFoundException | IOException | NumberFormatException | ServletException | EvalError ex) {
                    ex.printStackTrace();
                    if (code!=null) { resp.getWriter().print(code); System.err.println("\n---------------------------------------------------------\n"+code); } 
                    resp.getWriter().print(ex.getMessage());
                }
                if (!filename.startsWith("_")) {
                    dao.setCache(cacheKey, map);
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
