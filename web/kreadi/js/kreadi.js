var server = "/kreadi/set";
var timeout;
var idPreview = null;
var currentEdit = undefined;
var noBuild = false;
var tooglePropsValue = false;

if (typeof String.prototype.startsWith !== 'function') {
    String.prototype.startsWith = function (str) {
        return this.indexOf(str) === 0;
    };
}
if (typeof String.prototype.startsWith !== 'function') {
    String.prototype.startsWith = function (str) {
        return this.indexOf(str) === 0;
    };
}
if (typeof String.prototype.endsWith !== 'function') {
    String.prototype.endsWith = function (s) {
        return this.length >= s.length && this.substr(this.length - s.length) === s;
    };
}
if (typeof Array.prototype.contains !== 'function') {
    Array.prototype.contains = function (obj) {
        var i = this.length;
        while (i--) {
            if (this[i] === obj) {
                return true;
            }
        }
        return false;
    };
}

function keyNumberFilter(evt) {
    var code = evt.keyCode;
    if ((code >= 37 && code <= 40) || (code >= 16 && code <= 18) || code === 8 || code === 13 || code === 46 || code === 45 || code === 189 || code === 190 || (code >= 33 && code <= 36) || code === 110)
        return true;
    if ((code >= 48 && code <= 57) || code === 189 || code === 190 || code === 69)
        return true;
    evt.preventDefault();
    return false;
}

function initEdit(element, col, row, value, type, subId) {
    value = value ? unescape(value) : value;
    if (subId) {
        subId = ",\"" + subId + "\"";
    } else
        subId = "";
    if (currentEdit !== element) {
        currentEdit = element;
        if (type === "String" || type === "Id") {
            var html = "<input type='text' value='" + value + "' onkeydown='endEdit(this, " + col + ", " + row + ", event, \"" + escape(value) + "\"" + subId + " )' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + escape(value) + "\"" + subId + ")'>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Number") {
            var html = "<input type='text' value='" + value + "' onkeydown='if (keyNumberFilter(event)) endEdit(this, " + col + ", " + row + ", event, \"" + escape(value) + "\"" + subId + ")' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + escape(value) + "\"" + subId + ")'>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Select") {
            var html = "<select value='" + value + "' onblur='endEdit(this, " + col + ", " + row + ", undefined, \"" + escape(value) + "\"" + subId + ")'>";
            var types = [];
            if (subId) {
                var val = subId.substring(10);
                var idx = val.indexOf(".");
                var scol = parseInt(val.substring(0, idx));
                var srow = parseInt(val.substring(idx + 1));
                types = data.columns[scol].data[srow].columns[col].rules.split(",");
            } else {
                types = data.columns[col].rules.split(",");
            }
            for (var i = 0; i < types.length; i++) {
                html = html + "<option" + (value === types[i] ? " selected" : "") + ">" + types[i] + "</option>";
            }
            html = html + "</select>";
            element.innerHTML = html;
            element.childNodes[0].focus();
        } else if (type === "Boolean") {

        } else if (type === "File") {
            var html = "<input style=\"display:none\" type=\"file\" id=\"files\" onchange='fileSelect(this, " + col + ', ' + row + (subId ? subId : "") + ')\' name="files[]" />';
            element.innerHTML = element.innerHTML + html;
            element.childNodes[element.childNodes.length - 1].click();
            currentEdit = null;
        }
    }
}

function setWait(msg) {
    var wd = document.getElementById("waitDiv");
    if (msg) {
        wd.style.display = "block";
        document.getElementById("waitMsg").innerHTML = msg;
    } else
        wd.style.display = "none";
}

function templateSelect(elem) {
    setWait("Restaurando Template");
    var f = elem.files[0]; //Archivo zip
    zip.createReader(new zip.BlobReader(f), function (reader) {
        reader.getEntries(function (entries) {
            sendSerializable(entries, null, true, entries.length, true);
            currentEdit = undefined;
        });
    }, function (error) {
        document.getElementById("restoreType").innerHTML = "";
        console.error("ERROR 9847");
    });
}

function fileSelect(elem, col, row, subId) {//Upload de archivos y respaldo
    if (col !== undefined && col !== null) {
        var f = elem.files[0];
        setWait("Upload " + f.name);
        ajax(server, {command: 'upload', id: data.id, row: row, col: col, name: f.name, size: f.size, sid: subId, data: f},
        function (resp) {
            if (resp.startsWith("Error:")) {
            } else {
                resp = JSON.parse(resp);
                if (subId) {
                    var val = subId.substring(8);
                    var idx = val.indexOf(".");
                    var scol = parseInt(val.substring(0, idx));
                    var srow = parseInt(val.substring(idx + 1));
                    data.columns[scol].data[srow].columns[col].data[row] = resp;
                    buildTable(data.columns[scol].data[srow], col, subId);
                } else {
                    data.columns[col].data[row] = resp;
                    buildTable(data);
                }
            }
            setWait();
        });
    } else {
        setWait("Restaurando Web 0%");
        var f = elem.files[0]; //Archivo zip
        // use a BlobReader to read the zip from a Blob object
        zip.createReader(new zip.BlobReader(f), function (reader) {

            // get all entries from the zip
            reader.getEntries(function (entries) {
                sendSerializable(entries, f.name, true, entries.length);
                currentEdit = undefined;
            });
        }, function (error) {
            document.getElementById("restoreType").innerHTML = "";
            console.error("ERROR 8649769");
        });
    }
}

