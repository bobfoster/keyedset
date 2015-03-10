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

package org.genantics.set;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.DefaultIterator;
import org.genantics.access.Filter;
import org.genantics.access.Visitor;

/**
 * Immutable set based on AVL tree. Any operation that changes the set
 * returns a new set that shares structure with the original set.
 * 
 * <p>KeyedSet does not implement java.util.Set, as the interface
 * is not well-suited to an immutable set, but provides the common
 * set operations and a number of map-like operations.
 * 
 * Null values are not allowed.
 * 
 * The <code>containsAll</code>, <code>containsAny</code>, <code>addAll</code>,
 * <code>removeAll</code> and <code>retainAll</code> could be faster if
 * methods specialized to KeyedSet were added.
 * 
 * @author Bob Foster
 */
public class KeyedSet<K extends Comparable<K>, T extends Keyed<K>> implements Iterable<T> {

	protected T member;
	protected KeyedSet<K,T> left;
	protected KeyedSet<K,T> right;
	protected int height;
	
	/**
	 * Construct set from iterable.
	 * 
	 * @param iterable elements of new set
	 */
	public KeyedSet(Iterable<T> iterable) {
		KeyedSet<K,T> set = EMPTY;
		for (T t : iterable) {
			set = set.add(t);
		}
		copy(set);
	}
	
	private void copy(KeyedSet<K,T> set) {
		this.member = set.member;
		this.left = set.left;
		this.right = set.right;
		this.height = set.height;
	}
	
	/**
	 * Constructor. Not intended to be directly called by users.
	 * 
	 * @param element value of node
	 * @param left left subtree
	 * @param right right subtree
	 */
	protected KeyedSet(T element, KeyedSet<K,T> left, KeyedSet<K,T> right) {
		this.member = element;
		this.left = left;
		this.right = right;
		init();
	}
	
	/**
	 * Function that wraps constructor. Might be overridden by subclass.
	 * @param element value of node
	 * @param left left subtree
	 * @param right right subtree
	 * @return new tree
	 */
	protected KeyedSet<K,T> newTree(T element, KeyedSet<K,T> left, KeyedSet<K,T> right) {
		return new KeyedSet<K,T>(element, left, right);
	}
	
	protected void init() {
		height = Math.max(left.height, right.height) + 1;
	}
	
	// for testing
	int getHeight() {
		return height;
	}
	
	/**
	 * Compare sets. This is an O(n) operation.
	 * 
	 * @param o object
	 * @return true if both sets contain the same number of elements and for
	 * each pairwise element equals returns true; otherwise, false
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof KeyedSet)) {
			return false;
		}
		if (o == EMPTY) {
			return false;
		}
		try {
			KeyedSet<K,T> other = (KeyedSet<K,T>) o;
			return equalElements(iterator(), other.iterator());
		} catch (ClassCastException unused) {
			return false;
		} catch (NullPointerException unused) {
			return false;
		}
	}
	
	private boolean equalElements(Iterator<T> ita, Iterator<T> itb) {
		while (true) {
			boolean isa = ita.hasNext();
			boolean isb = itb.hasNext();
			if (isa != isb) {
				return false;
			}
			if (!isa) {
				return true;
			}
			T ta = ita.next();
			T tb = itb.next();
			if (!ta.equals(tb)) {
				return false;
			}
		}
	}
	
	/**
	 * Get the hashCode for the set. This is an O(n) operation.
	 * 
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		HashVisitor visitor = new HashVisitor();
		visitFwd(visitor);
		return visitor.value();
	}
	
	private class HashVisitor implements Visitor<T> {
		int hash = 0;

		public void visit(T t) {
			hash = hash*31 + t.hashCode();
		}
		
		public int value() {
			return hash;
		}
	}
	
	/**
	 * Get the tree size. Note this is an O(n) operation.
	 * Use <code>isEmpty()</code> rather than <code>getSize() == 0</code>.
	 * 
	 * @return the number of elements in the set
	 */
	public int size() {
		return left.size() + 1 + right.size();
	}
	
