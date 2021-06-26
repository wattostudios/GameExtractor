/**
Package that contains optimized stream implementations for working
with GZIP. Internally JDK provided efficient ZLIB codec is used for
actual encoding and decoding.
Code here
adds appropriate reuse to specifically improve handling of relatively
short compressed data; and may also have better support for alternate
operating modes such as "push-style" handling that is needed for
non-blocking ("async") stream processing.
*/

package com.ning.compress.gzip;
