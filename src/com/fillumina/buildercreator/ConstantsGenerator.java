package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Lookup;

public class ConstantsGenerator extends ExtendedCodeGenerator {
    private static final EnumSet<Modifier> PRIVATE_STATIC_FINAL_MODIFIERS =
            EnumSet.of(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC);

    private static final EnumSet<Modifier> PUBLIC_STATIC_FINAL_MODIFIERS =
            EnumSet.of(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC);

    private static final EnumSet<Modifier> PRIVATE_STATIC_MODIFIERS =
            EnumSet.of(Modifier.PRIVATE, Modifier.STATIC);

    public ConstantsGenerator(Lookup context, List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Constants...";
    }

    @Override
    protected void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields) {

        TypeElement typeClassElement = (TypeElement) wc.getTrees().getElement(path);
        if (typeClassElement != null) {
            TreeMaker make = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<>(classTree.getMembers());

            removeExistingConstants(members);

            int index = getIndexPosition(members);

            addConstants(fields, make, members, index);

            ClassTree newClassTree = make.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }

    @Override
    protected boolean filterOutField(VariableElement element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    private static void removeExistingConstants(List<Tree> members) {
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

    private static int getIndexPosition(List<Tree> members) {
        int index = 0;
        boolean implicitConstructor = false;
        if (members.size() > 0) {

            final Tree elem0 = members.get(0);
            // the first (hidden) element might be the default constructor
            if (elem0.getKind().equals(Tree.Kind.METHOD) &&
                    ((MethodTree)elem0).getName().toString().equals("<init>")) {
                index++;
                implicitConstructor = true;
            }

            if (members.size() > index) {
                // leaves serialVersionUID as the first member if present
                final Tree elem1 = members.get(index);
                if (elem1.getKind().equals(Tree.Kind.VARIABLE)) {
                    final VariableTree firstElem = (VariableTree) elem1;
                    if (firstElem.getKind().equals(Tree.Kind.VARIABLE) &&
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
                    if (elem2.getKind().equals(Tree.Kind.VARIABLE)) {
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

    private static void addConstants(List<VariableElement> elements,
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
}
