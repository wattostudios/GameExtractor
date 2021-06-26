package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public abstract class SlidingDicDecoder implements LhaDecoder {
	final protected static int OFFSET = 0x100 - 3;
	final protected static int UNSINGED_CHAR_MAX = 255;
	final protected static int CHAR_BIT = 8;
	final protected static int THRESHOLD = 3;
	final protected static int MAX_MATCH = 256;
	
	protected InputStream in;
	protected long originalSize;
	protected long decodeCount;

	protected int positionAdjust;
	protected int dictionarySize;
	protected int dictionaryMask;
	protected byte dictionaryBuffer[];
	protected int bufferPointerBegin;
	protected int bufferPointerEnd;
	
	protected int bitBuffer;
	protected int subBitBuffer;
	protected int bitCount;
	
	protected boolean needInitialRead;
	
	
	public SlidingDicDecoder(InputStream in, long originalSize, int dictionaryBit, int positionAdjust) {
		this.in = in;
		this.originalSize = originalSize;
		this.decodeCount = 0L;
		
		this.positionAdjust = positionAdjust;
		this.dictionarySize = 1 << dictionaryBit;
		this.dictionaryMask = dictionarySize - 1;
		this.dictionaryBuffer = new byte[dictionarySize];
		this.bufferPointerBegin = 0;
		this.bufferPointerEnd = 0;
		
		this.bitBuffer = 0;
		this.subBitBuffer = 0;
		this.bitCount = 0;
		
		this.needInitialRead = true;
	}
	
	public void close() {
	}

	/**
	 * 
	 */
	public int read(byte[] b, int off, int len) throws LhaException, IOException {
		if (needInitialRead) {
			initRead();
			needInitialRead = false;
		}
		
		int sl = len;
		int rs = bufferPointerEnd - bufferPointerBegin;

		if (rs < 0) {
			int bl = dictionarySize - bufferPointerBegin;

			if (bl >= len) {
				System.arraycopy(dictionaryBuffer, bufferPointerBegin, b, off, len);
				bufferPointerBegin += len;
				if (bufferPointerBegin == dictionarySize) {
					bufferPointerBegin = 0;
				}
				
				return (sl);
			} else {
				System.arraycopy(dictionaryBuffer, bufferPointerBegin, b, off, bl);
				off += bl;
				len -= bl;
				bufferPointerBegin = 0;

				if (bufferPointerEnd >= len) {
					System.arraycopy(dictionaryBuffer, 0, b, off, len);
					bufferPointerBegin = len;
					return (sl);
				} else if (bufferPointerEnd != 0) {
					System.arraycopy(dictionaryBuffer, 0, b, off, bufferPointerEnd);
					off += bufferPointerEnd;
					len -= bufferPointerEnd;
					bufferPointerBegin = bufferPointerEnd;
				}
			}
		} else if (rs >= len) {
			System.arraycopy(dictionaryBuffer, bufferPointerBegin, b, off, len);
			bufferPointerBegin += len;
			return (sl);
		} else if (rs != 0) {
			System.arraycopy(dictionaryBuffer, bufferPointerBegin, b, off, rs);
			off += rs;
			len -= rs;
			bufferPointerBegin = bufferPointerEnd;
		}

		if (originalSize <= decodeCount) {
			int l = sl - len;
			return l > 0 ? l : -1;
		}

		while ((decodeCount < originalSize) && (len > 0)) {
			int c = decodeCode();
			if (c <= UNSINGED_CHAR_MAX) {
				++decodeCount;
				--len;
				++bufferPointerBegin;
				dictionaryBuffer[bufferPointerEnd++] = b[off++] = (byte) c;
				if (bufferPointerEnd == dictionarySize) {
					bufferPointerBegin = bufferPointerEnd = 0;
				}
			} else {
				int matchLength = c - positionAdjust;
				int matchOffset = decodePosition();
				int matchPosition = (bufferPointerEnd - matchOffset - 1) & dictionaryMask;
				decodeCount += matchLength;

				for (int k = 0; k < matchLength; ++k) {
					byte t = (byte) (dictionaryBuffer[(matchPosition + k) & dictionaryMask] & 0xFF);
					dictionaryBuffer[bufferPointerEnd++] = t;
					if (len > 0) {
						--len;
						++bufferPointerBegin;
						if (bufferPointerBegin == dictionarySize) {
							bufferPointerBegin = 0;
						}
						b[off++] = t;
					}

					if (bufferPointerEnd == dictionarySize) {
						bufferPointerEnd = 0;
					}
				}
			}
		}

		return (sl - len);
	}

	/**
	 * 
	 * @param n
	 * @throws IOException
	 */
	final protected void fillBitBuffer(int n) throws IOException {
		while (n > bitCount) {
			n -= bitCount;
			bitBuffer = (bitBuffer << bitCount)
					+ (subBitBuffer >>> (CHAR_BIT - bitCount));

			int c = in.read();
			subBitBuffer = (c > 0) ? c : 0;
			bitCount = CHAR_BIT;
		}

		bitCount -= n;
		bitBuffer = ((bitBuffer << n) + (subBitBuffer >>> (CHAR_BIT - n))) & 0xFFFF;
		subBitBuffer = (subBitBuffer << n) & 0x00FF;
	}
	
	/**
	 * 
	 * @param n
	 * @return
	 * @throws IOException
	 */
	final protected int getBits(int n) throws IOException {
		int x = bitBuffer >>> (2 * CHAR_BIT - n);
		fillBitBuffer(n);
		
		return (x);
	}
	
	abstract protected int decodeCode() throws IOException;
	abstract protected int decodePosition() throws IOException;
	abstract protected void initRead() throws LhaException, IOException;
}
