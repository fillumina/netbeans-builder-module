package com.fillumina.buildercreator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class FluentSetterCodeGenerator {
    
    @SuppressWarnings("unchecked")    
    static void generateCode(WorkingCopy wc,
            TreePath path,
            int position,
            List<VariableElement> fields,
            boolean useWithPrefix) {

        TypeElement typeClassElement = (TypeElement) wc.getTrees().getElement(path);
        if (typeClassElement != null) {
            int index = position;

            TreeMaker make = wc.getTreeMaker();
            ClassTree classTree = (ClassTree) path.getLeaf();
            List<Tree> members = new ArrayList<>(classTree.getMembers());
            String className = typeClassElement.toString();

            FluentSettersMaker fluentSettersMaker = new FluentSettersMaker(
                    make, members, fields, className, useWithPrefix);

            index = fluentSettersMaker.removeExistingFluentSetters(index);

            fluentSettersMaker.addFluentSetters(index);

            ClassTree newClassTree = make.Class(classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    (List<ExpressionTree>) classTree.getImplementsClause(),
                    members);

            wc.rewrite(classTree, newClassTree);
        }
    }
    
}
