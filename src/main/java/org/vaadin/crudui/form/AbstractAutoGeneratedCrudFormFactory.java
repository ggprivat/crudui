package org.vaadin.crudui.form;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.converter.*;
import com.vaadin.data.util.BeanUtil;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.shared.util.SharedUtil;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.impl.DefaultFieldProvider;
import org.vaadin.data.converter.*;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alejandro Duarte.
 */
public abstract class AbstractAutoGeneratedCrudFormFactory<T> extends AbstractCrudFormFactory<T> {

    protected Map<CrudOperation, String> buttonCaptions = new HashMap<>();
    protected Map<CrudOperation, Resource> buttonIcons = new HashMap<>();
    protected Map<CrudOperation, Set<String>> buttonStyleNames = new HashMap<>();

    protected String cancelButtonCaption = "Cancel";
    protected String validationErrorMessage = "Please fix the errors and try again";
    protected Class<T> domainType;

    protected Binder<T> binder;

    public AbstractAutoGeneratedCrudFormFactory(Class<T> domainType) {
        this.domainType = domainType;

        setButtonCaption(CrudOperation.READ, "Ok");
        setButtonCaption(CrudOperation.ADD, "Add");
        setButtonCaption(CrudOperation.UPDATE, "Update");
        setButtonCaption(CrudOperation.DELETE, "Yes, delete");

        setButtonIcon(CrudOperation.READ, null);
        setButtonIcon(CrudOperation.ADD, VaadinIcons.CHECK);
        setButtonIcon(CrudOperation.UPDATE, VaadinIcons.CHECK);
        setButtonIcon(CrudOperation.DELETE, VaadinIcons.TRASH);

        addButtonStyleName(CrudOperation.READ, null);
        addButtonStyleName(CrudOperation.ADD, ValoTheme.BUTTON_PRIMARY);
        addButtonStyleName(CrudOperation.UPDATE, ValoTheme.BUTTON_PRIMARY);
        addButtonStyleName(CrudOperation.DELETE, ValoTheme.BUTTON_DANGER);

        setVisibleProperties(discoverProperties().toArray(new String[0]));
    }

    public void setButtonCaption(CrudOperation operation, String caption) {
        buttonCaptions.put(operation, caption);
    }

    public void setButtonIcon(CrudOperation operation, Resource icon) {
        buttonIcons.put(operation, icon);
    }

    public void addButtonStyleName(CrudOperation operation, String styleName) {
        buttonStyleNames.putIfAbsent(operation, new HashSet<>());
        buttonStyleNames.get(operation).add(styleName);
    }

    public void setCancelButtonCaption(String cancelButtonCaption) {
        this.cancelButtonCaption = cancelButtonCaption;
    }

    public void setValidationErrorMessage(String validationErrorMessage) {
        this.validationErrorMessage = validationErrorMessage;
    }