	/**
	 * Test if tree contains the specified key.
	 * 
	 * @param key
	 * @return true if key matched in tree; otherwise, false
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
	
	private class KeyIterator extends DefaultIterator<K> {
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
		
	}
	
	private class KeyIterable implements Iterable {

		public Iterator<K> iterator() {
			return new KeyIterator(KeyedSet.this.iterator());
		}
		
	}
	
	/**
	 * Get an iterable that can be used in foreach to obtain keys
	 * in ascending order.
	 * 
	 * @return iterable
	 */
	public Iterable<K> keys() {
		return new KeyIterable();
	}
	
	private class ReverseKeyIterable implements Iterable {

		public Iterator<K> iterator() {
			return new KeyIterator(KeyedSet.this.reverseIterator());
		}
		
	}
	
	/**
	 * Get an iterable that can be used in foreach to obtain keys
	 * in descending order.
	 * 
	 * @return iterable
	 */
	public Iterable<K> reverseKeys() {
		return new ReverseKeyIterable();
	}
	
	protected int getBalance() {
		return left.height - right.height;
	}
	
	/** Test tree balance - for testing */
	boolean isBalanced() {
		int balance = getBalance();
		boolean balanced = balance >= -1 && balance <= 1 && left.isBalanced() && right.isBalanced();
		return balanced;
	}
	
	/** Test tree order - for testing */
	boolean isOrdered() {
		K memberKey = member.getKey();
		return isEmpty() ||
		       ((left.isEmpty() || left.member.getKey().compareTo(memberKey) < 0 && left.isOrdered()) &&
			   (right.isEmpty() || memberKey.compareTo(right.member.getKey()) < 0 && right.isOrdered()));
	}

	protected KeyedSet<K,T> setLeft(KeyedSet<K,T> left) {
		return new KeyedSet<K,T>(member, left, right);
	}

	protected KeyedSet<K,T> setRight(KeyedSet<K,T> right) {
		return new KeyedSet<K,T>(member, left, right);
	}

	public boolean contains(T value) {
		if (value == null) {
			return false;
		}
		return containsKey(value);
	}
	
	protected boolean containsKey(T value) {
		if (value.equals(member)) {
			return true;
		}
		int cmp = value.getKey().compareTo(member.getKey());
		if (cmp < 0) {
			return left.containsKey(value);
		} else if (cmp > 0) {
			return right.containsKey(value);
		} else {
			return value.equals(member);
		}
	}
	
	/**
	 * @return true if set is empty; otherwise false
	 */
	public boolean isEmpty() {
		return false;
	}
	
	/**
	 * Add an element to the set.
	 * @param element
	 * @return new tree containing element
	 */
	public KeyedSet<K,T> add(T element) {
		boolean[] added = new boolean[2];
		return addElement(element, added);
	}
	
	/**
	 * Convenience method for java.util.Set, which returns true
	 * if the element was not already present in the tree.
	 * @param element to add
	 * @param added on return, added[0] true if element added
	 * and set size increased by 1; otherwise false; added[1]
	 * true if element with matching key was replaced (set size
	 * unchange); otherwise false; if neither are true, the element
	 * was already in the set
	 * @return set containing element
	 */
	public KeyedSet<K,T> add(T element, boolean[] added) {
		if (element == null) throw new NullPointerException();
		added[0] = added[1] = false;
		return addElement(element, added);
	}
	
