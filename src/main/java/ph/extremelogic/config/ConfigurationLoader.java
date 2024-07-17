package ph.extremelogic.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

public class ConfigurationLoader {
    protected final static String DEFAULT_CONFIG_NAME = "config";

    private Map<String, String> configuration = new HashMap<>();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Value {
        String value();
    }

    public void loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename + ".properties")) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                configuration.put(key, properties.getProperty(key));
            }
        }
    }

    public void loadYaml(String filename) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream input = new FileInputStream(filename + ".yml")) {
            Map<String, Object> yamlMap = yaml.load(input);
            flattenMap("", yamlMap);
        }
    }

    public void loadConfiguration(String name) throws IOException {
        loadProperties(name );
        loadYaml(name);

        // Load environment variables (overriding previous configurations)
        loadEnvironmentVariables();
    }

    public void injectConfig(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                String key = valueAnnotation.value().replace("${", "").replace("}", "");
                String value = getProperty(key);
                if (value != null) {
                    field.setAccessible(true);
                    field.set(obj, value);
                }
            }
        }
    }

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

    private Map<String, String> env = System.getenv();

    public void loadEnvironmentVariables() {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = convertEnvToPropertyKey(entry.getKey());
            if (key != null) {
                configuration.put(key, entry.getValue());
            }
        }
    }

    //private boolean mockEnvVar = false;

    protected void mockEnvironmentVariables(Map<String, String> envVars) {
        env = envVars;
    }

    private String convertEnvToPropertyKey(String envKey) {
        if (envKey.startsWith("APP_")) {
            return envKey.substring(0)
                    .toLowerCase()
                    .replace("_", ".");
        }
        return null;
    }

    public String getProperty(String key) {
        return configuration.get(key);
    }
}