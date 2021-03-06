package com.rookiefly.open.dictionary.database.introspector;

import com.rookiefly.open.dictionary.database.DBMetadataHolder;
import com.rookiefly.open.dictionary.database.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PGIntrospector extends DatabaseIntrospector {

    public PGIntrospector(DBMetadataHolder dbMetadataHolder) {
        super(dbMetadataHolder);
    }

    public PGIntrospector(DBMetadataHolder dbMetadataHolder, boolean forceBigDecimals, boolean useCamelCase) {
        super(dbMetadataHolder, forceBigDecimals, useCamelCase);
    }

    /**
     * 获取表名和注释映射
     *
     * @param config
     * @return
     */
    @Override
    protected Map<String, String> getTableComments(DatabaseConfig config) {
        PreparedStatement preparedStatement = null;
        Map<String, String> answer = new HashMap<>();
        try {
            preparedStatement = dbMetadataHolder.getConnection().prepareStatement("" +
                    "select tname,comments from(select relname as TNAME ,col_description(c.oid, 0) as COMMENTS from pg_class c where  relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%') as temp where comments is not null ");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                answer.put(rs.getString(dbMetadataHolder.convertLetterByCase("tname")), rs.getString(dbMetadataHolder.convertLetterByCase("comments")));
            }
            closeResultSet(rs);
        } catch (SQLException e) {
            log.error("getTableComments exception", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                log.error("preparedStatement close exception", e);
            }
        }
        return answer;
    }

    /**
     * 获取表字段注释
     *
     * @param config
     * @return
     */
    @Override
    protected Map<String, Map<String, String>> getColumnComments(DatabaseConfig config) {
        PreparedStatement preparedStatement = null;
        Map<String, Map<String, String>> answer = new HashMap<>();
        try {
            preparedStatement = dbMetadataHolder.getConnection().prepareStatement("" +
                    "select tname,cname,comments from(SELECT col_description(a.attrelid,a.attnum) as comments,a.attname as cname,c.relname as tname FROM pg_class as c,pg_attribute as a where a.attrelid = c.oid and a.attnum>0 and c.relname not like 'pg_%' and c.relname not like 'sql_%') as temp where comments is not null ");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                String tname = rs.getString(dbMetadataHolder.convertLetterByCase("tname"));
                if (!answer.containsKey(tname)) {
                    answer.put(tname, new HashMap<>());
                }
                answer.get(tname).put(rs.getString(dbMetadataHolder.convertLetterByCase("cname")), rs.getString(dbMetadataHolder.convertLetterByCase("comments")));
            }
            closeResultSet(rs);
        } catch (SQLException e) {
            log.error("getColumnComments exception", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                log.error("preparedStatement close exception", e);
            }
        }
        return answer;
    }
}
