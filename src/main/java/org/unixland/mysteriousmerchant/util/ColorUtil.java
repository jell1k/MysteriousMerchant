package org.unixland.mysteriousmerchant.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }

        String result = input;
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(result);
        while (gradientMatcher.find()) {
            String startHex = gradientMatcher.group(1);
            String endHex = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            String replacement = applyGradient(text, startHex, endHex);
            result = result.substring(0, gradientMatcher.start()) + replacement + result.substring(gradientMatcher.end());
            gradientMatcher = GRADIENT_PATTERN.matcher(result);
        }

        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(sb, Matcher.quoteReplacement(toLegacyHex(hex)));
        }
        hexMatcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) {
            return "";
        }

        int[] start = hexToRgb(startHex);
        int[] end = hexToRgb(endHex);
        int length = text.length();
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < length; i++) {
            double ratio = length == 1 ? 0.0 : (double) i / (double) (length - 1);
            int r = (int) Math.round(start[0] + ((end[0] - start[0]) * ratio));
            int g = (int) Math.round(start[1] + ((end[1] - start[1]) * ratio));
            int b = (int) Math.round(start[2] + ((end[2] - start[2]) * ratio));
            out.append(toLegacyHex(String.format("%02X%02X%02X", r, g, b))).append(text.charAt(i));
        }

        return out.toString();
    }

    private static int[] hexToRgb(String hex) {
        int value = Integer.parseInt(hex, 16);
        return new int[]{(value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF};
    }

    private static String toLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            builder.append('§').append(c);
        }
        return builder.toString();
    }
}
