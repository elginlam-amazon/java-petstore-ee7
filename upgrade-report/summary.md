# 📊 Executive Report: Petstore Java EE 7 → Java 25 / Jakarta EE Upgrade (Iteration 5)  
  
## 1. 🧭 Executive Summary  
  
The Petstore web application (`org.agoncal.application:petstoreee7`, a single-module WAR) was moved from Java 8 / Java EE 7 to **Java 25** and the **`jakarta.*` namespace**. OpenRewrite changed 144 files in 1 min 36 s. Its output did not compile, so Amazon Kiro CLI fixed the build in about 19 minutes. ✅ The final `mvn clean verify` passes on JDK 25 with zero compilation errors and 9/9 unit tests green. ⚠️ The pipeline requested **Jakarta EE 11** (`jakartaEeTarget=11`), but the dependencies currently target **Jakarta EE 10** (`jakarta.jakartaee-api:10.0.0`), so the EE 11 alignment still needs follow-up. ℹ️ No Spring upgrade applies: the project has no Spring dependencies, and the log says `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`.  
  
| Item | Before | After (verified) |  
|---|---|---|  
| ☕ Java | 1.8 | 25 (`maven.compiler.source/target`) |  
| 🏛️ EE platform | Java EE 7 (`jboss-javaee-7.0`, javax) | Jakarta EE **10** API (`jakarta.jakartaee-api:10.0.0`), jakarta namespace. EE 11 was requested and is not complete ⚠️ |  
| 🌱 Spring | n/a | n/a (no Spring in the project; upgrade skipped) |  
| 🧱 Build | ✅ initial build passed | ✅ `mvn clean verify`: BUILD SUCCESS, 9/9 unit tests |  
| 🧪 Integration tests | skipped by default | not run (`skipITs=true`) |  
  
## 2. 🏗️ Application Changes  
  
- ☕ **Java baseline**: Java 1.8 → 25. The build plugins were upgraded by the recipe (surefire/failsafe 3.0.0-M8 → 3.5.6, war 3.1.0 → 3.5.1).  
- 🔀 **Namespace migration**: `javax.*` → `jakarta.*` for persistence, validation, EJB, CDI, inject, interceptor, decorator, JAX-RS, XML binding, annotation and Faces, in main and test code.  
- ✅ **Correctly kept on javax**: the JDK packages `javax.security.auth.*` (JAAS login module and callbacks) and `javax.naming.*`.  
- 📄 **Descriptors**: `persistence.xml` (3.0), `validation.xml` (3.0), `beans.xml` (4.0), `faces-config.xml` (4.0), `web.xml` (5.0) and `forge.taglib.xml` (Faces 4.0 schema) now use the jakarta.ee namespaces.  
- 🖥️ **UI**: 35 `.xhtml` pages were migrated by the Faces recipes. PrimeFaces 6.1 → 14.0.18 (`jakarta` classifier).  
- 📦 **Packaging**: EE APIs are now `provided` and not bundled. The WAR contains only PrimeFaces, themes, webjars, log4j and the swagger annotations.  
- 📘 **API contract**: `swagger.json` was regenerated as **OpenAPI 3.0.1** (previously Swagger 2.0). It has the same 10 paths, 5 tags and 25 operation summaries, and the server URL is `http://localhost:8080/applicationPetstore/rest`.  
- 🖧 **Target runtime**: `version.wildfly` 26.1.3.Final (javax only) → 39.0.1.Final.  
  
## 3. 🛠️ Tools Used  
  
- ☕ **Java SDK**: Amazon Corretto **25.0.4.1** LTS (`openjdk 25.0.4.1 2026-08-18 LTS`), Apache Maven 3.9.13.  
- 🔄 **OpenRewrite**: `rewrite-maven-plugin` **6.42.0**, run in this order:  
  1. `com.amazonaws.java.migrate.UpgradeToJakartaEE11`, which wraps `org.openrewrite.java.migrate.jakarta.JakartaEE11`  
  2. `com.amazonaws.java.migrate.UpgradeToJava25`, which wraps `org.openrewrite.java.migrate.UpgradeToJava25`  
  - 📈 SourcesFileResults: **144 files changed** (102 `.java`, 35 `.xhtml`, 7 `.xml`).  
  - 🔝 Most-applied sub-recipes: `UpgradeJavaVersion` (413 results), `JavaxMigrationToJakarta` (210), `ChangePackage` (184), `FindAndReplace` (117), `Faces3xMigrationToFaces4x` (115), `JakartaFacesXhtmlEE10` (111).  
  - ⚠️ `UpdateJakartaPlatform11` produced only 1 result, a pom.xml change that left no EE 11 platform dependency in place. That gap is why the EE 11 alignment is still open.  
- 🤖 **Amazon Kiro CLI** `kiro-cli 2.14.2`, model **claude-opus-5.5**: automated build fix. The session took **18 min 57 s** and used 16.39 credits.  
- 🧠 **CodeKB**: indexed the pre-upgrade code, used to understand components (for example `SimpleCallbackHandler.handle`) before changing them.  
  
## 4. 💻 Code Changes  
  
### 🔄 Applied by OpenRewrite  
- 🔀 `javax` → `jakarta` imports across all entities, services, REST endpoints, JSF beans, CDI producers, interceptors and tests.  
- ➕ Added individual `jakarta.*` API dependencies (compile scope), `jaxb-runtime` 2.3.9, and the PrimeFaces `jakarta` classifier.  
- ✨ Java language modernisations:  
  - `IfElseIfConstructToSwitch`  
  - `ReplaceSystemOutWithIOPrint` (`System.out.println` → `IO.println`)  
  - `ReplaceUnusedVariablesWithUnderscore` (`catch (Exception _)`)  
  - `AddSerialAnnotationToSerialVersionUID`  
  - `PrimitiveWrapperClassConstructorToValueOf`  
  - `UseLocaleOf`  
