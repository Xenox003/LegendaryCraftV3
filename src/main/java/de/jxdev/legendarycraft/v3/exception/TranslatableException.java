package de.jxdev.legendarycraft.v3.exception;

import net.kyori.adventure.text.Component;

public class TranslatableException extends Exception {
    protected String errorTranslationKey;

    public TranslatableException(String translationKey) {
        super(translationKey);
        errorTranslationKey = translationKey;
    }

    public Component getChatComponent() {
        return Component.translatable(errorTranslationKey);
    }
}
