package ph.extremelogic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationLoaderTest {

    private static final String TEST_APP_NAME_PROP = "My Application Prop";
    private static final String TEST_APP_NAME_YML = "My Application Yml";
    private static final String TEST_APP_NAME_ENV = "My Application Env";

    private static final String MOCK_ENV_KEY = "APP_NAME";
    private static final String MOCK_ENV_VALUE = "EnvApp";

    private static final String APP_NAME = "app.name";


    private ConfigurationLoader loader;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    @Test
    void testBasicPropertyLoading() throws IOException {
        loader.loadProperties(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_PROP, loader.getProperty(APP_NAME));
    }

    @Test
    void testBasicYamlLoading() throws IOException {
        loader.loadConfiguration(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_YML, loader.getProperty(APP_NAME));
    }

    @Test
    void testPropertyOverriding() throws IOException {
        loader.loadProperties(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_PROP, loader.getProperty(APP_NAME));

        loader.loadYaml(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_YML, loader.getProperty(APP_NAME));

        Map<String, String> envMap = new HashMap<>();
        envMap.put(MOCK_ENV_KEY, TEST_APP_NAME_ENV);
        loader.mockEnvironmentVariables(envMap);
        loader.loadEnvironmentVariables();
        assertEquals(TEST_APP_NAME_ENV, loader.getProperty(APP_NAME));
    }

    @Test
    void testEnvironmentVariableOverriding() throws IOException {
        // Set environment variable

        Map<String, String> envMap = new HashMap<>();
        envMap.put(MOCK_ENV_KEY, MOCK_ENV_VALUE);

        loader.loadProperties(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_PROP, loader.getProperty(APP_NAME));

        loader.mockEnvironmentVariables(envMap);
        loader.loadConfiguration(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(MOCK_ENV_VALUE, loader.getProperty(APP_NAME));
    }

    @Test
    void testValueAnnotation() throws IOException, IllegalAccessException {
        loader.loadConfiguration(ConfigurationLoader.DEFAULT_CONFIG_NAME);

        AppConfig appConfig = new AppConfig();
        loader.injectConfig(appConfig);

        assertEquals(TEST_APP_NAME_YML, appConfig.getAppName());
    }
}
