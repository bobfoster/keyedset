/*
 * The MIT License
 *
 * Copyright 2015 Bob Foster.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.genantics.access;

import java.util.Iterator;

/**
 * Element access and data common to all immutable collections.
 * 
 * @author Bob Foster
 */
public interface Access<V> {

	public static final boolean JAVA_8 = "1.8".equals(System.getProperty("java.specification.version"));
	
	/**
	 * Test if empty. <code>isEmpty()</code> is guaranteed to be an O(1)
	 * operation; it should always be used in preference to
	 * <code>size() == 0</code>, as the latter may have cost up to O(n);
	 * 
	 * @return true if the collection contains no elements; otherwise false
	 */
	boolean isEmpty();
	
	/**
	 * Get the number of elements in the collection. Depending on the
	 * collection, <code>size()</code> may have cost O(1), O(log(n) up
	 * to O(n).
	 * 
	 * @return number of elements
	 */
	int size();
	
	/**
	 * Test if collection contains value.
	 * 
	 * @param value
	 * @return true if equals comparison of value is true for at least one
	 * element in the collection; otherwise false
	 */
	boolean contains(V value);
	
	/**
	 * Get the elements in order.
	 * 
	 * @return element iterator
	 */
	Iterator<V> iterator();
	
	/**
	 * Get the elements in reverse order.
	 * 
	 * @return element iterator
	 */
	Iterator<V> reverseIterator();
	
	/**
	 * Visit all elements in order.
	 * 
	 * @param visitor 
	 */
	void visit(Visitor<V> visitor);
	
	/**
	 * Visit all elements in order until <code>visitor.visit()</code> returns true;
	 * 
	 * @param visitor
	 * @return true if any call to <code>visitor.visit()</code> returned true;
	 * otherwise false
	 */
	boolean visit(ConditionalVisitor<V> visitor);
	
	/**
	 * Visit all elements in reverse order.
	 * 
	 * @param visitor 
	 */
	void reverseVisit(Visitor<V> visitor);
	
	/**
	 * Visit all elements in reverse order until <code>visitor.visit()</code> returns true;
	 * 
	 * @param visitor
	 * @return true if any call to <code>visitor.visit()</code> returned true;
	 * otherwise false
	 */
	boolean reverseVisit(ConditionalVisitor<V> visitor);
}
