package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class Lh2Decoder extends LhDecoder {
	private static final int N_CHAR = 256 + 60 - THRESHOLD + 1;
	private static final int TREESIZE_CODE = N_CHAR * 2;
	private static final int TREESIZE_POSITION = 128 * 2;
	private static final int TREESIZE = TREESIZE_CODE + TREESIZE_POSITION;
	private static final int ROOT_CODE = 0;
	private static final int ROOT_POSITION = TREESIZE_CODE;
	
	private int nMax;
	private int maxMatch;
	
	private int[] child;
	private int[] parent;
	private int[] block;
	private int[] edge;
	private int[] stock;
	private int[] sNode;
	private int[] freq;
	private int avail;
	private int n1;
	private int totalPosition;
	private int mostPosition;
	private int nn;
	private long nextCount;

	public Lh2Decoder(InputStream in, long originalSize) {
		super(in, originalSize, 13, OFFSET);
		
		this.nMax = 286;
		this.maxMatch = MAX_MATCH;
		this.nn = 1 << 13;
	}
	
	private void initCodeDynamic() {
		this.child = new int[TREESIZE];
		this.parent = new int[TREESIZE];
		this.block = new int[TREESIZE];
		this.edge = new int[TREESIZE];
		this.stock = new int[TREESIZE];
		this.sNode = new int[TREESIZE / 2];
		this.freq = new int[TREESIZE];
		
		n1 = (nMax >= (256 + maxMatch - THRESHOLD + 1)) ? 512 : nMax - 1;
		for (int i = 0; i < TREESIZE_CODE; ++i) {
			stock[i] = i;
			block[i] = 0;
		}
		
		int j = nMax * 2 - 2;
		for (int i = 0; i < nMax; ++i, --j) {
			freq[j] = 1;
			child[j] = ~i;
			sNode[i] = j;
			block[j] = 1;
		}
		avail = 2;
		edge[1] = nMax - 1;
		for (int i = nMax * 2 - 2; j >= 0; i -= 2, --j) {
			int f = freq[j] = freq[i] + freq[i - 1];
			child[j] = i;
			parent[i] = parent[i - 1] = j;
			if (f == freq[j + 1]) {
				block[j] = block[j + 1];
			} else {
				block[j] = stock[avail++];
			}
			edge[block[j]] = j;
		}
	}
	
	private void initPositionDynamic() {
		freq[ROOT_POSITION] = 1;
		child[ROOT_POSITION] = ~(N_CHAR);
		sNode[N_CHAR] = ROOT_POSITION;
		block[ROOT_POSITION] = stock[avail++];
		edge[block[ROOT_POSITION]] = ROOT_POSITION;
		mostPosition = ROOT_POSITION;
		totalPosition = 0;
		nextCount = 64;
	}
	
	private void reconstruct(int begin, int end) {
		int j, l, b;
		
		j = begin;
		b = block[begin];
		for (int i = begin; i < end; ++i) {
			int k = child[i];
			if (k < 0) {
				freq[j] = (freq[i] + 1) / 2;
				child[j] = k;
				++j;
			}
			b = block[i];
			if (edge[b] == i) {
				stock[--avail] = b;
			}
		}
		--j;
		l = end - 2;
		for (int i = end - 1; i >= begin; --i, l -= 2) {
			while (i >= l) {
				freq[i] = freq[j];
				child[i] = child[j];
				--i;
				--j;
			}
			int k;
			int f = freq[l] + freq[l + 1];
			for (k = begin; f < freq[k]; ++k)
				;
			while (j >= k) {
				freq[i] = freq[j];
				child[i] = child[j];
				--i;
				--j;
			}
			freq[i] = (int)f;
			child[i] = l + 1;
		}
		int f = 0;
		for (int i = begin; i < end; ++i) {
			j = child[i];
			if (j < 0) {
				sNode[~j] = i;
			} else {
				parent[j] = parent[j - 1] = i;
			}
			int g = freq[i];
			if (g == f) {
				block[i] = b;
			} else {
				b = block[i] = stock[avail++];
				edge[b] = i;
				f = g;
			}
		}
	}
	
	private int swapInc(int p) {
		int b = block[p];
		int q = edge[b];
		if (q != p) {
			int r = child[p];
			int s = child[q];
			child[p] = s;
			child[q] = r;
			if (r >= 0) {
				parent[r] = parent[r - 1] = q;
			} else {
				sNode[~r] = q;
			}
			if (s >= 0) {
				parent[s] = parent[s - 1] = p;
			} else {
				sNode[~s] = p;
			}
			p = q;

			++edge[b];
			++freq[p];
			if (freq[p] == freq[p - 1]) {
				block[p] = block[p - 1];
			} else {
				block[p] = stock[avail++];
				edge[block[p]] = p;
			}
		} else if (b == block[p + 1]) {
			++edge[b];
			++freq[p];
			if (freq[p] == freq[p - 1]) {
				block[p] = block[p - 1];
			} else {
				block[p] = stock[avail++];
				edge[block[p]] = p;
			}
		} else if (++freq[p] == freq[p - 1]) {
			stock[--avail] = b;
			block[p] = block[p - 1];
		}
		return (parent[p]);
	}
	
	private void updateCode(int p) {
		if (freq[ROOT_CODE] == 0x8000) {
			reconstruct(0, nMax * 2 - 1);
		}
		++freq[ROOT_CODE];
		int q = sNode[p];
		do {
			q = swapInc(q);
		} while(q != ROOT_CODE);
	}
	
	private void updatePosition(int p) {
		if (totalPosition == 0x8000) {
			reconstruct(ROOT_POSITION, mostPosition + 1);
			totalPosition = freq[ROOT_POSITION];
			freq[ROOT_POSITION] = 0xFFFF;
		}
		int q = sNode[p + N_CHAR];
		while (q != ROOT_POSITION) {
			q = swapInc(q);
		}
		++totalPosition;
	}
	
	private void makeNewNode(int p) {
		int r = mostPosition + 1;
		int q = r + 1;
		child[r] = child[mostPosition];
		child[~child[r]] = r;
		child[q] = ~(p + N_CHAR);
		child[mostPosition] = q;
		freq[r] = freq[mostPosition];
		freq[q] = 0;
		block[r] = block[mostPosition];
		if (mostPosition == ROOT_POSITION) {
			freq[ROOT_POSITION] = 0xFFFF;
			++edge[block[ROOT_POSITION]];
		}
		parent[r] = parent[q] = mostPosition;
		block[q] = stock[avail++];
		edge[block[q]] = sNode[p + N_CHAR] = mostPosition = q;
		updatePosition(p);
	}
	
	protected void initRead() throws LhaException, IOException {
		fillBitBuffer(2 * CHAR_BIT);
		initCodeDynamic();
		initPositionDynamic();
	}

	protected int decodeCode() throws IOException {
		int c = child[ROOT_CODE];
		int b = bitBuffer;
		int count = 0;
		do {
			if ((b & 0x8000) != 0) {
				c = child[c - 1];
			} else {
				c = child[c];
			}
			b <<= 1;
			++count;
			if (count == 16) {
				fillBitBuffer(16);
				b = bitBuffer;
				count = 0;
			}
		} while(c > 0);
		fillBitBuffer(count);
		c = ~c;
		updateCode(c);
		if (c == n1) {
			c += getBits(8);
		}
		
		return (c);
	}

	protected int decodePosition() throws IOException {
		while (decodeCount > nextCount) {
			makeNewNode((int)(nextCount / 64));
			nextCount += 64;
			if (nextCount >= nn) {
				nextCount = 0x00000000FFFFFFFFL;
			}
		}
		
		int c = child[ROOT_POSITION];
		int b = bitBuffer;
		int count = 0;
		while (c > 0) {
			if ((b & 0x8000) != 0) {
				c = child[c - 1];
			} else {
				c = child[c];
			}
			b <<= 1;
			++count;
			if (count == 16) {
				fillBitBuffer(16);
				b = bitBuffer;
				count = 0;
			}
		}
		fillBitBuffer(count);
		c = (~c) - N_CHAR;
		updatePosition(c);
		
		return ((c << 6) + getBits(6));
	}

}
