package com.optimizesolux.notificationhub.api;

public class CircuitOpenException extends RuntimeException {

    private final String key;

    public CircuitOpenException(String key) {
        super("Circuit ouvert pour le provider/canal: " + key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