function sendSerializable(entries, name, first, size, isTemplate) {
    if (isTemplate) {
        if (first) {
            var compare = function (a, b) {
                if (a.filename < b.filename)
                    return -1;
                if (a.filename > b.filename)
                    return 1;
                return 0;
            };
            entries.sort(compare);
            ajax(server, {command: "serialdelete"}, function (resp) {//Elimina los datos anteriores
                sendSerializable(entries, name, false, size, true);
            });
        } else {
            if (size > 0)
                var esize = entries.length;

            if (esize > 0) {
                entries[0].getData(new zip.BlobWriter(), function (text) {

                    ajax(server, {command: 'serial2', id: entries[0].filename, size: entries[0].uncompressedSize, data: text},
                    function (resp, json) {
                        console.log(json.id);
                        setWait("Restaurando Template " + json.id);
                        entries.splice(0, 1);
                        if (entries.length > 0)
                            sendSerializable(entries, name, false, size, true);
                        else {
                            window.location = "/admin";
                        }
                    });
                });
            }
        }
    } else {
        console.log(name);
        if (first) {
            if (name.indexOf(".zip") === name.length - 4) {
                ajax(server, {command: "serialdelete"}, function (resp) {//Elimina los datos anteriores
                    sendSerializable(entries, name, false, size);
                });
            } else
                sendSerializable(entries, name, false, size);
        } else {
            if (size > 0)
                var esize = entries.length;
            if (esize > 0) {
                esize = Math.min(2, esize);
                entries[0].getData(new zip.BlobWriter(), function (text) {
                    ajax(server, {command: 'serial', id: entries[0].filename, data: text},
                    function () {
                        setWait("Restaurando Web " + Math.round(100 * (size - entries.length) / size) + "%");
                        entries.splice(0, 1);
                        if (entries.length > 0)
                            sendSerializable(entries, name, false, size);
                        else {
                            window.location = "/admin";
                        }
                    });
                });
            }
        }

    }
}

function agentes(input) {
    ajax(server, {command: "agentes", list: input.value},
    function () {
    });
}

function users(command) {
    if (command === 'show') {
        document.getElementById("users").style.display = 'block';
    } else if (command === 'close') {
        document.getElementById("users").style.display = 'none';
    }
}

function showPreview(row, col, num, key, name, isImage, subId) {
    if (row === undefined) {
        var div = document.getElementById("preview");
        div.innerHTML = "";
        div.style.display = "none";
        document.title = ((data.name !== null && data.name.trim().length > 0) ? data.name : data.id);
        document.getElementById("preview").style.display = "none";
        document.getElementById("htmlBox").style.display = "none";
        document.getElementById("scriptDiv").style.display = "none";
        if (superAdmin || data.allowAdd) {
            document.getElementById('rowButtons').style.display = "inline-block";
        }
    } else {
        var html = "<div style='margin:12px'><span style='color:#FF8'> URL ESTATICA:</span> /" + data.id + "/" + name + (num > 1 ? "?n=" + num : "") +
                " <span style='margin-left:32px'> <span style='color:#FF8'> URL DINAMICA :</span> /kreadi/file" + key + "_" + name.replace(".", "_") + "</span>";
        if (isImage) {
            var img = new Image();
            img.src = server + "?id=" + key + "&name=" + name;
            img.onload = function () {
                var spanDim = document.getElementById("img" + key);
                var image = document.getElementById("image" + key);
                image.style.maxWidth = this.width + "px";
                image.style.display = "inline-block";
                if (spanDim) {
                    spanDim.innerHTML = "<span style='color:#FF8;margin-left:32px'> DIMENSIONES : </span> " + this.width + ":" + this.height;
                    ajax(server, {command: "setSize", key: key, width: this.width, height: this.height}, function (resp) {
                        console.log(resp);
                    });
                }
            };
            html = html + "<span id='img" + key + "' style='white-space: nowrap'></span></div>";
            html = html + "<button style='float:right;margin-top:-36px;margin-right:16px;' onclick='showPreview()'> [ESC] </button>";
            html = html + "<img id='image" + key + "' src=\"" + server + "?id=" + key + "&name=" + name + "\" style='display:none;width:96%;border:1px dotted white;padding:4px'>";
        } else {
            html = html + "<button style='float:right;' onclick='showPreview()'> [ESC] </button>";
        }
        var div = document.getElementById("preview");
        div.innerHTML = html;
        div.style.display = "block";
    }
}

