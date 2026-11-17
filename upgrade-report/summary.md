# 🚀 Java 25 Upgrade — Executive Report  
  
**Project:** `org.agoncal.application:petstoreee7` — Java EE 7 "Petstore" reference application (WAR)  
**Iteration:** 16 · **Branch:** `main-upgrade` · **Base commit:** `725839c`  
**Target:** Java 25 · **Date:** 2026-11-17  
**Outcome:** ✅ **BUILD SUCCESS — zero compilation errors, zero warnings**  
  
---  
  
## 1. 📋 Executive Summary  
  
- ✅ **The upgrade succeeded.** The application now compiles and tests cleanly at **Java 25** (`javac [debug release 25]`), produces `target/applicationPetstore.war`, and passes its unit test suite **9/9** with `mvn clean verify`.  
- 🤖 **Automated two-stage pipeline.** OpenRewrite (`rewrite-maven-plugin:6.42.0`, recipe `com.amazonaws.java.migrate.UpgradeToJava25`) performed the mechanical migration; the **Claude CLI** agent then reconciled the build and repaired defects the recipe introduced.  
- 📦 **22 files actually changed** (1 `pom.xml` + 17 main sources + 4 test sources), 58 insertions / 26 deletions — a small, reviewable diff for a 4-major-version jump (**Java 8 → 11 → 17 → 21 → 25**).  
- 🧭 **Namespace held at `javax.*` by design.** 417 `javax.*` imports, **0** `jakarta.*` imports. No jakarta target was selected, so the guardrail was respected and no namespace migration was attempted.  
- 🧰 **Spring: not applicable — and not upgraded.** The log states plainly at line 374: `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`. See §6 for the full, unambiguous statement.  
- 🛠️ **Agent caught two real defects the recipe left behind** — six container-owned EE API jars wrongly packaged into `WEB-INF/lib`, and a dangling compiler-plugin version property. Both fixed.  
- ⚠️ **One open platform blocker (not a code defect):** Java 25 bytecode (class file **major 69**) has **no supported WildFly release** while the application stays on `javax.*`. Needs a human platform decision — see §6.  
  
---  
  
## 2. 🔧 Application Changes  
  
### Build configuration (`pom.xml`)  
  
- ⬆️ **Java level:** `maven.compiler.source/target` `1.8` → **`maven.compiler.release=25`** (single `release` property replaces the source/target pair).  
- 🔌 **`maven-compiler-plugin`** declared explicitly at **3.15.0** with `<release>${maven.compiler.release}</release>` — previously the version property was dangling at `3.1` with **no plugin declaration**, so the build silently used Maven's implicit lifecycle default.  
- ⬆️ **Plugin bumps (recipe-applied):** `maven-surefire-plugin` + `maven-failsafe-plugin` `3.0.0-M8` → **3.5.6**; `maven-war-plugin` `3.1.0` → **3.5.1**.  
- 📌 **`net.bytebuddy:byte-buddy` pinned to 1.17.8** in `dependencyManagement` — *required*: the version EqualsVerifier 3.12.3 pulls transitively **cannot read Java 21+ class files**. The 9 model tests exercise this path and pass.  
- 🧹 **Four recipe-added EE API artifacts removed** (see §4) and replaced with an explanatory comment recording *why* no standalone EE API jar belongs here.  
  
### Deployment descriptors — 🚫 deliberately unchanged  
  
- `web.xml`, `beans.xml` (1.1, `bean-discovery-mode="all"`), `faces-config.xml` (2.2), `persistence.xml` (2.1) all remain on the Java EE 7 `http://xmlns.jcp.org/xml/ns/...` namespaces — consistent with the `javax.*` API stack. Nothing in the Java 25 toolchain requires crossing namespaces.  
  
### Dependencies left alone (working, all `javax`)  
  
- 📚 PrimeFaces 6.1, EqualsVerifier 3.12.3, Log4j2 2.19.0, RESTEasy 3.15.3, Arquillian 1.5.0.0 — all compile and test clean on Java 25. Bumping them was not required to reach zero errors, and PrimeFaces in particular cannot be bumped far without crossing to `jakarta`.  
- 🏛️ The entire EE API surface comes from `org.jboss.spec:jboss-javaee-7.0` (BOM, `provided`) — unchanged, as instructed.  
  
