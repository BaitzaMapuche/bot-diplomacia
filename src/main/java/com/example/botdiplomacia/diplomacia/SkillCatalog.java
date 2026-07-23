package com.example.botdiplomacia.diplomacia;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce entre los nombres cortos en español que usa el bot y los codigos
 * reales que espera la API de diplomacia.com.tr en el payload.
 */
public final class SkillCatalog {
    private static final Map<String, String> SKILL_CODE_BY_ALIAS = new LinkedHashMap<>();
    private static final Map<String, String> SKILL_DISPLAY_BY_CODE = new LinkedHashMap<>();
    private static final Map<String, String> RESOURCE_CODE_BY_ALIAS = new LinkedHashMap<>();
    private static final Map<String, String> RESOURCE_DISPLAY_BY_CODE = new LinkedHashMap<>();

    static {
        registerSkill("cuartel", "kisla", "Cuartel");
        registerSkill("guerra", "savas_teknikleri", "Guerra");
        registerSkill("cientifico", "bilim_insani", "Cientifico");

        registerResource("dinero", "money", "Dinero");
        registerResource("diamante", "diamond", "Diamante");
        registerResource("diamantes", "diamond", "Diamante");
    }

    private SkillCatalog() {
    }

    private static void registerSkill(String alias, String code, String display) {
        SKILL_CODE_BY_ALIAS.put(normalize(alias), code);
        SKILL_CODE_BY_ALIAS.put(normalize(code), code);
        SKILL_DISPLAY_BY_CODE.put(code, display);
    }

    private static void registerResource(String alias, String code, String display) {
        RESOURCE_CODE_BY_ALIAS.put(normalize(alias), code);
        RESOURCE_CODE_BY_ALIAS.put(normalize(code), code);
        RESOURCE_DISPLAY_BY_CODE.put(code, display);
    }

    /** Devuelve el codigo de skill que espera la API, o null si no se reconoce el alias. */
    public static String resolveSkillCode(String input) {
        return SKILL_CODE_BY_ALIAS.get(normalize(input));
    }

    /** Devuelve el codigo de recurso que espera la API, o null si no se reconoce el alias. */
    public static String resolveResourceCode(String input) {
        return RESOURCE_CODE_BY_ALIAS.get(normalize(input));
    }

    public static String skillDisplayName(String code) {
        return SKILL_DISPLAY_BY_CODE.getOrDefault(code, code);
    }

    public static String resourceDisplayName(String code) {
        return RESOURCE_DISPLAY_BY_CODE.getOrDefault(code, code);
    }

    public static String availableSkillsText() {
        return String.join(", ", SKILL_DISPLAY_BY_CODE.values());
    }

    public static String availableResourcesText() {
        return String.join(", ", RESOURCE_DISPLAY_BY_CODE.values());
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String stripped = Normalizer.normalize(input.strip().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped;
    }
}
