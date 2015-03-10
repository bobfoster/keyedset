/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genantics.list;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.genantics.access.ConditionalVisitor;
import org.genantics.access.DefaultIterator;
import org.genantics.access.Visitor;

/**
 *
 * @author Bob Foster
 */
public class Nil<V> implements CList<V> {
	
	static final Nil NIL = new Nil();
	
	private Nil() {
	}

	public CList<V> cons(V element) {
		return new CodedList<V>(element, this);
	}
	
	public V head() {
		throw new NoSuchElementException();
	}

	public CList<V> tail() {
		return NIL;
	}

	public CList<V> removeFirst() {
		return NIL;
	}

	public boolean contains(V element) {
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
	}

	public boolean visit(ConditionalVisitor<V> visitor) {
		return true;
	}

	public void reverseVisit(Visitor<V> visitor) {
	}

	public boolean reverseVisit(ConditionalVisitor<V> visitor) {
		return false;
	}

	public void toCollection(Collection<V> collection) {
	}

	public boolean isEmpty() {
		return true;
	}

	public int size() {
		return 0;
	}
}
