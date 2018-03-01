import java.util.Arrays;

/**
 * Produces a new Byte Array by performing an iterative XOR operation on an "Input" and "Key" Byte Arrays
 */
class XORCipher
{
    /**
     * Produces a new Byte Array by performing an XOR operation on each Byte. If the "Key" Array is smaller, then it is
     * resized to match the length of "Input"
     * @param input : the "input" Byte Array
     * @param key : the "key" Byte Array
     * @return : an Array of Bytes which are produced by performing an XOR operation on each Byte inside "input"
     */
    public static byte [] encrypt ( byte [] input, byte [] key )
    {
        byte [] result = new byte [input.length];

        if ( key.length < input.length )
        {
            final int originalKeySize = key.length;
            key = Arrays.copyOf ( key, input.length );

            for ( int first = 0, second = originalKeySize; second < input.length; ++first, ++second )
                key [second] = key [first];
        }

        for ( int count = 0; count < input.length; ++count )
            result [count] = (byte) ( input [count] ^ key [count] );

        return result;
    }

    /**
     * Delegates to encrypt
     * @param input : the "input" Byte Array
     * @param key : the "key" Byte Array
     * @return : an Array of Bytes which are produced by performing an XOR operation on each Byte inside "input"
     */
    public static byte [] decrypt ( byte [] input, byte [] key )
    {
        return encrypt ( input, key );
    }
}
