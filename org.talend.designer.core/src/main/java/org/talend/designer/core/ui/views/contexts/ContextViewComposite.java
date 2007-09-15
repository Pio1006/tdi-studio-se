// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.core.ui.views.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.ui.context.ContextComposite;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.MultiPageTalendEditor;
import org.talend.designer.core.ui.editor.cmd.ContextAddParameterCommand;
import org.talend.designer.core.ui.editor.cmd.ContextChangeDefaultCommand;
import org.talend.designer.core.ui.editor.cmd.ContextRemoveParameterCommand;
import org.talend.designer.core.ui.editor.cmd.ContextRenameParameterCommand;
import org.talend.designer.core.ui.editor.cmd.ContextRepositoryCommand;
import org.talend.designer.core.ui.editor.cmd.ContextTemplateModifyCommand;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.designer.core.ui.editor.process.Process;

/**
 * A concrete class of ContextComposite for the context view. <br/>
 * 
 */
public class ContextViewComposite extends ContextComposite {

    MultiPageTalendEditor part;

    private CCombo typeCombo;

    private CCombo repositoryCombo;

    private String currentRepositoryContext = null;

    private Map<String, ContextItem> repositoryContextItemMap = null;

    private BidiMap repositoryContextValueMap = null;

    /**
     * bqian ContextComposite constructor comment.
     * 
     * @param parent
     * @param style
     */
    public ContextViewComposite(Composite parent, ContextsView contextView) {
        super(parent);
    }

    public void setPart(MultiPageTalendEditor part) {
        this.part = part;
        refresh();
    }

    private Map<String, ContextItem> getRepositoryContextItemMap() {
        if (repositoryContextItemMap == null) {
            repositoryContextItemMap = new HashMap<String, ContextItem>();
        }
        return repositoryContextItemMap;
    }

    private BidiMap getRepositoryContextValueMap() {
        if (repositoryContextValueMap == null) {
            repositoryContextValueMap = new DualHashBidiMap();
        }
        return repositoryContextValueMap;
    }

    protected void initializeUI() {
        super.initializeUI();
    }

    @Override
    protected void addChoiceComponents(Composite composite) {
        updateContextList();
        CLabel label = new CLabel(composite, SWT.NONE);
        // label.setBackground(this.getBackground());
        label.setText(Messages.getString("ContextProcessSection2.contextType")); //$NON-NLS-1$

        typeCombo = new CCombo(composite, SWT.BORDER);
        typeCombo.setEditable(false);

        typeCombo.setItems(new String[] { EmfComponent.TEXT_BUILTIN, EmfComponent.TEXT_REPOSITORY });
        if (currentRepositoryContext == null) {
            typeCombo.setText(EmfComponent.TEXT_BUILTIN);
        } else {
            typeCombo.setText(EmfComponent.TEXT_REPOSITORY);
        }

        repositoryCombo = new CCombo(composite, SWT.BORDER);
        repositoryCombo.setEditable(false);

        if (currentRepositoryContext == null) {
            repositoryCombo.setVisible(false);
        } else {
            repositoryCombo.setVisible(true);
        }

        if (getProcess() != null && currentRepositoryContext != null) {
            updateContextList();
            repositoryCombo.setText(currentRepositoryContext);
        }
        addListeners();
        super.addChoiceComponents(composite);
    }

    protected void refreshChoiceComposite() {
        if (currentRepositoryContext == null) {
            typeCombo.setText(EmfComponent.TEXT_BUILTIN);
        } else {
            typeCombo.setText(EmfComponent.TEXT_REPOSITORY);
        }
        if (currentRepositoryContext == null) {
            repositoryCombo.setVisible(false);
        } else {
            repositoryCombo.setVisible(true);
        }
        if (getProcess() != null && currentRepositoryContext != null) {
            updateContextList();
            repositoryCombo.setText(currentRepositoryContext);
        }
        super.refreshChoiceComposite();
    }

