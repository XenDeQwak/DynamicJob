//logic was made possible by chatgpt and geeksforgeeks.com

package com.xen.dynamicjob;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

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
    @FXML
    private TableView<Process> jobsTable;
    @FXML
    private TableColumn<Process, String> nameCol;
    @FXML
    private TableColumn<Process, Integer> sizeCol;
    @FXML
    private TableColumn<Process, Integer> arrivalCol;
    @FXML
    private TableColumn<Process, Integer> durationCol;
    @FXML
    private Button scheduleJobsBtn;

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

        ObservableList<Process> initialJobsList = FXCollections.observableArrayList(
                new Process("Job1", 50, 0, 5),
                new Process("Job2", 20, 0, 4),
                new Process("Job3", 30, 0, 3),
                new Process("Job4", 70, 0, 2),
                new Process("Job5", 80, 0, 6),
                new Process("Job6", 20, 0, 7),
                new Process("Job7", 10, 0, 1),
                new Process("Job8", 30, 0, 3),
                new Process("Job9", 20, 0, 4),
                new Process("Job10", 50, 0, 5)
        );

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("processingTime"));

        jobsTable.setItems(initialJobsList);

        jobsTable.setEditable(true);
        sizeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        arrivalCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        durationCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        sizeCol.setOnEditCommit(e -> e.getRowValue().setSize(e.getNewValue()));
        arrivalCol.setOnEditCommit(e -> e.getRowValue().setArrivalTime(e.getNewValue()));
        durationCol.setOnEditCommit(e -> e.getRowValue().setProcessingTime(e.getNewValue()));



        addBtn.setOnAction(e -> {
            String name = processNameField.getText().trim();
            if (name.isEmpty()) return;

            boolean memorySizeHasValue = !memorySizeField.getText().trim().isEmpty();
            boolean otherFieldsHaveValue =
                    !processNameField.getText().trim().isEmpty() ||
                            !sizeField.getText().trim().isEmpty() ||
                            !arrivalField.getText().trim().isEmpty() ||
                            !durationField.getText().trim().isEmpty();

            if (memorySizeHasValue && otherFieldsHaveValue) {
                showError("Can't have a value in Memory Size when other fields also have values.");
                return;
            }


            try {
                Process process = getProcess(name);
                scheduleProcess(process);
            } catch (NumberFormatException ex) {
                showError("Size, arrival and compaction time, and processing time must be valid numbers.");
                sizeField.clear();
                arrivalField.clear();
                durationField.clear();
                compactionField.clear();
            }
        });

        scheduleJobsBtn.setOnAction(e -> {
            for (Process job : initialJobsList) {
                scheduleProcess(job);
            }
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
                memorySizeField.clear();
            } catch (NumberFormatException ex) {
                showError("Memory size must be a number.");
            }
        });


        compactionField.setOnAction(e -> setupCompactionTimer());
    }

    private Process getProcess(String name) {
        String sizeText = sizeField.getText().trim();
        String arrivalText = arrivalField.getText().trim();
        String durationText = durationField.getText().trim();

        if (sizeText.isEmpty() || arrivalText.isEmpty() || durationText.isEmpty())
            throw new NumberFormatException();

        int size = Integer.parseInt(sizeText);
        int arrival = Integer.parseInt(arrivalText);
        int duration = Integer.parseInt(durationText);

        Process process = new Process(name, size, arrival, duration);
        return process;
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
        } catch (NumberFormatException ex) {
            showError("Compaction interval must be a number.");
        }
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private Color randomColor() {
        return Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }
}
