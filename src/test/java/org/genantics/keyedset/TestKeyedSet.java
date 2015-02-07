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
import java.util.HashSet;
import junit.framework.TestCase;
import org.genantics.keyedset.KeyedSet.Tester;

/**
 *
 * @author Bob Foster
 */
public class TestKeyedSet extends TestCase {
	
	private static class Element<K extends Comparable<K>,T> implements Keyed<K> {
		private final K key;
		private final T value;
		
		public Element(K key, T value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		/**
		 * @return the value
		 */
		public T getValue() {
			return value;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof Element)) {
				return false;
			}
			Element<Integer,String> other = (Element<Integer,String>) o;
			return key.equals(other.key) && value.equals(other.value);
		}
	}
	
	private Element<Integer,String> makeElement(int i) {
		return new Element(i, Integer.toString(i));
	}
	
	private KeyedSet<Integer,Element<Integer,String>> makeElements(int n, int by) {
		KeyedSet<Integer,Element<Integer,String>> set = KeyedSet.EMPTY;
		assertTrue(n > 0);
		assertTrue(by != 0);
		if (by > 0) {
			for (int i = 0; i < n; i += by) {
				set = set.add(makeElement(i));
			}
		} else {
			for (int i = n-1; i >= 0; i += by) {
				set = set.add(makeElement(i));
			}
		}
		return set;
	}
	
	private void checkHeight(KeyedSet set) {
		assertTrue(set.getHeight() <= Integer.highestOneBit(set.size()));
	}
	
	// NB: does not implement equals
	public static class WrapInt implements Keyed<Integer> {

		private int i;
		
		public WrapInt(int i) {
			this.i = i;
		}
		
		public Integer getKey() {
			return i;
		}
	}
	
	private WrapInt makeWrap(int i) {
		return new WrapInt(i);
	}
	
	private KeyedSet<Integer,WrapInt> makeWraps(int n, int by) {
		KeyedSet<Integer,WrapInt> set = KeyedSet.EMPTY;
		assertTrue(n > 0);
		assertTrue(by != 0);
		if (by > 0) {
			for (int i = 0; i < n; i += by) {
				set = set.add(makeWrap(i));
			}
		} else {
			for (int i = n-1; i >= 0; i += by) {
				set = set.add(makeWrap(i));
			}
		}
		return set;
	}
	
	// Test constructors
	
	public void testNewFromOtherSet() {
		KeyedSet<Integer,Element<Integer,String>> seta = makeElements(100, 1);
		KeyedSet<Integer,Element<Integer,String>> setb = new KeyedSet<Integer,Element<Integer,String>>(seta);
		assertTrue(seta.size() == setb.size());
		for (Element<Integer,String> e : seta) {
			assertTrue(setb.contains(e));
		}
		for (Element<Integer,String> e : setb) {
			assertTrue(seta.contains(e));
		}
		seta = seta.remove(makeElement(50));
		assertTrue(seta.size() != setb.size());
	}
	
	public void testNewFromIterable() {
		ArrayList<Element<Integer,String>> primes = primeElements(100);
		KeyedSet<Integer,Element<Integer,String>> set = new KeyedSet<Integer,Element<Integer,String>>(primes);
		assertTrue(primes.size() == set.size());
		for (Element<Integer,String> p : primes) {
			assertTrue(set.contains(p));
		}
	}
	
	// Each method is tested in alphabetical order
	
	public void testAdd() {
		KeyedSet<Integer,Element<Integer,String>> set = makeElements(100, 1);
		assertTrue(set.size() == 100);
		for (int i = 0; i < 100; i++) {
			assertTrue(set.containsKey(i));
		}
		try {
			set = set.add(null);
			assertTrue(false);
		} catch (NullPointerException e) {
		}
	}
	
	public void testAddPredicate() {
		KeyedSet<Integer,WrapInt> set = KeyedSet.EMPTY;
		boolean[] added = new boolean[2];
		assertTrue(!added[0]);
		set = set.add(new WrapInt(1), added);
		assertTrue(added[0]);
		assertFalse(added[1]);
		// WrapInt does not implement equals, so any new element will be added
		set = set.add(new WrapInt(1), added);
		assertFalse(added[0]);
		assertTrue(added[1]);
		set = set.add(new WrapInt(2), added);
		assertTrue(added[0]);
		assertFalse(added[1]);
		set = set.add(new WrapInt(2), added);
		assertFalse(added[0]);
		assertTrue(added[1]);
		// however
		WrapInt w = set.get(2);
		set = set.add(w, added);
		assertFalse(added[0]);
		assertFalse(added[1]);
		
		boolean[] wrong = new boolean[0];
		try {
			set = set.add(null);
			assertTrue(false);
		} catch (NullPointerException e) {
		}
		try {
			set = set.add(new WrapInt(2), wrong);
			assertTrue(false);
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		
		// We get different behavior with a class that implements equals
		KeyedSet<Integer,Element<Integer,String>> eset = KeyedSet.EMPTY;
		added[0] = false;
		eset = eset.add(makeElement(1), added);
		assertTrue(added[0]);
		assertFalse(added[1]);
		eset = eset.add(makeElement(1), added);
		assertFalse(added[0]);
		assertFalse(added[1]);
	}
	
	public void testSize() {
		KeyedSet<Integer,WrapInt> set = KeyedSet.EMPTY;
		assertTrue(set.size() == 0);
		set = set.add(new WrapInt(0));
		assertTrue(set.size() == 1);
		set = set.add(new WrapInt(1));
		assertTrue(set.size() == 2);
		set = set.add(new WrapInt(-1));
		assertTrue(set.size() == 3);
		for (int i = 0; i < 10; i++) {
			set.add(new WrapInt(i));
		}
		assertTrue(set.size() == 3);
		for (int i = 0; i < 10; i++) {
			set = set.add(new WrapInt(i));
		}
		assertTrue(set.size() == 11);
	}
	
	public void testDelete() {
		KeyedSet<Integer,Element<Integer,String>> set = makeElements(100, 1);
		for (int i = 0; i < 100; i += 2) {
			set = set.delete(i);
		}
		int size = set.size();
		assertTrue(size == 50);
		for (Element<Integer,String> e : set) {
			assertTrue((e.getKey() & 1) == 1);
		}
	}
	
	public void testBalance() {
		KeyedSet<Integer,Element<Integer,String>> set = KeyedSet.EMPTY;
		assertTrue(set.isBalanced());
		set = makeElements(100, 1);
		assertTrue(set.isBalanced());
		set = makeElements(100, -1);
		assertTrue(set.isBalanced());
		for (int i = 0; i < 100; i += 2) {
			set = set.delete(i);
		}
		assertTrue(set.isBalanced());
	}
	
	public void testOrder() {
		KeyedSet<Integer,Element<Integer,String>> set = KeyedSet.EMPTY;
		assertTrue(set.isOrdered());
		set = makeElements(100, 1);
		assertTrue(set.isOrdered());
		set = makeElements(100, -1);
		assertTrue(set.isOrdered());
		for (int i = 0; i < 100; i += 2) {
			set = set.delete(i);
		}
		assertTrue(set.isOrdered());
	}
	
	public void testGet() {
		KeyedSet<Integer,Element<Integer,String>> set = makeElements(100, 1);
		Element<Integer,String> e = null;
		for (int i = 0; i < 100; i++) {
			e = set.get(i);
			assertTrue(e != null);
			assertTrue(e.getKey() == i);
			assertTrue(e.getValue().equals(Integer.toString(i)));
		}
		assertTrue(set.get(-1) == null);
		assertTrue(set.get(100) == null);
	}
	
	public void testContains() {
		KeyedSet<Integer,Element<Integer,String>> set = makeElements(100, 2);
		assertTrue(set.contains(new Element<Integer,String>(0, Integer.toString(0))));
		assertFalse(set.contains(new Element<Integer,String>(-1, "-1")));
		assertFalse(set.contains(new Element<Integer,String>(71, "71")));
		
		// Doesn't work for elements with default equals
		
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertFalse(wrapSet.contains(new WrapInt(77)));
		wrapSet = wrapSet.add(new WrapInt(77));
		// not true!
		assertFalse(wrapSet.contains(new WrapInt(77)));
	}

	public void testContainsKey() {
		KeyedSet<Integer,Element<Integer,String>> set = makeElements(100, 2);
		assertTrue(set.containsKey(0));
		assertFalse(set.containsKey(-1));
		assertFalse(set.containsKey(77));
		
		// Works for elements with default equals
		
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertFalse(wrapSet.containsKey(77));
		wrapSet = wrapSet.add(new WrapInt(77));
		// true!
		assertTrue(wrapSet.containsKey(77));
	}
	
	public void testGetFirst() {
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertTrue(wrapSet.getFirst() == null);
		wrapSet = wrapSet.add(new WrapInt(77));
		assertTrue(wrapSet.getFirst().getKey() == 77);
		wrapSet = wrapSet.add(new WrapInt(78));
		assertTrue(wrapSet.getFirst().getKey() == 77);
		wrapSet = wrapSet.add(new WrapInt(76));
		assertTrue(wrapSet.getFirst().getKey() == 76);
		wrapSet = wrapSet.add(new WrapInt(Integer.MIN_VALUE));
		assertTrue(wrapSet.getFirst().getKey() == Integer.MIN_VALUE);
		wrapSet = wrapSet.add(new WrapInt(Integer.MAX_VALUE));
		assertTrue(wrapSet.getFirst().getKey() == Integer.MIN_VALUE);
	}

	public void testGetLast() {
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertTrue(wrapSet.getLast() == null);
		wrapSet = wrapSet.add(new WrapInt(77));
		assertTrue(wrapSet.getLast().getKey() == 77);
		wrapSet = wrapSet.add(new WrapInt(78));
		assertTrue(wrapSet.getLast().getKey() == 78);
		wrapSet = wrapSet.add(new WrapInt(76));
		assertTrue(wrapSet.getLast().getKey() == 78);
		wrapSet = wrapSet.add(new WrapInt(Integer.MIN_VALUE));
		assertTrue(wrapSet.getLast().getKey() == 78);
		wrapSet = wrapSet.add(new WrapInt(Integer.MAX_VALUE));
		assertTrue(wrapSet.getLast().getKey() == Integer.MAX_VALUE);
	}

	public void testDeleteFirst() {
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertTrue(wrapSet.deleteFirst() == wrapSet);
		wrapSet = wrapSet.add(new WrapInt(77));
		wrapSet = wrapSet.add(new WrapInt(78));
		wrapSet = wrapSet.add(new WrapInt(76));
		wrapSet = wrapSet.add(new WrapInt(Integer.MIN_VALUE));
		wrapSet = wrapSet.add(new WrapInt(Integer.MAX_VALUE));
		assertTrue(wrapSet.getFirst().getKey() == Integer.MIN_VALUE);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteFirst();
		assertTrue(wrapSet.getFirst().getKey() == 76);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteFirst();
		assertTrue(wrapSet.getFirst().getKey() == 77);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteFirst();
		assertTrue(wrapSet.getFirst().getKey() == 78);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteFirst();
		assertTrue(wrapSet.getFirst().getKey() == Integer.MAX_VALUE);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteFirst();
		assertTrue(wrapSet.getFirst() == null);
		assertTrue(wrapSet.isBalanced());
	}

	public void testDeleteLast() {
		KeyedSet<Integer,WrapInt> wrapSet = KeyedSet.EMPTY;
		assertTrue(wrapSet.deleteLast() == wrapSet);
		wrapSet = wrapSet.add(new WrapInt(77));
		wrapSet = wrapSet.add(new WrapInt(78));
		wrapSet = wrapSet.add(new WrapInt(76));
		wrapSet = wrapSet.add(new WrapInt(Integer.MIN_VALUE));
		wrapSet = wrapSet.add(new WrapInt(Integer.MAX_VALUE));
		assertTrue(wrapSet.getLast().getKey() == Integer.MAX_VALUE);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteLast();
		assertTrue(wrapSet.getLast().getKey() == 78);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteLast();
		assertTrue(wrapSet.getLast().getKey() == 77);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteLast();
		assertTrue(wrapSet.getLast().getKey() == 76);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteLast();
		assertTrue(wrapSet.getLast().getKey() == Integer.MIN_VALUE);
		assertTrue(wrapSet.isBalanced());
		wrapSet = wrapSet.deleteLast();
		assertTrue(wrapSet.getLast() == null);
		assertTrue(wrapSet.isBalanced());
	}
	
	public void testDifference() {
		// All integers 0..99
		KeyedSet<Integer,Element<Integer,String>> seta = makeElements(100, 1);
		// All even integers 0..99
		KeyedSet<Integer,Element<Integer,String>> setb = makeElements(100, 2);
		// All odd integers 0..99
		KeyedSet<Integer,Element<Integer,String>> setc = seta.difference(setb);
		assertTrue(setc.size() == 50);
		for (int i = 0; i < 100; i++) {
			if ((i & 1) == 0) {
				assertFalse(setc.containsKey(i));
			} else {
				assertTrue(setc.containsKey(i));
			}
		}
		assertFalse(setc.containsKey(-1));
		assertFalse(setc.containsKey(100));
		
		// some other iterable
		ArrayList<Element<Integer,String>> primes = primeElements(100);
		KeyedSet<Integer,Element<Integer,String>> setd = setc.difference(primes);
		// except for 2, all values in primes should have been in setc
		assertTrue(setd.size() == setc.size() - primes.size() + 1);
		for (Element<Integer,String> p : primes) {
			assertTrue(!setd.contains(p));
			assertTrue(!setd.containsKey(p.getKey()));
		}
		
		checkHeight(seta);
		checkHeight(setb);
		checkHeight(setc);
	}
	
	private ArrayList<Element<Integer,String>> primeElements(int n) {
		ArrayList<Element<Integer,String>> pe = new ArrayList<Element<Integer,String>>();
		for (Integer p : primes(n)) {
			pe.add(makeElement(p));
		}
		return pe;
	}
	
	private ArrayList<Integer> primes(int n) {
		ArrayList<Integer> primes = new ArrayList<Integer>();
		outer:
		for (int i = 2; i < 100; i++) {
			for (Integer p : primes) {
				if ((i% p) == 0) {
					break outer;
				}
			}
			primes.add(i);
		}
		return primes;
	}
	
	public void testElements() {
		// Every element should be returned, in order
		KeyedSet<Integer,WrapInt> set = makeWraps(100,2);
		assertTrue(set.getFirst().getKey() == 0);
		assertTrue(set.getLast().getKey() == 98);
		assertTrue(set.size() == 50);
		int n = 0;
		int last = -1;
		for (WrapInt w : set.elements()) {
			int k = w.getKey();
			assertTrue(k > last);
			last = k;
			assertTrue((k & 1) == 0);
			n++;
		}
		assertTrue(n == 50);
		assertTrue(set.getHeight() <= Integer.highestOneBit(set.size()));
	}
	
	public void testEmpty() {
		KeyedSet<Integer,WrapInt> seta = KeyedSet.EMPTY;
		assertTrue(seta.isEmpty());
		assertTrue(seta.isBalanced());
		assertTrue(seta.isOrdered());
		assertTrue(seta.getHeight() == 0);
		assertTrue(seta.size() == 0);
		assertTrue(seta.clear() == seta);
		KeyedSet<Integer,WrapInt> setb = seta.union(KeyedSet.EMPTY);
		assertTrue(setb == KeyedSet.EMPTY);
		setb = seta.intersection(KeyedSet.EMPTY);
		assertTrue(setb == KeyedSet.EMPTY);
	}
	
	public void testFilter() {
		KeyedSet<Integer,WrapInt> seta = makeWraps(100,1);
		final HashSet<Integer> primes = new HashSet(primes(100));
		KeyedSet<Integer,WrapInt> setb = seta.filter(new Tester<WrapInt>() {
			public boolean test(WrapInt element) {
				return !primes.contains(element.getKey());
			}
		});
		assertTrue(setb.isBalanced());
		// all the primes should be in the original set, so
		assertTrue(seta.size() == setb.size() + primes.size());
		// and redundantly
		for (WrapInt w : setb) {
			assertTrue(!primes.contains(w.getKey()));
		}
		checkHeight(seta);
		checkHeight(setb);
	}
	
	public void testUnion() {
		KeyedSet<Integer,WrapInt> seta = makeWraps(100,1);
		// even integers
		KeyedSet<Integer,WrapInt> setb = makeWraps(100,2);
		// odd integers
		KeyedSet<Integer,WrapInt> setc = seta.difference(setb);
		assertTrue(setb.size() == setc.size());
		
		KeyedSet<Integer,WrapInt> setd = setb.union(setc);
		int setaSize = seta.size();
		int setbSize = setb.size();
		int setcSize = setc.size();
		int setdSize = setd.size();
		assertTrue(setd.size() == seta.size());
	}
}