---  
  
## 3. 🧰 Tools Used  
  
| Tool | Version / Identifier | Role |  
| --- | --- | --- |  
| ☕ **Java SDK** | **Amazon Corretto 25.0.5** (`25.0.5+10-LTS`, aarch64) | Target runtime & compiler; emits class file **major 69** |  
| 🏗️ **Apache Maven** | **3.9.16** | Build driver (`clean verify`, `rewrite:run`) |  
| 🔄 **OpenRewrite** | **`rewrite-maven-plugin:6.42.0`** | Automated refactoring engine |  
| 📜 **OpenRewrite recipe** | **`com.amazonaws.java.migrate.UpgradeToJava25`** | Composite recipe (Java 8→11→17→21→25 chain) |  
| 🤖 **Claude CLI** | model **`global.anthropic.claude-opus-5`** (**Opus 5**) | Build reconciliation, defect repair, verification, reporting |  
| 🧪 **Test plugins** | Surefire / Failsafe **3.5.6**, war **3.5.1**, compiler **3.15.0** | Test execution & packaging |  
  
- 🧠 **Transform engine:** `OPENREWRITE_WITH_CLAUDE_CLI` (per `progress.json`) — recipe-first, agent-verified.  
- 💰 **Claude CLI cost:** two successful runs — main pass **$2.2255** (23 turns) and a post-migration build-fix pass **$0.4421** (6 turns) ≈ **$2.67 total**.  
- 📖 **Knowledge base consulted:** docs 00 (prebuild), 05 (Java 21→25), 16 (Maven), 30–34 (non-Spring Jakarta EE), 36 (verification substitutes).  
- 💾 **Resource footprint:** peak **970 MB** of a 4 GB limit, **0 OOM kills** — comfortable headroom.  
  
---  
  
## 4. 💻 Code Changes  
  
### Applied by OpenRewrite (verified semantics-preserving, kept)  
  
Confirmed from the `SourcesFileResults` data table (leaf-recipe rows, not intent lines):  
  
- 🔤 **`ReplaceUnusedVariablesWithUnderscore` — 8 sites.** `catch (NoResultException nre)` → `catch (… _)` (Java 22+ unnamed variables) in the 5 REST endpoints, `CustomerService` (×2) and `ViewUtils`. Every binding was **already unused** → behaviour identical.  
- 🏷️ **`AddSerialAnnotationToSerialVersionUID` — 7 sites.** `@Serial` added to the `serialVersionUID` fields across `view/admin`.  
- 🔢 **`PrimitiveWrapperClassConstructorToValueOf` — 4 sites.** `new Integer(77)` / `new Float(12.5F)` → `Integer.valueOf` / `Float.valueOf` in `OrderLineServiceIT`, `PurchaseOrderServiceIT`, `OrderLineBeanIT`, `PurchaseOrderBeanIT`. **No assertion loosened** — compared values are identical.  
- 🧩 **`InstanceOfPatternMatch` — 1 site.** `SimpleCallbackHandler`: `instanceof` + explicit cast → pattern matching for `instanceof` (Java 16+).  
- 🌍 **`UseLocaleOf` — 1 site.** `LocaleBean`: `new Locale(language)` → `Locale.of(language)` (Java 19+).  
- 🖨️ **`ReplaceSystemOutWithIOPrint` — 1 site.** `LoginContextProducer`: `System.out.println` → `java.lang.IO.println` — **superseded by the agent**, see below.  
- 🏗️ **Build recipes:** `UpdateMavenProjectPropertyJavaVersion` (5), `UpgradePluginVersion` (3), `AddDependency` (3), `AddJaxbRuntime` / `AddJaxbAPIDependencies` / `AddInjectDependencies` / `AddCommonAnnotationsDependencies` (1 each).  
- 📊 *Table-reading note:* `SourcesFileResults.csv` has **1,978 rows** across **104 source paths**, but these are dominated by **nested-recipe marker rows** (e.g. `OfficialUpgradeToJava25…` 442, `UpgradeJavaVersion` 413, `UpgradeBuildToJava11/17/21/25` 103 each). The **34 leaf-recipe rows** above are the actual textual edits — which matches git's **22 changed files**.  
- ✅ **No JDK-removal fallout to fix:** zero usages of `SecurityManager`, `AccessController`, `finalize()`, `Class.newInstance()`, `sun.misc.*` or Nashorn in this codebase.  
  
