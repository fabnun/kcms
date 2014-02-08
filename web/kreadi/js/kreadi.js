var server = "/kreadi/set";

function keyNumberFilter(evt) {
    var code = evt.keyCode;
    if ((code >= 37 && code <= 40) || (code >= 16 && code <= 18) || code === 8 || code === 13 || code === 46 || code === 45 || code === 189 || code === 190 || (code >= 33 && code <= 36) || code === 110)
        return true;
    if ((code >= 48 && code <= 57) || code === 189 || code === 190 || code === 69)
        return true;
    evt.preventDefault();
    return false;
}

/**Inicia la edicion de un nodo
 * @param element
 * @param col
 * @param row
 * @param value
 * @param type*/
function initEdit(element, col, row, value, type) {
    if (currentEdit !== element) {
        currentEdit = element;
        if (type === "String" || type === "Id") {
            var html = "<input type='text' value='" + value + "' onkeydown='endEdit(this, " + col + ", " + row + ", event, \"" + value + "\")' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + value + "\")'>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Number") {
            var html = "<input type='text' value='" + value + "' onkeydown='if (keyNumberFilter(event)) endEdit(this, " + col + ", " + row + ", event, \"" + value + "\")' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + value + "\")'>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Select") {
            var html = "<select value='" + value + "' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + value + "\")'>";
            var types = data.columns[col].rules.split(",");
            for (var i = 0; i < types.length; i++) {
                html = html + "<option" + (value === types[i] ? " selected" : "") + ">" + types[i] + "</option>";
            }
            html = html + "</select>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Boolean") {

        } else if (type === "File") {
            var html = '<input style="display:none" type="file" id="files" onchange="fileSelect(this, ' + col + ', ' + row + ')" name="files[]" />';
            element.innerHTML = element.innerHTML + html;
            element.childNodes[element.childNodes.length - 1].click();
            currentEdit = null;
        }
    }
}

//Prototipo de funcion startWith en un String
if (typeof String.prototype.startsWith !== 'function') {
    String.prototype.startsWith = function(str) {
        return this.indexOf(str) === 0;
    };
}

function setWait(msg) {
    var wd = document.getElementById("waitDiv");
    if (msg) {
        wd.style.display = "block";
        document.getElementById("waitMsg").innerHTML = msg;
    } else
        wd.style.display = "none";
}

function fileSelect(elem, col, row) {//Upload de archivos y respaldo
    if (col !== undefined && col !== null) {
        var f = elem.files[0];
        setWait("Upload " + f.name);
        ajax(server, {command: 'upload', id: data.id, row: row, col: col, name: f.name, size: f.size, data: f},
        function(resp) {
            if (resp.startsWith("Error:")) {
            } else {
                resp = JSON.parse(resp);
                data.columns[col].data[row] = resp;
                buildTable();
            }
            setWait();
        });
    } else {
        setWait("Restaurando Web 0%");
        var f = elem.files[0];//Archivo zip
        // use a BlobReader to read the zip from a Blob object
        zip.createReader(new zip.BlobReader(f), function(reader) {

            // get all entries from the zip
            reader.getEntries(function(entries) {
                sendSerializable(entries, true, entries.length);
            });
        }, function(error) {
            // onerror callback
        });

//        ajax(server, {command: 'restore', data: f},
//        function() {
//            currentEdit = undefined;
//            ajax(server, {command: "getData", id: data.id}, function(resp) {
//                eval("data = " + resp);
//                buildTable();
//            });
//        }, document.getElementById("restoreAll"));
    }
}

function sendSerializable(entries, first, size) {
    if (first) {
        ajax(server, {command: "serialdelete"}, function(resp) {
            sendSerializable(entries, false, size);
        });
    } else {
        if (size > 0)
            if (entries.length > 0) {
                entries[0].getData(new zip.BlobWriter(), function(text) {
                    ajax(server, {command: 'serial', id: entries[0].filename, data: text},
                    function() {
                        setWait("Restaurando Web " + Math.round(100 * (size - entries.length) / size) + "%");
                        entries.splice(0, 1);
                        if (entries.length > 0)
                            sendSerializable(entries, false, size);
                        else {
                            ajax(server, {command: "getData", id: data.id}, function(resp) {
                                currentEdit = undefined;
                                eval("data = " + resp);
                                buildTable();
                                setWait();
                            });
                        }
                    });
                });
            }
    }
}

