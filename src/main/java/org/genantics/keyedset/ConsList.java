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

package org.genantics.keyedset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Classical immutable list.
 * 
 * @author bobfoster
 * @param <V> list element type
 */
public class ConsList<V> {
	V element;
	ConsList<V> next;
	
	public ConsList(V element, ConsList<V> next) {
		this.element = element;
		this.next = next;
	}
	public boolean contains(V value) {
		if (element.equals(value)) {
			return true;
		} else if (next != null) {
			return next.contains(value);
		}
		return false;
	}
	public V head() {
		return element;
	}
	public ConsList<V> tail() {
		return next;
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
	public List<V> toList() {
		List<V> list = new ArrayList<V>();
		toList(list);
		return list;
	}
	private void toList(List<V> list) {
		list.add(element);
		if (next != null) {
			next.toList(list);
		}
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
			list = list.tail();
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
	
