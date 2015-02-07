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
 * Sequence is an interface implemented by value providers.
 * 
 * <p>A provider implements the seq method, which accepts a receiver and returns
 * a value of the receiver value type. Unfortunately, a Sequence cannot be
 * implemented in Java except with a fixed type R, and the whole point is that a
 * receiver may compute a value of any type; hence the Object return. The
 * caller of seq must know the value type returned and cast it appropriately.
 * 
 * @author Bob Foster
 */
public interface Sequence<T> {
	public Object seq(Receiver<T> receiver);
}
