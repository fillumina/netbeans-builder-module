package com.fillumina.buildercreator;

import com.sun.source.util.TreePath;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Lookup;

public class FluentWithSetterGenerator extends ExtendedCodeGenerator {

    public FluentWithSetterGenerator(Lookup context, List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Fluent setters using with...";
    }

    @Override
    @SuppressWarnings("unchecked")    
    protected void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields) {
        FluentSetterCodeGenerator.generateCode(wc, path, position, fields, true);
    }

    @Override
    protected boolean filterOutField(VariableElement element) {
        return element.getModifiers().contains(Modifier.STATIC) ||
                element.getModifiers().contains(Modifier.FINAL);
    }
}