    private void updateContextList() {
        IProxyRepositoryFactory factory = DesignerPlugin.getDefault().getProxyRepositoryFactory();
        List<ContextItem> contextItemList = null;
        String[] repositoryContextNames = new String[] {};
        // String[] repositoryContextValues = new String[] {};
        List<String> contextNamesList = new ArrayList<String>();
        try {
            contextItemList = factory.getContextItem();
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        if (contextItemList != null) {
            getRepositoryContextItemMap().clear();
            for (ContextItem contextItem : contextItemList) {
                if (factory.getStatus(contextItem) != ERepositoryStatus.DELETED) {
                    String name = Messages.getString("ContextProcessSection2.context") + contextItem.getProperty().getLabel(); //$NON-NLS-1$
                    String value = contextItem.getProperty().getId();
                    getRepositoryContextItemMap().put(name, contextItem);
                    getRepositoryContextValueMap().put(value, name);
                    contextNamesList.add(name);
                }
            }
            repositoryContextNames = (String[]) contextNamesList.toArray(new String[0]);
        }
        loadRepositoryContextFromProcess();
        if (getProcess() != null && repositoryCombo != null && !repositoryCombo.isDisposed()) {
            repositoryCombo.setItems(repositoryContextNames);
            if (repositoryContextNames.length != 0 && (!contextNamesList.contains(currentRepositoryContext))) {
                currentRepositoryContext = repositoryContextNames[0];
                repositoryCombo.setText(repositoryContextNames[0]);
                layoutButtonBar();
                ContextItem contextItem = getRepositoryContextItemMap().get(repositoryContextNames[0]);
                getCommandStack().execute(new ContextRepositoryCommand(getJob(), contextItem));
            }
        }
    }

    private void loadRepositoryContextFromProcess() {
        if (getProcess() == null) {
            currentRepositoryContext = null;
            setReadOnly(true);
            return;
        }
        String repositoryId = ((Process) getProcess()).getRepositoryId();
        if (getRepositoryContextValueMap().containsKey(repositoryId)) {
            currentRepositoryContext = (String) getRepositoryContextValueMap().get(repositoryId);
            setReadOnly(true);
        } else {
            currentRepositoryContext = null;
            setReadOnly(getProcess().isReadOnly());
        }
    }

    /**
     * qzhang Comment method "addListeners".
     */
    private void addListeners() {
        typeCombo.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                CCombo combo = (CCombo) e.getSource();

                if (combo.getText().equals(EmfComponent.TEXT_REPOSITORY)) {
                    updateContextList();
                    if (repositoryCombo.getItemCount() == 0) {
                        repositoryCombo.setText("");
                    } else {
                        repositoryCombo.setText(currentRepositoryContext);
                    }
                    repositoryCombo.setVisible(true);
                } else {
                    repositoryCombo.setVisible(false);
                    currentRepositoryContext = null;
                    getCommandStack().execute(new ContextRepositoryCommand(getJob(), null));
                }
            }
        });
        repositoryCombo.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                CCombo combo = (CCombo) e.getSource();
                currentRepositoryContext = combo.getText();
                ContextItem contextItem = repositoryContextItemMap.get(currentRepositoryContext);
                getCommandStack().execute(new ContextRepositoryCommand(getJob(), contextItem));
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ui.context.ContextComposite#refresh()
     */
    @Override
    public void refresh() {
        super.refresh();
        DesignerPlugin.getDefault().getRunProcessService().refreshView();
    }

    public CommandStack getCommandStack() {
        return part == null ? null : (CommandStack) (part.getTalendEditor().getAdapter(CommandStack.class));
    }

    public IContextManager getContextManager() {
        return getProcess() == null ? null : getProcess().getContextManager();
    }

    public IProcess getProcess() {
        return part == null ? null : part.getTalendEditor().getProcess();
    }

    private Process getJob() {
        return (Process) getProcess();
    }

    public void onContextChangeDefault(IContextManager contextManager, IContext newDefault) {
        getCommandStack().execute(new ContextChangeDefaultCommand(contextManager, newDefault));
    }

    public void onContextRenameParameter(IContextManager contextManager, String oldName, String newName) {
        getCommandStack().execute(new ContextRenameParameterCommand(contextManager, oldName, newName));
    }

    public void onContextModify(IContextManager contextManager, IContextParameter parameter) {
        getCommandStack().execute(new ContextTemplateModifyCommand(getProcess(), contextManager, parameter));
    }

    public void onContextAddParameter(IContextManager contextManager, IContextParameter parameter) {
        getCommandStack().execute(new ContextAddParameterCommand(getContextManager(), parameter));
    }

    public void onContextRemoveParameter(IContextManager contextManager, String paramName) {
        getCommandStack().execute(new ContextRemoveParameterCommand(getContextManager(), paramName));
    }

}
