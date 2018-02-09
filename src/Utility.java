import java.util.ArrayList;

public class Utility
{
    public static String byteArrayToString ( byte[] bytes )
    {
        StringBuilder byteStringBuilder = new StringBuilder ();

        for ( int count = 0; count < bytes.length; ++count )
            byteStringBuilder.append ( bytes[count] ).append ( count + 1 < bytes.length ? " " : "" );

        return byteStringBuilder.toString ();
    }

    public static ArrayList<Byte> byteStringToArrayList( String stringOfBytes )
    {
        String [] byteStrings = stringOfBytes.split ( " " );

        ArrayList<Byte> bytes = new ArrayList<> (byteStrings.length);
        for ( String byteString : byteStrings )
            bytes.add ( new Byte ( byteString ) );

        return bytes;
    }
}
