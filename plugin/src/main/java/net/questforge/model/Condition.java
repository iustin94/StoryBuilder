package net.questforge.model;

public class Condition {

    private final String var;
    private final String op;
    private final Object value;

    public Condition(String var, String op, Object value) {
        this.var = var;
        this.op = op;
        this.value = value;
    }

    public String getVar() {
        return var;
    }

    public String getOp() {
        return op;
    }

    public Object getValue() {
        return value;
    }
}
