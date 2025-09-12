package com.xen.dynamicjob;

import javafx.scene.paint.Color;

public class Partition {
    private String name;
    private int size;
    private Color color;

    public Partition(String name, int size, Color color) {
        this.name = name;
        this.size = size;
        this.color = color;
    }

    public boolean isFree() {
        return name.equals("Free");
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Color getColor() {
        return color;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
