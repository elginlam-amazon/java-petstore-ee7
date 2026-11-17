# Java 25 upgrade — build-green summary

Module: `org.agoncal.application:petstoreee7` (Java EE 7 Petstore, WAR, deployed to WildFly)
Target: **Java 25** (Corretto 25.0.5), Maven 3.9.16
Namespace: **javax.\*** — unchanged (see "Namespace decision" below)

## Result

`mvn clean verify` — **BUILD SUCCESS**, zero compilation errors.

| Stage | Result |
| --- | --- |
| `compiler:3.15.0:compile` | 68 source files, `javac [debug release 25]`, 0 errors, 0 warnings |
| `compiler:3.15.0:testCompile` | 34 source files, `javac [debug release 25]`, 0 errors, 0 warnings |
| `surefire:3.5.6:test` | Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 |
| `war:3.5.1:war` | `target/applicationPetstore.war` built |
| `failsafe:3.5.6:integration-test` / `verify` | Skipped — `skipITs=true` is the project's own default (see "Next steps") |

A compile with `-Dmaven.compiler.showDeprecation -Dmaven.compiler.showWarnings` reports **no**
deprecation or removal warnings in main or test sources.

## Namespace decision: stays on javax

This is a non-Spring Jakarta EE application whose entire API surface comes from the
`org.jboss.spec:jboss-javaee-7.0` BOM (`provided`): `javax.persistence`, `javax.ejb`,
`javax.inject`, `javax.enterprise`, `javax.faces`, `javax.ws.rs`, `javax.validation`,
`javax.interceptor`, `javax.xml.bind`. No Spring is present, and no jakarta-line target was
selected, so **no `javax.*` → `jakarta.*` migration was performed**. The deployment descriptors
(`web.xml`, `beans.xml` 1.1, `faces-config.xml` 2.2, `persistence.xml` 2.1) remain on the
`http://xmlns.jcp.org/xml/ns/...` Java EE 7 namespaces, consistent with the API stack and the
WildFly 26 target. Nothing in the Java 25 toolchain requires crossing namespaces.

## What the OpenRewrite pass did (verified correct, kept)

Build:
- `maven.compiler.source/target` `1.8` → `25`.
- `maven-surefire-plugin` / `maven-failsafe-plugin` `3.0.0-M8` → `3.5.6`; `maven-war-plugin`
  `3.1.0` → `3.5.1`.
- Pinned `net.bytebuddy:byte-buddy` to `1.17.8` in `dependencyManagement` — required, because the
  version EqualsVerifier 3.12.3 pulls in cannot read Java 21+ class files. The 9 model tests
  exercise this and pass.

Source (all 102 files reviewed; each change is semantics-preserving):
- Unnamed variables: `catch (NoResultException nre)` → `catch (NoResultException _)` in the 5 REST
  endpoints, `CustomerService` (×2), `ViewUtils`, `LoginContextProducer`. Every one of these
  bindings was already unused, so behaviour is unchanged.
- `SimpleCallbackHandler`: `instanceof` + cast → pattern matching for `instanceof`.
- `LocaleBean`: `new Locale(language)` → `Locale.of(language)`.
- `@Serial` added to the 7 `serialVersionUID` fields in `view/admin`.
- `LoginContextProducer`: `System.out.println("ouch!!!")` → `IO.println("ouch!!!")`
  (`java.lang.IO`, final in JDK 25 via JEP 512 — compiles without `--enable-preview`).
  **Superseded in iteration 16** — see "Iteration 16 changes" below.
- Tests: `new Integer(77)` / `new Float(12.5F)` → `Integer.valueOf` / `Float.valueOf` in
  `OrderLineServiceIT`, `PurchaseOrderServiceIT`, `OrderLineBeanIT`, `PurchaseOrderBeanIT`. No
  assertion was loosened — the compared values are identical.

No `SecurityManager`, `AccessController`, `finalize()`, `Class.newInstance()`, `sun.misc.*` or
Nashorn usage exists in this codebase, so the JDK 25 removals required no source changes.

