package dev.ptnr;

import java.util.ArrayList;
import java.util.Map;

public class HitomiDTO {
    private String title;
    private int id;
    private ArrayList<String> artists;
    private String type;
    private String language;
    private ArrayList<String> imageHashList;
    private Map<String, ArrayList<String>> tags;

    public HitomiDTO() {}

    public HitomiDTO(String title, int id, ArrayList<String> artists, String type, String language, ArrayList<String> imageHashList, Map<String, ArrayList<String>> tags) {
        this.title = title;
        this.id = id;
        this.artists = artists;
        this.type = type;
        this.language = language;
        this.imageHashList = imageHashList;
        this.tags = tags;
    }

    public String getTagsAsString() {
        String tags = "";
        if (!this.tags.get("female").isEmpty()) {
            for (String tag : this.tags.get("female")) tags += "female:" + tag + ", ";
        }
        if (!this.tags.get("male").isEmpty()) {
            for (String tag : this.tags.get("male")) tags += "male:" + tag + ", ";
        }
        if (!this.tags.get("tag").isEmpty()) {
            for (String tag : this.tags.get("tag")) tags += "tag:" + tag + ", ";
        }

        if (tags.isEmpty()) return "N/A";

        return tags.substring(0, tags.length() - 2);
    }

    public String getArtistsAsString() {
        String artists = "";
        if (this.artists.isEmpty()) return "N/A";
        if (this.artists.size() == 1) return this.artists.get(0);
        
        for (String artist : this.artists) artists += artist + ", ";
        return artists.substring(0, artists.length() - 2);
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }

    public void setArtists(ArrayList<String> artists) {
        this.artists = artists;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public ArrayList<String> getImageHashList() {
        return imageHashList;
    }

    public void setImageHashList(ArrayList<String> imageHashList) {
        this.imageHashList = imageHashList;
    }

    public Map<String, ArrayList<String>> getTags() {
        return tags;
    }

    public void setTags(Map<String, ArrayList<String>> tags) {
        this.tags = tags;
    }
}
