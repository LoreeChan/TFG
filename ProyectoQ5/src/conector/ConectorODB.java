package conector;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;

public class ConectorODB {

	// ATRIBUTOS
	private String urlDB;
	private String nameDB;
	private String user;
	private String pass;
	
	OrientDB orientDB;
	
	
	
	// CONSTRUCTOR
	public ConectorODB(){}
	
	
	// GET/SET
	public String getUrlDB() {
		return urlDB;
	}
	public void setUrlDB(String urlDB) {
		this.urlDB = urlDB;
	}

	public String getNameDB() {
		return nameDB;
	}
	public void setNameDB(String nameDB) {
		this.nameDB = nameDB;
	}

	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}
	public void setPass(String pass) {
		this.pass = pass;
	}
	
	
	// FUNCIONALIDAD
	
	public ODatabaseSession connectBD() {
		
		// Conectar a BD
		orientDB = new OrientDB(urlDB, OrientDBConfig.defaultConfig());
				
		// Abrir Sesion
		return orientDB.open(nameDB, user, pass);	
	}
	
	public void disconnectDB() {
		orientDB.close();
	}
	
	
	
	
}
