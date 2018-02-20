public class Utility
{

    // TODO -> Transforms some byte ( e.g : 1 ) to binary ( e.g : 00000001 ) as 8 bits with sign
    public static String binaryRepresentation ( Byte inputByte )
    {
        return String.format ( "%8s", Integer.toBinaryString ( inputByte & 0xFF ) ).replace ( ' ', '0' );
    }

    // TODO -> Converts binary to byte ( e.g : 10000000 ) becomes ( -128 ) ( Byte MIN )
    private static Byte binaryStringToByte ( String bitString )
    {
        if ( bitString.length () > Byte.SIZE ) throw new NumberFormatException ();

        // TODO -> Find value of high-bit
        int result = bitString.charAt ( 0 ) == '0' ? 0 : Byte.MIN_VALUE;

        // TODO -> Add other portion to high-bit value
        result += Byte.parseByte ( bitString.substring ( 1, bitString.length () ), 2 );

        return ( byte ) result;
    }

    // TODO -> Converts a string of bytes in binary to actual bytes ( e.g : 10000000 00000001 ) becomes ( -128 1 )
    // TODO -> Spaces should be used to distinguish separate bits
    // TODO -> extraneous whitespace and punctuation will be removed to help processing
    public static byte[] binaryToBytes ( String stringOfBits )
    {
//        System.out.println ( "Transforming : " + stringOfBits + " to bytes" );

        String[] bytesAsBinary = stringOfBits.split ( "\\p{Space}" );
        byte[] bytes = new byte[bytesAsBinary.length];

        for ( int count = 0; count < bytesAsBinary.length; ++count )
            bytes[count] = binaryStringToByte ( bytesAsBinary[count] );

        return bytes;
    }


    // TODO -> Converts any sequence of Bytes to Binary representation
    // TODO -> e.g : ( 1 -1 127 -128 ) becomes ( 00000001 11111111 01111111 10000000 )
    public static String bytesToBinary ( byte... bytes )
    {
        StringBuilder bytesString = new StringBuilder ();

        for ( byte singleByte : bytes )
        {
            if ( bytesString.length () == 0 ) bytesString.append ( binaryRepresentation ( singleByte ) );
            else bytesString.append ( " " + binaryRepresentation ( singleByte ) );
        }

        return bytesString.toString ();
    }

    // TODO -> Convert Binary Text separated by whitespace to Bytes
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

    public static long hash ( byte... bytes )
    {
        long hash = 5381;

        for ( byte singleByte : bytes )
            hash = ( ( hash << 5 ) + hash ) + singleByte;

        return hash;
    }

    public static byte[] longToBytes ( long l )
    {
        byte[] result = new byte[8];
        for ( int i = 7; i >= 0; i-- )
        {
            result[i] = ( byte ) ( l & 0xFF );
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong ( byte[] b )
    {
        long result = 0;
        for ( int i = 0; i < 8; i++ )
        {
            result <<= 8;
            result |= ( b[i] & 0xFF );
        }
        return result;
    }

}
