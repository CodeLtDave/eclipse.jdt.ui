/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import java.util.Comparator;

import org.eclipse.jdt.core.IJavaElement;

class JavaElementTypeComparator implements Comparator {


	/**
	 * Compares two Java element types. A type is considered to be
	 * greater if it may contain the other.
	 * 
	 * @return		an int less than 0 if object1 is less than object2,
	 *				0 if they are equal, and > 0 if object1 is greater
	 * 
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof IJavaElement) || !(o2 instanceof IJavaElement))
			throw new ClassCastException();
		return getIdForJavaElement((IJavaElement)o1) - getIdForJavaElement((IJavaElement)o2);
	}

	/**
	 * Compares two Java element types. A type is considered to be
	 * greater if it may contain the other.
	 * 
	 * @return		an int < 0 if object1 is less than object2,
	 *				0 if they are equal, and > 0 if object1 is greater
	 * 
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, int elementType) {
		if (!(o1 instanceof IJavaElement))
			throw new ClassCastException();
		return getIdForJavaElement((IJavaElement)o1) - getIdForJavaElementType(elementType);
	}

	int getIdForJavaElement(IJavaElement element) {
		return getIdForJavaElementType(element.getElementType());
	}
	
	int getIdForJavaElementType(int elementType) {
		switch (elementType) {
			case IJavaElement.JAVA_MODEL:
				return 130;
			case IJavaElement.JAVA_PROJECT:
				return 120;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return 110;
			case IJavaElement.PACKAGE_FRAGMENT:
				return 100;
			case IJavaElement.COMPILATION_UNIT:
				return 90;
			case IJavaElement.CLASS_FILE:
				return 80;
			case IJavaElement.TYPE:
				return 70;
			case IJavaElement.FIELD:
				return 60;
			case IJavaElement.METHOD:
				return 50;
			case IJavaElement.INITIALIZER:
				return 40;
			case IJavaElement.PACKAGE_DECLARATION:
				return 30;
			case IJavaElement.IMPORT_CONTAINER:
				return 20;
			case IJavaElement.IMPORT_DECLARATION:
				return 10;
			default :
				return 1;
		}
	}
}
