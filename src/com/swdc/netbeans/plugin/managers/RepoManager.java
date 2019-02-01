/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonArray;
import com.swdc.netbeans.plugin.SoftwareUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

/**
 *
 */
public class RepoManager {

    public static final Logger LOG = Logger.getLogger("RepoManager");

    private static final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();

    private static RepoManager instance = null;

    public static RepoManager getInstance() {
        if (instance == null) {
            instance = new RepoManager();
        }
        return instance;
    }

    public JsonObject getLatestCommit(String projectDir) {
        JsonObject resource = softwareUtil.getResourceInfo(projectDir);
        if (resource.has("identifier")) {
            String identifier = resource.get("identifier").getAsString();
            String tag = (resource.has("tag")) ? resource.get("tag").getAsString() : "";
            String branch = (resource.has("branch")) ? resource.get("branch").getAsString() : "";

            try {
                String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
                String encodedTag = URLEncoder.encode(tag, "UTF-8");
                String encodedBranch = URLEncoder.encode(branch, "UTF-8");

                String qryString = "identifier=" + encodedIdentifier;
                qryString += "&tag=" + encodedTag;
                qryString += "&branch=" + encodedBranch;

                SoftwareResponse softwareResp =
                        softwareUtil.makeApiCall("/commits/latest?" + qryString, HttpGet.METHOD_NAME, null);
                if (softwareResp != null && softwareResp.isOk()) {
                    JsonObject payload = softwareResp.getJsonObj();
                    // will get a single commit object back with the following attributes
                    // commitId, message, changes, timestamp
                    JsonObject latestCommit = payload.get("commit").getAsJsonObject();
                    return latestCommit;
                } else {
                    LOG.log(Level.INFO, "Code Time: Unable to fetch latest commit info");
                }
            } catch (UnsupportedEncodingException e) {
                //
            }

        }

        return null;
    }

    public void getHistoricalCommits(String projectDir) {
        JsonObject resource = softwareUtil.getResourceInfo(projectDir);
        if (resource.has("identifier")) {
            String identifier = resource.get("identifier").getAsString();
            String tag = (resource.has("tag")) ? resource.get("tag").getAsString() : "";
            String branch = (resource.has("branch")) ? resource.get("branch").getAsString() : "";
            String email = (resource.has("email")) ? resource.get("email").getAsString() : "";

            JsonObject latestCommit = getLatestCommit(projectDir);

            String sinceOption = null;
            if (latestCommit != null && latestCommit.has("timestamp")) {
                long unixTs = latestCommit.get("timestamp").getAsLong();
                sinceOption = "--since=" + unixTs;
            }

            String authorOption = "--author=" + email;
            List<String> cmdList = new ArrayList<>();
            cmdList.add("git");
            cmdList.add("log");
            cmdList.add("--stat");
            cmdList.add("--pretty=COMMIT:%H,%ct,%cI,%s");
            cmdList.add(authorOption);
            if (sinceOption != null) {
                cmdList.add(sinceOption);
            }

            String[] commitHistoryCmd = Arrays.copyOf(cmdList.toArray(), cmdList.size(), String[].class);
            String historyContent = softwareUtil.runCommand(commitHistoryCmd, projectDir);

            if (historyContent == null || historyContent.isEmpty()) {
                return;
            }

            String latestCommitId = (latestCommit != null && latestCommit.has("commitId"))
                    ? latestCommit.get("commitId").getAsString() : null;

            // split the content
            JsonArray commits = new JsonArray();
            JsonObject commit = null;
            String[] historyContentList = historyContent.split("\n");
            if (historyContentList != null && historyContentList.length > 0) {
                for (String line : historyContentList) {
                    line = line.trim();
                    if (line.indexOf("COMMIT:") == 0) {
                        line = line.substring("COMMIT:".length());
                        if (commit != null) {
                            commits.add(commit);
                        }
                        // split by comma
                        String[] commitInfos = line.split(",");
                        if (commitInfos != null && commitInfos.length > 3) {
                            String commitId = commitInfos[0].trim();
                            if (latestCommitId != null && commitId.equals(latestCommitId)) {
                                commit = null;
                                // go to the next one
                                continue;
                            }
                            long timestamp = Long.valueOf(commitInfos[1].trim());
                            String date = commitInfos[2].trim();
                            String message = commitInfos[3].trim();
                            commit = new JsonObject();
                            commit.addProperty("commitId", commitId);
                            commit.addProperty("timestamp", timestamp);
                            commit.addProperty("date", date);
                            commit.addProperty("message", message);
                            JsonObject sftwTotalsObj = new JsonObject();
                            sftwTotalsObj.addProperty("insertions", 0);
                            sftwTotalsObj.addProperty("deletions", 0);
                            JsonObject changesObj = new JsonObject();
                            changesObj.add("__sftwTotal__", sftwTotalsObj);
                            commit.add("changes", changesObj);
                        }
                    } else if (commit != null && line.contains("|")) {
                        line = line.replaceAll("\\s+", " ");
                        String[] lineInfos = line.split("|");

                        if (lineInfos != null && lineInfos.length > 1) {
                            String file = lineInfos[0].trim();
                            String metricsLine = lineInfos[1].trim();
                            String[] metricInfos = metricsLine.split(" ");
                            if (metricInfos != null && metricInfos.length > 1) {
                                String addAndDeletes = metricInfos[1].trim();
                                // count the number of plus signs and negative signs to find
                                // out how many additions and deletions per file
                                int len = addAndDeletes.length();
                                int lastPlusIdx = addAndDeletes.lastIndexOf("+");
                                int insertions = 0;
                                int deletions = 0;
                                if (lastPlusIdx != -1) {
                                    insertions = lastPlusIdx + 1;
                                    deletions = len - insertions;
                                } else if (len > 0) {
                                    // all deletions
                                    deletions = len;
                                }
                                JsonObject fileChanges = new JsonObject();
                                fileChanges.addProperty("insertions", insertions);
                                fileChanges.addProperty("deletions", deletions);
                                JsonObject changesObj = commit.get("changes").getAsJsonObject();
                                changesObj.add(file, fileChanges);

                                JsonObject swftTotalsObj = changesObj.get("__sftwTotal__").getAsJsonObject();
                                int insertionTotal = swftTotalsObj.get("insertions").getAsInt() + insertions;
                                int deletionsTotal = swftTotalsObj.get("deletions").getAsInt() + deletions;
                                swftTotalsObj.addProperty("insertions", insertionTotal);
                                swftTotalsObj.addProperty("deletions", deletionsTotal);
                            }
                        }
                    }
                }

                if (commit != null) {
                    commits.add(commit);
                }

                if (commits.size() > 0) {
                    // send it in batches of 100
                    JsonArray batch = new JsonArray();
                    for (int i = 0; i < commits.size(); i++) {
                        batch.add(commits.get(i));
                        if (i > 0 && i % 100 == 0) {
                            this.processCommits(batch, identifier, tag, branch);
                            batch = new JsonArray();
                        }
                    }
                    if (batch.size() > 0) {
                        this.processCommits(batch, identifier, tag, branch);
                    }
                }
            }
        }
    }

