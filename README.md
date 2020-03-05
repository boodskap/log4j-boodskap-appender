# log4j-boodskap-appender
Log4J Boodskap Platform Appender

# Sample log4j2.xml Configuration

    <Boodskap name="Boodskap">
      
      <!--Refer to Log4J PatternLayout for more details-->
      <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %c{1}:%L - %m%n"/>
      
      <!--Enable / Disable synchronous logging, enabling may introduce considerable delay in the execution-->
      <sync>false</sync>
      
      <!--Max numer of log events to be buffered in the memory-->
      <queueSize>10000</queueSize>
      
      <!--Boodskap platform API base path-->
      <apiBasePath></apiBasePath>
      
      <!-- Boodskap platform's Domain Key -->
      <domainKey></domainKey>
      
      <!-- Boodskap platform's API Key -->
      <apiKey></apiKey>
      
      <!-- Boodskap Log Analyzer's Application ID -->
      <appId></appId>
      
    </Boodskap>
