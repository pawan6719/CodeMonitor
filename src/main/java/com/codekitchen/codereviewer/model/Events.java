package com.codekitchen.codereviewer.model;

public enum Events {
    PUSH, PULL_REQUEST;

    public String toString(){
        return this.name().toLowerCase();
    }
}
