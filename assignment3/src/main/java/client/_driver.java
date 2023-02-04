package client;

import static common.ConsoleLogger.assert_msg_exit;
import static common.GameState.GS_UNINIT;

import network.NetworkIntf;
import network.TCPNetworkIntf;
import network.UDPNetworkIntf;

import common.GamePacket;
import common.GameState;
import common.MessagePacket;
import common.MessageActionListener;

import java.awt.event.*;
import java.io.BufferedInputStream;
import java.awt.image.BufferedImage;

import javax.crypto.spec.DESKeySpec;
import javax.management.OperationsException;

import static common.ConsoleLogger.*;
import static common.GameState.*;

import server.GameReturnCode;

import gui.ClientGui;
/**
 * Main client side driver
 */
public class _driver {

    private NetworkIntf networkIntf = null;
    private GameState   gameState   = null;
    private ClientGui   guiHandle   = null;
    private String      response    = null;

    /**
     * shipClient
     * 
     * sends a message to the server and puts us in
     * an idle state to await their next message
     */
    private void shipClientMessage
        (
        MessagePacket message
        )
        {
        /**
         * Input validation
         */
        assert_msg_cont( ( message != null ), "Message was null" );

        /**
         * Local variables
         */
        BufferedImage image = null;
        
        /**
         * Send the server our message
         */
        networkIntf.network_intf_tx( message );

        /**
         * Wait for message (blocking call)
         * reuse the message packet
         */
        networkIntf.network_intf_rx( message );
        /**
         * Check for image flag set
         */
        if( message.img_flag )
            {
            /**
             * Grab the image from the message
             */
            image = networkIntf.network_intf_rx( );

            /**
             * Log output
             */
            assert_msg_cont( (  image != null ), "Image returned from rx proc was null" );
            }
            
        /**
         * After we have received a message 
         * from the server parse it
         */
        parseServerInput( message, image );

        }

    /**
     * parseServerInput
     * 
     * Desc:    Client side state machine. Handles
     *          commands from the user
     */
    private void parseServerInput
        (
        MessagePacket message,
        BufferedImage image
        )
    {        
        /*
         * Logg the game state that we need to handle
         */
        log_msg( "Server sent a message with game state: " 
                 + message.gameState );

        if  ( ( message.msg_data.equals( GameReturnCode.GM_CODE_PL_LS.toString() ) )
        ||   ( message.msg_data.equals( GameReturnCode.GM_CODE_PL_WN.toString() ) ) )
            {
            log_msg("Game is over");
            /*
             * shut down the gui
             */
            guiHandle.endGamePage();

            /*
             * Grab the next message from the server
             * and recurse
             */
            MessagePacket nextMessage = new MessagePacket();
            networkIntf.network_intf_rx( nextMessage );

            parseServerInput( nextMessage, null );
            }


        if( message.gameState == GS_LDR_BRD )
            {
            log_msg("Server sent a leaderboard message. Parsing message"
                         + "Showing popup with server data");
            
            /* update the module game state */

            gameState = GS_LDR_BRD;

            displayLeaderBoard( message );
            }
        
        /**
         * If the server sent us a menu state message
         * this means that we are in the preliminary
         * stage of the game and need to show the
         * pop-menu with the given server data
         */
        if( message.gameState == GS_GET_USR )
            {
            log_msg("Server sent a request to get the current " 
                    + "user" );

            /* update the module game state */
            gameState = GS_GET_USR;

            getUserIn( message );
            return;
            }

        /**
         * If the server sent us a menu state message
         * this means that we are in the preliminary
         * stage of the game and need to show the
         * pop-menu with the given server data
         */
        if( message.gameState == GS_MENU )
            {
            log_msg("Server sent menu message. Parsing message"
                         + "Showing popup with server data");

            displayMenu( message );

            return;
            }

        if( message.gameState == GS_GAME_INIT )
            {
            if( image == null )
                {
                log_err( "Server sent a game init message but "
                         + "no image was attached");
                }

            log_msg(" Server sent message to init the game" );

            /*
             * update the game state 
            */
            gameState = GS_GAME_MV;System.err.println();

            /*
             * !!! BLOCKING CALL DO NOT PUT ANYTHING BELOW THIS LINE !!!
             */
            guiHandle.startGamePage( message.game_pkt, image );

            return;
            }

       
        /**
         * handle game over
         * 
         * R14: The client shall handle the quitting of the game
         *      gracefully
         */
        if( message.gameState == GS_GAME_OVR )
            {
            log_msg("Server sent game over message. Parsing message");
            
            /**
             * shut down the GUI
             */
            guiHandle.endGamePage();

            /*
             * update the game state and send back
             * the game over message
            */
            gameState = GS_GAME_OVR;

            return;
            }

        if( gameState == GS_GAME_MV )
            {
            /*
             * update GUI components 
             * R10, R11, R12, R13: The Client shall display the current
             *                     points accordingly on the GUI
             * 
             * R15: The client shall display the current points
            */
            guiHandle.sendOutput( message.game_pkt.updt_msg );
            guiHandle.updatePoints( Integer.valueOf( message.game_pkt.points ) );
            guiHandle.updateCurrentWord( message.game_pkt.curr_word );

            }
    }

