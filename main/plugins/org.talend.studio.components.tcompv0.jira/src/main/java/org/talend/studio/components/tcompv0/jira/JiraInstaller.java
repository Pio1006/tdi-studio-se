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
package org.talend.studio.components.tcompv0.jira;

import org.talend.core.model.utils.BaseComponentInstallerTask;

/**
 * @author bhe created on Jul 20, 2021
 *
 */
public class JiraInstaller extends BaseComponentInstallerTask {

    @Override
    protected Class<? extends BaseComponentInstallerTask> getInstallerClass() {
        return JiraInstaller.class;
    }

}
