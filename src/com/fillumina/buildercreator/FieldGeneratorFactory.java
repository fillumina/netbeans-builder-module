package com.fillumina.buildercreator;

import java.util.List;
import javax.lang.model.element.VariableElement;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Lookup;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
interface FieldGeneratorFactory {

    CodeGenerator create(Lookup context, List<VariableElement> fields);
}
