/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.util.sut;

import java.nio.ByteBuffer;

public class TStringImpl implements TString {
    /**
     * The value is used for character storage.
     */
    public char value[];

    /**
     * The count is the number of characters in the String.
     */
    int count;

    /**
     * Cache the hash code for the string
     */
    private int hash = 0;

    /**
     * Initializes a newly created <code>TString</code> object so that it
     * represents the same sequence of characters as the argument; in other
     * words, the newly created string is a copy of the argument string. Unless
     * an explicit copy of <code>original</code> is needed, use of this
     * constructor is unnecessary since Strings are immutable.
     *
     * @param original a <code>String</code>.
     */
    TStringImpl(String original) {
        this.count = original.length();
        this.value = new char[this.count];
        original.getChars(0, this.count, this.value, 0);
    }

    TStringImpl(TString original) {
        TStringImpl ts = (TStringImpl) original;
        this.count = ts.count;
        this.value = new char[this.count];
        System.arraycopy(ts.value, 0, this.value, 0, this.count);
    }

    TStringImpl(String original, int start, int end) {
        this.count = end - start;
        this.value = new char[this.count];
        original.getChars(start, end, this.value, 0);
    }

    /**
     * Allocates a new <code>TString</code> so that it represents the
     * sequence of characters currently contained in the character array
     * argument. The contents of the character array are copied; subsequent
     * modification of the character array does not affect the newly created
     * string.
     *
     * @param value the initial value of the string.
     */
    TStringImpl(char value[]) {
        this.count = value.length;
        this.value = new char[count];
        System.arraycopy(value, 0, this.value, 0, count);
    }

    /**
     * Allocates a new <code>TString</code> that contains characters from
     * a subarray of the character array argument. The <code>offset</code>
     * argument is the index of the first character of the subarray and
     * the <code>count</code> argument specifies the length of the
     * subarray. The contents of the subarray are copied; subsequent
     * modification of the character array does not affect the newly
     * created string.
     *
     * @param value  array that is the source of characters.
     * @param offset the initial offset.
     * @param count  the length.
     * @throws IndexOutOfBoundsException if the <code>offset</code>
     *                                   and <code>count</code> arguments index characters outside
     *                                   the bounds of the <code>value</code> array.
     */
    TStringImpl(char value[], int offset, int count) {
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        // Note: offset or count might be near -1>>>1.
        if (offset > value.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }

        this.value = new char[count];
        this.count = count;
        System.arraycopy(value, offset, this.value, 0, count);
    }

    public TStringImpl() {
    }


    /**
     * Returns the length of this string.
     * The length is equal to the number of 16-bit
     * Unicode characters in the string.
     *
     * @return the length of the sequence of characters represented by this
     * object.
     */
    public int length() {
        return count;
    }

    public int getByteSize() {
        return 4 + 2 * count;
    }

    public void appendSelf(ByteBuffer buf) {
        buf.putInt(count);
        for (int i = 0; i < count; i++) {
            buf.putChar(value[i]);
        }
    }

    public int readInFromBytes(byte[] arr, int from) {
        count = sut.getIntegerFromBytes(arr, from);
        from += 4;
        if (value == null || value.length < count) {
            this.value = new char[count];
        }
        for (int i = 0; i < count; i++) {
            value[i] = sut.getCharFromBytes(arr, from);
            from += 2;
        }
        hash = 0;

        return from;
    }

    /**
     * Returns the character at the specified index. An index ranges
     * from <code>0</code> to <code>length() - 1</code>. The first character
     * of the sequence is at index <code>0</code>, the next at index
     * <code>1</code>, and so on, as for array indexing.
     *
     * @param index the index of the character.
     * @return the character at the specified index of this string.
     * The first character is at index <code>0</code>.
     * @throws IndexOutOfBoundsException if the <code>index</code>
     *                                   argument is negative or not less than the length of this
     *                                   string.
     */
    public char charAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public void slurp(StringBuffer buf) {
        int len = buf.length();
        this.count = len;
        if (value == null || value.length < len) {
            this.value = new char[len];
        }
        buf.getChars(0, this.count, this.value, 0);
        hash = 0;
    }

    public void slurp(String str) {
        int len = str.length();
        this.count = len;
        if (value == null || value.length < len) {
            this.value = new char[len];
        }
        str.getChars(0, this.count, this.value, 0);
        hash = 0;
    }

    public void slurp(char[] chars, int begin, int count) {
        this.count = count;
        if (value == null || value.length < count) {
            this.value = new char[count];
        }
        System.arraycopy(chars, 0, this.value, 0, count);
        hash = 0;
    }

