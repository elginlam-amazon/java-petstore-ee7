# Java 25 upgrade – build fix summary

Result: `mvn clean package` succeeds on JDK 25 (Corretto 25.0.4); 9/9 unit tests pass; `applicationPetstore.war` is produced.
The app stays on Java EE 7 / `javax.*` (the recipe's `jakarta.*` artifacts are the EE 8 1.x/2.3.x lines, which still use `javax` packages).

## Changes made after the OpenRewrite pass
- `security/SimpleCallbackHandler.java`: the recipe left a broken pattern-matching `switch` with template placeholders (`__P__...`). I rewrote it as a valid Java 21+ pattern switch that keeps the original behaviour: `NameCallback` gets the login, `PasswordCallback` gets the password, and anything else (including `null`) throws `UnsupportedCallbackException`.
- `pom.xml`: `version.equalsverifier` 3.12.3 -> 3.19.4. The bundled ByteBuddy in 3.12.3 can't read Java 25 class files.
- `OrderLineTest`, `PurchaseOrderTest`: the stricter EqualsVerifier 3.19 flags direct field access in JPA entity `equals`. I suppressed `Warning.JPA_GETTER`, which keeps the entity logic as it was.

## Next steps (non-blocking)
- Runtime target: the README targets WildFly 10–26. WildFly 26 does not officially support running on Java 25 or Java 25 bytecode (class file v69). The app must be deployed on a JDK 25–capable server. Note that WildFly 27+ is Jakarta EE 10, which would require a javax -> jakarta migration.
- The recipe added `jakarta.inject-api` 1.0.3 and `jakarta.xml.bind-api` 2.3.3 at compile scope, so they are packaged in the WAR next to APIs the server already provides. Consider `provided` scope after a deployment test.
- `LoginContextProducer` now uses `IO.println` (Java 25 `java.lang.IO`). It behaves the same, but consider replacing it with the existing logger.
- The Arquillian integration tests (`*IT`, skipped by default) compile but were not run. They need a WildFly instance.
