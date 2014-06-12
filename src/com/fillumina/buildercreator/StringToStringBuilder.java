package com.fillumina.buildercreator;

import com.sun.source.tree.BlockTree;
import java.util.Collections;
import javax.lang.model.element.Element;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;

/**
 *
 * @author Simon
 */
class StringToStringBuilder extends ToStringBuilder {

    public BlockTree buildToString(WorkingCopy wc, String className, BuilderOptions options) {
        TreeMaker make = wc.getTreeMaker();
        StringBuilder sb = new StringBuilder();
        sb.append("\"" + className + " [\" + ");
        boolean first = true;
        for (Element element : options.getElements()) {
            if (!first) {
                sb.append(" + \" \" + ");
            }
            if (options.isUseArrayToString() && isArray(element)) {
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
}
