/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ph.extremelogic.common.core.config.util;

import ph.extremelogic.common.core.config.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * The method first attempts to load the file as a resource from the classpath using
     * the current thread's context class loader. If the resource is not found, it attempts
     * to load it using the system class loader. If the resource is still not found, it
     * attempts to load it from the file system.
     *
     * @param filePath  The full path of the file (including filename and extension).
     * @return An InputStream for reading the file, or {@code null} if the file is not found.
     * @throws ConfigurationException If an I/O error occurs while accessing the file.
     */
    public static InputStream getInputStream(String filePath) throws ConfigurationException {
        InputStream input = null;
        try {
            Path path = Paths.get(filePath);
            String filename = path.getFileName().toString();

            // First, try to load from the current thread's context class loader
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

            // If not found, try to load from the system class loader
            if (input == null) {
                input = ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
            }

            // If still not found, try to load from the file system
            if (input == null) {
                return Files.newInputStream(path);
            }

            return input;
        } catch (IOException e) {
            throw new ConfigurationException("Error opening file: " + filePath, e);
        }
    }
}

