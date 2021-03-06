/**
 * Copyright (C) 2015-2016, Zhichun Wu
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.cassandra.jdbc.provider.datastax;

import com.datastax.driver.core.*;
import com.datastax.driver.core.QueryTrace.Event;
import com.github.cassandra.jdbc.*;
import com.google.common.base.Strings;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import static com.github.cassandra.jdbc.CassandraUtils.EMPTY_STRING;

/**
 * This is a statement implementation built on top of DataStax Java driver.
 *
 * @author Zhichun Wu
 */
public class CassandraStatement extends BaseCassandraStatement {



    protected CassandraStatement(CassandraConnection conn,
                                 DataStaxSessionWrapper session) {
        this(conn, session, EMPTY_STRING);
    }

    protected CassandraStatement(CassandraConnection conn,
                                 DataStaxSessionWrapper session,
                                 String cql) {
        super(conn);
        this.cqlStmt = CassandraCqlParser.parse(conn.getConfiguration(), cql);
        this.session = session;
    }

    @Override
    protected CassandraDataTypeMappings getDataTypeMappings() {
        return DataStaxDataTypes.mappings;
    }

    @Override
    protected CassandraDataTypeConverters getDataTypeConverters() {
        return DataStaxDataTypes.converters;
    }





    protected ResultSet executeCql(String cql) throws SQLException {
        Logger.debug("Trying to execute the following CQL:\n{}", cql);

        CassandraCqlStatement parsedStmt = CassandraCqlParser.parse(getConfiguration(), cql);
        CassandraCqlStmtConfiguration stmtConf = parsedStmt.getConfiguration();

        Logger.debug("Statement Configuration:\n{}", stmtConf);

        SimpleStatement ss = new SimpleStatement(parsedStmt.getCql());

        configureStatement(ss, stmtConf);

        ResultSet rs = null;
        if (stmtConf.noWait()) {
            session.executeAsync(ss);
        } else {
            rs = session.execute(ss);
        }

        postStatementExecution(parsedStmt, rs);

        return rs;
    }

    public int[] executeBatch() throws SQLException {
        CassandraEnums.Batch mode = getConfiguration().getBatch();
        BatchStatement batchStmt = new BatchStatement(
                mode == CassandraEnums.Batch.LOGGED ? BatchStatement.Type.LOGGED : BatchStatement.Type.UNLOGGED);

        for (CassandraCqlStatement stmt : batch) {
            batchStmt.add(new SimpleStatement(stmt.getCql()));
        }

        session.execute(batchStmt);

        int[] results = new int[batch.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = SUCCESS_NO_INFO;
        }

        return results;
    }

    public boolean execute(String sql) throws SQLException {
        validateState();

        executeCql(sql);

        return cqlStmt.getConfiguration().getStatementType().isQuery();
    }

    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public boolean execute(String sql, String[] columnNames)
            throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public java.sql.ResultSet executeQuery(String sql) throws SQLException {
        if (!execute(sql)) {
            throw CassandraErrors.invalidQueryException(sql);
        }

        return currentResultSet;
    }

    public int executeUpdate(String sql) throws SQLException {
        validateState();

        executeCql(sql);

        return cqlStmt.getConfiguration().getStatementType().isUpdate() ? 1 : 0;
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException {
        throw CassandraErrors.notSupportedException();
    }

    public java.sql.ResultSet getResultSet() throws SQLException {
        return cqlStmt.getConfiguration().getStatementType().isQuery() ? currentResultSet : null;
    }

    public int getUpdateCount() throws SQLException {
        CassandraStatementType stmtType = cqlStmt.getConfiguration().getStatementType();

        return stmtType.isQuery() ? -1 : (stmtType.isUpdate() ? 1 : 0);
    }

    public java.sql.ResultSet executeQuery() throws SQLException {
        // method inherited from BaseCassandraPreparedStatement
        throw CassandraErrors.notSupportedException();
    }

    public int executeUpdate() throws SQLException {
        // method inherited from BaseCassandraPreparedStatement
        throw CassandraErrors.notSupportedException();
    }

    public boolean execute() throws SQLException {
        // method inherited from BaseCassandraPreparedStatement
        throw CassandraErrors.notSupportedException();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw CassandraErrors.notSupportedException();
    }
}
