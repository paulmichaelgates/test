/**
 * Main Driver code for the server side
 * of the application 
 */
package server;
import common.NetworkUserAPIs;

import static common.ConsoleLogger.*;

import java.sql.Driver;

import javax.sound.sampled.Port;

import static common.GameState.*;

import common.GamePacket;
import common.GameState;
import common.MessageActionListener;
import common.MessagePacket;

import java.io.FileWriter;
import java.io.FileReader;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;

import java.awt.image.BufferedImage;

import static server.GameReturnCode.*;
import static common.UDPPublic.*;

//TODO remove imports after test
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
public class _driver 
{
    /**
     * Variables
     */
    GameState         gameState;
    String            currentPlayerNameString;
    NetworkUserAPIs   networkUserAPIs;
    
    public static void main( String[] args )
    { 
        /**
         * Check for arguments
         */
        if( args.length < 2 )
            {
            assert_msg_exit( false, 
                "Missing arguments, expects to be ran as\n"
                + "gradle runServer [-tcp/-udp] [port]" );
            }

        /**
         * Otherwise, process the arguments
         */
        String  config = args[ 0 ];
        int     port   = Integer.parseInt( args[ 1 ] );

        /**
         * Start the server 
         */
        _driver driver = new _driver( config, port );

    }

    /**
     * _driver
     */
    public _driver
        (
        String config,
        int port
        )
    {
        /*
         * Establishes connection with a client
         */
        connect( config, port );

        /**
         * If no errors were thrown then we can start
         * the state machine for the server
         */
        serverMain();

    }

    /**
     * serverMain main control center for the server 
     * 
     * REQ17 The server shall handle all input from the client
     */
    private void serverMain
        (
        //void
        )
    {
        /**
         * Init the game state to unit, we have not talked to
         * client yet so we do not know if they have initialized
         * on their end yet 
         */

         gameState = GS_UNINIT;

        while( true )
            {
                
            /**
             * Always check for clients that might
             * be trying to connect
             */
            networkUserAPIs.dealWithOtherClients();

            /**
             * Check for messages from the client
             */
            if( gameState == GS_UNINIT )
                {
                /**
                 * Check for messages from the client
                 */
                MessagePacket messagePacket = new MessagePacket();
                networkUserAPIs.userNetworkRX( messagePacket );

                /*  
                 * Check if the client has initialized
                 *
                 */
                if( messagePacket.gameState == GS_INIT )
                    {
                    /**
                     * Update to send the client the menu
                     * request mode
                     */
                    gameState = GS_GET_USR;
                    }
                }

            /**
             * request user information
             */
            if(  gameState == GS_GET_USR )
                {
                sendClientInit();
                }

            /**
             * Menu Request
             */
            if(  gameState == GS_MENU )
                {
                sendOptionsMenu();
                }

            /**
             * Entry Point into starting a new game
             */
            if(  gameState == GS_GAME_INIT )
                {
                log_msg( "The client has requested we start the game" );
                playGame();
                }
            
            /**
             * Leader Board Request
             */
            if(  gameState == GS_LDR_BRD )
                {
                log_msg( "The client has requested we show them the leader board" );

                /*
                 * send the leaderboard to the client
                */
                MessagePacket messagePacket = new MessagePacket();
                messagePacket.gameState     = GS_LDR_BRD;
                messagePacket.game_pkt      = new GamePacket();
                messagePacket.msg_data      = readInLeaderBoard();

                /**
                 * REQ4: Send the client the leader board
                 *
                 */
                shipMessage( messagePacket );
                
                /*
                 * Return to the menu
                 */
                gameState = GS_MENU;

                }                
            }
    }

