//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
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
    private Map<String, String> configuration = new HashMap<>();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Value {
        String value();
    }

    public void loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                configuration.put(key, properties.getProperty(key));
            }
        }
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

    public void loadYaml(String filename) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream input = new FileInputStream(filename)) {
            Map<String, Object> yamlMap = yaml.load(input);
            flattenMap("", yamlMap);
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

    public void loadEnvironmentVariables() {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = convertEnvToPropertyKey(entry.getKey());
            if (key != null) {
                configuration.put(key, entry.getValue());
            }
        }
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

    public static void main(String[] args) {
        ConfigurationLoader loader = new ConfigurationLoader();
        try {
            // Load properties file
            //loader.loadProperties("config.properties");

            // Load YAML file
            loader.loadYaml("config.yml");

            // Load environment variables (overriding previous configurations)
            loader.loadEnvironmentVariables();

            // Use the configuration
            System.out.println("Mail Server Host: " + loader.getProperty("app.mail-server.host"));
            System.out.println("Mail Server Port: " + loader.getProperty("app.mail-server.port"));
            System.out.println("Mail Server Username: " + loader.getProperty("app.mail-server.username"));
            System.out.println("Mail Server Password: " + loader.getProperty("app.mail-server.password"));

            // Example usage with annotation
            AppConfig appConfig = new AppConfig();
            loader.injectConfig(appConfig);

            System.out.println("App name: " + appConfig.getAppName());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}