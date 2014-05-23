kcms
====

Kreadi Content Management System for Google App Engine (Java)

Metodos
----------------------------------------------------------------------------------
            "getTable(String id){"//Obtiene la tabla asociada al id
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


Table
----------------------------------------------------------------------------------

Column	addCol(java.lang.String nombre, java.lang.String tipo)
Agrega una nueva columna al fina

Column	addCol(java.lang.String nombre, java.lang.String tipo, java.lang.String rules)
Agrega una nueva columna al fina

java.util.HashMap<java.lang.String,java.io.Serializable>	getFileMap(int col, java.lang.String filename, int n) 
java.util.HashMap<java.lang.String,java.io.Serializable>	getFileMap(java.lang.String filename) 
java.util.HashMap<java.lang.String,java.io.Serializable>	getFileMap(java.lang.String filename, int n) 

int	getRows()
Obtiene la cantidad de registros de la tabla

java.lang.String	getURL(int row)
Obtiene la url del registro de la primera columna tipo File

java.lang.String	getURL(int col, int row)
Obtiene la url de un registro file de una columna especifica

java.lang.String	getURLParam(int row)
Obtiene la url del registro de la primera columna tipo File

java.lang.String	getURLParam(int col, int row)
Obtiene la url de un registro file de una columna especifica

java.lang.String	subTables(java.lang.String username, Dao dao) 

java.lang.String	toJSON()
Obtiene la representacion json de la tabla

java.io.Serializable	value(int col, int row) 
java.lang.String	value(int col, int row, Dao dao) 

Column
----------------------------------------------------------------------------------
int	getRows() 
void	ordenar() 
void	ordenar(boolean ascendente) 
void	transformColumn(java.lang.String oldValue, java.lang.String value, Dao dao) 
java.lang.String	value(int row, Dao dao) 
java.lang.String	value(int row, Dao dao, javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) 