## What I fixed on top of the recipe

### 1. Removed four redundant Java EE API artifacts the recipe added

The recipe added standalone API jars for the packages the JDK dropped in Java 11
(`jakarta.inject-api:1.0.3`, `jakarta.xml.bind-api:2.3.3`, `jakarta.annotation-api:1.3.5`) plus
`org.glassfish.jaxb:jaxb-runtime:2.3.9`. All four are **javax-namespace** artifacts, so they did
not break the namespace — but they are redundant here and two of them were harmful:

- This application never got `javax.xml.bind` / `javax.annotation` / `javax.inject` from the JDK.
  It gets them from the `jboss-javaee-7.0` BOM (`javax.inject:javax.inject:1`,
  `jboss-annotations-api_1.2_spec`, `jboss-jaxb-api_2.2_spec`), all `provided`. Compilation
  therefore does not need the added artifacts — confirmed by a clean `verify` after removal.
- Three of them (`jakarta.inject-api`, `jakarta.xml.bind-api` + its `jakarta.activation-api`, and
  `jaxb-runtime` + `txw2` / `istack-commons-runtime` / `jakarta.activation`) were at `compile` /
  `runtime` scope and were therefore **packaged into `WEB-INF/lib`** — six container-owned EE API
  and JAXB-implementation jars inside a WAR that WildFly already supplies these to. That is a
  classic deployment-time class-loading conflict and a regression against the pre-upgrade WAR.

`WEB-INF/lib` is now back to the pre-upgrade set (`primefaces`, `all-themes`, `bootstrap`,
`jquery`, `fontawesome`, `swagger-annotations`, `log4j-api`, `log4j-core`) with the Java 25
toolchain changes retained. A comment in `pom.xml` records why no standalone EE API jar is declared.

### 2. Pinned `maven-compiler-plugin` and switched to `<release>`

`version.maven.compiler.plugin` was a dangling property at `3.1` — the plugin was never declared,
so the build silently used whatever version Maven's lifecycle default supplied, and `3.1` cannot
compile `release 25` if anyone ever wired the property up. Per knowledge base doc 16 (Step 2,
Step 9 validation checklist) the plugin is now declared explicitly at `3.15.0` with
`<release>${maven.compiler.release}</release>`, and `maven.compiler.source` / `maven.compiler.target`
were replaced by a single `maven.compiler.release=25`. The log now reads
`javac [debug release 25]` instead of `[debug target 25]`, so the build no longer depends on an
implicit plugin version.

## Iteration 16 changes

Carry-over items from iteration 15 that were actionable in code are now done. `mvn clean verify`
re-run green after each.

### `IO.println` replaced with the project's logger (carry-over item 5 — done)

`LoginContextProducer` no longer calls `java.lang.IO`. It now follows the pattern every other
logging site in this codebase uses — `@Inject private Logger log` (`java.util.logging.Logger`,
supplied by the existing `LoggingProducer` CDI producer, exactly as in `LoggingInterceptor` and
`ExceptionInterceptor`) — and logs at `severe` in `ExceptionInterceptor`'s message style:

```java
catch (Exception e)
{
   log.severe("/!\\ Could not create a LoginContext for login module " + loginModuleName + " - " + e.getMessage());
   return null;
}
```

Control flow is unchanged (still returns `null`). The `catch` binding went back from `_` to `e`
because the exception is now actually used: previously the login failure was discarded entirely,
which is poor practice on a security path. No new dependency was added — Log4j2 is on the
classpath but JUL is what this project already standardised on, and WildFly routes JUL into its
own log manager. `grep` confirms no `IO.print` / `IO.readln` call remains anywhere in `src/`.

### Confirmed already done in iteration 15