    /**
     * Copies characters from this string into the destination character
     * array.
     * <p/>
     * The first character to be copied is at index <code>srcBegin</code>;
     * the last character to be copied is at index <code>srcEnd-1</code>
     * (thus the total number of characters to be copied is
     * <code>srcEnd-srcBegin</code>). The characters are copied into the
     * subarray of <code>dst</code> starting at index <code>dstBegin</code>
     * and ending at index:
     * <p><blockquote><pre>
     *     dstbegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param srcBegin index of the first character in the string
     *                 to copy.
     * @param srcEnd   index after the last character in the string
     *                 to copy.
     * @param dst      the destination array.
     * @param dstBegin the start offset in the destination array.
     * @throws IndexOutOfBoundsException If any of the following
     *                                   is true:
     *                                   <ul><li><code>srcBegin</code> is negative.
     *                                   <li><code>srcBegin</code> is greater than <code>srcEnd</code>
     *                                   <li><code>srcEnd</code> is greater than the length of this
     *                                   string
     *                                   <li><code>dstBegin</code> is negative
     *                                   <li><code>dstBegin+(srcEnd-srcBegin)</code> is larger than
     *                                   <code>dst.length</code></ul>
     */

    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > count) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        System.arraycopy(value, srcBegin, dst, dstBegin,
                srcEnd - srcBegin);
    }

    public char[] toCharArray() {
        char t[] = new char[count];
        System.arraycopy(value, 0, t, 0, count);
        return t;
    }

    public void appendToStringBuffer(StringBuffer buf) {
        buf.append(value);
    }


    /**
     * Compares this string to the specified object.
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>String</code> object that represents
     * the same sequence of characters as this object.
     *
     * @param anObject the object to compare this <code>String</code>
     *                 against.
     * @return <code>true</code> if the <code>String </code>are equal;
     * <code>false</code> otherwise.
     * @see java.lang.String#compareTo(java.lang.String)
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof TStringImpl) {
            TStringImpl anotherString = (TStringImpl) anObject;
            int n = count;
            if (n == anotherString.count) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = 0;
                int j = 0;
                while (n-- != 0) {
                    if (v1[i++] != v2[j++])
                        return false;
                }
                return true;
            }
        } else if (anObject instanceof String) {
            String anotherString = (String) anObject;
            int n = count;
            if (n == anotherString.length()) {
                char v1[] = value;
                int i = 0;
                int j = 0;
                while (n-- != 0) {
                    if (v1[i++] != anotherString.charAt(j++))
                        return false;
                }
                return true;
            }
        } else if (anObject instanceof char[]) {
            char[] anotherArr = (char[]) anObject;
            int n = count;
            if (n == anotherArr.length) {
                char v1[] = value;
                int i = 0;
                int j = 0;
                while (n-- != 0) {
                    if (v1[i++] != anotherArr[j++])
                        return false;
                }
                return true;
            }

        }
        return false;
    }

    /**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in
     * the strings. The character sequence represented by this
     * <code>String</code> object is compared lexicographically to the
     * character sequence represented by the argument string. The result is
     * a negative integer if this <code>String</code> object
     * lexicographically precedes the argument string. The result is a
     * positive integer if this <code>String</code> object lexicographically
     * follows the argument string. The result is zero if the strings
     * are equal; <code>compareTo</code> returns <code>0</code> exactly when
     * the {@link #equals(Object)} method would return <code>true</code>.
     * <p/>
     * This is the definition of lexicographic ordering. If two strings are
     * different, then either they have different characters at some index
     * that is a valid index for both strings, or their lengths are different,
     * or both. If they have different characters at one or more index
     * positions, let <i>k</i> be the smallest such index; then the string
     * whose character at position <i>k</i> has the smaller value, as
     * determined by using the &lt; operator, lexicographically precedes the
     * other string. In this case, <code>compareTo</code> returns the
     * difference of the two character values at position <code>k</code> in
     * the two string -- that is, the value:
     * <blockquote><pre>
     * this.charAt(k)-anotherString.charAt(k)
     * </pre></blockquote>
     * If there is no index position at which they differ, then the shorter
     * string lexicographically precedes the longer string. In this case,
     * <code>compareTo</code> returns the difference of the lengths of the
     * strings -- that is, the value:
     * <blockquote><pre>
     * this.length()-anotherString.length()
     * </pre></blockquote>
     *
     * @param anotherString the <code>String</code> to be compared.
     * @return the value <code>0</code> if the argument string is equal to
     * this string; a value less than <code>0</code> if this string
     * is lexicographically less than the string argument; and a
     * value greater than <code>0</code> if this string is
     * lexicographically greater than the string argument.
     */
    public int compareTo(TStringImpl anotherString) {
        int len1 = count;
        int len2 = anotherString.count;
        int n = Math.min(len1, len2);
        char v1[] = value;
        char v2[] = anotherString.value;
        int i = 0;
        int k = i;

        int lim = n + i;
        while (k < lim) {
            char c1 = v1[k];
            char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    public int compareTo(Object o) {
        return compareTo((TStringImpl) o);
    }


    /**
     * Returns a hash code for this string. The hash code for a
     * <code>String</code> object is computed as
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * </pre></blockquote>
     * using <code>int</code> arithmetic, where <code>s[i]</code> is the
     * <i>i</i>th character of the string, <code>n</code> is the length of
     * the string, and <code>^</code> indicates exponentiation.
     * (The hash value of the empty string is zero.)
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            int off = 0;
            char val[] = value;
            int len = count;

            for (int i = 0; i < len; i++) {
                h = 31 * h + val[off++];
            }
            hash = h;
        }
        return h;
    }

    public String toString() {
        return new String(value);
    }
}
