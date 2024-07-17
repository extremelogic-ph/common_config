//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

public class ConfigurationLoader {
    private Map<String, String> configuration = new HashMap<>();

    public void loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                configuration.put(key, properties.getProperty(key));
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
            return envKey.substring(4)
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}