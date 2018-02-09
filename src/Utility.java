import com.sun.deploy.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Utility
{
    private static char [] zeros = { '0', '0', '0', '0', '0', '0', '0', '0' };

    public static String byteArrayToString ( byte[] bytes )
    {
        StringBuilder byteStringBuilder = new StringBuilder ();

        for ( int count = 0; count < bytes.length; ++count )
            byteStringBuilder.append ( bytes[count] ).append ( count + 1 < bytes.length ? " " : "" );

        return byteStringBuilder.toString ();
    }

    public static ArrayList<Byte> byteStringToArrayList( String stringOfBytes )
    {
//        String [] byteStrings = stringOfBytes.split ( "\\p{Punct} | \\p{Space}" );
        String [] byteStrings = stringOfBytes.split ( " " );

        ArrayList<Byte> bytes = new ArrayList<> (byteStrings.length);
        for ( String byteString : byteStrings )
            bytes.add ( new Byte ( byteString ) );

        return bytes;
    }

    public static String transformByteToBinary ( Byte inputByte )
    {
        String asBinary = Integer.toBinaryString ( inputByte );
        return String.valueOf ( zeros, 0, Byte.SIZE - asBinary.length () ) + asBinary;
    }

    public static String transformBytesToBinary ( Byte... inputBytes )
    {
        StringBuilder bytes = new StringBuilder ();

        for ( Byte singleByte : inputBytes )
            bytes.append ( transformByteToBinary ( singleByte ) );

        return bytes.toString ();
    }

    public static String transformBytesToBinary ( byte... inputBytes )
    {
        StringBuilder bytes = new StringBuilder ();

        for ( byte aByte : inputBytes )
            bytes.append ( transformByteToBinary ( aByte ) );

        return bytes.toString ();
    }

    public static ArrayList<Byte> transformBinaryToBytes ( String stringOfBinary )
    {
        ArrayList<Byte> bytes = new ArrayList<> ();

        for ( int origin = 0; origin < stringOfBinary.length (); origin += Byte.SIZE )
        {
            if ( origin + Byte.SIZE > stringOfBinary.length () )
                bytes.add ( new Byte ( stringOfBinary.substring ( origin, stringOfBinary.length () - origin ) ) );
            else
            {
                bytes.add ( new Byte ( stringOfBinary.substring ( origin, Byte.SIZE ) ) );
            }
        }

        return bytes;
    }

}
