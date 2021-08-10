// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.model.migration.fakemigration;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.database.EDatabaseTypeName;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.migration.AbstractItemMigrationTask;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.migration.MigrationReportRecorder;

/**
 * DOC jding  class global comment. Detailled comment
 */
public class FakeSetMysqlConnectionValueMigrationTask extends AbstractItemMigrationTask {

    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2021, 07, 29, 16, 0, 0);
        return gc.getTime();
    }

    @Override
    public List<ERepositoryObjectType> getTypes() {
        List<ERepositoryObjectType> toReturn = new ArrayList<ERepositoryObjectType>();
        toReturn.add(ERepositoryObjectType.METADATA_CONNECTIONS);
        return toReturn;
    }

    @Override
    public ExecutionResult execute(Item item) {
        if (item instanceof DatabaseConnectionItem) {
            DatabaseConnectionItem connectionItem = (DatabaseConnectionItem) item;
            DatabaseConnection connection = (DatabaseConnection) connectionItem.getConnection();
            if (connection instanceof DatabaseConnection) {
                DatabaseConnection dbConnection = connection;
                String dbType = dbConnection.getDatabaseType();
                if (!EDatabaseTypeName.MYSQL.getDbType().equals(dbType)) {
                    return ExecutionResult.NOTHING_TO_DO;
                }
                if (StringUtils.isBlank(dbConnection.getServerName())) {
                    dbConnection.setServerName("localhost");
                    generateReportRecord(new MigrationReportRecorder(this, MigrationReportRecorder.MigrationOperationType.ADD,
                            connectionItem, null, "server", null, "localhost"));
                    dbConnection.setPassword("");
                    try {
                        ProxyRepositoryFactory.getInstance().save(item, true);
                        generateReportRecord(
                                new MigrationReportRecorder(this, MigrationReportRecorder.MigrationOperationType.DELETE,
                                        connectionItem, null, "password", null, null));
                        return ExecutionResult.SUCCESS_WITH_ALERT;
                    } catch (PersistenceException e) {
                        ExceptionHandler.process(e);
                        return ExecutionResult.FAILURE;
                    }
                }
                return ExecutionResult.NOTHING_TO_DO;
            }
        }
        return ExecutionResult.NOTHING_TO_DO;
    }

}
