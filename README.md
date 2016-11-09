# log4j-integration

## Configuration

Log4j provides configuration opportunity via XML or properties files.
 
#### XML config 
Just add Report Rortal appender into `log4j.xml` configuration file.
```xml
<appender name="ReportPortalAppender" class="com.epam.ta.reportportal.log4j.appender.ReportPortalAppender">
   <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern" value="[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n"/>
   </layout>
</appender>
<logger name="com.epam.ta.apache">
   <level value="OFF"/>
</logger>
<root>
    <level value="info" />
    <appender-ref ref="ReportPortalAppender" />
</root>
```

#### Property file config 

For log4j.properties file it could be looks like:
```shell
log4j.appender.reportportal=com.epam.ta.reportportal.log4j.appender.ReportPortalAppender
log4j.appender.reportportal.layout=org.apache.log4j.PatternLayout
log4j.appender.reportportal.layout.ConversionPattern=[%d{HH:mm:ss}] %-5p (%F:%L) - %m%n
```

#### Screenshots
For log4j case it is possible to send binary data in next ways.

* by using specific message wrapper

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
* sending File object as log4j log message. In this case log4j Report Portal appender send log message which will contain sending file and string message `Binary data reported`.

* adding to log message additional text information which specify attaching file location or base64 representation of sending file.
  
  in this case log message should have next format:

  ```properties
  RP_MESSAGE#FILE#FILENAME#MESSAGE_TEST
  RP_MESSAGE#BASE64#BASE_64_REPRESENTATION#MESSAGE_TEST
  RP_MESSAGE - message header
  FILE, BASE64 - attaching data representation type
  FILENAME, BASE_64_REPRESENTATION - path to sending file/ base64 representation of sending data
  MESSAGE_TEST - string log message
 ```

#### Grayscale images
There is client parameter into `reportportal.properties` with `boolean` type value for screenshots sending in `grayscale` or `color` view. By default it is set as `true` and all pictures for Report Portal will be in `grayscale` format.

**reportportal.properties**
```properties
com.epam.ta.reportportal.ws.convertimage=true
```

 Possible values:
 
`true` - all images will be converted into `grayscale`

`false` - all images will be as `color`