String.prototype.endsWith = function(s) {
    return this.length >= s.length && this.substr(this.length - s.length) === s;
};

function getFileUploadedCode(col, row, key, name, size, num, admin, type) {
    var lname = (name ? name.toLowerCase() : "");
    var isImage = lname.endsWith('.jpg') || lname.endsWith('.jpeg') || lname.endsWith('.png') || lname.endsWith('.gif');
    var isText = lname.endsWith('.html') || lname.endsWith('.txt') || lname.endsWith('.css')
            || lname.endsWith('.js') || lname.endsWith('.jsp') || lname.indexOf('.') === 0 || lname.endsWith('.json');
    var html = "<img title='Upload' src='css/upload.png' style='cursor:pointer;top:5px;position:relative;' onclick='initEdit(this.parentNode, " + col + "," + row + ", \"\",\"File\")'>";
    if (admin && isText)
        html = html + "<img title='Edit Script' src='css/script.png' style='margin:0px 4px;cursor:pointer;top:5px;position:relative;' onclick='setScript(" + row + "," + col + ")'>";
    html = html + "<img title='Rename' src='css/edit.png' style='margin:0px 4px;cursor:pointer;top:5px;position:relative;' onclick='rename(" + row + "," + col + ")'>";
    if (key) {
        html = html + "<a class='tooltip'> <img src='css/see.png' style='cursor:pointer;top:5px;position:relative;'> " +
                "<div onclick='return false'><span style='white-space: nowrap'> <span style='color:white'> URL ESTATICA:</span> /" + data.id + "/" + name + (num > 1 ? "?n=" + num : "") + "</span><br>" +
                " <span style='white-space: nowrap'> <span style='color:white'> URL DINAMICA :</span> " + server + "?id=" + key + "&name=" + name + "</span><br>";
        if (isImage) {
            var img = new Image();
            img.src = server + "?id=" + key + "&name=" + name;
            img.onload = function() {
                var spanDim = document.getElementById("img" + row + "." + col);
                if (spanDim)
                    spanDim.innerHTML = " <span style='color:white'> DIMENSIONES : </span> " + this.width + ":" + this.height;
            };
            html = html + " <span id='img" + row + "." + col + "' style='white-space: nowrap'></span><br><br>";
            html = html + "<img src=\"" + server + "?id=" + key + "&name=" + name + "\">";
        }

        html = html + "</div></a>";

        html = html + " &nbsp; <a title='Download' style='position:relative;top:-2px;text-decoration:none' href='" + server + "?id=" + key + "&name=" + name + "&download=" + size + "'>" +
                name + (admin ? (" [" + type + "]") : "") + " </a> &nbsp; <span style='color:white;display: inline-block;text-align:right;top:-2px;position:relative'>(" + Math.round(size / 1024) + " kb)</span>";
    }
    return html;
}

var currentEdit = undefined;

/**Realiza un request ajax
 * @param url : server service url
 * @param json : json element request
 * @param element : element that shows the progress bar
 * @param func : function that process the response with params (resp, json)*/
function ajax(url, json, func, element) {
    var ajax = false;
    try {
        ajax = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
        try {
            ajax = new ActiveXObject("Microsoft.XMLHTTP");
        } catch (E) {
            ajax = false;
        }
    }
    if (!ajax && typeof XMLHttpRequest !== 'undefined') {
        ajax = new XMLHttpRequest();
    }
    if (func)
        ajax.onreadystatechange = function() {
            if (ajax.readyState === 4 && ajax.status === 200) {
                //console.log(JSON.stringify(json) + " -> " + ajax.responseText);
                func.call(this, ajax.responseText, json);
            }
        };
    ajax.open("POST", url, true);

//    if (element) {
//        ajax.element = element;
//        //ajax.upload.addEventListener("progress", progressHandler, false);
//        ajax.upload.addEventListener("progress",
//                (function(ele) {
//                    return function(event) {
//                        progressHandler(event, ele);
//                    };
//                }(element)), false);
//    }

    var fd = new FormData();

    if (!json.data)
        json.data = "";

    var isMultipart = true;//json.data;

    if (isMultipart)
    {
        for (var item in json)
            fd.append(item, json[item]);
    } else
    {
        var sb = [];
        for (var item in json)
            sb.push(item + "=" + encodeURIComponent(json[item]));
    }
    ajax.setRequestHeader("Content-type", isMultipart ? "multipart/mixed; charset=UTF-8" : "application/x-www-form-urlencoded; charset=UTF-8");
    if (isMultipart)
        ajax.send(fd);
    else
        ajax.send(sb.join("&"));
}

