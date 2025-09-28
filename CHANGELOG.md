# Changelog

## [Unreleased]

## [5.4.0]
### Changed
- Client version updated to [5.4.3](https://github.com/reportportal/client-java/releases/tag/5.4.3), by @HardNorth
- Switch on use of `Instant` class instead of `Date` to get more timestamp precision, by @HardNorth
### Removed
- Java 8-10 support, by @HardNorth

## [5.3.0]
### Changed
- Client version updated on [5.3.14](https://github.com/reportportal/client-java/releases/tag/5.3.14), by @HardNorth

## [5.2.3]
### Changed
- Client version updated on [5.2.25](https://github.com/reportportal/client-java/releases/tag/5.2.25), by @HardNorth

## [5.2.2]
### Changed
- Client version updated on [5.2.5](https://github.com/reportportal/client-java/releases/tag/5.2.5), by @HardNorth
- `client-java` and `log4j-core` dependencies marked as `compileOnly` to force users specify their own dependencies, by @HardNorth

## [5.2.1]
### Changed
- Client version updated on [5.2.4](https://github.com/reportportal/client-java/releases/tag/5.2.4), by @HardNorth
### Removed
- `commons-model` dependency to rely on `client-java` exclusions in security fixes, by @HardNorth

## [5.2.0]
### Changed
- Client version updated on [5.2.0](https://github.com/reportportal/client-java/releases/tag/5.2.0), by @HardNorth

## [5.1.8]
### Changed
- Client version updated on [5.1.22](https://github.com/reportportal/client-java/releases/tag/5.1.22), by @HardNorth

## [5.1.7]
### Added
- Plugin location by annotation processor, by @valfirst
### Changed
- Client version updated on [5.1.21](https://github.com/reportportal/client-java/releases/tag/5.1.21), by @HardNorth

## [5.1.6]
### Changed
- Client version updated on [5.1.16](https://github.com/reportportal/client-java/releases/tag/5.1.16), by @HardNorth
### Removed
- Log4j 1.2 support, since it's extremely vulnerable, by @HardNorth

## [5.1.5]
### Added
- Logging issues skip, by @HardNorth
### Changed
- Client version updated on [5.1.11](https://github.com/reportportal/client-java/releases/tag/5.1.11), by @HardNorth
- Log4j version updated on 2.17.2, by @HardNorth
### Fixed
- A bug with wrong object casting, by @jusski

## [5.1.4]
### Changed
- Client version updated on [5.1.4](https://github.com/reportportal/client-java/releases/tag/5.1.4)
- Log4j version updated on 2.17.1 due to vulnerability

## [5.1.3]
### Changed
- Log4j version updated on 2.17.0 due to critical vulnerability

## [5.1.2]
### Changed
- Log4j version updated on 2.16.0 due to critical vulnerability

## [5.1.1]
### Changed
- Log4j version updated on 2.15.0 due to critical vulnerability

## [5.1.0]
### Changed
- Version promoted to stable release
- Client version updated on [5.1.0](https://github.com/reportportal/client-java/releases/tag/5.1.0)

## [5.1.0-RC-1]
### Changed
- Client version updated on [5.1.0-RC-12](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-12)

## [5.0.3]
### Changed
- bumping up client version
### Fixed
- Workarounded: A multi-threaded bug in Log4j 1.2 log formatting. When a log line mixes with
  other nearest log lines. This fix affects logger performance, so it's a common recommendation to
  avoid using Log4j 1.2

## [5.0.2]
### Changed
- bumping up client version

## [3.0.2]
##### Released: 7 Jun 2017

### Bugfixes

* reportportal/agent-java-testNG#8 - Java agent sends duplicate log entries to Report Portal