function getFileUploadedCode(col, row, key, name, size, num, admin, type, subId) {
    var lname = (name ? name.toLowerCase() : "");
    var isImage = lname.endsWith('.jpg') || lname.endsWith('.jpeg') || lname.endsWith('.png') || lname.endsWith('.gif');
    var isText = lname.endsWith('.appcache') || lname.endsWith('.php') || lname.endsWith('.html') || lname.endsWith('.txt') || lname.endsWith('.css') || lname.endsWith('.xml') || lname.endsWith('.*') || lname.indexOf('.') === -1
            || lname.endsWith('.js') || lname.endsWith('.jsp') || lname.indexOf('.') === 0 || lname.endsWith('.json'); //initEdit(element, col, row, value, type, subId) {
    var html = "<img title='Upload' src='css/upload.png' style='cursor:pointer;top:5px;position:relative;' onclick='initEdit(this.parentNode, " + col + "," + row + ", \"\", \"File\"" + (subId ? (', "' + subId + '"') : "") + " )'>";
    html = html + "<a title='Download' href='" + server + "?id=" + key + "&name=" + name + "&download=" + size + "'><img title='Download' src='css/download.png' style='cursor:pointer;top:5px;position:relative;'></a>";
    if (admin && isText)
        html = html + "<img title='Edit Script' src='css/script.png' style='margin:0px 4px;cursor:pointer;top:5px;position:relative;' onclick='setScript(" + row + "," + col + ")'>";
    if (!subId)
        html = html + "<img title='Rename' src='css/edit.png' style='margin:0px 4px;cursor:pointer;top:5px;position:relative;' onclick='rename(" + row + "," + col + ")'>";
    if (key) {
        html = html + "<img title='Vista Previa' src='css/see.png'  onmouseout='preview(null)' onmouseover=\"preview('" + key + "', '" + name + "',  this)\" style='cursor:pointer;top:5px;position:relative;margin-left:4px'  onclick='showPreview(" + row + "," + col + "," + num + ",\"" + key + "\",\"" + name + "\", " + isImage + ",\"" + subId + "\")'> ";
        html = html + " &nbsp; <span style='position:relative;top:-2px;text-decoration:none''>" +
                name + (admin ? (" [" + type + "]") : "") + " </span> &nbsp; <span style='color:white;display: inline-block;text-align:right;top:-2px;position:relative'>(" + Math.round(size / 1024) + " kb)</span>";
    }
    return html;
}

function preview(id, name, elem) {
    var prev = document.getElementById("toolpreview");
    if (id === null || (!name.toLowerCase().endsWith(".jpg") && !name.toLowerCase().endsWith(".png") && !name.toLowerCase().endsWith(".gif"))) {
        prev.style.display = "none";
        if (timeout) {
            clearTimeout(timeout);
            timeout = null;
        }
        prev.src = "";
    } else {
        idPreview = id;
        prev.src = "/kreadi/set?id=" + id + "&name=" + name + "&resize=120x120";
        timeout = setTimeout(
                function () {
                    if (id === idPreview && idPreview !== null) {
                        prev.style.left = (elem.offsetLeft + 32) + "px";
                        prev.style.top = (elem.offsetTop - 64) + "px";
                        prev.style.display = "block";
                    }
                }, 500);
    }
}

function ajax(url, json, func) {
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
        ajax.onreadystatechange = function () {
            if (ajax.readyState === 4 && ajax.status === 200) {
                //console.log(JSON.stringify(json) + " -> " + ajax.responseText);
                func.call(this, ajax.responseText, json);
            }
        };
    ajax.open("POST", url, true);
    var fd = new FormData();
    if (!json.data)
        json.data = "";
    var isMultipart = true;
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

function getSelRows(subId) {
    var inputs = document.getElementsByClassName(subId ? ("check_" + subId) : "checkInput");
    var selected = [];
    for (var i = 0; i < inputs.length; i++) {
        if ((inputs[i].id.substring(0, 4) === "_sel" || inputs[i].id.substring(0, 8) === "subTable") && inputs[i].checked)
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
                function (resp, json) {
                    if (resp === "") {
                        var sel = json.rows.split(",");
                        for (var i = 0; i < data.columns.length; i++) {
                            for (var j = sel.length - 1; j >= 0; j--) {
                                data.columns[i].data.splice(sel[j], 1);
                            }
                        }
                        buildTable(data);
                    } else
                        alert(resp);
                });
            }
        }
    }
}

function addRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        ajax(server, {command: "addrow", id: data.id, before: sel.length > 0 ? sel[0] : -1},
        function (resp, json) {
            if (resp === "") {
                console.log("1: " + JSON.stringify(data));
                for (var i = 0; i < data.columns.length; i++) {
                    if (json.before === -1)
                        data.columns[i].data.push("");
                    else
                        data.columns[i].data.splice(json.before, 0, "");
                }
                console.log("2: " + JSON.stringify(data));
                buildTable(data);
                if (sel.length > 0) {
                    for (var i = 0; i < sel.length; i++) {
                        var check = document.getElementById("_sel" + (sel[i] + 1));
                        check.checked = true;
                    }
                }
            } else
                alert(resp);
        });
    }
}

function rename(row, col) {
    if (!currentEdit) {
        var oldname = data.columns[col].data[row].name;
        var newname = prompt('Rename', oldname);
        if (newname) {
            newname = newname ? newname.replace(/^\s+|\s+$/g, '') : "";
            if (oldname !== newname) {
                ajax(server, {command: "rename", id: data.id, row: row, col: col, name: newname},
                function (resp) {
                    if (resp === "OK") {
                        data.columns[col].data[row].name = newname;
                        buildTable(data);
                    }
                    if (resp) {
                        resp = JSON.parse(resp);
                        data.columns[col].data[row] = resp;
                        buildTable(data);
                    } else
                        alert(resp);
                });
            }
        }
    }
}

function setSel(add, indexes) {
    for (var i = 0; i < indexes.length; i++) {
        var id = "_sel" + (parseInt(indexes[i]) + add);
        var check = document.getElementById(id);
        check.checked = true;
    }
}

function downRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        if (sel.length > 0 && data.columns && data.columns.length > 0 && sel[sel.length - 1] < data.columns[0].data.length - 1) {
            var button = document.getElementById("downRowButton");
            button.disabled = true;
            ajax(server, {command: "downrow", id: data.id, rows: sel.join(",")},
            function (resp, json) {
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
                buildTable(data);
                button.disabled = false;
                setSel(1, sel);
                if (resp === "") {
                } else
                    alert(resp);
            });
        }
    }
}

function sortRow() {
    if (!currentEdit) {
        var sel = prompt("Ingrese lista de nombres ordenados separados por coma");
        if (sel.length > 0) {
            var button = document.getElementById("sortRowButton");
            button.disabled = true;
            ajax(server, {command: "sortrow", id: data.id, sort: sel},
            function (resp) {
                if (resp.startsWith("ERROR: ")) {
                    alert(resp);
                } else {
                    window.location.reload();
                }
                button.disabled = false;
            });
        }
    }
}

function restore() {
    if (!currentEdit) {
        var element = document.getElementById("restoreType");
        var html = '<input style="display:none" type="file" id="files" onchange="' + (element.checked ? "templateSelect" : "fileSelect") + '(this)" name="files[]" />';
        element.innerHTML = html;
        element.childNodes[0].click();
        currentEdit = undefined;
    }
}

function setBoolean(element, row, col, subId) {
    var value = element.checked;
    element.disabled = true;
    ajax(server, {command: "setTableVal", id: data.id, col: col, row: row, value: value, subId: subId}, function (resp, json) {
        element.disabled = false;
        data.columns[json.col].data[json.row] = value;
    });
}

function saveHtml() {
    setWait("Saving Html...");
    var value = CKEDITOR.instances.tinyeditor.getData();
    ajax(server, {command: "setTableVal", id: data.id, col: _editCol, row: _editRow, value: value, subId: _subId},
    function (resp, json) {
        data.columns[json.col].data[json.row] = JSON.parse(resp);
        document.getElementsByTagName("body")[0].style.overflow = "auto";
        setWait();
    });
}

function delUser(idx) {
    ajax(server, {command: "delUser", idx: idx},
    function (resp, json) {
        updateUsers(resp);
    });
}

function updateUsers(resp) {
    var html = "<tr><td style='padding:4px'>USUARIO</td><td colspan='2'>ROL</td></tr>";
    var users = resp.split(' ');
    if (users.length > 1)
        for (var i = 0; i < users.length; i = i + 2) {
            html = html + "<tr><td style='padding:4px'><input type='text' value='" + users[i] + "'></td>" +
                    "<td><input type='text' value='" + users[i + 1] + "'></td><td><button style='height:20px;width:20px' onclick='delUser(" + (i / 2) + ")'> - </button></td></tr>";
        }
    html = html + "<tr><td style='padding:4px'><input type='text' id='userName'></td>" +
            "<td width='60'><input type='text' id='userRol'></td><td width='30'><button style='height:20px;width:20px' onclick='addUser()'> + </button></td></tr>";
    document.getElementById('userTable').innerHTML = html;
}

function addUser() {
    var userName = document.getElementById("userName");
    userName = userName ? userName.value : "";
    var userRol = document.getElementById("userRol");
    userRol = userRol ? userRol.value : "";
    ajax(server, {command: "addUser", userName: userName, userRol: userRol},
    function (resp, json) {
        updateUsers(resp);
    });
}

function showScript(visible, row, col) {

    document.getElementById('scriptDiv').style.display = visible ? 'block' : 'none';
    editor.setOption("fullScreen", visible);
    if (visible) {
        document.title = data.columns[col].data[row].name + "[" + ((data.name !== null && data.name.trim().length > 0) ? data.name : data.id) + "]";
        _editCol = col;
        _editRow = row;
    } else {
        document.title = (data.name !== null && data.name.trim().length > 0) ? data.name : data.id;
    }
    buildTable(data);
}

function setScript(row, col) {
    showScript(true, row, col);
    var val = data.columns[col].data[row];
    document.getElementById("scriptname").value = "";
    editor.getDoc().setValue("");
    if (val) {
        document.getElementById("scriptname").value = val.name;
        ajax(server, {command: "getText", id: data.id, col: col, row: row}, function (resp) {
            editor.getDoc().setValue(resp);
            var idxExt = val.name.lastIndexOf(".");
            var ext = idxExt > -1 ? val.name.substring(idxExt + 1).toLowerCase() : null;
            var mode = "application/x-ejs";
            if (ext === "js")
                mode = "javascript";
            else if (ext === "css")
                mode = "css";
            editor.setOption("mode", mode);
            document.getElementById('rowButtons').style.display = "none";
        });
    }
}

function saveScript(name) {
    setWait("Saving Script...");
    var value = editor.getDoc().getValue();
    ajax(server, {command: "setTableVal", id: data.id, col: _editCol, row: _editRow, value: value, name: name, type: "Script"},
    function (resp, json) {
        data.columns[json.col].data[json.row] = JSON.parse(resp);
        buildTable(data);
        setWait();
    });
}

