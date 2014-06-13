package com.fillumina.buildercreator;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * A class to transfer options. This is just a transporter and used to reduce amount of parameters.
 * @author cperv
 * @since 0.4.0
 */
// TODO remove this
public class BuilderOptions {
    private final List<VariableElement> elements;
    private final int positionOfMethod;

    public BuilderOptions(final List<VariableElement> elements,
            int positionOfMethod) {
        this.elements = elements;
        this.positionOfMethod = positionOfMethod;
    }

    /**
     * Gets the elements to add to the toString method.
     * @return the elements to consider in toString()
     */
    public List<VariableElement> getElements() {
        return elements;
    }

    /**
     * Gets the position where to add the method.
     * @return the position to add the method in
     */
    public int getPositionOfMethod() {
        return positionOfMethod;
    }

}
