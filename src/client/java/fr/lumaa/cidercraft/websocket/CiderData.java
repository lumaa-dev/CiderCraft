package fr.lumaa.cidercraft.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lumaa.cidercraft.CiderCraft;
import fr.lumaa.cidercraft.data.CiderCraftConfig;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                URL url = new URI("http://localhost:10767/api/v1/playback/active").toURL();
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
            } catch (Exception e) {
                e.printStackTrace();
                CiderCraft.LOGGER.error("[CiderCraft] Received error from /v1/playback/active");
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

                Socket socket = IO.socket("http://localhost:10767", options);

                socket.on(Socket.EVENT_CONNECT, args -> CiderCraft.LOGGER.info("[CiderCraft] Socket.IO connected"));

                socket.on("API:Playback", args -> {
                    if (args.length > 0 && args[0] instanceof JSONObject playbackData) {
                        String type = playbackData.optString("type", "unknown");

                        switch (type) {
                            case "playbackStatus.nowPlayingItemDidChange":
                                try {
                                    this.currentTrack = this.findCurrentTrack(playbackData, "data");
                                    CiderCraft.LOGGER.info("[CiderCraft] Changed to new track: {}", this.currentTrack.title);
                                } catch (JSONException e) {
                                    CiderCraft.LOGGER.error("[CiderCraft] Error occured on \"" + type + "\" with data: " + playbackData);
                                    throw new RuntimeException(e);
                                }
                                break;
                            case "playbackStatus.playbackTimeDidChange":
                                if (this.currentTrack != null) {
                                    try {
                                        JSONObject data = playbackData.getJSONObject("data");
                                        float currentTime = (float) data.optDouble("currentPlaybackTime", 0.0);
                                        float duration = (float) data.optDouble("currentPlaybackDuration", 0.0);

                                        this.currentTrack.setTime(currentTime, duration);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                break;
                            case "playbackStatus.playbackStateDidChange":
                                try {
                                    JSONObject data = playbackData.getJSONObject("data");
                                    boolean playing = Objects.equals(data.optString("currentPlaybackTime", "unknown"), "playing");

                                    if (this.currentTrack == null) {
                                        this.currentTrack = this.findCurrentTrack(data, "attributes");
                                        CiderCraft.LOGGER.info("[CiderCraft] Detected current track: {}", this.currentTrack.title);
                                    }

                                    this.currentTrack.isPlaying = playing;
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            default:
//                                CiderCraft.LOGGER.info("[CiderCraft] Unhandled event type: {}", type);
                        }
                    } else {
                        CiderCraft.LOGGER.warn("[CiderCraft] Invalid playback data received");
                    }
                });

                socket.connect();
            } catch (URISyntaxException e) {
                CiderCraft.LOGGER.error("[CiderCraft] Failed to connect to Socket.IO server", e);
            }
        });
    }

    private Track findCurrentTrack(JSONObject object, String objName) throws JSONException {
        JSONObject attributes = object.getJSONObject(objName);
        String album = attributes.optString("albumName", "Unknown");
        String title = attributes.optString("name", "Unknown");
        String artist = attributes.optString("artistName", "Unknown");

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