function setHtml(row, col, subId) {
    _editCol = col;
    _editRow = row;
    _subId = subId;
    ajax(server, {command: "getText", id: data.id, col: _editCol, row: _editRow, subId: subId}, function (resp) {

        document.getElementsByTagName("body")[0].style.overflow = "hidden";
        document.getElementById('htmlBox').style.display = "block";
        document.getElementById('html').innerHTML = "<textarea id='tinyeditor' onkeydown='return keyprocess(event);' style='width:100%;resize: none;'></textarea>" +
                "<img src='css/cancel.png' style='position:absolute;right:12px;top:9px;cursor:pointer' onclick='showPreview()' title='Cancel'>";
        document.getElementById('tinyeditor').value = resp;
        var editor = CKEDITOR.replace('tinyeditor');
        CKEDITOR.config.startupOutlineBlocks = true;
        editor.on('key', function (val) {
            if (val.data.keyCode === 27) {
                document.title = ((data.name !== null && data.name.trim().length > 0) ? data.name : data.id);
                document.getElementById("preview").style.display = "none";
                document.getElementById("htmlBox").style.display = "none";
                document.getElementById("scriptDiv").style.display = "none";
                if (superAdmin || data.allowAdd) {
                    document.getElementById('rowButtons').style.display = "inline-block";
                }
            } else
            if (val.data.keyCode === CKEDITOR.CTRL + 83) {
                save();
                return false;
            } else if (val.data.keyCode === CKEDITOR.CTRL + 69) {//CTRL+E
                view();
                return false;
            }
        });
        document.getElementById('rowButtons').style.display = "none";
        // SET THE KEYSTROKE TO SAVE CTRL+S
        //setTimeout('var iframe=document.getElementsByClassName("cke_wysiwyg_frame");alert(iframe);if (iframe) iframe.onkeydown=function(){alert("chapala!!!");};',3000);

    });
}

function over(over, element) {
    var tds = element.children;
    for (var i = 0; i < tds.length; i++) {
        tds[i].style.background = over ? "#554" : "#332";
    }
}

function togleRow2(col, row, button) {
    var subId = "subTable" + col + "." + row;
    var div = document.getElementById(subId);
    var btn_div = document.getElementById("btn_" + subId);
    if (div.style.display === "none") {
        div.style.display = "block";
        btn_div.style.display = "inline-block";
        button.style.background = "#888";
    } else {
        div.style.display = "none";
        btn_div.style.display = "none";
        button.style.background = "#DDD";
    }
}

function upRow() {
    if (!currentEdit) {
        var sel = getSelRows();
        if (sel.length > 0 && data.columns && data.columns.length > 0 && sel[0] > 0) {
            var button = document.getElementById("upRowButton");
            button.disabled = true;
            ajax(server, {command: "uprow", id: data.id, rows: sel.join(",")},
            function (resp, json) {
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
                buildTable(data);
                button.disabled = false;
                setSel(-1, sel);
                if (resp === "") {
                } else
                    alert(resp);
            });
        }
    }
}

function upRow2(col, row) {
    var sel = getSelRows("subTable" + col + "." + row);
    if (sel.length > 0)
        if (sel[0] !== 0) {
            ajax(server, {command: "upRow2", id: data.id, col: col, row: row, rows: sel.join(",")}, function (resp, json) {
                var sel = json.rows.split(",");
                var parent = "_subTable" + json.col + "." + json.row + ".0";
                parent = document.getElementById(parent).parentNode;

                for (var i = 0; i < sel.length; i++) {
                    var row = parseInt(sel[i]);
                    var val0 = parent.childNodes[row];
                    var val1 = parent.childNodes[row - 1];
                    parent.removeChild(val0);
                    parent.insertBefore(val0, val1);
                }
            });
        }
}

function downRow2(col, row) {
    var sel = getSelRows("subTable" + col + "." + row);
    if (sel.length > 0) {
        var parent = "subTable" + col + "." + row + ".0";
        parent = document.getElementById(parent).parentNode;
        if (sel[sel.length - 1] !== parent.childNodes.length - 1)
            ajax(server, {command: "downRow2", id: data.id, col: col, row: row, rows: sel.join(",")}, function (resp, json) {
                var sel = json.rows.split(",");
                var parent = "_subTable" + json.col + "." + json.row + ".0";
                parent = document.getElementById(parent).parentNode;

                for (var i = sel.length - 1; i >= 0; i--) {
                    var row = parseInt(sel[i]);
                    var val0 = parent.childNodes[row];
                    var val1 = parent.childNodes[row + 1];
                    parent.removeChild(val1);
                    parent.insertBefore(val1, val0);
                }
            });
    }
}

function addRow2(col, row) {
    var sel = getSelRows("subTable" + col + "." + row);
    var before = sel.length > 0 ? sel[0] : -1;
    ajax(server, {command: "addRow2", id: data.id, col: col, row: row, before: before}, function (resp) {
        var sd = {};
        eval("sd = " + resp);
        data.columns[col].data[row] = sd;
        var count = document.getElementById("txt_subTable" + col + "." + row);
        if (sd) {
            buildTable(sd, undefined, "subTable" + col + "." + row);
            count.innerHTML = " ( " + data.columns[col].data[row].columns[0].data.length + " ) ";
        } else {
            buildTable(data);
            count.innertHTML = " ( 0 ) ";
        }
        if (sel.length > 0) {
            for (var i = 0; i < sel.length; i++) {
                var check = document.getElementById("subTable" + col + "." + row + "." + (sel[i] + 1));
                check.checked = true;
            }
        }
    });
}

