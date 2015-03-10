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
import java.util.NoSuchElementException;
import java.util.Stack;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.DefaultIterator;
import org.genantics.access.Visitor;
import static org.genantics.list.CList.NIL;

/**
 * CodedList implements a "CDR-coded" list. For large lists, CodedList should have
 * significant performance advantages over ConsList:
 * <ul>
 * <li><code>size()</code> is an O(1) operation.</li>
 * <li>List traversals have better cache locality* (even more so when the
 * <code>iterator()</code> or <code>visit</code> methods are used)</li>
 * </ul>
 * * - The element references are stored contiguously in arrays; true cache
 * locality (elements stored in contiguously in arrays) will
 * have to wait for value types.
 * 
 * <p>Use CList.NIL for nil.
 * 
 * @author Bob Foster
 */
public class CodedList<V> implements CList<V> {
	// Ideally this would be computed based on platform.
	// The tradeoff is between pressure on the garbage collector vs.
	// cache miss cost, though I suspect for small numbers like this
	// the latter always dominates.
	private static final int ELEMENTS_LENGTH = 14;
	
	Object[] elements = new Object[ELEMENTS_LENGTH];
	CList<V> rest;
	int size;
	int restSize;	// size + restSize = total size of the list
	
	/**
	 * Constructor. Can be called by NIL and with a NIL list argument.
	 * This constructor implements <code>cons</code>.
	 * 
	 * @param element
	 * @param list 
	 */
	protected CodedList(V element, CList<V> list) {
		if (list == CList.NIL) {
			rest = list;
		} else {
			CodedList<V> other = (CodedList<V>) list;
			if (other.size < other.elements.length) {
				//copy the list
				elements = other.elements;
				size = other.size;
				rest = other.rest;
				restSize = other.restSize;
			} else {
				// make list our rest
				rest = other;
				restSize = other.restSize + other.size;
			}
		}
		// note that elements are reversed in the elements array
		// but the rest links are in order
		elements[size++] = element;
	}
	
	protected CodedList(V element, CodedList<V> list, int pos) {
		// the list is never NIL and pos is always less than elements.length
		// (because a slice is always created with pos = original.size - 1
		// and can only go lower)
		// copy the list
		elements = list.elements;
		size = pos;
		rest = list.rest;
		restSize = list.restSize;
		// note that elements are reversed in the elements array
		// but the rest links are in order
		elements[size++] = element;
	}
	
	protected CodedList(CodedList<V> other) {
		elements = other.elements;
		size = other.size;
		rest = other.rest;
		restSize = other.restSize;
	}
	
	public boolean isEmpty() {
		return false;
	}
	
	public int size() {
		return size + restSize;
	}
	
	public boolean contains(V element) {
		return contains(element, size);
	}

	protected boolean contains(V element, int pos) {
		CodedList<V> list = this;
		while (((CList<V>)list) != CList.NIL) {
			for (int i = pos - 1; i >= 0; i--) {
				if (element.equals(list.elements[i])) {
					return true;
				}
			}
			list = (CodedList<V>) list.rest;
			pos = list.size;
		}
		return false;
	}

	public CList<V> cons(V element) {
		return new CodedList<V>(element, this);
	}

	public V head() {
		return (V) elements[size-1];
	}

	public CList<V> tail() {
		if (size > 1) {
			return new CodedSlice<V>(size - 1);
		}
		return rest;
	}
	
	protected void visit(Visitor<V> visitor, int pos) {
		CodedList<V> list = this;
		while (((CList<V>)list) != CList.NIL) {
			for (int i = pos - 1; i >= 0; i--) {
				visitor.visit((V) list.elements[i]);
			}
			list = (CodedList<V>) list.rest;
			pos = list.size;
		}
	}
	
	protected boolean visit(ConditionalVisitor<V> visitor, int pos) {
		CodedList<V> list = this;
		while (((CList<V>)list) != CList.NIL) {
			for (int i = pos - 1; i >= 0; i--) {
				if (visitor.visit((V) list.elements[i])) {
					return true;
				}
			}
			list = (CodedList<V>) list.rest;
			pos = list.size;
		}
		return false;
	}
	
	protected void reverseVisit(Visitor<V> visitor, int pos, CList<V> last) {
		if (rest != NIL) {
			((CodedList<V>)rest).reverseVisit(visitor, pos, last);
		}
		int max = last == this ? pos : size;
		for (int i = 0; i < max; i++) {
			visitor.visit((V) elements[i]);
		}
	}

	protected boolean reverseVisit(ConditionalVisitor<V> visitor, int pos, CList<V> last) {
		if (rest != NIL) {
			if (((CodedList<V>)rest).reverseVisit(visitor, pos, last)) {
				return true;
			}
		}
		int max = last == this ? pos : size;
		for (int i = 0; i < max; i++) {
			if (visitor.visit((V) elements[i])) {
				return true;
			}
		}
		return false;
	}

