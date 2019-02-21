package org.holodeckb2b.common.testhelpers.pmode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Created at 14:24 25.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ValidatorConfig implements IMessageValidatorConfiguration {

    private String  id;
    private String  validatorFactoryClass;
    private Collection<IProperty> parameters;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getFactory() {
        return validatorFactoryClass;
    }

    public void setFactory(String validatorFactoryClass) {
        this.validatorFactoryClass = validatorFactoryClass;
    }

    @Override
    public Map<String, ?> getSettings() {
        if (!Utils.isNullOrEmpty(parameters)) {
            final HashMap<String, String> settings = new HashMap<>();
            for (final IProperty p : parameters)
                settings.put(p.getName(), p.getValue());

            return settings;
        } else
            return null;
    }

}
