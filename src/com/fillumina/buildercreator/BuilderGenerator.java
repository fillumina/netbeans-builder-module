package com.fillumina.buildercreator;

import com.fillumina.buildercreator.ElementNode.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.editor.GuardedDocument;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class BuilderGenerator implements CodeGenerator {
    public static final String TOSTRING = "toString";

    JTextComponent textComp;
    EvaluationContainer  container;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private BuilderGenerator(Lookup context, EvaluationContainer  container) { // Good practice is not to save Lookup outside ctor
        textComp = context.lookup(JTextComponent.class);
        this.container = container;
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Builder Generator";
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    @Override
    public void invoke() {
        try {
            Document doc = textComp.getDocument();
            JavaSource javaSource = JavaSource.forDocument(doc);

            CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>() {

                @Override
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    CompilationUnitTree compUnit = workingCopy.getCompilationUnit();
                    TreeMaker make = workingCopy.getTreeMaker();

                    for (Tree typeDecl : compUnit.getTypeDecls()) {
                        //createMethod(typeDecl, make, workingCopy);
                        generate(workingCopy);


                    }
                }

                private void generate(WorkingCopy copy) throws IOException {
                    copy.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    final int caretOffset = textComp.getCaretPosition();

                    TreePath path = copy.getTreeUtilities().pathFor(caretOffset);
                    path = BuilderGenerator.getPathElementOfKind(Tree.Kind.CLASS, path);
                    int idx = findClassMemberIndex(copy, (ClassTree) path.getLeaf(), caretOffset);
                    ArrayList<Element> elements = new ArrayList<Element>();

                    getSelectedHandles(container.fDescription, elements, copy);

                    final BuilderOptions options = new BuilderOptions(elements, true, true,
                        true, null, idx);

                    generateToString(copy, path, options);
                }

                private void getSelectedHandles(ElementNode.Description description,
                         List<Element> elements, CompilationInfo ci ) {
                     //#143049
                     if (description == null) {
                         return;
                     }
                     List<ElementNode.Description> subs = description.getSubs();
                     if (subs == null) {
                         return;
                     }
                     for (ElementNode.Description d : subs) {
//                         if (d.isSelectable() && d.isSelected()) {
                             elements.add(d.getElementHandle().resolve(ci));
//                         } else {
//                             getSelectedHandles(d, elements, ci);
//                         }
                     }
                 }

                private void createMethod(Tree typeDecl, TreeMaker make,
                        WorkingCopy workingCopy) {
                    if (Tree.Kind.CLASS == typeDecl.getKind()) {
                        ClassTree clazz = (ClassTree) typeDecl;

                        ModifiersTree methodModifiers =
                                make.Modifiers(
                                        Collections.<Modifier>singleton(Modifier.PUBLIC),
                                        Collections.<AnnotationTree>emptyList());

                        VariableTree parameter =
                                make.Variable(
                                        make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                                                Collections.<AnnotationTree>emptyList()),
                                        "arg0",
                                        make.Identifier("Object"),
                                        null);

                        TypeElement element = workingCopy.getElements().getTypeElement("java.io.IOException");

                        ExpressionTree throwsClause = make.QualIdent(element);

                        MethodTree newMethod =
                                make.Method(methodModifiers,
                                        "writeExternal",
                                        make.PrimitiveType(TypeKind.VOID),
                                        Collections.<TypeParameterTree>emptyList(),
                                        Collections.singletonList(parameter),
                                        Collections.<ExpressionTree>singletonList(throwsClause),
                                        "{ throw new UnsupportedOperationException(\"Not supported yet.\") }",
                                        null);

                        ClassTree modifiedClazz = make.addClassMember(clazz, newMethod);
                        workingCopy.rewrite(clazz, modifiedClazz);
                    }
                }

                @Override
                public void cancel() {
                }
            };

            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void generateToString(WorkingCopy wc, TreePath path, final BuilderOptions options) {

        assert path.getLeaf().getKind() == Tree.Kind.CLASS;
        TypeElement te = (TypeElement) wc.getTrees().getElement(path);
        if (te != null) {
        int index = options.getPositionOfMethod();

            TreeMaker make = wc.getTreeMaker();
            ClassTree clazz = (ClassTree) path.getLeaf();

            List<Tree> members = new ArrayList<Tree>(clazz.getMembers());
            // use an iterator to prevent concurrent modification
            for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
                Tree member = treeIt.next();
                if (member.getKind().equals(Tree.Kind.METHOD)) {
                    MethodTree mt = (MethodTree) member;
                    // this may looks strange, but I've seen code with methods like:
                    // public String toString(Object o) {}
                    // and I think we shouldn't remove them :)
                    // so we should ensure to get right toString() method, means
                    // a return value and no parameters
                    if (mt.getName().contentEquals(TOSTRING) && mt.getParameters().isEmpty() &&
                            mt.getReturnType() != null && mt.getReturnType().getKind() == Tree.Kind.IDENTIFIER) {
                        treeIt.remove();
                        // decrease the index to use, as we else will get an ArrayIndexOutOfBounds (if added at the end of a class)
                        index--;
                        break;
                    }
                }
            }

            ToStringBuilder tsb = new StringToStringBuilder();


            Set<Modifier> mods = EnumSet.of(Modifier.PUBLIC);
            List<AnnotationTree> annotations = new ArrayList<AnnotationTree>();
            if (options.isAddOverride()) {
                AnnotationTree newAnnotation = make.Annotation(
                        make.Identifier("Override"),
                        Collections.<ExpressionTree>emptyList());
                annotations.add(newAnnotation);
            }
            TypeElement element = wc.getElements().getTypeElement("java.lang.String");
            ExpressionTree returnType = make.QualIdent(element);

            MethodTree method = make.Method(make.Modifiers(mods, annotations), TOSTRING, returnType, Collections.<TypeParameterTree>emptyList(),
                    Collections.<VariableTree>emptyList(), Collections.<ExpressionTree>emptyList(),
                    tsb.buildToString(wc, clazz.getSimpleName().toString(), options), null);

            members.add(index, method);

            ClassTree nue = make.Class(clazz.getModifiers(), clazz.getSimpleName(), clazz.getTypeParameters(), clazz.getExtendsClause(),
                    (List<ExpressionTree>) clazz.getImplementsClause(), members);
            wc.rewrite(clazz, nue);
        }
    }

    private int findClassMemberIndex(WorkingCopy wc, ClassTree clazz, int offset) {

        int index = 0;
        SourcePositions sp = wc.getTrees().getSourcePositions();
        GuardedDocument gdoc = null;
        try {
            Document doc = wc.getDocument();
            if (doc != null && doc instanceof GuardedDocument) {
                gdoc = (GuardedDocument) doc;
            }
        } catch (IOException ioe) {
        }

        Tree lastMember = null;
        for (Tree tree : clazz.getMembers()) {
            if (offset <= sp.getStartPosition(wc.getCompilationUnit(), tree)) {
                if (gdoc == null) {
                    break;
                }
                int pos = (int) (lastMember != null ? sp.getEndPosition(wc.getCompilationUnit(), lastMember) : sp.getStartPosition(wc.getCompilationUnit(), clazz));
                pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                if (pos <= sp.getStartPosition(wc.getCompilationUnit(), tree)) {
                    break;
                }
            }
            index++;
            lastMember = tree;
        }
        return index;
    }

    private static TreePath getPathElementOfKind(Tree.Kind kind, TreePath path) {
        return getPathElementOfKind(EnumSet.of(kind), path);
    }

    private static TreePath getPathElementOfKind(EnumSet<Tree.Kind> kinds, TreePath path) {

        while (path != null) {
            if (kinds.contains(path.getLeaf().getKind())) {
                return path;
            }
            path = path.getParentPath();
        }
        return null;
    }

    @MimeRegistration(mimeType = "text/x-java",
            service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        private static final String ERROR = "<error>";

        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> ret = new ArrayList<CodeGenerator>();
            JTextComponent component = context.lookup(JTextComponent.class);

            EvaluationContainer evaluationContainer = new EvaluationContainer(component);

            CompilationController controller = context.lookup(CompilationController.class);
            TreePath path = context.lookup(TreePath.class);
            path = path != null ? getPathElementOfKind(Tree.Kind.CLASS, path) : null;
            if (component == null || controller == null || path == null) {
                return ret;
            }
            try {
                controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
            } catch (IOException ioe) {
                return ret;
            }
            evaluationContainer.setFileObject(controller.getFileObject());
            Elements elements = controller.getElements();
            TypeElement typeElement = (TypeElement) controller.getTrees().getElement(path);
            if (typeElement == null || !typeElement.getKind().isClass()) {
                return ret;
            }
            evaluationContainer.setClassElement(typeElement);

            // we create separate mappings here for a complete list and list of fields as else
            // we have to clone the element in the selector panel later
            final Map<Element, List<ElementNode.Description>> complete = new LinkedHashMap<Element, List<ElementNode.Description>>();
            final Map<Element, List<ElementNode.Description>> fieldsOnly = new LinkedHashMap<Element, List<ElementNode.Description>>();

            final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            Map<String, List<ExecutableElement>> methods = new HashMap<String, List<ExecutableElement>>();
            for (ExecutableElement method : ElementFilter.methodsIn(elements.getAllMembers(typeElement))) {

                boolean isEnclosedToString = false;
                if (enclosedElements.contains(method) && "toString".equals(method.getSimpleName().toString()) && method.getParameters().isEmpty()) {
                    evaluationContainer.setExistingToString(method);
                    isEnclosedToString = true;
                }

                if (!isEnclosedToString && method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID &&
                        !"clone".equals(method.getSimpleName().toString())) {

                    addDescription(method, complete);
                }

                // check the method is not overriden as final in a super class
                // if so - return immediately
                // we can do it this way, as ElementFilter.methodsIn() delivers all methods, even the inherited ones
                // an override of toString in the current class was already checked in the above block
                //
                // notes on the conditions:
                // 1. it has to have the name toString()
                // 2. no parameters must be present
                // 3. it has to be final
                // 4. and of course it must not be defined in this file
                if (TOSTRING.equals(method.getSimpleName().toString()) && method.getParameters().isEmpty() &&
                        method.getModifiers().contains(Modifier.FINAL) && !enclosedElements.contains(method)) {
                    evaluationContainer.setFinalSuper(controller.getTrees().getTree(method));

                    evaluationContainer.setSuperClass(controller.getElementUtilities().enclosingTypeElement(method));
                }

                List<ExecutableElement> l = methods.get(method.getSimpleName().toString());
                if (l == null) {
                    l = new ArrayList<ExecutableElement>();
                    methods.put(method.getSimpleName().toString(), l);
                }
                l.add(method);
            }

            for (final VariableElement variableElement :
                    ElementFilter.fieldsIn(elements.getAllMembers(typeElement))) {
                if (ERROR.contentEquals(variableElement.getSimpleName())) {
                    continue;
                }
                addDescription(variableElement, complete);
                addDescription(variableElement, fieldsOnly);

            }

            Description plainList = getPlainList(complete, typeElement);
            if(plainList != null) {
                evaluationContainer.setCompleteDescription(plainList);
            }

            plainList = getPlainList(fieldsOnly, typeElement);
            if(plainList != null) {
                evaluationContainer.setFieldsOnlyDescription(plainList);
            }

            ret.add(new BuilderGenerator(context, evaluationContainer));
            return ret;
        }

        private Description getPlainList(final Map<Element, List<ElementNode.Description>> mapToCreateFrom, final Element typeElement) {
            if (!mapToCreateFrom.isEmpty()) {
                final List<ElementNode.Description> descriptions = new ArrayList<ElementNode.Description>();
                // the map is not used any further, so we remove the typeElement's mapping
                // and add it after the loop to ensure, the typeElement's elements will
                // be on top of throws ereturn list later
                List<Description> head = mapToCreateFrom.remove(typeElement);
                for (final Map.Entry<Element, List<ElementNode.Description>> entry : mapToCreateFrom.entrySet()) {
                    descriptions.add(ElementNode.Description.create(entry.getKey(), entry.getValue(), false, false));
                }
                descriptions.add(ElementNode.Description.create(typeElement, head, false, false));

                Collections.reverse(descriptions);
                return ElementNode.Description.create(typeElement, descriptions, false, false);
            }
            return null;
        }

        private void addDescription(final Element element, final Map<Element, List<ElementNode.Description>> mapToAddTo) {
            final ElementNode.Description description = ElementNode.Description.create(element, null, true, false);
            List<ElementNode.Description> descriptions = mapToAddTo.get(element.getEnclosingElement());

            if (descriptions == null) {
                descriptions = new ArrayList<ElementNode.Description>();
                mapToAddTo.put(element.getEnclosingElement(), descriptions);
            }
            descriptions.add(description);
        }
    }


    /**
     * A class used to map all the evaluation results, so we do not need to pass all that results to the constructor.
     */
    // TODO: may we should refactor this class out
    // TODO: add documentation
    private static class EvaluationContainer {

        private JTextComponent component;
        private ElementNode.Description cDescription;
        private ElementNode.Description fDescription;

        public Description getfDescription() {
            return fDescription;
        }

        public void setFieldsOnlyDescription(Description fDescription) {
            this.fDescription = fDescription;
        }
        private Element existingToString;
        private MethodTree finalSuperImpl;
        private Element classElement;
        private TypeElement superClass;
        private FileObject fileObject;

        public FileObject getFileObject() {
            return fileObject;
        }

        public void setFileObject(FileObject fileObject) {
            this.fileObject = fileObject;
        }

        public TypeElement getSuperClass() {
            return superClass;
        }

        public void setSuperClass(TypeElement superClass) {
            this.superClass = superClass;
        }

        public Element getClassElement() {
            return classElement;
        }

        public void setClassElement(Element classElement) {
            this.classElement = classElement;
        }

        public MethodTree getFinalSuper() {
            return finalSuperImpl;
        }

        public void setFinalSuper(MethodTree finalSuper) {
            this.finalSuperImpl = finalSuper;
        }

        protected EvaluationContainer(JTextComponent component) {
            this.component = component;
        }

        public Element getExistingToString() {
            return existingToString;
        }

        public void setExistingToString(Element existingToString) {
            this.existingToString = existingToString;
        }

        public JTextComponent getComponent() {
            return component;
        }

        public Description getDescription() {
            return cDescription;
        }

        public void setCompleteDescription(Description description) {
            this.cDescription = description;
        }
    }

}
