# Java 25 upgrade – build-fix cycle summary

Result: `mvn clean package` builds on JDK 25 (Corretto 25.0.4) with zero compilation errors. All 9 unit tests pass.

## Fixes applied on top of the OpenRewrite output

- `security/SimpleCallbackHandler.java`: the recipe's instanceof-to-switch rewrite left uncompilable template placeholders (`__P__.voidp()`). I rewrote it as a valid Java 25 pattern-matching switch with the original behaviour. `NameCallback` gets the login, `PasswordCallback` gets the password, and any other callback throws `UnsupportedCallbackException`.
- `pom.xml`:
  - Raised `version.equalsverifier` from 3.12.3 to 3.19.4. The Byte Buddy bundled with 3.12.3 can't read Java 25 class files (v69).
  - Changed the APIs the recipe added (`jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3, `jaxb-runtime` 2.3.9) to `provided` scope. They are javax-namespace versions, and WildFly already provides them through Java EE 7, so they are no longer packaged into the WAR.
- `OrderLineTest` and `PurchaseOrderTest`: added `Warning.JPA_GETTER` to `suppress(...)`. EqualsVerifier 3.19 adds a JPA check that the 3.12 version did not have. The entities' `equals`/`hashCode` are unchanged.

The app stays on the javax namespace (Java EE 7, `jboss-javaee-7.0`). It uses no Spring.

## Next steps

- Runtime: WildFly 26 officially supports Java 11/17 only. Running it on Java 25 is unverified. A Java 25-certified server means WildFly 27+ / Jakarta EE 10, which requires migrating javax to jakarta, and the README says the app does not work on WildFly 27. Decide which runtime JDK to target.
- Arquillian integration tests (`*IT`, skipped by default via `skipITs`) were not run because they need a WildFly instance.
- The recipe also replaced `System.out.println` with `IO.println` (Java 25 `java.lang.IO`) in `LoginContextProducer` and `new Locale(..)` with `Locale.of(..)`. Both are fine on Java 25.
- The `version.maven.compiler.plugin` property (3.1) isn't referenced by any plugin. The build uses Maven 3.9's default compiler plugin. Consider declaring maven-compiler-plugin 3.13.0 with `<release>25</release>` explicitly.
