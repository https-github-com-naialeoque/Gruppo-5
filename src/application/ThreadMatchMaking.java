package application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;

/**
 * Classe creata dalla classe ServerBattagliaNavale per gestire un client che si
 * connette al server
 * 
 * @author Congiusti Daniele
 *
 */
public class ThreadMatchMaking extends Thread {

	/**
	 * Istanza di tipo Socket passata tramite costruttore
	 */
	private Socket s;
	/**
	 * Stringa dove viene indicato il ruolo del giocatore durante la connessione:
	 * può essere StabilisciConnessioneClient (SCC) oppure AttendiConnessioneClient
	 * (ACC)
	 */
	private String ruolo;
	/**
	 * Stringa contenente il nickname del client associato
	 */
	private String nick;
	/**
	 * Stringa contenente la password inserita dal client
	 */
	private String password;
	/**
	 * Stream di scrittura relativo al canale di comunicaizone tcp ( Socket s)
	 */
	private DataOutputStream dos;
	/**
	 * Stream di lettura relativo al canale di comunicaizone tcp ( Socket s)
	 */
	private BufferedReader bf;
	/**
	 * Istanza di tipo Connection che verrà utilizzata per instaurare la connessione
	 * con il server 'localhost'
	 */
	private Connection c;
	/**
	 * Istanza di tipo Statement creata dalla variabile Connection c
	 */
	private Statement st;

