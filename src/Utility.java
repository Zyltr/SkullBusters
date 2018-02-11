import java.util.ArrayList;

public class Utility
{
    // TODO -> Transforms some byte ( e.g : 1 ) to binary ( e.g : 00000001 ) as 8 bits with sign
    public static String binaryRepresentation ( Byte inputByte )
    {
        return String.format("%8s", Integer.toBinaryString(inputByte & 0xFF)).replace(' ', '0');
    }

    // TODO -> Converts binary to byte ( e.g : 10000000 ) becomes ( -128 ) ( Byte MIN )
    private static Byte binaryConverter ( String byteString )
    {
        if ( byteString.length () > Byte.SIZE )
            throw new NumberFormatException ();

        // TODO -> Find value of high-bit
        int result = byteString.charAt ( 0 ) == '0' ? 0 : Byte.MIN_VALUE;

        // TODO -> Add other portion to high-bit value
        result += Byte.parseByte ( byteString.substring ( 1, byteString.length () ), 2 );

        return ( byte ) result;
    }

    // TODO -> Converts a string of bytes in binary to actual bytes ( e.g : 10000000 00000001 ) becomes ( -128 1 )
    // TODO -> Spaces should be used to distinguish separate bits
    // TODO -> extraneous whitespace and punctuation will be removed to help processing
    public static ArrayList<Byte> binaryToBytes ( String stringOfBytes )
    {
        ArrayList<Byte> bytes = new ArrayList<> ();

        System.out.println ( "Transforming : " + stringOfBytes + " to bytes" );

        stringOfBytes = stringOfBytes.replaceAll ( "\\p{Punct}", "" );

        for ( String byteString : stringOfBytes.split ( "\\p{Space}" ) )
            bytes.add ( binaryConverter ( byteString ) );

        return bytes;
    }


    // TODO -> Converts any sequence of Bytes to Binary representation
    // TODO -> e.g : ( 1 -1 127 -128 ) becomes ( 00000001 11111111 01111111 10000000 )
    public static String bytesToBinary ( byte... bytes )
    {
        StringBuilder bytesString = new StringBuilder ();

        for ( byte singleByte : bytes )
        {
            if ( bytesString.length () == 0 )
                bytesString.append ( binaryRepresentation ( singleByte ) );
            else
                bytesString.append ( " " + binaryRepresentation ( singleByte ) );
        }

        return bytesString.toString ();
    }

    public static String bytesToString ( byte... bytes )
    {
        StringBuilder stringBuilder = new StringBuilder ();

        for ( byte aByte : bytes )
        {
            if ( stringBuilder.length () == 0 )
                stringBuilder.append ( Byte.toString ( aByte ) );
            else
                stringBuilder.append ( " " + Byte.toString ( aByte ) );
        }

        return stringBuilder.toString ();
    }

    public static byte [] stringToBytes ( String stringOfBytes )
    {
        if ( stringOfBytes.isEmpty () )
            return new byte[] {};

        String [] byteStrings = stringOfBytes.split ( "\\p{Space}" );

        byte [] bytes = new byte [byteStrings.length];
        int count = 0;

        for ( String aByte : byteStrings )
            bytes [count++] = Byte.parseByte ( aByte );

        return bytes;
    }
}
