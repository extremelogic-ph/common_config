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

package ph.extremelogic.common.core.config.sample;

import ph.extremelogic.common.core.config.ConfigurationLoader;

import java.io.IOException;

/**
 * A demonstration class showing how to use the ConfigurationLoader.
 */
public class ConfigDemo {
    public static void main(String[] args) {
        ConfigurationLoader loader = new ConfigurationLoader();
        try {
            loader.loadConfiguration(ConfigurationLoader.DEFAULT_CONFIG_NAME);

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
