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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDir;
import ph.extremelogic.common.core.config.sample.AppConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {

    private static final String TEST_APP_NAME_PROP = "My Application Prop";
    private static final String TEST_APP_NAME_YML = "My Application Yml";
    private static final String TEST_APP_NAME_ENV = "My Application Env";
    private static final double DELTA = 1e-6;

    private static final String MOCK_ENV_KEY = "APP_NAME";
    private static final String MOCK_ENV_VALUE = "EnvApp";

    private static final String APP_NAME = "app.name";

    private static final String TEST_ENCRYPTION_KEY = "your16charEncKey";

    private ConfigurationLoader loader;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();

        // Set the encryption key as a system property for testing
        System.setProperty(ConfigurationLoader.ENCRYPTION_KEY_PROP, TEST_ENCRYPTION_KEY);

        loader.loadConfiguration(false);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testBasicPropertyLoading() {
        loader.loadProperties(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_PROP, loader.getProperty(APP_NAME));
    }

    @Test
    void testMissingConfigProfilesActive() {
        System.setProperty(ConfigurationLoader.CONFIG_PROFILES_ACTIVE_PROP, "missing");

        String missingFileName = "config-missing";

        Exception exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadConfiguration(true);
        });
        var msg = exception.getMessage();
        assertTrue(msg.contains("Unable to load " + missingFileName + ".properties"));
        System.clearProperty(ConfigurationLoader.CONFIG_PROFILES_ACTIVE_PROP);
    }

    @Test
    void testBasicYamlLoading() {
        loader.loadConfiguration();
        assertEquals(TEST_APP_NAME_YML, loader.getProperty(APP_NAME));
    }

    @Test
    void testPropertyOverriding() {
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
    void testEnvironmentVariableOverriding() {
        // Set environment variable

        Map<String, String> envMap = new HashMap<>();
        envMap.put(MOCK_ENV_KEY, MOCK_ENV_VALUE);

        loader.loadProperties(ConfigurationLoader.DEFAULT_CONFIG_NAME);
        assertEquals(TEST_APP_NAME_PROP, loader.getProperty(APP_NAME));

        loader.mockEnvironmentVariables(envMap);
        loader.loadConfiguration();
        assertEquals(MOCK_ENV_VALUE, loader.getProperty(APP_NAME));
    }

    @Test
    void testValueAnnotation() throws IllegalAccessException {

        loader.loadConfiguration();

        AppConfig appConfig = new AppConfig();
        loader.injectConfig(appConfig);

        assertEquals(TEST_APP_NAME_YML, appConfig.getAppName());
        assertFalse(appConfig.isAppDebug());
        assertTrue(appConfig.isAppVerbose());
        assertEquals(879, appConfig.getAppTimeout());
        assertEquals(143, appConfig.getAppDelay());
        assertEquals(0.678, appConfig.getThreshold(), DELTA);
        assertEquals(9.3136, appConfig.getFrequency(), DELTA);
    }

    @Test
    public void testDefaultValues() throws IllegalAccessException {
        AppConfig appConfig = new AppConfig();
        ConfigurationLoader loader = new ConfigurationLoader();
        loader.injectConfig(appConfig);

        assertEquals("MyApp", appConfig.getDefaultName());
        assertEquals(1, appConfig.getDefaultVersion());
        assertFalse(appConfig.isDefaultDebug());
        assertEquals(3000L, appConfig.getDefaultTimeout());
        assertEquals(50.0f, appConfig.getDefaultFrequency());
    }

    @Test
    void testEncrypt() throws IllegalAccessException {
        String originalPassword = "secretPassword";
        String encryptedPassword = loader.encrypt(originalPassword);

        System.out.println("Encrypted password: " + encryptedPassword);

        assertTrue(encryptedPassword.startsWith("ENC(") && encryptedPassword.endsWith(")"),
                "Encrypted password should be wrapped in ENC()");

        loader.loadConfiguration();


        AppConfig appConfig = new AppConfig();
        loader.injectConfig(appConfig);

        assertEquals("secretPassword", loader.getProperty("app.password"));
        assertEquals("secretPassword", appConfig.getPassword());
    }
}
