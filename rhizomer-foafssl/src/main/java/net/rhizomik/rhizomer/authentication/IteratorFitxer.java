package net.rhizomik.rhizomer.authentication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import net.rhizomik.rhizomer.authentication.Constants.Constants;

/**
 * Classe Iteradora per a fitxers
 * @author <mtp1268@gmail.com>
 */
public class IteratorFitxer extends Iterator {
    private BufferedReader br = null;
    private String ruta_usuaris = "";

    public IteratorFitxer(String ruta){
        this.ruta_usuaris = ruta+"/";
    }
    @Override
    public void primer(){
        try {
            FileReader fr = null;
            String s = this.ruta_usuaris+""+Constants.NOM_FITXER_USUARIS;
            File f = new File(s);
            if(f.exists()){
                fr = new FileReader(f);
                this.br = new BufferedReader(fr);
            }
        } catch (FileNotFoundException ex) {
            this.br = null;
        }
    }

    @Override
    public Object seguent() {
        try {
            return this.br.readLine();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public boolean hi_ha_mes() {
        try {
            return this.br.ready();
        } catch (Exception e) {
            try {
                this.br.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            return false;
        }
    }

    @Override
    public Object elements_actuals() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}