package com.fillumina.buildercreator;

import com.sun.source.util.TreePath;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Lookup;

public class FluentSetterGenerator extends ExtendedCodeGenerator {
    private static final Logger LOG = Logger.getLogger(FluentSetterGenerator.class.getName());
    
    public FluentSetterGenerator(Lookup context, List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Fluent setters...";
    }

    @Override
    @SuppressWarnings("unchecked")    
    protected void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields) {
        LOG.log(Level.INFO, "Fluent Setter called");
        
        FluentSetterCodeCreator.generateCode(wc, path, position, fields, false);
    }

    @Override
    protected boolean filterOutField(VariableElement element) {
        return element.getModifiers().contains(Modifier.STATIC) ||
                element.getModifiers().contains(Modifier.FINAL);
    }
}
