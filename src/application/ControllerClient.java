package application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Classe che gestisce gli eventi dell'interfaccia predisposta dal file Frame.fxml
 * @author Congiusti Daniele
 *
 */
public class ControllerClient {

	private DataOutputStream dos;
	private BufferedReader bf;

	@FXML
	private ProgressIndicator Loading;

	@FXML
	private TextField TextFieldNickname;

	@FXML
	private Button BottoneInvia;

	@FXML
	private TextField TextFieldPassword;

	@FXML
	private Button BottoneRegistra;

	@FXML
	private void initialize() {

		Loading.setVisible(false);

		//inserisco listener sul bottone di invio di dati per il login
		BottoneInvia.setOnAction((event) -> {
			if (TextFieldNickname.getText() == "" || TextFieldPassword.getText() == "") {
				Alert alert = new Alert(AlertType.WARNING, "Inserisci tutti i dati!");
				Loading.setVisible(false);
				alert.showAndWait();
			} else {
				Loading.setVisible(true);
				Socket s = null;
				boolean connesso = false;

				do {
					try {

						// per effettuare le prove utilizzo il localhost, in seguito userò indirzzo
						// pubblico
						s = new Socket("localhost", ServerBattagliaNavale.porta);
						dos = new DataOutputStream(s.getOutputStream());
						bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
						connesso = true;

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ConnectException e) {
						// TODO Auto-generated catch block
						System.out.println("Il server non è ancora pronto, aspetto...");
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						connesso = false;
					} catch (IOException e) {
						e.printStackTrace();
					}

				} while (!connesso);

				// cripto la password
				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance("SHA-512");
					md.update(TextFieldPassword.getText().getBytes());
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				String password_criptata = new String(md.digest());

				try {

					// dopo essermi connesso mando i dati al server
					dos.writeBytes("0#" + TextFieldNickname.getText() + "#" + password_criptata + "\n");
					String res = bf.readLine();
					if (res.equals("1")) {
						Alert alert = new Alert(AlertType.ERROR, "Controlla i dati inseriti");
						alert.showAndWait();
					} else {
						Alert alert = new Alert(AlertType.INFORMATION, "Inizio la ricerca");
						alert.showAndWait();
						//mando Max Wait
						dos.writeBytes("0\n");
						String info = bf.readLine();
						String[] dati_avversario=info.split(":");
						//System.out.println("Ho i dati");
						//mando il CodiceFinePartia
						dos.writeBytes("1\n");
						s.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		//inserisco listener sul bottone per la registrazione
		BottoneRegistra.setOnAction((event) -> {
			if (TextFieldNickname.getText() == "" || TextFieldPassword.getText() == "") {
				Alert alert = new Alert(AlertType.WARNING, "Inserisci tutti i dati!");
				Loading.setVisible(false);
				alert.showAndWait();
			} else {
				Loading.setVisible(true);
				Socket s = null;
				boolean connesso = false;
				
				do {
					try {

						// per effettuare le prove utilizzo il localhost, in seguito userò indirzzo
						// pubblico
						s = new Socket("localhost", ServerBattagliaNavale.porta);
						dos = new DataOutputStream(s.getOutputStream());
						bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
						connesso = true;

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ConnectException e) {
						// TODO Auto-generated catch block
						connesso = false;
						System.out.println("Il server non è ancora pronto, aspetto...");
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				} while (!connesso);
				
				// cripto la password
				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance("SHA-512");
					md.update(TextFieldPassword.getText().getBytes());
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				String password_criptata = new String(md.digest());

				try {

					// dopo essermi connesso mando le informazioni al server
					dos.writeBytes("1#" + TextFieldNickname.getText() + "#" + password_criptata + "\n");
					String res = bf.readLine();
					if (res.equals("1")) {
						Alert alert = new Alert(AlertType.ERROR, "Controlla i dati inseriti");
						alert.showAndWait();
					} else {
						Alert alert = new Alert(AlertType.INFORMATION, "Utente registrato");
						alert.showAndWait();
					}
					Loading.setVisible(false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

}
