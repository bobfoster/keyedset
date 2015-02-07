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

package org.genantics.seq;

/**
 * Receiver processes a sequence of values from Sequence provider.
 * 
 * <p>Both the sender and receiver can stop values being received; the
 * receiver by <code>receive</code> returning false; the sender by
 * a) stop sending, and b) calling <code>close</code>.
 * 
 * @author bobfoster
 */
public interface Receiver<T> {
	/**
	 * Receive the next value in a sequence.
	 * 
	 * <p>It is the sender's responsibility to not send more values after
	 * receive has returned false or sender has called close().
	 * 
	 * @param t value
	 * @return true if wish to receive more values; false if no more values
	 * should be sent
	 */
	boolean receive(T t);
	
	/**
	 * Tell the receiver there will be no more values sent.
	 * 
	 * <p>This gives the receiver a chance to prepare its final value.
	 * 
	 * <p>It is not an error if close is called multiple times. Receivers
	 * who do something other than pass close along must ensure they
	 * don't do it twice.
	 */
	void close();
	
	/**
	 * Get the final value calculated by the receiver.
	 * 
	 * <p>This may only be called by sender after it calls <code>close</code>.
	 * If called in any other state, result is
	 * undefined. If called more than once, returns the
	 * same value.
	 * 
	 * <p>Unfortunately, a typed value cannot be implemented in Java, because
	 * a given Sequence could only implement <code>seq</code> for a single 
	 * return type. Hence, the Object return.
	 * 
	 * @return final value
	 */
	Object value();
}
