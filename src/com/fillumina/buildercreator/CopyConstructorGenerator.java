package com.fillumina.buildercreator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Lookup;

public class CopyConstructorGenerator extends ExtendedCodeGenerator {

    public CopyConstructorGenerator(Lookup context, 
            List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog.
     */
    @Override
    public String getDisplayName() {
        return "Copy Constructor...";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void generateCode(WorkingCopy wc,
            TreePath path,
            int index,
            List<VariableElement> fields) {

        TypeElement typeClassElement = (TypeElement)
                wc.getTrees().getElement(path);

        if (typeClassElement != null) {
            TreeMaker make = wc.getTreeMaker();
            
            ClassTree classTree = (ClassTree) path.getLeaf();
            List<Tree> members = new ArrayList<>(classTree.getMembers());

            int position = members.size();
            
            members.add(position, createCopyConstructor(make, classTree));

            ClassTree newClassTree = make.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }

    MethodTree createCopyConstructor(TreeMaker make, ClassTree classTree) {
        ModifiersTree modifiers = make.Modifiers(EnumSet.of(Modifier.PUBLIC));

        final StringBuilder body = new StringBuilder();
        body.append("{\n");

        ModifiersTree finalModifier = make.Modifiers(Collections.emptySet());

        List<VariableTree> params = new ArrayList<>();
        Name className = classTree.getSimpleName();
        Tree vartype = make.Type(new StringBuilder(className).toString());
        params.add(make.Variable(finalModifier, "copy", vartype, null));
        
        for (VariableElement element : getFields()) {
            final String varName = element.getSimpleName().toString();
            body.append("this.")
                    .append(varName)
                    .append(" = copy.")
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
    
    
    @Override
    protected boolean filterOutField(VariableElement element) {
        return element.getModifiers().contains(Modifier.STATIC) ||
                    (element.getModifiers().contains(Modifier.FINAL) &&
                    element.getConstantValue() != null);
    }
}
