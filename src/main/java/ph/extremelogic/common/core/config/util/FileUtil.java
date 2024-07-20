package ph.extremelogic.common.core.config.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility class for file operations.
 */
public class FileUtil {
    private FileUtil() {
        // This class is not meant to be instantiated.
    }

    /**
     * Retrieves an InputStream for the given file name with the specified extension.
     * The method first attempts to load the file as a resource from the classpath.
     * If the resource is not found, it attempts to load it from the file system.
     *
     * @param filename  The name of the file (without extension).
     * @param extension The file extension (e.g., ".txt", ".properties").
     * @return An InputStream for reading the file.
     * @throws IOException If an I/O error occurs while accessing the file.
     */
    public static InputStream getInputStream(String filename, String extension) throws IOException {
        InputStream input = FileUtil.class.getClassLoader().getResourceAsStream(filename + extension);
        if (input == null) {
            input = Files.newInputStream(Paths.get(filename + extension));
        }
        return input;
    }
}

