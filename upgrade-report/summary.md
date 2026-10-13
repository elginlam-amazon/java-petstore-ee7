# 📊 Executive Report: Petstore Java EE 7 → Java 25 / Jakarta EE 11  
  
## 1. 🧭 Executive summary  
  
- 🎯 **Scope:** `org.agoncal.application:petstoreee7`, a single-module WAR. It moved from Java 8 / Java EE 7 to **Java 25 + Jakarta EE 11**, with Jakarta Faces 4.1, PrimeFaces 14 (`jakarta` classifier) and Apache POI 5.5.1.  
- ✅ **Outcome:**  
  - The final `mvn clean package` on Corretto 25 shows **BUILD SUCCESS**: zero compilation errors and **9/9 unit tests passing**.  
  - Every OpenRewrite run ended in BUILD SUCCESS, and the log reports "template-marker check after the Java leg: clean".  
- 🔄 **Approach:**  
  - Four OpenRewrite runs changed **150 files** (per the SourcesFileResults tables).  
  - Kiro CLI then fixed the build: it completed the dependency migration the recipes left half-done and bumped the config files.  
- 🌱 **Spring:** not part of this run. The log shows `springBootTargetVersion=none`, and the project has no Spring dependency. No Spring version was changed, and there is no "Verified: effective Spring Framework version" line, because none applies.  
- ⚠️ **Not yet proven:** the app has not been deployed. Deployment on WildFly 40/41, the Arquillian ITs and the PrimeFaces UI review are still open (see Next steps).  
  
## 2. 🏗️ Application changes  
  
- ☕ **Java:** `maven.compiler.source/target` went from 1.8 to **25**.  
- 🔁 **Namespace:** `javax.*` to `jakarta.*` (Persistence, Validation, CDI, EJB, JAX-RS, Faces, Interceptor, Inject, XML Binding) across main and test code.  
- 🛡️ **Kept on javax on purpose:**  
  - JDK packages: `javax.naming`, `javax.security.auth.*`  
  - jsr305 `javax.annotation.Nonnull/Nullable`  
  - `datasource.ftl`  
- 🖥️ **Faces 4.1:** the recipes changed 35 XHTML views to the `jakarta.faces.*`, `jakarta.tags.*` and `primefaces` URNs. They also set `faces-config.xml` to 4.1 and `FacesServlet` to `jakarta.faces.webapp.FacesServlet`.  
- 🗄️ **Persistence:**  
  - `persistence.xml` is now version **3.2** (`persistence_3_2.xsd`).  
  - The provider changed from `org.hibernate.ejb.HibernatePersistence` to `org.hibernate.jpa.HibernatePersistenceProvider`.  
  - Property keys moved to `jakarta.persistence.*`.  
- 🐘 **Hibernate 7.1 step:** it was selected (`hibernateTarget=7.1`), but its data table has **no rows**. The app has no Hibernate dependency or API calls, so the step was a no-op. Only the persistence.xml provider step applied.  
- 📗 **Apache POI 3.14 → 5.5.1:** 16 short `setAlignment`/`setFillPattern` call sites were migrated to value-preserving enum calls.  
- 📦 **Packaging:** the WAR now ships only application libraries. Jakarta APIs are `provided` by the server.  
  
## 3. 🧰 Tools used  
  
- ☕ **Java SDK:** Amazon Corretto **25.0.4.1** (OpenJDK 25 LTS) and Apache Maven 3.9.16.  
- 🔧 **OpenRewrite:** `rewrite-maven-plugin` 6.42.0, with:  
  - `rewrite-migrate-java` 3.38.0  
  - `rewrite-hibernate` 2.22.0  
  - `rewrite-apache` 2.27.1  
