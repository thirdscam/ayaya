package dev.ptnr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Hitomi {
    private static final String cdnUrl = ".gold-usergeneratedcontent.net";
    private static final String baseCDNUrl = "https://ltn" + cdnUrl;

    private static ArrayList<String> getGalleryData(Integer galleryId) {
        String baseUrl = baseCDNUrl + "/galleries/" + galleryId + ".js";

        byte[] rawData = AyayaUtils.GetFileFromUrl(baseUrl);
        if  (rawData == null) {
            System.out.println("[ERR > Hitomi.GetGalleryData()] Failed to Get " + galleryId + ".js");
            return null;
        }
        String rawJson = new String(rawData).replace("var galleryinfo = ", "");

        ArrayList<String> galleryData = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode files = rootNode.get("files");

            for (JsonNode node : files) {
                String fileHash = node.get("hash").asText();

                galleryData.add(fileHash);
            }
        } catch (Exception e) {
            System.out.println("[ERR > Hitomi.GetGalleryData()] Failed to Parse JSON\n" + e.getMessage());
            return null;
        }

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

    private static String getImageUrl(String rawGG, String hash) {
        int defaultDomainKey = getIdFromGG("var o = (\\d)", rawGG) + 1;
        int offsetDomainKey = getIdFromGG("o = (\\d); break;", rawGG) + 1;
        int commonKey = getIdFromGG("b: '(\\d+)\\/'", rawGG);

        Map<Integer, Integer> Offsets = getOffsetsFromGG("case (\\d+):", rawGG, offsetDomainKey);
        int domainOffset = defaultDomainKey;

        // ex) hash == 415e8c1bfbac7cb6192b6f7c14768c196dde9029233126ca5f108d6aa99818de
        // 8de to slice hash[-1:] / hash[-3:-1] (e / 8d)
        // -> s == hash[-1:] + hash[-3:-1] == "e8d"
        // s to parseInt(s, 16) == 3725 == ImageId
        int imageId = Integer.parseInt(hash.substring(hash.length() - 1) + hash.substring(hash.length() - 3, hash.length() - 1), 16);
        if (Offsets.containsKey(imageId)) domainOffset = offsetDomainKey;

        // domain number (a1, a2 / w1, w2..)
        // webp = w1, w2 / avif = a1, a2

        return "https://w" + domainOffset + cdnUrl + "/" + commonKey + "/" + imageId + "/" + hash + ".webp";
    }

    public static String GetHitomiData(Integer galleryId) {
        ArrayList<String> galleryData = getGalleryData(galleryId);
        if (galleryData == null) {
            System.err.println("[ERR > Hitomi.GetHitomiData()] Failed to Get Gallery Data");
            return null;
        }

        String GGJS = getGGJS();
        if  (GGJS == null) {
            System.err.println("[ERR > Hitomi.GetHitomiData()] Failed to Get gg.js");
            return null;
        }

        ArrayList<String> images = new ArrayList<>();

        galleryData.forEach((hash) -> images.add(getImageUrl(GGJS, hash)));

        return images.get(0);
    }
}
