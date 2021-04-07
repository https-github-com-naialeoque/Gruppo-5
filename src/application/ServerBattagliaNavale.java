package application;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
/**
 * Classe che accetta le richieste di connessione dei client e che permette di gestire le partite tramite le strutture dati di tipo ArrayList e Hashtable
 * @author Congiusti Daniele
 *
 */
public class ServerBattagliaNavale {

	/**
	 * intero che rappresenta la porta su cui il server sarà in ascolto
	 */
	public static final int porta = 1998;
	/**
	 * struttura dati dinamica dove vengono memorizzate le informazioni dei giocatori
	 */
	public static Hashtable<String,String> giocatori= new Hashtable<String,String>();
	/**
	 * struttura dati dinamica dove vengono inseriti i thread associati ai giocatori
	 */
	public static ArrayList<ThreadMatchMaking> online = new ArrayList<ThreadMatchMaking>();
	
	public static void main(String[] args) {
		ServerSocket ss = null;
		try {
			//assegno la porta alla variabile 
			ss = new ServerSocket(porta);
			
			while(true) {
				//metto il server in ascolto sulla porta 
				Socket s = ss.accept();
				//creo nuovo thread che gestirà il client
				ThreadMatchMaking t = new ThreadMatchMaking(s);
				//aggiungo il thread alla struttura dati
				online.add(t);
				//faccio partire il thread
				t.start();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				 ss.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
}
