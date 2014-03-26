package com.kreadi.servlets;

import bsh.EvalError;
import com.kreadi.compiler.Scriptlet;
import com.kreadi.model.Dao;
import com.kreadi.model.Table;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String uri = req.getRequestURI();
        String param = req.getQueryString();
        String server =  req.getServerName();
        if (uri.startsWith("/kreadi/") || uri.startsWith("/_ah/")) {
            chain.doFilter(request, response);
        } else {
            if (uri.startsWith("/admin")) {
                resp.sendRedirect("/kreadi/admin.jsp");
            } else {
                try {
                    HashMap<String, HashMap<String, Serializable>> keyCodes = (HashMap<String, HashMap<String, Serializable>>) dao.getSerial("map:map");
                    HashMap<String, Serializable> map = null;
                    if (keyCodes == null) {
                        keyCodes = new HashMap<String, HashMap<String, Serializable>>();
                    } else {
                        map = keyCodes.get(uri);
                    }
                    String filename;
                    boolean storeMaps = false;
                    if (map == null) {
                        int idxDiv = uri.lastIndexOf("/");
                        if (idxDiv == 0) {
                            uri = "/" + uri;
                            idxDiv = 1;
                        }
                        String tableId = idxDiv > 0 ? uri.substring(1, idxDiv) : "";

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

                            OutputStream os = resp.getOutputStream();
                            byte[] bytes;
                            int idx = 0;
                            String subId = "";
                            try {
                                int size = 0;
                                do {
                                    bytes = (byte[]) dao.getSerial("file:" + (String) map.get("key") + subId);
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
                            os.close();
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
                                    : sc.getMimeType(filename));//Obtiene el mime type

                            long now = System.currentTimeMillis();
                            long expireTime = expires != null ? (Long.parseLong(expires)) : 0;
                            resp.setDateHeader("Expires", now + expireTime);
                            resp.setHeader("Cache-Control", "public, max-age=" + expireTime);

                            resp.setContentType(mimeType);
                            resp.setHeader("Accept-Ranges", "bytes");
                            
                            String cache = filename.startsWith("_") ? null : (String) map.get("cache."+server+"/"+param);
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
                                String process = new Scriptlet(code).process(req, resp, dao, index);
                                if (!filename.startsWith("_")) {
                                    map.put("cache."+server+"/"+param, process);
                                    storeMaps=true;
                                }
                                resp.getOutputStream().write(process.getBytes("UTF-8"));
                            } else {
                                resp.getOutputStream().write(cache.getBytes("UTF-8"));
                            }
                        }
                        if (storeMaps) {
                            dao.setSerial("map:map", keyCodes);
                        }
                    } else {
                        chain.doFilter(request, response);
                    }

                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } catch (EvalError ex) {
                    resp.getWriter().print(ex.getMessage());
                }
            }
        }
    }

    public void init(FilterConfig fc) throws ServletException {
        dao = new Dao();
        setFilterConfig(fc);
    }

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
