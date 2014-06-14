package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

    public static final String TO_STRING = "toString";

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
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Generate fluent setters...";
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

                    final BuilderOptions options =
                            new BuilderOptions(fields, idx);

                    generateFluentSetters(wc, path, options);
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
            final BuilderOptions options) {

        assert path.getLeaf().getKind() == Tree.Kind.CLASS;

        TypeElement typeClassElement = (TypeElement) wc.getTrees().getElement(path);
        if (typeClassElement != null) {
            int index = options.getPositionOfMethod();

            TreeMaker make = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<>(classTree.getMembers());
            final List<VariableElement> elements = options.getElements();
            Collections.reverse(elements);

            index = removeExistingMethods(members, index, elements);

            Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
            List<AnnotationTree> annotations = new ArrayList<>();
//            AnnotationTree newAnnotation = maker.Annotation(
//                    maker.Identifier("Override"),
//                    Collections.<ExpressionTree>emptyList());
//            annotations.add(newAnnotation);


            for (VariableElement element : elements) {
                if (element.getModifiers().contains(Modifier.STATIC) ||
                        element.getModifiers().contains(Modifier.FINAL)) {
                    continue;
                }

                VariableTree parameter =
                        make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                        Collections.<AnnotationTree>emptyList()),
                        "value",
                        make.Identifier(removePackages(element)),
                        null);

                ExpressionTree returnType = make.QualIdent(typeClassElement);

                final String bodyText = createBody(element);

                MethodTree method = make.Method(
                        make.Modifiers(modifiers, annotations),
                        element.getSimpleName(),
                        returnType,
                        Collections.<TypeParameterTree>emptyList(),
                        Collections.<VariableTree>singletonList(parameter),
                        Collections.<ExpressionTree>emptyList(),
                        bodyText,
                        null);

                members.add(index, method);
            }

            ClassTree newClassTree = make.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }

    private static String removePackages(VariableElement element) {
        final String fullName = element.asType().toString();
        final List<String> list = new ArrayList<>();
        int idx = 0, counter = 0;
        for (char c : fullName.toCharArray()) {
            switch (c) {
                case ',':
                case '<':
                case '>':
                    list.add(fullName.substring(idx, counter));
                    list.add(String.valueOf(c));
                    idx = counter + 1;
                    break;
            }
            counter++;
        }
        if (list.isEmpty()) {
            return removePackage(fullName);
        }
        StringBuilder buf = new StringBuilder();
        for (String s : list) {
            if ("<>,".contains(s)) {
                buf.append(s);
            } else {
                buf.append(removePackage(s));
            }
        }
        return buf.toString();
    }

    private static String removePackage(String fullname) {
        int lastIndexOfPoint = fullname.lastIndexOf('.');
        if (lastIndexOfPoint == -1) {
            return fullname;
        }
        return fullname.substring(lastIndexOfPoint + 1, fullname.length());
    }

    private String createBody(Element element) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\nthis.")
                .append(element.getSimpleName())
                .append(" = value;\n")
                .append("return this;\n}");
        return sb.toString();
    }

    private int removeExistingMethods(List<Tree> members,
            int index,
            Iterable<? extends Element> elements) {
        //
        // removes an existing toString() method
        //
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.METHOD)) {
                MethodTree mt = (MethodTree) member;
                for (Element element : elements) {
                    if (mt.getName().contentEquals(element.getSimpleName()) &&
                            mt.getParameters().size() == 1 &&
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
        }
        return index;
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
