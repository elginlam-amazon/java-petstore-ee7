# Java 25 upgrade summary

## Result
`mvn clean package` succeeds on Corretto 25.0.4 with Maven 3.9.16: 68 main and 34 test sources compile with target 25, all 9 unit tests pass, and the swagger-maven-plugin generates the API contract. This cycle needed no code or POM changes beyond the OpenRewrite output.

## Changes applied by OpenRewrite (verified by the build)
- `pom.xml`: `maven.compiler.source/target` set to 25, byte-buddy 1.17.8 pinned in the managed dependencies (needed by equalsverifier on Java 21+ class files), and the Java EE APIs that left the JDK added back on the javax line (`jakarta.xml.bind-api` 2.3.3, `jaxb-runtime` 2.3.9, `jakarta.annotation-api` 1.3.5, `jakarta.inject-api` 1.0.3). All of these still use the `javax.*` packages.
- Source: unnamed catch variables (`_`), `instanceof` pattern matching (`SimpleCallbackHandler`), `Locale.of(...)` replacing the deprecated `new Locale(...)` (`LocaleBean`), and `IO.println` replacing `System.out.println` (`LoginContextProducer`). Business logic is unchanged.
- The `*IT` integration tests got the same minor syntax changes.

## Next steps
- Runtime: the README targets WildFly 10–26, and WildFly 26 does not officially support Java 25 runtimes or class files. Before deploying, confirm the target server runs on JDK 25. Moving to WildFly 27+ means moving to Jakarta EE 10 (javax -> jakarta), which is a separate migration and was not done here.
- Arquillian integration tests (`-Parquillian-wildfly-*`) were not run. They need a running or managed WildFly.
- `maven-compiler-plugin` is not declared, so Maven's default version is used. The unused property `version.maven.compiler.plugin` (3.1) could be removed, or the plugin could be pinned to a current version so builds are reproducible.
- `LoginContextProducer` now uses `java.lang.IO` (Java 25 only). This is fine on a JDK 25 runtime. Swap it for a logger if the code ever has to run on an older JDK.
