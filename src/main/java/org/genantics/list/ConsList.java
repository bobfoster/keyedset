/*
 * The MIT License
 *
 * Copyright 2015 bobfoster.
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
import java.util.NoSuchElementException;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.Visitor;

/**
 * Classical immutable list.
 * 
 * Use null for nil.
 * 
 * @author bobfoster
 * @param <V> list element type
 */
public class ConsList<V> implements CList<V> {
	V element;
	ConsList<V> next;
	
	public ConsList(V element, ConsList<V> next) {
		this.element = element;
		this.next = next;
	}
	public ConsList<V> cons(V element) {
		return new ConsList(element, this);
	}
	public V head() {
		return element;
	}
	public CList<V> tail() {
		return next;
	}
	public boolean contains(V value) {
		if (element.equals(value)) {
			return true;
		} else if (next != null) {
			return next.contains(value);
		}
		return false;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		toString(sb);
		sb.append("]");
		return sb.toString();
	}
	private void toString(StringBuilder sb) {
		sb.append(element.toString());
		if (next != null) {
			sb.append(" ");
			next.toString(sb);
		}
	}
	public void toCollection(Collection<V> collection) {
		for (ConsList<V> list = this; list != null; list = list.next) {
			collection.add(element);
		}
	}

	public void visit(Visitor<V> visitor) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public boolean visit(ConditionalVisitor<V> visitor) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Return the number of elements in the list. Note this is an O(n) operation.
	 * If possible, use <code>isEmpty()</code> instead.
	 * @return list size
	 */
	public int size() {
		int size = 0;
		for (CList list = this; !list.isEmpty(); list = list.tail()) {
			size++;
		}
		return size;
	}

	public Iterator<V> reverseIterator() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public void reverseVisit(Visitor<V> visitor) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public boolean reverseVisit(ConditionalVisitor<V> visitor) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	private static class ConsListIterator<V> implements Iterator<V> {
		ConsList<V> list;
		public ConsListIterator(ConsList<V> list) {
			this.list = null;
		}
		@Override
		public boolean hasNext() {
			return list != null;
		}
		@Override
		public V next() {
			if (list == null) {
				throw new NoSuchElementException();
			}
			V head = (V) list.head();
			list = list.next;
			return head;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	public Iterator<V> iterator() {
		return new ConsListIterator(this);
	}
}
	
