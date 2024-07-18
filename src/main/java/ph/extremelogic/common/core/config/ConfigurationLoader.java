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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

/**
 * A utility class for loading and managing application configuration from various sources.
 * Supports loading from properties files, YAML files, and environment variables.
 */
public class ConfigurationLoader {
    public static final String DEFAULT_CONFIG_NAME = "config";

    private final Map<String, String> configuration = new HashMap<>();

    private Map<String, String> env = System.getenv();

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
        Properties properties = new Properties();
        try (InputStream input = getInputStream(filename, ".properties")) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                configuration.put(key, properties.getProperty(key));
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
        try (InputStream input = getInputStream(filename, ".yml")) {
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
        loadProperties(name);
        loadYaml(name);
        loadEnvironmentVariables();
    }

    /**
     * Injects configuration values into fields annotated with {@link Value}.
     * @param obj the object to inject configuration into
     * @throws IllegalAccessException if the fields cannot be accessed
     */
    public void injectConfig(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                String key = valueAnnotation.value().replace("${", "").replace("}", "");
                String value = getProperty(key);
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
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                flattenMap(key, nestedMap);
            } else {
                configuration.put(key, entry.getValue().toString());
            }
        }
    }

    /**
     * Loads environment variable to override configuration.
     */
    public void loadEnvironmentVariables() {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = convertEnvToPropertyKey(entry.getKey());
            if (key != null) {
                configuration.put(key, entry.getValue());
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
        if (envKey.startsWith("APP_")) {
            return envKey.substring(0)
                    .toLowerCase()
                    .replace("_", ".");
        }
        return null;
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
        InputStream input = getClass().getClassLoader().getResourceAsStream(filename + extension);
        if (input == null) {
            input = Files.newInputStream(Paths.get(filename + extension));
        }
        return input;
    }
}
