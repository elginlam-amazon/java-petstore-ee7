# Java 25 upgrade – Petstore Java EE 7

## Result
`mvn clean verify` on JDK 25 (Corretto 25.0.4) passes: BUILD SUCCESS. 68 main and 34 test sources compile with target 25, and all 9 unit tests pass. Integration tests are skipped by default (`skipITs=true`).

No more code changes were needed after the OpenRewrite pass. These are the changes the recipe made, as reviewed:
- `pom.xml`: `maven.compiler.source/target` set to 25. maven-war-plugin 3.5.1, surefire/failsafe 3.5.6. Byte Buddy 1.17.8 is managed so it can read Java 21+ class files. Added `jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3, `jakarta.annotation-api` 1.3.5 (provided) and `jaxb-runtime` 2.3.9 (runtime). All of these versions still use the `javax.*` namespace, so the application stays on Java EE 7 / javax.
- Java sources: unnamed catch variables (`_`), pattern matching for `instanceof`, `Locale.of(...)` and `IO.println(...)` (Java 25 `java.lang.IO`). Business logic is unchanged.

## Next steps
- Runtime target: the README says WildFly 10–26. WildFly 26 does not officially support Java 25, so bytecode compiled for 25 (class version 69) may fail to deploy there. Either run on a WildFly/JBoss EAP release that supports Java 25 (that probably means moving to Jakarta EE 10+ and migrating `javax` to `jakarta`, which is out of scope here), or lower `maven.compiler.release` to the highest version the target server supports.
- `jakarta.inject-api`, `jakarta.xml.bind-api` and `jakarta.annotation-api` overlap with APIs already in the provided `jboss-javaee-7.0` BOM. If the target server provides them, consider making `jakarta.inject-api` and `jakarta.xml.bind-api` `provided` (they are currently packaged into the WAR).
- The `version.maven.compiler.plugin` property (3.1) is not used. Maven 3.9's default compiler plugin is used. Consider declaring maven-compiler-plugin ≥ 3.13.0 explicitly and using `maven.compiler.release`.
- The Arquillian integration tests (`-Parquillian-wildfly-managed/remote`) were not run, because they need a running WildFly.
