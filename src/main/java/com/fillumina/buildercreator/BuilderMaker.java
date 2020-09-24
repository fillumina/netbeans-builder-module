package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class BuilderMaker {
    private static final EnumSet<Modifier> PUBLIC_STATIC =
            EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);

    private final TreeMaker make;
    private final List<Tree> members;
    private final List<VariableElement> elements;
    private final TypeElement typeClassElement;
    private final String builderClassName;
    private final String builderMethodName;
    private final String buildMethodName;

    public BuilderMaker(TreeMaker make,
            List<Tree> members,
            List<VariableElement> elements,
            TypeElement typeClassElement,
            String builderClassName,
            String builderMethodName,
            String buildMethodName) {
        this.make = make;
        this.members = members;
        this.elements = elements;
        this.typeClassElement = typeClassElement;
        this.builderClassName = builderClassName;
        this.builderMethodName = builderMethodName;
        this.buildMethodName = buildMethodName;
    }

    int removeExistingBuilder(int index) {
        int counter = 0;
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.METHOD)) {
                MethodTree mt = (MethodTree) member;
                if (mt.getName().contentEquals(builderMethodName) &&
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

    MethodTree createBuildMethod() {
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

        return make.Method(modifiers,
                buildMethodName,
                returnType,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                body.toString(),
                null);
    }

    MethodTree createBuilderPrivateConstructor() {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PRIVATE));

        return make.Constructor(modifiers,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                "{}");
    }

    MethodTree createPrivateConstructor() {
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

    MethodTree createStaticBuilderCreatorMethod() {
        Set<Modifier> modifiers = PUBLIC_STATIC;
        List<AnnotationTree> annotations = new ArrayList<>();

        String builderName = typeClassElement.getSimpleName() + "." +
                builderClassName;

        ExpressionTree returnType = make.QualIdent(builderName);

        final String bodyText = "{return new " + builderName + "();}";

        return make.Method(
                make.Modifiers(modifiers, annotations), builderMethodName,
                returnType,
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                bodyText,
                null);
    }

    ClassTree createStaticInnerBuilderClass(final List<Tree> builderMembers) {
        List<AnnotationTree> annotations = new ArrayList<>();

        ClassTree clazz = make.Class(
                make.Modifiers(PUBLIC_STATIC, annotations),
                builderClassName,
                Collections.<TypeParameterTree>emptyList(),
                null,
                Collections.<Tree>emptyList(),
                builderMembers);

        return clazz;
    }

}
