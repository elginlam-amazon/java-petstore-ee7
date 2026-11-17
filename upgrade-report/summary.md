# Java 25 Upgrade — Executive Report  
  
**Application:** Petstore EE7 (`org.agoncal.application:petstoreee7`)  
**Date:** 2026-11-17  
**Branch:** `main-upgrade`  
  
---  
  
## 1. Executive Summary  
  
The Petstore Java EE 7 WAR application was successfully upgraded from its prior Java version to **Java 25** using an automated OpenRewrite pass (`com.amazonaws.java.migrate.UpgradeToJava25` via rewrite 6.42.0), followed by a Claude CLI build-fix validation cycle. OpenRewrite modified 22 files (1 POM + 21 Java sources) across a staged migration chain (Java 8 → 11 → 17 → 21 → 25). The post-upgrade build completed with **zero compilation errors**, 68 main source files and 34 test source files compiled successfully against Java 25, and all **9 unit tests passed** (0 failures, 0 skipped). No manual code fixes were required — the OpenRewrite pass left the codebase in a fully buildable state. Spring Boot upgrade was **not requested** and was not attempted.  
  
---  
  
## 2. Application Changes  
  
- 🏗️ **Build target** — `maven.compiler.source` and `maven.compiler.target` bumped to `25`  
- 📦 **New runtime dependencies added to `pom.xml`:**  
  - `jakarta.inject:jakarta.inject-api:1.0.3`  
  - `jakarta.xml.bind:jakarta.xml.bind-api:2.3.3`  
  - `org.glassfish.jaxb:jaxb-runtime:2.3.9` (runtime scope)  
  - `jakarta.annotation:jakarta.annotation-api:1.3.5` (provided scope)  
- 🔒 **Byte Buddy pinned** in `dependencyManagement` to `1.17.8` (required to read Java 21+ class files)  
- 🔌 **Maven plugins upgraded:**  
  - `maven-surefire-plugin` → `3.5.6`  
  - `maven-failsafe-plugin` → `3.5.6`  
  - `maven-war-plugin` → `3.5.1`  
- 📝 **21 Java source files modified** across security, REST, service, view, and test layers  
  
---  
  
## 3. Tools Used  
  
| Tool | Version | Role |  
|------|---------|------|  
| ☕ **Java SDK** | Java 25 | Compilation target |  
| 🔁 **OpenRewrite** | `rewrite:6.42.0` | Automated migration recipe runner |  
| 📋 **Recipe** | `com.amazonaws.java.migrate.UpgradeToJava25` | Staged Java 8→11→17→21→25 upgrade |  
| 🤖 **Claude CLI** | `2.1.266` (Claude Code) | Automated build-fix and report generation |  
| 🧠 **Claude Model** | `global.anthropic.claude-sonnet-4-6` | AI-assisted analysis and verification |  
| 🏗️ **Maven** | (project default) | Build system |  
  
---  
  
## 4. Code Changes  
  
### `pom.xml`  
- ☕ Java compiler source/target set to `25`  
- ➕ Added `jakarta.inject-api`, `jakarta.xml.bind-api`, `jaxb-runtime`, `jakarta.annotation-api`  
- 🔒 Pinned `net.bytebuddy:byte-buddy:1.17.8` in dependency management  
- 🔌 Upgraded `maven-surefire-plugin`, `maven-failsafe-plugin`, `maven-war-plugin`  
  
### Security layer  
- 🔄 `LoginContextProducer.java` — `System.out` calls replaced with `IO.print` (`ReplaceSystemOutWithIOPrint`); unused variables replaced with `_` (`ReplaceUnusedVariablesWithUnderscore`)  
- 🔄 `SimpleCallbackHandler.java` — `instanceof` checks modernised to pattern-matching (`InstanceOfPatternMatch`)  
  
### REST endpoints (5 files)  
- 🔄 `CategoryEndpoint`, `CountryEndpoint`, `CustomerEndpoint`, `ItemEndpoint`, `ProductEndpoint` — unused variables replaced with `_` (`ReplaceUnusedVariablesWithUnderscore`)  
  
### Service layer  
- 🔄 `CustomerService.java` — unused variables replaced with `_`  
  
### View layer  
- 🔄 `LocaleBean.java` — `new Locale(...)` constructor calls replaced with `Locale.of(...)` factory (`UseLocaleOf`)  
- 🔄 `ViewUtils.java` — unused variables replaced with `_`  
  
