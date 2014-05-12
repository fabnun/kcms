
function doLayouth(layout) {
    if (layout.length > 0) {
        var buf = [];
        for (var i = 0; i < layout.length; i++) {
            buf.push(doLayouth(layout[i]));
            css = null;
        }
        return buf.join("");
    }
    var stringBuffer = [];
    
    stringBuffer.push("<div");
    if (layout.dh) {
        layout.style = layout.style ? layout.style : "";
        layout.style = layout.style + "display:table;width:100%;";
    }
    for (var a in layout) {
        if (a !== "dv" && a !== "dh" && a!=="por") {
            stringBuffer.push(" " + a + "=\"", layout[a], "\"");
        }
    }
    stringBuffer.push(">");
    if (layout.dv) {
        for (var i = 0; i < layout.dv.length; i++) {
            stringBuffer.push(doLayouth(layout.dv[i]));
        }
    }
    if (layout.dh) {
        for (var i = 0; i < layout.dh.length; i++)
            stringBuffer.push(doLayouth( layout.dh[i]));
    }
    stringBuffer.push((layout.id || layout.class) && !layout.dh && !layout.dv ? ((layout.id?layout.id:"")+(layout.class?(" ."+layout.class.split(" ").join(".")):"")).trim() : "");
    stringBuffer.push("</div>\n");
    var html = stringBuffer.join("");
    if (layout.por && layout.por > 1) {
        var h0 = "";
        for (var i = 0; i < layout.por; i++)
            h0 = h0 + html;
        html = h0;
    }
    return html;
}

function send() {
    var XHR = new XMLHttpRequest();
    var json = document.getElementById("json").value;
    var css = document.getElementById("css").value;
    XHR.addEventListener('load', function() {
        applyChange(json, css);
    });
    XHR.addEventListener('error', function() {
        alert('Oups! Something goes wrong.');
    });
    XHR.open("POST", "save.jsp");
    XHR.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    XHR.send("json=" + encodeURI(json) + "&css=" + encodeURI(css));
}

if (!String.prototype.trim) {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g, '');
    };
}

if (!String.prototype.rtrim) {
    String.prototype.rtrim = function() {
        return this.replace(/~+$/, '');
    };
}

if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(str) {
        return this.indexOf(str) === 0;
    };
}

if (!String.prototype.endsWith) {
    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };
}

function compile2Json(code) {
    var codeLines = code.trim().split("\n");
    var lines = codeLines.length;
    var lista = [];
    for (var i = 0; i < lines; i++) {
        var lineComands = codeLines[i].split(/ +/);
        if (codeLines[i].trim().length > 0) {
            var id = null, clase = "", tab = 0;
            while (codeLines[i].charAt(tab) === '\t')
                tab++;
            var style = [];
            var por = 1;
            for (var j = 0; j < lineComands.length; j++) {
                var cmd = lineComands[j];
                var command = cmd.trim();
                if (command.length > 0) {
                    if (command.indexOf(':') > -1) {
                        style.push(command + ";");
                    } else if (command.endsWith("%")) {
                        style.unshift("width:" + command + ";display:inline-flex;");
                    } else if (command.startsWith(".")) {
                        clase = clase + " " + command.substring(1);
                    } else if (command.startsWith("*")) {
                        por = parseInt(command.substring(1));
                    } else
                        id = command;
                    clase = clase.trim();
                }
            }
            var elemento = {tab: tab};
            if (clase.length > 0) {
                elemento.class = clase;
            }
             if (id && id.trim().length > 0) {
                elemento.id = id;
            }
            if (style.length > 0) {
                elemento.style = style.join("");
                style = [];
            }
            if (por > 1) {
                elemento.por = por;
            }

            lista.push(elemento);
        }
    }
    do {
        var idxStart = -1, idxEnd = -1;
        var more = false;
        for (var i = 1; i < lista.length; i++) {
            if (lista[i].tab > lista[i - 1].tab) {
                idxStart = i;
            } else if (lista[i].tab < lista[i - 1].tab) {
                idxEnd = i - 1;
                break;
            }
        }
        if (idxStart !== -1 && idxEnd === -1) {
            idxEnd = lista.length - 1;
        }
        if (idxStart !== -1 && idxEnd !== -1) {
            var childs = [];
            var isDH = true;
            for (var i = 0; i <= idxEnd - idxStart; i++) {
                var child = lista[idxStart];
                delete child.tab;
                if (child.style===undefined || (child.style && !child.style.startsWith("width:")))
                    isDH = false;
                childs.push(child);
                lista.splice(idxStart, 1);
            }
            if (isDH)
                lista[idxStart - 1].dh = childs;
            else
                lista[idxStart - 1].dv = childs;
        }
        for (var i = 0; i < lista.length; i++) {
            if (lista[i].tab && lista[i].tab > 0) {
                more = true;
                break;
            }

        }
    } while (more);
    for (var i = 0; i < lista.length; i++) {
        delete lista[i].tab;
        delete lista[i].por;
    }
    if (lista.length === 0)
        return "";
    return JSON.stringify(lista);
}

function code(json, css) {
    console.log(json);
    var model = compile2Json(json);
    console.log(model);
    var html = "<!DOCTYPE html><html><head><meta name='viewport' content='initial-scale=1.0'>\n<style>\n"+css+"</style></head><body>\n" + doLayouth(JSON.parse(model)) + "</body></html>";
    console.log(html);
    document.getElementById('external').src = "data:text/html;charset=utf-8," + escape(html);

}



function applyChange(json, css) {
    console.log(json);
    var model = compile2Json(json);
    console.log(model);
    var html = "<!DOCTYPE html><html><head><meta name='viewport' content='initial-scale=1.0'>\n<style>\n"+css+"</style></head><body>\n" + doLayouth(JSON.parse(model)) + "</body></html>";
    console.log(html);
    document.getElementById('external').src = "data:text/html;charset=utf-8," + escape(html);

}
