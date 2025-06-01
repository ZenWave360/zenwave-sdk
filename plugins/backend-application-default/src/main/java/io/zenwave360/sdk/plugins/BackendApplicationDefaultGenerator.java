package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.ZDLProjectGenerator;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Generates a backend application following configured project layout.
 */
public class BackendApplicationDefaultGenerator extends ZDLProjectGenerator {

    @DocumentedOption(description = "Persistence")
    public PersistenceType persistence = PersistenceType.mongodb;

    @DocumentedOption(description = "SQL database flavor")
    public DatabaseType databaseType = DatabaseType.postgresql;

    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Use @Getter and @Setter annotations from Lombok")
    public boolean useLombok = false;

    @DocumentedOption(description = "Whether to add AsyncAPI/ApplicationEventPublisher as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work.")
    public boolean includeEmitEventsImplementation = true;

    @DocumentedOption(description = "Controls whether to add a read/write relationship by id when mapping relationships between aggregate (not recommended) keeping the relationship by object readonly.")
    public boolean addRelationshipsById = false;

    @DocumentedOption(description = "Specifies the Java data type for the ID fields of entities. Defaults to Long for JPA and String for MongoDB if not explicitly set.")
    public String idJavaType;

    public String mavenModulesPrefix;

    @Override
    public void onPropertiesSet() {
        if (templates == null) {
            if (StringUtils.isNotBlank(mavenModulesPrefix)) {
                templates =new BackendApplicationMultiModuleProjectTemplates();
            } else {
                templates = new BackendApplicationProjectTemplates();
            }
        }
        super.onPropertiesSet();
    }

    @Override
    public Map<String, Object> asConfigurationMap() {
        var config = super.asConfigurationMap();
        config.put("idJavaType", getIdJavaType());
        return config;
    }

    public String getIdJavaType() {
        return ObjectUtils.firstNonNull(idJavaType, this.persistence == PersistenceType.jpa ? "Long" : "String");
    }

}
