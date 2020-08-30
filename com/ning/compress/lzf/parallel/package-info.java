/**
Package that contains parallel implementation of LZF compressor: granularity
is at chunk-level, so that each processing thread operates on a single chunk
at a time (and conversely, no chunk is "split" across threads).
<p>
The main abstraction to use is {@link com.ning.compress.lzf.parallel.PLZFOutputStream},
which orchestrates operation of multi-thread compression.
 */

package com.ning.compress.lzf.parallel;
