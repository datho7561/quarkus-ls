/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.qute.jdt.internal.template.datamodel;

import static com.redhat.qute.jdt.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION;
import static com.redhat.qute.jdt.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME;
import static com.redhat.qute.jdt.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAMES;
import static com.redhat.qute.jdt.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;
import com.redhat.qute.jdt.QuteSupportForTemplate;
import com.redhat.qute.jdt.internal.resolver.ITypeResolver;
import com.redhat.qute.jdt.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.qute.jdt.template.datamodel.SearchContext;
import com.redhat.qute.jdt.utils.AnnotationUtils;
import com.redhat.qute.jdt.utils.JDTTypeUtils;

/**
 * @TemplateExtension annotation support.
 *
 *                    <code>
 * class Item {
 *
 * 		public final BigDecimal price;
 *
 * 		public Item(BigDecimal price) {
 * 			this.price = price;
 * 		}
 * }
 *
 * &#64;TemplateExtension
 * class MyExtensions {
 * 		static BigDecimal discountedPrice(Item item) {
 * 			return item.getPrice().multiply(new BigDecimal("0.9"));
 * 		}
 * }
 * </code>
 *
 *
 * @author Angelo ZERR
 *
 * @see https://quarkus.io/guides/qute-reference#template_extension_methods
 *
 */
public class TemplateExtensionAnnotationSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateExtensionAnnotationSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = { TEMPLATE_EXTENSION_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(IJavaElement javaElement, IAnnotation annotation, String annotationName,
			SearchContext context, IProgressMonitor monitor) throws JavaModelException {
		if (!(javaElement instanceof IAnnotatable)) {
			return;
		}
		IAnnotation templateExtension = AnnotationUtils.getAnnotation((IAnnotatable) javaElement,
				TEMPLATE_EXTENSION_ANNOTATION);
		if (templateExtension == null) {
			return;
		}
		if (javaElement instanceof IType) {
			IType type = (IType) javaElement;
			collectResolversForTemplateExtension(type, templateExtension,
					context.getDataModelProject().getValueResolvers(), monitor);
		} else if (javaElement instanceof IMethod) {
			IMethod method = (IMethod) javaElement;
			collectResolversForTemplateExtension(method, templateExtension,
					context.getDataModelProject().getValueResolvers(), monitor);
		}
	}

	private static void collectResolversForTemplateExtension(IType type, IAnnotation templateExtension,
			List<ValueResolverInfo> resolvers, IProgressMonitor monitor) {
		try {
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(type);
			IMethod[] methods = type.getMethods();
			int resolversLengthPreAdd = resolvers.size();
			for (IMethod method : methods) {
				if (isTemplateExtensionMethod(method)) {
					IAnnotation methodTemplateExtension = AnnotationUtils.getAnnotation(method,
							TEMPLATE_EXTENSION_ANNOTATION);
					collectResolversForTemplateExtension(method,
							methodTemplateExtension != null ? methodTemplateExtension : templateExtension, resolvers,
							typeResolver, ValueResolverKind.TemplateExtensionOnClass);
				}
			}
			if (resolversLengthPreAdd == resolvers.size()) {
				// Add a dummy ValueResolverInfo to indicate that this is a template extensions
				// class
				addDummyResolverForTemplateExtensionsClass(type, resolvers);
			}
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Error while getting methods of '" + type.getElementName() + "'.", e);
		}
	}

	/**
	 * Returns true if the given method <code>method</code> is a template extension
	 * method and false otherwise.
	 *
	 * A template extension method:
	 *
	 * <ul>
	 * <li>must not be private</li>
	 * <li>must be static,</li>
	 * <li>must not return void.</li>
	 * </ul>
	 *
	 * @param method the method to check.
	 * @return true if the given method <code>method</code> is a template extension
	 *         method and false otherwise.
	 */
	private static boolean isTemplateExtensionMethod(IMethod method) {
		try {
			return !method.isConstructor() /* && Flags.isPublic(method.getFlags()) */
					&& !JDTTypeUtils.isVoidReturnType(method);
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Error while getting method information of '" + method.getElementName() + "'.", e);
			return false;
		}
	}

	public static void collectResolversForTemplateExtension(IMethod method, IAnnotation templateExtension,
			List<ValueResolverInfo> resolvers, IProgressMonitor monitor) {
		if (isTemplateExtensionMethod(method)) {
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(method);
			collectResolversForTemplateExtension(method, templateExtension, resolvers, typeResolver,
					ValueResolverKind.TemplateExtensionOnMethod);
		}
	}

	private static void collectResolversForTemplateExtension(IMethod method, IAnnotation templateExtension,
			List<ValueResolverInfo> resolvers, ITypeResolver typeResolver, ValueResolverKind kind) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setSourceType(method.getDeclaringType().getFullyQualifiedName());
		resolver.setSignature(typeResolver.resolveMethodSignature(method));
		resolver.setKind(kind);
		resolver.setBinary(method.getDeclaringType().isBinary());
		try {
			String namespace = AnnotationUtils.getAnnotationMemberValue(templateExtension,
					TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE);			
			resolver.setNamespace(namespace);

			List<String> matchNames = getAnnotationMemberValueAsArray(templateExtension,
					TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAMES);
			if (matchNames != null) {
				resolver.setMatchNames(matchNames);
			} else {
				String matchName = AnnotationUtils.getAnnotationMemberValue(templateExtension,
						TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME);
				if (StringUtils.isNotEmpty(matchName)) {
					resolver.setMatchNames(Arrays.asList(matchName));
				}
			}
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE,
					"Error while getting annotation member value of '" + method.getElementName() + "'.", e);
		}
		if (!resolvers.contains(resolver)) {
			resolvers.add(resolver);
		}
	}

	private static void addDummyResolverForTemplateExtensionsClass(IType type, List<ValueResolverInfo> resolvers) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setSourceType(type.getFullyQualifiedName());
		resolver.setKind(ValueResolverKind.TemplateExtensionOnClass);
		resolver.setBinary(type.isBinary());
		// Needed to prevent NPE
		resolver.setSignature("");
		resolvers.add(resolver);
	}

	/**
	 * Returns the value array of the given member name of the given annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the value array of the given member name of the given annotation.
	 * @throws JavaModelException
	 */
	public static List<String> getAnnotationMemberValueAsArray(IAnnotation annotation, String memberName)
			throws JavaModelException {
		for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
			if (memberName.equals(pair.getMemberName())) {
				if (pair.getValue() instanceof Object[]) {
					Object[] values = (Object[]) pair.getValue();
					return Stream.of(values).filter(o -> o != null).map(o -> o.toString()).collect(Collectors.toList());
				}
				return null;
			}
		}
		return null;
	}
}
