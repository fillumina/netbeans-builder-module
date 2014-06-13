package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.editor.GuardedDocument;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class BuilderGenerator implements CodeGenerator {

    public static final String TO_STRING = "toString";

    private final JTextComponent textComp;
    private final List<VariableElement> fields;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private BuilderGenerator(Lookup context, List<VariableElement> fields) {
        this.textComp = context.lookup(JTextComponent.class);
        this.fields = fields;
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Fluent setters generator";
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
                    //createMethod(workingCopy);
                    generate(workingCopy);
                }

                private void generate(WorkingCopy wc) throws IOException {
                    wc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    final int caretOffset = textComp.getCaretPosition();

                    TreePath path = wc.getTreeUtilities().pathFor(caretOffset);

                    path = getParentElementOfKind(Tree.Kind.CLASS, path);

                    int idx = findClassMemberIndex(wc,
                            (ClassTree) path.getLeaf(),
                            caretOffset);

                    final BuilderOptions options =
                            new BuilderOptions(fields, idx);

                    generateToString(wc, path, options);
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

    private void generateToString(WorkingCopy wc,
            TreePath path,
            final BuilderOptions options) {

        assert path.getLeaf().getKind() == Tree.Kind.CLASS;

        TypeElement te = (TypeElement) wc.getTrees().getElement(path);
        if (te != null) {
            int index = options.getPositionOfMethod();

            TreeMaker maker = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();

            //
            // removes an existing toString() method
            //
            List<Tree> members = new ArrayList<>(classTree.getMembers());
            // use an iterator to prevent concurrent modification
            for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
                Tree member = treeIt.next();

                if (member.getKind().equals(Tree.Kind.METHOD)) {
                    MethodTree mt = (MethodTree) member;
                    // this may looks strange, but I've seen code with methods like:
                    // public String toString(Object o) {}
                    // and I think we shouldn't remove them :)
                    // so we should ensure to get right toString() method, means
                    // a return value and no parameters
                    if (mt.getName().contentEquals(TO_STRING) &&
                            mt.getParameters().isEmpty() &&
                            mt.getReturnType() != null &&
                            mt.getReturnType().getKind() == Tree.Kind.IDENTIFIER) {
                        treeIt.remove();
                        // decrease the index to use, as we else will get an
                        // ArrayIndexOutOfBounds (if added at the end of a class)
                        index--;
                        break;
                    }
                }
            }

            ToStringBuilder tsb = new ToStringBuilder();

            Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
            List<AnnotationTree> annotations = new ArrayList<>();
            AnnotationTree newAnnotation = maker.Annotation(
                    maker.Identifier("Override"),
                    Collections.<ExpressionTree>emptyList());
            annotations.add(newAnnotation);

            TypeElement element = wc.getElements()
                    .getTypeElement("java.lang.String");
            ExpressionTree returnType = maker.QualIdent(element);

            final BlockTree body = tsb.buildToString(wc,
                    classTree.getSimpleName().toString(),
                    options);

            MethodTree method = maker.Method(
                    maker.Modifiers(modifiers, annotations),
                    TO_STRING,
                    returnType,
                    Collections.<TypeParameterTree>emptyList(),
                    Collections.<VariableTree>emptyList(),
                    Collections.<ExpressionTree>emptyList(),
                    body,
                    null);

            members.add(index, method);

            ClassTree newClassTree = maker.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }

    private int findClassMemberIndex(WorkingCopy wc,
            ClassTree classTree,
            int offset) {

        int index = 0;
        SourcePositions sp = wc.getTrees().getSourcePositions();
        GuardedDocument gdoc = null;
        try {
            Document doc = wc.getDocument();
            if (doc != null && doc instanceof GuardedDocument) {
                gdoc = (GuardedDocument) doc;
            }
        } catch (IOException ioe) {
        }

        Tree lastMember = null;
        for (Tree tree : classTree.getMembers()) {
            if (offset <= sp.getStartPosition(wc.getCompilationUnit(), tree)) {
                if (gdoc == null) {
                    break;
                }
                int pos = (int) (lastMember != null ? sp.getEndPosition(wc.
                        getCompilationUnit(), lastMember) : sp.getStartPosition(
                                wc.getCompilationUnit(), classTree));
                pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                if (pos <= sp.getStartPosition(wc.getCompilationUnit(), tree)) {
                    break;
                }
            }
            index++;
            lastMember = tree;
        }
        return index;
    }

    @MimeRegistration(mimeType = "text/x-java",
            service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {

            JTextComponent component = context.lookup(JTextComponent.class);

            CompilationController controller =
                    context.lookup(CompilationController.class);

            TreePath treePath = context.lookup(TreePath.class);

            TreePath path = getParentElementOfKind(Tree.Kind.CLASS, treePath);

            if (component == null || controller == null || path == null) {
                return Collections.<CodeGenerator>emptyList();
            }

            try {
                controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
            } catch (IOException ioe) {
                return Collections.<CodeGenerator>emptyList();
            }

            Elements elements = controller.getElements();

            TypeElement typeElement = (TypeElement)
                    controller.getTrees().getElement(path);

            if (typeElement == null || !typeElement.getKind().isClass()) {
                return Collections.<CodeGenerator>emptyList();
            }

//            final List<? extends Element> enclosedElements =
//                    typeElement.getEnclosedElements();

            // gives out all the methods
            //ElementFilter.methodsIn(elements.getAllMembers(typeElement));


            final List<VariableElement> fields =
                    ElementFilter.fieldsIn(elements.getAllMembers(typeElement));

            return Collections.singletonList(new BuilderGenerator(context, fields));
        }
    }

    private static TreePath getParentElementOfKind(Tree.Kind kind,
            TreePath path) {
        if (path != null) {
            TreePath tpath = path;
            while (tpath != null) {
                if (kind == tpath.getLeaf().getKind()) {
                    return tpath;
                }
                tpath = tpath.getParentPath();
            }
        }
        return null;
    }

}
