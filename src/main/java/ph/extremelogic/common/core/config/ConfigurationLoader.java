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

package ph.extremelogic.common.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

public class ConfigurationLoader {
    public static final String DEFAULT_CONFIG_NAME = "config";
    public static final String ENCRYPTION_KEY_PROPERTY = DEFAULT_CONFIG_NAME + ".encryption.key";
    public static final String ACTIVE_PROFILE_PROPERTY = DEFAULT_CONFIG_NAME + ".profiles.active";

    private final Map<String, String> configuration = new HashMap<>();
    private Map<String, String> env = System.getenv();
    private PropertyEncryptor encryptor;
    private List<String> activeProfiles = new ArrayList<>();

    /**
     * Annotation to mark fields for configuration value injection.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Value {
        String value();
    }

    /**
     * Loads properties from a .properties file.
     * @param filename the name of the file to load (without extension)
     * @throws IOException if an I/O error occurs
     */
    public void loadProperties(String filename) throws IOException {
        var properties = new Properties();
        try (var input = getInputStream(filename, ".properties")) {
            properties.load(input);
            for (var key : properties.stringPropertyNames()) {
                var value = properties.getProperty(key);
                configuration.put(key, decryptIfNeeded(value));
            }
        }
    }

    /**
     * Loads configuration from a YAML file.
     * @param filename the name of the file to load (without extension)
     * @throws IOException if an I/O error occurs
     */
    public void loadYaml(String filename) throws IOException {
        Yaml yaml = new Yaml();
        try (var input = getInputStream(filename, ".yml")) {
            Map<String, Object> yamlMap = yaml.load(input);
            flattenMap("", yamlMap);
        }
    }