### Admin beans (6 files)  
- 🔄 `CategoryBean`, `CountryBean`, `CustomerBean`, `ItemBean`, `OrderLineBean`, `ProductBean`, `PurchaseOrderBean` — `@Serial` annotation added to `serialVersionUID` fields (`AddSerialAnnotationToSerialVersionUID`)  
  
### Integration tests (4 files)  
- 🔄 `OrderLineServiceIT`, `PurchaseOrderServiceIT`, `PurchaseOrderBeanIT`, `OrderLineBeanIT` — deprecated `new Integer(...)` / `new Long(...)` wrapper constructors replaced with `Integer.valueOf(...)` / `Long.valueOf(...)` (`PrimitiveWrapperClassConstructorToValueOf`)  
  
---  
  
## 5. Time Savings Estimate  
  
| Activity | Manual Effort | Automated |  
|----------|--------------|-----------|  
| POM version bumps + dependency additions | ~45 min | ✅ Automated |  
| Plugin version research & updates | ~30 min | ✅ Automated |  
| `instanceof` pattern-match refactors | ~20 min | ✅ Automated |  
| Primitive wrapper constructor replacements | ~15 min | ✅ Automated |  
| `@Serial` annotation additions | ~20 min | ✅ Automated |  
| `Locale.of()` factory migration | ~10 min | ✅ Automated |  
| Unused variable `_` replacements | ~15 min | ✅ Automated |  
| Build validation & test run | ~15 min | ✅ Automated |  
| **Total** | **~2h 50m** | **~0 min manual** |  
  
> 💡 OpenRewrite self-reported estimate: **2h 48m** saved. The automated pipeline (OpenRewrite + Claude CLI) reduced manual engineering effort to effectively zero for this upgrade.  
  
---  
  
## 6. Next Steps  
  
### ✅ Validation steps  
- 🧪 Run integration tests against a live WildFly container: `mvn verify -Parquillian-wildfly-remote` (requires a running WildFly 26.1.3 instance)  
- 🐳 Validate WAR deployment on WildFly 26.x — confirm the app boots cleanly and all REST endpoints respond  
- 🔍 Review Swagger/OpenAPI generated output at `src/main/webapp/swagger.json` for correctness after the upgrade  
  
### ⚠️ Improvement recommendations  
- 📦 **WildFly compatibility** — WildFly 26.x ships with Java EE 8 / Jakarta EE 8 APIs; Java 25 is not officially supported on this release line. Consider upgrading to **WildFly 32+** (Jakarta EE 10) which has validated Java 21/25 support  
- 🔄 **Spring Boot upgrade not performed** — `springBootTargetVersion=none` was passed; no Spring is in use in this project (pure Java EE)  
- 🔌 **PrimeFaces 6.1 is EOL** — upgrade path to PrimeFaces 12–14 on WildFly 32+ should be evaluated alongside the WildFly upgrade  
- 🔒 **Arquillian REST extension `1.0.0.Alpha4`** — still on a pre-release version; check for a stable release compatible with Java 25  
- 🛡️ **`javassist` deprecation warnings** — two warnings for `javassist:3.21.0-GA` and `3.18.2-GA` during the Swagger plugin phase; evaluate switching to Byte Buddy-based introspection  
- 📋 **`maven-compiler-plugin` on 3.1** — very old; upgrade to `3.13+` to get better Java 25 annotation processor support

## Verification results

Copied from this iteration's report.json, progress.json and runtime-validation.log after the final build. This section is not written by the AI.

- 🛠️ Post-upgrade verification build: SUCCESS
- 🧪 Automated tests: 9 tests run
- ⏱️ Transformation duration: 7 minute(s)
- ⏳ Estimated time saved: 78 minute(s)
- 🚦 Runtime validation (runtime-validation.log):
  - OK (static): no Java-17-incompatible Spring detected (effective Spring n/a: no build file pins a Spring Framework version; a Spring Boot parent or imported BOM, if present, manages it).
  - Smoke-start tier skipped (opt-in: export ENABLE_RUNTIME_VALIDATION=true to enable an advisory app-boot check).

## Claude CLI usage

💳 Estimated Claude CLI usage (USD): 0.608174  
