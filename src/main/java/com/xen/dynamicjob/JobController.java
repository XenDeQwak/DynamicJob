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
    @FXML private VBox queueBox;
    @FXML private TextField memorySizeField;
    @FXML private TextField compactionField;
    @FXML private Button setMemoryBtn;

    private int totalMemory = 200;
    private final LinkedList<Partition> memory = new LinkedList<>();
    private final LinkedList<Process> waitingQueue = new LinkedList<>();
    private final Random random = new Random();
    private Timeline compactionTimer;

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
                if (arrival < 0) throw new IllegalArgumentException("Arrival time cannot be negative");
                if (duration <= 0) throw new IllegalArgumentException("Processing time must be positive");
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

        setMemoryBtn.setOnAction(e -> {
            try {
                int newSize = Integer.parseInt(memorySizeField.getText().trim());
                if (newSize <= 0) return;
                totalMemory = newSize;
                memory.clear();
                memory.add(new Partition("Free", totalMemory, Color.LIGHTGRAY));
                refreshView();
            } catch (NumberFormatException ignored) {}
        });

        compactionField.setOnAction(e -> setupCompactionTimer());
    }

    private void scheduleProcess(Process process) {
        if (process.getArrivalTime() == 0) {
            allocateProcess(process);
            refreshView();
            if (process.getProcessingTime() > 0) scheduleEnd(process);
        } else {
            Timeline arrivalTimer = new Timeline(new KeyFrame(Duration.seconds(process.getArrivalTime()), ev -> {
                allocateProcess(process);
                refreshView();
                if (process.getProcessingTime() > 0) scheduleEnd(process);
            }));
            arrivalTimer.setCycleCount(1);
            arrivalTimer.play();
        }
    }

    private void scheduleEnd(Process process) {
        Timeline endTimer = new Timeline(new KeyFrame(Duration.seconds(process.getProcessingTime()), ev -> {
            freeProcess(process.getName());
            refreshView();
        }));
        endTimer.setCycleCount(1);
        endTimer.play();
    }

    private void allocateProcess(Process process) {
        boolean allocated = tryAllocate(process);
        if (!allocated) {
            compactMemory();
            allocated = tryAllocate(process);
        }
        if (!allocated) waitingQueue.add(process);
    }

    private boolean tryAllocate(Process process) {
        for (int i = 0; i < memory.size(); i++) {
            Partition p = memory.get(i);
            if (p.isFree() && p.getSize() >= process.getSize()) {
                Partition allocatedPartition = new Partition(process.getName(), process.getSize(), randomColor());
                p.setSize(p.getSize() - process.getSize());
                if (p.getSize() == 0) memory.set(i, allocatedPartition);
                else memory.add(i, allocatedPartition);
                return true;
            }
        }
        return false;
    }


    private void freeProcess(String name) {
        for (int i = 0; i < memory.size(); i++) {
            Partition p = memory.get(i);
            if (!p.isFree() && p.getName().equals(name)) {
                p.setName("Free");
                p.setColor(Color.LIGHTGRAY);
                merge();
                compactMemory();
                allocateFromQueue();
                return;
            }
        }
    }

    private void allocateFromQueue() {
        if (waitingQueue.isEmpty()) return;

        LinkedList<Process> allocatedProcesses = new LinkedList<>();
        for (Process process : waitingQueue) {
            boolean allocated = false;
            for (int i = 0; i < memory.size(); i++) {
                Partition p = memory.get(i);
                if (p.isFree() && p.getSize() >= process.getSize()) {
                    Partition allocatedPartition = new Partition(process.getName(), process.getSize(), randomColor());
                    p.setSize(p.getSize() - process.getSize());
                    if (p.getSize() == 0) memory.set(i, allocatedPartition);
                    else memory.add(i, allocatedPartition);
                    allocated = true;

                    if (process.getProcessingTime() > 0) scheduleEnd(process);
                    break;
                }
            }
            if (allocated) allocatedProcesses.add(process);
        }
        waitingQueue.removeAll(allocatedProcesses);
        refreshView();
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
    private void setupCompactionTimer() {
        if (compactionTimer != null) compactionTimer.stop();
        try {
            int interval = Integer.parseInt(compactionField.getText().trim());
            if (interval <= 0) return;
            compactionTimer = new Timeline(new KeyFrame(Duration.seconds(interval), ev -> {
                compactMemory();
                refreshView();
            }));
            compactionTimer.setCycleCount(Timeline.INDEFINITE);
            compactionTimer.play();
        } catch (NumberFormatException ignored) {}
    }

    private void compactMemory() {
        LinkedList<Partition> allocated = new LinkedList<>();
        int freeSize = 0;
        for (Partition p : memory) {
            if (p.isFree()) freeSize += p.getSize();
            else allocated.add(p);
        }
        memory.clear();
        memory.addAll(allocated);
        if (freeSize > 0) memory.add(new Partition("Free", freeSize, Color.LIGHTGRAY));
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
            Label label = new Label(p.getName() + " (" + p.getSize() + " KB)");
            box.getChildren().add(label);
            memoryBox.getChildren().add(box);
        }

        // Show waiting queue
        queueBox.getChildren().clear();
        for (Process p : waitingQueue) {
            HBox box = new HBox();
            box.setPrefWidth(200);
            box.setStyle("-fx-border-color: gray; -fx-border-style: dashed;");
            Label label = new Label(p.getName() + " (" + p.getSize() + " KB)");
            box.getChildren().add(label);
            queueBox.getChildren().add(box);
        }
    }


    private Color randomColor() {
        return Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }
}