    /**
     * displayLeaderBoard
     * 
     * Desc:    Displays the leaderboard to the user using the
     *          options provided by the server
     */
    private void displayLeaderBoard
        (
        MessagePacket message
        )
    {
        SendData mSendData = new SendData();
        guiHandle.showLeaderBoard( message.msg_data, mSendData );
    }
    /**
     * displayMenu
     * 
     * Desc:    Displays the menu to the user using the
     *          options provided by the server
     */
    private void displayMenu
        (
        MessagePacket message
        )
    {

        SendData mSendData = new SendData();

        // TODO unhard code these values
        String[] options = { "Play the game", "View the leaderboard" };

        guiHandle.showMenu
            (
                options,
                mSendData
            );
    }


    /**
     * getUserIn
     * 
     * Desc:    Procedure for user input display type
     * 
     */
    private void getUserIn
        (
        MessagePacket message
        )
    {
        /**
         * get the server's message
         */
        String messageString = message.msg_data;

        /**
         * "TX" a message. TX in quotes because really sending to
         * GUI module but we don' make any assumptions here
         */
        SendData sendDataProc = new SendData();

        /**
         * Synchronize the messages with the server
         */
        sendDataProc.messagePacket.gameState = GS_GET_USR;

        guiHandle.showNamePrompt( messageString, sendDataProc );
    }

    /**
     * Initializes and runs the GUI
     */
    private boolean bootGUI
    (
    //void
    )
    {

        /**
         * get a gui handle
         */
        SendGameMove gameUserInHandler =  new SendGameMove();
        guiHandle = new ClientGui( gameUserInHandler );

        return true;

    }


    /*
     * Class members
     */
    String protocol = null;
    String host     = null;
    int    port     = -1;
    
    public _driver
        (
        String protocol,
        String host,
        int    port 
        )
    {
        /**
         * Set up class data members
         */
        this.protocol   = protocol;
        this.host       = host;
        this.port       = port;

        /**
         * Attempt to establish a connection using the
         * provided network interface
         */
        if ( connect( protocol, host, port ) )
            {
            /**
             * After connection has been established go ahead
             * and listen for messages from the server
             */
            kickOff();
            }

    }

    /**
     * kickOff
     * 
     * Desc:    Main entry point for starting the application.
     *          Utilizes a common object shared between the 
     *          the client (us) and server, in order to process
     *          data at a high level
     * 
     * Note:    These objects are eventually resolved into json
     *          and again into byte data that is sent over the
     *          wire to the server
     */
    private void kickOff
        (
        //void
        )
    {   
        /**
         * Set our state to initialized
         */
        gameState = GS_INIT;

        /**
         * Boot the GUI
         */
        bootGUI();

        /**
         * TX start up message to the server (blocking call)
         */
        MessagePacket outMessagePacket = new MessagePacket();
        outMessagePacket.gameState = GS_INIT;
        outMessagePacket.msg_data = "Hello server!";
        
        networkIntf.network_intf_tx( outMessagePacket );

        /**
         * RX message from server (blocking call)
         */
        MessagePacket inMessagePacket = new MessagePacket();
        networkIntf.network_intf_rx( inMessagePacket );

        /**
         * Yay! Received first message. Consult with the
         * server on what to do next
         */
        parseServerInput( inMessagePacket, null );
    }

