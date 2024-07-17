package ph.extremelogic.config;

public class AppConfig {
    @ConfigurationLoader.Value("${app.name}")
    private String appName;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