function progressHandler(event, element) {
    if (event.loaded !== event.total) {
        var text = Math.round(100 * event.loaded / event.total) + "%";
        element.innerHTML = text;
    }
}

function getSelRows() {
    var inputs = document.getElementsByClassName("checkInput");
    var selected = [];
    for (var i = 0; i < inputs.length; i++) {
        if (inputs[i].id.substring(0, 3) === "sel" && inputs[i].checked)
            selected.push(i);
    }
    return selected;
}


function delRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        if (sel.length > 0) {
            if (confirm("Desea eliminar " + sel.length + " registros?")) {
                ajax(server, {command: "delrow", id: data.id, rows: sel.join(",")},
                function(resp, json) {
                    if (resp === "") {
                        var sel = json.rows.split(",");
                        for (var i = 0; i < data.columns.length; i++) {
                            for (var j = sel.length - 1; j >= 0; j--) {
                                data.columns[i].data.splice(sel[j], 1);
                            }
                        }
                        buildTable();
                    } else
                        alert(resp);
                });
            }
        }
    }
}

function addRow() {
    if (!currentEdit) {
        ajax(server, {command: "addrow", id: data.id},
        function(resp) {
            if (resp === "") {
                for (var i = 0; i < data.columns.length; i++) {
                    data.columns[i].data.push("");
                }
                buildTable();
            } else
                alert(resp);
        });
    }
}

function rename(row, col) {
    if (!currentEdit) {
        var oldname = data.columns[col].data[row].name;
        var newname = prompt('Rename', oldname);
        newname = newname ? newname.replace(/^\s+|\s+$/g, '') : null;
        if (newname && oldname !== newname) {
            ajax(server, {command: "rename", id: data.id, row: row, col: col, name: newname},
            function(resp) {
                if (resp === "") {
                    data.columns[col].data[row].name = newname;
                    buildTable();
                } else
                    alert(resp);
            });
        }
    }
}


function setSel(add, indexes) {
    for (var i = 0; i < indexes.length; i++) {
        var id = "sel" + (parseInt(indexes[i]) + add);
        document.getElementById(id).checked = true;
    }
}

function downRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        if (sel.length > 0 && data.columns && data.columns.length > 0 && sel[sel.length - 1] < data.columns[0].data.length - 1) {
            var button = document.getElementById("downRowButton");
            button.disabled = true;
            ajax(server, {command: "downrow", id: data.id, rows: sel.join(",")},
            function(resp, json) {
                var sel = json.rows.split(",");
                for (var j = sel.length - 1; j >= 0; j--) {
                    var index = parseInt(sel[j]);
                    for (var i = 0; i < data.columns.length; i++) {
                        var val0 = data.columns[i].data[index];
                        var val1 = data.columns[i].data[index + 1];
                        data.columns[i].data[index] = val1;
                        data.columns[i].data[index + 1] = val0;
                    }
                }
                buildTable();
                setSel(1, sel);
                if (resp === "") {
                } else
                    alert(resp);
            });
        }
    }
}

function upRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        if (sel.length > 0 && data.columns && data.columns.length > 0 && sel[0] > 0) {
            var button = document.getElementById("upRowButton");
            button.disabled = true;
            ajax(server, {command: "uprow", id: data.id, rows: sel.join(",")},
            function(resp, json) {
                var sel = json.rows.split(",");
                for (var j = 0; j < sel.length; j++) {
                    var index = parseInt(sel[j]);
                    for (var i = 0; i < data.columns.length; i++) {
                        var val0 = data.columns[i].data[index];
                        var val1 = data.columns[i].data[index - 1];
                        data.columns[i].data[index] = val1;
                        data.columns[i].data[index - 1] = val0;
                    }
                }
                buildTable();
                setSel(-1, sel);
                if (resp === "") {
                } else
                    alert(resp);
            });
        }
    }
}

function restore(element) {
    if (!currentEdit) {
        currentEdit = element;
        var html = '<input style="display:none" type="file" id="files" onchange="fileSelect(this)" name="files[]" />';
        element.innerHTML = element.innerHTML + html;
        element.childNodes[element.childNodes.length - 1].click();
    }
}

function setBoolean(element, row, col) {
    var value = element.checked;
    element.disabled = true;
    ajax(server, {command: "setTableVal", id: data.id, col: col, row: row, value: value}, function(resp) {
        element.disabled = false;
    });
}

function saveHtml() {
    setWait("Saving Html...");
    var value = document.getElementById("tinyeditor").value;
    ajax(server, {command: "setTableVal", id: data.id, col: _editCol, row: _editRow, value: value},
    function(resp, json) {
        data.columns[json.col].data[json.row] = JSON.parse(resp);
        document.getElementsByTagName("body")[0].style.overflow = "auto";
        setWait();
    });
}

function showScript(visible, row, col) {
    document.getElementById('scriptDiv').style.display = visible ? 'block' : 'none';
    editor.setOption("fullScreen", visible);
    if (visible) {
        _editCol = col;
        _editRow = row;
    }
    buildTable();
}

function setScript(row, col) {
    showScript(true, row, col);
    var val = data.columns[col].data[row];
    document.getElementById("scriptname").value = "";
    editor.getDoc().setValue("");
    if (val) {
        document.getElementById("scriptname").value = val.name;
        ajax(server, {command: "getText", id: data.id, col: col, row: row}, function(resp) {
            editor.getDoc().setValue(resp);
        });
    }
}

function saveScript(name) {
    setWait("Saving Script...");
    var value = editor.getDoc().getValue();
    ajax(server, {command: "setTableVal", id: data.id, col: _editCol, row: _editRow, value: value, name: name, type: "Script"},
    function(resp, json) {
        data.columns[json.col].data[json.row] = JSON.parse(resp);
        setWait();
    });
}

function setHtml(row, col) {
    _editCol = col;
    _editRow = row;
    ajax(server, {command: "getText", id: data.id, col: _editCol, row: _editRow}, function(resp) {

        document.getElementsByTagName("body")[0].style.overflow = "hidden";
        document.getElementById('html').style.display = "block";
        document.getElementById('html').innerHTML = "<textarea id='tinyeditor' style='width:100%;resize: none;'></textarea>" +
                "<img src='css/cancel.png' style='position:absolute;right:12px;top:9px;cursor:pointer' onclick='html.style.display=\"none\"' title='Cancel'>";
        document.getElementById('tinyeditor').value = resp;
        CKEDITOR.replace('tinyeditor');
    });

}

noBuild = false;
/**Construye una tabla a partir de un json
 * @param {Numbrer} colIndex indice de la columna seleccionada*/