- **Carry-over item 4** (`jakarta.inject-api` / `jakarta.xml.bind-api` / `jaxb-runtime` scope):
  resolved by removing them outright rather than marking them `provided`, because the
  `jboss-javaee-7.0` BOM already supplies those APIs at `provided`. `WEB-INF/lib` now holds 8 jars
  and a scan for `jakarta|javax|jaxb|activation` inside it returns nothing — no duplicate API jars.
- **Carry-over item 6** (`version.maven.compiler.plugin`): wired up at `3.15.0` with
  `<release>${maven.compiler.release}</release>`, and `maven.compiler.release` is `25`.

### Items that cannot be executed in this environment

- **Runtime validation / `ENABLE_RUNTIME_VALIDATION=true`** is a pipeline-level switch owned by the
  upgrade runner; it is not set in this container and cannot be set from inside the cycle.
- **Deploying the WAR / booting WildFly** is outside what this cycle is permitted to do (the
  harness forbids executing the built application and leaving server processes running). The
  static analysis that informs that decision is below instead of a repeated "please deploy".

## Deployment decision: Java 25 bytecode vs. the javax stack (needs a human)

This is the one genuinely blocking question, and it is a platform decision rather than a code
defect, so it is written up rather than guessed at.

Facts established in this cycle:

- `target/classes` is **class file major version 69** (= Java 25), verified with `javap -v`. A JVM
  older than 25 cannot load it, so "deploy the Java 25 WAR on a JDK the server supports" is not an
  option — the bytecode level and the runtime JDK must match or the JDK must be newer.
- `version.wildfly` is `26.1.3.Final`. WildFly 26.x is the **last** WildFly line on the `javax.*`
  (Jakarta EE 8) namespace, and it targets JDK 8/11/17. WildFly 27 and later moved to `jakarta.*`.
- Therefore **no WildFly release both runs a `javax.*` application and supports JDK 25.** On this
  namespace the Java 25 compile target cannot be matched by a supported server. The upgrade is
  complete and correct as a *compile* target; it is the deployment platform that has no answer
  while the app stays on javax.

The three options, with what each costs:

| Option | Keeps javax? | What it needs |
| --- | --- | --- |
| **A. Separate, explicitly authorised jakarta cycle** — migrate `javax.*` → `jakarta.*`, descriptors to EE 10/11, PrimeFaces to a `jakarta`-classifier build, and move to WildFly 37+/40 | No | A namespace migration, which the current guardrail forbids without an explicit jakarta target. Note per doc 33 that Jakarta EE 11 is itself certified on SE 17/21 only, so even this does not make **Java 25** a certified deployment combination — it needs WildFly's own JDK 25 support statement. Docs 06, 30–34. |
| **B. Lower the compile target to 17** and deploy on WildFly 26.1.3 / JDK 17 | Yes | Also requires reverting two recipe modernizations that do not exist in Java 17: the 8 unnamed-variable `catch (… _)` bindings (Java 22+) in `CustomerService` (lines 60, 83), the 5 REST endpoints (line 76 each) and `ViewUtils` (line 43), and `Locale.of(language)` (Java 19+) in `LocaleBean:39`. This abandons the Java 25 goal, so it is not something this cycle should decide. |
| **C. Keep the Java 25 build, defer deployment** | Yes | Accept that the artifact is currently buildable and testable but has no supported server. Reasonable if the Java 25 move is being staged ahead of a planned jakarta migration. |

**Recommendation:** C now, A as the planned follow-up — i.e. keep this cycle's Java 25 build as the
compile-level deliverable and schedule the jakarta + WildFly move as its own authorised iteration,
since that is the only path that ends with both a modern JDK and a supported server. Do not pick B
without an explicit decision to abandon the Java 25 target.

## Next steps (non-blocking; for human review)

