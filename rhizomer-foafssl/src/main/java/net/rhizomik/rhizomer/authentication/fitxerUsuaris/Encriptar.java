package net.rhizomik.rhizomer.authentication.fitxerUsuaris;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.rhizomik.rhizomer.authentication.Constants.Constants;

/**
 * Classe que ens serveix per encriptar la contrasenya
 * @author <mtp1268@gmail.com>
 */
public class Encriptar {
    private byte[] buffer;
    private MessageDigest md = null;
    private static final char[] HEX_CHARS = {'0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'};

    public Encriptar(){
        try {
            this.md = MessageDigest.getInstance(Constants.TIPO_ENCRIPTACIO);
        } catch (NoSuchAlgorithmException ex) {
            this.md = null;
            System.err.println("No es pot encriptar la contrasenya.");
        }
    }

    /**
     * Metode que inicialitzem la contrasenya
     * @param pass contrasenya
     */
    public void setPass(String pass){
        if(pass == null){
            pass = "";
        }
        byte[] textBytes = String.valueOf(pass).getBytes();
        this.md.update(textBytes);
        this.buffer = this.md.digest();
    }

    /**
     * Obtenim la contrasenya encriptada amb el format que hem triat
     * @return contrasenya encriptada en format String
     */
    public String getPass(){
        char buf[] = new char[this.buffer.length*2];
        for (int i = 0, x = 0; i < this.buffer.length; i++) {
            buf[x++] = HEX_CHARS[(this.buffer[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[this.buffer[i] & 0xf];
        }
        return new String(buf);
    }
}