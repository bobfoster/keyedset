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
 * Pass on the last up to n elements of the sequence.
 * 
 * @author Bob Foster
 */
public class Last<T> extends DefaultPipeline<T,T> {
	private int i;
	private Object[] buffer;
	private int n;

	public Last(int n, Receiver<T> next) {
		super(next);
		buffer = new Object[n];
		i = 0;
	}

	@Override
	public boolean receive(T t) {
		if (n == 0) {
			return false;
		}
		if (i == buffer.length) {
			i = 0;
		}
		buffer[i++] = t;
		n--;
		if (n == 0) {
			sendLast();
			next.close();
			return false;
		}
		return true;
	}
	
	@Override
	public void close() {
		// if n == 0 was already sent
		if (n > 0) {
			sendLast();
		}
		next.close();
	}
	
	private void sendLast() {
		int from = i - buffer.length + n;
		if (from < 0) {
			from += buffer.length;
		}
		for (; from != i; from++) {
			from %= buffer.length;
			if (!next.receive((T)buffer[from])) {
				break;
			}
		}
		next.close();
	}
}
