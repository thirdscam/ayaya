package dev.ptnr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Hitomi {
    private static String cdnUrl = ".gold-usergeneratedcontent.net";
    private static String baseCDNUrl = "https://ltn" + cdnUrl;

    private static Map<String, Boolean> getGalleryData(Integer galleryId) throws Exception {
        Map<String, Boolean> galleryData = new LinkedHashMap<>();

        String baseUrl = baseCDNUrl + "/galleries/" + galleryId + ".js";

        String rawData = new String(AyayaUtils.GetFileFromUrl(baseUrl));
        if  (rawData.length() == 3) {
            throw new Exception("Failed to Get JS (code " + rawData + ")");
        }

        rawData = rawData.replace("var galleryinfo = ", "");

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(rawData);
            JsonNode files = rootNode.get("files");

            for (JsonNode node : files) {
                String fileHash = node.get("hash").asText();
                Boolean hasAvif = node.get("hasavif").asBoolean();

                galleryData.put(fileHash, hasAvif);
            }
        } catch (JsonProcessingException e) {
            System.out.println("[ERR] In Hitomi.getGalleryData() > " + e.getMessage());
            throw e;
        }

        return galleryData;
    }

    private static String getIdFromGG(String regex, String data) {
        Matcher matcher = Pattern.compile(regex).matcher(data);
        String regexData = "";
        matcher.find();
        regexData = matcher.group(1);
        return regexData;
    }

    private static Map<String, Integer> getOffsetsFromGG(String regex, String data, Integer key) {
        Matcher matcher = Pattern.compile(regex).matcher(data);
        Map<String, Integer> offsets = new HashMap<>();
        while (matcher.find()) {
            offsets.put(matcher.group(1), key);
        }
        return offsets;
    }

    private static String getGGJS() {
        String baseUrl = baseCDNUrl + "/gg.js";

        try {
            return new String(AyayaUtils.GetFileFromUrl(baseUrl));
        } catch (Exception e) {
            System.out.println("[ERR] In Hitomi.GetGGJS() > " + e.getMessage());
            return "";
        }
    }

    private static String getImageUrl(String rawGG, String hash, Boolean hasAvif) {
        String DefaultSubdomainKey = getIdFromGG("var o = (\\d)", rawGG);
        String SubdomainKey = getIdFromGG("o = (\\d); break;", rawGG);
        String CommonImagePath = getIdFromGG("b: '(.+)'", rawGG);

        Map<String, Integer> Offsets = getOffsetsFromGG("case (\\d+):", rawGG, Integer.parseInt(SubdomainKey));

        String ImageUrl = "";
        Integer SubdomainOffset = Integer.parseInt(DefaultSubdomainKey) + 1;

        // ex) hash == 415e8c1bfbac7cb6192b6f7c14768c196dde9029233126ca5f108d6aa99818de
        // 8de to slice hash[-1:] / hash[-3:-1] (e / 8d)
        // -> s == hash[-1:] + hash[-3:-1] == "e8d"
        String SubKey = hash.substring(hash.length() - 1, hash.length()) + hash.substring(hash.length() - 3, hash.length() - 1);

        // s to parseInt(s, 16) == 3725 == ImageId
        Integer ImageId = Integer.parseInt(SubKey, 16);

        if (Offsets.containsKey(ImageId.toString())) SubdomainOffset = Integer.parseInt(SubdomainKey) + 1;

        // a{key}.gold-usergeneratedcontent.net/1749488401/3725/415e8c1bfbac7cb6192b6f7c14768c196dde9029233126ca5f108d6aa99818de.avif
        // domain number (a1, a2 / w1, w2..)
        // webp = w1, w2 / avif = a1, a2
        // in gg.js case -> o(SubdomainKey) + 1 == 1, out of ggjs case -> o(DefaultSubdomainKey) + 1 == 2
        // hasavif flag 1 -> a1, a2 / flag 0 -> w1, w2

        // if (hasAvif) ImageUrl = "https://a" + SubdomainOffset + cdnUrl + "/" + CommonImagePath + ImageId + "/" + hash + ".avif";
        // else ImageUrl = "https://w" + SubdomainOffset + cdnUrl + "/" + CommonImagePath + ImageId + "/" + hash + ".webp";
        ImageUrl = "https://w" + SubdomainOffset + cdnUrl + "/" + CommonImagePath + ImageId + "/" + hash + ".webp";

        // System.out.println("ImageUrl: " + ImageUrl);

        return ImageUrl;
    }

    public static String GetHitomiData(Integer galleryId) {
        Map<String, Boolean> galleryData = new LinkedHashMap<>();
        ArrayList<String> images = new ArrayList<>();

        try {
            galleryData = getGalleryData(galleryId);
        } catch (Exception e) {
            System.err.println("In Hitomi.main() > " + e.getMessage());
            return "";
        }

        String GGJS = getGGJS();

        galleryData.forEach((hash, hasAvif) -> {
            images.add(getImageUrl(GGJS, hash, hasAvif));
        });

        return images.get(0);
    }

    public static void main(String[] args) {
        // GetHitomiData(3390541);
    }
}
