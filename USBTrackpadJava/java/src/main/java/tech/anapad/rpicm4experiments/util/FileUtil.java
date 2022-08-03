package tech.anapad.rpicm4experiments.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * {@link FileUtil} contains utility functions for files.
 */
public final class FileUtil {

    public static void createDirectoryPath(String path) {
        new File(path).mkdirs();
    }

    public static void removePath(String path) {
        new File(path).delete();
    }

    public static void fileWithStringContents(String path, String contents) {
        try {
            Files.writeString(Paths.get(path), contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fileWithByteContents(String path, byte[] contents) {
        try {
            Files.write(Paths.get(path), contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
