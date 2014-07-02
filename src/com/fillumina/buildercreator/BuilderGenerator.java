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

public class BuilderGenerator extends ExtendedCodeGenerator {
    private static final String BUILDER_CLASS_NAME = "Builder";
    private static final String BUILDER_METHOD_NAME = "builder";
    private static final String BUILD_METHOD_NAME = "build";

    public BuilderGenerator(Lookup context, List<VariableElement> fields) {
        super(context, fields);
    }

    /**
     * The name which will be inserted inside Insert Code dialog.
     */
    @Override
    public String getDisplayName() {
        return "Builder...";
    }

    @Override
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

            BuilderMaker builderMaker =
                new BuilderMaker(make, members, fields, typeClassElement,
                    BUILDER_CLASS_NAME, BUILDER_METHOD_NAME, BUILD_METHOD_NAME);

            int position = builderMaker.removeExistingBuilder(index);

            if (position > members.size()) {
                position = members.size();
            }

            members.add(position, builderMaker.createPrivateConstructor());

            members.add(position,
                    builderMaker.createStaticBuilderCreatorMethod());

            List<Tree> builderMembers = new ArrayList<>();

            FluentSettersMaker fluentSettersMaker =
                new FluentSettersMaker(make, builderMembers, fields,
                        BUILDER_CLASS_NAME);

            fluentSettersMaker.addFields();

            builderMembers.add(builderMaker.createBuilderPrivateConstructor());

            fluentSettersMaker.addFluentSetters(builderMembers.size());

            builderMembers.add(builderMaker.createBuildMethod());

            ClassTree clazz = builderMaker.createStaticInnerBuilderClass(builderMembers);
            members.add(position, clazz);

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
        return element.getModifiers().contains(Modifier.STATIC) ||
                    (element.getModifiers().contains(Modifier.FINAL) &&
                    element.getConstantValue() != null);
    }
}
