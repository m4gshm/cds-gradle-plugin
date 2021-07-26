package com.github.m4gshm.cds.test;

public class Main {

    public static final MainSupport support = new MainSupport();

    public static void main(String[] args) {
        support.println("running " + Main.class.getCanonicalName() + ".main()");
    }
}
