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
 * Pass on the first up to n elements of the sequence.
 * 
 * @author Bob Foster
 */
public class First<T> extends DefaultPipeline<T,T> {
	private int n;

	public First(int n, Receiver<T> next) {
		super(next);
		this.n = n;
	}

	@Override
	public boolean receive(T t) {
		if (n > 0) {
			next.receive(t);
			n--;
			if (n == 0) {
				next.close();
				return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void close() {
		// sequence should never send after false return, but just in case
		n = 0;
		next.close();
	}
}