	public void visit(Visitor<V> visitor) {
		visit(visitor, size);
	}

	public boolean visit(ConditionalVisitor<V> visitor) {
		return visit(visitor, size);
	}
	
	public void reverseVisit(Visitor<V> visitor) {
		reverseVisit(visitor, size, this);
	}

	public boolean reverseVisit(ConditionalVisitor<V> visitor) {
		return reverseVisit(visitor, size, this);
	}

	protected void toCollection(Collection<V> collection, int pos) {
		CodedList<V> list = this;
		while (((CList<V>)list) != CList.NIL) {
			for (int i = pos - 1; i >= 0; i--) {
				collection.add((V) list.elements[i]);
			}
			list = (CodedList<V>) list.rest;
			pos = list.size;
		}
	}

	public void toCollection(Collection<V> collection) {
		toCollection(collection, size);
	}

	protected class CListIterator<V> extends DefaultIterator<V> {
		CList<V> current;
		int pos = size;

		CListIterator(CList<V> current, int pos) {
			this.current = current;
			this.pos = pos;
		}
		
		public boolean hasNext() {
			if (current != NIL) {
				if (pos == 0) {
					current = ((CodedList<V>)current).rest;
					if (current != NIL) {
						pos = ((CodedList<V>)current).size;
					}
				}
			}
			return current != NIL;
		}

		public V next() {
			if (current == CList.NIL) {
				throw new NoSuchElementException();
			}
			return (V) ((CodedList<V>)current).elements[pos--];
		}
	}

	public Iterator<V> iterator() {
		return new CListIterator<V>(this, size);
	}
	
	protected class CListReverseIterator<V> extends DefaultIterator<V> {
		CList<V> current;
		int saveMax;
		int max;
		int pos;
		Stack<CList<V>> stack = new Stack<CList<V>>();

		CListReverseIterator(CList<V> current, int pos) {
			this.current = current;
			this.saveMax = pos;
			CodedList<V> last = (CodedList<V>) current;
			while (last.rest != NIL) {
				stack.push(last);
				last = (CodedList<V>) last.rest;
			}
			this.max = last == current ? saveMax : last.size;
		}
		
		public boolean hasNext() {
			if (current != NIL) {
				if (pos == max) {
					if (stack.isEmpty()) {
						current = NIL;
					} else {
						current = stack.pop();
						// Use temporary to work around stupid compiler bug
						CodedList<V> cur = (CodedList<V>) current;
						max = stack.isEmpty() ? saveMax : cur.size;
						pos = 0;
					}
				}
			}
			return current != NIL;
		}

		public V next() {
			if (current == CList.NIL) {
				throw new NoSuchElementException();
			}
			return (V) ((CodedList<V>)current).elements[pos++];
		}
	}

	public Iterator<V> reverseIterator() {
		return new CListIterator<V>(this, size);
	}
	
	/**
	 * CodedSlice is used to iterate through the elements in a single
	 * CodedList. <code>getFirst</code> and <code>getRest</code>
	 * stay in the CodedSlice until the position reaches zero;
	 * then return to the CodedList.
	 * @param <V> 
	 */
	private class CodedSlice<V> implements CList<V> {
		int pos;
		
		CodedSlice(int pos) {
			this.pos = pos;
		}

		public CList<V> cons(V element) {
			return new CodedList<V>(element, (CodedList<V>) CodedList.this, pos);
		}
		
		public boolean isEmpty() {
			return false;
		}
		
		public int size() {
			return pos + ((CodedList<V>)CodedList.this).restSize;
		}

		public V head() {
			return (V) CodedList.this.elements[pos-1];
		}

		public CList<V> tail() {
			if (pos > 1) {
				return new CodedSlice(pos - 1);
			}
			return (CList<V>) CodedList.this.rest;
		}
		
		public boolean contains(V element) {
			return ((CodedList<V>)CodedList.this).contains(element, pos);
		}

		public Iterator<V> iterator() {
			return new CListIterator<V>((CList<V>) CodedList.this, pos);
		}

		public Iterator<V> reverseIterator() {
			return new CListReverseIterator<V>((CList<V>) CodedList.this, pos);
		}

		public void visit(Visitor<V> visitor) {
			((CodedList<V>)CodedList.this).visit(visitor, pos);
		}

		public boolean visit(ConditionalVisitor<V> visitor) {
			return ((CodedList<V>)CodedList.this).visit(visitor, pos);
		}

		public void reverseVisit(Visitor<V> visitor) {
			((CodedList<V>)CodedList.this).reverseVisit(visitor, pos, (CList<V>)CodedList.this);
		}

		public boolean reverseVisit(ConditionalVisitor<V> visitor) {
			return ((CodedList<V>)CodedList.this).reverseVisit(visitor, pos, (CList<V>)CodedList.this);
		}

		public void toCollection(Collection<V> collection) {
			((CodedList<V>)CodedList.this).toCollection(collection, pos);
		}
	}
}
