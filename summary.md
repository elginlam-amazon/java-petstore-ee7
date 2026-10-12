# Java 25 upgrade summary

## Result
`mvn clean package` passes on OpenJDK 25 (Corretto 25.0.4): 68 main and 34 test sources compile with `--release 25`, the 9 unit tests pass, and the WAR and Swagger contract are generated. The OpenRewrite output needed no manual fixes.

## Changes made by OpenRewrite (checked)
- `pom.xml`: `maven.compiler.source/target` set to 25. Byte Buddy is pinned to 1.17.8 so EqualsVerifier and Mockito can read Java 25 class files. Surefire and Failsafe are at 3.5.6. JAXB, `jakarta.inject` 1.0.3 and `jakarta.annotation` 1.3.5 were added. These are still the `javax.*` packages, so the app stays on Java EE 7 as intended.
- Source: unused catch variables became `_` (Java 22+). Pattern-matching `instanceof` is now used in `SimpleCallbackHandler`. `new Locale(..)` became `Locale.of(..)`. `@Serial` was added to the admin view beans. `System.out.println` became `IO.println` in `LoginContextProducer`.
- Tests: the deprecated `new Float(..)` and `new Integer(..)` became `valueOf(..)`.

## Next steps
- Runtime: the target server is WildFly 10–26 (Java EE 7, `javax`). WildFly 26 does not officially support a Java 25 JVM, and classes compiled for Java 25 (class file version 69) need a Java 25 runtime. Deploying therefore needs a server that runs on JDK 25. For Java EE / `javax`, that is not available without moving to Jakarta EE (WildFly 27+). Decide the server strategy before deploying.
- `IO.println` (java.lang.IO, Java 25) only works on a Java 25 runtime. A logger would be a better replacement.
- Arquillian integration tests (`-Parquillian-wildfly-*`) were not run because they need a running or downloaded WildFly.
- The `version.maven.compiler.plugin` property (3.1) is unused. Maven 3.9 uses its default compiler plugin, which supports `release 25`. Consider declaring maven-compiler-plugin explicitly at 3.13+.
- `rewrite.yml` (the OpenRewrite recipe file) is untracked. Remove it or commit it as needed.
