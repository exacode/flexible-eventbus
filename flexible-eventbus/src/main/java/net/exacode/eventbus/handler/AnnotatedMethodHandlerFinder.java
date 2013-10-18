/*
 * Copyright (C) 2007 The Guava Authors
 * Copyright (C) 2007 mendlik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.exacode.eventbus.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.exacode.eventbus.exception.ExceptionHandler;
import net.exacode.eventbus.exception.LoggingExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MethodHandlerFinder} for collecting all event handler methods that
 * are marked with an appropriate annotation.
 * 
 * @author Paweł Mendelski
 */
public class AnnotatedMethodHandlerFinder<A extends Annotation> implements
		MethodHandlerFinder {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Class<A> annotationType;

	private final ExceptionHandler exceptionHandler;

	public AnnotatedMethodHandlerFinder(Class<A> annotationType) {
		this(annotationType, new LoggingExceptionHandler());
	}

	public AnnotatedMethodHandlerFinder(Class<A> annotationType,
			ExceptionHandler exceptionHandler) {
		this.annotationType = annotationType;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public Map<Class<?>, Set<MethodHandler>> findHandlerMethods(Object listener) {
		Map<Class<?>, Set<MethodHandler>> methodsInListener = new HashMap<Class<?>, Set<MethodHandler>>();
		Class<?> clazz = listener.getClass();

		for (Method method : clazz.getMethods()) {
			A eventHandlerAnnotation = findMethodAnnotation(method,
					annotationType);
			if (eventHandlerAnnotation != null) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length != 1) {
					throw new IllegalArgumentException(
							"Method "
									+ method
									+ " has @EventHandler annotation, but requires "
									+ parameterTypes.length
									+ " arguments.  Event handler methods must require a single argument.");
				}
				Class<?> eventType = parameterTypes[0];
				if (eventType.isPrimitive()) {
					eventType = Primitives.wrap(eventType);
				}
				MethodHandler handler = new SimpleHandlerMethod(listener,
						method, exceptionHandler);

				Set<MethodHandler> handlers = methodsInListener.get(eventType);
				if (handlers == null) {
					handlers = new HashSet<MethodHandler>();
					methodsInListener.put(eventType, handlers);
				}
				handlers.add(handler);
				logger.trace(
						"Connected handler with event.\nHandler: {}\nEvent: {}",
						handler, eventType);
			}
		}
		return methodsInListener;
	}

	private A findMethodAnnotation(Method method, Class<A> annotationType) {
		return recursivelyFindAnnotation(new ArrayList<Annotation>(),
				method.getDeclaredAnnotations(), annotationType);
	}

	@SuppressWarnings("unchecked")
	private A recursivelyFindAnnotation(List<Annotation> checkedAnnotations,
			Annotation[] annotations, Class<A> annotationTargetType) {
		for (Annotation annotation : annotations) {
			if (!checkedAnnotations.contains(annotation)) {
				if (annotationTargetType.isInstance(annotation)) {
					return (A) annotation;
				}
				checkedAnnotations.add(annotation);
				return recursivelyFindAnnotation(checkedAnnotations, annotation
						.annotationType().getDeclaredAnnotations(),
						annotationTargetType);
			}
		}
		return null;
	}
}
