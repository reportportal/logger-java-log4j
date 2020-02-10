# log4j-integration

[![Download](https://api.bintray.com/packages/epam/reportportal/logger-java-log4j/images/download.svg) ](https://bintray.com/epam/reportportal/logger-java-log4j/_latestVersion)
 
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

* [Configuration - log4j](https://github.com/reportportal/logger-java-log4j#configuration---log4j)
* [Configuration - log4j2](https://github.com/reportportal/logger-java-log4j#configuration---log4j2)


## Configuration - log4j

Log4j provides a configuration opportunity via XML or properties files.

#### XML config 
Just add a Report Portal appender into the `log4j.xml` configuration file.
```xml
<appender name="ReportPortalAppender" class="com.epam.ta.reportportal.log4j.appender.ReportPortalAppender">
   <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern" value="[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n"/>
   </layout>
</appender>
<logger name="com.epam.reportportal.apache">
   <level value="OFF"/>
</logger>
<root>
    <level value="info" />
    <appender-ref ref="ReportPortalAppender" />
</root>
```

#### Property file config 

For the log4j.properties file it could look like:
```properties
log4j.appender.reportportal=com.epam.ta.reportportal.log4j.appender.ReportPortalAppender
log4j.appender.reportportal.layout=org.apache.log4j.PatternLayout
log4j.appender.reportportal.layout.ConversionPattern=[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n
```

ReportPortal' agent logs can be hidded by increasing logging level for the following package:
```xml
<logger name="rp">
    <level value="WARN"/>
</logger>
<logger name="com.epam.reportportal">
    <level value="WARN"/>
</logger>
```

#### Attaching files
For the log4j case it is possible to send binary data in next ways.

* by using a specific message wrapper

  ```java
  private static Logger logger;
  /*
   * Path to screenshot file
   */
  public String screenshot_file_path = "demoScreenshoot.png";
  /*
   * Message for attached screenshot
   */
  public String rp_message = "test message for Report Portal";
  ReportPortalMessage message = new ReportPortalMessage(new File(screenshot_file_path), rp_message);
  logger.info(message);
  ```
* sending a File object as a log4j log message. In this case a log4j Report Portal appender sends a log message which will contain the sending file and the string message `Binary data reported`.

* adding to the log message additional text information which specifies the attaching file location or the base64 representation of the sending file.
  
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
      @Test
  	  public void logJsonBase64() throws IOException {  
  	      /* here we are logging some binary data as BASE64 string */
  		  LOGGER.info(
  				"RP_MESSAGE#BASE64#{}#{}",
  				BaseEncoding.base64().encode(Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).read()),
  				"I'm logging content via BASE64"
  		  );
      }
      
      @Test
      public void logJsonFile() throws IOException, InterruptedException {
  	      /* here we are logging some binary data as file (useful for selenium) */
      	  File file = File.createTempFile("rp-test", ".json");
      	  Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).copyTo(Files.asByteSink(file));
      	  LOGGER.info("RP_MESSAGE#FILE#{}#{}", file.getAbsolutePath(), "I'm logging content via temp file");
      }
  ```

* Explicit logging. You can call the ReportPortal logger explicitly. To do this consider the following example:
  ```java
    File file = new File("my path to file");
    ReportPortal.emitLog("My message", "INFO", Calendar.getInstance().getTime(), file);
  ```
  
#### Grayscale images
There is a client parameter in `reportportal.properties` with the `boolean` type value for screenshots sending in the `grayscale` or 
`color` view. By default it is set as `true` and all pictures for Report Portal will be in the `grayscale` format.

**reportportal.properties**
```properties
rp.convertimage=true
```

 Possible values:
 
`true` - all images will be converted into `grayscale`

`false` - all images will be as `color`

## Configuration - log4j2

Log4j2 provides configuration options via XML or JSON files.
 
#### XML
Update `log4j2.xml` as follows

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration packages="com.epam.ta.reportportal.log4j.appender">
   <properties>
      <property name="pattern">[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n</property>
   </properties>
   <appenders>
      <ReportPortalLog4j2Appender name="ReportPortalAppender">
         <PatternLayout pattern="${pattern}" />
      </ReportPortalLog4j2Appender>
   </appenders>
   <loggers>
      <root level="all">
         <appender-ref ref="ReportPortalAppender"/>
      </root>
   </loggers>
</configuration>
```    
 
#### JSON
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
<Logger name="rp" level="WARN"/>
<Logger name="com.epam.reportportal" level="WARN"/>
```

For the log4j2 case it is possible to send binary data the same ways as for [log4j](#attaching-files).
Also you can use `rp.convertimage` parameter as described in [grayscale images](#grayscale-images) section.

## Troubleshooting

In some cases `log4j` can't find all enabled Appenders.

please follow with Shaded Plugin to avoid this issue: 
https://github.com/edwgiz/maven-shaded-log4j-transformer