function delRow2(col, row) {
    var sel = getSelRows("subTable" + col + "." + row);
    if (sel.length > 0) {
        if (confirm("Desea eliminar " + sel.length + " registros?")) {
            ajax(server, {command: "delRow2", id: data.id, col: col, row: row, checks: sel.join(",")}, function (resp) {
                var sd = undefined;
                if (resp !== "")
                    eval("sd = " + resp);
                data.columns[col].data[row] = sd;
                var count = document.getElementById("txt_subTable" + col + "." + row);
                if (sd) {
                    buildTable(sd, undefined, "subTable" + col + "." + row);
                    count.innerHTML = " ( " + data.columns[col].data[row].columns[0].data.length + " ) ";
                } else {
                    buildTable(data);
                    count.innertHTML = " ( 0 ) ";
                }
            });
        }
    }
}

function dragStartHandler(e) {
//    e.dataTransfer.effectAllowed = 'copy';
//    e.dataTransfer.setData('id', e.target.id);
//    e.dataTransfer.setData('fid', getParameterByName("id"));
//    e.dataTransfer.setData('html', e.target.innerHTML);
}

function noopHandler(evt) {
    evt.stopPropagation();
    evt.preventDefault();
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function drop(evt) {

//    evt.stopPropagation();
//    evt.preventDefault();
//    if (evt.target.className !== 'tableCell') {
//        return;
//    }
//    var dat = evt.dataTransfer.getData('id');
//    var fid = evt.dataTransfer.getData('fid');
//    var html = evt.dataTransfer.getData('html');
//
//    var fn = function (resp, json) {
//        if (resp === "") {
//            evt.target.style.bborder = "1px solid red";
//
//        } else {
//            evt.target.innerHTML = html;
//            evt.target.style.border = "1px solid green";
//
//        }
//    };
//    ajax(server, {command: "drag", tid: getParameterByName("id"), from: dat, to: this.id, fid: fid}, fn);
}


function buildTable(data, colIndex, subId) {
    if (subId || !noBuild) {
        var subTables = {};
        noBuild = true;
        var idData = subId ? subId : "data";
        var element = document.getElementById(idData);
        var html = [];
        html.push("<table class='table'");
        if (!data.columns || (data.columns && data.columns.length === 0) || (data.columns && data.columns.length > 0 && data.columns[0].data.length === 0)) {
            html.push("style='padding-right:30px'");
        }
        html.push(">");
        var cols = 0;
        try {
            cols = data.columns.length;
        } catch (e) {

        }
        if (!subId) {
            html.push("<tr class='tableHeader'>",
                    "<td colspan=", data.columns.length, " id='tituloTabla'><span style='top:6px;position:relative'>", data.name, "</span>");
            var cols = data.columns.length;
            document.getElementById("rowButtons").style.display = (cols > 0 && (superAdmin || data.allowAdd)) ? "block" : "none";
            html.push("</td></tr>");
            if (cols > 0) {
                html.push("<tr class='tableHeader'>");
                for (var i = 0; i < cols; i++) {
                    var columna = data.columns[i];
                    html.push("<td", columna.width > 0 ? (" width='" + columna.width + "'") : "");
                    html.push(">", columna.name);
                    html.push("</td>");
                }
                html.push("</tr>");
            }
        }
        if (data.columns && data.columns.length > 0) {
            var rows = data.columns[0].data.length;
            for (var i = 0; i < rows; i++) {
                html.push("<tr class='tableData' id='" + (subId ? ("_" + subId + ".") : "sel") + i + "' onmouseover='over(true,this)' onmouseout='over(false,this)'>");
                for (var j = 0; j < cols; j++) {
                    var columna = data.columns[j];
                    var value = columna.data[i];
                    html.push("<td class='tableCell' id='");
                    html.push(idData + "-");
                    html.push(i);
                    html.push(".");
                    html.push(j);
                    html.push("'");
                    if (subId && i === 0 && columna.width > 0) {
                        html.push(" width='" + columna.width + "'");
                    }
                    if (columna.editable && (columna.type === 'Number' || columna.type === 'Select' || columna.type === 'String' || columna.type === 'Id')) {
                        html.push(" class='editable'");
                        if (columna.type !== "File") {
                            html.push(" onclick=\"initEdit(this, ", j, ",", i);
                            html.push(", '", escape(value));
                            html.push("','", columna.type, "'", (subId ? (",'" + subId + "'") : ""), ")\"");
                        }
                    }
                    html.push(">");
                    if (!subId && columna.type === "SubTable") {
                        try {
                            html.push("<div style='float:right'><span  id='txt_subTable" + j + "." + i + "'  style='float:right;display:block;position:relative;margin:6px 8px'>( " + (value ? value.columns[0].data.length : "0") + " )</span> <span id='btn_subTable" + j + "." + i + "' style='display:none;'>");
                            html.push("<button id='upRowButton' title='Mover arriba' onclick='upRow2(" + j + "," + i + ")' style='width:26px;padding:0;margin-right:2px'><img src='css/up.png'></button>");
                            html.push("<button id='downRowButton' title='Mover abajo' onclick='downRow2(" + j + "," + i + ")' style='width:26px;padding:0;margin-right:4px'><img src='css/down.png'></button>");
                            html.push("<button onclick='addRow2(" + j + "," + i + ")' style='padding:0;'><img src='css/add.png'></button>");
                            html.push("<button onclick='delRow2(" + j + "," + i + ")' style='padding:0;'><img src='css/del.png'></button></span>");
                            html.push("<button onclick='togleRow2(" + j + "," + i + ",this)' style='padding:0;margin-left:12px;margin-right:6px'><img src='css/rest.png'></button></div>");
                        } catch (e) {

                        }
                    }
                    if (columna.type === "File") {
                        if (value) {
                            var count = 1;
                            for (var k = 0; k < i; k++) {
                                if (columna.data[k] && columna.data[k].name === value.name)
                                    count++;
                            }
                            html.push(getFileUploadedCode(j, i, value.key, value.name, value.size, count, true, value.type, subId));
                        } else
                            html.push(getFileUploadedCode(j, i, null, null, null, null, null, null, subId));
                    } else if (columna.type === "Boolean") {
                        html.push("<input type='checkbox' " + (value ? "checked" : "") + " style='margin-left:10px;width:12px' onchange='setBoolean(this," + i + "," + j + (subId ? (",\"" + subId + "\"") : "") + ")'>");
                    } else if (columna.type === "Html") {
                        html.push("<button style='width:90px' onclick='setHtml(" + i + "," + j + (subId ? ",\"" + subId + "\"" : "") + ")'> Edit Html </button>");
                    } else if (columna.type === "Script") {
                        html.push("<button style='width:90px' onclick='setScript(" + i + "," + j + ")'> Edit Script </button> " + (value ? value.name : ""));
                    } else if (columna.type === "SubTable") {
                        html.push("<div id='subTable" + j + "." + i + "' style='display:none'></div> ");
                        if (value !== null)
                            subTables["subTable" + j + "." + i] = value;
                    } else
                        html.push(value);
                    html.push("</td>");
                }
                if (subId || superAdmin || data.allowAdd) {
                    html.push("<td style='width:20px;padding-top:16px'><input id='" + (subId ? (subId + ".") : "_sel") + i + "' type='checkbox' class='" + (subId ? ("check_" + subId) : "checkInput") + "'></td>");
                }
                html.push("</tr>");
            }


        }



        if (superAdmin && !subId) {
            html.push("<tr class='tableHeader'><td onclick='toogleProps()' id='propertiesSwitch' colspan=", data.columns.length, ">");
            html.push("&darr;Show Properties &darr;");
            html.push("</td></tr><tr class='tableHeader prop'>",
                    "<td colspan=", data.columns.length, " style='text-align:left'><span style='color:yellow'>Table Properties</span><br><br>",
                    " Table ID ", "<input type='text' onchange='changeTableVal(this, \"" + data.id + "\")' id='id' class='shortInput' value='" + data.id + "'>",
                    " &nbsp; Name <input type='text' onchange='changeTableVal(this, \"" + data.name + "\")' id='name' class='midInput' value='" + data.name + "'>",
                    " &nbsp; Users <input type='text' onchange='changeTableVal(this, \"" + data.admins + "\")' id='admins' class='midInput' value='" + data.admins + "'>",
                    " &nbsp; <input type='checkbox' onchange='changeTableVal(this, \"" + data.allowAdd + "\")' id='allowAdd' class='checkInput'" + (data.allowAdd ? " checked " : "") + ">&nbsp; Add/Remove/Sort",
                    "<button style='float:right;margin-top:-6px' onclick='movTable()'>Table Move</button> <button style='float:right;margin-top:-6px' onclick='delTable()'>Table Delete</button><br><br></td></tr>", "<tr class='tableHeader prop'>",
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
        if (!subId)
            html.push("<br>");


        var keys = [], k, i, len;

        for (k in data.subTableMap) {
            if (data.subTableMap.hasOwnProperty(k)) {
                keys.push(k);
            }
        }

        keys.sort();
        len = keys.length;

        for (i = 0; i < len; i++)
        {
            var subtable = keys[i];

            var loContiene = subtables.contains(subtable);
            if (superAdmin || loContiene) {
                html.push("<button onclick='newTab(\"admin.jsp?id=" + subtable + "\")' style='background:#dfa'> " + data.subTableMap[subtable] + (superAdmin ? " (" + subtable + ")" : "") + "</button>");
            }
        }

        element.innerHTML = html.join("");


        for (var id in subTables) {
            if (subTables[id])
                buildTable(subTables[id], undefined, id);
        }

        if (document.getElementById("colIdx"))
            if (colIndex)
                changeColIndex(colIndex);
            else
                changeColIndex();
        noBuild = false;
        toogleProps(true);
    }

    var cells = document.querySelectorAll('.tableData td');


    [].forEach.call(cells, function (cel) {
        cel.addEventListener('dragenter', noopHandler, false);
        cel.addEventListener('dragexit', noopHandler, false);
        cel.addEventListener('dragover', noopHandler, false);
        cel.addEventListener('drop', drop, false);
        cel.addEventListener('dragstart', dragStartHandler, false);
    });

    var img = document.getElementsByTagName('img');
    var falso = function () {
        return false;
    };
    for (var i = 0; i < img.length; i++) {
        img[i].ondragstart = falso;
        img[i].ondrop = falso;
    }
}

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
        ajax(server, {command: "rightCol", id: data.id, idx: idx}, function (resp, json) {
            if (resp === "") {
                var idx = parseInt(json.idx);
                var idx0 = (idx + 1) % data.columns.length;
                var col = data.columns[idx];
                var col0 = data.columns[idx0];
                data.columns[idx] = col0;
                data.columns[idx0] = col;
                buildTable(data);
                changeColIndex(idx0);
            } else
                alert(resp);
        });
}

function leftCol() {
    if (data.columns.length > 1)
        ajax(server, {command: "leftCol", id: data.id, idx: document.getElementById("colIdx").value}, function (resp, json) {
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
                buildTable(data);
                changeColIndex(idx0);
            } else
                alert(resp);
        });
}

function delCol() {
    if (confirm("Delete this column?"))
        ajax(server, {command: "delCol", id: data.id, idx: document.getElementById("colIdx").value}, function (resp, json) {
            if (resp === "") {
                data.columns.splice(json.idx, 1);
                buildTable(data);
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
    }, function (resp, json) {
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
            buildTable(data);
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
        function (resp, json) {
            if (resp === "") {
                var param = json.param.substr(3);
                data.columns[json.idx][param] = json.value;
                if (json.param === "coltype") {
                    ajax(server, {command: "getData", id: data.id, idx: json.idx}, function (resp) {
                        eval("data = " + resp);
                        buildTable(data, json.idx);
                    });
                } else
                    buildTable(data, json.idx);
            } else {
                alert(resp);
                buildTable(data, json.idx);
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
    function (resp, json) {
        if (resp === "") {
            idElem.value = "";
            nameElem.value = "";
            if (!data.subTableMap)
                data.subTableMap = {};
            data.subTableMap[json.key] = json.value;
            buildTable(data);
        } else {
            alert(resp);
        }
    });
}

function endEdit(elem, col, row, evt, oldValue, subId) {

    if (!elem.disabled && (evt === undefined || evt.keyCode === 13 || evt.keyCode === 27)) {
        if (evt && evt.keyCode === 27) {
            elem.value = oldValue;
            elem.blur();
        } else {
            elem.disabled = true;
            if (elem.value !== oldValue) {
                ajax(server, {command: "setTableVal", id: data.id, col: col, row: row, value: elem.value, subId: subId},
                function (resp, json) {
                    var newValue = resp === "" ? elem.value : oldValue;
                    if (resp !== "") {
                        alert(resp);
                    } else {
                        if (subId) {
                            var val = subId.substring(8);
                            var idx = val.indexOf(".");
                            var scol = parseInt(val.substring(0, idx));
                            var srow = parseInt(val.substring(idx + 1));
                            data.columns[scol].data[srow].columns[json.col].data[json.row] = newValue;

                            elem.parentNode.onclick = function () {
                                var command = "initEdit(this, " + json.col + "," + json.row + ", '" + escape(newValue) + "','" + data.columns[scol].data[srow].columns[json.col].type + "'" + (subId ? (",'" + subId + "'") : "") + ")";
                                eval(command);
                            };

                        } else {
                            data.columns[json.col].data[json.row] = newValue;

                            elem.parentNode.onclick = function () {
                                var command = "initEdit(this, " + json.col + "," + json.row + ", '" + escape(newValue) + "','" + data.columns[json.col].type + "'" + (subId ? (",'" + subId + "'") : "") + ")";
                                eval(command);
                            };
                        }
                    }

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
        function (resp) {
            if (resp === "") {
                if (window.opener) {
                    delete window.opener.data.subTableMap[data.id];
                    window.opener.buildTable(window.opener.data);
                }
                if (data.id === "ROOT")
                    window.location.reload();
                else
                    window.close();
            } else
                alert(resp);
        });
    }
}

function movTable() {
    var id = data.id;
    if (id === "ROOT") {
        alert("No puede mover la carpeta raiz");
    } else {
        var parentId = data.parentId;
        var newParentId = prompt('New Parent ID', parentId);
        if (newParentId) {
            ajax(server, {command: "movTable", id: data.id, parentId: newParentId},
            function (resp) {
                if (resp === "") {
                    if (window.opener) {
                        delete window.opener.data.subTableMap[data.id];
                        window.opener.buildTable(window.opener.data);
                    }
                    window.location.reload();
                } else
                    alert(resp);
            });
        }
    }
}

function changeTableVal(elem, oldValue) {

    function setParentSubId(newId) {
        if (window.opener) {
            var value = window.opener.data.subTableMap[data.id];
            delete window.opener.data.subTableMap[data.id];
            window.opener.data.subTableMap[newId] = value;
            window.opener.buildTable(window.opener.data);
        }
    }

    function setParentSubName(newName) {
        if (window.opener) {
            window.opener.data.subTableMap[data.id] = newName;
            window.opener.buildTable(window.opener.data);
        }
    }

    elem.disabled = true;
    var value = (elem.type === "checkbox") ? elem.checked : elem.value;
    ajax(server, {command: "setTable", table: data.id, key: elem.id, value: value},
    function (resp, json) {
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
            buildTable(data);
        }
        elem.disabled = false;
    });
}