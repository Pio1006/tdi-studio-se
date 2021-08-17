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
package org.talend.repository.ui.login.connections.settings;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.service.IStudioLiteP2Service;
import org.talend.core.service.IStudioLiteP2Service.UpdateSiteConfig;
import org.talend.repository.i18n.Messages;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class UpdatesitePreferencePage extends PreferencePage {

    IStudioLiteP2Service p2Service = IStudioLiteP2Service.get();

    private Text releaseUriText;

    private Text updateUriText;

    @Override
    protected Control createContents(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        panel.setLayoutData(data);

        FillLayout layout = new FillLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        panel.setLayout(layout);

        Composite composite = new Composite(panel, SWT.NONE);
        GridLayout compLayout = new GridLayout(2, false);
        compLayout.marginHeight = 0;
        compLayout.marginWidth = 0;
        composite.setLayout(compLayout);

        Label releaseLabel = new Label(composite, SWT.NONE);
        releaseLabel.setText(Messages.getString("UpdatesitePreferencePage.releaseUrl"));
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        releaseLabel.setLayoutData(gd);

        releaseUriText = new Text(composite, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        releaseUriText.setLayoutData(gd);

        Label updateLabel = new Label(composite, SWT.NONE);
        updateLabel.setText(Messages.getString("UpdatesitePreferencePage.updateUrl"));
        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        updateLabel.setLayoutData(gd);

        updateUriText = new Text(composite, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        updateUriText.setLayoutData(gd);

        init();
        addListener();
        return parent;
    }

    private void init() {
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            UpdateSiteConfig config = p2Service.getUpdateSiteConfig(new NullProgressMonitor());
            URI release = config.getRelease(monitor);
            releaseUriText.setText(release == null ? "" : release.toString());
            releaseUriText.setEditable(config.isReleaseEditable());
            if (!config.isReleaseEditable()) {
                releaseUriText.setToolTipText(Messages.getString("UpdatesitePreferencePage.tooltip.cantEdit"));
            }
            Collection<URI> updates = config.getUpdates(monitor);
            StringBuilder updateStr = new StringBuilder();
            if (updates != null && !updates.isEmpty()) {
                for (String uri : updates.stream().map(uri -> uri.toString()).collect(Collectors.toList())) {
                    if (0 < updateStr.length()) {
                        updateStr.append(",");
                    }
                    updateStr.append(uri);
                }
            }
            updateUriText.setText(updateStr.toString());
            updateUriText.setEditable(config.isUpdateEditable());
            if (!config.isUpdateEditable()) {
                updateUriText.setToolTipText(Messages.getString("UpdatesitePreferencePage.tooltip.cantEdit"));
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    private void addListener() {
        releaseUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                refresh();
            }
        });
        updateUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                refresh();
            }
        });
    }

    @Override
    public boolean performOk() {
        if (this.isControlCreated()) {
            try {
                IProgressMonitor monitor = new NullProgressMonitor();
                UpdateSiteConfig config = p2Service.getUpdateSiteConfig(new NullProgressMonitor());
                if (config.isReleaseEditable()) {
                    String release = releaseUriText.getText();
                    config.setRelease(monitor, StringUtils.isBlank(release) ? null : toUri(release.trim()));
                }
                if (config.isUpdateEditable()) {
                    String update = updateUriText.getText();
                    if (StringUtils.isBlank(update)) {
                        config.setUpdates(monitor, null);
                    } else {
                        Collection<URI> updates = new ArrayList<>();
                        String[] splits = update.split(",");
                        for (String split : splits) {
                            updates.add(toUri(split.trim()));
                        }
                        config.setUpdates(monitor, updates);
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
        }
        return super.performOk();
    }

    private URI toUri(String path) throws Exception {
        URI uri = null;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            if (path.contains("\\")) {
                try {
                    uri = new File(path).toURI();
                } catch (Exception ex) {
                    throw e;
                }
            } else {
                throw e;
            }
        }
        return uri;
    }

    @Override
    protected void performDefaults() {
        if (this.isControlCreated()) {
            // TODO using talend offical?
        }
        super.performDefaults();
    }

    private void refresh() {
        this.updateApplyButton();
        this.getContainer().updateButtons();
    }

    private boolean validate() {
        this.setErrorMessage(null);
        if (StringUtils.equals(releaseUriText.getText().trim(), updateUriText.getText().trim())
                && StringUtils.isNotBlank(releaseUriText.getText())) {
            this.setErrorMessage(Messages.getString("UpdatesitePreferencePage.err.releaseAndUpdateShouldBeDiff"));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid() && (this.isControlCreated() && validate());
    }

}
