/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genantics.keyedset;

import org.genantics.seq.ToList;

/**
  * Receiver converts sequence to keyed set.
 * @author Bob Foster
 */
public class ToKeyedSet<K extends Comparable<K>, T extends Keyed<K>> extends ToList<T> {

	@Override
	public Object value() {
		return new KeyedSet<K,T>(list);
	}

}
