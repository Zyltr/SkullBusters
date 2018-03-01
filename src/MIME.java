import java.util.Arrays;

/**
 * A Class that transforms an Array of Bytes into a BASE64 encoded String or a BASE64 encoded String into an Array of Bytes
 */
class MIME
{
    // base64StringTable :
    // base64Table : the index of each character in "base64StringTable" serves as the actual value in the Base64 table. Used for lookup.
    private static final String base64StringTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final char [] base64Table = base64StringTable.toCharArray ();


    /**
     * Encodes an Array of Bytes into a BASE64 encoded String
     * @param bytes : the Byte data which will be transformed to the BASE64 String
     * @return : the BASE64 encoded String created using "bytes"
     */
    public static String base64Encoding ( byte... bytes )
    {
        StringBuilder resultBuilder = new StringBuilder ();
        StringBuilder encodingBuilder = new StringBuilder ( Utility.bytesToBinary ( bytes ).replaceAll ( "\\p{Space}", "" ) );

        boolean needsPadding =  bytes.length % 3 > 0;
        int padding = ( 3 - ( bytes.length % 3 ) ) * Byte.SIZE;

        if ( needsPadding )
        {
            char [] zeroPadding = new char [padding];
            Arrays.fill ( zeroPadding, '0' );
            encodingBuilder.append ( zeroPadding );
        }

        for ( int count = 0; count < encodingBuilder.length (); count += 6 )
        {
            String sixBitString = encodingBuilder.substring ( count, count + 6 );

            if ( needsPadding && count >= encodingBuilder.length () - padding )
                resultBuilder.append ( "=" );
            else
            {
                int sixBitValue = Integer.parseInt ( sixBitString, 2 );
                resultBuilder.append ( base64Table[sixBitValue] );
            }
        }

        return resultBuilder.toString ();
    }


    /**
     * Decodes a Base64 String to an Array of Bytes
     * @param base64String : the BASE64 encoded String
     * @return : the Bytes represented by the BASE64 encoded String
     */
    public static byte [] base64Decoding ( String base64String )
    {
        StringBuilder resultBuilder = new StringBuilder ();
        int totalPaddedBits = 0;

        for ( Character character : base64String.toCharArray () )
        {
            if ( character == '=' )
            {
                totalPaddedBits += resultBuilder.length () > 0 ? Byte.SIZE : Byte.SIZE - 2;
                resultBuilder.append ( "000000" );
                continue;
            }

            Byte value = (byte) base64StringTable.indexOf ( character );
            String binaryValue = Utility.binaryRepresentation ( value ).substring ( 2 );

            resultBuilder.append ( binaryValue );
        }

        resultBuilder = resultBuilder.delete ( resultBuilder.length () - totalPaddedBits, resultBuilder.length () );
        String binaryString = resultBuilder.toString ();
        resultBuilder = new StringBuilder ();

        for ( int count = 0; count < binaryString.length (); count += Byte.SIZE )
        {
            if ( resultBuilder.length () == 0 )
                resultBuilder.append ( binaryString.substring ( count, count + Byte.SIZE ) );
            else
                resultBuilder.append ( " " ).append ( binaryString.substring ( count, count + Byte.SIZE ) );
        }

        return Utility.binaryToBytes ( resultBuilder.toString () );
    }
}
