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

package org.genantics.array;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.DefaultIterator;
import org.genantics.access.Visitor;

/**
 *
 * @author Bob Foster
 */
public abstract class Array<V> implements IndexedAccess<V> {

	private static final int ElEMENTS_LENGTH = 32;
	private static final int ELEMENTS_SHIFT = 5;
	private static final int ELEMENTS_MASK = ElEMENTS_LENGTH - 1;
	
	/**
	 * An empty array suitable for growing.
	 */
	public static final Array EMPTY = new Empty();
	
	public static Array newArray(int size, Object initialValue) {
		if (size < 0) throw new ArrayIndexOutOfBoundsException(""+size);
		if (size == 0) return EMPTY;
		
		// There are log32(size) levels in this tree.
		// y = b^x <=> x = log b(y)
		// size = 32^x
		
		int height = 0;
		int n = size;
		while (n != 0) {
			height++;
			n >>= ELEMENTS_SHIFT;
		}
		int[] sizeSoFar = new int[1];
		return createAndFill(size, initialValue, height, sizeSoFar);
	}
	
	private static Array createAndFill(int size, Object initialValue, int height, int[] sizeSoFar) {
		if (height == 1) {
			Values values = new Values();
			int max = Math.min(ElEMENTS_LENGTH, sizeSoFar[0]);
			for (int i = 0; i < max; i++) {
				values.elements[i] = initialValue;
			}
			values.size = max;
			sizeSoFar[0] += max;
			return values;
		} else {
			References references = new References();
			int startSize = sizeSoFar[0];
			for (int i = 0; i < ElEMENTS_LENGTH && sizeSoFar[0] < size; i++) {
				references.refs[i] = createAndFill(size, initialValue, height - 1, sizeSoFar);
			}
			references.size = sizeSoFar[0] - startSize;
			references.shift = 5*height;
			references.mask = (1 << references.shift) - 1;
			return references;
		}
	}
	
	protected class ArrayState {
		Array<V> array;
		int index;
		public ArrayState(Array<V> array, int index) {
			this.array = array;
			this.index = index;
		}
	}
	
	protected abstract ArrayState getNext(int index, Stack<ArrayState> stack);

	protected abstract ArrayState getPrev(int index, Stack<ArrayState> stack);

	// The base Array class has no storage
	
	/**
	 * Empty is a singleton object representing the empty array.
	 * 
	 * The type parameters really have no meaning for Empty; it is applicable
	 * to all types.
	 */
	protected static class Empty<V> extends Array<V> {
		
		protected ArrayState getNext(int index, Stack<ArrayState> stack) {
			return null;
		}

		protected ArrayState getPrev(int index, Stack<ArrayState> stack) {
			return null;
		}

		public V get(int i) {
			throw new ArrayIndexOutOfBoundsException(""+i);
		}

		public Array<V> set(int i, V value) {
			throw new ArrayIndexOutOfBoundsException(""+i);
		}

		public int find(V value) {
			return -1;
		}

		public boolean isEmpty() {
			return true;
		}

		public int size() {
			return 0;
		}

		public boolean contains(V value) {
			return false;
		}

		public Iterator<V> iterator() {
			return new DefaultIterator<V>() {
				public boolean hasNext() {
					return false;
				}
				public V next() {
					throw new NoSuchElementException();
				}
			};
		}

		public Iterator<V> reverseIterator() {
			return iterator();
		}

		public void visit(Visitor<V> visitor) {
			// do nothing
		}

		public boolean visit(ConditionalVisitor<V> visitor) {
			// not here
			return false;
		}

		public void reverseVisit(Visitor<V> visitor) {
			// do nothing
		}

		public boolean reverseVisit(ConditionalVisitor<V> visitor) {
			// not here
			return false;
		}
	}
	
	/**
	 * A values array contains actual data.
	 * 
	 * @param <V> 
	 */
	protected static class Values<V> extends Array<V> {
		
		Object[] elements = new Object[ElEMENTS_LENGTH];
		int size;
		
		private Values() {
		}
		
		private Values(Values<V> other) {
			size = other.size;
			System.arraycopy(other.elements, 0, elements, 0, size);
		}
		
		protected ArrayState getNext(int index, Stack<ArrayState> stack) {
			index++;
			if (index < size) {
				return new ArrayState(this, index);
			}
			if (stack.isEmpty()) {
				return null;
			}
			ArrayState state = stack.pop();
			return state.array.getNext(state.index, stack);
		}
		
		protected ArrayState getPrev(int index, Stack<ArrayState> stack) {
			if (index < 0) {
				index = size;
			}
			index--;
			if (index >= 0) {
				return new ArrayState(this, index);
			}
			if (stack.isEmpty()) {
				return null;
			}
			ArrayState state = stack.pop();
			return state.array.getNext(state.index, stack);
		}
		
		private void checkIndex(int i) {
			if (i > size) throw new IndexOutOfBoundsException();
		}

		public V get(int i) {
			checkIndex(i);
			return (V) elements[i];
		}
		
		public void checkNull(V value) {
			if (value == null) throw new NullPointerException();
		}

		public Array<V> set(int i, V value) {
			checkIndex(i);
			checkNull(value);
			Values<V> values = new Values<V>(this);
			values.elements[i] = value;
			return values;
		}

		public int find(V value) {
			checkNull(value);
			for (int i = 0; i < size; i++) {
				if (value.equals(elements[i])) {
					return i;
				}
			}
			return -1;
		}

		public boolean isEmpty() {
			return false;
		}

		public int size() {
			return size;
		}

		public boolean contains(V value) {
			return find(value) >= 0;
		}

