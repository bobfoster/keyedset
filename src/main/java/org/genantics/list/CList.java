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

package org.genantics.list;

import java.util.Collection;
import java.util.Iterator;
import org.genantics.access.Access;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.Visitor;

/**
 * Classical List interface.
 * 
 * @author bobfoster
 */
public interface CList<V> extends Iterable<V>, Access<V> {
	
	public static final Nil NIL = Nil.NIL;
	
	/**
	 * Add an element to the head of the list.
	 * 
	 * @param element
	 * @return new list
	 */
	CList<V> cons(V element);
	
	/**
	 * Test for element.
	 * 
	 * @param element
	 * @return true if list contains element; otherwise, false
	 */
	boolean contains(V element);
	
	/**
	 * Get the head of the list.
	 * 
	 * @return element
	 */
	V head();
	
	/**
	 * Get the rest of the list after the head.
	 * 
	 * Note that <code>getFirst/getRest</code> are not the most efficient
	 * way to traverse the list elements.
	 * @see #iterator() 
	 * 
	 * @return list
	 */
	CList<V> tail();
	
	/**
	 * Iterate through the elements in the list. This is the most efficient
	 * way to traverse the list elements.
	 * 
	 * @return 
	 */
	Iterator<V> iterator();
	
	/**
	 * Visit each element in order.
	 * 
	 * @param visitor 
	 */
	void visit(Visitor<V> visitor);
	
	/**
	 * Visit each element in order until visit returns false.
	 * 
	 * <p>If the list is empty, returns true.
	 * @param visitor
	 * @return false if any call to visit returned false; otherwise true
	 */
	boolean visit(ConditionalVisitor<V> visitor);
	
	/**
	 * Copy elements in list to collection in order.
	 * 
	 * @param collection 
	 */
	void toCollection(Collection<V> collection);
}
