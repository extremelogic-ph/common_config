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

    //@ConfigurationLoader.Value("${app.version}")
    //private int appVersion;

    @ConfigurationLoader.Value("${app.debug}")
    private boolean appDebug;

    @ConfigurationLoader.Value("${app.verbose}")
    private boolean appVerbose;

    @ConfigurationLoader.Value("${app.timeout}")
    private long appTimeout;

    @ConfigurationLoader.Value("${app.delay}")
    private int appDelay;

    @ConfigurationLoader.Value("${app.threshold}")
    private float threshold;

    @ConfigurationLoader.Value("${app.frequency}")
    private float frequency;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ConfigurationLoader.Value("${app.password}")
    private String password;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

   // public int getAppVersion() {
   //     return appVersion;
    //}

    //public void setAppVersion(int appVersion) {
    //    this.appVersion = appVersion;
    //}

    public boolean isAppDebug() {
        return appDebug;
    }

    public void setAppDebug(boolean appDebug) {
        this.appDebug = appDebug;
    }

    public boolean isAppVerbose() {
        return appVerbose;
    }

    public void setAppVerbose(boolean appVerbose) {
        this.appVerbose = appVerbose;
    }

    public long getAppTimeout() {
        return appTimeout;
    }

    public void setAppTimeout(long appTimeout) {
        this.appTimeout = appTimeout;
    }

    public int getAppDelay() {
        return appDelay;
    }

    public void setAppDelay(int appDelay) {
        this.appDelay = appDelay;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }
}
