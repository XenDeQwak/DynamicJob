package com.xen.dynamicjob;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Random;

public class JobController {
    @FXML private VBox memoryBox;
    @FXML private TextField processNameField;
    @FXML private TextField sizeField;
    @FXML private TextField arrivalField;
    @FXML private TextField durationField;
    @FXML private Button addBtn;
    @FXML private Button freeBtn;

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
                int arrival = Integer.parseInt(arrivalField.getText().trim());
                int duration = Integer.parseInt(durationField.getText().trim());
                if (arrival < 0) {
                    throw new IllegalArgumentException("Arrival time cannot be negative");
                }
                if (duration <= 0) {
                    throw new IllegalArgumentException("Processing time must be positive");
                }
                Process process = new Process(name, size, arrival, duration);
                scheduleProcess(process);
            } catch (NumberFormatException ignored) {}
        });

        freeBtn.setOnAction(e -> {
            String name = processNameField.getText().trim();
            if (name.isEmpty()) return;
            freeProcess(name);
            refreshView();
        });
    }

    private void scheduleProcess(Process process) {
        if (process.getArrivalTime() == 0) {
            allocateProcess(process);
            refreshView();
            if (process.getProcessingTime() > 0) {
                Timeline endTimer = new Timeline(new KeyFrame(Duration.seconds(process.getProcessingTime()), ev -> {
                    freeProcess(process.getName());
                    refreshView();
                }));
                endTimer.setCycleCount(1);
                endTimer.play();
            }
        } else {
            Timeline arrivalTimer = getTimeline(process);
            arrivalTimer.play();
        }
    }

    private Timeline getTimeline(Process process) {
        Timeline arrivalTimer = new Timeline(new KeyFrame(Duration.seconds(process.getArrivalTime()), ev -> {
            allocateProcess(process);
            refreshView();
            if (process.getProcessingTime() > 0) {
                Timeline endTimer = new Timeline(new KeyFrame(Duration.seconds(process.getProcessingTime()), ev2 -> {
                    freeProcess(process.getName());
                    refreshView();
                }));
                endTimer.setCycleCount(1);
                endTimer.play();
            }
        }));
        arrivalTimer.setCycleCount(1);
        return arrivalTimer;
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