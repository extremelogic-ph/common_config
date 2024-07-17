import java.io.IOException;

public class ConfigDemo {
    public static void main(String[] args) {
        ConfigurationLoader loader = new ConfigurationLoader();
        try {
            loader.loadConfiguration("config");

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
