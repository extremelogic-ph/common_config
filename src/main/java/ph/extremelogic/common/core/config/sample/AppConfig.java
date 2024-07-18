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

/**
 * Represents the application configuration.
 * This class uses the {@link ConfigurationLoader.Value} annotation to inject configuration values.
 */
public class AppConfig {

    /**
     * The name of the application.
     */
    @ConfigurationLoader.Value("${app.name}")
    private String appName;

    /**
     * Gets the application name.
     * @return the application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets the application name.
     * @param appName the application name to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }
}
