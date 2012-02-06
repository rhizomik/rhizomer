package net.rhizomik.rhizomer.autoia.generator;

import java.util.*;

import static net.rhizomik.rhizomer.util.Namespaces.rdf;
import static net.rhizomik.rhizomer.util.Namespaces.rdfs;
import static net.rhizomik.rhizomer.util.Namespaces.xsd;


public class TypeHierarchy {

    private static class TypeNode {

        private String type;
        private TypeNode parent;
        private int level;

        public TypeNode(String type, TypeNode parent) {
            this.type = type;
            this.parent = parent;
            if (parent == null) {
                this.level = 0;
            } else {
                this.level = parent.level + 1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TypeNode typeNode = (TypeNode) o;

            return !(type != null ? !type.equals(typeNode.type) : typeNode.type != null);

        }

        @Override
        public int hashCode() {
            return type != null ? type.hashCode() : 0;
        }
    }
    
    public static final TypeHierarchy RDF;
    
    static {
        RDF = new TypeHierarchy(rdfs("Resource"));
        TypeNode rdfs_Class = RDF.addType(rdfs("Class"), RDF.rootNode);
        TypeNode rdfs_Literal = RDF.addType(rdfs("Literal"), RDF.rootNode);
        TypeNode rdf_Property = RDF.addType(rdf("Property"), RDF.rootNode);
        TypeNode rdfs_Datatype = RDF.addType(rdfs("Datatype"), rdfs_Class);
        TypeNode rdf_XMLLiteral = RDF.addType(rdf("XLMLiteral"), rdfs_Literal);
        TypeNode xsd_anySimpleType = RDF.addType(xsd("anySimpleType"), rdfs_Literal);
        TypeNode xsd_string = RDF.addType(xsd("string"), xsd_anySimpleType);
        TypeNode xsd_double = RDF.addType(xsd("double"), xsd_anySimpleType);
        TypeNode xsd_decimal = RDF.addType(xsd("decimal"), xsd_anySimpleType);
        TypeNode xsd_integer = RDF.addType(xsd("integer"), xsd_decimal);
        TypeNode xsd_date = RDF.addType(xsd("date"), xsd_anySimpleType);
    }

    private Map<String, TypeNode> nodes;
    private TypeNode rootNode;

    public TypeHierarchy(String root) {
        nodes = new HashMap<String, TypeNode>();
        rootNode = createRoot(root);
    }

    private TypeNode createRoot(String root) {
        return addType(root, null);
    }

    private TypeNode addType(String child, TypeNode parentNode) {
        TypeNode childNode = new TypeNode(child, parentNode);
        nodes.put(child, childNode);
        return childNode;
    }

    private TypeNode lowestCommonAncestor(Collection<TypeNode> nodes) {
        Iterator<TypeNode> it = nodes.iterator();
        TypeNode lowest = it.next();
        while (it.hasNext()) {
            lowest = lowestCommonAncestor(lowest, it.next());
        }
        return lowest;
    }

    private TypeNode lowestCommonAncestor(TypeNode node1, TypeNode node2) {
        while (!node1.equals(node2)) {
            if (node1.level == node2.level) {
                node1 = node1.parent;
                node2 = node2.parent;
            } else if (node1.level > node2.level) {
                node1 = node1.parent;
            } else {
                node2 = node2.parent;
            }
        }
        return node1;
    }

    public String lowestCommonType(Collection<String> types) {
        Collection<TypeNode> typeNodes = new HashSet<TypeNode>();
        for (String type : types) {
            TypeNode node = nodes.get(type);
            if (node == null) {
                // If we don't find the type, we add it to the hierarchy as a child of the root node
                node = addType(type, rootNode);
            }
            typeNodes.add(node);
        }
        return lowestCommonAncestor(typeNodes).type;
    }

}
