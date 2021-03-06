package com.kreadi.compiler;

import bsh.EvalError;
import bsh.Interpreter;
import com.kreadi.model.Dao;
import com.kreadi.model.Data;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scriptlet {

    private static final String Methods
            = "import com.kreadi.model.Data;"
            + "getTable(String id){"//Obtiene la tabla asociada al id
            + " return dao.loadTable(id);"
            + "};"
            + "index(){"//Obtiene la tabla asociada al id
            + " return index;"
            + "};"
            + ""
            + "log(Object s){"//log
            + " System.out.println(s);"
            + "};"
            + ""
            + "setTable(table){"//Persiste la tabla
            + " return dao.saveTable(table);"
            + "};"
            + ""
            + "getText(html, max){"//Obtiene el texto desde html
            + " html=html.replaceAll(\"<[^>]*>\", \"\");"
            + " return html.substring(0,(int)Math.min(html.length(),max));"
            + "};"
            + ""
            + "include(String url){"//Incluye el texto un html o de un script procesado
            + " append(dao.getValue(url, request, response));"
            + "}"
            + ""
            + "append(Object s){"//Incluye un texto
            + " sb.append(s);"
            + " return sb;"
            + "};"
            + ""
            + "include(){"//Incluye el texto de otro script que es indicado con el metodo content
            + " append(include);"
            + "};"
            + ""
            + "content(String url){"//Indica la url del script contenedor
            + " include=url;"
            + "};"
	    + "content(String url, Object[] _cparams){"//Indica la url del script contenedor
            + " include=url;"
	    + " cparams=_cparams;"
            + "};";

    private static String buildCode(String scriptlet) {

        StringBuilder sb = new StringBuilder();
        sb.append(Methods);

        LinkedList<String> sections = new LinkedList<>();
        int lastPos = 0, state = 0;
        char c0 = '\0', c1;
        for (int pos = 0; pos < scriptlet.length(); pos++) {
            c1 = c0;
            c0 = scriptlet.charAt(pos);
            if (state == 0 && c0 == '%' && c1 == '<') {
                sections.add(scriptlet.substring(lastPos, pos - 1));
                lastPos = pos - 1;
                state = 1;
            } else if (state == 1 && c1 == '%' && c0 == '>') {
                sections.add(scriptlet.substring(lastPos, pos + 1));
                lastPos = pos + 1;
                state = 0;
            }
        }
        int size = scriptlet.length();
        if (lastPos != size) {
            sections.add(scriptlet.substring(lastPos, size));
        }

        for (String s : sections) {
            size = s.length();
            if (size > 0) {
                if (s.startsWith("<%") && s.endsWith("%>")) {
                    if (s.startsWith("<%=")) {
                        sb.append("sb.append(");
                        sb.append(s.substring(3, size - 2).trim());
                        sb.append(");");
                    } else {
                        sb.append(s.substring(2, size - 2));
                    }
                } else {
                    s = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"").
                            replace("\r", "\\r").replace("\t", "\\t");
                    sb.append("sb.append(\"");
                    sb.append(s);
                    sb.append("\");");
                }
            }
        }
        String result = sb.toString();
        // result = escaper.translate(result).replace("\\u", "<%u");
        return result;
    }

    public String script;
    private transient Interpreter bsh;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public Scriptlet(String script) {
        setScript(script);
    }

    public void setScript(String script) {
        this.script = script;
        bsh = null;
    }

    public Data processData(HttpServletRequest request, HttpServletResponse response, Dao dao, Data data) throws EvalError, IOException, ClassNotFoundException {
        if (request == null && response == null && dao == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream pst = new PrintStream(baos)) {
            if (bsh == null) {
                String code = buildCode(script);
                StringReader reader = new StringReader(code);
                bsh = new Interpreter(reader, pst, System.err, false);
            } else {
                bsh.setOut(pst);
            }
            
            bsh.set("dao", dao);
            bsh.set("data", data);
            bsh.set("request", request);
            bsh.set("response", response);
            bsh.run();
        }
        return (Data) bsh.get("data");
    }

    public String process(HttpServletRequest request, HttpServletResponse response, Dao dao, int index, String url) throws EvalError, IOException, ClassNotFoundException {
        return process(request, response, dao, null, index, url, new Object[0]);
    }
    
    public String process(HttpServletRequest request, HttpServletResponse response, Dao dao, String include, int index, String url) throws EvalError, IOException, ClassNotFoundException {
	return process(request, response, dao, include, index, url, new Object[0]);
    }
    
    public String process(HttpServletRequest request, HttpServletResponse response, Dao dao, String include, int index, String url, Object[] cp) throws EvalError, IOException, ClassNotFoundException {
        if (request == null && response == null && dao == null) {
            return null;
        }
        boolean nullInclude;
        StringBuilder sb;
	Object[] cparams=new Object[0];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream pst = new PrintStream(baos)) {
            if (bsh == null) {
                String code = buildCode(script);
                StringReader reader = new StringReader(code);
                bsh = new Interpreter(reader, pst, System.err, false);
            } else {
                bsh.setOut(pst);
            }
            nullInclude = include == null;
            sb = new StringBuilder();

            bsh.set("sb", sb);
	    bsh.set("cparams", cp);
            bsh.set("include", include);
            bsh.set("dao", dao);
            bsh.set("request", request);
            bsh.set("response", response);
            bsh.set("index", index);
            bsh.set("url", url);
            bsh.run();
	    cparams=(Object[]) bsh.get("cparams");
	    cparams=cparams==null?new String[0]:cparams;
            include = (String) bsh.get("include");
	    
        }
        if (include != null && nullInclude) {//Si indico la posicion del include
            String base = include;
            int idxTable = base.lastIndexOf("/");
            String tableId = base.substring(0, idxTable);
            String scriptName = base.substring(idxTable + 1);
            HashMap<String, Serializable> map = dao.loadTable(tableId).getFileMap(scriptName);
            String code = new String((byte[]) dao.getSerial("file:" + map.get("key")),"utf-8");
            String includeText = sb.toString();
            String result = new Scriptlet(code).process(request, response, dao, includeText, index, url, cparams);
            return result;
        } else {
            return sb.toString();
        }

    }
}