function buildTable(colIndex) {
    if (!noBuild) {
        noBuild = true;
        element = document.getElementById("data");
        var html = [];
        html.push("<table class='table'");
        if (!data.columns || (data.columns && data.columns.length===0) || (data.columns && data.columns.length>0 && data.columns[0].data.length === 0)) {
            html.push("style='padding-right:30px'");
        }
        html.push("><tr class='tableHeader'>",
                "<td colspan=", data.columns.length, " id='tituloTabla'>", data.name);

        var cols = data.columns.length;

        if (cols > 0 && (superAdmin || data.allowAdd)) {
            html.push("<button onclick='delRow()' style='width:26px;padding:0;float:right;margin-right:10px'><img src='css/del.png'></button>",
                    "<button onclick='addRow()' style='width:26px;padding:0;float:right;margin-right:10px'><img src='css/add.png'></button>");
            html.push("<button id='downRowButton' onclick='downRow()' style='width:26px;padding:0;float:right;margin-right:10px'><img src='css/down.png'></button>",
                    "<button id='upRowButton' onclick='upRow()' style='width:26px;padding:0;float:right;margin-right:10px'><img src='css/up.png'></button>");
        }

        html.push("</td></tr>");
        if (cols > 0) {
            html.push("<tr class='tableHeader'>");

            for (var i = 0; i < cols; i++) {
                var columna = data.columns[i];
                html.push("<td", columna.width > 0 ? " width='" + columna.width + "'" : "");
                html.push(">", columna.name);
//        if (superAdmin || data.allowAdd) {
//            html.push("<button style='width:26px;padding:0;float:left'><img src='css/up.png'></button>",
//                    "<button style='width:26px;padding:0;float:left;margin-right:6px'><img src='css/down.png'></button>");
//        }
                html.push("</td>");
            }
            html.push("</tr>");
        }
        if (data.columns && data.columns.length > 0) {
            var rows = data.columns[0].data.length;
            for (var i = 0; i < rows; i++) {
                html.push("<tr class='tableData'>");
                for (var j = 0; j < cols; j++) {
                    var columna = data.columns[j];
                    var value = columna.data[i];
                    html.push("<td");
                    if (columna.editable && (columna.type === 'Number' || columna.type === 'Select' || columna.type === 'String' || columna.type === 'Id')) {
                        html.push(" class='editable'");
                        if (columna.type !== "File") {
                            html.push(" onclick=\"initEdit(this, ", j, ",", i);
                            html.push(", '", value);
                            html.push("','", columna.type, "')\"");
                        }
                    }
                    html.push(">");
                    if (columna.type === "File") {
                        if (value) {
                            var count = 1;
                            for (var k = 0; k < i; k++) {
                                if (columna.data[k] && columna.data[k].name === value.name)
                                    count++;
                            }
                            html.push(getFileUploadedCode(j, i, value.key, value.name, value.size, count, true, value.type));

                        } else
                            html.push(getFileUploadedCode(j, i, null, null, null));
                    } else if (columna.type === "Boolean") {
                        html.push("<input type='checkbox' " + (value ? "checked" : "") + " style='margin-left:10px;width:12px' onchange='setBoolean(this," + i + "," + j + ")'>");
                    } else if (columna.type === "Html") {
                        html.push("<button style='width:90px' onclick='setHtml(" + i + "," + j + ")'> Edit Html </button>");
                    } else if (columna.type === "Script") {
                        html.push("<button style='width:90px' onclick='setScript(" + i + "," + j + ")'> Edit Script </button>");
                    } else
                        html.push(value);
                    html.push("</td>");
                }
                if (superAdmin || data.allowAdd) {
                    html.push("<td style='width:20px'><input id='sel" + i + "' type='checkbox' class='checkInput'></td>");
                }
                html.push("</tr>");
            }
        }
        if (superAdmin) {
            html.push("<tr class='tableHeader'><td onclick='toogleProps()' id='propertiesSwitch' colspan=", data.columns.length, ">");
            html.push("&darr;Show Properties &darr;");



            html.push("</td></tr><tr class='tableHeader prop'>",
                    "<td colspan=", data.columns.length, " style='text-align:left'><span style='color:yellow'>Table Properties</span><br><br>",
                    " Table ID ", "<input type='text' onchange='changeTableVal(this, \"" + data.id + "\")' id='id' class='shortInput' value='" + data.id + "'>",
                    " &nbsp; Name <input type='text' onchange='changeTableVal(this, \"" + data.name + "\")' id='name' class='midInput' value='" + data.name + "'>",
                    " &nbsp; Users <input type='text' onchange='changeTableVal(this, \"" + data.admins + "\")' id='admins' class='midInput' value='" + data.admins + "'>",
                    " &nbsp; <input type='checkbox' onchange='changeTableVal(this, \"" + data.allowAdd + "\")' id='allowAdd' class='checkInput'" + (data.allowAdd ? " checked " : "") + ">&nbsp; Add/Remove/Sort",
                    " <button style='float:right;margin-top:-6px' onclick='delTable()'>Table Delete</button><br><br></td></tr>", "<tr class='tableHeader prop'>",
                    "<td colspan=", data.columns.length, " style='text-align:left'><span style='color:yellow'>Column Properties</span><br><br>", "<select onChange='changeColIndex()' id='colIdx'>");
            if (!data.columns) {
                data.columns = [];
            }
            for (var i = 0; i < data.columns.length; i++) {
                html.push("<option value='" + i + "'>" + data.columns[i].name + "</option>");
            }
            html.push("<option value='-1'> - NEW - </option>");
            html.push("</select> &nbsp; Column Name <input id='colname' onchange='changeColVal(this, \"" + "\")' type='text' class='shortInput'>",
                    " &nbsp; Width <select id='colwidth' onchange='changeColVal(this, \"" + "\")'><option>30</option><option>50</option><option>100</option><option>200</option><option>400</option><option>0</option></select>",
                    " &nbsp; Type <select id='coltype' onchange='changeColVal(this, \"" + "\")'>",
                    "<option>String</option>",
                    "<option>Number</option>",
                    "<option>Boolean</option>",
                    "<option>Select</option>",
                    "<option>Script</option>",
                    "<option>Html</option>",
                    "<option>File</option>",
                    "<option>Id</option>",
                    "<option>SubTable</option>",
                    "</select>",
                    " &nbsp; <input id='coleditable' type='checkbox' onchange='changeColVal(this, \"" + "\")' class='checkInput'> Editable",
                    " &nbsp; Rules <input id='colrules' onchange='changeColVal(this, \"" + "\")' type='text' class='midInput'> ",
                    " <button id='colnew' style='float:right;margin-top:-4px' onclick='newCol()'>Column Create</button>",
                    " <button id='coldel' style='float:right;margin-top:-4px' onclick='delCol()'>Column Delete</button>",
                    " <button id='colright' style='float:right;margin-top:-4px' onclick='rightCol()'>Column to Right</button>",
                    " <button id='colleft' style='float:right;margin-top:-4px' onclick='leftCol()'>Column to Left</button>",
                    "<br><br></td></tr>");

            html.push("<tr class='tableHeader prop'><td colspan=", data.columns.length, " style='text-align:left'><span style='color:yellow'>Subtables</span><br>");
            html.push("<br>Id <input type='text' id='newId' class='shortInput' value=''>",
                    " &nbsp; Name <input type='text' id='newName' class='midInput' value=''> ",
                    "<button onclick='newTable()'>New Table</button><br><br>");
            html.push("</td></tr></table>");

        } else {
            html.push("</table>");
        }

        html.push("<br>");
        for (var subtable in data.subTableMap) {
            var loContiene = subtables.contains(subtable);
            if (superAdmin || loContiene) {
                html.push("<button onclick='newTab(\"admin.jsp?id=" + subtable + "\")' style='background:#dfa'> Editar " + data.subTableMap[subtable] + (superAdmin ? " (" + subtable + ")" : "") + "</button>");
            }
        }
        if (superAdmin && data.id === 'ROOT') {
            html.push("<br><br><button onclick='restore(this)' style='background:#88ff88;' id='restoreAll'> Restaurar Web </button>");
            html.push("<button style='background:#ffff88;'> <a style='text-decoration:none;color:black' target='_blank' href='" + server + "?backup'> Respaldar Web </a> </button>");
        }
        element.innerHTML = html.join("");

        if (document.getElementById("colIdx"))
            if (colIndex)
                changeColIndex(colIndex);
            else
                changeColIndex();
        noBuild = false;
        toogleProps(true);
    }
}

