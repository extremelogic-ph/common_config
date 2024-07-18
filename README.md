
## ConfigurationLoader

ConfigurationLoader is a flexible and easy-to-use configuration management library for Java applications. It supports loading configuration from properties files, YAML files, and environment variables, with a priority system that allows for easy overriding of values.

## Features

- Load configuration from properties files
- Load configuration from YAML files
- Load configuration from environment variables
- Annotation-based injection of configuration values
- Hierarchical configuration with dot notation
- Easy to use API

## Getting Started
### Prerequisites

- Java 8 or higher
- Maven or Gradle (for dependency management)

### Installation
- Add the following dependency to your pom.xml file:

```
<dependency>
<groupId>ph.extremelogic.common.core</groupId>
<artifactId>config</artifactId>
<version>1.0.0</version>
</dependency>
```

Or for Gradle, add this to your build.gradle file:
gradle

```
implementation 'ph.extremelogic.common.core:config:1.0.0'
```

### Usage
Here's a simple example of how to use ConfigurationLoader:

```
ConfigurationLoader loader = new ConfigurationLoader();
loader.loadConfiguration(ConfigurationLoader.DEFAULT_CONFIG_NAME);

// Use the configuration
String host = loader.getProperty("app.mail-server.host");
String port = loader.getProperty("app.mail-server.port");

// Use with annotations
AppConfig appConfig = new AppConfig();
loader.injectConfig(appConfig);
String appName = appConfig.getAppName();
```

### Configuration Priority
The library loads configuration in the following order, with later sources overriding earlier ones:

- Properties files
- YAML files
- Environment variables

Environment variables should be prefixed with APP_ and use underscores instead of dots. For example, app.name would be APP_NAME.

## Encryption and Decryption

ConfigurationLoader supports encrypted property values. To use this feature:

1. Set the encryption key:
```
export APP_ENCRYPTION_KEY=your-secret-key
```
or

```
java -Dapp.encryption.key=your-secret-key -jar your-app.jar
```

2. Encrypt sensitive values:

```
String encryptedValue = configLoader.encrypt("sensitive-value");
```

3. Use encrypted values in your configuration files:

```
db.password=ENC(encrypted-value-here)
```

The ConfigurationLoader will automatically decrypt these values when loading the configuration.

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is licensed under the Apache License 2.0 - see the LICENSE file for details.