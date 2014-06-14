package com.fillumina.buildercreator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class FluentSetterGenerator implements CodeGenerator {

    private final JTextComponent textComp;
    private final List<VariableElement> fields;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private FluentSetterGenerator(Lookup context, List<VariableElement> fields) {
        this.textComp = context.lookup(JTextComponent.class);
        this.fields = fields;
        removeStaticFinalFields(fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Fluent setters...";
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    @Override
    public void invoke() {
        try {
            Document doc = textComp.getDocument();
            JavaSource javaSource = JavaSource.forDocument(doc);

            CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>() {

                @Override
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    generate(workingCopy);
                }

                private void generate(WorkingCopy wc) throws IOException {
                    wc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    final int caretOffset = textComp.getCaretPosition();

                    TreePath path = wc.getTreeUtilities().pathFor(caretOffset);

                    path = SourceHelper
                            .getParentElementOfKind(Tree.Kind.CLASS, path);

                    int idx = SourceHelper.findClassMemberIndex(wc,
                            (ClassTree) path.getLeaf(),
                            caretOffset);

                    generateFluentSetters(wc, path, idx);
                }

                @Override
                public void cancel() {
                }
            };

            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void generateFluentSetters(WorkingCopy wc,
            TreePath path,
            int positionOfMethod) {

        assert path.getLeaf().getKind() == Tree.Kind.CLASS;

        TypeElement typeClassElement = (TypeElement) wc.getTrees().getElement(path);
        if (typeClassElement != null) {
            int index = positionOfMethod;

            TreeMaker make = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<>(classTree.getMembers());

            index = SourceHelper
                    .removeExistingFluentSetters(members, index, fields);

            SourceHelper.addFluentSetterMethods(fields,
                    make, typeClassElement.toString(), members, index);

            ClassTree newClassTree = make.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }

    private void removeStaticFinalFields(List<VariableElement> fields) {
        for (Iterator<VariableElement> i=fields.iterator(); i.hasNext();) {
            VariableElement element = i.next();

            if (element.getModifiers().contains(Modifier.STATIC) ||
                    element.getModifiers().contains(Modifier.FINAL)) {
                i.remove();
            }
        }
    }

    @MimeRegistration(mimeType = "text/x-java",
            service = CodeGenerator.Factory.class)
    public static class Factory extends BaseCodeGeneratorFactory {

        public Factory() {
            super(new FieldGeneratorFactory() {

                @Override
                public CodeGenerator create(Lookup context,
                        List<VariableElement> fields) {
                    return new FluentSetterGenerator(context, fields);
                }
            });
        }
    }
}
