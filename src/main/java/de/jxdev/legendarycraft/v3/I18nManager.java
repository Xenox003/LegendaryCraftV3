package de.jxdev.legendarycraft.v3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18nManager {
    public I18nManager() {
        init();
    }

    public static void init() {
        TranslationStore.StringBased<MessageFormat> store = TranslationStore.messageFormat(Key.key("legendarycraft", "lang"));
        translateStores(
                store,
                Locale.GERMAN
        );
        GlobalTranslator.translator().addSource(store);
    }

    private static void translateStores(TranslationStore.StringBased<MessageFormat> store, Locale... locales) {
        for (Locale locale : locales) {
            translateStore(store, "lang.Bundle", locale);
        }
    }
    private static void translateStore(TranslationStore.StringBased<MessageFormat> store, String baseName, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, UTF8ResourceBundleControl.utf8ResourceBundleControl());
        store.registerAll(locale, bundle, true);
    }
}
