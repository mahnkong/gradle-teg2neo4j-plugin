package mahnkong.gteg2neo4j.gteg2neo4j;

/**
 * Created by mahnkong on 28.02.2016.
 */
public enum Greg2Neo4JConstants {
    EXTENSION_NAME("gteg2neo4j");

    private String value;
    Greg2Neo4JConstants(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