    private void processCommits(JsonArray commits, String identifier, String tag, String branch) {
        try {

            // send the commits
            JsonObject commitData = new JsonObject();
            commitData.add("commits", commits);
            commitData.addProperty("identifier", identifier);
            commitData.addProperty("tag", tag);
            commitData.addProperty("branch", branch);
            String commitDataStr = commitData.toString();

            SoftwareResponse softwareResp =
                    softwareUtil.makeApiCall("/commits", HttpPost.METHOD_NAME, commitDataStr);

            if (softwareResp != null) {

                // {"status":"success","message":"Updated commits"}
                // {"status":"failed","data":"Unable to process commits data"}
                JsonObject responseObj = softwareResp.getJsonObj();
                String message = "";
                if (responseObj.has("data")) {
                    JsonObject data = responseObj.get("data").getAsJsonObject();
                    message = data.get("message").getAsString();
                } else if (responseObj.has("message")) {
                    message = responseObj.get("message").getAsString();
                }

                LOG.log(Level.INFO, "Code Time: completed commits update - {0}", message);
            } else {
                LOG.log(Level.INFO, "Code Time: Unable to process repo commits");
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Code Time: Unable to process repo commits, error: {0}", e.getMessage());
        }
    }

    public void processRepoMembersInfo(final String projectDir) {
        JsonObject resource = softwareUtil.getResourceInfo(projectDir);
        if (resource.has("identifier")) {
            String identifier = resource.get("identifier").getAsString();
            String tag = (resource.has("tag")) ? resource.get("tag").getAsString() : "";
            String branch = (resource.has("branch")) ? resource.get("branch").getAsString() : "";

            String[] identifierCmd = {"git", "log", "--pretty=%an,%ae"};
            String devOutput = softwareUtil.runCommand(identifierCmd, projectDir);

            // String[] devList = devOutput.replace(/\r\n/g, "\r").replace(/\n/g,"\r").split(/\r/);
            String[] devList = devOutput.split("\n");
            JsonArray members = new JsonArray();
            Map<String, String> memberMap = new HashMap<>();
            if (devList != null && devList.length > 0) {
                for (String line : devList) {
                    String[] parts = line.split(",");
                    if (parts != null && parts.length > 1) {
                        String name = parts[0].trim();
                        String email = parts[1].trim();
                        if (!memberMap.containsKey(email)) {
                            memberMap.put(email, name);
                            JsonObject json = new JsonObject();
                            json.addProperty("email", email);
                            json.addProperty("name", name);
                            members.add(json);
                        }
                    }
                }
            }

            if (members.size() > 0) {
                // send the members
                try {
                    JsonObject repoData = new JsonObject();
                    repoData.add("members", members);
                    repoData.addProperty("identifier", identifier);
                    repoData.addProperty("tag", tag);
                    repoData.addProperty("branch", branch);
                    String repoDataStr = repoData.toString();
                    
                    SoftwareResponse softwareResp =
                            softwareUtil.makeApiCall("/repo/members", HttpPost.METHOD_NAME, repoDataStr);
                    if (softwareResp.isOk()) {
                        LOG.log(Level.INFO, "Code Time: Completed sending repo member info");
                    } else {
                        LOG.log(Level.WARNING, "Code Time: Unable to process repo member metrics, error: {0}", softwareResp.getErrorMessage());
                    }
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Code Time: Unable to process repo member metrics, error: {0}", e.getMessage());
                }
            }
        }
    }
}
