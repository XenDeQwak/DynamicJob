package com.xen.dynamicjob;

public class Process {
    private final String name;
    private final int size;
    private final int arrivalTime;
    private final int processingTime;

    public Process(String name, int size, int arrivalTime, int processingTime) {
        this.name = name;
        this.size = size;
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getProcessingTime() {
        return processingTime;
    }
}
