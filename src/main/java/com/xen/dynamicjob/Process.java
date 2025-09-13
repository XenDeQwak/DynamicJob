package com.xen.dynamicjob;

public class Process {
    private String name;
    private int size;
    private int arrivalTime;
    private int processingTime;

    public Process(String name, int size, int arrivalTime, int processingTime) {
        this.name = name;
        this.size = size;
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }
}

