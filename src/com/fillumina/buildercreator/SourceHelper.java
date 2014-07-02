package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final String BUILDER_NAME = "builder";
    private static final String BUILD_NAME = "build";

    private static final List<Modifier> PRIVATE_STATIC_FINAL_MODIFIERS =
            Arrays.asList(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC);

    private static final List<Modifier> PUBLIC_STATIC_FINAL_MODIFIERS =
            Arrays.asList(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC);

    private static final List<Modifier> PRIVATE_STATIC_MODIFIERS =
            Arrays.asList(Modifier.PRIVATE, Modifier.STATIC);

    static MethodTree createBuildMethod(TreeMaker make,
            TypeElement typeClassElement,
            List<VariableElement> elements) {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PUBLIC));

        final StringBuilder body = new StringBuilder();
        body.append("{\nreturn new ").append(typeClassElement.toString()).append("(");

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

        return make.Method(modifiers, BUILD_NAME,
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
                make.Modifiers(modifiers, annotations), BUILDER_NAME,
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

    private static final long serialVersionUID = 1L;

    static int getIndexPosition(List<Tree> members) {
        int index = 0;
        boolean implicitConstructor = false;
        if (members.size() > 0) {

            final Tree elem0 = members.get(0);
            // the first (hidden) element might be the default constructor
            if (elem0.getKind().equals(Kind.METHOD) &&
                    ((MethodTree)elem0).getName().toString().equals("<init>")) {
                index++;
                implicitConstructor = true;
            }

            if (members.size() > index) {
                // leaves serialVersionUID as the first member if present
                final Tree elem1 = members.get(index);
                if (elem1.getKind().equals(Kind.VARIABLE)) {
                    final VariableTree firstElem = (VariableTree) elem1;
                    if (firstElem.getKind().equals(Kind.VARIABLE) &&
                            firstElem.getName().contentEquals("serialVersionUID") &&
                            firstElem.getModifiers().getFlags().containsAll(
                                PRIVATE_STATIC_FINAL_MODIFIERS) &&
                            "long".equals(firstElem.getType().toString()) ) {
                        index++;
                    }
                }

                if (members.size() > index) {
                    // leaves a logger as the second member if present
                    final Tree elem2 = members.get(index);
                    if (elem2.getKind().equals(Kind.VARIABLE)) {
                        final VariableTree secondElem = (VariableTree) elem2;
                        final String secondElemName = secondElem.getName().toString();
                        if (!secondElemName.startsWith("_") &&
                                secondElemName.toLowerCase().contains("log") &&
                                secondElem.getModifiers().getFlags().containsAll(
                                    PRIVATE_STATIC_MODIFIERS)) {
                            index++;
                        }
                    }
                }
            }
        }
        return index == 1 && implicitConstructor ? 0 : index ;
    }

    static void addConstants(List<VariableElement> elements,
            TreeMaker make,
            List<Tree> members,
            int index) {
        int position = index;
        for (VariableElement element : elements) {
            final String name = element.getSimpleName().toString();
            final ExpressionTree init = make.QualIdent("\"" + name + "\"");
            final VariableTree variable = make.Variable(
                    make.Modifiers(new HashSet<>(Arrays.asList(
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)),
                    Collections.<AnnotationTree>emptyList()),
                    createConstantName(name),
                    make.Identifier("String"),
                    init);

            members.add(position++, variable);
        }
    }

    private static String createConstantName(final String fieldName) {
        final StringBuilder buf = new StringBuilder("_");
        final char[] cname = fieldName.toCharArray();
        char c;
        for (int i=0; i<cname.length; i++) {
            c = cname[i];
            if (Character.isUpperCase(c) &&
                    i>0 &&
                    (!Character.isUpperCase(cname[i-1]) ||
                    i<cname.length - 1 && !Character.isUpperCase(cname[i+1])) ) {
                buf.append('_');
            }
            buf.append(Character.toUpperCase(c));
        }
        return buf.toString();
    }

    static void addFluentSetters(List<VariableElement> elements,
            TreeMaker make,
            String className,
            List<Tree> members,
            int index) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        int position = index - 1;
        for (VariableElement element : elements) {
            VariableTree parameter =
                    make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                            Collections.<AnnotationTree>emptyList()),
                            "value",
                            make.Identifier(toStringWithoutPackages(element)),
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

            position = Math.min(position + 1, members.size());
            members.add(position, method);
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

    static void addFields(List<VariableElement> elements,
            TreeMaker make,
            String className,
            List<Tree> members) {
        for (VariableElement element : elements) {
            VariableTree field =
                    make.Variable(make.Modifiers(
                            EnumSet.of(Modifier.PRIVATE),
                            Collections.<AnnotationTree>emptyList()),
                            element.getSimpleName().toString(),
                            make.Identifier(toStringWithoutPackages(element)),
                            null);

            members.add(field);
        }
    }

    static String toStringWithoutPackages(VariableElement element) {
        return PackageHelper.removePackagesFromGenericsType(
                element.asType().toString());
    }

    /**
     * Search up the hierarchy of elements for one of the given kind.
     *
     * @param kind the element's kind to search for
     * @param path the starting element
     * @return {@code null} if no element was found.
     */
    static TreePath getParentElementOfKind(Tree.Kind kind, TreePath path) {
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

    static int removeExistingFluentSetters(List<Tree> members,
            int index,
            Iterable<? extends Element> elements) {
        int counter = 0;
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
                        if (index > counter) {
                            index--;
                        }
                        break;
                    }
                }
            }
            counter++;
        }
        return index;
    }

    static void removeExistingConstants(List<Tree> members) {
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.VARIABLE)) {
                VariableTree field = (VariableTree) member;
                final Set<Modifier> modifiers = field.getModifiers().getFlags();
                final String name = field.getName().toString();
                if (name.startsWith("_") &&
                        modifiers.containsAll(PUBLIC_STATIC_FINAL_MODIFIERS) &&
                        field.getType().toString().contains("String")) {
                    treeIt.remove();
                }
            }
        }
    }

    static int removeExistingBuilder(
            String typeClassName,
            String builderClassName,
            List<Tree> members,
            List<? extends Element> elements,
            int index) {
        int counter = 0;
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.METHOD)) {
                MethodTree mt = (MethodTree) member;
                if (mt.getName().contentEquals(BUILDER_NAME) &&
                        mt.getParameters().isEmpty() &&
                        mt.getReturnType() != null) {
                    treeIt.remove();
                    if (index > counter) {
                        index--;
                    }

                } else if (mt.getName().contentEquals("<init>") &&
                        mt.getModifiers().getFlags().contains(Modifier.PRIVATE) &&
                        mt.getReturnType() == null) {
                    treeIt.remove();
                    if (index > counter) {
                        index--;
                    }
                }

            } else if (member.getKind().equals(Tree.Kind.CLASS)) {
                ClassTree ct = (ClassTree) member;
                if (ct.getSimpleName().contentEquals(builderClassName)) {
                    treeIt.remove();
                    if (index > counter) {
                        index--;
                    }
                }
            }
            counter++;
        }
        return index;
    }

}
