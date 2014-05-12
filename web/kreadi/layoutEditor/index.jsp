<%@page import="java.util.HashMap"%>
<%@page import="com.kreadi.model.Dao"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>LAYOUT EDITOR</title>
        <link rel="icon" href="../favicon.ico" type="image/x-icon" />
        <script type='text/javascript' src='http://code.jquery.com/jquery-1.8.3.js'></script>
        <script type="text/javascript" src="http://code.jquery.com/ui/1.9.2/jquery-ui.js"></script>
        <link rel="stylesheet" type="text/css" href="http://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css">
        <style>
            body{
                font-family: sans-serif;
                font-size: 14px;
                margin:0;
                padding: 0;
            }
            .layoutRange{
                width: 100px;
                display: table-cell;
                top:2px;
                position: relative;
            }
            .label{
                white-space: nowrap;
                margin:0;
                top:-3px;
                position: relative;
            }
        </style>
    </head>
    <body>
        <div style="display: table;width: 100%;height: 100%;position: fixed;">
            <div style="display: table-row">
                <div style="vertical-align: top;background-color: #012;color: white; display: table-cell;width:0%;padding-top:6px;padding-left: 6px">
                    <span style="padding: 4px">JSON LAYOUT</span>
                    <button onclick="send()" style="float: right;margin-right: 16px;position: relative;top:-4px">Aplicar</button>
                    <button onclick="code()" style="float: right;margin-right: 16px;position: relative;top:-4px">Obtener Código</button>
                    <br>
                    <textarea id="json" name="json" onfocus="textFocus = '#json'" spellcheck='false' style="-moz-tab-size:4; -o-tab-size:4; tab-size:4;font-family: monospace; background: #024; color:white;border: none;margin: 3px 8px 3px 3px;position: relative;padding: 6px;"></textarea>
                    <span style="padding: 4px">CSS</span><br>
                    <textarea id="css" name="css" onfocus="textFocus = '#css'" spellcheck='false' style="-moz-tab-size:4; -o-tab-size:4; tab-size:4;font-family: monospace; background: #024; color:white;border: none;margin: 3px 8px 3px 3px;position: relative;padding: 6px;resize: none;"></textarea>
                </div>
                <div style="background-color: white;display: table-cell;width: 100%">
                    <div id="mask" style="top:0;right:0;position:fixed;bottom:0;background: transparent;">
                        <div id="ruler" style="position:fixed;bottom:0;right:0;display:inline-block;background: rgba(64,255,64,.4);max-width:200px;border:1px solid rgba(0,128,0,.25);text-align: right;padding:6px"></div>
                            
                    </div>
                    <iframe id="external" style="width: 100%;height:100%" frameBorder="0"></iframe>
                </div>
            </div>
        </div>
        <script src="js/jquery-1.8.3.js"></script>
        <script src="js/jquery-ui.js"></script>
        <script src=jsTemplateEditor.js></script>
        <script>
            <%
                Dao dao = new Dao();
                HashMap<String, String> data = (HashMap<String, String>) dao.getSerial("data");
                String json = "";
                String css = "";
                if (data != null) {
                    json = data.get("json");
                    css = data.get("css");
                }
            %>
                        var json = "<%=json.replaceAll("\\s+\\n *", "\n").replaceAll("\\t", "\\\\t").replaceAll("\\n", "\\\\n").replaceAll("\"", "\\\\\"")%>";
                        var css = "<%=css.replaceAll("\\s+\\n *", "\n").replaceAll("\\t", "\\\\t").replaceAll("\\n", " \\\\n").replaceAll("\"", "\\\\\"")%>";
                        document.getElementById("json").value = json;
                        document.getElementById("css").value = css;




                        applyChange(json, css);
                        initSize();
                        function initSize() {
                            var ta_size = localStorage.getItem("xy");
                            if (ta_size === null)
                                ta_size = {width: '320px', height: '240px'};
                            else
                                ta_size = JSON.parse(ta_size);
                            $('#json').css(ta_size);
                        }
                        $('#json').resizable({
                            resize: function() {
                                var sizeHistory = JSON.stringify({width: this.style.width, height: this.style.height});
                                localStorage.setItem("xy", sizeHistory);
                                resize();
                            }
                        });
                        onresize = function() {
                            resize();
                        };
                        resize();
                        var textFocus = "#json";
                        onkeydown = function(val) {
                            if (val.keyCode === 9) {
                                val.preventDefault();
                                var start = $(textFocus).get(0).selectionStart;
                                var end = $(textFocus).get(0).selectionEnd;
                                $(textFocus).val($(textFocus).val().substring(0, start)
                                        + "\t"
                                        + $(textFocus).val().substring(end));
                                $(textFocus).get(0).selectionStart =
                                        $(textFocus).get(0).selectionEnd = start + 1;
                            }
                            if (val.ctrlKey && val.keyCode !== 17) {
                                if (val.keyCode === 83) {// CTRL+S
                                    send();
                                    return false;
                                }
                            }
                        };

                        function resize() {
                            var e0 = document.getElementById("json");
                            var e2 = document.getElementById("mask");
                            var e3 = document.getElementById("ruler");
                            var e1 = document.getElementById("css");
                            var w = window,
                                    d = document,
                                    e = d.documentElement,
                                    g = d.getElementsByTagName('body')[0],
                                    h = w.innerHeight || e.clientHeight || g.clientHeight,
                                    width = w.innerHeight || e.clientHeight || g.clientHeight;
                            e1.style.height = (h - e0.clientHeight - 98) + "px";
                            e2.style.left = (e0.clientWidth + 24) + "px";
                            e1.style.width = (e0.clientWidth - 12) + "px";
                            e3.innerHTML=e2.clientWidth+" px ";
                        }


        </script>
    </body>
</html>
