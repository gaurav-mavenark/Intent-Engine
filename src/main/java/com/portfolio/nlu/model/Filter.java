package com.portfolio.nlu.model;

/**
 * A single extracted filter condition, e.g. field="purchaseDate" operator="=" value="Last Year".
 */
public class Filter {

    private String field;
    private String operator;
    private String value;

    public Filter() {
    }

    public Filter(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}
