package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
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

    public static final String TOSTRING = "toString";

    JTextComponent textComp;
    List<VariableElement> fields;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private BuilderGenerator(Lookup context, List<VariableElement> fields) {
        // Good practice is not to save Lookup outside ctor
        textComp = context.lookup(JTextComponent.class);
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

                private void generate(WorkingCopy copy) throws IOException {
                    copy.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    final int caretOffset = textComp.getCaretPosition();

                    TreePath path = copy.getTreeUtilities().pathFor(caretOffset);
                    path = BuilderGenerator.
                            getPathElementOfKind(Tree.Kind.CLASS, path);
                    int idx = findClassMemberIndex(copy, (ClassTree) path.
                            getLeaf(), caretOffset);

                    final BuilderOptions options =
                            new BuilderOptions(fields, idx);

                    generateToString(copy, path, options);
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

    private void generateToString(WorkingCopy wc, TreePath path,
            final BuilderOptions options) {

        assert path.getLeaf().getKind() == Tree.Kind.CLASS;
        TypeElement te = (TypeElement) wc.getTrees().getElement(path);
        if (te != null) {
            int index = options.getPositionOfMethod();

            TreeMaker make = wc.getTreeMaker();
            ClassTree clazz = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<>(clazz.getMembers());
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
                    if (mt.getName().contentEquals(TOSTRING) && mt.
                            getParameters().isEmpty() &&
                            mt.getReturnType() != null && mt.getReturnType().
                            getKind() == Tree.Kind.IDENTIFIER) {
                        treeIt.remove();
                        // decrease the index to use, as we else will get an ArrayIndexOutOfBounds (if added at the end of a class)
                        index--;
                        break;
                    }
                }
            }

            ToStringBuilder tsb = new ToStringBuilder();

            Set<Modifier> mods = EnumSet.of(Modifier.PUBLIC);
            List<AnnotationTree> annotations = new ArrayList<>();
            if (true) {
                AnnotationTree newAnnotation = make.Annotation(
                        make.Identifier("Override"),
                        Collections.<ExpressionTree>emptyList());
                annotations.add(newAnnotation);
            }
            TypeElement element = wc.getElements().getTypeElement(
                    "java.lang.String");
            ExpressionTree returnType = make.QualIdent(element);

            MethodTree method = make.Method(make.Modifiers(mods, annotations),
                    TOSTRING, returnType, Collections.
                    <TypeParameterTree>emptyList(),
                    Collections.<VariableTree>emptyList(), Collections.
                    <ExpressionTree>emptyList(),
                    tsb.buildToString(wc, clazz.getSimpleName().toString(),
                            options), null);

            members.add(index, method);

            ClassTree nue = make.Class(clazz.getModifiers(), clazz.
                    getSimpleName(), clazz.getTypeParameters(), clazz.
                    getExtendsClause(),
                    (List<ExpressionTree>) clazz.getImplementsClause(), members);
            wc.rewrite(clazz, nue);
        }
    }

    private int findClassMemberIndex(WorkingCopy wc, ClassTree clazz, int offset) {

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
        for (Tree tree : clazz.getMembers()) {
            if (offset <= sp.getStartPosition(wc.getCompilationUnit(), tree)) {
                if (gdoc == null) {
                    break;
                }
                int pos = (int) (lastMember != null ? sp.getEndPosition(wc.
                        getCompilationUnit(), lastMember) : sp.getStartPosition(
                                wc.getCompilationUnit(), clazz));
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

    private static TreePath getPathElementOfKind(Tree.Kind kind, TreePath path) {
        EnumSet<Tree.Kind> kinds = EnumSet.of(kind);
        while (path != null) {
            if (kinds.contains(path.getLeaf().getKind())) {
                return path;
            }
            path = path.getParentPath();
        }
        return null;
    }

    @MimeRegistration(mimeType = "text/x-java",
            service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> generators = new ArrayList<>();

            JTextComponent component = context.lookup(JTextComponent.class);

            CompilationController controller = context.lookup(
                    CompilationController.class);

            TreePath path = context.lookup(TreePath.class);

            path = path != null ?
                    getPathElementOfKind(Tree.Kind.CLASS, path) : null;

            if (component == null || controller == null || path == null) {
                return generators;
            }

            try {
                controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
            } catch (IOException ioe) {
                return generators;
            }

            Elements elements = controller.getElements();

            TypeElement typeElement = (TypeElement) controller.getTrees().
                    getElement(path);

            if (typeElement == null || !typeElement.getKind().isClass()) {
                return generators;
            }

            final List<? extends Element> enclosedElements = typeElement.
                    getEnclosedElements();
            Map<String, List<ExecutableElement>> methods = new HashMap<>();
            for (ExecutableElement method : ElementFilter.methodsIn(elements.
                    getAllMembers(typeElement))) {


                // check the method is not overriden as final in a super class
                // if so - return immediately
                // we can do it this way, as ElementFilter.methodsIn() delivers all methods, even the inherited ones
                // an override of toString in the current class was already checked in the above block
                //
                // notes on the conditions:
                // 1. it has to have the name toString()
                // 2. no parameters must be present
                // 3. it has to be final
                // 4. and of course it must not be defined in this file
                if (TOSTRING.equals(method.getSimpleName().toString()) &&
                        method.getParameters().isEmpty() &&
                        method.getModifiers().contains(Modifier.FINAL) &&
                        !enclosedElements.contains(method)) {
                    // toString is already present in some super class
                    // exit gracefully please...
                }

                List<ExecutableElement> l =
                        methods.get(method.getSimpleName().toString());
                if (l == null) {
                    l = new ArrayList<>();
                    methods.put(method.getSimpleName().toString(), l);
                }
                l.add(method);
            }

            final List<VariableElement> fields = ElementFilter.fieldsIn(
                    elements.getAllMembers(typeElement));

            generators.add(new BuilderGenerator(context, fields));
            return generators;
        }
    }
}