- ❌ **Broke compilation**: the `instanceof` → pattern-switch rewrite in `SimpleCallbackHandler.handle` left unexpanded template text (`__P__…`), which caused 4 compile errors.  
  
### 🤖 Fixed by Kiro CLI  
- 🩹 **`SimpleCallbackHandler`**: rewrote the switch as a valid pattern-matching switch with the original behaviour: set the login, set the password, otherwise throw `UnsupportedCallbackException`.  
- 📦 **`pom.xml` dependency reconciliation**:  
  - Replaced javax `jboss-javaee-7.0` and the compile-scope jakarta APIs with a single provided `jakarta.jakartaee-api:10.0.0`. This fixed the missing Faces API errors (`jakarta.faces.*` "package does not exist").  
  - Removed the javax-based `jaxb-runtime` 2.3.9.  
  - Test stack moved to jakarta-compatible versions:  
    - Arquillian 1.5.0.0 universe → `arquillian-bom` 1.9.5.Final with `arquillian-junit-container`  
    - wildfly-arquillian 3.0.1 → 5.1.0.Final  
    - RESTEasy 3.15.3 → 6.2.19.Final (`resteasy-client`, `resteasy-jackson2-provider`)  
    - Removed the unused javax-only `arquillian-rest-client` extension  
- 📘 **Swagger / OpenAPI**: the `kongchen` plugin 3.1.8 failed on missing JAXB, and once that was fixed it produced an empty contract because it does not read `jakarta.ws.rs`. Kiro made these changes:  
  - Replaced it with `io.swagger.core.v3:swagger-maven-plugin-jakarta` 2.2.55.  
  - `swagger-annotations` 1.6.9 → `swagger-annotations-jakarta` 2.2.55.  
  - In 5 endpoints, `@Api` → `@Tag` and 25 × `@ApiOperation` → `@Operation`.  
  - Added `@OpenAPIDefinition` (info, contact, servers) on `RestApplication`.  
- 🧪 **Unit tests**:  
  - EqualsVerifier 3.12.3 → 3.19.4, because the old Byte Buddy rejected Java 25 class files and all 9 tests failed.  
  - `OrderLineTest` and `PurchaseOrderTest` now suppress `Warning.JPA_GETTER` (a new check in that version); the entity code is unchanged.  
- 🏷️ **`forge.taglib.xml`**: removed the leftover Facelets 1.0 DTD and declared the Faces 4.0 taglib schema.  
  
## 5. ⏱️ Time Savings Estimate  
  
- 🔄 **OpenRewrite**: the tool's own estimate is **30 h 43 m** saved (`Estimate time saved: 30h 43m`) for 144 files, in a run of 1 min 36 s.  
- 🤖 **Kiro CLI build fix**: 18 min 57 s, against an estimated **8–12 h** for a developer. The basis for that estimate:  
  - the javax/jakarta dependency conflicts  
  - the Swagger toolchain replacement and annotation migration  
  - the Java 25 test-tool incompatibility  
  - the broken recipe output  
  - verifying the WAR contents  
- 💰 **Total**: about **39–43 engineer-hours** (roughly 5 days) of manual effort compressed into about **21 minutes** of automated work. This is an estimate; only the OpenRewrite figure comes from the log.  
  
## 6. 🚀 Next Steps  
  
### ✅ Validation  
- 🏛️ **Finish the Jakarta EE 11 alignment** (requested target, not complete):  
  - `jakarta.jakartaee-api` 10.0.0 → 11.0.0  
  - descriptor versions: persistence 3.2, faces 4.1, web-app 6.1, beans 4.1  
  - Then rebuild and confirm that PrimeFaces 14 supports Faces 4.1.  
- 🧪 **Run the integration tests**: `mvn verify -Parquillian-wildfly-managed` (or `-remote`) against WildFly 39. Fix the hard-coded `wildfly-11.0.0.Final` unpack directory in the managed profile, which existed before the upgrade.  
- 🖥️ **UI smoke test** on WildFly:  
  - shopping flow, admin CRUD, login (JAAS `SimpleLoginModule`)  
  - PrimeFaces 14 with `all-themes` 1.1.0  
- 🌐 **Check REST and contract consumers**: `curl` the `/rest/*` endpoints (XML and JSON) and confirm that consumers accept OpenAPI 3 at `/swagger.json`.  
- ☕ **Confirm the runtime JDK**: check that the chosen WildFly release is supported on Java 25.  
  
### 💡 Improvements  
- 🏷️ Migrate the xhtml namespaces from `http://xmlns.jcp.org/jsf/*` to `jakarta.faces.*`. They are deprecated in Faces 4.0 and removed in 4.1, so this is required for EE 11.  
- 🧪 Migrate the tests from JUnit 4 to JUnit 5 (Arquillian JUnit 5 container).  
- 🔐 Bump log4j 2.19.0 to a current 2.x release.  
- 📚 Update the README, which still says Java EE 7 and WildFly 10–26.  
- 🧹 Consider making entity `equals` use getters instead of suppressing `JPA_GETTER` in the tests.  

## Kiro credit usage

💳 Kiro CLI credits used: 41.90  

## Codebase index refresh

- Outcome: Index current
- Reason: Index current at 725839cad3c52515b9702dec2edb3984df58d04f.
- Previously indexed commit: 725839cad3c52515b9702dec2edb3984df58d04f
- Source commit at transformation start: 725839cad3c52515b9702dec2edb3984df58d04f
- Duration: 0.0s
