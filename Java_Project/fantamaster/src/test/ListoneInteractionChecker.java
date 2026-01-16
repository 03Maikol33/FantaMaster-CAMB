package it.camb.fantamaster.util;

import it.camb.fantamaster.controller.ListoneController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class ListoneInteractionChecker {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Listone interaction checker...");

        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(() -> {
            try {
                URL url = ListoneInteractionChecker.class.getResource("/fxml/listone.fxml");
                if (url == null) {
                    System.err.println("listone.fxml not found");
                    latch.countDown();
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();
                Object ctrl = loader.getController();

                if (ctrl instanceof ListoneController) {
                    ListoneController controller = (ListoneController) ctrl;

                    System.out.println("Loaded ListoneController, waiting 1s for init tasks...");

                    // wait a bit for async init to populate combo
                    Thread.sleep(1000);

                    Platform.runLater(() -> {
                        try {
                            // Access via getter instead of private field
                            if (controller.getRoleCombo() != null &&
                                controller.getRoleCombo().getItems().size() > 1) {

                                controller.getRoleCombo()
                                          .getSelectionModel()
                                          .select(1);

                                System.out.println("Selected role: " +
                                        controller.getRoleCombo()
                                                  .getSelectionModel()
                                                  .getSelectedItem());
                            }

                            if (controller.getMinPriceField() != null)
                                controller.getMinPriceField().setText("10");

                            if (controller.getMaxPriceField() != null)
                                controller.getMaxPriceField().setText("50");

                            // apply filters
                            controller.applyFilters();
                            System.out.println("Applied filters programmatically.");

                        } catch (Exception e) {
                            ErrorUtil.log("Errore durante il controllo delle interazioni", e);
                        } finally {
                            Platform.runLater(() -> {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    // 1. Ripristina lo stato di interruzione (FIX PER SONARQUBE)
                                    Thread.currentThread().interrupt();
                                    // 2. Logga l'evento o gestisci la chiusura
                                    System.err.println("Thread interrotto durante il controllo interazioni.");
                                }
                                latch.countDown();
                                Platform.exit();
                            });
                        }
                    });

                } else {
                    System.err.println("Controller is not ListoneController: " + ctrl);
                    latch.countDown();
                    Platform.exit();
                }

            } catch (Exception e) {
                ErrorUtil.log("Errore durante il controllo delle interazioni", e);
                latch.countDown();
                Platform.exit();
                // 1. Ripristina lo stato di interruzione (FIX PER SONARQUBE)
                Thread.currentThread().interrupt();
                
                // 2. Logga l'evento o gestisci la chiusura
                System.err.println("Thread interrotto durante il controllo interazioni.");
            }
        });

        latch.await();
        System.out.println("Checker finished.");
    }
}
