package org.SFM;

public enum Colour {
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    CYAN("\u001B[36m");

    private final String ANSIcode;
    Colour(String code){
        this.ANSIcode = code;
    }

    @Override
    public String toString(){
        return ANSIcode;
    }
}
