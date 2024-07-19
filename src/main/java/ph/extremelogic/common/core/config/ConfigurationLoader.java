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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;

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

public class ConfigurationLoader {
    private static final Log logger = LogFactory.getLog(ConfigurationLoader.class);

    public static final String DEFAULT_CONFIG_NAME = "config";
    public static final String ENCRYPTION_KEY_PROP = DEFAULT_CONFIG_NAME + ".encryption.key";
    public static final String ENCRYPTION_KEY_ENV = DEFAULT_CONFIG_NAME.toUpperCase() + "_ENCRYPTION_KEY";
    public static final String CONFIG_PROFILES_ACTIVE_PROP = DEFAULT_CONFIG_NAME + ".profiles.active";
    public static final String CONFIG_PROFILES_ACTIVE_ENV = DEFAULT_CONFIG_NAME.toUpperCase() + "_PROFILES_ACTIVE";

    private final Map<String, String> configuration = new HashMap<>();
    private Map<String, String> env = System.getenv();
    private PropertyEncryptor encryptor;
    private List<String> activeProfiles = new ArrayList<>();
    private String configName = DEFAULT_CONFIG_NAME;

    // Need this outside method because it slows down the method
    private final Pattern patternEnc = Pattern.compile("ENC\\((.*)\\)");
    private final Pattern patternVar = Pattern.compile("\\$\\{(.+?)\\}");
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
     *
     * @param name the name of the file to load (without extension)
     */
    public void loadProperties(String name) {
        var properties = new Properties();
        try (var input = getInputStream(name, ".properties")) {
            properties.load(input);
            for (var key : properties.stringPropertyNames()) {
                var value = properties.getProperty(key);
                configuration.put(key, decryptIfNeeded(value));
            }
        } catch (IOException e) {
            logger.warn("Unable to load " + name + ".properties " + e.getLocalizedMessage());
        }
    }

    public void loadProperties() {
        loadProperties(configName);
    }


    /**
     * Loads configuration from a YAML file.
     *
     * @param name the name of the file to load (without extension)
     */
    public void loadYaml(String name) {
        Yaml yaml = new Yaml();
        try (var input = getInputStream(name, ".yml")) {
            Map<String, Object> yamlMap = yaml.load(input);
            flattenMap("", yamlMap);
        } catch (IOException e) {
            logger.warn("Unable to load " + name + ".yml " + e.getLocalizedMessage());
        }
    }

    public void loadYaml() {
        loadYaml(configName);
    }

    /**
     * Loads configuration from all supported sources (properties, YAML, environment variables).
     */
    public void loadConfiguration(String[] args) {
        var name = configName;

        // Load default configurations
        loadProperties(name);
        loadYaml(name);

        var activeProfile = getConfigProfilesActiveByPrecedence(args);

        // Load active profiles
        if (activeProfile != null) {
            activeProfiles = Arrays.asList(activeProfile.split(","));
            for (var profile : activeProfiles) {
                var filename = name + "-" + profile.trim();

                loadProperties(filename);
                loadYaml(filename);
            }
            // Properties and Yaml calls above had probably
            // overwritten the CONFIG_PROFILES_ACTIVE
            configuration.put(CONFIG_PROFILES_ACTIVE_PROP, activeProfile);
        }

        // Load environment variables (higher precedence)
        loadEnvironmentVariables();

        // Load system properties (even higher precedence)
        loadSystemProperties();

        // Load command line arguments (highest precedence)
        if (args != null && args.length > 0) {
            loadCommandLineArguments(args);
        }

        // Resolve property placeholders
        resolvePlaceholders();
    }

    public void loadConfiguration() {
        loadConfiguration(null);
    }

    private String getConfigProfilesActiveByPrecedence(String[] args) {
        String profile = null;

        // Load command line arguments (highest precedence)
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--" + CONFIG_PROFILES_ACTIVE_PROP + "=")) {
                    profile = arg.substring(("--" + CONFIG_PROFILES_ACTIVE_PROP + "=").length());
                    break;
                }
            }
        }

        // Load system properties if previous is null
        if (profile == null) {
            profile = System.getProperty(CONFIG_PROFILES_ACTIVE_PROP);
        }

        // Load env variable if previous is null
        if (profile == null) {
            profile = env.get(CONFIG_PROFILES_ACTIVE_ENV);
        }

        // Return default if all above are null
        if (profile == null || profile.isBlank()) {
            profile = configuration.get(CONFIG_PROFILES_ACTIVE_PROP);
        }

        return profile;
    }

    /**
     * Injects configuration values into fields annotated with {@link Value}.
     *
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
     *
     * @param field the field to set
     * @param obj   the object on which to set the field
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
     *
     * @param prefix the current prefix for the keys
     * @param map    the map to flatten
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
     *
     * @param envVars the mock environment variables
     */
    protected void mockEnvironmentVariables(Map<String, String> envVars) {
        env = envVars;
    }

    /**
     * Converts an environment variable key to a property key format.
     *
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
     *
     * @param key the key of the property to retrieve
     * @return the value of the property, or null if not found
     */
    public String getProperty(String key) {
        return configuration.get(key);
    }

    /**
     * Gets an input stream for a configuration file. Loading initially from resource
     * alse from current directory.
     *
     * @param filename  the name of the file
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
     *
     * @param value the value to decrypt if needed
     * @return the decrypted value or the original value if not encrypted
     */
    private String decryptIfNeeded(String value) {
        var matcher = patternEnc.matcher(value);
        if (matcher.find()) {
            String encryptedValue = matcher.group(1);
            return decrypt(encryptedValue);
        }
        return value;
    }

    /**
     * Gets the encryption key from environment variables, system properties, or configuration.
     *
     * @return the encryption key
     * @throws IllegalStateException if the encryption key is not found
     */
    private String getEncryptionKey() {
        var encKey = System.getenv(ENCRYPTION_KEY_ENV);
        if (null != encKey) {
            return encKey;
        }

        encKey = configuration.get(ENCRYPTION_KEY_PROP);
        if (null != encKey) {
            return encKey;
        }
        throw new IllegalStateException("Encryption key not found. Please set " + ENCRYPTION_KEY_PROP);
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
     *
     * @param value the value to encrypt
     * @return the encrypted value wrapped in ENC()
     */
    public String encrypt(String value) {
        initializeEncryptor();
        return "ENC(" + encryptor.encrypt(value) + ")";
    }

    /**
     * Decrypts an encrypted value.
     *
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
        var matcher = patternVar.matcher(value);
        var sb = new StringBuilder();
        while (matcher.find()) {
            var key = matcher.group(1);
            var replacement = getProperty(key);
            matcher.appendReplacement(sb, replacement != null ? replacement : matcher.group());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }
}