Array.prototype.contains = function(obj) {
    var i = this.length;
    while (i--) {
        if (this[i] === obj) {
            return true;
        }
    }
    return false;
}

var tooglePropsValue = false;
function toogleProps(toogle) {
    if (!toogle)
        tooglePropsValue = !tooglePropsValue;
    if (superAdmin) {
        var elements = document.getElementsByClassName('prop');
        for (var i = 0; i < elements.length; i++) {
            elements[i].style.display = tooglePropsValue ? "table-row" : "none";
        }
        document.getElementById("propertiesSwitch").innerHTML = tooglePropsValue ? "&uarr; Hide Properties &uarr;" : "&darr; Show Properties &darr;";
    }
}

function rightCol() {
    var idx = document.getElementById("colIdx").value;
    if (data.columns.length > 1)
        ajax(server, {command: "rightCol", id: data.id, idx: idx}, function(resp, json) {
            if (resp === "") {
                var idx = parseInt(json.idx);
                var idx0 = (idx + 1) % data.columns.length;
                var col = data.columns[idx];
                var col0 = data.columns[idx0];
                data.columns[idx] = col0;
                data.columns[idx0] = col;
                buildTable();
                changeColIndex(idx0);
            } else
                alert(resp);
        });
}

function leftCol() {
    if (data.columns.length > 1)
        ajax(server, {command: "leftCol", id: data.id, idx: document.getElementById("colIdx").value}, function(resp, json) {
            if (resp === "") {
                var idx = parseInt(json.idx);
                var idx0 = idx - 1;
                if (idx0 < 0) {
                    idx0 = data.columns.length - 1;
                }
                var col = data.columns[idx];
                var col0 = data.columns[idx0];
                data.columns[idx] = col0;
                data.columns[idx0] = col;
                buildTable();
                changeColIndex(idx0);
            } else
                alert(resp);
        });
}


