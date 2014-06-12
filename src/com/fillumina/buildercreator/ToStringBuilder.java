package com.fillumina.buildercreator;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;

/**
 * Abstract base class for toString() builder implementations
 * @author Simon
 */
public abstract class ToStringBuilder {


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

    public abstract BlockTree buildToString(WorkingCopy wc, String className, BuilderOptions options);
}
