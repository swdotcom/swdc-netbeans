/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpPost;

/**
 *
 * @author xavierluiz
 */
public class MusicManager {

    public static final Logger LOG = Logger.getLogger("MusicManager");

    private static final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();

    private static JsonObject currentTrack = new JsonObject();
    
    private static MusicManager instance = null;

    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }
    
    private MusicManager() {
        //
    }

    public void processMusicTrack() {
        //
        // get the music track json string
        //
        JsonObject trackInfo = this.getCurrentMusicTrack();

        if (trackInfo == null || !trackInfo.has("id") || !trackInfo.has("name")) {
            return;
        }

        SoftwareResponse response = null;

        String existingTrackId = (currentTrack.has("id")) ? currentTrack.get("id").getAsString() : null;
        String trackId = (trackInfo != null && trackInfo.has("id")) ? trackInfo.get("id").getAsString() : null;

        if (trackId != null && !trackId.contains("spotify") && !trackId.contains("itunes")) {
            // update it to itunes since spotify uses that in the id
            trackId = "itunes:track:" + trackId;
            trackInfo.addProperty("id", trackId);
        }

        Integer offset = ZonedDateTime.now().getOffset().getTotalSeconds();
        long now = Math.round(System.currentTimeMillis() / 1000);
        long local_start = now + offset;

        String trackStr = null;

        if (trackId != null) {

            if (existingTrackId != null && !existingTrackId.equals(trackId)) {
                // update the end time on the previous track and send it as well
                currentTrack.addProperty("end", now);
                // send the post to end the previous track
                trackStr = SoftwareUtil.gson.toJson(currentTrack);
            }

            // if the current track doesn't have an "id" then a song has started
            if (existingTrackId == null || !existingTrackId.equals(trackId)) {

                // send the post to send the new track info
                trackInfo.addProperty("start", now);
                trackInfo.addProperty("local_start", local_start);

                trackStr = SoftwareUtil.gson.toJson(trackInfo);

                // update the current track
                cloneTrackInfoToCurrent(trackInfo);
            }

        } else {
            if (existingTrackId != null) {
                // update the end time on the previous track and send it as well
                currentTrack.addProperty("end", now);
                // send the post to end the previous track
                trackStr = SoftwareUtil.gson.toJson(currentTrack);
            }

            // song has ended, clear out the current track
            currentTrack = new JsonObject();
        }

        if (trackStr != null) {
            SoftwareResponse resp = softwareUtil.makeApiCall(
                    "/data/music", HttpPost.METHOD_NAME, trackStr);
            if (resp == null || !resp.isOk()) {
                String errorStr = (resp != null && resp.getErrorMessage() != null) ? resp.getErrorMessage() : "";
                LOG.log(Level.INFO, "Software.com: Unable to get the music track response from the http request, error: {0}", errorStr);
            }
        }
    }

    private JsonObject getCurrentMusicTrack() {
        if (!softwareUtil.isMac()) {
            return new JsonObject();
        }
        String script
                = "on buildItunesRecord(appState)\n"
                + "tell application \"iTunes\"\n"
                + "set track_artist to artist of current track\n"
                + "set track_name to name of current track\n"
                + "set track_genre to genre of current track\n"
                + "set track_id to database ID of current track\n"
                + "set track_duration to duration of current track\n"
                + "set json to \"type='itunes';genre='\" & track_genre & \"';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='playing';duration='\" & track_duration & \"'\"\n"
                + "end tell\n"
                + "return json\n"
                + "end buildItunesRecord\n"
                + "on buildSpotifyRecord(appState)\n\n"
                + "tell application \"Spotify\"\n"
                + "set track_artist to artist of current track\n"
                + "set track_name to name of current track\n"
                + "set track_duration to duration of current track\n"
                + "set track_id to id of current track\n"
                + "set track_duration to duration of current track\n"
                + "set json to \"type='spotify';genre='';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='playing';duration='\" & track_duration & \"'\"\n"
                + "end tell\n"
                + "return json\n"
                + "end buildSpotifyRecord\n\n"
                + "try\n"
                + "if application \"Spotify\" is running and application \"iTunes\" is not running then\n"
                + "tell application \"Spotify\" to set spotifyState to (player state as text)\n"
                + "-- spotify is running and itunes is not\n"
                + "if (spotifyState is \"paused\" or spotifyState is \"playing\") then\n"
                + "set jsonRecord to buildSpotifyRecord(spotifyState)\n"
                + "else\n"
                + "set jsonRecord to {}\n"
                + "end if\n"
                + "else if application \"Spotify\" is running and application \"iTunes\" is running then\n"
                + "tell application \"Spotify\" to set spotifyState to (player state as text)\n"
                + "tell application \"iTunes\" to set itunesState to (player state as text)\n"
                + "-- both are running but use spotify as a higher priority\n"
                + "if spotifyState is \"playing\" then\n"
                + "set jsonRecord to buildSpotifyRecord(spotifyState)\n"
                + "else if itunesState is \"playing\" then\n"
                + "set jsonRecord to buildItunesRecord(itunesState)\n"
                + "else if spotifyState is \"paused\" then\n"
                + "set jsonRecord to buildSpotifyRecord(spotifyState)\n"
                + "else\n"
                + "set jsonRecord to {}\n"
                + "end if\n"
                + "else if application \"iTunes\" is running and application \"Spotify\" is not running then\n"
                + "tell application \"iTunes\" to set itunesState to (player state as text)\n"
                + "set jsonRecord to buildItunesRecord(itunesState)\n"
                + "else\n"
                + "set jsonRecord to {}\n"
                + "end if\n"
                + "return jsonRecord\n"
                + "on error\n"
                + "return {}\n"
                + "end try";

        String[] args = {"osascript", "-e", script};
        String trackInfoStr = softwareUtil.runCommand(args, null);
        // genre:Alternative, artist:AWOLNATION, id:6761, name:Kill Your Heroes, state:playing
        JsonObject jsonObj = new JsonObject();
        if (trackInfoStr != null && !trackInfoStr.equals("")) {
            // trim and replace things
            trackInfoStr = trackInfoStr.trim();
            trackInfoStr = trackInfoStr.replace("\"", "");
            trackInfoStr = trackInfoStr.replace("'", "");
            String[] paramParts = trackInfoStr.split(";");
            for (String paramPart : paramParts) {
                paramPart = paramPart.trim();
                String[] params = paramPart.split("=");
                if (params != null && params.length == 2) {
                    jsonObj.addProperty(params[0], params[1]);
                }
            }

        }
        return jsonObj;
    }

    private void cloneTrackInfoToCurrent(JsonObject trackInfo) {
        currentTrack = new JsonObject();
        currentTrack.addProperty("start", trackInfo.get("start").getAsLong());
        long end = (trackInfo.has("end")) ? trackInfo.get("end").getAsLong() : 0;
        currentTrack.addProperty("end", end);
        currentTrack.addProperty("local_start", trackInfo.get("local_start").getAsLong());
        JsonElement durationElement = (trackInfo.has("duration")) ? trackInfo.get("duration") : null;
        double duration = 0;
        if (durationElement != null) {
            String durationStr = durationElement.getAsString();
            duration = Double.parseDouble(durationStr);
            if (duration > 1000) {
                duration /= 1000;
            }
        }
        currentTrack.addProperty("duration", duration);
        String genre = (trackInfo.has("genre")) ? trackInfo.get("genre").getAsString() : "";
        currentTrack.addProperty("genre", genre);
        String artist = (trackInfo.has("artist")) ? trackInfo.get("artist").getAsString() : "";
        currentTrack.addProperty("artist", artist);
        currentTrack.addProperty("name", trackInfo.get("name").getAsString());
        String state = (trackInfo.has("state")) ? trackInfo.get("state").getAsString() : "";
        currentTrack.addProperty("state", state);
        currentTrack.addProperty("id", trackInfo.get("id").getAsString());
    }
}
