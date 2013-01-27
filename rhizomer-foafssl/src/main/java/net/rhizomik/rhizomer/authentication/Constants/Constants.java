package net.rhizomik.rhizomer.authentication.Constants;

/**
 * Classe de variables
 * @author <mtp1268@gmail.com>
 */
public class Constants {

    //Variable del nom del fitxer on estan els usuaris
    public static final String NOM_FITXER_USUARIS = "usuaris.txt";
    //Tipo de encriptació que s'utilitza per la contrasenya
    public static final String TIPO_ENCRIPTACIO = "MD5";
    //Ruta on es troba el fitxer si s'executem la clase de CreacioUsuaris!
    public static final String RUTA_FITXER_CREACIO_USUARIS = "rhizomer-foafssl\\src\\main\\webapp\\WEB-INF\\"+NOM_FITXER_USUARIS;

    public static final String NOM_SESSIO_USUARI= "net.rhizomik.rhizomer.authentication";
}