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
    
    protected static boolean isItunesRunning() {
        // get running of application "iTunes"
        String[] args = { "osascript", "-e", "get running of application \"iTunes\"" };
        String result = softwareUtil.runCommand(args, null);
        return (result != null) ? Boolean.valueOf(result) : false;
    }

    protected static String itunesTrackScript = "tell application \"iTunes\"\n" +
            "set track_artist to artist of current track\n" +
            "set track_album to album of current track\n" +
            "set track_name to name of current track\n" +
            "set track_duration to duration of current track\n" +
            "set track_id to id of current track\n" +
            "set track_genre to genre of current track\n" +
            "set track_state to player state\n" +
            "set json to \"type='itunes';album='\" & track_album & \"';genre='\" & track_genre & \"';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='\" & track_state & \"';duration='\" & track_duration & \"'\"\n" +
            "end tell\n" +
            "return json\n";

    protected static String getItunesTrack() {
        String[] args = { "osascript", "-e", itunesTrackScript };
        return softwareUtil.runCommand(args, null);
    }

    protected static boolean isSpotifyRunning() {
        String[] args = { "osascript", "-e", "get running of application \"Spotify\"" };
        String result = softwareUtil.runCommand(args, null);
        return (result != null) ? Boolean.valueOf(result) : false;
    }

    protected static String spotifyTrackScript = "tell application \"Spotify\"\n" +
                "set track_artist to artist of current track\n" +
                "set track_album to album of current track\n" +
                "set track_name to name of current track\n" +
                "set track_duration to duration of current track\n" +
                "set track_id to id of current track\n" +
                "set track_state to player state\n" +
                "set json to \"type='spotify';album='\" & track_album & \"';genre='';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='\" & track_state & \"';duration='\" & track_duration & \"'\"\n" +
            "end tell\n" +
            "return json\n";

    protected static String getSpotifyTrack() {
        String[] args = { "osascript", "-e", spotifyTrackScript };
        return softwareUtil.runCommand(args, null);
    }

    public void processMusicTrack() {
        //
        // get the music track json string
        //
        JsonObject trackInfo = this.getCurrentMusicTrack();
        SoftwareUtil.TimesData timesData = softwareUtil.getTimesData();
        long local_start = timesData.local_now;
        String trackStr = null;
        String existingTrackId = (currentTrack.has("id")) ? currentTrack.get("id").getAsString() : null;

        if (trackInfo == null || !trackInfo.has("id") || !trackInfo.has("name")) {
            // end the existing track if the one coming is null
            if (existingTrackId != null) {
                // update the end time on the previous track and send it as well
                currentTrack.addProperty("end", timesData.now);
                // send the post to end the previous track
                trackStr = SoftwareUtil.gson.toJson(currentTrack);
                SoftwareResponse resp = softwareUtil.makeApiCall("/data/music", HttpPost.METHOD_NAME, trackStr);
                // song has ended, clear out the current track
                currentTrack = new JsonObject();
                if (resp == null || !resp.isOk()) {
                    String errorStr = (resp != null && resp.getErrorMessage() != null) ? resp.getErrorMessage() : "";
                    LOG.log(Level.INFO, "Code Time: Unable to get the music track response from the http request, error: {0}", errorStr);
                }
            }
            return;
        }

        SoftwareResponse response = null;

        String trackId = (trackInfo.has("id")) ? trackInfo.get("id").getAsString() : null;

        if (trackId != null && !trackId.contains("spotify") && !trackId.contains("itunes")) {
            // update it to itunes since spotify uses that in the id
            trackId = "itunes:track:" + trackId;
            trackInfo.addProperty("id", trackId);
        }
        
        boolean isSpotify = (trackId != null && trackId.contains("spotify"));
        if (isSpotify) {
            // convert the duration from milliseconds to seconds
            String durationStr = trackInfo.get("duration").getAsString();
            long duration = Long.parseLong(durationStr);
            int durationInSec = Math.round(duration / 1000);
            trackInfo.addProperty("duration", durationInSec);
        }
        String trackState = (trackInfo.get("state").getAsString());

        boolean isPaused = !(trackState.toLowerCase().equals("playing"));

        if (trackId != null) {

            if (existingTrackId != null && (!existingTrackId.equals(trackId) || isPaused)) {
                // update the end time on the previous track and send it as well
                currentTrack.addProperty("end", timesData.now);
                // send the post to end the previous track
                trackStr = SoftwareUtil.gson.toJson(currentTrack);
                // clear out the current track
                currentTrack = new JsonObject();
            }

            // if the current track doesn't have an "id" then a song has started
            if (!isPaused && (existingTrackId == null || !existingTrackId.equals(trackId))) {

                // send the post to send the new track info
                trackInfo.addProperty("start", timesData.now);
                trackInfo.addProperty("local_start", local_start);

                trackStr = SoftwareUtil.gson.toJson(trackInfo);

                // update the current track
                cloneTrackInfoToCurrent(trackInfo);
            }

        }

        if (trackStr != null) {
            SoftwareResponse resp = softwareUtil.makeApiCall(
                    "/data/music", HttpPost.METHOD_NAME, trackStr);
            if (resp == null || !resp.isOk()) {
                String errorStr = (resp != null && resp.getErrorMessage() != null) ? resp.getErrorMessage() : "";
                LOG.log(Level.INFO, "Code Time: Unable to get the music track response from the http request, error: {0}", errorStr);
            }
        }
    }


    private JsonObject getCurrentMusicTrack() {
        JsonObject jsonObj = new JsonObject();
        if (!softwareUtil.isMac()) {
            return jsonObj;
        }

        boolean spotifyRunning = isSpotifyRunning();
        boolean itunesRunning = isItunesRunning();

        String trackInfo = "";
        // Vintage Trouble, My Whole World Stopped Without You, spotify:track:7awBL5Pu8LD6Fl7iTrJotx, My Whole World Stopped Without You, 244080
        if (spotifyRunning) {
            trackInfo = getSpotifyTrack();
        } else if (itunesRunning) {
            trackInfo = getItunesTrack();
        }

        if (trackInfo != null && !trackInfo.equals("")) {
            // trim and replace things
            trackInfo = trackInfo.trim();
            trackInfo = trackInfo.replace("\"", "");
            trackInfo = trackInfo.replace("'", "");
            String[] paramParts = trackInfo.split(";");
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