    /**
     * connect
     * 
     * Desc:    The application can either be ran with TCP or UDP
     *          networking protocols. 
     * 
     * Notes:   Before launching GUI, must establish connection with
     *          the server
     * 
     *          Depending on which is chosen
     *          will affect the type of interface this module will
     *          interact with. However, this module will not be
     *          explosed to the underlying procotocol details
     */
    private boolean connect
        (
        String  protocol,
        String  host,
        int     port
        )
    {
        /**
         * determine the correct protocol
         */
        if( protocol.equals("-tcp" ) )
            {
            /**
             * This will initialize the TCP Network interface
             */
            networkIntf = new TCPNetworkIntf(host, port);
            log_msg("CONFIG=TCP");
            }
        else if ( protocol.equals("-udp" ) )
            {
            /**
             * This will initialize the UDP Network interface
             */
            networkIntf = new UDPNetworkIntf( host, port, false );

            log_msg("CONFIG=UDP" );  
            }
        /*
         * Unkown configuration
         */
        else
            {
            assert_msg_exit( false, "Unknown configuration" );
            }
        /**
         * indicate a successful connection
        */
        return true;
    }

    /**
     * Application Entry
     * 
     * Expects to be ran as:
     * gradle runClient [-udp | -tcp] [server host | ip] [port] 
     */
    public static void main(String[] aStrings)
    {

        /* parse the command line arguments
        * 
        */
        if( aStrings.length <  3 )
            {
            System.out.println( "Not enough arguments specified to run this "
                                +  "application\n"
                                + "gradle runClient <-udp / -tcp> <server host / ip> < port >" );
            }

        /**
         *  Verify command line arguments 
         */
        String protocol   = aStrings[ 0 ];
        String hostString = aStrings[ 1 ];
        int    portNum    = Integer.parseInt(aStrings[ 2 ]);

        /**
         * Attempt to start a network connection
         */
        _driver driver = new _driver(protocol, hostString, portNum);

    }

    // TODO PMG mot sure if I need these are not TDB
    /**
     * CALLBACK PROCEDURES
     */

     /**
      * SendName
      * Listener used for sending data to the server
      */
     public class SendData extends MessageActionListener
     {
        /**
         *  SendData
         */
        public SendData
            (
            //void
            )
        {
            super();
        }

        @Override
        /**
         * actionPerformned
         * 
         * send the server the name
         */
        public void actionPerformed(ActionEvent e)
        {

            /**
             * Log this event
             */
            log_msg( "SendData.actionPerformed callback invoked" );
             
            /**
             * Input validation
             */
            if( (            this.messagePacket == null )
             ||  ( this.messagePacket.msg_data  == null ) )
                {
                log_msg( "No message from embedded call" );
                return;
                }
            /**
             * Ensure the server is aware of our current
             * state
             */
            messagePacket.gameState = gameState;

            /**
             * send the message. This call contains
             * a blocking call that will wait for
             * the server to send us a message
             */
            shipClientMessage( messagePacket );

        }

     }

     public class SendGameMove extends SendData
     {
        /**
         *  SendData
         */
        public SendGameMove
            (
            //void
            )
        {
            super();
        }

        @Override
        /**
         * actionPerformned
         * 
         * send the server the name
         */
        public void actionPerformed(ActionEvent e)
        {
            this.messagePacket.gameState = GS_GAME_MV;
            /**
             * Log this event
             */
            log_msg( "SendGameMove.actionPerformed callback invoked" );

            super.actionPerformed( e );
        }

     }
}
