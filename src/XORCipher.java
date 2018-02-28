import java.util.Arrays;

class XORCipher
{
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

    public static byte [] decrypt ( byte [] input, byte [] key )
    {
        return encrypt ( input, key );
    }
}