- 📜 **Recipes that actually made changes (per the log and data tables):**  
  - `PreparePoiStyleEnums`: 2 files (pom.xml and `ReportStyles.java`; POI temporarily moved to 3.16)  
  - `MigratePoiStyleShortArguments`: 1 file (`forInt` wrapping)  
  - The main Java leg, 146 files (104 Java, 35 XHTML, 7 XML), made up of:  
    - `UpgradeToJakartaEE11`  
    - `UpgradeToJakartaFaces41`  
    - `UpgradePersistenceXmlToJakartaEE11`  
    - `UpgradeApachePoi5`  
    - `UpgradeToJava25`  
  - `MigrateJakartaEeTemplates`: 1 file (`entity.ftl`)  
- 🤖 **Amazon Kiro CLI:** model **claude-opus-5.5**. It fixed the build, consulted migration knowledge docs 30–35 through the knowledge base, and wrote the reports. The session took about 6 minutes.  
  
## 4. 💻 Code changes  
  
- 🤖 **Automated (OpenRewrite):**  
  - 📝 Imports changed from `javax.*` to `jakarta.*` in entities, services, REST endpoints, CDI producers, interceptors, JSF beans and ITs.  
  - 📗 `ReportStyles.java`:  
    - Named constants were swapped for the enum constant with the same code, e.g. `CellStyle.ALIGN_CENTER` → `HorizontalAlignment.CENTER` and `SOLID_FOREGROUND` → `FillPatternType.SOLID_FOREGROUND`.  
    - Short values were wrapped in `forInt(...)`.  
    - `setBoldweight` → `setBold(true)`, and `setCellType(0)` → `CellType.NUMERIC`.  
  - 🆕 `WriteWorkbooks.main` became a Java 25 instance `void main(String[])`.  
  - 🧩 POM:  
    - PrimeFaces 6.1 → 14.0.18 with the `jakarta` classifier  
    - POI → 5.5.1  
    - surefire/failsafe → 3.5.6, war plugin → 3.5.1  
    - Byte Buddy pinned to 1.17.8  
- 🛠️ **Kiro CLI fixes (needed for a green build):**  
  - 🔴 **Root cause of the failure:** `package jakarta.faces.* does not exist`. The javax `jboss-javaee-7.0` API was still in the POM. Kiro replaced it with `jakarta.platform:jakarta.jakartaee-api` **11.0.0** (`provided`).  
  - 🧹 Removed duplicate compile-scope Jakarta API jars that the recipes had added, so they don't end up in the WAR. `jakarta.xml.bind-api` 4.0.5 is kept as `provided`.  
  - 🗑️ Removed `jaxb-runtime` 2.3.9, which is still on javax.  
  - ⚖️ Aligned versions: log4j 2.19.0 → **2.24.3** (to match the log4j-api that POI 5 pulls in) and commons-lang3 pinned to **3.18.0** (needed by commons-compress 1.28).  
  - 📄 Swagger: `swagger-maven-plugin` 3.1.8 only reads javax JAX-RS annotations, so it overwrote `swagger.json` with an empty contract. Generation is now skipped (`swagger.skip=true`) and the committed contract is kept.  
  - 📑 Config files:  
    - `web.xml` 5.0 → **6.1**  
    - `beans.xml` 4.0 → **4.1**, still `bean-discovery-mode="all"`  
    - `forge.taglib.xml`: the legacy Facelets 1.0 DOCTYPE was replaced with the `web-facelettaglibrary_4_1.xsd` header  
  - 🔍 POI mappings were checked against the doc 35 tables; all are correct. Only indentation was changed.  
  
## 5. ⏱️ Time savings estimate  
  
- 🤖 **OpenRewrite's own estimates (from the log):**  
  
  | Run | Estimate |  
  | --- | --- |  
  | Main Java leg | 32h 18m |  
  | POI constants | 35m |  
  | POI short arguments | 10m |  
  | Templates | 5m |  
  | **Total** | **≈ 33h 08m** |  
  
