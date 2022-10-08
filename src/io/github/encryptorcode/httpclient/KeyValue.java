package io.github.encryptorcode.httpclient;

class KeyValue<K,V>{
    private K key;
    private V value;

    KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    K getKey() {
        return key;
    }

    V getValue() {
        return value;
    }
}