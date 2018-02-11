import java.util.Arrays;

public class MIME
{
    private static String base64StringTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static char [] base64Table = base64StringTable.toCharArray ();

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

//        System.out.println ( encodingBuilder.toString () );

        for ( int count = 0; count < encodingBuilder.length (); count += 6 )
        {
            String sixBitString = encodingBuilder.substring ( count, count + 6 );

//            System.out.println ( sixBitString );

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

//            System.out.println ( "Value : " + value);
//            System.out.println ( "Binary Value : " + binaryValue );

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
                resultBuilder.append ( " " + binaryString.substring ( count, count + Byte.SIZE ) );
        }

//        System.out.println ( "Final Binary String : " + resultBuilder.toString () );

        return Utility.binaryToBytes ( resultBuilder.toString () );
    }

}
