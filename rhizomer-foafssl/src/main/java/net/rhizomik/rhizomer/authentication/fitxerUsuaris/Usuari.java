package net.rhizomik.rhizomer.authentication.fitxerUsuaris;

/**
 * Classe on comprovem i validem que el usauri sigui vàlid i encriptem la contrasenya
 * @author <mtp1268@gmail.com>
 */
public class Usuari {
    private String usuari,pass;
    private Fitxer f;
    private Encriptar encriptar = new Encriptar();

    /**
     * Es comprova que l'usuari no contigui ':' i que no existeixi al fitxer
     * @return true si l'usuari es vàlid altrament, false
     */
    public boolean es_valid(){
        boolean es_valid_usr = false;
        if( this.usuari.indexOf(":") == -1){
            this.f = new Fitxer();
            if(!this.f.existeix_usuari(this.usuari)){
                es_valid_usr = true;
            }
        }
        return es_valid_usr;
    }

    /**
     * Guardem l'usuari al fitxer
     * @return true si l'usuari s'ha guardat, sino false
     */
    public boolean guardar(){
        return this.f.guardar(this);
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        if(pass == null){
            pass = "";
        }
        this.encriptar.setPass(pass);
        this.pass = this.encriptar.getPass();
    }

    public String getUsuari() {
        return usuari;
    }

    public void setUsuari(String usuari) throws Exception {
        if(usuari == null || usuari.equals("")){
            throw new Exception("El usuari no pot tindre un camp buit.");
        }
        this.usuari = usuari;
    }
}