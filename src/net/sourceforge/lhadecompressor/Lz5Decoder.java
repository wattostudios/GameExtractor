package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class Lz5Decoder extends SlidingDicDecoder {
	private static final int MAGIC = 19;
	
	private int flag;
	private int flagCount;
	private int matchPosition;

	public Lz5Decoder(InputStream in, long originalSize) {
		super(in, originalSize, 12, OFFSET);
	}

	protected int decodeCode() throws IOException {
		if (flagCount == 0) {
			flagCount = 8;
			flag = in.read();
		}
		--flagCount;
		int c = in.read();
		if ((flag & 0x0001) == 0) {
			matchPosition = c;
			c = in.read();
			matchPosition += (c & 0x00F0) << 4;
			c &= 0x000F;
			c |= 0x0100;
		}
		flag >>>= 1;
		return(c);
	}

	protected int decodePosition() throws IOException {
		return((bufferPointerEnd - matchPosition - MAGIC) & dictionaryMask);
	}

	protected void initRead() throws LhaException, IOException {
		flagCount = 0;
		for (int i = 0; i < 256; ++i) {
			for (int j = 0; j < 13; ++j) {
				dictionaryBuffer[i * 13 + 18 + j] = (byte)i;
			}
			dictionaryBuffer[256 * 13 + 18 + i] = (byte)i;
			dictionaryBuffer[256 * 13 + 256 + 18 + i] = (byte)(255 - i);
		}
		for (int i = 0; i < 128; ++i) {
			dictionaryBuffer[256 * 13 + 512 + 18] = 0;
		}
		for (int i = 0; i < (128 - 18); ++i) {
			dictionaryBuffer[256 * 13 + 512 + 128 + 18] = 0x20;
		}
	}

}
