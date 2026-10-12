<#-- T7 seed (test plan 1.3): EE javax.* in a template; must be migrated to jakarta.* -->
package ${package};

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ${className} {
    @Id
    private Long id;
}
