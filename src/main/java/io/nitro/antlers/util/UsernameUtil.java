package io.nitro.antlers.util;

import java.text.Normalizer;

public class UsernameUtil {

    /**
     * Разрешаем ТОЛЬКО буквы кириллицы (любой поднабор — укр/рус и т.д.) и пробел.
     * Запрещаем цифры и любые другие символы. Пробелы схлопываем. NFKC + lower.
     */
    public static String normalizeCyrillicOnly(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC).toLowerCase().trim();
        s = s.replaceAll("\\s+", " ");            // один пробел
        s = s.replaceAll("[^\\p{IsCyrillic} ]", ""); // выкинуть всё, что не кириллица/пробел
        return s.trim();
    }

    /** После нормализации минимально нужно >=2 букв (пробелы не считаем). */
    public static boolean isValidDisplayName(String raw) {
        String norm = normalizeCyrillicOnly(raw);
        int letters = norm.replace(" ", "").length();
        return letters >= 2;
    }
}
