package org.talend.sdk.studio.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.utils.emf.talendfile.ElementValueType;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ElementParameterTypeImpl;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ElementValueTypeImpl;
import org.talend.designer.core.model.utils.emf.talendfile.impl.NodeTypeImpl;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.model.parameter.PropertyDefinitionDecorator;
import org.talend.sdk.component.studio.model.parameter.VersionParameter;
import org.talend.sdk.component.studio.model.parameter.WidgetTypeMapper;

/**
 * Wrapper for NodeTypeImpl - class which represents persisted node (component in the process).
 * Provides additional functionality
 */
public final class TaCoKitNode {

    public static final String TACOKIT_COMPONENT_ID = "TACOKIT_COMPONENT_ID";

    public static final String TACOKIT_METADATA_TYPE_ID = "TACOKIT_METADATA_TYPE_ID";

    private final NodeTypeImpl node;

    private final ComponentDetail detail;

    private static final Set<String> MIGRATION_EXCLUSIONS = new HashSet<>(
            Arrays.asList(EParameterName.UNIQUE_NAME.getName(), EParameterName.ACTIVATE.getName(), TACOKIT_COMPONENT_ID));

    public TaCoKitNode(final NodeTypeImpl node) {
        Objects.requireNonNull(node, "Node should not be null");
        if (!isTacokit(node)) {
            throw new IllegalArgumentException("It is not Tacokit node " + node.getComponentName());
        }
        this.node = node;
        final String componentId = getComponentId(node).orElseThrow(() ->
                new IllegalStateException("No component detail for " + node.getComponentName()));
        this.detail = Lookups.service().getDetailById(componentId);
    }

    public String getId() {
        return detail.getId().getId();
    }

    public boolean needsMigration() {
        final int currentVersion = detail.getVersion();
        final int persistedVersion = getPersistedVersion();
        if (currentVersion < persistedVersion) {
            throw new IllegalStateException("current version: " + currentVersion + " persisted version: " + persistedVersion);
        }
        return currentVersion != persistedVersion;
    }

    public Map<String, String> getPropertiesToMigrate() {
        Map<String, String> properties = new HashMap<>();
        @SuppressWarnings("rawtypes")
        EList parameters = node.getElementParameter();
        for (final Object elem : parameters) {
            ElementParameterTypeImpl parameter = (ElementParameterTypeImpl) elem;
            if (!MIGRATION_EXCLUSIONS.contains(parameter.getName())) {
                if (EParameterFieldType.TABLE.name().equals(parameter.getField())) {
                    addTableElementValue(properties, parameter);
                } else if (!EParameterFieldType.TECHNICAL.name().equals(parameter.getField())
                        || parameter.getName().endsWith(VersionParameter.VERSION_SUFFIX)) {
                    properties.put(parameter.getName(), parameter.getValue());
                }
            }
        }
        return properties;
    }

    private void addTableElementValue(Map<String, String> properties, ElementParameterTypeImpl tableElementParam) {
        List list = tableElementParam.getElementValue();
        if (list != null) {
            int index = 0;
            Set<String> keySet = new HashSet<String>();
            for (int i = 0; i < list.size(); i++) {
                Object value = list.get(i);
                if (value instanceof ElementValueType) {
                    ElementValueType eValue = (ElementValueType) value;
                    if (keySet.contains(eValue.getElementRef())) {
                        keySet.clear();
                        index ++;
                    } else {
                        keySet.add(eValue.getElementRef());
                    }
                    String paramName = getTableParamName(index, eValue);
                    if (paramName != null) {
                        properties.put(paramName, eValue.getValue());
                    }
                }
            }
        }
    }

    private String getTableParamName(int index, ElementValueType elementValueType) {
        String paramValue = elementValueType.getElementRef();
        if (paramValue != null && paramValue.indexOf("[") >= 0 && paramValue.indexOf("]") > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(paramValue.substring(0, paramValue.indexOf("[") + 1)).append(index)
                    .append(paramValue.substring(paramValue.indexOf("]")));
            return sb.toString();
        }
        return null;
    }
    
    private boolean isComponentProperty(final String name) {
        return detail.getProperties().stream().anyMatch(property -> property.getPath().equals(name));
    }

    @SuppressWarnings("unchecked")
    public void migrate(final Map<String, String> properties) {
        final List<ElementParameterTypeImpl> noMigration = getParametersExcludedFromMigration();
        final List<ElementParameterTypeImpl> tableFieldParamList = getTableFieldParameterFromMigration();
        node.getElementParameter().clear();
        node.getElementParameter().addAll(noMigration);
        properties.entrySet().stream()
                .filter(e -> (isComponentProperty(e.getKey()) && !(Pattern.compile("(\\[)\\d+(\\])").matcher(e.getKey()).find())))
                .forEach(e -> node.getElementParameter().add(createParameter(e.getKey(), e.getValue())));
        properties.entrySet().stream().filter(e -> e.getKey().endsWith(VersionParameter.VERSION_SUFFIX)).forEach(e -> {
            final ElementParameterTypeImpl parameter = new ElementParameter();
            parameter.setName(e.getKey());
            parameter.setValue(e.getValue());
            parameter.setField(EParameterFieldType.TECHNICAL.getName());
            parameter.setShow(false);
            node.getElementParameter().add(parameter);
        });
        properties.entrySet().stream().filter(e -> Pattern.compile("(\\[)\\d+(\\])").matcher(e.getKey()).find())
                .forEach(e -> fillTableParamData(tableFieldParamList, e.getKey(), e.getValue()));
        node.getElementParameter().addAll(tableFieldParamList);
        node.setComponentVersion(Integer.toString(detail.getVersion()));
    }

