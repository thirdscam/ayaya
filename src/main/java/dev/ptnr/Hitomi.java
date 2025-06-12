package dev.ptnr;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Hitomi {
    private static final String cdnUrl = ".gold-usergeneratedcontent.net";
    private static final String baseCDNUrl = "https://ltn" + cdnUrl;

    private static HitomiDTO getGalleryData(Integer galleryId) {
        String baseUrl = baseCDNUrl + "/galleries/" + galleryId + ".js";
        String rawJson;

        JsonNode rootNode;
        ArrayList<String> imageHashList = new ArrayList<>();
        ArrayList<String> artists = new ArrayList<>();
        ArrayList<String> groups = new ArrayList<>();
        ArrayList<String> characters = new ArrayList<>();
        ArrayList<String> parodys = new ArrayList<>();
        ArrayList<String> femaleTag = new ArrayList<>();
        ArrayList<String> maleTag = new ArrayList<>();
        ArrayList<String> normalTag = new ArrayList<>();
        Map<String, ArrayList<String>> tags = new HashMap<String, ArrayList<String>>();

        byte[] rawData = AyayaUtils.GetFileFromUrl(baseUrl);
        if  (rawData == null) {
            System.out.println("[ERR > Hitomi.GetGalleryData()] Failed to Get " + galleryId + ".js");
            return null;
        }
        
        rawJson = new String(rawData, StandardCharsets.UTF_8).replace("var galleryinfo = ", "");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            rootNode = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            System.out.println("[ERR > Hitomi.GetGalleryData()] Failed to Parse JSON\n" + e.getMessage());
            return null;
        }

        for (JsonNode node : rootNode.get("files")) {
            String fileHash = node.get("hash").asText();

            imageHashList.add(fileHash);
        }

        for (JsonNode node : rootNode.get("tags")) {
            String tag = node.get("tag").asText();

            if (node.get("female") == null) {
                normalTag.add(tag);
                continue;
            }

            if (node.get("female").asInt() == 1) femaleTag.add(tag);
            if (node.get("male").asInt() == 1) maleTag.add(tag);
        }
        tags.put("female", femaleTag);
        tags.put("male", maleTag);
        tags.put("tag", normalTag);

        for (JsonNode node : rootNode.get("artists")) {
            String artist = node.get("artist").asText();

            artists.add(artist);
        }

        for (JsonNode node : rootNode.get("groups")) {
            String group = node.get("group").asText();

            groups.add(group);
        }

        for (JsonNode node : rootNode.get("characters")) {
            String character = node.get("character").asText();

            characters.add(character);
        }

        for (JsonNode node : rootNode.get("parodys")) {
            String parody = node.get("parody").asText();

            parodys.add(parody);
        }

        HitomiDTO galleryData = new HitomiDTO(rootNode.get("title").asText(),
                                                galleryId,
                                                artists,
                                                groups,
                                                characters,
                                                parodys,
                                                rootNode.get("type").asText(),
                                                rootNode.get("language").asText(),
                                                imageHashList,
                                                tags);

        return galleryData;
    }

    private static Integer getIdFromGG(String regex, String data) {
        Matcher matcher = Pattern.compile(regex).matcher(data);
        int regexData;
        matcher.find();
        regexData = Integer.parseInt(matcher.group(1));
        return regexData;
    }

    private static Map<Integer, Integer> getOffsetsFromGG(String regex, String data, Integer key) {
        Matcher matcher = Pattern.compile(regex).matcher(data);
        Map<Integer, Integer> offsets = new HashMap<>();
        while (matcher.find()) {
            offsets.put(Integer.parseInt(matcher.group(1)), key);
        }
        return offsets;
    }

    private static String getGGJS() {
        String baseUrl = baseCDNUrl + "/gg.js";
        byte[] rawJs = AyayaUtils.GetFileFromUrl(baseUrl);
        if (rawJs == null) {
            System.out.println("[ERR > Hitomi.GetGGJS()] Failed to Get gg.js");
            return null;
        }
        return new String(rawJs);
    }

    public static String getImageUrl(String hash) {
        String ggData = getGGJS();
        if (ggData == null) {
            System.err.println("[ERR > Hitomi.GetHitomiData()] Failed to Get gg.js");
            return null;
        }

        // ex) hash == 415e8c1bfbac7cb6192b6f7c14768c196dde9029233126ca5f108d6aa99818de
        // 8de to slice hash[-1:] / hash[-3:-1] (e / 8d)
        // -> s == hash[-1:] + hash[-3:-1] == "e8d"
        // s to parseInt(s, 16) == 3725 == ImageId
        int imageId = Integer.parseInt(hash.substring(hash.length() - 1) + hash.substring(hash.length() - 3, hash.length() - 1), 16);
        
        int defaultDomainKey = getIdFromGG("var o = (\\d)", ggData) + 1;
        int offsetDomainKey = getIdFromGG("o = (\\d); break;", ggData) + 1;
        int commonKey = getIdFromGG("b: '(\\d+)\\/'", ggData);

        Map<Integer, Integer> Offsets = getOffsetsFromGG("case (\\d+):", ggData, offsetDomainKey);
        
        int domainOffset = defaultDomainKey;
        if (Offsets.containsKey(imageId)) domainOffset = offsetDomainKey;

        // domain number (a1, a2 / w1, w2..)
        // webp = w1, w2 / avif = a1, a2
        return "https://w" + domainOffset + cdnUrl + "/" + commonKey + "/" + imageId + "/" + hash + ".webp";
    }

    public static HitomiDTO GetHitomiData(Integer galleryId) {
        HitomiDTO galleryData = getGalleryData(galleryId);
        if (galleryData == null) {
            System.err.println("[ERR > Hitomi.GetHitomiData()] Failed to Get Gallery Data");
            return null;
        }

        return galleryData;
    }

    public static void main(String[] args) {
        HitomiDTO test = GetHitomiData(3193561);
        System.out.println(test.getTagsAsString());
        System.out.println(test.getArtistsAsString());
        System.out.println(test.getGroupsAsString());
        System.out.println(test.getCharactersAsString());
        System.out.println(test.getParodysAsString());

        String downloadPath = "D:\\Hitomi_Test";
        int idx = 1;

        for (String hash : test.getImageHashList()) {
            File file = new File(downloadPath + "/" + test.getId());
            file.mkdirs();
            byte[] data = AyayaUtils.GetFileFromUrl(getImageUrl(hash));
            AyayaUtils.writeFile(downloadPath + "/" + test.getId() + "/" + idx + ".webp", data);
            ++idx;
        }
    }
}
