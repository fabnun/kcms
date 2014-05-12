<%@page import="java.util.LinkedList"%>
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
    LinkedList<String> superAdmins = new LinkedList();
    superAdmins.add("test@example.com");
    superAdmins.add("fabnun");

    boolean isSuperAdmin = false;
    boolean isAdmin = false;
    String username = "";
    Dao dao = new Dao();

    String rls = (String) dao.getSerial("user:rol");
    if (rls != null) {
        rls = rls == null ? "" : rls;
        String[] role = rls.split(" ");
        for (int i = 0; i < role.length; i = i + 2) {
            if ("_super_".equals(role[i + 1])) {
                superAdmins.add(role[i]);
            }
        }
    }

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
            tabla = new Table("ROOT", "Configuración");
        }
%>

<!DOCTYPE html>
<html lang="es">
    <head>
        <title><%=(tabla.name != null && tabla.name.trim().length() > 0) ? tabla.name : tabla.id%></title>
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
                String rol = (String) dao.getSerial("user:rol");
                rol = rol == null ? "" : rol;
                String[] roles = rol.split(" ");
                String usr = username;
                username = "";

                for (int i = 0; i < roles.length; i = i + 2) {
                    if (usr.equals(roles[i])) {
                        username = roles[i + 1];
                        break;
                    }
                }

                String[] usrs = tabla.admins.split(",");
                for (String us : usrs) {
                    us = us.trim().toLowerCase();
                    if (username.toLowerCase().equals(us)) {
                        isAdmin = true;
                        break;
                    }
                }
                username = usr;
            }
            if (isAdmin || isSuperAdmin) {%>
        <div id="top">
            <img src="logo.png" style="float:left;margin-left: 10px">
            <button style="position:relative;top:5px;right:16px;margin-left:32px;padding:3px 12px;" onclick="window.open('/', 'test', 'toolbar=0,titlebar=0,menubar=0,location=0,status=0,scrollbars=1,width=1200,height=600');">
                <img src="ver.png" style="position: relative; margin-bottom:-2px"> VER 
            </button>
            <%if (isSuperAdmin && tableID.equals("ROOT")) {%>

            <button style="position:relative;top:5px;right:16px;background-color:#aaffaa;padding:3px 12px;margin-left:64px">
                <a style='text-decoration:none;color:black' target='_blank' href='/kreadi/set?backup'>
                    <img src="respaldar.png" style="position: relative; margin-bottom:-2px"> RESPALDAR 
                </a>
            </button>

            <button style="position:relative;top:5px;right:16px;background-color:#ffffaa;padding:3px 12px" onclick="restore(this);">
                <img src="restaurar.png" style="position: relative; margin-bottom:-2px"> RESTAURAR 
            </button>
            
            <button style="position:relative;top:5px;right:16px;background-color:#aaffff;padding:3px 12px" onclick="window.open('/kreadi/layoutEditor/')">
                <img src="restaurar.png" style="position: relative; margin-bottom:-2px"> LAYOUTS 
            </button>

            <button style="position:relative;top:5px;right:16px;background-color:#aaffff;padding:3px 12px" onclick="users('show');">
                <img src="user.png" style="position: relative; margin-bottom:-2px"> USUARIOS 
                <div id="users" style="background:#aaffff;position:fixed;width:380px;padding:3px;margin:-32px 0 0 -16px;-webkit-box-shadow:  0px 0px 1px 2px rgba(0, 0, 0, .42);
                     box-shadow:  0px 0px 1px 2px rgba(0,0,0, .42);text-align: left;
                     -webkit-border-radius: 3px 3px 3px 3px;cursor: default;display:none;
                     border-radius: 3px 3px 3px 3px;">
                    &nbsp;&nbsp;<img src="user.png" style="position: relative; margin-bottom:-2px"> USUARIOS <span style='float:right'>[ESC para Salir]</span>
                    <br><br><table id='userTable'></table>
                </div>
            </button>
            <%}%>

            <span style="float:right;position: relative;top:5px"><%=isSuperAdmin ? "(SUPER/ADMIN) " : ""%><%=username%> <a style="margin-right:12px" id="logout" href="<%=userService.createLogoutURL(request.getRequestURI())%>">Salir</a>&nbsp;</span>
            <div id="rowButtons" style="display:none;float:right;margin-right:16px">
                <button id='upRowButton' onclick='upRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/up.png'></button>
                <button id='downRowButton' onclick='downRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/down.png'></button>
                <button onclick='addRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/add.png'></button>
                <button onclick='delRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/del.png'></button>
            </div>
        </div>
        <div id='data'></div>

        <div id="scriptDiv">
            <textarea id="script"></textarea>
            <div style="position:fixed;right:12px;top:24px;background:rgba(164,164,164,.75);z-index:30000;-webkit-border-radius: 8px 8px 8px 8px; border-radius: 8px 8px 8px 8px;">
                <input type="text" id="scriptname" style="width:164px;top:-6px;position:relative;background:white;margin-left:8px" title="filename">
                <img src="css/run.png" onclick="newTab('/' + (data.id !== '' ? data.id + '/' : '') + document.getElementById('scriptname').value)" style="cursor:pointer;top:2px;position:relative;" title="execute">
                <img src="css/save.png" onclick="saveScript(document.getElementById('scriptname').value)" style="cursor:pointer;top:2px;position:relative;"  title="save">
                <img src="css/del.png" onclick="showScript(false);" style="cursor:pointer;margin-right:8px;top:2px;position:relative;margin-top:5px"  title="close">
            </div>
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


        <div id="htmlBox" style="position:fixed; width: 100%;height: 100%;background: rgba(0,0,0,.6);top:58px;display:none">
            <div id='html' style="position:relative;max-width: 960px; width: 100%;margin: 0 auto;height: 100%;background: white"></div>
        </div>
        <div id='preview' style="text-align:center;position:fixed;width: 100%;height: 100%;background: rgba(0,0,0,.75);color:white;font-size:16px;top:56px;display:none"></div>

        <script src="ckeditor/ckeditor.js"></script>
        <script type="text/javascript" src="js/kreadi.js"></script>
        <script type = "text/javascript">
            <%if (isSuperAdmin && tableID.equals("ROOT")) {%>addUser();<%}%>
                data = <%=tabla.toJSON()%>;
                subtables = [<%=tabla.subTables(username, dao)%>];
                superAdmin =<%=isSuperAdmin%>;
                buildTable(data);

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

                function save() {
                    var scriptDiv = document.getElementById("scriptDiv");
                    var htmlDiv = document.getElementById("htmlBox");
                    if (scriptDiv.style.display === "block") {
                        saveScript(document.getElementById('scriptname').value);
                    } else if (htmlDiv.style.display === "block") {
                        saveHtml();
                    }
                }

                function view() {
                    window.open('/', 'test', 'toolbar=0,titlebar=0,menubar=0,location=0,status=0,scrollbars=1,width=1200,height=600');
                }

                onkeydown = function(val) {
                    if (val.keyCode === 27) {
                        document.getElementById("preview").style.display = "none";
                        document.getElementById("htmlBox").style.display = "none";
                        document.getElementById("scriptDiv").style.display = "none";
                        document.getElementById("users").style.display = "none";
                        if (superAdmin || data.allowAdd) {
                            document.getElementById('rowButtons').style.display = "inline-block";
                        }
                    } else if (val.ctrlKey && val.keyCode !== 17) {
                        if (val.keyCode === 83) {// CTRL+S
                            save();
                            return false;
                        } else if (val.keyCode === 69) {//CTRL+E
                            view();
                            return false;
                        }
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