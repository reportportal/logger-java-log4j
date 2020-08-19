# Changelog

## [Unreleased]
### Changed
- bumping up client version
### Fixed
- Workarounded: A multi-threaded bug in Log4j 1.2 log formatting. When a log line mixes with
  other nearest log lines. This fix affects logger performance, so it's common recommendation to
  avoid using Log4j 1.2

## [5.0.2]
### Changed
- bumping up client version

## [3.0.2]
##### Released: 7 Jun 2017

### Bugfixes

* reportportal/agent-java-testNG#8 - Java agent sends duplicate log entries to Report Portal
