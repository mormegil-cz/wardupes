package cz.mormegil.wardupes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DuplicateDetector {
    public final Map<String, FileInfo> files = new HashMap<>();
    public final Map<String, List<FileInfo>> duplicates = new HashMap<>();

    private final ZipFile zipFile;

    private static final MessageDigest hasher;

    static {
        try {
            hasher = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public DuplicateDetector(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    public void addClass(ZipEntry entry, ZipEntry parent, String ignorePrefix, String classHash) throws IOException {
        String path = entry.getName();
        if (ignorePrefix != null && path.startsWith(ignorePrefix)) path = path.substring(ignorePrefix.length());
        final FileInfo fileInfo = new FileInfo(parent == null ? null : parent.getName(), path, classHash, entry.getSize());
        if (files.containsKey(path)) {
            final List<FileInfo> duplicateList = duplicates.computeIfAbsent(path, p -> initList(files.get(p)));
            duplicateList.add(fileInfo);
        } else {
            files.put(path, fileInfo);
        }
    }

    public void processJar(ZipEntry jarEntry, ZipEntry parent) throws IOException {
        try (final ZipInputStream jarStream = new ZipInputStream(zipFile.getInputStream(jarEntry))) {
            while (true) {
                final ZipEntry entry = jarStream.getNextEntry();
                if (entry == null) break;

                if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                    addClass(entry, jarEntry, null, computeHash(jarStream));
            }
        }
    }

    public static String computeHash(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (final InputStream stream = zipFile.getInputStream(entry)) {
            if (stream == null) {
                throw new IOException("No stream found for " + entry + " from " + zipFile.getName());
            }
            return computeHash(stream);
        }
    }

    public static String computeHash(InputStream stream) throws IOException {
        hasher.reset();
        return bytesToHex(hasher.digest(stream.readAllBytes()));
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private static String bytesToHex(byte[] bytes) {
        final byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.US_ASCII);
    }

    private static <T> List<T> initList(T value) {
        final ArrayList<T> result = new ArrayList<>();
        result.add(value);
        return result;
    }
}
