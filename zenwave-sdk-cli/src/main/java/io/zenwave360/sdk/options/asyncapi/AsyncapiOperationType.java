package io.zenwave360.sdk.options.asyncapi;

public enum AsyncapiOperationType {
    publish, subscribe,
    send, receive;

    public boolean isEquivalent(AsyncapiOperationType that) {
        if(this == publish || this == send) {
            return that == publish || that == send;
        } else {
            return that == subscribe || that == receive;
        }
    }
}
