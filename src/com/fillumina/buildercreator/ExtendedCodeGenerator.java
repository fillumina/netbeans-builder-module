package com.fillumina.buildercreator;

import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.lang.model.element.VariableElement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Lookup;

public abstract class ExtendedCodeGenerator implements CodeGenerator {
    static final Logger LOG = Logger.getLogger(ExtendedCodeGenerator.class.getName());

    private final JTextComponent textComponent;
    private final List<VariableElement> fields;

    public ExtendedCodeGenerator(Lookup context, List<VariableElement> fields) {
        this.textComponent = context.lookup(JTextComponent.class);
        final ArrayList<VariableElement> filteredFields = new ArrayList<>(fields);
        filterFields(filteredFields);
        this.fields = Collections.unmodifiableList(filteredFields);
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog.
     */
    @Override
    public void invoke() {
        Document doc = textComponent.getDocument();
        JavaSource javaSource = JavaSource.forDocument(doc);

        CancellableTask<WorkingCopy> task =
                new CodeGeneratorCancellableTask(textComponent) {

            @Override
            public void generateCode(WorkingCopy workingCopy, TreePath path,
                    int position) {
                ExtendedCodeGenerator.this
                        .generateCode(workingCopy, path, position,
                                ExtendedCodeGenerator.this.fields);
            }

        };

        try {
            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();
        } catch (IOException ex) {
            LOG.severe(ex.toString());
        }

    }

    public List<VariableElement> getFields() {
        return fields;
    }

    protected abstract void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields);


    private void filterFields(List<VariableElement> fields) {
        for (Iterator<VariableElement> i=fields.iterator(); i.hasNext();) {
            VariableElement element = i.next();
            if (filterOutField(element)) {
                i.remove();
            }
        }
    }

    protected abstract boolean filterOutField(VariableElement element);
}
