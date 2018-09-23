// always a good idea to add an on console status listener
statusListener(OnConsoleStatusListener)

// set the context's name
context.name = "processor"
// add a status message regarding context's name
addInfo("Context name has been set to ${context.name}")

appender("FILE", RollingFileAppender) {
  encoder(PatternLayoutEncoder) {
    Pattern = "%d %level %thread %mdc %logger - %m%n"
  }
  rollingPolicy(TimeBasedRollingPolicy) {
    FileNamePattern = "/var/log/PoleAMS/processor-%d{yyyy-MM}.zip"
  }
}

root(WARN, ["FILE"])
