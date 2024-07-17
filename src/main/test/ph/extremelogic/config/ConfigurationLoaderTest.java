package ph.extremelogic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationLoaderTest {

    private ConfigurationLoader loader;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    @Test
    void testBasicPropertyLoading() throws IOException {
        loader.loadProperties("config");
        assertEquals("My Application", loader.getProperty("app.name"));
    }

    @Test
    void testBasicYamlLoading() throws IOException {
        loader.loadConfiguration("config");
        assertEquals("My Application", loader.getProperty("app.name"));
    }

    @Test
    void testPropertyOverriding() throws IOException {
        createPropertiesFile("app.name=TestApp");
        createYamlFile("app:\n  name: YamlApp");
        loader.loadConfiguration(tempDir.resolve("config").toString());
        assertEquals("YamlApp", loader.getProperty("app.name"));
    }

    @Test
    void testEnvironmentVariableOverriding() throws IOException {
        // Set environment variable

        Map<String, String> envMap = new HashMap<>();
        envMap.put("APP_NAME", "EnvApp");

        loader.loadProperties("config");
        assertEquals("My Application", loader.getProperty("app.name"));

        loader.mockEnvironmentVariables(envMap);
        loader.loadConfiguration("config");
        assertEquals("EnvApp", loader.getProperty("app.name"));
    }

    @Test
    void testValueAnnotation() throws IOException, IllegalAccessException {
        loader.loadConfiguration("config");

        AppConfig appConfig = new AppConfig();
        loader.injectConfig(appConfig);

        assertEquals("My Application", appConfig.getAppName());
    }

    private void createPropertiesFile(String content) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, content);
    }

    private void createYamlFile(String content) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, content);
    }
}
