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
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.swing.text.Document;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.editor.GuardedDocument;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class SourceHelper {
    private static final String BUILDER_NAME = "Builder";

    static void addStaticBuilderCreatorMethod(TreeMaker make,
            TypeElement typeClassElement,
            List<Tree> members,
            int index) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        String builderName = typeClassElement.getSimpleName() + "." + BUILDER_NAME;

        ExpressionTree returnType = make.QualIdent(builderName);

        final String bodyText = "{return new " + builderName + "();}";

        MethodTree method = make.Method(
                make.Modifiers(modifiers, annotations),
                "buider",
                returnType,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                bodyText,
                null);

        members.add(index, method);
    }

    static ClassTree addStaticBuilderClass(TreeMaker make,
            TypeElement typeClassElement) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        ClassTree clazz = make.Class(
                make.Modifiers(modifiers, annotations),
                BUILDER_NAME,
                Collections.<TypeParameterTree>emptyList(),
                null,
                Collections.<Tree>emptyList(),
                Collections.<Tree>emptyList());

        return clazz;
    }

    static void addFluentSetterMethods(List<VariableElement> elements,
            TreeMaker make,
            TypeElement typeClassElement,
            List<Tree> members,
            int index) {
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
                            make.Identifier(PackageHelper.removePackages(element)),
                            null);

            ExpressionTree returnType = make.QualIdent(typeClassElement);

            final String bodyText = createFluentSetterMethodBody(element);

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
    }

    private static String createFluentSetterMethodBody(Element element) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\nthis.")
                .append(element.getSimpleName())
                .append(" = value;\n")
                .append("return this;\n}");
        return sb.toString();
    }

    /**
     * Search up the hierarchy of elements one of the given kind.
     *
     * @param kind the element's kind to search for
     * @param path the starting element
     * @return {@code null} if no element was found.
     */
    static TreePath getParentElementOfKind(Tree.Kind kind,
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

    /**
     * Find the index of the current class member.
     *
     * @param wc
     * @param classTree
     * @param offset
     * @return
     */
    static int findClassMemberIndex(WorkingCopy wc,
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

}