    /**
     * readInLeaderBoard
     */
    public String readInLeaderBoard
        (
        //void
        )
    {
      /*
       * Read in the leader board
       * from the local text file
       */
        String leaderBoard = "";
        try
            {
            BufferedReader br = new BufferedReader(new FileReader("leaderboard.txt"));
            String line = br.readLine();

            while (line != null) 
            {
                leaderBoard += line;
                leaderBoard += "\n";
                line = br.readLine();

            }
            br.close();

            }
        catch( IOException e )
            {
                e.printStackTrace();
            }
        
            return leaderBoard;

    }
    /**
     * playGame
     * 
     * Desc:    Controls in game logic via
     *          a state machine
     */
    private void playGame
        (
        //void
        )
    {
        /**
         * local vars
         */
        GameReturnCode gameReturnCode = GM_CODE_UNK;
    
        /**
         * Send the client the GAME INIT message
         * which will tell them to do all the 
         * things they need to do to start a game
         */
        MessagePacket messagePacket = new MessagePacket();
        messagePacket.gameState     = GS_GAME_INIT; 

        /**
         * Initialize the game logic
         */
        BufferedImage out_img = null;
        messagePacket.game_pkt  = new GamePacket();
        GameLogic gameLogic = GameLogic.InitGameLogic
                        (
                        currentPlayerNameString,
                        messagePacket.game_pkt
                        );

        /**
         * Save off the current word
         */
        String currentWord = messagePacket.game_pkt.curr_word;

        /*
         * log the message from the game onto the console
         */
        log_msg("From the game: " + messagePacket.game_pkt.updt_msg);

       messagePacket.msg_data = gameLogic.getWord();  //debug purposes 
       out_img = gameLogic.getImage();

        /**
         * REQ7: Show the game word on the terminal
         */
        print_cyan( "The word is: " + messagePacket.game_pkt.curr_word );
        /**
         * blocking call send game start up packet
         */
        shipMessage( messagePacket, out_img, false );

        /**
         * Clear message packet
         */
        messagePacket = new MessagePacket();

        /*
         * If this is a fresh game packet reserver
         * some memory for it
         */
        if(  messagePacket.game_pkt == null )
            {
            messagePacket.game_pkt = new GamePacket();
            }

        /**
         * restore the current word
         */
        messagePacket.game_pkt.curr_word = currentWord;


        /* Play the game until the player loses,
         * the player wins, or they quit
         */
        while( (  gameReturnCode != GM_CODE_PL_LS ) 
        &&     (  gameReturnCode != GM_CODE_PL_WN ) )
            {
            /**
             * For debug purposes print out the name of the word
             */
            log_msg( "The Game Word is: " +  gameLogic.getWord() );

            /* default to quit in case we lose connection with client 
            */
            messagePacket.msg_data = "quit";   

            /**
             * save off the current word
             */
            currentWord = messagePacket.game_pkt.curr_word;
            /**
             * Get the user input (Blocking call) this procedure
             * will write the message to the messagePacket
             * which will contain the user input for this move
             */
            networkUserAPIs.userNetworkRX( messagePacket );

            /**
             * restore the current word
             */
            messagePacket.game_pkt.curr_word = currentWord;

            /**
             * Log the return message on the console
             */
            log_msg( "From the client: " + messagePacket.msg_data );
            
            /**
             * Check if the user wants to quit
             * 
             * REQ8: The user shall be able to quit the game
             */
            if( messagePacket.msg_data.toLowerCase().contains( "quit" ) )
                {
                log_msg( "The user has requested to quit" );
                break;
                }
        
            /* 
             * Give user input to the game logic module
            */
            gameReturnCode = gameLogic.insertMove( messagePacket );
            messagePacket.msg_data = gameReturnCode.toString();

            log_success( "The game has returned: " + gameReturnCode.toString() );

            /** 
             * Send the game logic response to the client
             * REQ9: The client shall be able to receive the game
             * response from the server
            */
            networkUserAPIs.userNetworkTX( messagePacket );

            }

        
        /**
         * REQ4: log to leaderboard
         */
        log_msg( "The game has ended. Go ahead and log this play to the "
                +    "leaderboard" );
        logGameToLeaderBoard( messagePacket );

        
        /**
         * REQ20: After the game has ended the client shall be able to
         *        return to the main menu
         */
        gameState = GS_MENU;

    }

    /**
     * logGameToLeaderBoard
     * REQ5
     */
    private void logGameToLeaderBoard
        (
        MessagePacket messagePacket
        )
    {
        /*
         * print out the stats
         */
        log_msg( "The game has ended. Here are the stats: " );
        log_msg( "The player name is: " + currentPlayerNameString );
        log_msg( "The player score is: " + messagePacket.game_pkt.points );

        /**
         * write out this data to the leaderboard
         * file
         */
        try
            {
            FileWriter fileWriter = new FileWriter( "leaderboard.txt", true );
            BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
            PrintWriter printWriter = new PrintWriter( bufferedWriter );

            printWriter.println( currentPlayerNameString + " " + messagePacket.game_pkt.points );

            printWriter.close();
            }
        catch( IOException e )
            {
            log_err( "Error writing to leaderboard file" );
            }
    }

