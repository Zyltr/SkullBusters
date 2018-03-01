/**
 * This Class has many utility methods used by the application to performing vital, crucial operations such as converting
 * Bytes to Binary, String to Bytes, and so on.
 */
class Utility
{
    /**
     * Transforms some Byte ( e.g : 1 ) to a binary string of 8 Bits ( e.g : 00000001 )
     * @param inputByte : Byte that will be converted to a 8-Bit String
     * @return : the String representation of the Byte as 8 Bits
     */
    public static String binaryRepresentation ( Byte inputByte )
    {
        return String.format ( "%8s", Integer.toBinaryString ( inputByte & 0xFF ) ).replace ( ' ', '0' );
    }


    /**
     * Converts A String Binary Representation of a Byte to it's actual Byte value. First Bit is treated as Sign
     * (e.g : 10000000 ) becomes ( -128 ) ( Byte MIN )
     * @param bitString : A String Binary Representation of a Byte. First bit is treated as Sign Bit
     * @return : the Byte value of the Binary String
     */
    private static Byte binaryStringToByte ( String bitString )
    {
        if ( bitString.length () > Byte.SIZE ) throw new NumberFormatException ();

        // Find value of high-bit
        int result = bitString.charAt ( 0 ) == '0' ? 0 : Byte.MIN_VALUE;

        // Add other portion to high-bit value
        result += Byte.parseByte ( bitString.substring ( 1, bitString.length () ), 2 );

        return ( byte ) result;
    }


    /**
     * Converts a String of Bits in Binary to actual Byte values.
     * ( e.g : 10000000 00000001 ) becomes ( -128 1 )
     * Spaces should be used to distinguish separate bits
     * Extraneous whitespace and punctuation will be removed to help processing
     * @param stringOfBits : A String Representation of Bytes, as 8 Bit Strings, each separated by whitespace
     * @return : an Array of Bytes derived from the parameterized "String of Bits"
     */
    public static byte[] binaryToBytes ( String stringOfBits )
    {
        String[] bytesAsBinary = stringOfBits.split ( "\\p{Space}" );
        byte[] bytes = new byte[bytesAsBinary.length];

        for ( int count = 0; count < bytesAsBinary.length; ++count )
            bytes[count] = binaryStringToByte ( bytesAsBinary[count] );

        return bytes;
    }


    /**
     * Converts any sequence of Bytes to a String Binary Representation
     * E.g : ( 1 -1 127 -128 ) becomes ( 00000001 11111111 01111111 10000000 )
     * @param bytes : Bytes that will be converted to a Binary String
     * @return : a String of Bytes as Bits, each separated by whitespace
     */public static String bytesToBinary ( byte... bytes )
    {
        StringBuilder bytesString = new StringBuilder ();

        for ( byte singleByte : bytes )
        {
            if ( bytesString.length () == 0 ) bytesString.append ( binaryRepresentation ( singleByte ) );
            else bytesString.append ( " " ).append ( binaryRepresentation ( singleByte ) );
        }

        return bytesString.toString ();
    }


    /**
     * Convert A String of Bytes ( not Bits )  separated by whitespace to an Array of Bytes
     * @param stringOfBytes : a String of Bytes : e.g : [ -1, -2, 1, 2 ]
     * @return : an Array of Bytes derived from the String of Bytes
     */
    public static byte[] stringToBytes ( String stringOfBytes )
    {
        if ( stringOfBytes.isEmpty () ) return new byte[] { };

        String[] byteStrings = stringOfBytes.replaceAll ( "[\\p{Punct}&&[^+-]]", "" ).split ( "\\p{Space}" );

        byte[] bytes = new byte[byteStrings.length];
        int count = 0;

        for ( String aByte : byteStrings )
            bytes[count++] = Byte.parseByte ( aByte );

        return bytes;
    }


    /**
     * Computes a hash value for any given range of Bytes
     * @param bytes : a sequence of Bytes
     * @return : a hash value computed using the Bytes stored inside "bytes"
     */
    public static long hash ( byte... bytes )
    {
        long hash = 5381;

        for ( byte singleByte : bytes )
            hash = ( ( hash << 5 ) + hash ) + singleByte;

        return hash;
    }


    /**
     * Converts a long value to an Array of Bytes
     * @param longValue : the long value which will be converted to an Array of Bytes
     * @return : an Array of Bytes based on the value of the parameterized "longValue"
     */
    public static byte[] longToBytes ( long longValue )
    {
        byte[] result = new byte[8];
        for ( int i = 7; i >= 0; i-- )
        {
            result[i] = ( byte ) ( longValue & 0xFF );
            longValue >>= 8;
        }
        return result;
    }


    /**
     * Converts an Array of Bytes to a Long value
     * @param bytes : Bytes which will be used to convert to a Long value
     * @return : the Long value created using "bytes"
     */
    public static long bytesToLong ( byte[] bytes )
    {
        long result = 0;
        for ( int i = 0; i < 8; i++ )
        {
            result <<= 8;
            result |= ( bytes[i] & 0xFF );
        }
        return result;
    }
}
