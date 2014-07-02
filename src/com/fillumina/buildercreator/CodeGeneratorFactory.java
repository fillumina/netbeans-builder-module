package com.fillumina.buildercreator;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Lookup;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
@MimeRegistration(mimeType = "text/x-java",
        service = CodeGenerator.Factory.class)
public class CodeGeneratorFactory implements CodeGenerator.Factory {
    static final Logger LOG =
            Logger.getLogger(CodeGeneratorFactory.class.getName());

    private static class CodeGeneratorException extends Exception {
        private static final long serialVersionUID = 1L;

        public CodeGeneratorException(String message) {
            super(message);
        }

        public CodeGeneratorException(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public List<? extends CodeGenerator> create(Lookup context) {
        try {
            CompilationController controller =
                    context.lookup(CompilationController.class);

            controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

            List<VariableElement> fields = getFields(context, controller);

            return Arrays.asList(new BuilderGenerator(context, fields),
                    new FluentSetterGenerator(context, fields),
                    new ConstantsGenerator(context, fields));

        } catch (CodeGeneratorException | NullPointerException | IOException ex) {
            LOG.warning(ex.toString());
            return Collections.<CodeGenerator>emptyList();
        }
    }

    private List<VariableElement> getFields(Lookup context,
            CompilationController controller)
            throws CodeGeneratorException {
        try {
            TreePath treePath = context.lookup(TreePath.class);

            TreePath path = TreeHelper
                    .getParentElementOfKind(Tree.Kind.CLASS, treePath);

            TypeElement typeElement = (TypeElement)
                    controller.getTrees().getElement(path);

            if (!typeElement.getKind().isClass()) {
                throw new CodeGeneratorException("typeElement " +
                        typeElement.getKind().name() +
                        " is not a class, cannot generate code.");
            }

            Elements elements = controller.getElements();

            return ElementFilter.fieldsIn(elements.getAllMembers(typeElement));

        } catch (NullPointerException ex) {
            throw new CodeGeneratorException(ex);
        }
    }
}