function delCol() {
    if (confirm("Delete this column?"))
        ajax(server, {command: "delCol", id: data.id, idx: document.getElementById("colIdx").value}, function(resp, json) {
            if (resp === "") {
                data.columns.splice(json.idx, 1);
                buildTable();
            } else
                alert(resp);
        });
}

function newCol() {
    ajax(server, {command: "newCol", id: data.id,
        name: document.getElementById("colname").value,
        type: document.getElementById("coltype").value,
        width: document.getElementById("colwidth").value,
        editable: document.getElementById("coleditable").checked,
        rules: document.getElementById("colrules").value
    }, function(resp, json) {
        if (resp === "") {
            var size = 0;
            var jsonCol = {name: json.name, type: json.type, width: json.width, editable: json.editable, rules: json.rules, data: []};
            if (data.columns.length > 0) {
                size = data.columns[0].data.length;
                for (var i = 0; i < size; i++) {
                    jsonCol.data.push(null);
                }
            }
            data.columns.push(jsonCol);
            buildTable();
            changeColIndex(data.columns.length - 1);
        } else
            alert(resp);
    });
}

function changeColVal(elem) {
    var colIdx = document.getElementById("colIdx").value;
    if (colIdx > -1) {
        var param = elem.id;
        var value = elem.type === "checkbox" ? elem.checked : elem.value;
        ajax(server, {command: "setCol", id: data.id, idx: colIdx, param: param, value: value},
        function(resp, json) {
            if (resp === "") {
                var param = json.param.substr(3);
                data.columns[json.idx][param] = json.value;
                if (json.param === "coltype") {
                    ajax(server, {command: "getData", id: data.id, idx: json.idx}, function(resp) {
                        eval("data = " + resp);
                        buildTable(json.idx);
                    });
                } else
                    buildTable(json.idx);
            } else {
                alert(resp);
                buildTable(json.idx);
            }
        });
    }
}

