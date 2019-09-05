package programa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

public class Consultor {

	
	////////
	// Q4 //
	////////
	
	public static OResultSet q2(ODatabaseSession db, String idUsuario, String patogeno, String fechaInic, String fechaFin) {		
		
		// Parametros de la consulta
		Map<String, Object> params = new HashMap<>();
		params.put("idUsuario", idUsuario);
		params.put("patogeno", patogeno);
		params.put("fechaInic", fechaInic);
		params.put("fechaFin", fechaFin);
		
		// Texto de la consulta
		String q2 = "SELECT expand(DISTINCT(in('tieneHistoricoDe'))) FROM (SELECT expand(in('realizadoEn')) FROM (TRAVERSE out('realizadoEn'), both('situadoEn', 'alberga') FROM (SELECT FROM (SELECT expand(out('tieneHistoricoDe')) FROM Paciente WHERE id=:idUsuario) WHERE (@class='EventoPuntual' AND descripcion=:patogeno AND out('esDeTipo').@Class='TipoInfeccion' AND ((fecha between :fechaInic and :fechaFin) OR ((first(in('comprende')).inicio between :fechaInic and :fechaFin) OR (first(in('comprende')).fin between :fechaInic and :fechaFin) OR (first(in('comprende')).inicio <= :fechaInic AND (first(in('comprende')).fin IS NULL OR first(in('comprende')).fin >= :fechaFin)))))) WHILE ((@class NOT IN ['Zona', 'Area', 'Bloque', 'Unidad', 'Planta'] OR (@class='Zona' AND $depth IN [4, 5, 6]) OR (@class='Area' AND $depth IN [4, 6])) AND ($depth<=9))) WHERE @class instanceOf 'Aparato') WHERE ((@class instanceOf 'EventoIntervalo') AND ((inicio between :fechaInic and :fechaFin) OR (fin between :fechaInic and :fechaFin) OR (inicio <= :fechaInic AND (fin IS NULL OR fin >= :fechaFin))) AND out('comprende') CONTAINS (@rid IN (SELECT FROM EventoPuntual WHERE (descripcion=:patogeno AND out('esDeTipo').@Class='TipoInfeccion'))))";
		
		// Se lanza la consulta		
		return db.query(q2, params);
		
	}
	
	
	
	
	////////////////////
	// Q5 - CASO RAÍZ //
	////////////////////
	
	public static OVertex q5(OResultSet resultado, String patogeno, String fechaInicio, String fechaFin) {
		
		// Pasar fechas inicial y final a Date
		Date dateInicio = convertirFecha(fechaInicio);
		Date dateFin = convertirFecha(fechaFin);
		
		// Lista con los Eventos Infeccion que cumplen las restricciones temporales
		List<OVertex> eventosOK = new LinkedList<OVertex>();
		
		// Vertices con el primer Evento Infección y su Paciente
		OVertex minEvento = null;
		OVertex minPaciente = null;
		
		
		OResult res;
		OVertex v;
	
		while (resultado.hasNext()) {
			
			res = resultado.next();
			v = res.toElement().asVertex().get();
			
			// De cada Paciente se obtienen sus Eventos
			Iterable<OVertex> eventos = v.getVertices(ODirection.OUT, "tieneHistoricoDe");
			for (OVertex evento : eventos) {
				
				// Solo interesan los Eventos Puntuales cuya descripción sea la bacteria del brote
				if (evento.getProperty("@class").equals("EventoPuntual")  &&  evento.getProperty("descripcion").equals(patogeno)) {
					
					// De esos, los que sean de TipoInfeccion  (solo debe haber un nodo)
					Iterable<OVertex> tipos = evento.getVertices(ODirection.OUT, "esDeTipo");
					int nTip = 0;
					for (OVertex tipo : tipos) {
						if (nTip == 0  &&  tipo.getProperty("@class").equals("TipoInfeccion")) {
								
							// De esos, los que ocurren en las fechas indicadas
							if (isEvPuntualBetweenFechas(evento.getProperty("fecha"), dateInicio, dateFin)) 
								eventosOK.add(evento);
								
							// Es posible que un EventoPuntual no ocurra dentro del período de tiempo especificado, 
							// pero la Estancia/EventoIntervalo en la que ha ocurrido sí
							else {
							
								// Se obtiene la Estancia/EventoIntervalo asociado al Evento
								Iterable<OVertex> intervalos = evento.getVertices(ODirection.IN, "comprende");
								OVertex intvlAsociado = null;
								for (OVertex evInt : intervalos) 
									intvlAsociado = evInt;
								
								// Si el Evento ha ocurrido en una Estancia/EventoIntervalo
								// se comprueba que esta ocurra en el tiempo indicado
								if (intvlAsociado != null) {
									Date fechaInicioInt = intvlAsociado.getProperty("inicio");
									Date fechaFinInt = intvlAsociado.getProperty("fin");
									
									// La Estancia ha terminado  ->  Tiene Inicio y Fin
									if (fechaFinInt != null) {
										if (isEstanciaBetweenFechas(fechaInicioInt, fechaFinInt, dateInicio, dateFin))
											eventosOK.add(evento);
									}
									// La Estancia NO ha terminado  ->  Solo tiene Inicio
									else {
										if (isEstanciaNoTerminadaBetweenFechas(fechaInicioInt, dateFin))
											eventosOK.add(evento);	
									}
								}
								
							}
							
							nTip++;
						}
					}
				}
			}
			
		}
		
		// De esos Eventos nos quedamos con el que tiene la menor fecha
		Date minFecha = new Date();
		for (OVertex evento : eventosOK) {
			Date dateEvent = evento.getProperty("fecha");
			if (dateEvent.before(minFecha)) {
				minFecha = dateEvent;
				minEvento = evento;
			}
		}
		
		if (minEvento != null) {
			System.out.println("Primer Evento: \n @RID: " + minEvento.getIdentity() + "\n id: " + minEvento.getProperty("id") + "\n Fecha: " + minEvento.getProperty("fecha").toString() + "\n");
			
			// De ese Evento obtenemos el Paciente
			Iterable<OVertex> minPacientes = minEvento.getVertices(ODirection.IN, "tieneHistoricoDe");
			for (OVertex p : minPacientes)
				minPaciente = p;
		}
			
		return minPaciente;
	} 
	
	
	
