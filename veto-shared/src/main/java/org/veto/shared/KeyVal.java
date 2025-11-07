package org.veto.shared;

import java.util.Objects;

public class KeyVal <K, V>{
    public KeyVal() {
    }

    public KeyVal(K key, V value) {
        this.key = key;
        this.val = value;
    }

    private K key;

    private volatile V val;

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getVal() {
        return val;
    }

    public void setVal(V val) {
        this.val = val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyVal)) return false;
        KeyVal<?, ?> keyVal = (KeyVal<?, ?>) o;
        return Objects.equals(key, keyVal.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