    /**
     * Loads configuration from all supported sources (properties, YAML, environment variables).
     * @param name the base name of the configuration files to load
     * @throws IOException if an I/O error occurs
     */
    public void loadConfiguration(String name) throws IOException {
        // Load default configurations
        loadProperties(name);
        loadYaml(name);

        // Load active profiles
        String profilesStr = getProperty(ACTIVE_PROFILE_PROPERTY);
        if (profilesStr != null) {
            activeProfiles = Arrays.asList(profilesStr.split(","));
            for (String profile : activeProfiles) {
                var filename = name + "-" + profile.trim();
                loadProperties(filename);
                loadYaml(filename);
            }
        }

        // Load environment variables (higher precedence)
        loadEnvironmentVariables();

        // Load system properties (even higher precedence)
        loadSystemProperties();

        // Load command line arguments (highest precedence)
        //loadCommandLineArguments();

        // Resolve property placeholders
        resolvePlaceholders();
    }
    /**
     * Injects configuration values into fields annotated with {@link Value}.
     * @param obj the object to inject configuration into
     * @throws IllegalAccessException if the fields cannot be accessed
     */
    public void injectConfig(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        for (var field : clazz.getDeclaredFields()) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                var key = valueAnnotation.value().replace("${", "").replace("}", "");
                var value = getProperty(key);
                if (value != null) {
                    field.setAccessible(true);
                    setFieldValue(field, obj, value);
                }
            }
        }
    }

    /**
     * Sets the value of a field on the given object, converting the string value to the appropriate type.
     * @param field the field to set
     * @param obj the object on which to set the field
     * @param value the value to set
     * @throws IllegalAccessException if the field cannot be accessed
     */
    private void setFieldValue(Field field, Object obj, String value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            field.set(obj, value);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(obj, Integer.parseInt(value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(obj, Long.parseLong(value));
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(obj, Float.parseFloat(value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(obj, Double.parseDouble(value));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(obj, Boolean.parseBoolean(value));
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * Recursively flattens a nested map and adds its entries to the configuration map.
     * @param prefix the current prefix for the keys
     * @param map the map to flatten
     */
    private void flattenMap(String prefix, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            var key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                flattenMap(key, nestedMap);
            } else {
                configuration.put(key, decryptIfNeeded(entry.getValue().toString()));
            }
        }
    }

    /**
     * Loads environment variable to override configuration.
     */
    public void loadEnvironmentVariables() {
        for (var entry : env.entrySet()) {
            var key = convertEnvToPropertyKey(entry.getKey());
            if (key != null) {
                configuration.put(key, decryptIfNeeded(entry.getValue()));
            }
        }
    }

    /**
     * Mock environment variables for testing purposes.
     * @param envVars the mock environment variables
     */
    protected void mockEnvironmentVariables(Map<String, String> envVars) {
        env = envVars;
    }

    /**
     * Converts an environment variable key to a property key format.
     * @param envKey the environment variable key
     * @return the converted property key, or null if the key is not valid
     */
    private String convertEnvToPropertyKey(String envKey) {
            return envKey
                    .toLowerCase()
                    .replace("_", ".");
    }

    /**
     * Gets a configuration property value.
     * @param key the key of the property to retrieve
     * @return the value of the property, or null if not found
     */
    public String getProperty(String key) {
        return configuration.get(key);
    }

    /**
     * Gets an input stream for a configuration file. Loading initially from resource
     * alse from current directory.
     * @param filename the name of the file
     * @param extension the file extension
     * @return the input stream
     * @throws IOException if an I/O error occurs
     */
    private InputStream getInputStream(String filename, String extension) throws IOException {
        var input = getClass().getClassLoader().getResourceAsStream(filename + extension);
        if (input == null) {
            input = Files.newInputStream(Paths.get(filename + extension));
        }
        return input;
    }

    /**
     * Decrypts the value if it's encrypted, otherwise returns the original value.
     * @param value the value to decrypt if needed
     * @return the decrypted value or the original value if not encrypted
     */
    private String decryptIfNeeded(String value) {
        var pattern = Pattern.compile("ENC\\((.*)\\)");
        var matcher = pattern.matcher(value);
        if (matcher.find()) {
            String encryptedValue = matcher.group(1);
            return decrypt(encryptedValue);
        }
        return value;
    }

    /**
     * Gets the encryption key from environment variables, system properties, or configuration.
     * @return the encryption key
     * @throws IllegalStateException if the encryption key is not found
     */
    private String getEncryptionKey() {
        var key = System.getenv(ENCRYPTION_KEY_PROPERTY);
        if (key == null) {
            key = System.getProperty(ENCRYPTION_KEY_PROPERTY);
        }
        if (key == null) {
            key = configuration.get(ENCRYPTION_KEY_PROPERTY);
        }
        if (key == null) {
            throw new IllegalStateException("Encryption key not found. Please set " + ENCRYPTION_KEY_PROPERTY);
        }
        return key;
    }

    /**
     * Initializes the PropertyEncryptor with the encryption key.
     */
    private void initializeEncryptor() {
        if (encryptor == null) {
            encryptor = new PropertyEncryptor(getEncryptionKey());
        }
    }

    /**
     * Encrypts a value.
     * @param value the value to encrypt
     * @return the encrypted value wrapped in ENC()
     */
    public String encrypt(String value) {
        initializeEncryptor();
        return "ENC(" + encryptor.encrypt(value) + ")";
    }

    /**
     * Decrypts an encrypted value.
     * @param encryptedValue the encrypted value to decrypt
     * @return the decrypted value
     */
    private String decrypt(String encryptedValue) {
        initializeEncryptor();
        return encryptor.decrypt(encryptedValue);
    }

    public List<String> getActiveProfiles() {
        return activeProfiles;
    }

    private void loadSystemProperties() {
        var sysProps = System.getProperties();
        for (var key : sysProps.stringPropertyNames()) {
            configuration.put(key, decryptIfNeeded(sysProps.getProperty(key)));
        }
    }

    private void loadCommandLineArguments(String[] args) {
        for (var arg : args) {
            if (arg.startsWith("--")) {
                String[] keyValue = arg.substring(2).split("=", 2);
                if (keyValue.length == 2) {
                    configuration.put(keyValue[0], decryptIfNeeded(keyValue[1]));
                }
            }
        }
    }

    private void resolvePlaceholders() {
        for (var entry : configuration.entrySet()) {
            entry.setValue(resolvePlaceholder(entry.getValue()));
        }
    }

    private String resolvePlaceholder(String value) {
        if (value == null) return null;
        var pattern = Pattern.compile("\\$\\{(.+?)\\}");
        var matcher = pattern.matcher(value);
        var sb = new StringBuffer();
        while (matcher.find()) {
            var key = matcher.group(1);
            var replacement = getProperty(key);
            matcher.appendReplacement(sb, replacement != null ? replacement : matcher.group());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}