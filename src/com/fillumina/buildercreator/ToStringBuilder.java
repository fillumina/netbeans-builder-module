package com.fillumina.buildercreator;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import java.util.Collections;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;

/**
 *
 * @author Simon
 */
class ToStringBuilder {

    public BlockTree buildToString(WorkingCopy wc, String className, BuilderOptions options) {
        TreeMaker make = wc.getTreeMaker();
        StringBuilder sb = new StringBuilder();
        sb.append("\"" + className + " [\" + ");
        boolean first = true;
        for (Element element : options.getElements()) {
            if (!first) {
                sb.append(" + \" \" + ");
            }
            if (isArray(element)) {
                addArrayImport(make, wc);
                sb.append("\"" + element.getSimpleName() + " \" + Arrays.toString(" + element.getSimpleName() + ")");
            } else {
                sb.append("\"" + element.getSimpleName() + " \" + " + element.getSimpleName() + (isMethod(element) ? "()" : "" ));
            }
            first = false;
        }
        if (first) {
            sb.append("\"\"");
        }
        sb.append(" + \"]\"");
        BlockTree body = make.Block(Collections.singletonList(make.Return(make.Identifier(sb.toString()))), false);
        return body;
    }

    /**
     * Indicates whether the passed element is a method.
     * @param element the element to check
     * @return {@code true} if {@code element} is of type method
     */
    public final boolean isMethod(Element element) {
        // do NOT try instanceof here - it's highly discouraged to so by the APIs
        return element.getKind() == ElementKind.METHOD;
    }

    /**
     * Indicates whether the passed element is an array type.
     * @param element the element to check
     * @return {@code true} if {@code element} is an array type
     */
    public final boolean isArray(final Element element) {
        return element.asType().getKind() == TypeKind.ARRAY;
    }

    /**
     * Adds an import statement for java.util.Arrays if necessary.
     * @param make the tree maker used to add the import
     * @param wc the working copy used to determine an whether an import is present or not
     */
    public final void addArrayImport(TreeMaker make, WorkingCopy wc) {
        for (ImportTree importTree : wc.getCompilationUnit().getImports()) {
            if (importTree.getQualifiedIdentifier().toString().equals("java.util.Arrays")) {
                return;
            }
        }
        ImportTree importStmt = make.Import(make.Identifier("java.util.Arrays"), false);
        CompilationUnitTree newCut = make.addCompUnitImport(wc.getCompilationUnit(), importStmt);
        wc.rewrite(wc.getCompilationUnit(), newCut);
    }

}