function changeColIndex(idx) {
    if (idx)
        document.getElementById("colIdx").value = idx;
    var colIdx = document.getElementById("colIdx").value;
    var colNameElem = document.getElementById("colname");
    var colWidthElem = document.getElementById("colwidth");
    var colType = document.getElementById("coltype");
    var colEditable = document.getElementById("coleditable");
    var colRulesElem = document.getElementById("colrules");

    var colnewElem = document.getElementById("colnew");
    var coldelElem = document.getElementById("coldel");
    var colrightElem = document.getElementById("colright");
    var colleftElem = document.getElementById("colleft");

    if (colIdx >= 0) {
        colNameElem.value = data.columns[colIdx].name;
        colRulesElem.value = data.columns[colIdx].rules;
        colWidthElem.value = data.columns[colIdx].width;
        colType.value = data.columns[colIdx].type;
        colEditable.checked = data.columns[colIdx].editable;

        colnewElem.style.display = "none";
        coldelElem.style.display = "inline-block";
        colrightElem.style.display = "inline-block";
        colleftElem.style.display = "inline-block";
    } else {
        colNameElem.value = "";
        colWidthElem.value = "0";
        colType.value = "String";
        colRulesElem.value = "";
        colEditable.checked = true;

        colnewElem.style.display = "inline-block";
        coldelElem.style.display = "none";
        colrightElem.style.display = "none";
        colleftElem.style.display = "none";
    }
}

function newTab(url) {
    var win = window.open(url, '_blank');
    win.focus();
}

function newTable() {
    var idElem = document.getElementById("newId");
    var nameElem = document.getElementById("newName");
    ajax(server, {command: "newTable", parent: data.id, key: idElem.value, value: nameElem.value},
    function(resp, json) {
        if (resp === "") {
            idElem.value = "";
            nameElem.value = "";
            if (!data.subTableMap)
                data.subTableMap = {};
            data.subTableMap[json.key] = json.value;
            buildTable();
        } else {
            alert(resp);
        }
    });
}

/**Envia las modificaciones de un campo al servidor
 * @param elem
 * @param col
 * @param row
 * @param oldValue
 * @param evt */
function endEdit(elem, col, row, evt, oldValue) {

    if (!elem.disabled && (evt === undefined || evt.keyCode === 13 || evt.keyCode === 27)) {
        if (evt && evt.keyCode === 27) {
            elem.value = oldValue;
            elem.blur();
        } else {
            elem.disabled = true;
            if (elem.value !== oldValue) {
                ajax(server, {command: "setTableVal", id: data.id, col: col, row: row, value: elem.value},
                function(resp, json) {
                    var newValue = resp === "" ? elem.value : oldValue;
                    if (resp !== "") {
                        alert(resp);
                    } else {
                        data.columns[json.col].data[json.row] = newValue;
                    }
                    elem.parentNode.onclick = function() {
                        eval("initEdit(this, " + json.col + "," + json.row + ", '" + newValue + "','" + data.columns[json.col].type + "')");
                    };
                    elem.parentNode.innerHTML = newValue;
                    currentEdit = undefined;
                });
            } else {
                elem.parentNode.innerHTML = oldValue;
                currentEdit = undefined;
            }
        }
    }
}

function delTable() {
    if (confirm("Do delete the table " + data.id + ":" + data.name + "?")) {
        ajax(server, {command: "delTable", id: data.id},
        function(resp) {
            if (resp === "") {
                if (window.opener) {
                    delete window.opener.data.subTableMap[data.id];
                    window.opener.buildTable();
                }
                if (data.id === "ROOT")
                    window.location.href = window.location.href;
                else
                    window.close();
            } else
                alert(resp);
        });
    }
}

function changeTableVal(elem, oldValue) {

    function setParentSubId(newId) {
        if (window.opener) {
            var value = window.opener.data.subTableMap[data.id];
            delete window.opener.data.subTableMap[data.id];
            window.opener.data.subTableMap[newId] = value;
            window.opener.buildTable();
        }
    }

    function setParentSubName(newName) {
        if (window.opener) {
            window.opener.data.subTableMap[data.id] = newName;
            window.opener.buildTable();
        }
    }

    elem.disabled = true;
    var value = (elem.type === "checkbox") ? elem.checked : elem.value;
    ajax(server, {command: "setTable", table: data.id, key: elem.id, value: value},
    function(resp, json) {
        if (resp !== "") {
            alert(resp);
            if (elem.type === "checkbox")
                elem.checked = oldValue;
            else {
                elem.value = oldValue;
            }
        } else {
            if (json.key === "id")
                setParentSubId(json.value);
            if (json.key === "name")
                setParentSubName(json.value);
            data[json.key] = json.value;
            buildTable();
        }
        elem.disabled = false;
    });
}