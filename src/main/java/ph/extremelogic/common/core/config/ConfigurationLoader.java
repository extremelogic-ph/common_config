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
import ph.extremelogic.common.core.config.encrypt.DefaultPropertyEncryptor;
import ph.extremelogic.common.core.config.encrypt.PropertyEncryptor;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.regex.Pattern;

import static ph.extremelogic.common.core.config.util.FileUtil.getInputStream;
import static ph.extremelogic.common.core.config.util.ReflectiveValueSetter.setFieldValue;

/**
 * The ConfigurationLoader class is responsible for loading configuration properties
 * from various sources such as YAML files, environment variables, and system properties.
 * It also supports placeholder resolution and property encryption.
 * <p>
 * This class provides methods to set custom configuration names, property encryptors,
 * and behavior for handling exceptions during configuration loading.
 * </p>
 */
public class ConfigurationLoader {
    private static final Log logger = LogFactory.getLog(ConfigurationLoader.class);

    public static final String DEFAULT_CONFIG_NAME = "config";
    public static final String ENCRYPTION_KEY_PROP = DEFAULT_CONFIG_NAME + ".encryption.key";
    public static final String ENCRYPTION_KEY_ENV = DEFAULT_CONFIG_NAME.toUpperCase() + "_ENCRYPTION_KEY";
    public static final String CONFIG_PROFILES_ACTIVE_PROP = DEFAULT_CONFIG_NAME + ".profiles.active";
    public static final String CONFIG_PROFILES_ACTIVE_ENV = DEFAULT_CONFIG_NAME.toUpperCase() + "_PROFILES_ACTIVE";

    private final Map<String, String> configuration = new LinkedHashMap<>();
    private Map<String, String> env = System.getenv();

