/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ResourceInfo {
    private String identifier = "";
    private String branch = "";
    private String tag = "";
    private String email = "";
    private String repoName = "";
    private String ownerId = "";
    private List<TeamMember> members = new ArrayList<>();

    public ResourceInfo clone() {
        ResourceInfo info = new ResourceInfo();
        info.setIdentifier(this.identifier);
        info.setBranch(this.branch);
        info.setTag(this.tag);
        info.setEmail(this.email);
        info.setMembers(this.members);
        return info;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public JsonArray getJsonMembers() {
        JsonArray jsonMembers = new JsonArray();
        for (TeamMember member : members) {
            JsonObject json = new JsonObject();
            json.addProperty("email", member.getEmail());
            json.addProperty("name", member.getName());
            jsonMembers.add(json);
        }
        return jsonMembers;
    }
}

