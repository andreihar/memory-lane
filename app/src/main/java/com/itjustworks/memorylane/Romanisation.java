package com.itjustworks.memorylane;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/*
 * Romanisation.java
 * 
 * Class Description: Converts Chinese characters to transliteration systems.
 * Class Invariant: Transliteration system depends on the default language of the system.
 *                  Jyutping for zh-HK.
 *                  Zhuyin for zh-TW.
 *                  Pinyin for zh of other regions.
 *
 */

public class Romanisation {
    private final boolean romanisationOn;
    public Romanisation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        romanisationOn = prefs.getBoolean("romanisation_on", false);
    }
    public String input(String input, Activity context) {
        if (!Locale.getDefault().getLanguage().equals("zh") || !romanisationOn)
            return input;
        switch(Locale.getDefault().getCountry()) {
            case "HK":
            case "MO":
                return jyutping(input, context);
            case "TW":
                return zhuyin(input, context);
            default:
                return pinyin(input, context, false);
        }
    }

    private String jyutping(String input, Activity context) {
        StringBuilder converted = new StringBuilder();
        try {
            InputStream is = context.getAssets().open("jyutping_dictionary.json");
            int size = is.available();
            byte[] bufferData = new byte[size];
            is.read(bufferData);
            is.close();

            JSONObject jsonObject = new JSONObject(new String(bufferData, StandardCharsets.UTF_8));
            for (int i = 0; i < input.length(); i++) {
                String hex = String.format("%04x", (int)input.charAt(i));
                if (jsonObject.has(hex.toUpperCase())) {
                    String jyutping = (String) jsonObject.get(hex.toUpperCase());
                    converted = new StringBuilder(converted.toString().concat(" " + jyutping + " "));
                }
                else
                    converted.append(input.charAt(i));
            }
        } catch (Exception e) {e.printStackTrace();}
        converted = new StringBuilder(converted.toString().replace("  ", " "));
        converted = new StringBuilder(converted.toString().replace(" ？", "?"));
        return converted.toString().trim();
    }

    private String pinyin(String input, Activity context, boolean zhuyin) {
        StringBuilder converted = new StringBuilder();
        try {
            InputStream is = context.getAssets().open("pinyin_characters.json");
            // Load Hanzi to Pinyin
            int size = is.available();
            byte[] bufferData = new byte[size];
            is.read(bufferData);
            is.close();
            JSONObject characters = new JSONObject(new String(bufferData, StandardCharsets.UTF_8));

            // Load Hanzi words to Pinyin
            is = context.getAssets().open("pinyin_words.json");
            size = is.available();
            bufferData = new byte[size];
            is.read(bufferData);
            is.close();
            JSONObject words = new JSONObject(new String(bufferData, StandardCharsets.UTF_8));
            while (!input.isEmpty()) {
                int j;
                for (j = 4; j >= 2; j--) {
                    if (input.length() >= j) {
                        String word = input.substring(0, j);
                        if (words.has(word)) {
                            word = (String) words.get(word);
                            if (!zhuyin)
                                word = word.replaceAll(" ", "");
                            else
                                word = word.toLowerCase();
                            converted.append(" ").append(word).append(" ");
                            break;
                        }
                    }
                }
                if (j == 1) {
                    String hex = String.valueOf(input.charAt(0));
                    if (characters.has(hex)) {
                        String pinyin = (String) characters.get(hex);
                        converted = new StringBuilder(converted.toString().concat(" " + pinyin + " "));
                    }
                    else
                        converted.append(input.charAt(0));
                }
                if (input.length() == 1)
                    input = "";
                else
                    input = input.substring(j);
            }
        } catch (Exception e) {e.printStackTrace();}
        converted = new StringBuilder(converted.toString().replace("  ", " "));
        converted = new StringBuilder(converted.toString().replace(" ？", "?"));
        return converted.toString().trim();
    }

    private String zhuyin(String input, Activity context) {
        StringBuilder pinyin = new StringBuilder();
        String converted = "";
        try {
            InputStream is = context.getAssets().open("traditional.json");
            int size = is.available();
            byte[] bufferData = new byte[size];
            is.read(bufferData);
            is.close();

            JSONObject jsonObject = new JSONObject(new String(bufferData, StandardCharsets.UTF_8));
            for (int i = 0; i < input.length(); i++) {
                String hex = String.valueOf(input.charAt(i));
                if (jsonObject.has(hex)) {
                    String simplified = (String) jsonObject.get(hex);
                    pinyin = new StringBuilder(pinyin.toString().concat(simplified));
                }
                else
                    pinyin.append(input.charAt(i));
            }

            pinyin = new StringBuilder(pinyin(pinyin.toString(), context, true) + " ");
            pinyin = new StringBuilder(pinyin.toString().replace("?", " ?"));
            pinyin = new StringBuilder(pinyin.toString().replace(",", " ,"));
            is = context.getAssets().open("zhuyin.json");
            size = is.available();
            bufferData = new byte[size];
            is.read(bufferData);
            is.close();
            jsonObject = new JSONObject(new String(bufferData, StandardCharsets.UTF_8));

            while (pinyin.length() > 0) {
                String sub= pinyin.substring(0, pinyin.indexOf(" "));
                pinyin = new StringBuilder(pinyin.substring(pinyin.indexOf(" ") + 1));
                sub = zhuyinTones(sub);
                String tone = "";
                if (sub.contains("2"))
                    tone = "ˊ";
                else if (sub.contains("3"))
                    tone = "ˇ";
                else if (sub.contains("4"))
                    tone = "ˋ";
                else if (sub.contains("5"))
                    tone = "˙";
                if (jsonObject.has(sub.substring(0, sub.length()-1))) {
                    sub = (String) jsonObject.get(sub.substring(0, sub.length()-1));
                    if (tone.equals("˙"))
                        sub = tone+sub;
                    else
                        sub += tone;
                } else {
                    sub = sub.substring(0, sub.length()-1);
                }
                converted = converted.concat(sub + " ");
            }
        } catch (Exception e) {e.printStackTrace();}
        converted = converted.replace(" ?", "?");
        converted = converted.replace(" ,", ",");
        converted = converted.replace("  ", " ");
        return converted;
    }

    private String zhuyinTones(String input) {
        if (input.contains("á") || input.contains("é") || input.contains("í") || input.contains("ó") || input.contains("ú") || input.contains("ǘ"))
            input += "2";
        else if (input.contains("ǎ") || input.contains("ě") || input.contains("ǐ") || input.contains("ǒ") || input.contains("ǔ") || input.contains("ǚ"))
            input += "3";
        else if (input.contains("à") || input.contains("è") || input.contains("ì") || input.contains("ò") || input.contains("ù") || input.contains("ǜ"))
            input += "4";
        else if (input.contains("a") || input.contains("e") || input.contains("i") || input.contains("o") || input.contains("u") || input.contains("ü"))
            input += "5";
        else
            input += " ";
        input = input.replaceAll("[āáǎà]", "a");
        input = input.replaceAll("[ēéěè]", "e");
        input = input.replaceAll("[īíǐì]", "i");
        input = input.replaceAll("[ōóǒò]", "o");
        input = input.replaceAll("[ūúǔù]", "u");
        input = input.replaceAll("[ǖǘǚǜ]", "ü");
        return input;
    }
}