    /**
     * shipMessage
     * 
     * procedure for sending a message with an
     * image
     */
    private void shipMessage
        (
        MessagePacket   messagePacket,
        BufferedImage   out_img,
        boolean         wait_for_response
        ) 
    {
        /**
         * set the image flag to indicate to the
         * receiver that there is an image msg to
         * parse after this
         */
        messagePacket.img_flag = true;

        /**
         * First send the message through normal tx
         * process
         */
        networkUserAPIs.userNetworkTX( messagePacket );

        networkUserAPIs.userNetworkTX( out_img );

        /**
         * Wait for response
         */
        if( wait_for_response )
            {
            messagePacket = new MessagePacket();
            networkUserAPIs.userNetworkRX( messagePacket );
            }
    }

    /**
     * shipMessage
     * 
     * Desc:    ships a message and returns the result
     *          expects synchronized
     * 
     * Note:    The sender packets game state is used
     *          to synchronize the client and the server
     *          only when the client sendsus back a msg
     *          indicating that it is has the same game
     *          state can we move on.
     * 
     // TODO PMG
     *          As of right now, there is potential for this
     *          to get stucy in a loop continously sending
     *          to client need to fix this if there is time
     */
    private MessagePacket shipMessage
        (
        MessagePacket message
        )
    {
        /**
         * init local variables
         */
        MessagePacket clientMessage = new MessagePacket();

        /**
         * Client game state should be synched with 
         * the server's game state
        */
        GameState clientGameState = GS_UNK;
        /**
         * Send message until the game states are synched
         */
        while( clientGameState != message.gameState )
            {
            /**
             * Attempt to send the message
             *
             */
            networkUserAPIs.userNetworkSendCommandWaitForResponse
                                (
                                message, 
                                clientMessage
                                );
                            
            /**
             * store the client's game state to
             * determine if the states have synched
             */
            clientGameState = message.gameState;

            }

        return clientMessage;
    }


    
    /**
     * sendOptionsMenu
     * 
     * Desc:    Send the menu options to the
     *          user
     * 
     * REQ2, REQ3
     */
    private void sendOptionsMenu
        (
            //void
        )
    {
        /**
        * load the message to the send to the client
        */
        MessagePacket message = new MessagePacket();
        message.gameState     = GS_MENU;             /* tell the client to synch states  */
 
        // TODO PMG clean this up if there is time, for now it works fine so don't mess with it
        message.msg_data      =  "Welcome " + currentPlayerNameString + " Please choose option [\"Play Game\",\"View leaderbord\"]";

        MessagePacket clientMessage = shipMessage( message );

        // TODO PMG figure out a safer way to do this
        /**
         * Parse the response from the client
         * 
         */
        if( clientMessage.msg_data.toLowerCase().contains( "play"  ) )
            {
            gameState = GS_GAME_INIT;
            }
        else if( clientMessage.msg_data.toLowerCase().contains( "leader" ) )
            {
            gameState = GS_LDR_BRD;
            }
        /**
         * Unknown response
         */
        else
            {
            log_err( "client send unknown response to our message." );
            gameState  = GS_UNK;
            }
    }

    /**
     * sendClientInit
     * 
     * REQ 1
     */
    private void sendClientInit
        (
        //void
        )
    {
        /**
         * load the message to the send to the client
         */
        MessagePacket message = new MessagePacket();
        message.gameState     = GS_GET_USR; /* tell the client to init  */
        message.msg_data     =  "Please enter your name";      /* nothing to parse here    */

        MessagePacket clientMessage = shipMessage( message );

        /**
         * Parse the response from the client
         */
        if( clientMessage.msg_data != null )
            {
            currentPlayerNameString = clientMessage.msg_data;
            }
        else
            {
            log_err( "client send unknown response to our message." );
            }
    
        /**
         * Update gamestate to 
         */
        gameState = GS_MENU;
    }

    /**
     * connect
     * 
     * Desc:    Establish a connection via a certain port
     */
    private void connect
        (
        String config,
        int    port
        )
    {

        /**
         * Attempt to connect with a network interface
         * At this point we just pass off the protocol
         * configuration and let the network user
         * module do the rest
         * 
         * Uses the is server flag so that the network
         * API will know to set up a server type socket
         * and not a client type socket
         */
        networkUserAPIs = new NetworkUserAPIs( config, "localhost", port, true );

        /**
         * Unknown configuration
         */
        if(  networkUserAPIs == null )
            {
            assert_msg_exit(false, "Unkown configuration " + config);
            }

        /**
         * Test to see if connection was properly established
         *
         */
        if( networkUserAPIs.connectionIsOpen() == false )
            {
            assert_msg_exit(false, "Failed to connect to a socket");
            }
    }

}