1. **Arquillian integration tests are not executed.** 25 `*IT` classes exist and all of them
   **compile** under Java 25, but `failsafe` skips them because `skipITs=true` is the project's own
   pre-existing default (set by the repo's "Separate Unit and Integration tests" commit). They need
   a WildFly container via the `arquillian-wildfly-remote` or `arquillian-wildfly-managed` profile.
   Nothing was disabled, skipped or weakened by this upgrade. Running them requires standing up
   WildFly, which is out of scope for a build-green cycle — please run
   `mvn verify -Parquillian-wildfly-managed` against a real container to validate runtime
   behaviour, especially the `javax.xml.bind` marshalling in `CategoryIT` / `ProductIT` / `ItemIT`
   (which now rely on the container's JAXB implementation rather than a bundled `jaxb-runtime`).
2. **Pick a deployment platform — the one real blocker.** See "Deployment decision: Java 25
   bytecode vs. the javax stack" above for the established facts (class file major 69; WildFly 26.x
   is the last javax line and targets JDK 8/11/17; no WildFly release runs javax on JDK 25) and the
   three costed options. Recommendation: keep the Java 25 build now, schedule the jakarta + WildFly
   move as its own authorised iteration.
3. **`DebugBean` mixes JSF and CDI scopes.** `src/main/java/.../view/DebugBean.java` uses
   `javax.faces.bean.RequestScoped` (the JSF 2 managed-bean annotation) together with
   `javax.inject.Named`, so it has no CDI scope and behaves as `@Dependent`. This is pre-existing,
   compiles fine on the Java EE 7 stack, and was not touched because fixing it changes runtime bean
   behaviour rather than compilation. If the bean is ever ported to a jakarta line, that import is
   gone and must become `javax.enterprise.context.RequestScoped` /
   `jakarta.enterprise.context.RequestScoped` (knowledge base doc 30).
4. **Review this `summary.md` and decide whether to keep, squash or drop it** as part of the PR
   review — it is an untracked file the upgrade pass added at the project root, not part of the
   application.
   *(The former item 4, `IO.println` in `LoginContextProducer`, was fixed in iteration 16 — see
   "Iteration 16 changes".)*
5. **`beans.xml` keeps `bean-discovery-mode="all"`.** Correct and intentional on CDI 1.2 / EE 7.
   Flagged only because knowledge base doc 32 documents the silent default flip to `annotated` in
   CDI 4 — relevant only if a future cycle moves this app to Jakarta EE 10/11.
6. **Cosmetic plugin warning.** `swagger-maven-plugin:3.1.8` logs two
   "problem ... building the effective model for org.javassist:javassist" warnings. They come from
   the plugin's own transitive POMs, do not affect the build, and the `generate` goal succeeds.
7. **Dated but working dependencies left alone** (all javax, all green): PrimeFaces 6.1,
   EqualsVerifier 3.12.3, Log4j2 2.19.0, RESTEasy 3.15.3, Arquillian 1.5.0.0. They compile and
   test clean on Java 25; bumping them was not needed to reach zero errors and PrimeFaces in
   particular cannot be bumped far without crossing to jakarta (doc 31).
8. **Re-run the pipeline's runtime validation** with `ENABLE_RUNTIME_VALIDATION=true` if an
   app-boot check is wanted. That switch belongs to the upgrade runner and is not set in this
   container, so the cycle cannot enable it from the inside. Note that an app-boot check depends on
   item 2 being decided first: booting the Java 25 WAR needs a server that supports JDK 25, which
   the current javax stack does not have.
9. **Doc 36 runtime silent-break checklist does not apply to this module.** Neither trigger is met:
   the Hibernate 7 step did not run (no `hibernate-core` declared — JPA comes from the EE 7 BOM and
   the provider is the container's) and no Jakarta EE 10/11 target was selected, so the CDI 4
   `beans.xml` discovery flip and the `@Named`-without-scope resolution change are not in play. The
   module also writes no PDF, Excel or JasperReports output, so the golden-master scaffolding in
   doc 36 Part 2 has no output path to capture. Both become relevant if item 2 is resolved via the
   jakarta route — `DebugBean` (item 3) is exactly the `@Named`-without-a-CDI-scope pattern doc 32
   warns about.
