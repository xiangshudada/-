package com.easylink.ruleproduct.infra.mysql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqlRows {

    private SqlRows() {
    }

    static Map<String, Object> toCamelCaseMap(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = metaData.getColumnLabel(i);
            row.put(toCamelCase(label), rs.getObject(i));
        }
        return row;
    }

    private static String toCamelCase(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            upperNext = false;
        }
        return builder.toString();
    }
}