	/** addEntry returns this tree with added element */
	protected KeyedSet<K,T> addElement(T element, boolean[] added) {
		int cmp = element.getKey().compareTo(member.getKey());
		KeyedSet<K,T> node = null;
		if (cmp < 0) {
			KeyedSet<K,T> newLeft = left.addElement(element, added);
			if (!(added[0] || added[1])) {
				return this;
			}
			node = newTree(member, newLeft, right);
		} else if (cmp > 0) {
			KeyedSet<K,T> newRight = right.addElement(element, added);
			if (!(added[0] || added[1])) {
				return this;
			}
			node = newTree(member, left, newRight);
		} else {
			// We allow for the Comparable to compare only part
			// of the element, e.g., a key, so equals can return
			// a different result.
			if (!element.equals(member)) {
				added[1] = true;
				return newTree(element, left, right);
			}
			// element already in set
			return this;
		}
		return balanceTree(node);
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
	
	/**
	 * Delete the element with matching key.
	 * 
	 * If an element is deleted, the size of the returned tree will be
	 * one less than this.
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
	 * If an element is deleted, the size of the returned tree will be
	 * one less than this.
	 * 
	 * @param key
	 * @param deleted deleted[0] true if the size of the tree reduced by 1;
	 * otherwise false
	 * @return new tree without matching element, if any
	 */
	public KeyedSet<K,T> delete(K key, boolean[] deleted) {
		deleted[0] = false;
		return deleteKey(key, deleted);
	}
	
	protected KeyedSet<K,T> deleteKey(K key, boolean[] deleted) {
		int cmp = key.compareTo(member.getKey());
		KeyedSet<K,T> node = this;
		if (cmp < 0) {
			KeyedSet<K,T> newLeft = left.deleteKey(key, deleted);
			if (!deleted[0]) {
				return node;
			}
			node = newTree(member, newLeft, right);
		} else if (cmp > 0) {
			KeyedSet<K,T> newRight = right.deleteKey(key, deleted);
			if (!deleted[0]) {
				return node;
			}
			node = newTree(member, left, newRight);
		} else {
			deleted[0] = true;
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
	 * If the element is removed, the size of the returned tree will be
	 * less than the size of this tree.
	 * 
	 * @param element
	 * @return set with element removed
	 */
	public KeyedSet<K,T> remove(T element) {
		return removeElement(element, new boolean[1]);
	}

	/**
	 * Remove an element if the keys match and the values are equal.
	 * 
	 * If the element is removed, the size of the returned tree will be
	 * less than the size of this tree.
	 * 
	 * @param element
	 * @param removed on return, removed[0] true if element removed and
	 * set size reduced by 1; otherwise, false
	 * @return
	 */
	public KeyedSet<K,T> remove(T element, boolean[] removed) {
		removed[0] = false;
		return removeElement(element, removed);
	}
	
	protected KeyedSet<K,T> removeElement(T element, boolean[] removed) {
		// TODO this duplicates most of delete(key) - see if they can be combined
		int cmp = element.getKey().compareTo(member.getKey());
		KeyedSet<K,T> node = this;
		if (cmp < 0) {
			KeyedSet<K,T> newLeft = left.removeElement(element, removed);
			if (!removed[0]) {
				return node;
			}
			node = newTree(member, newLeft, right);
		} else if (cmp > 0) {
			KeyedSet<K,T> newRight = right.removeElement(element, removed);
			if (!removed[0]) {
				return node;
			}
			node = newTree(member, left, newRight);
		} else {
			if (!element.equals(member)) {
				return node;
			}
			removed[0] = true;
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
	
	protected KeyedSet<K,T> removeHigh(KeyedSet<K,T> node, KeyedSet<K,T> high) {
		if (node == high) {
			return node.left;
		}
		node = node.setRight(removeHigh(node.right, high));
		return balanceTree(node);
	}
	
	protected class TreeState {
		public static final int NONE = 0;
		public static final int LEFT = 1;
		public static final int THIS = 2;
		public static final int RIGHT = 3;
		
		KeyedSet<K,T> tree;
		int visited;
		
		public TreeState(KeyedSet<K,T> tree, int visited) {
			this.tree = tree;
			this.visited = visited;
		}
	}
	
	class TreeIterator implements Iterator<KeyedSet<K,T>> {

		KeyedSet<K,T> current;
		int visited;
		Stack<TreeState> stack = new Stack<TreeState>();
		
		public TreeIterator() {
			visited = TreeState.NONE;
			current = loopTraversal(KeyedSet.this);
		}
		
		/**
		 * Implements the recursive generator schema:
		 * <pre>
		 * 	 if (node.left != EMPTY) {
		 * 	   traversal(node.left);
		 * 	 }
		 * 	 yield node.member;
		 * 	 if (node.right != EMPTY) {
		 * 	   traversal(node.right);
		 * 	 }

		 * </pre>
		 * @param node top of tree or last node returned
		 * @return next node containing member of interest or null if none
		 */
		protected KeyedSet<K,T> loopTraversal(KeyedSet<K,T> node) {
			for (;;) {
				switch (visited) {
				case TreeState.NONE:
					if (node.left != EMPTY) {
						stack.push(new TreeState(node, TreeState.LEFT));
						node = node.left;
						visited = TreeState.NONE;
						continue;
					}
					// fall through
				case TreeState.LEFT:
					visited = TreeState.THIS;
					return node;
				case TreeState.THIS:
					if (node.right != EMPTY) {
						stack.push(new TreeState(node, TreeState.RIGHT));
						node = node.right;
						visited = TreeState.NONE;
						continue;
					}
					// fall through
				case TreeState.RIGHT:
					if (stack.isEmpty()) {
						return null;
					} else {
						TreeState treeState = stack.pop();
						node = treeState.tree;
						visited = treeState.visited;
						continue;
					}
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
			current = loopTraversal(current);
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
	
	TreeIterator treeIterator() {
		return new TreeIterator();
	}
	
	protected class ReverseTreeIterator implements Iterator<KeyedSet<K,T>> {

		KeyedSet<K,T> current;
		int visited;
		Stack<TreeState> stack = new Stack<TreeState>();
		
		public ReverseTreeIterator() {
			visited = TreeState.NONE;
			current = loopTraversal(KeyedSet.this);
		}
		
		/**
		 * Implements the recursive generator schema:
		 * <pre>
		 * 	 if (node.right != EMPTY) {
		 * 	   traversal(node.right);
		 * 	 }
		 * 	 yield node.member;
		 * 	 if (node.left != EMPTY) {
		 * 	   traversal(node.left);
		 * 	 }
		 * </pre>
		 * @param node top of tree or last node returned
		 * @return next node containing member of interest or null if none
		 */
		protected KeyedSet<K,T> loopTraversal(KeyedSet<K,T> node) {
			for (;;) {
				switch (visited) {
				case TreeState.NONE:
					if (node.right != EMPTY) {
						stack.push(new TreeState(node, TreeState.RIGHT));
						node = node.right;
						visited = TreeState.NONE;
						continue;
					}
					// fall through
				case TreeState.RIGHT:
					visited = TreeState.THIS;
					return node;
				case TreeState.THIS:
					if (node.left != EMPTY) {
						stack.push(new TreeState(node, TreeState.LEFT));
						node = node.left;
						visited = TreeState.NONE;
						continue;
					}
					// fall through
				case TreeState.LEFT:
					if (stack.isEmpty()) {
						return null;
					} else {
						TreeState treeState = stack.pop();
						node = treeState.tree;
						visited = treeState.visited;
						continue;
					}
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
			current = loopTraversal(current);
			return tmp;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	protected class ReverseTreeIterable implements Iterable<KeyedSet<K,T>> {
		@Override
		public Iterator<KeyedSet<K,T>> iterator() {
			return new ReverseTreeIterator();
		}
	}
	
	protected class ReverseValueIterator implements Iterator<T> {

		ReverseTreeIterator it = new ReverseTreeIterator();
		
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
	
	protected class ReverseValueIterable implements Iterable<T> {
		@Override
		public Iterator<T> iterator() {
			return new ReverseValueIterator();
		}
		
	}
	
	/**
	 * Return an iterator over the elements of the set in descending order.
	 * Since the tree is immutable, the iterator will never
	 * throw ConcurrentModificationException.
	 * @return iterator
	 */
	public Iterator<T> reverseIterator() {
		return new ReverseValueIterator();
	}
	
	/**
	 * Return an iterable that can be used in foreach to obtain elements
	 * in ascending order.
	 * @see #iterator()
	 * @return iterable
	 */
	public Iterable<T> elements() {
		return new ValueIterable();
	}
	
	/**
	 * Return an iterable that can be used in foreach to obtain elements
	 * in descending order.
	 * @see #reverseIterator()
	 * @return iterable
	 */
	public Iterable<T> reverseElements() {
		return new ReverseValueIterable();
	}
	
	/**
	 * Add all elements in iterable to set.
	 * @param iterable elements to add
	 * @return set + iterable elements
	 */
	public KeyedSet<K,T> addAll(Iterable<T> iterable) {
		KeyedSet<K,T> tree = this;
		for (T t : iterable) {
			tree = tree.add(t);
		}
		return tree;
	}
	
	/**
	 * Remove all elements in iterable from tree.
	 * 
	 * @param iterable elements to remove
	 * @return set - iterable elements
	 */
	public KeyedSet<K,T> removeAll(Iterable<T> iterable) {
		KeyedSet<K,T> tree = this;
		boolean[] removed = new boolean[1];
		for (T t : iterable) {
			tree = tree.remove(t, removed);
		}
		return tree;
	}
	
	/**
	 * Get all elements in both set and iterable.
	 * 
	 * @param iterable
	 * @return {element} such that element in set and element in iterable
	 */
	public KeyedSet<K,T> retainAll(Iterable<T> iterable) {
		KeyedSet<K,T> tree = EMPTY;
		for (T t : iterable) {
			if (contains(t)) {
				tree = tree.add(t);
			}
		}
		return tree;
	}
	
	/**
	 * Visit all elements in order.
	 * 
	 * @param visitor visits each element
	 */
	public void visitFwd(Visitor visitor) {
		left.visitFwd(visitor);
		visitor.visit(member);
		right.visitFwd(visitor);
	}
	
	/**
	 * Visit elements in order.
	 * 
	 * @param visitor visits each element, returning false if next element
	 * is to be visited; otherwise true (desired/last element found)
	 * @return false if all elements visited; otherwise, true
	 */
	public boolean visitFwd(ConditionalVisitor visitor) {
		return left.visitFwd(visitor)
				|| visitor.visit(member)
				|| right.visitFwd(visitor);
	}
	
	/**
	 * Visit all elements in reverse order.
	 * 
	 * @param visitor visits each element
	 */
	public void visitRev(Visitor visitor) {
		right.visitRev(visitor);
		visitor.visit(member);
		left.visitRev(visitor);
	}
	
	/**
	 * Visit all elements in reverse order.
	 * 
	 * @param visitor visits each element, returning false if next element
	 * is to be visited; otherwise true (desired element found)
	 * @return false if all elements visited; otherwise true
	 */
	public boolean visitRev(ConditionalVisitor visitor) {
		return right.visitRev(visitor)
				|| visitor.visit(member)
				|| left.visitRev(visitor);
	}
	
	private class FilterVisitor implements Visitor<T> {
		KeyedSet<K,T> set = EMPTY;
		private final Filter<T> filter;
		
		public FilterVisitor(Filter<T> filter) {
			this.filter = filter;
		}
		
		public void visit(T t) {
			if (filter.accept(t)) {
				set = set.add(t);
			}
		}
		
		public KeyedSet<K,T> value() {
			return set;
		}
	}
	
	/**
	 * Filter the set, removing all elements for which the
	 * <code>test</code> method returns false.
	 * 
	 * @param filter
	 * @return filtered tree
	 */
	public KeyedSet<K,T> filter(Filter filter) {
		FilterVisitor visitor = new FilterVisitor(filter);
		visitFwd(visitor);
		return visitor.value();
	}
	
	private class Collector implements Visitor<T> {
		private final Collection<T> collection;
		
		public Collector(Collection<T> collection) {
			this.collection = collection;
		}

		public void visit(T t) {
			collection.add(t);
		}
	}
	
	/**
	 * Add all elements to collection.
	 * @param collection target
	 */
	public void collect(Collection<T> collection) {
		Collector visitor = new Collector(collection);
		visitFwd(visitor);
	}
	
	/**
	 * @param elements
	 * @return true if set contains all elements in elements;
	 * otherwise false
	 */
	public boolean containsAll(Iterable<T> elements) {
		for (T t : elements) {
			if (!contains(t)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param elements
	 * @return true if set contains any elements in elements;
	 * otherwise false
	 */
	public boolean containsAny(Iterable<T> elements) {
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
		System.out.println(indent+member.toString()+"-"+height);
		if (!left.isEmpty()) {
			left.prettyPrint(indent+"  ");
		}
	}
	
	/**
	 * Empty is a singleton object representing the empty set.
	 * 
	 * The type parameters really have no meaning for Empty; it is applicable
	 * to all types.
	 */
	protected static class Empty<T extends Keyed<K>, K extends Comparable<K>> extends KeyedSet<K,T> {
		protected Empty() {
			super(null, EMPTY, EMPTY);
		}
		protected void init() {
		}
		// for sanity, these are in the same order as in KeyedSet
		// init and getHeight skipped
		@Override
		public boolean equals(Object o) {
			return o == EMPTY;
		}
		@Override
		public int hashCode() {
			return 0;
		}
		@Override
		public int size() {
		  return 0;
		}
		@Override
		public boolean containsKey(K key) {
			return false;
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
		public KeyedSet<K,T> deleteFirst() {
			return this;
		}
		@Override
		public KeyedSet<K,T> deleteLast() {
			return this;
		}
		// getKeys and getBalance skipped
		@Override
		boolean isBalanced() {
		  return true;
		}
		@Override
		boolean isOrdered() {
		  return true;
		}
		// setLeft, setRight, contains skipped
		@Override
		protected boolean containsKey(T value) {
			return false;
		}
		@Override
		public boolean isEmpty() {
			return true;
		}
		// add, add skipped
		@Override
		protected KeyedSet<K,T> addElement(T element, boolean[] added) {
			added[0] = true;
			return newTree(element, EMPTY, EMPTY);
		}
		@Override
		public T get(K key) {
			return null;
		}
		// delete, delete skipped
		@Override
		protected KeyedSet<K,T> deleteKey(K key, boolean[] deleted) {
			return this;
		}
		// balanceTree, rotateLeft, rotateRight, remove, remove skipped
		@Override
		protected KeyedSet<K,T> removeElement(T value, boolean[] removed) {
		  return this;
		}
		// removeHigh skipped
		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				public boolean hasNext() {
					return false;
				}
				public T next() {
					throw new NoSuchElementException();
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		@Override
		public Iterator<T> reverseIterator() {
			return iterator();
		}
		@Override
		public Iterable<T> elements() {
			return this;
		}
		@Override
		public Iterable<T> reverseElements() {
			return this;
		}
		// addAll skipped
		@Override
		public KeyedSet<K,T> removeAll(Iterable<T> iterable) {
		  return this;
		}
		@Override
		public KeyedSet<K,T> retainAll(Iterable<T> iterable) {
		  return this;
		}
		@Override
		public void visitFwd(Visitor visitor) {
		}
		@Override
		public boolean visitFwd(ConditionalVisitor visitor) {
			return false;
		}
		@Override
		public void visitRev(Visitor visitor) {
		}
		@Override
		public boolean visitRev(ConditionalVisitor visitor) {
			return false;
		}
		@Override
		public KeyedSet<K,T> filter(Filter filter) {
		  return this;
		}
		@Override
		public void collect(Collection<T> collection) {
		}
		@Override
		public boolean containsAll(Iterable<T> elements) {
		  return !elements.iterator().hasNext();
		}
		@Override
		public boolean containsAny(Iterable<T> elements) {
		  return false;
		}
		// clear skipped
		@Override
		public String toString() {
			return "()";
		}
		@Override
		public void prettyPrint() {
		}
	}
	public static final KeyedSet EMPTY = new Empty();
	static {
		EMPTY.left = EMPTY;
		EMPTY.right = EMPTY;
		EMPTY.height = 0;
	}
	
}
