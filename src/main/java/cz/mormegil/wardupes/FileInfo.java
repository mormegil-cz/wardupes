package cz.mormegil.wardupes;

public class FileInfo {
    public final String parentPath;
    public final String path;
    public final String hash;
    public final long size;

    public FileInfo(String parentPath, String path, String hash, long size) {
        this.parentPath = parentPath;
        this.path = path;
        this.hash = hash;
        this.size = size;
    }
}
