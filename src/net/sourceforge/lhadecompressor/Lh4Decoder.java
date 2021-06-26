package net.sourceforge.lhadecompressor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class Lh4Decoder extends LhDecoder {
	final private static int UNSIGNED_SHORT_BIT = 16;
	final private static int NT = UNSIGNED_SHORT_BIT + 3;
	final private static int TBIT = 5;
	final private static int CBIT = 9;
	final private static int CODE_TABLE_SIZE = 4096;
	
	private int codeLength[];
	private int codeTable[];
	
	private int np;
	private int positionBit;
	private int blockSize;
	
	public Lh4Decoder(InputStream in, long originalSize, int dictionaryBit, int np, int positionBit) {
		super(in, originalSize, dictionaryBit, OFFSET);
		
		this.codeLength = new int[NC];
		this.codeTable = new int[CODE_TABLE_SIZE];
		
		this.np = np;
		this.positionBit = positionBit;
		this.blockSize = 0;
	}
	
	protected void initRead() throws LhaException, IOException {
		fillBitBuffer(2 * CHAR_BIT);
	}

	protected int decodeCode() throws IOException {
		if (blockSize == 0) {
			blockSize = getBits(16);
			readPositionLength(NT, TBIT, 3);
			readCodeLength();
			readPositionLength(np, positionBit, -1);
		}

		--blockSize;
		int j = codeTable[bitBuffer >>> (16 - 12)];

		if (j < NC) {
			fillBitBuffer(codeLength[j]);
		} else {
			fillBitBuffer(12);
			int mask = 1 << (16 - 1);
			do {
				if ((bitBuffer & mask) != 0) {
					j = treeRight[j];
				} else {
					j = treeLeft[j];
				}

				mask >>>= 1;
			} while ((j >= NC) && ((mask != 0) || (j != treeLeft[j])));

			fillBitBuffer(codeLength[j] - 12);
		}

		return (j);
	}

	protected int decodePosition() throws IOException {
		int j = positionTable[bitBuffer >>> (16 - 8)];

		if (j < np) {
			fillBitBuffer(positionLength[j]);
		} else {
			fillBitBuffer(8);

			int mask = 1 << (16 - 1);
			do {
				if ((bitBuffer & mask) != 0) {
					j = treeRight[j];
				} else {
					j = treeLeft[j];
				}

				mask >>>= 1;
			} while ((j >= np) && ((mask != 0) || (j != treeLeft[j])));

			fillBitBuffer(positionLength[j] - 8);
		}

		if (j != 0) {
			j = ((1 << (j - 1)) + getBits(j - 1));
		}

		return (j);
	}

	final private void readPositionLength(int nn, int nbit, int i_special) throws IOException {
		int n = getBits(nbit);
		if (n == 0) {
			int c = getBits(nbit);

			for (int i = 0; i < nn; ++i) {
				positionLength[i] = 0;
			}

			for (int i = 0; i < POSITION_TABLE_SIZE; ++i) {
				positionTable[i] = c;
			}
		} else {
			int i = 0;
			int max = n < NPT ? n : NPT;
			while (i < max) {
				int c = bitBuffer >>> (16 - 3);
				if (c != 7) {
					fillBitBuffer(3);
				} else {
					int mask = 1 << (16 - 4);
					while ((mask & bitBuffer) != 0) {
						mask >>>= 1;
						++c;
					}
					fillBitBuffer(c - 3);
				}

				positionLength[i++] = c;

				if (i == i_special) {
					c = getBits(2);
					while ((--c >= 0) && (i < NPT)) {
						positionLength[i++] = 0;
					}
				}
			}

			while (i < nn) {
				positionLength[i++] = 0;
			}

			makeTable(nn, positionLength, 8, positionTable);
		}
	}

	final private void readCodeLength() throws IOException {
		int n = getBits(CBIT);
		if (n == 0) {
			int c = getBits(CBIT);
			for (int i = 0; i < NC; ++i) {
				codeLength[i] = 0;
			}

			for (int i = 0; i < CODE_TABLE_SIZE; ++i) {
				codeTable[i] = c;
			}
		} else {
			int i = 0;
			int max = n < NC ? n : NC;
			while (i < max) {
				int c = positionTable[bitBuffer >>> (16 - 8)];
				if (c >= NT) {
					int mask = 1 << (16 - 9);
					do {
						if ((bitBuffer & mask) != 0) {
							c = treeRight[c];
						} else {
							c = treeLeft[c];
						}

						mask >>>= 1;
					} while ((c >= NT) && ((mask != 0) || (c != treeLeft[c])));
				}

				fillBitBuffer(positionLength[c]);

				if (c <= 2) {
					if (c == 0) {
						c = 1;
					} else if (c == 1) {
						c = getBits(4) + 3;
					} else {
						c = getBits(CBIT) + 20;
					}

					while (--c >= 0) {
						codeLength[i++] = 0;
					}
				} else {
					codeLength[i++] = c - 2;
				}
			}

			while (i < NC) {
				codeLength[i++] = 0;
			}

			makeTable(NC, codeLength, 12, codeTable);
		}
	}

}