		public Iterator<V> iterator() {
			return new DefaultIterator<V>() {
				int i = 0;
				
				public boolean hasNext() {
					return i < Values.this.size;
				}
				
				public V next() {
					if (i == Values.this.size) throw new NoSuchElementException();
					return (V) Values.this.elements[i++];
				}
				
			};
		}

		public Iterator<V> reverseIterator() {
			return new DefaultIterator<V>() {
				int i = Values.this.size - 1;
				
				public boolean hasNext() {
					return i >= 0;
				}
				
				public V next() {
					if (i < 0) throw new NoSuchElementException();
					return (V) Values.this.elements[i--];
				}
				
			};
		}

		public void visit(Visitor<V> visitor) {
			for (int i = 0; i < this.size; i++) {
				visitor.visit((V) elements[i]);
			}
		}

		public boolean visit(ConditionalVisitor<V> visitor) {
			for (int i = 0; i < this.size; i++) {
				if (visitor.visit((V) elements[i])) {
					return true;
				}
			}
			return false;
		}

		public void reverseVisit(Visitor<V> visitor) {
			for (int i = this.size - 1; i >= 0; i--) {
				visitor.visit((V) elements[i]);
			}
		}

		public boolean reverseVisit(ConditionalVisitor<V> visitor) {
			for (int i = this.size - 1; i >= 0; i--) {
				if (visitor.visit((V) elements[i])) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * A references array contains references to References or, at height 2, Values.
	 * @param <V> 
	 */
	protected static class References<V> extends Array<V> {
		Array[] refs = new Array[ElEMENTS_LENGTH];
		int size;  // number of elements indirectly stored - for top level, array size
		int shift; // shift to get index into refs
		int mask;  // mask to get index to pass down
		
		References() {
		}
		
		References(References other) {
			System.arraycopy(other.refs, 0, refs, 0, ElEMENTS_LENGTH);
			size = other.size;
			shift = other.shift;
			mask = other.mask;
		}
		
		protected ArrayState getNext(int index, Stack<ArrayState> stack) {
			index++;
			if (index < size) {
				stack.push(new Array.ArrayState(this, index));
				Array<V> array = refs[index >>> shift];
				return array.getNext(-1, stack);
			}
			if (stack.isEmpty()) {
				return null;
			}
			ArrayState state = stack.pop();
			return state.array.getNext(state.index, stack);
		}
		
		protected ArrayState getPrev(int index, Stack<ArrayState> stack) {
			if (index < 0) {
				index = size;
			}
			index--;
			if (index >= 0) {
				stack.push(new Array.ArrayState(this, index));
				Array<V> array = refs[index >>> shift];
				return array.getNext(-1, stack);
			}
			if (stack.isEmpty()) {
				return null;
			}
			ArrayState state = stack.pop();
			return state.array.getNext(state.index, stack);
		}
		
		private void checkIndex(int i) {
			if (i < 0 || i > size) {
				// This can only happen at the top level.
				throw new ArrayIndexOutOfBoundsException(""+i+" for array size "+size);
			}
		}

		public V get(int i) {
			checkIndex(i);
			Array<V> lower = refs[i >>> shift];
			return lower.get(i & mask);
		}

		public Array<V> set(int i, V value) {
			checkIndex(i);
			int index = i >>> shift;
			Array<V> lower = refs[index];
			Array<V> newLower = lower.set(i & mask, value);
			References newThis = new References(this);
			newThis.refs[index] = newLower;
			return newThis;
		}

		public int find(V value) {
			int max = size >>> shift;
			for (int i = 0; i < max; i++) {
				int result = refs[i].find(value);
				if (result >= 0) {
					return result;
				}
			}
			return -1;
		}

		public boolean isEmpty() {
			return false;
		}

		public int size() {
			return size;
		}

		public boolean contains(V value) {
			return find(value) >= 0;
		}
		
		private class ArrayIterator extends DefaultIterator<V> {
			ArrayState current;
			Stack<ArrayState> stack;
			
			public ArrayIterator() {
				stack = new Stack<ArrayState>();
				current = getNext(-1, stack);
			}

			public boolean hasNext() {
				return current != null;
			}

			public V next() {
				if (current == null) {
					throw new NoSuchElementException();
				}
				V tmp = current.array.get(current.index);
				current = current.array.getNext(current.index, stack);
				return tmp;
			}
			
		}
		
		public Iterator<V> iterator() {
			return new ArrayIterator();
		}

		private class ReverseArrayIterator extends DefaultIterator<V> {
			ArrayState current;
			Stack<ArrayState> stack;
			
			public ReverseArrayIterator() {
				stack = new Stack<ArrayState>();
				current = getPrev(-1, stack);
			}

			public boolean hasNext() {
				return current != null;
			}

			public V next() {
				if (current == null) {
					throw new NoSuchElementException();
				}
				V tmp = current.array.get(current.index);
				current = current.array.getPrev(current.index, stack);
				return tmp;
			}
			
		}
		
		public Iterator<V> reverseIterator() {
			return new ReverseArrayIterator();
		}

		public void visit(Visitor<V> visitor) {
			int max = size >>> shift;
			for (int i = 0; i < max; i++) {
				refs[i].visit(visitor);
			}
		}

		public boolean visit(ConditionalVisitor<V> visitor) {
			int max = size >>> shift;
			for (int i = 0; i < max; i++) {
				if (refs[i].visit(visitor)) {
					return true;
				}
			}
			return false;
		}

		public void reverseVisit(Visitor<V> visitor) {
			int max = size >>> shift;
			for (int i = max-1; i >=0; i--) {
				refs[i].visit(visitor);
			}
		}

		public boolean reverseVisit(ConditionalVisitor<V> visitor) {
			int max = size >>> shift;
			for (int i = max-1; i >=0; i--) {
				if (refs[i].visit(visitor)) {
					return true;
				}
			}
			return false;
		}
	}
}
