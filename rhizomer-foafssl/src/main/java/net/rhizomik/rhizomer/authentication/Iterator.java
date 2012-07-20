package net.rhizomik.rhizomer.authentication;

/**
 * Clase abstracta d'un Iterator
 * @author <mtp1268@gmail.com>
 */
public abstract class Iterator{
    public abstract void primer()throws NullPointerException;
    public abstract Object seguent();
    public abstract boolean hi_ha_mes();
    public abstract Object elements_actuals();

}