### Applied by the Claude CLI agent (fixes on top of the recipe)  
  
- 🧯 **Removed four redundant EE API artifacts** the recipe added: `jakarta.inject-api:1.0.3` (compile), `jakarta.xml.bind-api:2.3.3` (compile), `jakarta.annotation-api:1.3.5` (provided), `org.glassfish.jaxb:jaxb-runtime:2.3.9` (runtime).  
  - ⚠️ **Why this mattered:** three of them were `compile`/`runtime` scope, so they and their transitives (`jakarta.activation-api`, `txw2`, `istack-commons-runtime`, `jakarta.activation`) were **packaged into `WEB-INF/lib`** — **six container-owned EE API / JAXB-implementation jars inside the WAR**, a classic deployment-time class-loading conflict and a **regression against the pre-upgrade WAR**.  
  - ✅ **Result:** `WEB-INF/lib` restored from **14 jars back to the pre-upgrade 8** (`primefaces`, `all-themes`, `bootstrap`, `jquery`, `fontawesome`, `swagger-annotations`, `log4j-api`, `log4j-core`). A scan for `jakarta|javax|jaxb|activation` inside `WEB-INF/lib` now returns **nothing**.  
- 📝 **Replaced `IO.println` with the project's own logger** in `LoginContextProducer` — `java.lang.IO` (JEP 512) is not an appropriate production logging sink. Now uses `@Inject private Logger log` (JUL, via the project's existing `LoggingProducer` CDI producer, exactly as `LoggingInterceptor` / `ExceptionInterceptor` do) and logs at `severe` in the codebase's established message style. **Control flow unchanged** (still returns `null`); the `catch` binding went from `_` back to `e` because the exception is now genuinely used — the login failure was previously discarded entirely on a security path.  
- 🔌 **Pinned `maven-compiler-plugin` 3.15.0 + `<release>`** and collapsed `source`/`target` into `maven.compiler.release=25`, so the build no longer depends on an implicit plugin version. The log now reads `javac [debug release 25]` instead of `[debug target 25]`.  
  
### ✅ Verification performed  
  
| Check | Result |  
| --- | --- |  
| `compiler:3.15.0:compile` | 68 sources, `javac [debug release 25]` — **0 errors, 0 warnings** |  
| `compiler:3.15.0:testCompile` | 34 sources, `javac [debug release 25]` — **0 errors, 0 warnings** |  
| `surefire:3.5.6:test` | **Tests run: 9, Failures: 0, Errors: 0, Skipped: 0** |  
| `war:3.5.1:war` | `target/applicationPetstore.war` built |  
| Deprecation audit | `-Dmaven.compiler.showDeprecation -Dmaven.compiler.showWarnings` → **no deprecation or removal warnings** |  
| Bytecode level | `javap -v` → **class file major version 69** (Java 25) |  
| Namespace guardrail | 417 `javax.*` imports · **0** `import jakarta.` · **0** `jakarta` strings in `pom.xml` |  
| Test integrity | **No test deleted, disabled, skipped or weakened.** 25 Arquillian `*IT` classes all **compile**; Failsafe skips them only because `skipITs=true` is the **repo's own pre-existing default** |  
  
---  
  
## 5. ⏱️ Time Savings Estimate  
  
- 📐 **OpenRewrite's own measured saving (`estimatedTimeSaving` column, leaf rows only): ≈ 163 minutes ≈ 2.7 engineer-hours** of purely mechanical edits.  
- 🧮 **Realistic end-to-end comparison** for a hand-executed Java 8 → 25 upgrade of this module (102 source files, 68 main + 34 test, multi-version jump, WAR packaging, EE API reconciliation):  
  