	/**
	 * Costruttore classe ThreadMatchMaking
	 * 
	 * @param socket: canale di comunazione tra client e server
	 * @author Congiusti Daniele
	 */
	public ThreadMatchMaking(Socket socket) {
		s = socket;
		try {
			// decoro gli stream del canale di comunicazione
			dos = new DataOutputStream(s.getOutputStream());
			bf = new BufferedReader(new InputStreamReader(s.getInputStream()));

			// instauro la connesione e creo la statement
			Class.forName("com.mysql.cj.jdbc.Driver");
			// anche qui inserisco temporaneamente localhost come server sql con cui
			// eseguirò le query
			c = DriverManager.getConnection("jdbc:mysql://localhost:3306/battaglia_navale", "root", "root");
			st = c.createStatement();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectException e) {
			System.out.println("Connessione caduta");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (CommunicationsException e) {
			System.out.println("Server non disponibile");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		String qry = null;
		boolean validate = true;

		try {

			do {
				String info = null;
				String[] dati_client = null;

				// attendo i dati del client
				info = bf.readLine();
				dati_client = info.split("#");
				nick = dati_client[1];
				this.setName(nick);
				password = dati_client[2];

				// effettuo i controlli sul nickname e sulla password
				if (nick.equals("timeout"))
					dos.writeBytes("1\n");

				// se viene indicato nei dati ricevuti, il client viene registrato
				if (dati_client[0].equals("1"))
					registraAccount();
				else {
					// nel caso il client effettui un login allora si vanno a controllare le
					// credenziali inserite
					// System.out.println("Inizio validazione");
					// guardo se esiste l'account
					if (validaAccount(nick)) {
						// se l'account viene trovato allora controllo la password
						if (!validaPassword()) {
							dos.writeBytes("1\n");
							validate = false;
						}

						else {
							// se anche il controllo della password viene superato allora viene il client
							// viene registrato nelle strutture dati del server
							dos.writeBytes("0\n");
							// System.out.println("Registro le info nel server");
							if (ServerBattagliaNavale.giocatori.containsKey(nick)) {
								// System.out.println("Account presente");
								if (!ServerBattagliaNavale.giocatori.get(nick)
										.equals(String.valueOf(s.getInetAddress()))) {
									ServerBattagliaNavale.giocatori.put(nick, String.valueOf(s.getInetAddress()));
								}
							} else {
								ServerBattagliaNavale.giocatori.put(nick, String.valueOf(s.getInetAddress()));
							}
							ServerBattagliaNavale.online.add(this);
							validate = true;
						}
					}

					else
						validate = false;

				}
			} while (!validate);

			// una volta validati i dati del client allora passiamo alla ricerca
			// dell'avversario

			// prendo il max wait
			int Max_wait = Integer.parseInt(bf.readLine());
			//aggiorno lo stato del client per poter essere visibile dagli altri giocatori
			qry = "UPDATE account SET statoGiocatore = 1 WHERE Nickname='" + nick + "'";
			st.executeUpdate(qry);
			ricercaGiocatore(Max_wait);

			// una volta iniziata la partita il server ne attende la fine
			attesaFine();

		} catch (ConnectException e) {
			System.out.println("Connessione caduta");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} catch (SocketException e) {
			System.out.println("Connessione caduta");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (CommunicationsException e) {
			System.out.println("Server non disponibile");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Metodo che riceve il CodiceFineParita(CFP) e fa partire il conteggio di
	 * conclusione della partita (controllare classe Partita)
	 */
	public void attesaFine() {
		String CFP = null;
		String qry = null;
		try {
			synchronized (this) {
				//leggo CFP
				CFP = bf.readLine();
				//una volta letto il CFP allora provvedo ad aggiornare i dati sul database
				qry = "UPDATE account SET Inpartita=0 WHERE Nickname='" + nick + "'";
				st.executeUpdate(qry);
				ruolo = null;
				//System.out.println("CFP ricevuto:" + CFP);
				notifyAll();
				s.close();
			}
		} catch (ConnectException e) {
			System.out.println("Connessione caduta");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (CommunicationsException e) {
			System.out.println("Server non disponibile");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Metodo per effettuare la ricerca di un avversario
	 * 
	 * @param <strong>t</strong> Max Wait(MW)
	 */
	public void ricercaGiocatore(int t) {
		int time = t;
		int c = 0;
		Partita p = null;
		String app = null;
		String qry = null;
		String qry1 = "SELECT nickname FROM account WHERE statoGiocatore=1 AND Inpartita=0 AND nickname <> '" + nick
				+ "'";
		boolean gioco = false;
		//finchè non trovo un giocatore resto in attesa
		while (!gioco && !s.isClosed()) {
			try {
				//controllo di non essere già in una partita
				if (inPartita()) {
					inviaInfo(nick);
					break;
				}

				//dentro questa variabile sono contenuti i giocatori online 
				ResultSet rs = st.executeQuery(qry1);
				//effettuo il conteggio
				if (t != 0) {
					Thread.sleep(1000);
					time--;
					if (time == 0) {
						dos.writeBytes("timeout\n");
					}
				}
				//se è disponibile un altro giocatore allora creo una parita
				if (rs.next()) {
					//System.out.println("Partita formata");
					app = rs.getString(1);
					//stabilisco i ruoli dei client durante la connessione
					for (c = 0; c < ServerBattagliaNavale.online.size(); c++) {
						if (ServerBattagliaNavale.online.get(c).getName().equals(app)) {
							ServerBattagliaNavale.online.get(c).setRuolo("ACC");
						}
					}
					this.ruolo = "SCC";
					//System.out.println("Ruoli cambiati");
					//metto i giocatori in partita
					qry = "UPDATE account SET Inpartita=1 WHERE Nickname='" + nick + "'";
					st.executeUpdate(qry);
					qry = "UPDATE account SET Inpartita=1 WHERE Nickname='" + app + "'";
					st.executeUpdate(qry);
					//inizializzo l'istanza di tipo Partita
					for (c = 0; c < ServerBattagliaNavale.online.size(); c++) {
						if (ServerBattagliaNavale.online.get(c).getName().equals(app)) {
							p = new Partita(this, ServerBattagliaNavale.online.get(c));
							p.start();
						}
					}

					inviaInfo(app);
					gioco = true;
				}

			} catch (ConnectException e) {
				System.out.println("Connessione caduta");
				try {
					s.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (CommunicationsException e) {
				System.out.println("Server non disponibile");
				try {
					s.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void inviaInfo(String nick_avversario) {
		String info = null;
		//System.out.println("il mio avversario è:" + nick_avversario);
		//System.out.println("Ora invio le info\nIo sono:" + ruolo);
		try {
			//in base al ruolo del client invio differenti dati
			// SCC -> ip ACC :nickname ACC
			// ACC -> nickname SCC
			if (ruolo.equals("SCC")) {
				System.out.println("Devo stabilire la connessione");
				for (String s : ServerBattagliaNavale.giocatori.keySet()) {
					if (!ServerBattagliaNavale.giocatori.containsKey(nick_avversario)) {
						System.out.println("Giocatore non presente");
					} else {
						if (s.equals(nick_avversario)) {
							info = ServerBattagliaNavale.giocatori.get(s) + ":" + nick_avversario + "\n";
							info = info.substring(1);
							//System.out.println("trovato giocatore\necco le info:" + info);
						}
					}
				}
				dos.writeBytes(info);
			} else {
				System.out.println("Attendo la connessione");
				dos.writeBytes(nick + "\n");
			}
		} catch (ConnectException e) {
			System.out.println("Connessione caduta");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Info inviate");
	}

	public String getRuolo() {
		return ruolo;
	}

	public void setRuolo(String ruolo) {
		this.ruolo = ruolo;
	}

	/**
	 * Metodo per controllare se un utente è in partita
	 * 
	 * @return <strong>true</strong> se il client è in partita <br>
	 *         <strong>false</strong> se il client è ancora in ricerca di un
	 *         avversario
	 */
	public boolean inPartita() {
		ResultSet rs = null;
		try {
			//prendo il campo dal database
			rs = st.executeQuery("SELECT Inpartita FROM account WHERE nickname='" + nick + "'");
			if (rs.next()) {
				if (rs.getBoolean("Inpartita"))
					return true;
			}
			return false;
		} catch (CommunicationsException e) {
			System.out.println("Server non disponibile");
			try {
				s.close();
				return false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Metodo per verificare se la password immessa dall'utente è corretta
	 * 
	 * @return <strong>true</strong> se la password è corretta <br>
	 *         <strong>false</strong> se la password è sbagliata
	 */
	public boolean validaPassword() {
		String qry = "SELECT account.password FROM account WHERE account.Nickname='" + nick + "'";
		ResultSet rs;
		String pass = null;
		try {
			//prenod la password registrata e la confornto con quella ricevuta
			rs = st.executeQuery(qry);
			if (rs.next())
				pass = rs.getString("password");

			if (!pass.equals(password)) {
				return false;
			}

			else {
				return true;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * Metodo per registrare un account nel database
	 */
	public void registraAccount() {
		String qry = null;
		boolean esistente = false;
		try {
			//verifico che il nickname non sia già preso
			qry = "SELECT * FROM account WHERE nickname='" + nick + "'";
			ResultSet rs = st.executeQuery(qry);
			while (rs.next()) {
				if (rs.getString("nickname").equals(nick))
					esistente = true;
			}

			if (esistente) {
				//se esiste un account con il nickname scelto allora invito il client a rinserire i dati
				dos.writeBytes("1\n");
			} else {
				//nel caso venga superato il controllo del nickaname allora inserisco l'account nel database
				qry = "INSERT INTO account VALUES ('" + nick + "','" + password + "',1,0);";
				ServerBattagliaNavale.giocatori.put(nick, String.valueOf(s.getInetAddress()));
				st.executeUpdate(qry);
				dos.writeBytes("0\n");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				dos.writeBytes("1\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				dos.writeBytes("1\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Metodo per verificare se il nickname inserito corrisponde ad un account
	 * esistente
	 * 
	 * @param <strong>nickname</strong>: inviato dall'utente tramite socket tcp
	 * @return <strong> true </strong> se l'account esiste <strong> false </strong>
	 *         se l'account non esiste
	 */
	public boolean validaAccount(String nickname) {
		String qry = null;
		boolean valido = false;
		try {
			// verifico se esiste l'account
			qry = "SELECT * FROM account WHERE account.Nickname='" + nick + "'";
			ResultSet rs = st.executeQuery(qry);
			if (rs.next()) {
				if (rs.getString("Nickname").equals(nick))
					valido = true;
			}

			if (valido) {
				return true;
			} else
				return false;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
