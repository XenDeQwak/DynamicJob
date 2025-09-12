module com.xen.rzlgame.dynamicjob {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.xen.dynamicjob to javafx.fxml;
    exports com.xen.dynamicjob;
}