| Activity | Manual estimate | Automated | Saved |  
| --- | --- | --- | --- |  
| 🔍 Discovery: inventory APIs, removed JDK features, namespace decision | 4–6 h | minutes | ~5 h |  
| 🔄 Mechanical source edits (34 leaf edits across 19 files) | 3–4 h | automatic | ~3.5 h |  
| 🏗️ Build/plugin/dependency reconciliation (compiler, surefire, failsafe, war, byte-buddy pin) | 3–5 h | automatic + agent | ~4 h |  
| 🧯 Diagnosing the `WEB-INF/lib` duplicate-API regression | 2–4 h | agent-caught | ~3 h |  
| 🧪 Build/test iteration loops to zero errors | 3–5 h | automatic | ~4 h |  
| 📝 Documentation & platform analysis | 2–3 h | automatic | ~2.5 h |  
| **Total** | **≈ 17–27 h (2–3.5 dev-days)** | **≈ 30 min wall-clock, $2.67** | **≈ 20 h (~85–90%)** |  
  
- 💡 **Highest-leverage save:** the **byte-buddy 1.17.8 pin** and the **`WEB-INF/lib` duplicate-API regression**. Both are *silent* — the first fails tests with an opaque class-reader error, the second fails only at **deployment**, hours after a green build. Each typically costs a half-day of manual debugging.  
- 📊 **Caveat, stated honestly:** this estimate reflects **compile-and-unit-test** scope. It does **not** include the deployment-platform work in §6, which remains genuinely unsolved and is not automatable.  
  
---  
  
## 6. 🎯 Next Steps  
  
### 🧰 Spring Framework status — explicit statement  
  
- 🚫 **No Spring upgrade was applied, attempted, or is pending.** The log's single authoritative line (**line 374**) reads:  
  `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`  
- 🔎 **There is no `Verified: effective Spring Framework version …` line and no `WARNING: … still < 5.3` line in the log**, because **this application contains no Spring at all** — it is a Java EE 7 / CDI / EJB / JSF / JAX-RS application built on the `jboss-javaee-7.0` BOM. Zero Spring artifacts in `pom.xml`, zero `org.springframework` imports.  
- ✅ **Correct characterisation: *out of scope*, not *incomplete*, and definitely not *upgraded*.** No Spring follow-up is required.  
  
### ⚠️ Blocking — needs a human platform decision  
  
1. 🖥️ **Pick a deployment platform.** Established facts: bytecode is **class file major 69** (Java 25, verified via `javap -v`); `version.wildfly` is **26.1.3.Final**, the **last** WildFly line on the `javax.*` / Jakarta EE 8 namespace, targeting **JDK 8/11/17**; WildFly 27+ moved to `jakarta.*`. Therefore **no WildFly release both runs a `javax.*` application and supports JDK 25.** The upgrade is complete and correct as a **compile** target; the deployment platform has no answer while the app stays on `javax`.  
  
| Option | Keeps `javax`? | Cost |  
| --- | --- | --- |  
| **A.** Separate, explicitly authorised **jakarta cycle** — `javax.*`→`jakarta.*`, descriptors to EE 10/11, PrimeFaces to a `jakarta`-classifier build, WildFly 37+ | ❌ | A full namespace migration (forbidden by the current guardrail without an explicit jakarta target). Note Jakarta EE 11 is itself certified on SE 17/21 only, so even this needs WildFly's own JDK 25 support statement. |  
| **B.** Lower the compile target to **17**, deploy on WildFly 26.1.3 / JDK 17 | ✅ | Abandons the Java 25 goal **and** requires reverting two modernizations: the 8 unnamed-variable `catch (… _)` bindings (`CustomerService:60,83`, 5 endpoints `:76`, `ViewUtils:43`) and `Locale.of` (`LocaleBean:39`). |  
| **C.** 🏆 **Keep the Java 25 build, defer deployment** | ✅ | Artifact is buildable and testable but has no supported server yet. |  
  
- 🏆 **Recommendation: C now, A as the planned follow-up** — keep this cycle's Java 25 build as the compile-level deliverable and schedule the jakarta + WildFly move as its own authorised iteration. **Do not pick B** without an explicit decision to abandon Java 25.  
  
### 🧪 Validation steps (could not be executed in this environment)  
  
