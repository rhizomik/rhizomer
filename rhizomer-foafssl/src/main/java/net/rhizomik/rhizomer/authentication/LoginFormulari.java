package net.rhizomik.rhizomer.authentication;

import net.rhizomik.rhizomer.authentication.fitxerUsuaris.Encriptar;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import sun.misc.BASE64Decoder;

/**
 * Mostra un formulari si no te cap certificat(o no es vàlid) i comproba les dades que s'han passat pel formulari
 @author <mtp1268@gmail.com>
 */
public class LoginFormulari {
    private String autoritzacio;
    private String usuari,pswd;
    private Iterator iter = null;
    public LoginFormulari(String ruta_usuaris){
        this.iter = new IteratorFitxer(ruta_usuaris);

    }

    /**
     * Comprova si esta autentificat  mitjançant el formulari del navegador
     * @param request variable Request
     * @return true si s'ha de autentificar altrament false
     */
    public boolean sha_de_autentificar(ServletRequest request){
        this.autoritzacio = ((HttpServletRequest)request).getHeader("Authorization");
        return (this.autoritzacio == null);
    }

    /**
     * Mostra el formulari a través del navegador
     * @param r el valor del Response
     */
    public void mostrar_formulari(ServletResponse r) {
        HttpServletResponse response = (HttpServletResponse) r;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // I.e., 401
        response.setHeader("WWW-Authenticate","BASIC realm=\"Insider-Trading\"");
    }
    /**
     * Obtenim l'usuari i contrasenya
     * @throws IOException
     */
    public void obtenir_dades() throws IOException{
        String info = this.autoritzacio.substring(6).trim();
        BASE64Decoder decoder = new BASE64Decoder();
        String nameAndPassword = new String(decoder.decodeBuffer(info));
        int index = nameAndPassword.indexOf(":");//Decoded part looks like "username:password".
        this.usuari = nameAndPassword.substring(0, index);
        this.pswd = nameAndPassword.substring(index + 1);
    }

    /**
     * Comprovem que l'usuari i contrasenya siguin correctes
     * @return true si l'usuari i contrasenya son correctes sino false
     */
    public boolean son_valides() throws NullPointerException{
        boolean son_valids = false;
        Encriptar e = new Encriptar();
        this.iter.primer();
        while (this.iter.hi_ha_mes()) {
            String[] str = ((String) this.iter.seguent()).split(":");
            e.setPass(this.pswd);
            if (this.usuari.equals(str[0]) && e.getPass().equals(str[1])) {
                son_valids = true;
                break;
            }
        }
        return son_valids;
    }

    public String getUsuari() {
        return usuari;
    }

}