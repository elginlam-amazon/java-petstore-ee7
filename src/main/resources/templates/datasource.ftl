<#-- T7 seed (test plan 1.3): JDK javax.* in a template; must be left alone -->
import javax.naming.InitialContext;
import javax.sql.DataSource;

DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/${dsName}");