    private void fillTableParamData(List<ElementParameterTypeImpl> tableFieldParamList, String paramKey, String paramValue) {
        String paramName = paramKey.substring(0, paramKey.indexOf("["));
        String elemRef = paramKey.substring(0, paramKey.indexOf("[") + 1) + paramKey.substring(paramKey.indexOf("]"));
        int paramIndex = Integer.parseInt(paramKey.substring(paramKey.indexOf("[") + 1, paramKey.indexOf("]")));
        ElementParameterTypeImpl sameNameParam = null;
        for (ElementParameterTypeImpl param : tableFieldParamList) {
            if (param.getName().equals(paramName)) {
                sameNameParam = param;
                List list = param.getElementValue();
                Set<String> keySet = new HashSet<String>();
                int index = 0, i = 0;
                for (; i < list.size();) {
                    ElementValueType eValue = (ElementValueType) list.get(i);
                    if (paramIndex == index) {
                        break;
                    } else if (keySet.contains(eValue.getElementRef())) {
                        index ++;
                        keySet.clear();
                    } else {
                        keySet.add(eValue.getElementRef());
                        i++;
                    }
                }
                for (; i < list.size(); i++) {
                    ElementValueType eValue = (ElementValueType) list.get(i);
                    if (elemRef.equals(eValue.getElementRef())) {
                        eValue.setValue(paramValue);
                        return;
                    } else {
                        continue;
                    }
                }
            }
        }
        if (sameNameParam == null) {
            sameNameParam = new ElementParameter();
            sameNameParam.setName(paramName);
            sameNameParam.setField(EParameterFieldType.TABLE.name());
            tableFieldParamList.add(sameNameParam);
        }
        ElementValueType elementValueType = new ElementValueType();
        elementValueType.setElementRef(elemRef);
        elementValueType.setValue(paramValue);    
        sameNameParam.getElementValue().add(elementValueType);        
    }

    private ElementParameterTypeImpl createParameter(final String name, final String value) {
        final ElementParameterTypeImpl parameter = new ElementParameter();
        parameter.setName(name);
        parameter.setValue(value);
        parameter.setField(getPropertyType(name));
        return parameter;
    }

    private SimplePropertyDefinition getProperty(final String path) {
        return detail.getProperties().stream().filter(property -> property.getPath().equals(path)).findFirst().orElseThrow(
                () -> new IllegalArgumentException("Can't find property for name: " + path));
    }

    private String getPropertyType(final String path) {
        final PropertyDefinitionDecorator property = PropertyDefinitionDecorator.wrap(getProperty(path));
        return new WidgetTypeMapper().getFieldType(property).getName();
    }

    private List<ElementParameterTypeImpl> getTableFieldParameterFromMigration() {
        List<ElementParameterTypeImpl> list = new ArrayList<>();
        for (final Object elem : node.getElementParameter()) {
            if (EParameterFieldType.TABLE.name().equals(((ElementParameterTypeImpl) elem).getField())) {
                list.add((ElementParameterTypeImpl) elem);
            }
        }
        return list;
    }
    
    private List<ElementParameterTypeImpl> getParametersExcludedFromMigration() {
        List<ElementParameterTypeImpl> technical = new ArrayList<>();
        for (final Object elem : node.getElementParameter()) {
            if (MIGRATION_EXCLUSIONS.contains(((ElementParameterTypeImpl) elem).getName())) {
                technical.add((ElementParameterTypeImpl) elem);
            }
        }
        return technical;
    }

    public int getPersistedVersion() {
        return Integer.parseInt(node.getComponentVersion());
    }

    public static boolean isTacokit(final NodeTypeImpl node) {
        return getComponentId(node).isPresent();
    }

    private static Optional<String> getComponentId(final NodeTypeImpl node) {
        for (final Object elem : node.getElementParameter()) {
            ElementParameterTypeImpl parameter = (ElementParameterTypeImpl) elem;
            if (TACOKIT_COMPONENT_ID.equals(parameter.getName())) {
                return Optional.ofNullable(parameter.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * This class extends ElementParameterTypeImpl to allow instance creation
     */
    private static class ElementParameter extends ElementParameterTypeImpl {

        /**
         * Extends super class constructor visibility
         */
        public ElementParameter() {
            super();
        }
    }
    
    private static class ElementValueType extends ElementValueTypeImpl {

        /**
         * Extends super class constructor visibility
         */
        public ElementValueType() {
            super();
        }
    }

}
