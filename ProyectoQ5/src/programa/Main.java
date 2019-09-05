package programa;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import conector.ConectorODB;

public class Main {

	public static void main(String[] args) {
		
		// Crear conector
		ConectorODB conector = new ConectorODB();
		conector.setUrlDB("remote:localhost/");
		conector.setNameDB("GrafoHospitales");
		conector.setUser("invitado");
		conector.setPass("1234");
		
		// Conectar a la BD
		ODatabaseSession db = conector.connectBD();
		
		
		// Parametros de las consultas
		String idUsuario = "3"; 
		String patogeno = "Varicela"; 
		String fechaInic = "2019-03-13 00:00:00"; 
		String fechaFin = "2019-03-23 00:00:00";
		
		// Consulta Q2
		OResultSet resultadoQ2 = Consultor.q2(db, idUsuario, patogeno, fechaInic, fechaFin);
		
		// Listar consulta
		//Consultor.listarResultado(resultadoQ2, "Q2");
		
		// Consulta Q5
		OVertex resultadoQ5 = Consultor.q5(resultadoQ2, patogeno, fechaInic, fechaFin);
		
		// Mostrar Paciente Raiz
		System.out.println("Paciente Raíz:\n" + Consultor.writePropiedadesNodo(resultadoQ5));
		
		// Cerrar resultado de Q2
		Consultor.cerrarResultado(resultadoQ2);
		
		// Desconectar de la BD
		conector.disconnectDB();
		
	}

}
