import com.sun.deploy.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

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
        String [] byteStrings = stringOfBytes.split ( "\\p{Punct} | \\p{Space}" );

        ArrayList<Byte> bytes = new ArrayList<> (byteStrings.length);
        for ( String byteString : byteStrings )
            bytes.add ( new Byte ( byteString ) );

        return bytes;
    }

    public static String transformFileAtPathToBinary ( Path path ) throws IOException
    {
        StringBuilder xorKey = new StringBuilder ();

        try ( InputStream inputStream = Files.newInputStream ( path ) )
        {
            char[] zeros = new char [Byte.SIZE];
            Arrays.fill (zeros, '0');

            for ( Integer inputByte; ( inputByte = inputStream.read () ) != -1; )
            {
                String asBinary = Integer.toBinaryString ( inputByte );
                xorKey.append ( String.valueOf ( zeros, 0, Byte.SIZE - asBinary.length () ) + asBinary );
            }
        }

        return xorKey.toString ();
    }

    public static ArrayList<Byte> transformBinaryStringToBytes ( String stringOfBinary )
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
