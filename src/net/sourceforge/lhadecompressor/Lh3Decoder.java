package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class Lh3Decoder extends LhDecoder {
	private static final int BUFBITS = 16;
	private static final int NP = 8 * 1024 / 64;
	final private static int CBIT = 9;
	private static final int CODE_TABLE_SIZE = 4096;
	private static final int N1 = 286;
	private static final int EXTRA_BITS = 8;
	private static final int LENGTH_FIELD = 4;
	
	
	private static final int[] FIXED = {2, 0x01, 0x01, 0x03, 0x06, 0x0D, 0x1F, 0x4E, 0};

	private int positionCode[];
	
	private int codeLength[];
	private int codeTable[];
	
	private int np;
	private int blockSize;

	public Lh3Decoder(InputStream in, long originalSize) {
		super(in, originalSize, 13, OFFSET);
		
		this.positionCode = new int[NPT];
		
		this.codeLength = new int[NC];
		this.codeTable = new int[CODE_TABLE_SIZE];
		
		this.blockSize = 0;
		
		this.np = 1 << (13 - 6);
	}
	
	private void readyMade() {
		int index = 0;
		int j = FIXED[index++];
		int weight = 1 << (16 - j);
		int code = 0;
		for (int i = 0; i < np; ++i) {
			while(FIXED[index] == i) {
				++j;
				++index;
				weight >>>= 1;
			}
			positionLength[i] = j;
			positionCode[i] = code;
			code += weight;
		}
	}
	
	private void readTreeCode() throws IOException {
		int i = 0;
		while (i < N1) {
			if (getBits(1) != 0) {
				codeLength[i] = getBits(LENGTH_FIELD) + 1;
			} else {
				codeLength[i] = 0;
			}
			++i;
			if ((i == 3) && (codeLength[0] == 1) && (codeLength[1] == 1) && (codeLength[2] == 1)) {
				int c = getBits(CBIT);
				for (int j = 0; j < N1; ++j) {
					codeLength[j] = 0;
				}
				for (int j = 0; j < 4096; ++j) {
					codeTable[j] = c;
				}
				return;
			}
		}
		makeTable(N1, codeLength, 12, codeTable);
	}
	
	private void readTreePosition() throws IOException {
		int i = 0;
		while (i < NP) {
			positionLength[i] = getBits(LENGTH_FIELD);
			++i;
			if ((i == 3) && (positionLength[0] == 1) && (positionLength[1] == 1) && (positionLength[2] == 1)) {
				int c = getBits(13 - 6);
				for (int j = 0; j < NP; ++j) {
					positionLength[j] = 0;
				}
				for (int j = 0; j < 256; ++j) {
					positionTable[j] = c;
				}
				return;
			}
		}
	}
	
	protected void initRead() throws LhaException, IOException {
		fillBitBuffer(2 * CHAR_BIT);
	}

	protected int decodeCode() throws IOException {
		if (blockSize == 0) {
			blockSize = getBits(BUFBITS);
			readTreeCode();
			if (getBits(1) != 0) {
				readTreePosition();
			} else {
				readyMade();
			}
			makeTable(NP, positionLength, 8, positionTable);
		}
		--blockSize;
		int j = codeTable[bitBuffer >>> (16 - 12)];
		if (j < N1) {
			fillBitBuffer(codeLength[j]);
		} else {
			fillBitBuffer(12);
			int b = bitBuffer;
			do {
				if ((b & 0x8000) != 0) {
					j = treeRight[j];
				} else {
					j = treeLeft[j];
				}
				b <<= 1;
			} while (j >= N1);
			fillBitBuffer(codeLength[j] - 12);
		}
		
		if (j == (N1 - 1)) {
			j += getBits(EXTRA_BITS);
		}		
		return (j);
	}

	protected int decodePosition() throws IOException {
		int j = positionTable[bitBuffer >>> (16 - 8)];
		if (j < np) {
			fillBitBuffer(positionLength[j]);
		} else {
			fillBitBuffer(8);
			int b = bitBuffer;
			do {
				if ((b & 0x80) != 0) {
					j = treeRight[j];
				} else {
					j = treeLeft[j];
				}
				b <<= 1;
			} while(j >= np);
			fillBitBuffer(positionLength[j] - 8);
		}
		
		return ((j << 6) + getBits(6));
	}

}