    private static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^a-zA-Z0-9]");


    private PropertyEncryptor propertyEncryptor;
    private List<String> activeProfiles = new ArrayList<>();
    private String configName = DEFAULT_CONFIG_NAME;

    // Need this outside method because it slows down the method
    private final Pattern patternEnc = Pattern.compile("ENC\\((.*)\\)");
    private final Pattern patternVar = Pattern.compile("\\$\\{(.+?)\\}");


    private boolean throwException = true;

    /**
     * Annotation to mark fields for configuration value injection.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Value {
        String value();
    }

    protected void put(String key, String value) {
        configuration.put(normalizeKey(key), value);
    }

    private String normalizeKey(String key) {
        // Convert to lowercase and remove all non-alphanumeric characters
        return NON_ALPHA_NUMERIC.matcher(key.toLowerCase()).replaceAll("");
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
                put(key, decryptIfNeeded(value));
            }
        } catch (ConfigurationException | IOException e) {
            var msg = "Unable to load " + name + ".properties " + e.getLocalizedMessage();
            logger.warn(msg);
            if (throwException) {
                throw new ConfigurationException(msg, e);
            }
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
        } catch (ConfigurationException | IOException e) {
            var msg = "Unable to load " + name + ".yml " + e.getLocalizedMessage();
            logger.warn(msg);
            if (throwException) {
                throw new ConfigurationException(msg, e);
            }
        }
    }

    public void loadYaml() {
        loadYaml(configName);
    }

    /**
     * Loads configuration from all supported sources (properties, YAML, environment variables).
     */
    public void loadConfiguration(String[] args, boolean throwException) {
        var name = configName;

        this.throwException = throwException;

        setEncryptionKeyByPrecedence(args);

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
        loadConfiguration(null, false);
    }
    public void loadConfiguration(boolean throwException) {
        loadConfiguration(null, throwException);
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

    private void setEncryptionKeyByPrecedence(String[] args) {
        String encryptionKey = null;

        // Load command line arguments (highest precedence)
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--" + ENCRYPTION_KEY_PROP + "=")) {
                    encryptionKey = arg.substring(("--" + ENCRYPTION_KEY_PROP + "=").length());
                    break;
                }
            }
        }

        // Load system properties if previous is null
        if (encryptionKey == null) {
            encryptionKey = System.getProperty(ENCRYPTION_KEY_PROP);
        }

        // Load env variable if previous is null
        if (encryptionKey == null) {
            encryptionKey = env.get(ENCRYPTION_KEY_ENV);
        }

        if (null != encryptionKey) {
            configuration.put(ENCRYPTION_KEY_PROP, encryptionKey);
        }
    }

    private String getConfigValue(String propertyKey, String envKey, String defaultValue) {
        String value = getProperty(propertyKey);
        if (value == null || value.isEmpty()) {
            value = env.get(envKey);
        }
        return (value != null && !value.isEmpty()) ? value : defaultValue;
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
                var propertyWithDefaultValue = valueAnnotation.value();
                var propertyTmp = propertyWithDefaultValue.replace("${", "").replace("}", "").trim();
                var parts = propertyTmp.split(":", 2);
                var propertyKey = parts[0];
                var defaultValue = parts.length > 1 ? parts[1] : null;
                var value = getConfigValue(propertyKey, propertyKey.toUpperCase().replace('.', '_'), defaultValue);

                if (value == null && defaultValue == null) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    setFieldValue(field, obj, value);
                } catch (Exception e) {
                    logger.error("Failed to set field value: " + field.getName(), e);
                }
            }
        }
    }

    /**
     * Recursively flattens a nested map and adds its entries to the configuration map.
     *
     * @param prefix the current prefix for the keys
     * @param map    the map to flatten
     */
    private void flattenMap(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                flattenMap(key, nestedMap);
            } else {
                put(key, decryptIfNeeded(entry.getValue().toString()));
            }
        }
    }

    /**
     * Loads environment variable to override configuration.
     */
    protected void loadEnvironmentVariables() {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = convertEnvToPropertyKey(entry.getKey());
            if (key != null) {
                put(key, decryptIfNeeded(entry.getValue()));
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
        return envKey.toLowerCase().replace('_', '.');
    }

    /**
     * Gets a configuration property value.
     *
     * @param key the key of the property to retrieve
     * @return the value of the property, or null if not found
     */
    public String getProperty(String key) {
        return configuration.get(normalizeKey(key));
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
        if (propertyEncryptor == null) {
            propertyEncryptor = new DefaultPropertyEncryptor(getEncryptionKey());
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
        return "ENC(" + propertyEncryptor.encrypt(value) + ")";
    }

    /**
     * Decrypts an encrypted value.
     *
     * @param encryptedValue the encrypted value to decrypt
     * @return the decrypted value
     */
    private String decrypt(String encryptedValue) {
        initializeEncryptor();
        return propertyEncryptor.decrypt(encryptedValue);
    }

    /**
     * Returns the list of active profiles for the current configuration.
     *
     * @return A list of strings representing the active profiles.
     */
    public List<String> getActiveProfiles() {
        return activeProfiles;
    }

    /**
     * Loads system properties into the configuration.
     * Any encrypted values are decrypted before being added.
     */
    private void loadSystemProperties() {
        var sysProps = System.getProperties();
        for (var key : sysProps.stringPropertyNames()) {
            configuration.put(key, decryptIfNeeded(sysProps.getProperty(key)));
        }
    }

    /**
     * Processes command line arguments and adds them to the configuration.
     * Arguments should be in the format "--key=value".
     * Any encrypted values are decrypted before being added.
     *
     * @param args An array of command line arguments.
     */
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

    /**
     * Resolves placeholders in all configuration values.
     * Placeholders are in the format ${key} and are replaced with their corresponding values.
     */
    private void resolvePlaceholders() {
        for (var entry : configuration.entrySet()) {
            entry.setValue(resolvePlaceholder(entry.getValue()));
        }
    }

    /**
     * Resolves placeholders in a single string value.
     * Placeholders are in the format ${key} and are replaced with their corresponding values.
     *
     * @param value The string value potentially containing placeholders.
     * @return The input string with all placeholders resolved, or null if the input is null.
     */
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

    /**
     * Gets the current configuration name.
     *
     * @return The name of the current configuration.
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * Sets the configuration name.
     *
     * @param configName The new name for the configuration.
     */
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * Sets the property encryptor to be used for encrypting and decrypting sensitive values.
     *
     * @param propertyEncryptor The PropertyEncryptor implementation to be used.
     */
    public void setPropertyEncryptor(PropertyEncryptor propertyEncryptor) {
        this.propertyEncryptor = propertyEncryptor;
    }

    /**
     * Sets whether exceptions should be thrown when configuration loading fails.
     *
     * @param throwException If true, exceptions will be thrown on configuration loading failures.
     *                       If false, failures will be logged but not thrown.
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
}