    protected List<String> discoverProperties() {
        try {
            List<PropertyDescriptor> descriptors = BeanUtil.getBeanPropertyDescriptors(domainType);
            return descriptors.stream()
                    .filter(d -> !d.getName().equals("class"))
                    .map(d -> d.getName()).collect(Collectors.toList());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    protected Binder<T> buildBinder(CrudOperation operation, T domainObject) {
        if (getConfiguration(operation).isUseBeanValidation()) {
            return new BeanValidationBinder(domainObject.getClass());
        }

        return new Binder(domainObject.getClass());
    }

    protected List<HasValue> buildAndBind(CrudOperation operation, T domainObject, boolean readOnly) {
        ArrayList<HasValue> fields = new ArrayList<>();
        binder = buildBinder(operation, domainObject);
        binder.setBean(domainObject);
        CrudFormConfiguration configuration = getConfiguration(operation);

        for (int i = 0; i < configuration.getVisibleProperties().size(); i++) {
            String property = configuration.getVisibleProperties().get(i);
            try {
                HasValue<Object> field;
                Class<?> propertyType = BeanUtil.getPropertyType(domainObject.getClass(), property);

                if (propertyType == null) {
                    throw new RuntimeException("Cannot find type for property " + domainObject.getClass().getName() + "." + property);
                }

                FieldProvider provider = configuration.getFieldProviders().get(property);
                if (provider != null) {
                    field = provider.buildField();
                } else {
                    Class<? extends HasValue> fieldType = configuration.getFieldTypes().get(property);
                    if (fieldType != null) {
                        field = fieldType.newInstance();
                    } else {
                        field = new DefaultFieldProvider(propertyType).buildField();
                    }
                }

                if (field instanceof AbstractField) {
                    if (!configuration.getFieldCaptions().isEmpty()) {
                        ((AbstractField) field).setCaption(configuration.getFieldCaptions().get(i));
                    } else {
                        ((AbstractField) field).setCaption(SharedUtil.propertyIdToHumanFriendly(property));
                    }
                }

                setDefaultConfiguration(field);

                Binder.BindingBuilder bindingBuilder = binder.forField(field);

                if (AbstractTextField.class.isAssignableFrom(field.getClass())) {
                    bindingBuilder = bindingBuilder.withNullRepresentation("");
                }

                if (Double.class.isAssignableFrom(propertyType) || double.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToDoubleConverter(null, "Must be a number"));
                    
                } else if (Long.class.isAssignableFrom(propertyType) || long.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToLongConverter(null, "Must be a number"));
                    
                } else if (BigDecimal.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToBigDecimalConverter(null, "Must be a number"));

                } else if (BigInteger.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToBigIntegerConverter(null, "Must be a number"));

                } else if (Integer.class.isAssignableFrom(propertyType) || int.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToIntegerConverter(null, "Must be a number"));

                } else if (Byte.class.isAssignableFrom(propertyType) || byte.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToByteConverter(null, "Must be a number"));

                } else if (Character.class.isAssignableFrom(propertyType) || char.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToCharacterConverter());

                } else if (Float.class.isAssignableFrom(propertyType) || float.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new StringToFloatConverter(null, "Must be a number"));

                } else if (Date.class.isAssignableFrom(propertyType)) {
                    bindingBuilder = bindingBuilder.withConverter(new LocalDateTimeToDateConverter(ZoneId.systemDefault()));
                }

                bindingBuilder.bind(property);

                field.setReadOnly(readOnly);

                if (!configuration.getDisabledProperties().isEmpty()) {
                    ((Component) field).setEnabled(!configuration.getDisabledProperties().contains(property));
                }

                FieldCreationListener creationListener = configuration.getFieldCreationListeners().get(property);
                if (creationListener != null) {
                    creationListener.fieldCreated(field);
                }

                fields.add(field);
            } catch (Exception e) {
                throw new RuntimeException("Error creating Field for property " + domainObject.getClass().getName() + "." + property, e);
            }
        }

        if (!fields.isEmpty() && !readOnly) {
            HasValue field = fields.get(0);
            if (field instanceof Component.Focusable) {
                ((Component.Focusable) field).focus();
            }
        }

        return fields;
    }

    protected void setDefaultConfiguration(HasValue<?> field) {
        if (field != null && field instanceof Component) {
            ((Component) field).setWidth("100%");
        }
    }

    protected Button buildOperationButton(CrudOperation operation, T domainObject, Button.ClickListener clickListener) {
        if (clickListener == null) {
            return null;
        }

        Button button = new Button(buttonCaptions.get(operation), buttonIcons.get(operation));
        buttonStyleNames.get(operation).forEach(styleName -> button.addStyleName(styleName));
        button.addClickListener(event -> {
            if (binder.validate().isOk()) {
                clickListener.buttonClick(event);
            } else {
                Notification.show(validationErrorMessage);
            }
        });
        return button;
    }

    protected Button buildCancelButton(Button.ClickListener clickListener) {
        if (clickListener == null) {
            return null;
        }

        return new Button(cancelButtonCaption, clickListener);
    }

    protected Layout buildFooter(CrudOperation operation, T domainObject, Button.ClickListener cancelButtonClickListener, Button.ClickListener operationButtonClickListener) {
        Button operationButton = buildOperationButton(operation, domainObject, operationButtonClickListener);
        Button cancelButton = buildCancelButton(cancelButtonClickListener);

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setSizeUndefined();
        footerLayout.setSpacing(true);

        if (cancelButton != null) {
            footerLayout.addComponent(cancelButton);
        }

        if (operationButton != null) {
            footerLayout.addComponent(operationButton);
        }

        return footerLayout;
    }

}
