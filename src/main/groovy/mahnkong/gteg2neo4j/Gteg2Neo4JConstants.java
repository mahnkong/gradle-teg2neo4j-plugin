package mahnkong.gteg2neo4j;

/**
 * Created by mahnkong on 28.02.2016.
 */
public enum Gteg2Neo4JConstants {
    EXTENSION_NAME("gteg2neo4j");

    private String value;
    Gteg2Neo4JConstants(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
