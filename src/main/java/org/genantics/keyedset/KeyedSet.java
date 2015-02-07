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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.genantics.seq.Receiver;
import org.genantics.seq.ReversableSequence;
import org.genantics.seq.Sequence;

/**
 * KeyedSet is an immutable ordered set of elements with keys.
 * 
 * <p>Any operation that changes the set returns a new set that shares
 * structure with the original set.
 * 
 * <p>KeyedSet does not implement java.util.Set, as the Set interface
 * is not well-suited to an immutable tree, but provides the common
 * set operations and a number of map-like operations.
 * 
 * <p>To minimize memory overhead, the size() method is O(n). If an O(1)
 * size() is absolutely necessary, wrap KeyedSet in another class
 * that keeps track of size. Convenience methods are provided for
 * add, delete and remove that allow this to be done easily.
 * 
 * <p>The fast union and intersection algorithms are described in 
 * <a href="http://www.cs.princeton.edu/~appel/papers/redblack.pdf">"Efficient
 * Verified Red-Black Trees", Andrew W. Appel, Princeton University</a>.
 * 
 * 
 * @author Bob Foster
 */
public class KeyedSet<K extends Comparable<K>, T extends Keyed<K>>
	implements Iterable<T>, Sequence<T>, ReversableSequence<T> {

	protected T member;
	protected KeyedSet<K,T> left;
	protected KeyedSet<K,T> right;
	protected int height;
	
	public KeyedSet(KeyedSet<K,T> other) {
		copy(other);
	}
	
	public KeyedSet(Iterable<T> other) {
		KeyedSet<K,T> set = EMPTY;
		for (T t : other) {
			set = set.add(t);
		}
		copy(set);
	}
	
	private void copy(KeyedSet<K,T> other) {
		// it's ok to copy the internals, as they are never modified after construction
		member = other.member;
		left = other.left;
		right = other.right;
		height = other.height;
	}
	
	protected KeyedSet(T value, KeyedSet<K,T> left, KeyedSet<K,T> right) {
		this.member = value;
		this.left = left;
		this.right = right;
		init();
	}
	
	/** Allow subclass to override */
	protected KeyedSet<K,T> newTree(T value, KeyedSet<K,T> left, KeyedSet<K,T> right) {
		return new KeyedSet<K,T>(value, left, right);
	}
	
	protected void init() {
		height = Math.max(left.getHeight(), right.getHeight()) + 1;
	}
	
	/**
	 * Get the size of the set. Note this is an O(n) operation!
	 * If you need a fast size, wrap the set in another class that
	 * keeps track of the size.
	 * 
	 * @return the number of elements in the set
	 */
	public int size() {
		return 1+left.size()+right.size();
	}
	
	/**
	 * Test if tree contains the specified key.
	 * 
	 * @param key
	 * @return true if key matched in tree; otherwise false
	 */
	public boolean containsKey(K key) {
		int cmp = key.compareTo(member.getKey());
		if (cmp == 0) {
			return true;
		} else if (cmp < 0) {
			return left.containsKey(key);
		} else {
			return right.containsKey(key);
		}
	}
	
	/**
	 * Test if set contains the specified value.
	 * 
	 * @param value
	 * @return true if element with matching key equals value; otherwise false
	 */
	public boolean contains(T value) {
		T t = get(value.getKey());
		return t == null ? false : t.equals(value);
	}
	
	/**
	 * Get the first element in the tree.
	 * 
	 * @return element with lowest key value
	 */
	public T getFirst() {
		if (left == EMPTY) {
			return member;
		}
		return left.getFirst();
	}
	
	/**
	 * Get the last element in the tree.
	 * 
	 * @return element with highest key value
	 */
	public T getLast() {
		if (right == EMPTY) {
			return member;
		}
		return right.getLast();
	}
	
	/**
	 * Delete the first element of the tree. If called with an empty tree,
	 * returns an empty tree.
	 * 
	 * @return tree without first element, if any
	 */
	public KeyedSet<K,T> deleteFirst() {
		if (left == EMPTY) {
			if (right == EMPTY) {
				return EMPTY;
			}
			return right;
		}
		return balanceTree(newTree(member, left.deleteFirst(), right));
	}
	
	/**
	 * Delete the last element of the tree. If called with an empty tree,
	 * returns an empty tree.
	 * 
	 * @return tree without last element, if any
	 */
	public KeyedSet<K,T> deleteLast() {
		if (right == EMPTY) {
			if (left == EMPTY) {
				return EMPTY;
			}
			else {
				return left;
			}
		}
		return balanceTree(newTree(member, left, right.deleteLast()));
	}
	
	/**
	 * Delete the first element of the tree. If called with an empty tree,
	 * returns an empty tree.
	 * 
	 * <p>If there was a first element, it is written to deleted[0].
	 * Thus, a combination of <code>getFirst()</code> and <code>deleteFirst()</code>.
	 * 
	 * @return tree without first element, if any
	 */
	public KeyedSet<K,T> deleteFirst(T[] deleted) {
		if (left == EMPTY) {
			deleted[0] = member;
			if (right == EMPTY) {
				return EMPTY;
			}
			return right;
		}
		return balanceTree(newTree(member, left.deleteFirst(deleted), right));
	}
	
	/**
	 * Delete the last element of the tree. If called with an empty tree,
	 * returns an empty tree.
	 * 
	 * <p>If there was a last element, it is written to deleted[0].
	 * Thus, a combination of <code>getLast()</code> and <code>deleteLast()</code>.
	 * 
	 * @return tree without last element, if any
	 */
	public KeyedSet<K,T> deleteLast(T[] deleted) {
		if (right == EMPTY) {
			deleted[0] = member;
			if (left == EMPTY) {
				return EMPTY;
			}
			else {
				return left;
			}
		}
		return balanceTree(newTree(member, left, right.deleteLast(deleted)));
	}

	public Object seq(Receiver<T> receiver) {
		doSeq(receiver);
		receiver.close();
		return receiver.value();
	}
	
	private void doSeq(final Receiver<T> receiver) {
		if (left != EMPTY) {
			left.doSeq(receiver);
		}
		if (!receiver.receive(member)) {
			return;
		}
		if (right != EMPTY) {
			right.doSeq(receiver);
		}
	}

	public Object rev(Receiver<T> receiver) {
		doRev(receiver);
		receiver.close();
		return receiver.value();
	}
	
	private void doRev(final Receiver<T> receiver) {
		if (right != EMPTY) {
			right.doSeq(receiver);
		}
		if (!receiver.receive(member)) {
			return;
		}
		if (left != EMPTY) {
			left.doSeq(receiver);
		}
	}

	private class KeyIterator implements Iterator<K> {
		private final Iterator<T> it;

		KeyIterator(Iterator<T> it) {
			this.it = it;
		}
		
		public boolean hasNext() {
			return it.hasNext();
		}

		public K next() {
			return it.next().getKey();
		}
		
		public void remove() {
		  it.remove();
		}
		
	}
	
	private class KeyIterable implements Iterable {

		public Iterator<K> iterator() {
			return new KeyIterator(KeyedSet.this.iterator());
		}
		
	}
	
	public Iterable<K> getKeys() {
		return new KeyIterable();
	}
	
	protected int getHeight() {
		return height;
	}
	
	protected int getBalance() {
		return left.getHeight() - right.getHeight();
	}
	
	/** Test tree balance - for testing */
	boolean isBalanced() {
		int balance = getBalance();
		return balance >= -1 && balance <= 1 && left.isBalanced() && right.isBalanced();
	}
	
	/** Test tree order - for testing */
	boolean isOrdered() {
		K memberKey = member.getKey();
		return ((left.isEmpty() || left.member.getKey().compareTo(memberKey) < 0 && left.isOrdered()) &&
			   (right.isEmpty() || memberKey.compareTo(right.member.getKey()) < 0 && right.isOrdered()));
	}

	protected KeyedSet<K,T> setLeft(KeyedSet<K,T> left) {
		return new KeyedSet<K,T>(member, left, right);
	}

	protected KeyedSet<K,T> setRight(KeyedSet<K,T> right) {
		return new KeyedSet<K,T>(member, left, right);
	}

	/**
	 * @return true if set is empty; otherwise false
	 */
	public boolean isEmpty() {
		return false;
	}
	
	/**
	 * Add an element to the set. Null is not allowed.
	 * 
	 * @param element
	 * @return new tree containing element
	 * @throws NullPointerException if element is null
	 */
	public KeyedSet<K,T> add(T element) {
		boolean[] added = new boolean[2];
		if (element == null) throw new NullPointerException();
		return addElement(element, added);
	}
	
	/**
	 * Convenience method for implementing API like java.util.Set, which sets
	 * an added flag true if the key was not already in the tree, i.e.,
	 * the tree became larger, and another added flag true if the element
	 * was not already in the tree, i.e., the element was replaced. The
	 * latter flag is based on equals and will always be true for identity
	 * comparison.
	 * 
	 * The added[0] flag can be used by any subclass needing a fast size().
	 * 
	 * @param element to add
	 * @param added boolean[2] On return added[0] true indicates element was added (tree size grew);
	 * otherwise, key already present (tree size unchanged); added[1] true indicates
	 * element was replaced (tree changed); otherwise element was not replaced.
	 * The flags are independent. The same tree is returned iff
	 * !(added[0] || added[1]).
	 * @return
	 * @throws NullPointerException if element is null
	 * @throws ArrayIndexOutOfBoundsException if added length is less than 2
	 */
	public KeyedSet<K,T> add(T element, boolean[] added) {
		added[0] = false;
		added[1] = false;
		if (element == null) throw new NullPointerException();
		return addElement(element, added);
	}
	
	/**
	 * Get the element with matching key.
	 * 
	 * @param key
	 * @return element or null if not found
	 */
	public T get(K key) {
		int cmp = key.compareTo(member.getKey());
		if (cmp == 0) {
			return member;
		} else if (cmp < 0) {
			return left.get(key);
		} else {
			return right.get(key);
		}
	}
	
	protected static interface DeleteTest<T> {
		boolean doDelete(T t);
		boolean wasDeleted();
	}
		
	/**
	 * Delete the element with matching key.
	 * 
	 * @param key
	 * @return new tree without matching element, if any
	 */
	public KeyedSet<K,T> delete(K key) {
		return delete(key, new boolean[1]);
	}
	
	/**
	 * Delete the element with matching key.
	 * 
	 * @param key
	 * @param deleted deleted[0] is true on return if the key was present
	 * in the tree, i.e., the tree became smaller; otherwise false
	 * 
	 * @return new tree without matching element, if any
	 */
	public KeyedSet<K,T> delete(K key, final boolean[] deleted) {
		deleted[0] = false;
		return delete(key, new DeleteTest<T>() {
			public boolean doDelete(T t) {
				return deleted[0] = true;
			}
			public boolean wasDeleted() {
				return deleted[0];
			}
		});
	}
	
	protected KeyedSet<K,T> delete(K key, DeleteTest<T> test) {
		int cmp = key.compareTo(member.getKey());
		KeyedSet<K,T> node = this;
		if (cmp < 0) {
			KeyedSet<K,T> left = this.left.delete(key, test);
			if (!test.wasDeleted()) {
				return node;
			}
			node = newTree(member, left, right);
		} else if (cmp > 0) {
			KeyedSet<K,T> right = this.right.delete(key, test);
			if (!test.wasDeleted()) {
				return node;
			}
			node = newTree(member, left, right);
		} else {
			if (!test.doDelete(member)) {
				return node;
			}
			// If a leaf, just remove it
			if (left.isEmpty() && right.isEmpty()) {
				return EMPTY;
			}
			// If only left or right, hoist them up
			if (left.isEmpty()) {
				return right;
			}
			if (right.isEmpty()) {
				return left;
			}
			// Both left and right, find the highest lower node
			KeyedSet<K,T> high = left;
			while (!high.right.isEmpty()) {
				high = high.right;
			}
			// High cannot have a right, so pivot on it
			if (high == left) {
				// Replace this node with left node
				node = newTree(left.member, left.left, right);
			} else {
				// Replace this node with high's left subtree as right subtree
				node = newTree(high.member, removeHigh(left, high), right);
			}
		}
		return balanceTree(node);
	}
	
	/** addElement replaces this tree with a tree containing element */
	protected KeyedSet<K,T> addElement(T element, boolean[] added) {
		int cmp = element.getKey().compareTo(member.getKey());
		KeyedSet<K,T> node = null;
		if (cmp < 0) {
			KeyedSet<K,T> left = this.left.addElement(element, added);
			if (!(added[0] || added[1])) {
				return this;
			}
			node = newTree(member, left, right);
		} else if (cmp > 0) {
			KeyedSet<K,T> right = this.right.addElement(element, added);
			if (!(added[0] || added[1])) {
				return this;
			}
			node = newTree(member, left, right);
		} else {
			// We allow for the Comparable to compare only part
			// of the element, e.g., a key, so equals can return
			// a different result. This egregious violation of
			// the standard relationship of compareTo, equals
			// and hashCode makes the tree more useful for
			// implementing maps.
			if (!this.member.equals(element)) {
				added[1] = true;
				return newTree(element, left, right);
			}
			return this;
		}
		return balanceTree(node);
	}

	protected KeyedSet<K,T> balanceTree(KeyedSet<K,T> node) {
		int balance = node.getBalance();
		if (balance > 1) {
			// unbalanced on the left
			if (node.left.getBalance() < 0) {
				// left is right-heavy, so rotate it left
				node = node.setLeft(rotateLeft(node.left));
			}
			node = rotateRight(node);
		} else if (balance < -1) {
			// unbalanced on the right
			if (node.right.getBalance() > 0) {
				// right is left-heavy, so rotate it right
				node = node.setRight(rotateRight(node.right));
			}
			node = rotateLeft(node);
		}
		return node;
	}

	/**
	 * Rotate left, pivoting on a's right child.
	 * <pre>
	 * a               b
	 *  \             / \
	 *   b     to    a   c
	 *  / \           \
	 * ?   c           ?
	 * </pre>
	 * @param a
	 * @return modified right child of a
	 */
	private KeyedSet<K,T> rotateLeft(KeyedSet<K,T> a) {
		KeyedSet<K,T> b = a.right;
		return b.setLeft(a.setRight(b.left));
	}
	
	/**
	 * Rotate right, pivoting on c's left child.
	 * <pre>
	 *       c          b  
	 *      /          / \
	 *     b    to    a   c
	 *    / \            /
	 *   a   ?          ?
	 * </pre>
	 * @param c
	 * @return modified left child of c
	 */
	private KeyedSet<K,T> rotateRight(KeyedSet<K,T> c) {
		KeyedSet<K,T> b = c.left;
		return b.setRight(c.setLeft(b.right));
	}
	
	/**
	 * Remove an element if the keys match and the values are equal.
	 * 
	 * @param value
	 * @param changed
	 * @return
	 */
	public KeyedSet<K,T> remove(final T value) {
		return delete(value.getKey(), new boolean[1]);
	}
	
	/**
	 * Remove an element if the keys match and the values are equal.
	 * 
	 * If the element is removed, the size of the returned tree will be
	 * less than the size of this tree.
	 * 
	 * @param value
	 * @param changed
	 * @return
	 */
	public KeyedSet<K,T> remove(final T value, final boolean[] removed) {
		removed[0] = false;
		
		return delete(value.getKey(), new DeleteTest<T>() {
			public boolean doDelete(T t) {
				return removed[0] = t.equals(value);
			}
			public boolean wasDeleted() {
				return removed[0];
			}
		});
	}
	
	protected KeyedSet<K,T> removeHigh(KeyedSet<K,T> node, KeyedSet<K,T> high) {
		if (node == high) {
			return node.left;
		}
		node = node.setRight(removeHigh(node.right, high));
		return balanceTree(node);
	}
	
	protected class TreeState {
		public static final int LEFT = 0;
		public static final int THIS = 1;
		public static final int RIGHT = 2;
		
		KeyedSet<K,T> tree;
		int visited;
		
		public TreeState(KeyedSet<K,T> tree, int visited) {
			this.tree = tree;
			this.visited = visited;
		}
	}
	
	// Sequence operations
	
	protected class TreeIterator implements Iterator<KeyedSet<K,T>> {

		KeyedSet<K,T> current;
		ConsList<TreeState> stack;
		
		public TreeIterator() {
			current = findLowest(KeyedSet.this);
		}
		
		protected void pushState(KeyedSet<K,T> node, int visited) {
			stack = new ConsList<TreeState>(new TreeState(node, visited), stack);
		}
		
		protected KeyedSet<K,T> findLowest(KeyedSet<K,T> node) {
			while (!node.left.isEmpty()) {
				pushState(node, TreeState.LEFT);
				node = node.left;
			}
			pushState(node, TreeState.THIS);
			return node;
		}
		
		protected KeyedSet<K,T> findNext() {
			while (true) {
				if (stack == null) {
					return null;
				}
				TreeState state = stack.head();
				stack = stack.tail();
				KeyedSet<K,T> node = state.tree;
				switch (state.visited) {
				case TreeState.LEFT:
					pushState(node, TreeState.THIS);
					return node;
				case TreeState.THIS:
					if (!node.right.isEmpty()) {
						return findLowest(node.right);
					}
					// fall through
				default: // TreeState.RIGHT
				}
			}
			
		}
		
		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public KeyedSet<K,T> next() {
			if (current == null) {
				throw new NoSuchElementException();
			}
			KeyedSet<K,T> tmp = current;
			current = findNext();
			return tmp;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	protected class TreeIterable implements Iterable<KeyedSet<K,T>> {
		@Override
		public Iterator<KeyedSet<K,T>> iterator() {
			return new TreeIterator();
		}
	}
	
	protected class ValueIterator implements Iterator<T> {

		TreeIterator it = new TreeIterator();
		
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public T next() {
			return it.next().member;
		}

		@Override
		public void remove() {
			it.remove();
		}
		
	}
	
	protected class ValueIterable implements Iterable<T> {
		@Override
		public Iterator<T> iterator() {
			return new ValueIterator();
		}
		
	}
	
	/**
	 * Return an iterator over the elements of the set in ascending order.
	 * Since the tree is immutable, the iterator will never
	 * throw ConcurrentModificationException.
	 * @return iterator
	 */
	public Iterator<T> iterator() {
		return new ValueIterator();
	}
	
	/**
	 * Return an iterable that can be used in foreach.
	 * It's iterator is the same as <code>iterator()</code>.
	 * @see #iterator()
	 * @return iterable
	 */
	public Iterable<T> elements() {
		return new ValueIterable();
	}
	
	/**
	 * Add all elements in iterable to tree.
	 * @param elements to add
	 * @return new tree containing the elements
	 */
	public KeyedSet<K,T> union(Iterable<T> elements) {
		KeyedSet<K,T> tree = this;
		for (T t : elements) {
			tree = tree.add(t);
		}
		return tree;
	}
	
	/**
	 * Add all elements in iterable to tree.
	 * 
	 * @param elements to add
	 * @return new tree containing the elements
	 */
	public KeyedSet<K,T> union(KeyedSet<K,T> other) {
		// bound is O(from.size*log(to.size)), where from smaller than to
		if (height > (other.height << 1)) {
			// height >> other.height - O(|other|*log(|this|))
			KeyedSet<K,T> set = this;
			for (T t : other) {
				set.add(t);
			}
			return set;
		} else if (other.height > (height << 1)) {
			// other.height >> height - O(|this|*log(|other|))
			KeyedSet<K,T> set = other;
			for (T t : this) {
				set.add(t);
			}
			return set;
		} else {
			// roughly the same size - O(|this|+|other|)
			List<T> a = toSortedList();
			List<T> b = other.toSortedList();
			return treeifyUnion(a, b);
		}
	}
	
	private KeyedSet<K,T> treeifyUnion(List<T> a, List<T> b) {
		// Initial capacity at most twice as large as necessary
		ArrayList<T> merged = new ArrayList<T>(a.size()+b.size());
		// merge
		int alen = a.size();
		int blen = b.size();
		int ai = 0;
		int bi = 0;
		int mi = 0;
		T at = ai < alen ? a.get(ai) : null;
		T bt = bi < blen ? b.get(bi) : null;
		while (at != null && bt != null) {
			int cmp = at.getKey().compareTo(bt.getKey());
			if (cmp <= 0) {
				merged.add(at);
				ai++;
				at = ai < alen ? a.get(ai) : null;
				if (cmp == 0) {
					bi++;
					bt = bi < blen ? b.get(bi) : null;
				}
			} else {
				merged.add(bt);
				bi++;
				bt = bi < blen ? b.get(bi) : null;
			}
		}
		while (ai < alen) {
			merged.add(a.get(ai++));
		}
		while (bi < blen) {
			merged.add(a.get(bi++));
		}
		// treeify
		return treeify(merged, 0, mi);
	}
	
	/**
	 * Convert the set to a sorted list of elements.
	 * 
	 * @return sorted list
	 */
	public List<T> toSortedList() {
		ArrayList<T> list = new ArrayList<T>(1 << height);
		toSortedList(list);
		return list;
	}
	
	private void toSortedList(ArrayList<T> list) {
		// recursive tree walk
		if (left != EMPTY) {
			left.toSortedList(list);
		}
		list.add(member);
		if (right != EMPTY) {
			right.toSortedList(list);
		}
	}
	
	/**
	 * Make a KeyedSet from a sorted list in linear time
	 * (O(n) vs. O(n log(n))).
	 * 
	 * @param sorted sorted list
	 * @return KeyedSet
	 */
	public KeyedSet<K,T> treeify(List<T> sorted) {
		return treeify(sorted, 0, sorted.size());
	}
	
	/**
	 * A sorted list is also a sorted heap.
	 * 
	 * <p>The range lo and hi is initially 0 and the size of the list, respectively.
	 * 
	 * <p>Make the midpoint of the range the tree element, the left subtree
	 * from the elements from lo to mid exclusive, and the right subtree
	 * from the elements from mid+1 to hi exclusive.
	 * 
	 * <p>For ranges with 3 or fewer elements, construct the tree directly.
	 * 
	 * @param sorted
	 * @param lo
	 * @param hi
	 * @return 
	 */
	private KeyedSet<K,T> treeify(List<T> sorted, int lo, int hi) {
		switch (hi - lo) {
			case 3:
				return newTree(sorted.get(lo+1),
						// left
						newTree(sorted.get(lo), EMPTY, EMPTY),
						// right
						newTree(sorted.get(lo+2), EMPTY, EMPTY));
			case 2:
				return newTree(sorted.get(lo+1),
						// left
						newTree(sorted.get(lo), EMPTY, EMPTY),
						// right
						EMPTY);
			case 1:
				return newTree(sorted.get(lo), EMPTY, EMPTY);
			case 0:
				return EMPTY;
			default:
				int mid = (lo + hi) >> 1;
				return newTree(sorted.get(mid),
						// left
						treeify(sorted, lo, mid),
						// right
						treeify(sorted, mid+1, hi));
		}
	}
		
	/**
	 * Subtract all elements in iterable from tree. If no elements
	 * are in iterable, return this.
	 * @param elements to add
	 * @return new tree containing the elements
	 */
	public KeyedSet<K,T> difference(Iterable<T> elements) {
		KeyedSet<K,T> tree = this;
		for (T t : elements) {
			tree = tree.remove(t);
		}
		return tree;
	}
	
	/**
	 * Remove all elements not in other tree. If all elements are
	 * in other, return this.
	 * @param other tree
	 * @return tree not containing elements not in other
	 */
	public KeyedSet<K,T> intersection(Iterable<T> other) {
		KeyedSet<K,T> tree = EMPTY;
		for (T t : other) {
			if (contains(t)) {
				tree.add(t);
			}
		}
		return tree;
	}
	
	/**
	 * Remove all elements not in other tree. If all elements are
	 * in other, return this.
	 * 
	 * @param other tree
	 * @return tree containing only elements also in other
	 */
	public KeyedSet<K,T> intersection(KeyedSet<K,T> other) {
		KeyedSet<K,T> tree = EMPTY;
		// bound is O(from.size*log(to.size)), where from smaller than to
		if (height > (other.height << 1)) {
			// height >> other.height - O(|other|*log(|this|))
			for (T t : other) {
				if (contains(t)) {
					tree = tree.add(t);
				}
			}
			return tree;
		} else if (other.height > (height << 1)) {
			// other.height >> height - O(|this|*log(|other|))
			for (T t : this) {
				if (other.contains(t)) {
					tree = tree.add(t);
				}
			}
			return tree;
		} else {
			// roughly the same size - O(|this|+|other|)
			List<T> a = toSortedList();
			List<T> b = other.toSortedList();
			return treeifyIntersection(a, b);
		}
	}
	
	private KeyedSet<K,T> treeifyIntersection(List<T> a, List<T> b) {
		// Initial capacity assures the list array will not grow.
		ArrayList<T> merged = new ArrayList<T>(Math.min(a.size(), b.size()));
		// merge
		int alen = a.size();
		int blen = b.size();
		int ai = 0;
		int bi = 0;
		int mi = 0;
		T at = ai < alen ? a.get(ai) : null;
		T bt = bi < blen ? b.get(bi) : null;
		while (at != null && bt != null) {
			int cmp = at.getKey().compareTo(bt.getKey());
			if (cmp == 0) {
				merged.add(at);
				ai++;
				at = ai < alen ? a.get(ai) : null;
				bi++;
				bt = bi < blen ? b.get(bi) : null;
			} else if (cmp < 0) {
				ai++;
				at = ai < alen ? a.get(ai) : null;
			} else {
				bi++;
				bt = bi < blen ? b.get(bi) : null;
			}
		}
		// treeify
		return treeify(merged, 0, mi);
	}
	
	/**
	 * Add all elements in elements to tree (union). If no elements in
	 * elements, return this.
	 * @param elements to add
	 * @param changed [0]
	 * @return possibly modified tree
	 */
	public KeyedSet<K,T> addAll(Collection<? extends T> elements) {
		KeyedSet<K,T> tree = this;
		for (T t : elements) {
			tree = tree.add(t);
		}
		return tree;
	}
	
	/** 
	 * Remove all in elements from the tree (difference). If no
	 * elements in elements, return this.
	 * 
	 * @param elements to remove
	 * @return tree containing no elements in elements
	 */
	public KeyedSet<K,T> removeAll(Collection<? extends T> elements) {
		KeyedSet<K,T> tree = this;
		for (Object o : elements) {
			tree = tree.remove((T) o);
		}
		return tree;
	}
	
	/**
     * Remove all elements not in the elements argument (intersection). If all elements in
     * tree are also in elements, return this.
     * 
	 * @param elements to test
	 * @return tree containing only original elements that are also
	 * in the elements
	 * @throws ClassCastException if elements are not of type T
	 */
	public KeyedSet<K,T> retainAll(Collection<? extends T> elements) {
		KeyedSet<K,T> tree = this;
		for (T t : this) {
			if (!elements.contains(t)) {
				tree = tree.remove(t);
			}
		}
		return tree;
	}
	
	public static interface Tester<T> {
		/**
		 * Return true if test passes.
		 * @param element
		 * @return
		 */
		public boolean test(T element);
	}
	
	/**
	 * Filter the tree, removing all elements for which the
	 * <code>test</code> method does not return true. If all
	 * elements pass test, return this.
	 * @param f Test
	 * @return filtered tree
	 */
	public KeyedSet<K,T> filter(Tester f) {
		KeyedSet<K,T> tree = this;
		for (KeyedSet<K,T> t : new TreeIterable()) {
			if (!f.test(t.member)) {
				tree = tree.remove(t.member);
			}
		}
		return tree;
	}
	
	public static interface Mapper<T> {
		/**
		 * Map element to another element of same type.
		 * @param t
		 * @return
		 */
		public T map(T t);
	}
	
	/**
	 * Map the elements of the tree according to the specified
	 * <code>map</code> method. Does not modify the original tree.
	 * @param f Mapper
	 * @return new tree with mapped elements
	 */
	public KeyedSet<K,T> map(Mapper<T> f) {
		KeyedSet<K,T> tree = this;
		KeyedSet<K,T> other = EMPTY;
		for (T t : this) {
			other.add(f.map(t));
		}
		return other;
	}
	
	public static interface Selector<T> {
		/**
		 * Add selected elements to argument collection.
		 * @param collection
		 * @param element to add or not
		 */
		public void select(Collection<T> collection, T element);
	}
	
	/**
	 * Add selected elements to collection
	 * @param collection target
	 * @param f Selector
	 */
	public void select(Collection<T> collection, Selector f) {
		for (T t : elements()) {
			f.select(collection, t);
		}
	}
	
	/**
	 * Add all elements to collection.
	 * @param collection target
	 */
	public void collect(Collection<T> collection) {
		for (T t : elements()) {
			collection.add(t);
		}
	}
	
	/**
	 * @param elements
	 * @return true if tree contains all elements in elements;
	 * otherwise false
	 */
	public boolean containsAll(Collection<?> elements) {
		for (Object o : elements) {
			if (!contains((T) o)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param elements
	 * @return true if tree contains any elements in elements;
	 * otherwise false
	 */
	public boolean containsAny(Collection<?> elements) {
		for (Object o : elements) {
			if (contains((T) o)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param elements
	 * @return true if tree contains all elements in elements;
	 * otherwise false
	 */
	public boolean containsAll(KeyedSet<K,T> elements) {
		for (Object o : elements) {
			if (!contains((T) o)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param elements
	 * @return true if tree contains any elements in elements;
	 * otherwise false
	 */
	public boolean containsAny(KeyedSet<K,T> elements) {
		for (Object o : elements) {
			if (contains((T) o)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Vacuous method for compatibility.
	 * @return empty tree
	 */
	public KeyedSet<K,T> clear() {
		return EMPTY;
	}
	
	@Override
	public String toString() {
		return "("+left.toString()+member.toString()+right.toString()+")";
	}
	
	public void prettyPrint() {
		prettyPrint("");
	}
	
	protected void prettyPrint(String indent) {
		if (!right.isEmpty()) {
			right.prettyPrint(indent+"  ");
		}
		System.out.println(indent+member+"-"+getHeight());
		if (!left.isEmpty()) {
			left.prettyPrint(indent+"  ");
		}
	}
	
	/**
	 * Compare two KeyedSets.
	 * 
	 * This is an O(n) operation.
	 * 
	 * @param o
	 * @return true if both are KeyedSets and have the same number of elements
	 * and all elements are equal
	 */
	public boolean equals(Object o) {
		if (!(o instanceof KeyedSet)) {
			return false;
		}
		Iterator otherIt = ((KeyedSet)o).iterator();
		Iterator it = iterator();
		while (it.hasNext()) {
			if (!otherIt.hasNext() || !otherIt.next().equals(it.next())) {
				return false;
			}
		}
		return !otherIt.hasNext();
	}
	
	/**
	 * Get the hash code for the set.
	 * 
	 * This is an O(n) operation.
	 * 
	 * @return hash sum of all elements
	 */
	public int hashCode() {
		// caching the hash code would take up space in every node
		int hash = 0;
		for (T t : this) {
			hash = hash*31 + t.hashCode();
		}
		return hash;
	}
	
	protected static class Empty<T extends Keyed<K>, K extends Comparable<K>> extends KeyedSet<K,T> {
		protected Empty() {
			super(null, EMPTY, EMPTY);
		}
		@Override
		protected void init() {
			height = 0;
		}
		@Override
		public boolean containsKey(K key) {
			return false;
		}
		@Override
		public T get(K key) {
			return null;
		}
		@Override
		public T getFirst() {
			return null;
		}
		@Override
		public T getLast() {
			return null;
		}
		@Override
		public KeyedSet<K,T> delete(K key) {
			return this;
		}
		@Override
		protected KeyedSet<K,T> delete(K key, DeleteTest<T> test) {
			return this;
		}
		@Override
		public KeyedSet<K,T> deleteFirst() {
			return this;
		}
		@Override
		public KeyedSet<K,T> deleteLast() {
			return this;
		}
		@Override
		public boolean contains(T value) {
			return false;
		}
		@Override
		public boolean isEmpty() {
			return true;
		}
		@Override
		protected int getBalance() {
			return 0;
		}
		@Override
		boolean isBalanced() {
			return true;
		}
		@Override
		boolean isOrdered() {
			return true;
		}
		@Override
		public KeyedSet<K,T> add(T element, boolean[] added) {
			added[0] = true;
			return newTree(element, EMPTY, EMPTY);
		}
		@Override
		protected KeyedSet<K,T> addElement(T element, boolean[] added) {
			return add(element, added);
		}
		@Override
		public KeyedSet<K,T> remove(T value) {
			return this;
		}
		@Override
		protected int getHeight() {
			return 0;
		}
		@Override
		public int size() {
			return 0;
		}
		@Override
		public Object seq(final Receiver<T> receiver) {
			receiver.close();
			return receiver.value();
		}
		@Override
		public Object rev(Receiver<T> receiver) {
			receiver.close();
			return receiver.value();
		}
		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return false;
				}
				@Override
				public T next() {
					throw new NoSuchElementException();
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		@Override
		public String toString() {
			return "()";
		}
	}
	public static final KeyedSet EMPTY = new Empty();
	static {
		EMPTY.left = EMPTY;
		EMPTY.right = EMPTY;
	}
	
}
