package net.rhizomik.rhizomer.authentication.fitxerUsuaris;

import net.rhizomik.rhizomer.authentication.Constants.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Classe que comprova si un usuari existeix al fitxer i guarda l'usuari i contrasenya
 * @author <mtp1268@gmail.com>
 */
public class Fitxer {
    private BufferedReader br;
    private BufferedWriter bw;

    /**
     * Comprovem si l'usuari ja hi ha un altre usuari amb el mateix nom
     * @param usr usuari el qual volem comprovar si existeix
     * @return true si existeix al fitxer sino false
     */
    public boolean existeix_usuari(String usr){
        boolean existeix = false;
        try {
            this.br = new BufferedReader(new FileReader(Constants.RUTA_FITXER_CREACIO_USUARIS));
            while (this.br.ready()) {
                String[] c = this.br.readLine().split(":");
                if (c[0].equals(usr)) {
                    existeix = true;
                    break;
                }
            }
        } catch (IOException ex) {
            System.err.println("Hi ha hagut un error al comprovar l'usuari.");
        }finally{
            if(this.br != null){
                try {
                    this.br.close();
                } catch (IOException ex) {
                }
            }
        }
        return existeix;
    }

    /**
     * Guardem l'usuari al fitxer
     * @param u Usuari
     * @return true si l'ha guardat; false en cas d'error
     */
    public boolean guardar(Usuari u){
        boolean afegit = false;
        try {
            this.bw = new BufferedWriter(new FileWriter(Constants.RUTA_FITXER_CREACIO_USUARIS,true));
            this.bw.write(u.getUsuari()+":"+u.getPass());
            this.bw.newLine();
            this.bw.flush();
            this.bw.close();
            afegit = true;
        } catch (IOException ex) {
            System.err.println("Hi ha hagut un error al guardar l'usauri.");
        }
        return afegit;
    }
}