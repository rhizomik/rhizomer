/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rhizomik.rhizomer.service;

import java.io.Serializable;

/**
 * @author kwal85
 * @author http://rhizomik.net/~roberto/
 */

public class Service implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 6956251796170459085L;
    protected String uri;
    protected String label;
    protected String icon;
    protected String sparqlQuery;
    private String outInput;
    private String endpoint;


    public Service(String uri){
        this.uri = uri;
    }
    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the sparqlQuery
     */
    public String getSparqlQuery() {
        return sparqlQuery;
    }

    /**
     * @param sparqlQuery the sparqlQuery to set
     */
    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the outInput
     */
    public String getOutInput() {
        return outInput;
    }

    /**
     * @param outInput the outInput to set
     */
    public void setOutInput(String outInput) {
        this.outInput = outInput;
    }

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
