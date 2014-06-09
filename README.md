kcms
====

Kreadi Content Management System for Google App Engine (Java)

----------------------------------------------------------------------------------

Metodos
----------------------------------------------------------------------------------
getTable(String id){//Obtiene la tabla asociada al id

index(){//Obtiene la tabla asociada al id

log(Object s){//log

setTable(table){//Persiste la tabla

getText(html, max){//Obtiene el texto desde html

include(String url){//Incluye el texto un html o de un script procesado

append(Object s){//Incluye un texto

include(){//Incluye el texto de otro script que es indicado con el metodo content

content(String url){//Indica la url del script contenedor

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

DAO
----------------------------------------------------------------------------------
void	clearAllCache() 
void	clearCache(java.lang.String key) 

void	del(java.lang.Object objeto)
Elimina el objeto

void	delClass(java.lang.Class clase)
Elimina todos los objetos de una clase

boolean	delSerial(java.lang.String id) 
void	delTable(java.lang.String id) 

<T> T	getObject(java.lang.Class<T> clase, long id)
Obtiene un objecto mediante su clase e id

<T> T	getObject(java.lang.Class<T> clase, java.lang.String id)
Obtiene un objecto mediante su clase e id

java.io.Serializable	getSerial(java.lang.String id) 
java.lang.String	getSHA512(java.lang.String pass) 
java.lang.String	getValue(java.lang.String url, javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) 
Table	loadTable(java.lang.String id) 

java.util.List<?>	query(java.lang.Class<?> clase)
Obtiene la lista de objetos de una clase

void	saveTable(Table table) 
void	setSerial(java.lang.String id, java.io.Serializable serial) 

void	store(java.lang.Object objeto)
Persiste un objeto