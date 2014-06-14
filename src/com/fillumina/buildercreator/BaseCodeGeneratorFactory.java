package com.fillumina.buildercreator;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.swing.text.JTextComponent;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Lookup;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class BaseCodeGeneratorFactory implements CodeGenerator.Factory {

    private final FieldGeneratorFactory factory;

    public BaseCodeGeneratorFactory(FieldGeneratorFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<? extends CodeGenerator> create(Lookup context) {

        JTextComponent component = context.lookup(JTextComponent.class);

        CompilationController controller =
                context.lookup(CompilationController.class);

        TreePath treePath = context.lookup(TreePath.class);

        TreePath path = SourceHelper
                .getParentElementOfKind(Tree.Kind.CLASS, treePath);

        if (component == null || controller == null || path == null) {
            return Collections.<CodeGenerator>emptyList();
        }

        try {
            controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
        } catch (IOException ioe) {
            return Collections.<CodeGenerator>emptyList();
        }

        Elements elements = controller.getElements();

        TypeElement typeElement = (TypeElement) controller.getTrees().
                getElement(path);

        if (typeElement == null || !typeElement.getKind().isClass()) {
            return Collections.<CodeGenerator>emptyList();
        }

        final List<VariableElement> fields =
                ElementFilter.fieldsIn(elements.getAllMembers(typeElement));

        return Collections.singletonList(factory.create(context, fields));
    }
}
