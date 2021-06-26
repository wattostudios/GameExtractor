package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class LzsDecoder extends SlidingDicDecoder {
	private static final int MAGIC = 18;
	
	private int matchPosition;

	public LzsDecoder(InputStream in, long originalSize) {
		// TODO dictionaryBit
		super(in, originalSize, 11, 256 - 2);
	}

	protected int decodeCode() throws IOException {
		int b = getBits(1);
		if (b != 0) {
			return(getBits(8));
		} else {
			matchPosition = getBits(11);
			return(getBits(4) + 0x100);
		}
	}

	protected int decodePosition() throws IOException {
		return((bufferPointerEnd - matchPosition - MAGIC) & dictionaryMask);
	}

	protected void initRead() throws LhaException, IOException {
		fillBitBuffer(2 * CHAR_BIT);
	}

}
