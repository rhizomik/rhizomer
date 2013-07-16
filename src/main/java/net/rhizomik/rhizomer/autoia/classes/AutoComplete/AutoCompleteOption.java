package net.rhizomik.rhizomer.autoia.classes.AutoComplete;


public class AutoCompleteOption {

    private String uri;
    private String label;
    private String klass;
    private int numInstances;

    public AutoCompleteOption(String uri, String label, String klass){
        this.uri = uri;
        this.label = label;
        this.klass = klass;
    }

    public void setNumInstances(int numInstances){
        this.numInstances = numInstances;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }
}
