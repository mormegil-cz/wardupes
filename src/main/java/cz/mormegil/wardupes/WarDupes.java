package cz.mormegil.wardupes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WarDupes {
    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: WarDupes file.war");
            System.exit(2);
            return;
        }
        // WAR file:
        //  WEB-INF/classes → [class] (recursive)
        //  WEB-INF/lib → [JAR]
        //      JAR → [class]

        final ZipFile war = new ZipFile(args[0]);
        final DuplicateDetector detector = new DuplicateDetector(war);
        final ArrayList<? extends ZipEntry> warEntries = Collections.list(war.entries());

        warEntries.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("WEB-INF/classes/") && entry.getName().endsWith(".class"))
                .forEach(wrapExc((ZipEntry entry) -> detector.addClass(entry, null, "WEB-INF/classes/", DuplicateDetector.computeHash(war, entry))));
        warEntries.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("WEB-INF/lib/") && entry.getName().endsWith(".jar"))
                .forEach(wrapExc((ZipEntry entry) -> detector.processJar(entry, null)));

        System.out.println(String.format("%s processed. %d class(es), %d duplicate(s)", args[0], detector.files.size(), detector.duplicates.size()));
        System.out.println();
        for (final Map.Entry<String, List<FileInfo>> duplicates : detector.duplicates.entrySet()) {
            System.out.println(duplicates.getKey());
            for (final FileInfo duplicate : duplicates.getValue()) {
                System.out.println(String.format("\t%s\t%s\t%d bytes, %s", duplicate.parentPath, duplicate.path, duplicate.size, duplicate.hash));
            }
            System.out.println();
        }
    }

    private static <P> Consumer<P> wrapExc(ThrowingConsumer<P> func) {
        return (arg) -> {
            try {
                func.accept(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         */
        void accept(T t) throws Exception;
    }
}
