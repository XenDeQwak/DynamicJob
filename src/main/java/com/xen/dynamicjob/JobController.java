package com.xen.dynamicjob;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.Random;

public class JobController {
    @FXML
    private VBox memoryBox;
    @FXML
    private TextField processNameField;
    @FXML
    private TextField sizeField;
    @FXML
    private Button addBtn;
    @FXML
    private Button freeBtn;

    private int totalMemory = 1000;
    private final LinkedList<Partition> memory = new LinkedList<>();
    private final Random random = new Random();

    @FXML
    private void initialize() {
        memory.clear();
        memory.add(new Partition("Free", totalMemory, Color.LIGHTGRAY));
        refreshView();

        addBtn.setOnAction(e -> {
            String name = processNameField.getText().trim();
            if (name.isEmpty()) return;
            try {
                int size = Integer.parseInt(sizeField.getText().trim());
                allocateProcess(new Process(name, size));
                refreshView();
            } catch (NumberFormatException ignored) {}
        });

        freeBtn.setOnAction(e -> {
            String name = processNameField.getText().trim();
            if (name.isEmpty()) return;
            freeProcess(name);
            refreshView();
        });
    }

    private void allocateProcess(Process process) {
        for (int i = 0; i < memory.size(); i++) {
            Partition p = memory.get(i);
            if (p.isFree() && p.getSize() >= process.getSize()) {
                Partition allocated = new Partition(process.getName(), process.getSize(), randomColor());
                p.setSize(p.getSize() - process.getSize());
                if (p.getSize() == 0) memory.set(i, allocated);
                else memory.add(i, allocated);
                return;
            }
        }
    }

    private void freeProcess(String name) {
        for (int i = 0; i < memory.size(); i++) {
            Partition p = memory.get(i);
            if (!p.isFree() && p.getName().equals(name)) {
                p.setName("Free");
                p.setColor(Color.LIGHTGRAY);
                merge();
                return;
            }
        }
    }

    private void merge() {
        for (int i = 0; i < memory.size() - 1; i++) {
            Partition current = memory.get(i);
            Partition next = memory.get(i + 1);
            if (current.isFree() && next.isFree()) {
                current.setSize(current.getSize() + next.getSize());
                memory.remove(i + 1);
                i--;
            }
        }
    }

    private void refreshView() {
        memoryBox.getChildren().clear();
        double scale = 300.0 / totalMemory;
        for (Partition p : memory) {
            HBox box = new HBox();
            box.setPrefWidth(200);
            box.setPrefHeight(p.getSize() * scale);
            box.setStyle("-fx-border-color: black;");
            box.setBackground(new Background(new BackgroundFill(p.getColor(), CornerRadii.EMPTY, null)));
            Label label = new Label(p.getName() + " (" + p.getSize() + " MB)");
            box.getChildren().add(label);
            memoryBox.getChildren().add(box);
        }
    }

    private Color randomColor() {
        return Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }
}