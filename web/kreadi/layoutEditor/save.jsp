<%@page import="java.util.HashMap"%>
<%@page import="com.kreadi.model.Dao"%>
<%
    String json = request.getParameter("json").replaceAll("\\s+\n", "\n");
    String css = request.getParameter("css").replaceAll("\\s+\n", "\n");
    Dao dao = new Dao();
    HashMap<String, String> data = new HashMap();
    data.put("json", json);
    data.put("css", css);
    dao.setSerial("data", data);
%>