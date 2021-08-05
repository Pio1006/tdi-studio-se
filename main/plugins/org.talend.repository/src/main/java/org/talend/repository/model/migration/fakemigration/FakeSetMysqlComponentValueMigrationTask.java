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

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.ModifyComponentsAction;
import org.talend.core.model.components.conversions.IComponentConversion;
import org.talend.core.model.components.filters.IComponentFilter;
import org.talend.core.model.components.filters.NameComponentFilter;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.core.utils.TalendQuoteUtils;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.migration.IProjectMigrationTask;
import org.talend.migration.MigrationReportRecorder;

/**
 * DOC jding  class global comment. Detailled comment
 */
public class FakeSetMysqlComponentValueMigrationTask extends AbstractJobMigrationTask {

    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2021, 07, 29, 15, 0, 0);
        return gc.getTime();
    }

    @Override
    public ExecutionResult execute(Item item) {
        ProcessType processType = getProcessType(item);
        if (getProject().getLanguage() != ECodeLanguage.JAVA || processType == null) {
            return ExecutionResult.NOTHING_TO_DO;
        }

        boolean modified = false;
        IProjectMigrationTask task = this;
        IComponentFilter filter = new NameComponentFilter("tMysqlInput"); //$NON-NLS-1$
        try {
            modified = ModifyComponentsAction.searchAndModify(item, processType, filter,
                    Arrays.<IComponentConversion> asList(new IComponentConversion() {

                        public void transform(NodeType node) {
                            ElementParameterType parameter = ComponentUtilities.getNodeProperty(node,
                                    "HOST");
                            String value = TalendQuoteUtils.removeQuotes(parameter.getValue());
                            if (StringUtils.isBlank(value)) {
                                parameter.setValue("localhost");
                                generateReportRecord(
                                        new MigrationReportRecorder(task, MigrationReportRecorder.MigrationOperationType.ADD,
                                                item, node, "HOST", null, "localhost"));
                                ElementParameterType passparameter = ComponentUtilities.getNodeProperty(node, "PASS");
                                passparameter.setValue("");
                                generateReportRecord(new MigrationReportRecorder(task,
                                        MigrationReportRecorder.MigrationOperationType.DELETE, item, node, "PASS", null, null));
                            }
                        }
                    }));
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }

        return modified ? ExecutionResult.SUCCESS_WITH_ALERT : ExecutionResult.NOTHING_TO_DO;
    }

}
