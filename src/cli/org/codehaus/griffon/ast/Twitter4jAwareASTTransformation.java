/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.ast;

import griffon.plugins.twitter4j.DefaultTwitter4jProvider;
import griffon.plugins.twitter4j.Twitter4jAware;
import griffon.plugins.twitter4j.Twitter4jContributionHandler;
import griffon.plugins.twitter4j.Twitter4jProvider;
import lombok.core.handlers.Twitter4jAwareConstants;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codehaus.griffon.ast.GriffonASTUtils.*;

/**
 * Handles generation of code for the {@code @Twitter4jAware} annotation.
 * <p/>
 *
 * @author Andres Almiray
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class Twitter4jAwareASTTransformation extends AbstractASTTransformation implements Twitter4jAwareConstants {
    private static final Logger LOG = LoggerFactory.getLogger(Twitter4jAwareASTTransformation.class);
    private static final ClassNode TWITTER4j_CONTRIBUTION_HANDLER_CNODE = makeClassSafe(Twitter4jContributionHandler.class);
    private static final ClassNode TWITTER4j_AWARE_CNODE = makeClassSafe(Twitter4jAware.class);
    private static final ClassNode TWITTER4j_PROVIDER_CNODE = makeClassSafe(Twitter4jProvider.class);
    private static final ClassNode DEFAULT_TWITTER4j_PROVIDER_CNODE = makeClassSafe(DefaultTwitter4jProvider.class);

    private static final String[] DELEGATING_METHODS = new String[]{
        METHOD_WITH_TWITTER
    };

    static {
        Arrays.sort(DELEGATING_METHODS);
    }

    /**
     * Convenience method to see if an annotated node is {@code @Twitter4jAware}.
     *
     * @param node the node to check
     * @return true if the node is an event publisher
     */
    public static boolean hasTwitter4jAwareAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (TWITTER4j_AWARE_CNODE.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes  the ast nodes
     * @param source the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        checkNodesForAnnotationAndType(nodes[0], nodes[1]);
        addTwitter4jContributionIfNeeded(source, (ClassNode) nodes[1]);
    }

    public static void addTwitter4jContributionIfNeeded(SourceUnit source, ClassNode classNode) {
        if (needsTwitter4jContribution(classNode, source)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Injecting " + Twitter4jContributionHandler.class.getName() + " into " + classNode.getName());
            }
            apply(classNode);
        }
    }

    protected static boolean needsTwitter4jContribution(ClassNode declaringClass, SourceUnit sourceUnit) {
        boolean found1 = false, found2 = false, found3 = false;
        ClassNode consideredClass = declaringClass;
        while (consideredClass != null) {
            for (MethodNode method : consideredClass.getMethods()) {
                // just check length, MOP will match it up
                found1 = method.getName().equals(METHOD_WITH_TWITTER) && method.getParameters().length == 2;
                found2 = method.getName().equals(METHOD_SET_TWITTER4j_PROVIDER) && method.getParameters().length == 1;
                found3 = method.getName().equals(METHOD_GET_TWITTER4j_PROVIDER) && method.getParameters().length == 0;
                if (found1 && found2 && found3) {
                    return false;
                }
            }
            consideredClass = consideredClass.getSuperClass();
        }
        if (found1 || found2 || found3) {
            sourceUnit.getErrorCollector().addErrorAndContinue(
                new SimpleMessage("@Twitter4jAware cannot be processed on "
                    + declaringClass.getName()
                    + " because some but not all of methods from " + Twitter4jContributionHandler.class.getName() + " were declared in the current class or super classes.",
                    sourceUnit)
            );
            return false;
        }
        return true;
    }

    public static void apply(ClassNode declaringClass) {
        injectInterface(declaringClass, TWITTER4j_CONTRIBUTION_HANDLER_CNODE);

        // add field:
        // protected Twitter4jProvider this$twitter4jProvider = DefaultTwitter4jProvider.instance
        FieldNode providerField = declaringClass.addField(
            TWITTER4j_PROVIDER_FIELD_NAME,
            ACC_PRIVATE | ACC_SYNTHETIC,
            TWITTER4j_PROVIDER_CNODE,
            defaultTwitter4jProviderInstance());

        // add method:
        // Twitter4jProvider getTwitter4jProvider() {
        //     return this$twitter4jProvider
        // }
        injectMethod(declaringClass, new MethodNode(
            METHOD_GET_TWITTER4j_PROVIDER,
            ACC_PUBLIC,
            TWITTER4j_PROVIDER_CNODE,
            Parameter.EMPTY_ARRAY,
            NO_EXCEPTIONS,
            returns(field(providerField))
        ));

        // add method:
        // void setTwitter4jProvider(Twitter4jProvider provider) {
        //     this$twitter4jProvider = provider ?: DefaultTwitter4jProvider.instance
        // }
        injectMethod(declaringClass, new MethodNode(
            METHOD_SET_TWITTER4j_PROVIDER,
            ACC_PUBLIC,
            ClassHelper.VOID_TYPE,
            params(
                param(TWITTER4j_PROVIDER_CNODE, PROVIDER)),
            NO_EXCEPTIONS,
            block(
                ifs_no_return(
                    cmp(var(PROVIDER), ConstantExpression.NULL),
                    assigns(field(providerField), defaultTwitter4jProviderInstance()),
                    assigns(field(providerField), var(PROVIDER))
                )
            )
        ));

        for (MethodNode method : TWITTER4j_CONTRIBUTION_HANDLER_CNODE.getMethods()) {
            if (Arrays.binarySearch(DELEGATING_METHODS, method.getName()) < 0)
                continue;
            List<Expression> variables = new ArrayList<Expression>();
            Parameter[] parameters = new Parameter[method.getParameters().length];
            for (int i = 0; i < method.getParameters().length; i++) {
                Parameter p = method.getParameters()[i];
                parameters[i] = new Parameter(makeClassSafe(p.getType()), p.getName());
                parameters[i].getType().setGenericsTypes(p.getType().getGenericsTypes());
                variables.add(var(p.getName()));
            }
            ClassNode returnType = makeClassSafe(method.getReturnType());
            returnType.setGenericsTypes(method.getReturnType().getGenericsTypes());
            returnType.setGenericsPlaceHolder(method.getReturnType().isGenericsPlaceHolder());

            MethodNode newMethod = new MethodNode(
                method.getName(),
                ACC_PUBLIC,
                returnType,
                parameters,
                NO_EXCEPTIONS,
                returns(call(
                    field(providerField),
                    method.getName(),
                    args(variables)))
            );
            newMethod.setGenericsTypes(method.getGenericsTypes());
            injectMethod(declaringClass, newMethod);
        }
    }

    private static Expression defaultTwitter4jProviderInstance() {
        return call(DEFAULT_TWITTER4j_PROVIDER_CNODE, "getInstance", NO_ARGS);
    }
}
