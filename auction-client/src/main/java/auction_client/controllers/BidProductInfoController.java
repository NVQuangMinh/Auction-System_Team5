package auction_client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

public class BidProductInfoController {
    @FXML
    private CheckBox autoBidCheck;
    @FXML
    private HBox bidContainer;
    @FXML
    private HBox autoBidContainer;

    @FXML
    public void initialize() {
        bidContainer.managedProperty().bind(bidContainer.visibleProperty());
        autoBidContainer.managedProperty().bind(autoBidContainer.visibleProperty());
        autoBidContainer.visibleProperty().bind(autoBidCheck.selectedProperty());
        bidContainer.visibleProperty().bind(autoBidCheck.selectedProperty().not());
    }

}

