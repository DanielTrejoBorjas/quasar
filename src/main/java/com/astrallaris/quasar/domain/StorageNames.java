package com.astrallaris.quasar.domain;

public final class StorageNames {

    private StorageNames() {
    }

    public static String canonicalKey(String key) {
        return key.replace('/', '-').replace('\\', '-');
    }

    public static boolean isSafeName(String name) {
        return !name.isBlank()
                && !name.equals(".")
                && !name.equals("..")
                && !name.contains("/")
                && !name.contains("\\");
    }
}
