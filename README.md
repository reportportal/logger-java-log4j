# Log4j2 appender for Report Portal

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/logger-java-log4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.epam.reportportal/logger-java-log4j)
[![CI Build](https://github.com/reportportal/logger-java-log4j/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/logger-java-log4j/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/logger-java-log4j/branch/develop/graph/badge.svg?token=iEy7fURz1P)](https://codecov.io/gh/reportportal/logger-java-log4j)
[![Join Slack chat!](https://slack.epmrpp.reportportal.io/badge.svg)](https://slack.epmrpp.reportportal.io/)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The latest version: 5.2.1. Please use `Maven Central` link above to get the library.

## Dependencies

To start using Report Portal log appending you need to include this library into your project dependencies according to
your build system.

### Maven

Add this section into `<dependecies>` section:

```xml

<dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>logger-java-log4j</artifactId>
    <version>5.2.1</version>
    <scope>test</scope>
</dependency>
```

### Gradle

Add this line into `dependecies` block:

```groovy
testImplementation 'com.epam.reportportal:logger-java-log4j:5.2.1'
```

## Configuration

Log4j2 provides configuration options via XML or JSON files.

### XML

Update `log4j2.xml` as follows

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <properties>
        <property name="pattern">[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n</property>
    </properties>
    <appenders>
        <ReportPortalLog4j2Appender name="ReportPortalAppender">
            <PatternLayout pattern="${pattern}"/>
        </ReportPortalLog4j2Appender>
    </appenders>
    <loggers>
        <root level="all">
            <appender-ref ref="ReportPortalAppender"/>
        </root>
    </loggers>
</configuration>
```    

### JSON

Update `log4j2.json` as follows

```JSON
{
  "configuration": {
    "properties": {
      "property": {
        "name": "pattern",
        "value": "%d{HH:mm:ss.SSS} [%t] %-5level - %msg%n"
      }
    },
    "appenders": {
      "ReportPortalLog4j2Appender": {
        "name": "ReportPortalAppender",
        "PatternLayout": {
          "pattern": "${pattern}"
        }
      }
    },
    "loggers": {
      "root": {
        "level": "all",
        "AppenderRef": {
          "ref": "ReportPortalAppender"
        }
      }
    }
  }
}
```

ReportPortal's agent logs can be hided by increasing logging level for the following package:

```xml

<loggers>
    <Logger name="rp" level="WARN"/>
    <Logger name="com.epam.reportportal" level="WARN"/>
</loggers>
```

### Attaching files (Screenshots, videos, archives, reports etc.)

For the log4j case it is possible to send binary data in next ways.

* by using a specific message wrapper

```java
public class Test {
	private static final Logger LOGGER = LogManager.getLogger(Test.class);

	/*
	 * Path to screenshot file
	 */
	public static final String screenshot_file_path = "demoScreenshoot.png";

	@Test
	public void test() {
		/*
		 * Message for attached screenshot
		 */
		public String rp_message = "test message for Report Portal";
		ReportPortalMessage message = new ReportPortalMessage(new File(screenshot_file_path), rp_message);
		LOGGER.info(message);
	}
}
```

* sending a File object as a log4j log message. In this case a log4j Report Portal appender sends a log message which
  will contain the sending file and the string message `Binary data reported`.

* adding to the log message additional text information which specifies the attaching file location or the base64
  representation of the sending file.

  in this case a log message should have the next format:

```
RP_MESSAGE#FILE#FILENAME#MESSAGE_TEST
RP_MESSAGE#BASE64#BASE_64_REPRESENTATION#MESSAGE_TEST
```

> RP_MESSAGE - message header

> FILE, BASE64 - attaching data representation type

> FILENAME, BASE_64_REPRESENTATION - path to sending file/ base64 representation of sending data

> MESSAGE_TEST - string log message

```java
public class Test {
	private static final Logger LOGGER = LogManager.getLogger(Test.class);

	private static final String JSON_FILE_PATH = "files/file.json";

	@Test
	public void logJsonBase64() {
		/* here we are logging some binary data as BASE64 string */
		LOGGER.info(
				"RP_MESSAGE#BASE64#{}#{}",
				BaseEncoding.base64().encode(Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).read()),
				"I'm logging content via BASE64"
		);
	}

	@Test
	public void logJsonFile() {
		/* here we are logging some binary data as file (useful for selenium) */
		File file = File.createTempFile("rp-test", ".json");
		Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).copyTo(Files.asByteSink(file));
		LOGGER.info("RP_MESSAGE#FILE#{}#{}", file.getAbsolutePath(), "I'm logging content via temp file");
	}
}
```

* Explicit logging. You can call the ReportPortal logger explicitly. To do this consider the following example:

```java
public class Test {
	@Test
	public void log() {
		File file = new File("my path to file");
		ReportPortal.emitLog("My message", "INFO", Calendar.getInstance().getTime(), file);
	}
}
```

### Grayscale images

There is a client parameter in `reportportal.properties` with the `boolean` type value for screenshots sending in
the `grayscale` or
`color` view. By default, it is set as `true` and all pictures for Report Portal will be in the `grayscale` format.

**reportportal.properties**

```properties
rp.convertimage=true
```

Possible values:

* `true` - all images will be converted into `grayscale`
* `false` - all images will be as `color`

## JDK Logging / java.util.logger (JUL)

Due to low popularity of JUL logger, Report Portal does not have adapter for it. Bridge to SLF4J or Log4j may be used in
this case:
[Log4j2 JDK Logging Adapter](https://logging.apache.org/log4j/2.0/log4j-jul/index.html)

## Troubleshooting

In some cases `log4j` can't find all enabled Appenders.

please follow with Shaded Plugin to avoid this issue:
https://github.com/edwgiz/maven-shaded-log4j-transformer