	//////////////////////////////////////////////////////////////
	// FUNCIONES AUXILIARES PARA Q5  -> Comprobaciones de Fecha //
	//////////////////////////////////////////////////////////////
	
	private static boolean isEvPuntualBetweenFechas(Date dateEvento, Date fechaInicio, Date fechaFin) {
		
		int compInicio = dateEvento.compareTo(fechaInicio);
		int compFin = dateEvento.compareTo(fechaFin);
			
		return (compInicio >= 0)  &&  (compFin <= 0);  	
	}
	
	private static boolean isEstanciaBetweenFechas(Date estanciaInicio, Date estanciaFin, Date fechaInicio, Date fechaFin) {
		
		int compInicioInicio = estanciaInicio.compareTo(fechaInicio);
		int compInicioFin = estanciaInicio.compareTo(fechaFin);
		
		int compFinInicio = estanciaFin.compareTo(fechaInicio);
		int compFinFin = estanciaFin.compareTo(fechaFin);
		
		
		// La Estancia empezó en el periodo de tiempo indicado
		if (compInicioInicio >= 0  &&  compInicioFin <= 0)
			return true;
		
		// La Estancia terminó en el periodo de tiempo indicado
		if (compFinInicio >= 0  &&  compFinFin <= 0)
			return true;
		
		// La Estancia fue más larga que el periodo de tiempo indicado
		if (compInicioInicio <= 0  &&  compFinFin  >= 0)
			return true;
		
		return false;
	}	
	
	private static boolean isEstanciaNoTerminadaBetweenFechas(Date estanciaInicio, Date fechaFin) {
		
		// Se comprueba que el Inicio de la Estancia sea anterior a la fecha de Fin del periodo indicado
		// Que haya empezado antes del Inicio es irrelevante
		return (estanciaInicio.compareTo(fechaFin) <= 0);  
	}
	
	
	private static Date convertirFecha(String fecha) {
	
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		
		try {
			date = formato.parse (fecha);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return date;
	}
	
	
	
	
	//////////////////////
	// LISTAR RESULTADO //
	//////////////////////
	
	public static void listarResultado(OResultSet resultado, String titulo) {
		
		System.out.println("\n--- " + titulo + " ---\n");
		
		OResult res;
		Optional<OVertex> ov;
		OVertex v;
		
		int i = 0;
		while(resultado.hasNext()) {
			System.out.println("* NODO: " + i + " *");
			i++;
			
			res = resultado.next();
			ov = res.getVertex();
			v = ov.get();
			
			System.out.println(writePropiedadesNodo(v));
		}
	}
	
	
	public static String writePropiedadesNodo(OVertex vertice) {
		String text = "";
		
		Set<String> propiedades = vertice.getPropertyNames();
		for (String p : propiedades) 
			text += " " + p + ": " + vertice.getProperty(p) + "\n";
		
		text += " @RID: " + vertice.getIdentity() + "  -  @CLASS: " + vertice.getProperty("@class") + "\n";
		
		return text;
	}
	
	
	
	
	//////////////////////
	// CERRAR RESULTADO //
	//////////////////////
	
	public static void cerrarResultado(OResultSet resultado) {
		resultado.close();
	}
	
	
	
}
