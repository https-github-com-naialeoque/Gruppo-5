package application;
/**
 * Classe per gestire una partita,  gestendo eventuali comportamenti non aspettati da uno dei client
 * @author Congiusti Daniele
 *
 */
public class Partita extends Thread {
	/**
	 * Istanza di tipo ThreadMatchMaking associata al primo giocatore
	 */
	private ThreadMatchMaking G1;
	/**
	 * Istanza di tipo ThreadMatchMaking associata al secondo giocatore
	 */
	private ThreadMatchMaking G2;

	/**
	 * Costruttore classe Partia
	 * @param <strong>g1</strong>: Istanza di tipo ThreadMatchMaking che è associata al primo giocatore
	 * @param <strong>g2</strong>: Istanza di tipo ThreadMatchMaking che è associata al secondo giocatore
	 */
	public Partita(ThreadMatchMaking g1, ThreadMatchMaking g2) {
		G1 = g1;
		G2 = g2;
	}

	@Override
	public void run() {
		//aspetto che uno dei due client abbia finito la partita
		while (G1.isAlive() && G2.isAlive()) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		aspettaGiocatore();

	}

	/**
	 * Metodo per iniziare il conteggio di chiusura forzata della connessione
	 */
	@SuppressWarnings("deprecation")
	public void aspettaGiocatore() {
		int i = 0;
		if (G1.isAlive()) {
			while (G1.isAlive()) {
				try {
					
					do {
						if (i == 60)
							break;
						Thread.sleep(1000);
						i++;
					} while (i <= 60);

					if (i == 60)
						G1.stop();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else {
			if (G2.isAlive()) {
				while (G2.isAlive()) {
					try {

						do {
							if (i == 60)
								break;
							Thread.sleep(1000);
							i++;
						} while (i <= 60);

						if (i == 60)
							G2.stop();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

}
