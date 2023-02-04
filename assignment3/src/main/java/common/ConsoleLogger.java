/**
 * Author: Paul Gates
 * 
 * Logs errors to the console and deals with
 * asserts
 */
package common;

public class ConsoleLogger 
{
    
    /**
     * print_red
     * Desc:    Prints red letters to console
     */
    public static void print_red
        (
        String msg
        )
    {
        // print red to the console
        System.out.println("\u001B[31m"+msg+"\u001B[0m");
    }

    public static void print_cyan
        (
        String msg
        )
    {   
        System.out.println("\u001B[36m"+msg+"\u001B[0m");
    }

    /**
     * print_green
     * Desc:    Prints red letters to console
     */
    public static void print_green
        (
        String msg
        )
    {
        // print red to the console
        System.out.println("\u001B[32m"+msg+"\u001B[0m");
    }

    public static void print_yellow
        (
        String msg
        )
    {
        // print yellow to the console
        System.out.println("\u001B[33m"+msg+"\u001B[0m");
    }
    /**
     * assert_msg_cont
     * Desc:    assert exc is true
     */
    public static void assert_msg_cont
        (
            boolean exc,
            String  msg
        )
    {
        if( !exc )
            {
            print_red(msg);
            }
    }

    /**
     * assert_msg_cont
     * Desc:    assert exc is true
     * 
     * Note:    Should exit with a friendly message
     */
    public static void assert_msg_exit
        (
            boolean exc,
            String  msg
        )
    {
        if( !exc )
            {
            print_red( msg );
            System.exit( -1 );
            }
        

    }

    /**
     * log_err
     * Desc:    Prints an error message to the console
     */
    public static void log_err
        (
        String msg
        )
    {
        print_red( msg );
    }

    /**
     * log_err_rx
     * Desc:    Prints an error message to the console
     */
    public static void log_err_rx
        (
        //void
        )
    {
        print_red( "[RX_ERR] BAD RX" );
    }

    /**
     * log_success
     * 
     */
    public static void log_success
        (
        String msg
        )
    {
        print_green( msg );
    }

    public static void log_send_msg
        (
        String msg
        )
    {
        print_cyan( msg );

    }

    /**
     * log_msg
     * 
     */
    public static void log_msg
        (
        String msg
        )
    {
        print_yellow( msg );
    }
}
