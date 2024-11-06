package io.zenwave360.sdk.options;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DatabaseType {
    generic,
    postgresql,
    mysql,
    mariadb,
    oracle;

    @JsonCreator
    public static DatabaseType fromValue(String value) {
        for (DatabaseType type : DatabaseType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return generic;
    }
}
