package com.fillumina.buildercreator;

import javax.lang.model.element.Element;

/**
 * A class to transfer options. This is just a transporter and used to reduce amount of parameters.
 * @author cperv
 * @since 0.4.0
 */
public class BuilderOptions {
    private boolean useArrayToString = false;
    private boolean chainAppends = false;
    private boolean addOverride = true;
    private Iterable<? extends Element> elements = null;
//    private ToStringBuilderType builderType = null;
    private int positionOfMethod;

    public BuilderOptions(final Iterable<? extends Element> elements, boolean useArrayToString,
        boolean chainAppends, boolean addOverride, final Object type, int positionOfMethod) {
        this.addOverride = addOverride;
        this.chainAppends = chainAppends;
        this.useArrayToString = useArrayToString;
        this.elements = elements;
//        this.builderType = type;
        this.positionOfMethod = positionOfMethod;
    }

    /**
     * Indicates whether to add {@code @Override}:
     * @return {@code true} if to add the override annotation
     */
    public boolean isAddOverride() {
        return addOverride;
    }

    /**
     * Indicates whether to use chain "appends" statements.
     * @return {@code true} if "append" should be chained
     */
    public boolean isChainAppends() {
        return chainAppends;
    }

    /**
     * Gets the elements to add to the toString method.
     * @return the elements to consider in toString()
     */
    public Iterable<? extends Element> getElements() {
        return elements;
    }

    /**
     * Indicates whether to use native array toString or Arrays.toString();
     * @return {@code true} if to use Arrays.toString();
     */
    public boolean isUseArrayToString() {
        return useArrayToString;
    }

//    /**
//     * Gets the builder type to use.
//     * @return the type builder to use
//     */
//    public ToStringBuilderType getBuilderType() {
//        return builderType;
//    }

    /**
     * Gets the position where to add the method.
     * @return the position to add the method in
     */
    public int getPositionOfMethod() {
        return positionOfMethod;
    }

}
