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
    public static final String BUILDER_NAME = "Builder";

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

            int position = SourceHelper.removeExistingBuilder(
                    typeClassElement.getSimpleName().toString(),
                    BUILDER_NAME,
                    members,
                    fields,
                    index);

            if (position > members.size()) {
                position = members.size();
            }

            members.add(position,
                    SourceHelper.createPrivateConstructor(BUILDER_NAME,
                            make,
                            typeClassElement,
                            fields));

            members.add(position,
                    SourceHelper.createStaticBuilderCreatorMethod(
                            BUILDER_NAME, make, typeClassElement));

            List<Tree> builderMembers = new ArrayList<>();
            SourceHelper.addFields(fields,
                    make, BUILDER_NAME, builderMembers);

            builderMembers.add(
                    SourceHelper.createBuilderPrivateConstructor(BUILDER_NAME,make));

            SourceHelper.addFluentSetters(fields,
                    make, BUILDER_NAME, builderMembers, builderMembers.size());

            builderMembers.add(
                    SourceHelper.createBuildMethod(make, typeClassElement, fields));

            ClassTree clazz =
                    SourceHelper.createStaticInnerBuilderClass(BUILDER_NAME, make,
                            typeClassElement, builderMembers);
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
