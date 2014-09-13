<%@page import="eu.bitwalker.useragentutils.Browser"%>
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
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <!--link rel="stylesheet" href="css/admin.css" >
        <link rel="stylesheet" href="css/codemirror.css">
        <link rel="stylesheet" href="css/dialog.css">
        <link rel="stylesheet" href="css/fullscreen.css"-->
        <link rel="stylesheet" href="css/all.css">
        <script src="ckeditor/ckeditor.js"></script>
        <style>
            .editable{padding:12px 0 0 8px !important}
            .editable input{position:relative;margin-top:-12px;margin-left: -6px;}
            input[type=checkbox]{
                margin-left:6px;
                -webkit-box-shadow: none;
	-moz-box-shadow: none;
	box-shadow: none;
            }
        </style>
        <!--script src="js/codemirror.js"></script>
        <script src="js/search.js"></script>
        <script src="js/searchcursor.js"></script>
        <script src="js/dialog.js"></script>
        
        
        <script src="js/xml.js"></script>
        <script src="js/javascript.js"></script>
        <script src="js/css.js"></script>
        <script src="js/htmlmixed.js"></script>
        <script src="js/htmlembedded.js"></script>
        <script src="js/fullscreen.js"></script>
        <script src="js/active-line.js"></script>
        <script src="js/zip.js"></script-->
        <script src="js/all.js"></script>

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
            <img src="logo.png" alt="logo" style="float:left;margin-left: 10px">
            <button style="position:relative;top:5px;right:16px;margin-left:32px;padding:3px 12px;" onclick="window.open('/', 'test', 'toolbar=0,titlebar=0,menubar=0,location=0,status=0,scrollbars=1,width=1200,height=600');">
                <img src="ver.png" alt="ver" style="position: relative; margin-bottom:-2px"> VER 
            </button>
            <%if (isSuperAdmin && tableID.equals("ROOT")) {%>

            <button style="position:relative;top:5px;right:16px;background-color:#aaffaa;padding:3px 12px;margin-left:64px" onclick="window.open('/kreadi/set?backup')" title="Respaldar">
                <img src="respaldar.png" alt="respaldar" style="position: relative; margin-bottom:-2px"> 
            </button>
            <input type="checkbox" id="restoreType" style="width: 9px;display: inline-block;margin-right: 16px;margin-left: -12px;padding: 0;" title="Restaurar Template">
            <button id="restoreButton" style="position:relative;top:5px;right:16px;background-color:#ffffaa;padding:3px 12px" onclick="restore();" title="Restaurar">
                <img src="restaurar.png" alt="restaurar" style="position: relative; margin-bottom:-2px">
            </button>
            <button id="colorButton" style="position:relative;top:5px;right:16px;background-color:#aaffff;padding:3px 12px" onclick="window.open('/kreadi/colorist/index.html')" title="Colorist">
                <img src="palette.png" alt="restaurar" style="position: relative; margin-bottom:-2px">
            </button>
            <button style="position:relative;top:5px;right:16px;background-color:#ddff88;padding:3px 12px" onclick="users('show');"  title="Configuración">
                <img src="user.png" alt="config" style="position: relative; margin-bottom:-2px">
                <span id="users" style="background:#ddff88;position:fixed;width:380px;padding:3px;margin:-32px 2px 0 -16px;-webkit-box-shadow:  0px 0px 1px 2px rgba(0, 0, 0, .42);
                      box-shadow:  0px 0px 1px 2px rgba(0,0,0, .42);text-align: left;
                      -webkit-border-radius: 3px 3px 3px 3px;cursor: default;display:none;
                      border-radius: 3px 3px 3px 3px;">
                    &nbsp;&nbsp;<img src="user.png"  alt='Usuarios' style="position: relative; margin-bottom:-2px"><span style='float:right'>[ESC para Salir]</span>
                    <hr><br><table id='userTable'></table>
                    &nbsp;AGENTES ESPECIALES <input id="agentes" type="text" style="margin: 8px;width:95%"
                                                    onblur="agentes(this)"<%=Browser.all != null ? " value='" + Browser.all + "'" : ""%>>
                </span>
            </button>
            <%}%>

            <span style="float:right;position: relative;top:5px"><%=isSuperAdmin ? "(SUPER/ADMIN) " : ""%><%=username%> <a style="margin-right:12px" id="logout" href="<%=userService.createLogoutURL(request.getRequestURI())%>">Salir</a>&nbsp;</span>
            <div id="rowButtons" style="display:none;float:right;margin-right:16px">
                <%if (isSuperAdmin) {%><button title="Ordenar" id='sortRowButton' onclick='sortRow()' style='width:26px;height:26px;padding:0;margin-right:10px'><img src='css/sort.png'></button><%}%>
                <button id='upRowButton' title="Mover arriba" onclick='upRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/up.png'></button>
                <button id='downRowButton' title="Mover abajo" onclick='downRow()' style='width:26px;padding:0;margin-right:10px'><img src='css/down.png'></button>
                <button onclick='addRow()' title="Agregar registro" style='width:26px;padding:0;margin-right:10px'><img src='css/add.png'></button>
                <button onclick='delRow()' title="Eliminar registros" style='width:26px;padding:0;margin-right:10px'><img src='css/del.png'></button>
            </div>
        </div>
        <div id='data'></div>

        <div id="scriptDiv">
            <textarea id="script"></textarea>
            <div style="position:fixed;right:12px;top:24px;background:rgba(164,164,164,.75);z-index:30000;-webkit-border-radius: 8px 8px 8px 8px; border-radius: 8px 8px 8px 8px;">
                <input type="text" id="scriptname" style="width:140px;top:-6px;position:relative;background:white;margin-left:8px" title="filename">
                <img src="css/find.png" onclick="CodeMirror.commands['find'](editor)" style="cursor:pointer;top:2px;position:relative;"  title="find">
                <img src="css/next.png" onclick="CodeMirror.commands['findNext'](editor)" style="cursor:pointer;top:2px;position:relative;"  title="find next">
                <img src="css/prev.png" onclick="CodeMirror.commands['findPrev'](editor)" style="cursor:pointer;top:2px;position:relative;"  title="find prev">
                <img src="css/replace.png" onclick="CodeMirror.commands['replaceAll'](editor)" style="cursor:pointer;top:2px;position:relative;"  title="replace all">
                <img src="css/run.png" onclick="newTab('/' + (data.id !== '' ? data.id + '/' : '') + document.getElementById('scriptname').value)" style="cursor:pointer;top:2px;position:relative;" title="execute">
                <img src="css/save.png" onclick="saveScript(document.getElementById('scriptname').value)" style="cursor:pointer;top:2px;position:relative;"  title="save">
                <img src="css/help.png" onclick="window.open('help.html')" style="cursor:pointer;top:2px;position:relative;"  title="documentation">
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
                        document.title = ((data.name !== null && data.name.trim().length > 0) ? data.name : data.id);
                        document.getElementById("preview").style.display = "none";
                        document.getElementById("htmlBox").style.display = "none";
                        document.getElementById("scriptDiv").style.display = "none";
                        if (document.getElementById("users"))
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
        <img id="toolpreview" style="border:2px solid white; position: absolute;top:0; left:0; background: rgba(0,0,0,.25);z-index: 999999;display:none">
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