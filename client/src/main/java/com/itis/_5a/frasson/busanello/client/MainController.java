package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MainController {

    @FXML
    private VBox adContainer;

    @FXML
    private WebView adWebView;

    private boolean isUserAuthenticated = false;

    @FXML
    public void initialize() {

        checkAuthentication();

        if (!isUserAuthenticated) {
            loadAds();
        } else {
            hideAds();
        }
    }

    private void checkAuthentication() {

        ClientInfo info=ClientInfo.getInstance();
        isUserAuthenticated=info.isValue();
    }

    private void loadAds() {
        String adHtml = """
                <!doctype html>
                <!--
                 @license
                 Copyright 2022 Google LLC. All Rights Reserved.
                 SPDX-License-Identifier: Apache-2.0
                -->
                <html>
                  <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <meta name="description" content="Display a fixed-sized test ad." />
                    <title>Display a test ad</title>
                    <script
                      async
                      src="https://securepubads.g.doubleclick.net/tag/js/gpt.js"
                      crossorigin="anonymous"
                    ></script>
                    <script>
                      window.googletag = window.googletag || { cmd: [] };
                
                      googletag.cmd.push(() => {
                        // Define an ad slot for div with id "banner-ad".
                        googletag
                          .defineSlot("/6355419/Travel/Europe/France/Paris", [300, 250], "banner-ad")
                          .addService(googletag.pubads());
                
                        // Enable the PubAdsService.
                        googletag.enableServices();
                      });
                    </script>
                    <style></style>
                  </head>
                  <body>
                    <div id="banner-ad" style="width: 300px; height: 250px"></div>
                    <script>
                      googletag.cmd.push(() => {
                        // Request and render an ad for the "banner-ad" slot.
                        googletag.display("banner-ad");
                      });
                    </script>
                  </body>
                </html>
                
                """;
        if (adWebView != null) {
            WebEngine webEngine = adWebView.getEngine();
            webEngine.loadContent(adHtml);
        }
    }

    private void hideAds() {

        adContainer.setVisible(false);
        adContainer.setManaged(false);
    }




    @FXML
    public void startNewGame() throws Exception {
        System.out.println("Avvio nuova partita...");
        SocketClient socketClient=SocketClient.getInstance();
        Message message=new Message("FMATCH");
        socketClient.sendAndReceive(Json.serializedMessage(message), Message.class);
    }

    @FXML
    public void loadGame() {
        System.out.println("Caricamento partita...");
    }

    @FXML
    public void openSettings() {
        System.out.println("Apertura impostazioni...");
    }
}
