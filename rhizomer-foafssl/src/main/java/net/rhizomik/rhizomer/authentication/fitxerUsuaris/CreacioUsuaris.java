package net.rhizomik.rhizomer.authentication.fitxerUsuaris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Classe per a crear nous usuaris per a la web
 * @author <mtp1268@gmail.com>
 */
public class CreacioUsuaris {

    public static void main(String args[]) {
        String usr, pass;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Usuari usuari = null;

        usr = null;
        pass = null;

        try{
            System.out.println("===[** GENERACIÓ USUARI/CONTRASENYA **]===");
            System.out.print("\tEscriu el nom d'usuari( Recorda que l'usuari no pot ser buit): ");
            usr = br.readLine();
            System.out.println();
            System.out.print("\tEscriu la contrasenya: ");
            pass = br.readLine();
            usuari = new Usuari();
            try{
                usuari.setUsuari(usr);
                usuari.setPass(pass);
                if(usuari.es_valid()){
                    if(usuari.guardar()){
                        System.out.println("L'usuari: "+usr+" s'ha guardat correctament.");
                    }else{
                        System.out.println("Hi ha hagut un error al guardar l'usuari, per tant l'usuari no s'ha guardat.");
                    }
                }
            }catch(Exception ex){
                System.err.println(ex.getMessage());
            }
        }catch(IOException ex){
            System.err.println("Error al capturar les dades per pantalla.");
        }
    }
}