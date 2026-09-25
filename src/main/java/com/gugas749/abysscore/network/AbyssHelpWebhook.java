package com.gugas749.abysscore.network;

import com.gugas749.abysscore.AbysscoreServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class AbyssHelpWebhook {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void sendDCMessage(String playerName, String playerUUID, String reason) {
        if (!AbysscoreServerConfig.isHelpWebhookEnabled()) return;
        String DISCORD_WEBHOOK_URL = AbysscoreServerConfig.getHelpWebhookUrl();

        if (DISCORD_WEBHOOK_URL.isBlank()) {
            LOGGER.warn("[Abysshelp] Discord webhook URL not configured, skipping log.");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                String timestamp = Instant.now().toString();

                String payload = """
                    {
                        "embeds": [{
                            "title": "Pedido de ajuda - AbyssHelp",
                            "color": %d,
                            "fields": [
                                { "name": "Player", "value": "`%s`", "inline": true },
                                { "name": "UUID", "value": "`%s`", "inline": true },
                                { "name": "Reason", "value": "%s", "inline": false }
                            ],
                            "footer": { "text": "AbyssCore" },
                            "timestamp": "%s"
                        }]
                    }
                    """.formatted(
                        0x0000FF,
                        playerName,
                        playerUUID,
                        reason,
                        timestamp
                    );

                URL url = new URL(DISCORD_WEBHOOK_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    LOGGER.info("[Abysshelp] Discord webhook sent for player '{}'.", playerName);
                } else {
                    LOGGER.warn("[Abysshelp] Discord webhook returned unexpected status: {}", responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                LOGGER.warn("[Abysshelp] Failed to send Discord webhook: {}", e.getMessage());
            }
        });
    }
}