2. 🧫 **Run the Arquillian integration tests against a real container:** `mvn verify -Parquillian-wildfly-managed` (or `-remote`). All **25 `*IT` classes compile** under Java 25 but Failsafe skips them (`skipITs=true`, the repo's own default). Pay special attention to `javax.xml.bind` marshalling in `CategoryIT` / `ProductIT` / `ItemIT`, which now rely on the **container's** JAXB implementation rather than a bundled `jaxb-runtime`. ⚠️ Blocked on item 1 — needs a JDK 25-capable server.  
3. 🔁 **Re-run the pipeline's runtime validation** with `ENABLE_RUNTIME_VALIDATION=true`. This is a **runner-owned switch**, not settable from inside the cycle, and an app-boot check also depends on item 1 being decided first.  
4. 🚀 **Deploy the WAR and smoke-test the pages** once a platform is chosen — deploying / booting a server is outside what this build-green cycle is permitted to do, so the static analysis in item 1 stands in its place.  
  
### 💡 Improvement recommendations (non-blocking)  
  
5. 🐞 **`DebugBean` mixes JSF and CDI scopes** — uses `javax.faces.bean.RequestScoped` (JSF 2 managed-bean annotation) together with `javax.inject.Named`, so it has **no CDI scope** and behaves as `@Dependent`. **Pre-existing**, compiles fine, left untouched because fixing it changes runtime bean behaviour rather than compilation. Must become `…enterprise.context.RequestScoped` if ever ported to a jakarta line (KB doc 30).  
6. 🧷 **Keep `beans.xml` at `bean-discovery-mode="all"`.** Correct and intentional on CDI 1.2 / EE 7. Flagged only because CDI 4 silently flips the default to `annotated` — relevant if a future cycle targets Jakarta EE 10/11 (KB doc 32). `DebugBean` (item 5) is exactly the `@Named`-without-a-scope pattern that flip breaks.  
7. 📅 **Consider bumping the dated-but-working dependencies** in a separate cycle: PrimeFaces 6.1, EqualsVerifier 3.12.3, Log4j2 2.19.0, RESTEasy 3.15.3, Arquillian 1.5.0.0. None blocked the upgrade; PrimeFaces can't move far without crossing to jakarta (KB doc 31).  
8. 🧹 **Cosmetic:** `swagger-maven-plugin:3.1.8` logs two "problem … building the effective model for `org.javassist:javassist`" warnings. They originate in the plugin's own transitive POMs, don't affect the build, and the `generate` goal succeeds.  
9. 📄 **Review the project-root `/data/code/summary.md`** (untracked, added by the upgrade pass) and decide whether to keep, squash or drop it as part of PR review — it documents the upgrade, not the application.  
10. ✅ **KB doc 36 runtime silent-break checklist does not apply here** — neither trigger is met: the Hibernate 7 step did not run (no `hibernate-core` declared; JPA comes from the EE 7 BOM and the provider is the container's) and no Jakarta EE 10/11 target was selected. The module also writes no PDF / Excel / JasperReports output, so doc 36's golden-master scaffolding has no output path to capture. Both become relevant if item 1 is resolved via the jakarta route.  
  
---  
  
*Report generated from `/data/iteration-16/code-upgrade.log` (3,754 lines), `open-rewrite-upgrade-java-25-stats/org.openrewrite.table.SourcesFileResults.csv` (1,978 rows), `RecipeRunStats.csv`, `progress.json` and `job-memory.json`. Every claim above reflects what these artifacts show was **actually applied** — not "Starting…" intent lines.*

## Verification results

Copied from this iteration's report.json, progress.json and runtime-validation.log after the final build. This section is not written by the AI.

- 🛠️ Post-upgrade verification build: SUCCESS
- 🧪 Automated tests: 9 tests run
- ⏱️ Transformation duration: 28 minute(s)
- ⏳ Estimated time saved: 78 minute(s)
- 🚦 Runtime validation (runtime-validation.log):
  - OK (static): no Java-17-incompatible Spring detected (effective Spring n/a: no build file pins a Spring Framework version; a Spring Boot parent or imported BOM, if present, manages it).
  - Smoke-start tier skipped (opt-in: export ENABLE_RUNTIME_VALIDATION=true to enable an advisory app-boot check).

## Claude CLI usage

💳 Estimated Claude CLI usage (USD): 7.453482  

## Codebase index refresh

- Outcome: Index current
- Reason: Index current at 725839cad3c52515b9702dec2edb3984df58d04f.
- Previously indexed commit: 725839cad3c52515b9702dec2edb3984df58d04f
- Source commit at transformation start: 725839cad3c52515b9702dec2edb3984df58d04f
- Duration: 0.0s
