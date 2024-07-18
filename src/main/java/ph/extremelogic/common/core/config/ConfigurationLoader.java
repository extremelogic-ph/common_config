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
        try (InputStream input = getInputStream(filename, ".properties")) {
            if (input != null) {
                properties.load(input);
                for (String key : properties.stringPropertyNames()) {
                    configuration.put(key, properties.getProperty(key));
                }
            }
        }
    }

    public void loadYaml(String filename) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream input = getInputStream(filename, ".yml")) {
            if (input != null) {
                Map<String, Object> yamlMap = yaml.load(input);
                flattenMap("", yamlMap);
            }
        }
    }

    public void loadConfiguration(String name) throws IOException {
        loadProperties(name);
        loadYaml(name);
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

    private InputStream getInputStream(String filename, String extension) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(filename + extension);
        if (input == null) {
            input = new FileInputStream(filename + extension);
        }
        return input;
    }
}
