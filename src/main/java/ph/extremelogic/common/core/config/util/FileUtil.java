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
     * @throws ConfigurationException If an I/O error occurs while accessing the file.
     */
    public static InputStream getInputStream(String filename, String extension) throws ConfigurationException {
        InputStream input = FileUtil.class.getClassLoader().getResourceAsStream(filename + extension);
        if (input == null) {
            try {
                input = Files.newInputStream(Paths.get(filename + extension));
            } catch (ConfigurationException | IOException e) {
                throw new ConfigurationException(e);
            }
        }
        return input;
    }
}

