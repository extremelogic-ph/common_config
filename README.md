
## ConfigurationLoader

ConfigurationLoader is a flexible and powerful configuration management library for Java applications. Inspired by Spring Boot's configuration management but designed to work independently, this library provides a robust solution for applications requiring configuration outside of the Spring ecosystem.

## Features

- Load configuration from multiple sources:
  - Properties files
  - YAML files
  - Environment variables
  - System properties
  - Command-line arguments
- Support for configuration profiles
- Hierarchical configuration with dot notation
- Placeholder resolution in configuration values
- Encryption and decryption of sensitive configuration values
- Annotation-based injection of configuration values

## Getting Started
### Prerequisites

- Java 11 or higher
- Maven or Gradle (for dependency management)

### Installation

**Note: The dependency information below is not yet set up and will be available in a few days.**

Add the following dependency to your `pom.xml` file:

```
<dependency>
    <groupId>ph.extremelogic.common.core.config</groupId>
    <artifactId>common-config</artifactId>
    <version>0.0.1</version>
</dependency>
```

Or for Gradle, add this to your build.gradle file:
gradle

```
implementation 'ph.extremelogic.common.core.config:common-config:0.0.1'
```

### Usage
Here's a simple example of how to use ConfigurationLoader:

1. Basic usage:

```java
ConfigurationLoader loader = new ConfigurationLoader();
loader.loadConfiguration();

// Use the configuration
String host = loader.getProperty("app.mail-server.host");
int port = Integer.parseInt(loader.getProperty("app.mail-server.port"));
```

2. Using with annotations:

```java
public class AppConfig {
    @ConfigurationLoader.Value("app.name")
    private String appName;

    @ConfigurationLoader.Value("app.version")
    private String appVersion;

    // getters and setters
}

ConfigurationLoader loader = new ConfigurationLoader();
loader.loadConfiguration();

AppConfig appConfig = new AppConfig();
loader.injectConfig(appConfig);

System.out.println("App Name: " + appConfig.getAppName());
System.out.println("App Version: " + appConfig.getAppVersion());
```

3. Loading YAML configuration:

```java
ConfigurationLoader loader = new ConfigurationLoader();
loader.loadYaml();

String databaseUrl = loader.getProperty("database.url");
String databaseUsername = loader.getProperty("database.username");
```


## Configuration Priority
The library loads configuration in the following order, with later sources overriding earlier ones:

1. Default properties file (`config.properties`)
2. Default YAML file (`config.yml`)
3. Profile-specific properties files (`config-{profile}.properties`)
4. Profile-specific YAML files (`config-{profile}.yml`)
5. Environment variables
6. System properties
7. Command-line arguments

## Configuration Profiles
You can use configuration profiles to load environment-specific configurations. Set the active profile using:

- The `config.profiles.active` property in your default configuration file
- The `CONFIG_PROFILES_ACTIVE` environment variable
- The `config.profiles.active` system property
- The `--config.profiles.active` command-line argument

## Encryption and Decryption

ConfigurationLoader supports encrypted property values. To use this feature:

1. Set the encryption key:

```shell
export CONFIG_ENCRYPTION_KEY=your-16-char-secret-key
```
or

```shell
java -Dconfig.encryption.key=your-16-char-secret-key -jar your-app.jar
```

2. Encrypt sensitive values using the PropertyEncryptor utility:

```java
public static void main(String[] args) {
    var scanner = new Scanner(System.in);

    System.out.print("Enter a 16-character encryption key: ");
    String encryptionKey = scanner.nextLine();

    if (encryptionKey.length() != KEY_LENGTH) {
        System.out.println("Error: Encryption key must be 16 characters long.");
        return;
    }

    var encryptor = new PropertyEncryptor(encryptionKey);

    System.out.print("Enter a value to create an encrypted equivalent for your configuration: ");
    var valueToEncrypt = scanner.nextLine();

    var encryptedValue = encryptor.encrypt(valueToEncrypt);
    System.out.println();
    System.out.println("Place this in your configuration.");
    System.out.println("Encrypted Value: ENC(" + encryptedValue + ")");

    var decryptedValue = encryptor.decrypt(encryptedValue);
    System.out.println();
    System.out.println("Below is just a verification that we can decrypt it.");
    System.out.println("Decrypted Value: " + decryptedValue);
}
```

3. Use encrypted values in your configuration files:

```properties
db.password=ENC(encrypted-value-here)
```

The ConfigurationLoader will automatically decrypt these values when loading the configuration.

## Limitations

- The encryption key must be exactly 16 characters long.
- The library uses AES encryption in ECB mode, which may not be suitable for all security requirements.
- Placeholder resolution is currently limited to simple ${key} syntax and does not support default values or nested placeholders.

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is licensed under the Apache License 2.0 - see the LICENSE file for details.