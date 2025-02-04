/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.service;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.runtime.service.ITaCoKitService;
import org.talend.core.PluginChecker;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.utils.ComponentInstallerTaskRegistryReader;
import org.talend.core.model.utils.IComponentInstallerTask;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.ServerManager;
import org.talend.sdk.component.studio.metadata.TaCoKitCache;
import org.talend.sdk.component.studio.model.parameter.PropertyDefinitionDecorator;
import org.talend.sdk.component.studio.model.parameter.VersionParameter;
import org.talend.sdk.component.studio.toolbar.ReloadAction;
import org.talend.sdk.component.studio.util.TaCoKitUtil;
import org.talend.updates.runtime.service.ITaCoKitUpdateService;


public class TaCoKitService implements ITaCoKitService {

    @Override
    public void start() {
        if (CommonsPlugin.isHeadless() || CommonsPlugin.isJUnitTest() || PluginChecker.isSWTBotLoaded()) {
            // install all of TCK components here
//            installTCKComponents(new NullProgressMonitor());
            ServerManager.getInstance().start();
        } else {
            new Thread(new Runnable() {
              
                @Override
                public void run() {
                    
                    // install all of TCK components here
//                    installTCKComponents(new NullProgressMonitor());
                    
                    // start server
                    ServerManager.getInstance().start();
                }
                
            }, "Starting TaCoKit in background...").start();
        }
    }
    
    public void installTCKComponents(IProgressMonitor monitor) {
        List<IComponentInstallerTask> tasks = ComponentInstallerTaskRegistryReader.getInstance().getTasks(IComponentInstallerTask.COMPONENT_TYPE_TCOMPV1);
        tasks.forEach(t -> {
            try {
                t.install(monitor);
            } catch (InvocationTargetException | InterruptedException e) {
                ExceptionHandler.process(e);
            }
        });
    }

    public void waitForStart() {
        ServerManager.getInstance().waitForStart();
    }

    @Override
    public boolean isStarted() throws Exception {
        return ServerManager.getInstance().isStarted();
    }

    @Override
    public String reload(IProgressMonitor monitor) throws Exception {
        String result = new ReloadAction().reload(monitor);
        try {
            TaCoKitUtil.registAllTaCoKitRepositoryTypes();
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return result;
    }

    @Override
    public boolean isTaCoKitCar(File file, IProgressMonitor monitor) throws Exception {
        return ITaCoKitUpdateService.getInstance().isCar(file, monitor);
    }

    @Override
    public boolean isTaCoKitType(Object repoType) {
        if (repoType instanceof ERepositoryObjectType) {
            return TaCoKitUtil.isTaCoKitType((ERepositoryObjectType) repoType);
        } else {
            throw new IllegalArgumentException("Currently only support ERepositoryObjectType, please implement it if needed");
        }
    }

    @Override
    public boolean isNeedMigration(String componentName, Map<String, String> persistedProperties) {
        TaCoKitCache currentCach = Lookups.taCoKitCache();
        Optional<ComponentDetail> detail = Lookups.service().getDetail(componentName);         
        final Collection<PropertyDefinitionDecorator> properties = PropertyDefinitionDecorator
                .wrap(detail.get().getProperties());
        for (String key : persistedProperties.keySet()) {
            for (PropertyDefinitionDecorator p : properties) {
                if (p.getConfigurationType() != null && p.getConfigurationTypeName() != null
                        && (p.getPath() + VersionParameter.VERSION_SUFFIX).equals(key)) {
                    int currentVersion = TaCoKitUtil.getConfigTypeVersion(p, currentCach.getConfigTypeNodes(),
                            detail.get().getId().getFamilyId());
                    int persistedVersion = Integer.parseInt(persistedProperties.get(key));
                    if (currentVersion > persistedVersion) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }
}
