package fr.lumaa.cidercraft.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lumaa.cidercraft.CiderCraft;
import fr.lumaa.cidercraft.data.CiderCraftConfig;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CiderData {
    public String token;
    public String reqUrl;
    @Nullable public Track currentTrack = null;

    public CiderData(CiderCraftConfig config) {
        this.token = config.token;
        this.reqUrl = config.url;
    }

    public CompletableFuture<Boolean> isActive() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URI(this.reqUrl + "/api/v1/playback/active").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestProperty("apptoken", this.token);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStreamReader reader = new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
                    JsonObject jsonResponse = JsonParser.parseReader(reader).getAsJsonObject();
                    return "ok".equalsIgnoreCase(jsonResponse.get("status").getAsString());
                } else {
                    CiderCraft.LOGGER.error("[CiderCraft] Cider is currently inactive");
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public void connectWebSocket() {
        this.isActive().thenAccept(isActive -> {
            if (!isActive) {
                CiderCraft.LOGGER.error("[CiderCraft] Couldn't start WebSocket because Cider is inactive");
                return;
            }

            try {
                IO.Options options = new IO.Options();
                options.transports = new String[]{"websocket"};
                options.extraHeaders = Map.of("apptoken", List.of(this.token));

                Socket socket = IO.socket(this.reqUrl, options);

                socket.on(Socket.EVENT_CONNECT, args -> CiderCraft.LOGGER.info("[CiderCraft] Socket.IO connected"));

                socket.on("API:Playback", args -> {
                    if (args.length > 0 && args[0].toString() instanceof String jsonString) {
                        try {
                            JsonObject playbackData = JsonParser.parseString(jsonString).getAsJsonObject();
                            String type = playbackData.has("type") ? playbackData.get("type").getAsString() : "unknown";

                            switch (type) {
                                case "playbackStatus.nowPlayingItemDidChange" -> {
                                    this.currentTrack = this.findCurrentTrack(playbackData.getAsJsonObject("data"));
                                    CiderCraft.LOGGER.info("[CiderCraft] Changed to new track: {}", this.currentTrack.title);
                                }
                                case "playbackStatus.playbackTimeDidChange" -> {
                                    if (this.currentTrack != null && playbackData.has("data")) {
                                        JsonObject data = playbackData.getAsJsonObject("data");
                                        float currentTime = data.has("currentPlaybackTime") ? data.get("currentPlaybackTime").getAsFloat() : 0.0f;
                                        float duration = data.has("currentPlaybackDuration") ? data.get("currentPlaybackDuration").getAsFloat() : 0.0f;

                                        this.currentTrack.setTime(currentTime, duration);
                                    }
                                }
                                case "playbackStatus.playbackStateDidChange" -> {
                                    JsonObject data = playbackData.getAsJsonObject("data");
                                    boolean playing = data.has("currentPlaybackTime") && "playing".equals(data.get("currentPlaybackTime").getAsString());

                                    if (this.currentTrack == null) {
                                        this.currentTrack = this.findCurrentTrack(data.getAsJsonObject("attributes"));
                                        CiderCraft.LOGGER.info("[CiderCraft] Detected current track: {}", this.currentTrack.title);
                                    }

                                    this.currentTrack.isPlaying = playing;
                                }
                                default -> {
                                    // CiderCraft.LOGGER.info("[CiderCraft] Unhandled event type: {}", type);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        CiderCraft.LOGGER.warn("[CiderCraft] Invalid playback data received: " + args[0].toString());
                    }
                });

                socket.connect();
            } catch (URISyntaxException e) {
                CiderCraft.LOGGER.error("[CiderCraft] Failed to connect to Socket.IO server", e);
            }
        });
    }

    private Track findCurrentTrack(JsonObject object) {
        String album = object.get("albumName").getAsString();
        String title = object.get("name").getAsString();
        String artist = object.get("artistName").getAsString();

        return new Track(title, artist, album);
    }

    public class Track {
        public String title;
        public String artist;
        public String album;
        public float timePourcentage = 0;
        public boolean isPlaying = false;

        public Track(String title, String artist, String album) {
            this.title = title;
            this.artist = artist;
            this.album = album;
        }

        public void setTime(float currentTime, float totalTime) {
            this.timePourcentage = (currentTime / totalTime);
        }
    }
}
