package cz.mormegil.wardupes;

public record FileInfo(String parentPath, String path, String hash, long size) {
}
