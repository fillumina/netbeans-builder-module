package com.fillumina.buildercreator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Lookup;

public class FluentSetterGenerator extends ExtendedCodeGenerator {

    public FluentSetterGenerator(Lookup context, List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Fluent setters...";
    }

    @Override
    protected void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields) {

        TypeElement typeClassElement = (TypeElement) wc.getTrees().getElement(path);
        if (typeClassElement != null) {
            int index = position;

            TreeMaker make = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<>(classTree.getMembers());

            index = SourceHelper
                    .removeExistingFluentSetters(members, index, fields);

            SourceHelper.addFluentSetters(fields,
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

    @Override
    protected boolean filterField(VariableElement element) {
        return element.getModifiers().contains(Modifier.STATIC) ||
                element.getModifiers().contains(Modifier.FINAL);
    }
}