- 🛠️ **Kiro build fixes:** I estimate **6–10 h** of manual work saved. That covers diagnosing the API classpath, reconciling dependencies, the Swagger regression, the config bumps and checking the POI mappings.  
- 📈 **Combined:** roughly **39–43 engineer-hours** saved, against about **20 minutes** of wall-clock automation (the OpenRewrite runs plus the Kiro session).  
- ℹ️ OpenRewrite's figures are its built-in heuristics. The Kiro figure is my own estimate.  
  
## 6. 🚀 Next steps  
  
- 🧪 **Deployment test (CDI 4):** deploy to WildFly and open every page. Confirm that injection, the `util/*Producer` producers and the `LoggingInterceptor` / `ExceptionInterceptor` interceptors listed in `beans.xml` still resolve.  
- 🖧 **Target server:**  
  - The app needs **WildFly 40/41** for EE 11. The README and `version.wildfly` (26.1.3.Final) still name WildFly 26, which cannot run a jakarta app.  
  - WildFly 40/41 are EE 11 certified only on SE 17/21. A human needs to choose between SE 25 (recommended, not certified) and SE 21 (certified).  
- 🔬 **Arquillian ITs:** these are still skipped. To run them, upgrade `wildfly-arquillian-container-*` (3.0.1.Final), RESTEasy test libraries 3.15.3 → 6.2+/7 (use `resteasy-jackson2-provider`), and `arquillian-rest-client-impl-3x`. Then run `-Parquillian-wildfly-managed`.  
- 📄 **Swagger:** migrate to swagger-core v3 (`swagger-annotations-jakarta` + `swagger-maven-plugin-jakarta`). That means rewriting `@Api`/`@ApiOperation` on 5 endpoints. Re-enable generation afterwards.  
- 📗 **POI output:** compare the workbooks written by `WriteWorkbooks` before and after the upgrade with the doc 36 golden-master capture, checking alignment, fill and bold per cell.  
- 🐘 **Hibernate 7:** no app code uses Hibernate APIs. Still, check the server-provided Hibernate 7 behavior (`drop-and-create` schema generation and the `init_db.sql` load script) in an integration run.  
- 🧹 **Housekeeping:**  
  - Update `maven-compiler-plugin` 3.1 to 3.13+ with `<maven.compiler.release>25</maven.compiler.release>`.  
  - Update or remove the `src/main/forge/generate.fsh` Forge script, which still names `javax.*`.  
  - Update the README (WildFly version, Java EE 7 references).  
- 🌱 **Spring:** nothing to do. No Spring stack exists and none was targeted.  
  
## PrimeFaces human UI gate  
  
🎨 Each of these sites was **left unchanged for human UI review**, because changing them alters what users see. PrimeFaces 15 replacements are from migration knowledge doc 31.  
  
- `pom.xml:114`: `org.primefaces.themes:all-themes` (dependencyManagement), the legacy ThemeRoller themes jar. **PF 15 replacement:** built-in `saga-blue` / `arya-blue` / `vela-blue`, `primefaces-themes.jar` (Luna, Nova, Material, Bootstrap) or a Theme Designer theme, then drop `all-themes`. Left unchanged for human UI review.  
- `pom.xml:209`: `org.primefaces.themes:all-themes` (dependency), the legacy ThemeRoller themes jar. **PF 15 replacement:** same as above; remove it once a PF 15 theme is chosen. Left unchanged for human UI review.  
- `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme, which no longer renders correctly in PF 15). **PF 15 replacement:** a PF 15 theme such as `saga-blue`. Left unchanged for human UI review.  
- `src/main/webapp/resources/templates/template.xhtml:86`: FontAwesome `fa fa-user`. **PF 15 replacement:** PrimeIcons `pi pi-user`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- `src/main/webapp/resources/templates/template.xhtml:92`: FontAwesome `fa fa-sign-out`. **PF 15 replacement:** PrimeIcons `pi pi-sign-out`, or an app-supplied FontAwesome. Left unchanged for human UI review.  
- `src/main/webapp/resources/templates/template.xhtml:100`: FontAwesome `fa fa-sign-in`. **PF 15 replacement:** PrimeIcons `pi pi-sign-in`, or an app-supplied FontAwesome. Left unchanged for human UI review.  
- `src/main/webapp/resources/templates/template.xhtml:107`: FontAwesome `fa fa-shopping-cart`. **PF 15 replacement:** PrimeIcons `pi pi-shopping-cart`, or an app-supplied FontAwesome. Left unchanged for human UI review.  
- `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button" pattern="d/M/yyyy">` (deprecated since PF 10). **PF 15 replacement:** `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`, `pattern`). Left unchanged for human UI review.  
- `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button">` (deprecated since PF 10). **PF 15 replacement:** `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`). Left unchanged for human UI review.

