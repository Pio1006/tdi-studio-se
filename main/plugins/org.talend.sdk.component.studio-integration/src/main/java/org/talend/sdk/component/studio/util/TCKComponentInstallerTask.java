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
package org.talend.sdk.component.studio.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.utils.BaseComponentInstallerTask;
import org.talend.core.model.utils.ComponentGAV;
import org.talend.core.model.utils.IComponentInstallerTask;
import org.talend.updates.runtime.service.ITaCoKitUpdateService;
import org.talend.updates.runtime.service.ITaCoKitUpdateService.ICarInstallationResult;

/**
 * @author bhe created on Jul 1, 2021
 *
 */
abstract public class TCKComponentInstallerTask extends BaseComponentInstallerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCKComponentInstallerTask.class);

    private static final String SYS_PROP_TCK = "tck.update";
    
    protected boolean updateTck() {
        String prop = System.getProperty(SYS_PROP_TCK, "false");
        return Boolean.valueOf(prop);
    }
    
    /**
     * Get car file
     * 
     * @return car file
     */
    protected List<File> getCarFiles() {
        URL carFolder = FileLocator.find(FrameworkUtil.getBundle(getInstallerClass()), new Path("car"), null);
        File carDir = null;
        if (carFolder != null) {
            try {
                carDir = new File(FileLocator.toFileURL(carFolder).getPath());
                if (carDir.isDirectory()) {
                    File[] cars = carDir.listFiles();
                    LOGGER.info("Files found: {}", Arrays.toString(cars));

                    List<File> carFiles = Stream.of(cars).filter(f -> f.getName().endsWith(".car")).collect(Collectors.toList());
                    return carFiles;
                }

            } catch (IOException e) {
                LOGGER.error("Can't find car file", e);
            }
        }
        LOGGER.error("Can't find car file from folder {}", carDir);
        return null;
    }

    @Override
    public boolean needInstall() {
        
        if (this.updateTck()) {
            LOGGER.info("System property: {} is true", SYS_PROP_TCK);
            return true;
        }
        
        boolean toInstall = false;
        Set<ComponentGAV> tcompv1Gavs = this.getComponentGAV(IComponentInstallerTask.COMPONENT_TYPE_TCOMPV1);

        ILibraryManagerService librairesManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(ILibraryManagerService.class);
        if (librairesManagerService != null) {
            for (ComponentGAV gav : tcompv1Gavs) {
                File jarFile = librairesManagerService.resolveStatusLocally(gav.toMavenUri());
                if (jarFile == null) {
                    LOGGER.info("Component: {} was not installed", gav.toString());
                    toInstall = true;
                    break;
                }
            }
        }

        if (toInstall) {
            LOGGER.info("Component: {} is going to be installed", Arrays.toString(tcompv1Gavs.toArray()));
        }
        return toInstall;
    }

    @Override
    public boolean install(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        if (!super.needInstall() && !this.needInstall()) {
            return false;
        }
        boolean installed = false;
        if (this.needInstall()) {

            Set<File> files = new HashSet<File>();
            List<File> carFiles = getCarFiles();
            if (carFiles != null) {
                LOGGER.info("Car file: {} was added", Arrays.toString(carFiles.toArray()));
                files.addAll(carFiles);
            }
            if (!files.isEmpty()) {
                try {
                    ICarInstallationResult result = ITaCoKitUpdateService.getInstance().installCars(files, true, monitor, true, false);
                    for (File carFile : carFiles) {
                        IStatus stat = result.getInstalledStatus().get(carFile);
                        if (stat == null) {
                            LOGGER.info("Skipped to install car: {}", carFile);
                        } else if (stat.getCode() == IStatus.OK) {
                            installed = true;
                            LOGGER.info("TCK Component installed: {}", carFile);
                        } else {
                            LOGGER.error("Failed to install car: {}", carFile);
                        }
                    }

                    if (installed) {
                        LOGGER.info("TCK Component installed: {}", Arrays.toString(carFiles.toArray()));
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to install car: {}", Arrays.toString(carFiles.toArray()), e);
                }
            } else {
                LOGGER.info("No car files");
            }
        }

        if (super.needInstall()) {
            installed = super.install(monitor);
        }
        return installed;
    }

}
