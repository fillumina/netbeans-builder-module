package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
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

    static MethodTree createBuildMethod(TreeMaker make,
            TypeElement typeClassElement,
            List<VariableElement> elements) {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PUBLIC));

        final StringBuilder body = new StringBuilder();
        body.append("{\nreturn new ").append(typeClassElement.toString()).append("(");

        List<VariableTree> params = new ArrayList<>();
        boolean first = true;
        for (VariableElement element : elements) {
            final String varName = element.getSimpleName().toString();

            if (first) {
                first = false;
            } else {
                body.append(", ");
            }

            body.append(varName);
        }

        body.append(");\n}");

        ExpressionTree returnType = make.QualIdent(typeClassElement.toString());

        return make.Method(modifiers,
                "build",
                returnType,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                body.toString(),
                null);
    }

    static MethodTree createBuilderPrivateConstructor(String builderClassName,
            TreeMaker make) {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PRIVATE));

        return make.Constructor(modifiers,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                "{}");
    }

    static MethodTree createPrivateConstructor(String builderClassName,
            TreeMaker make,
            TypeElement typeClassElement,
            List<VariableElement> elements) {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PRIVATE));

        final StringBuilder body = new StringBuilder();
        body.append("{\n");

        ModifiersTree finalModifier = make.Modifiers(EnumSet.of(Modifier.FINAL));

        List<VariableTree> params = new ArrayList<>();
        for (VariableElement element : elements) {
            final String varName = element.getSimpleName().toString();
            Tree type = make.Type(element.asType());
            params.add(make.Variable(finalModifier, varName, type, null));

            body.append("this.")
                    .append(varName)
                    .append(" = ")
                    .append(varName)
                    .append(";\n");
        }

        body.append("}");

        return make.Constructor(modifiers,
                Collections.<TypeParameterTree>emptyList(),
                params,
                Collections.<ExpressionTree>emptyList(),
                body.toString());
    }

    static MethodTree createStaticBuilderCreatorMethod(String builderClassName,
            TreeMaker make,
            TypeElement typeClassElement) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        String builderName = typeClassElement.getSimpleName() + "." +
                builderClassName;

        ExpressionTree returnType = make.QualIdent(builderName);

        final String bodyText = "{return new " + builderName + "();}";

        return make.Method(
                make.Modifiers(modifiers, annotations),
                "buider",
                returnType,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                bodyText,
                null);
    }


    static ClassTree createStaticInnerBuilderClass(String builderClassName,
            TreeMaker make,
            TypeElement typeClassElement,
            List<Tree> members) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        ClassTree clazz = make.Class(
                make.Modifiers(modifiers, annotations),
                builderClassName,
                Collections.<TypeParameterTree>emptyList(),
                null,
                Collections.<Tree>emptyList(),
                members);

        return clazz;
    }

    static void addFluentSetterMethods(List<VariableElement> elements,
            TreeMaker make,
            String className,
            List<Tree> members,
            int index) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        List<AnnotationTree> annotations = new ArrayList<>();
//            AnnotationTree newAnnotation = maker.Annotation(
//                    maker.Identifier("Override"),
//                    Collections.<ExpressionTree>emptyList());
//            annotations.add(newAnnotation);

        for (VariableElement element : elements) {
            VariableTree parameter =
                    make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                            Collections.<AnnotationTree>emptyList()),
                            "value",
                            make.Identifier(PackageHelper.removePackages(element)),
                            null);

            ExpressionTree returnType = make.QualIdent(className);

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

            members.add(index++, method);
        }
    }

    static void addFields(List<VariableElement> elements,
            TreeMaker make,
            String className,
            List<Tree> members) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        List<AnnotationTree> annotations = new ArrayList<>();
//            AnnotationTree newAnnotation = maker.Annotation(
//                    maker.Identifier("Override"),
//                    Collections.<ExpressionTree>emptyList());
//            annotations.add(newAnnotation);

        for (VariableElement element : elements) {
            VariableTree field =
                    make.Variable(make.Modifiers(
                            EnumSet.of(Modifier.PRIVATE),
                            Collections.<AnnotationTree>emptyList()),
                            element.getSimpleName().toString(),
                            make.Identifier(PackageHelper.removePackages(element)),
                            null);

            members.add(field);
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
