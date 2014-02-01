<%@page import="com.kreadi.model.Column"%>
<%@page import="com.kreadi.model.Table"%>
<%@page import="com.kreadi.model.Dao"%>
<%@page import="com.google.appengine.api.users.User"%>
<%@page import="com.google.appengine.api.users.UserServiceFactory"%>
<%@page import="com.google.appengine.api.users.UserService"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    String[] superAdmins = new String[]{
        "test@example.com", "fabnun", "mariajose@kreadi.com"};
    boolean isSuperAdmin = false;
    boolean isAdmin = false;
    String username = "";
    Dao dao = new Dao();
    if (user != null) {
        username = user.getNickname();
        for (String sa : superAdmins) {
            if (sa.equals(username)) {
                isSuperAdmin = true;
                break;
            }
        }
        String tableID = request.getParameter("id");
        tableID = tableID == null ? "ROOT" : tableID;
        Table tabla = dao.loadTable(tableID);
        if (tabla == null) {
            tabla = new Table("ROOT", "TABLA RAIZ");
        }
%>
<!DOCTYPE html>
<html lang="es">
    <head>
        <title><%=tabla.name%></title>
        <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="css/admin.css" rel="stylesheet" type="text/css">
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <!-- Create a simple CodeMirror instance -->
        <link rel="stylesheet" href="css/codemirror.css">
        <link rel="stylesheet" href="css/fullscreen.css">
        <script src="js/codemirror.js"></script>
        <script src="js/xml.js"></script>
        <script src="js/javascript.js"></script>
        <script src="js/css.js"></script>
        <script src="js/htmlmixed.js"></script>
        <script src="js/htmlembedded.js"></script>
        <script src="js/fullscreen.js"></script>
        <script src="js/active-line.js"></script>
        <script src="js/zip.js"></script>
    </head>
    <body>
        <%

            if (tabla == null && tableID.equals("ROOT")) {
                tabla = new Table("ROOT", "");
                dao.saveTable(tabla);
            }
            if (tabla != null) {
                String[] usrs = tabla.admins.split(",");
                for (String us : usrs) {
                    us = us.trim().toLowerCase();
                    if (username.toLowerCase().equals(us)) {
                        isAdmin = true;
                        break;
                    }
                }
            }
            if (isAdmin || isSuperAdmin) {%>
        <div id="top">
            <img src="logo.png" style="float:left;margin-left: 10px">
            <button style="position:relative;top:10px;right:16px" onclick="window.open('/', 'test', 'toolbar=0,titlebar=0,menubar=0,location=0,status=0,scrollbars=1,width=1200,height=600');">
                <img src="css/run.png" style="position: relative; margin-bottom:-8px"> Ejecutar </button>
            <span style="position: relative;top:16px"><%=isSuperAdmin ? "(SUPER/ADMIN) " : ""%><%=username%> <a style="margin-right:12px" id="logout" href="<%=userService.createLogoutURL(request.getRequestURI())%>">Salir</a>&nbsp;</span>
        </div>
        <div id='data'></div>
        <%if (isSuperAdmin && tableID.equals("ROOT")) {%>
        <!--div>
            ACA LA DEBE IR CONFIGURACION DE LOS USUARIOS Y PERFILES!!!
        </div-->
        <%}%>
        <div id="scriptDiv">
            <textarea id="script"></textarea>
            <input type="text" id="scriptname" style="width:100px;position:fixed;right:92px;top:64px;z-index: 20000;" title="filename">
            <img src="css/run.png" onclick="newTab('/' + (data.id !== '' ? data.id + '/' : '') + document.getElementById('scriptname').value)" style="position:fixed;right:66px;top:68px;z-index: 20000;cursor:pointer" title="execute">
            <img src="css/save.png" onclick="saveScript(document.getElementById('scriptname').value)" style="position:fixed;right:44px;top:68px;z-index: 20000;cursor:pointer"  title="save">
            <img src="css/del.png" onclick="showScript(false);" style="position:fixed;right:22px;top:68px;z-index: 20000;cursor:pointer"  title="close">
        </div>
        <script>
            var script = document.getElementById("script");
            var editor = CodeMirror.fromTextArea(script, {
                lineNumbers: true,
                styleActiveLine: true,
                lineWrapping: true,
                mode: "application/x-ejs",
                indentUnit: 4,
                indentWithTabs: true,
                enterMode: "keep",
                tabMode: "shift"
                        //fullScreen: true
            });

        </script>

        <div id='html' style="position:fixed;width: 100%;height: 100%;background: white;top:58px;display:none"></div>
        <script src="ckeditor/ckeditor.js"></script>

        <script type="text/javascript" src="js/kreadi.js"></script>
        <script type = "text/javascript">
            data = <%=tabla.toJSON()%>;
            subtables = [<%=tabla.subTables(username, dao)%>];
            superAdmin =<%=isSuperAdmin%>;
            buildTable();
            CKEDITOR.plugins.registered['save'] = {
                init: function(editor)
                {
                    editor.addCommand('save',
                            {
                                modes: {wysiwyg: 1, source: 1},
                                exec: function(editor) {
                                    editor.updateElement();
                                    saveHtml();
                                }
                            }
                    );
                    editor.ui.addButton('Save', {label: 'Guardar', command: 'save'});
                }
            };

            // Hook each textarea with its editor
            CKEDITOR.on('instanceReady', function(e) {
                onresize();
            });

            onresize = function() {
                var te = document.getElementById('cke_tinyeditor');
                if (te) {
                    var h = document.getElementById('html').offsetHeight;
                    te.style.height = (h - 124) + "px";
                    document.getElementById('cke_tinyeditor').style.height = (h - 124) + "px";
                    document.getElementsByClassName('cke_contents')[0].style.height = (h - 88 - document.getElementsByClassName('cke_top')[0].offsetHeight) + "px";
                }
            };
        </script>
        <div id="waitDiv" style="z-index:99999;display:none;background:#15191f;opacity:.9;position:fixed;top:0;bottom:0;left:0;right:0;background-image: url(load.gif);background-repeat:no-repeat;background-position:center;">
            <div id="waitMsg" style="color:white;position:fixed;bottom:8px;right:8px"></div>
        </div>
        <%} else {%>
        <%=username%> no tiene permiso para administrar.
        <br><a href="<%=userService.createLogoutURL(request.getRequestURI())%>">Ingresar con otro correo</a>
    </body>
</html>
<%}
    } else {
        response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
    }
%>