# Change Log

## **1.0.x-RELEASE** `~2019-01-XX`
- Final release

## **1.0.20-BETA** `2018-12-10`
- JPQL Query REST interface
- Fixed pagination for REST endpoints

## **1.0.19-BETA** `2018-11-23`
- Support building with Java 10 (releases from now on will be java 10 only)
- Fixed "total count" in REST responses
- Added support for impex value resolver "format" parameter
- Renamed `Item.pk` to `Item.id`, and `UniqueIdItem.id` to `UniqueIdItem.uid`
- Fixed Impex command `INSERT_UPDATE` parsed as `INSERT`

## **1.0.18-BETA** `2018-11-15`
- Fixed item type bean registration issue
- Fixed REST crud interface issue (sub-items PK properties was long, not string)

## **1.0.17-BETA** `2018-11-14`
- Fixed boot problem in archetype (spring bean overriding not activated)

## **1.0.16-BETA** `2018-11-13`
- Fixed boot problem in archetype (instrumentation/weaving caused a crash)

## **1.0.15-BETA** `2018-11-12`
- Updated to latest Spring Boot
- Updated other dependencies

## **1.0.14-BETA** `2018-11-7`
- REST interface fixes (added sorting param, fixed JSON PK number overflows)
- Fixed query ordering when using pagination
- Implemented/fixed XML serialization for `Item` types

## **1.0.13-BETA** `2018-10-30`
- Fix for Hibernate indexes
- Documentation update
- CORS support

## **1.0.12-BETA** `2018-10-23`
- Fix for ´spot:transform-types` not working properly on windows

## **1.0.11-BETA** `2018-10-17`
- Bugfix, REST interface authentication was not checked properly
- Added SSL to REST-interface
- Hibernate performance improvements

## **1.0.10-BETA** `2018-09-23`
- Lots of fixes related to transaction handling
- Updated/fixed quickstart
- Improved Eclipse/m2e integration (types are generated and woven/transformed automatically from eclipse on workspace refresh)

## **1.0.9-BETA** `2018-09-12`
- Lots of impex fixes
- Build system fixes (type generation and transformation)
- Work on Eclipse/m2e integration
- Start of CMS infrastructure
- Archetype fixes

## **1.0.8-BETA** `2018-09-01`
- Work on the Impex infrastructure
- Archetype fixes

## **1.0.4-BETA** `2018-08-12`
- Fixed archetype-empty

## **1.0.3-BETA** `2018-08-12`
- Fixed archetype-empty
- Fixed flattened POM (deployed parent POM was unusable)

## **1.0.2-BETA** `2018-08-08`
- Fixed corrupt pom.xmls (by adding flatten-maven-plugin)
- Added simple sample app
- Fixed setting of default value during item type generation
- Fixed launch params not used when bootstraping custom module
- Fixed logging real classname in logging aspect for proxies 

## **1.0.1-BETA** `2018-08-07`
- Added "default value" handling for impex importer

## **1.0.0-BETA** `2018-08-07`
- First beta release, contains most of the planned functionality
- Basic ImpEx support
- Generic REST-service
- Basic domain model
- System initialization, update and initial/sample data import 

