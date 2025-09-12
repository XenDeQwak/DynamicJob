package com.xen.dynamicjob;

public class Process {
    private final String name;
    private final int size;

    public Process(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }
}
