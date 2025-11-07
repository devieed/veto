package org.veto.shared;

public class KeyFieldVal <K, F, V>{
    public KeyFieldVal() {
    }
    private K key;

    private F field;

    private volatile V val;

    public KeyFieldVal(K key, F field, V val) {
        this.key = key;
        this.field = field;
        this.val = val;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public F getField() {
        return field;
    }

    public void setField(F field) {
        this.field = field;
    }

    public V getVal() {
        return val;
    }

    public void setVal(V val) {
        this.val = val;
    }
}