## Runtime verification checklist (silent breaks)

The build passing does not prove any of these. Each one fails only at deployment or run time, so verify it on the target server and database before accepting the upgrade (migration knowledge doc 36).

- Applies because: Hibernate 7: the Hibernate 7 step ran (hibernateTarget=7.1); the module uses JPA (src/main/resources/META-INF/persistence.xml)
- Applies because: CDI 4: jakartaEeTarget=11 (CDI 4.1); the module uses CDI (src/main/webapp/WEB-INF/beans.xml)

- [ ] **Hibernate 7: Native queries return java.time types** (doc 10b, §5.1). Run every native query that returns date or time columns against the target database: Hibernate 7 returns java.time.LocalDate / LocalTime / LocalDateTime instead of java.sql.Date / Time / Timestamp, so a cast or a formatter can fail with ClassCastException. Fix the reads, or set hibernate.query.native.prefer_jdbc_datetime_types=true for the persistence unit.
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **Hibernate 7: StatelessSession now uses the second-level cache** (doc 10b, §5.2). Check every StatelessSession: in Hibernate 7 it reads from and writes to the L2 cache by default, so a batch job can read stale data with no error. Restore the old behavior with setCacheMode(CacheMode.IGNORE), or CacheMode.GET for bulk writers.
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **Hibernate 7: refresh() / lock() on a detached entity throws** (doc 10b, §5.3). Exercise every refresh() and lock() call whose entity may come from another transaction, an HTTP session, a JSF view-scoped bean or a cache: Hibernate 7 throws IllegalArgumentException on a detached entity. Work on a managed instance (find with a lock mode, or merge then refresh).
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **CDI 4: beans.xml discovery mode default flipped to annotated** (doc 32, "What changed"). Deploy the app and open its pages: under CDI 4 an empty beans.xml, or one without bean-discovery-mode, means annotated, so classes without a bean-defining annotation stop being beans and fail only at deployment (WELD-001408) or page render. Keep bean-discovery-mode="all" or add scopes.
  - `src/main/webapp/WEB-INF/beans.xml: bean-discovery-mode="all", version 4.1: old behavior kept`
- [ ] **CDI 4: @Named classes without a bean-defining annotation** (doc 32, "Detection patterns"). Render every view that uses these @Named classes and inject them once: @Named alone is not a bean-defining annotation, so they are beans only while discovery is "all". With annotated discovery EL resolves them to null (Target Unreachable). Add a scope, or keep discovery "all".
  - `src/main/java/org/agoncal/application/petstore/security/SimpleCallbackHandler.java:21`

## Golden-master output checks (optional)

This module writes PDF, Excel or JasperReports output, and a green build does not show that the output is unchanged. Optionally, capture each output file before and after the upgrade and compare the captures structurally: PDFBox text per page and Apache POI cell values, never the bytes (PDF and XLSX embed timestamps). Migration knowledge doc 36 has the steps.

High-risk output paths found by the static scan:

- `src/main/java/org/agoncal/application/petstore/export/poi/ReportStyles.java` (Excel: Apache POI)
- `src/main/java/org/agoncal/application/petstore/export/poi/WriteWorkbooks.java` (Excel: Apache POI)

No golden-master capture was made in this iteration (the scaffolding is optional).

## Kiro credit usage

💳 Kiro CLI credits used: 48.09  
