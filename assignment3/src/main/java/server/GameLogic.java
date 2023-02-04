package server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.CipherOutputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.xml.transform.OutputKeys;

import common.GamePacket;
import common.MessagePacket;

import static common.ConsoleLogger.*;
import javax.swing.ImageIcon;

import gui.OutputPanel;

/** used in test driver which is commented out
 * in the "production" code
  */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter.Config;

/**
 * GameLogic
 */
public class GameLogic 
{
    /**
     * MEMORY CONSTANTS
     */
    private final int PTS_UNIT      = 1; 
    private final int PTS_WORD_UNIT = 5;
    private final int PTS_START     = 0;

    /**
    *  DATA TYPES
    */

    /**
     * Player
     * 
     * Desc:    The player data structure contains all
     *          information necessary to keep track of
     *          the player is doing in addition to their
     *          name
     */
    public class Player
        {
            public String  name;
            public int     points;

            /**
             *  Player
             */
            public Player
                (
                  String name
                )
            {
                this.name = name;
                points = PTS_START;
            }
        }

            /**
    *  DATA TYPES
    */

    /**
     * GameType
     * 
     * Desc:    specifies type of game we are playing
     *          i.e., country or a city
     */
    public enum GameType
    {
        GM_TP_COUNTRY,
        GM_TP_CITY,
    }

    /**
     * Game
     * 
     * Desc:    The Game data structure keeps of the
     *          Game components that change during each game
     *          It does not respond to direct actions from the
     *          client but rather it is manipulated by the outer
     *          GameLogic module
     */
    public class Game
    { 

        public HashMap<String, BufferedImage>   game_db;          /* Tables for image and word data */

        public ArrayList<Character>             characters;             /* the characters that have been 
                                                                    guessed so far                  */
        public GameConfig                       config;                 /* the current game configuration */
        public GameType                         type;                   /* what type of game we are playing */
        
        public final String[]                 cities_list = 
                                                {
                                                "berlin",
                                                "rome",
                                                "phoenix",
                                                "paris"
                                                };

        public final String[]                 countries_list = 
                                                {
                                                "germany",
                                                "ireland",
                                                "southafrica" 
                                                };

        /**
         * GameConfig
         */
        class GameConfig
        {
            public String word = null;
            public BufferedImage image = null;
        }

        /** 
         *  Game
         */
        public Game
            (
            GameType type
            )
        {

            /**
             * initialze character array
             */
            characters = new ArrayList<>();
            config = new GameConfig();

            /**
             * init tables
             */
           game_db = new HashMap<>();
           loadTable();

            /**
             * Load the word and the image depending on the
             * configuration
             * 
             */
            if( type == GameType.GM_TP_COUNTRY )
                {
                selectKey( config, countries_list );
                }
            else if ( type == GameType.GM_TP_CITY )
                {
                selectKey( config, cities_list);
                }
            /* Unknown configuration */
            else
                {
                System.out.println( "Unkown game configuration" );
                
                }

        }

        /**
         * loadTables
         * 
         * Desc:    loads the table which contains all possible game
         *          words and their corresponding images
         */
        private void loadTable
            (
            //void
            )
        {
            try
                {
                System.out.println( "DIRECTORY=" + System.getProperty("user.dir") );
                
                game_db.put("germany", ImageIO.read(new File("img/country/germany.jpg")));
                game_db.put("ireland", ImageIO.read(new File("img/country/ireland.jpg")));
                game_db.put("southafrica", ImageIO.read(new File("img/country/southafrica.jpg")));
                game_db.put("berlin",  ImageIO.read(new File("img/city/berlin.jpg")));
                game_db.put("paris",  ImageIO.read(new File("img/city/paris.jpg")));
                game_db.put("phoenix", ImageIO.read(new File("img/city/phoenix.jpg")));
                game_db.put("rome",  ImageIO.read(new File("img/city/rome.jpg")));
                }
            catch( IOException e )
                {
                e.printStackTrace();
                }
        }

        /**
         * selectKey
         * 
         * Desc:    used in generating a random word/image.
         *          at this point the selection of country
         *          or city is completed all we need to do
         *          is slect a key for the value that we
         *          will be using in the table
         * 
         * REQ7:    The game word shall be selected randomly
         */
        private void selectKey
            (
            GameConfig          cfg,
            String[]            list        /* list of keys to choose from */
            ) 
        {
            // generate a random number
            Random rand     = new Random(); 
            int upperbound  = list.length - 1;
            int index       = rand.nextInt(upperbound); 

            cfg.word    = list[ index ];

            cfg.image   = game_db.get(  cfg.word  );
        }

        
    }

    /**
     * VARIABLES
     * Just for simplicity expose the player and game
     */
    public  Player player; 
    public  Game   game;

    /**
     * GameLogic
     */
    public GameLogic
        (
        String playerName
        )
    {
        player = new Player( playerName );
        game   = new Game( GameType.GM_TP_CITY );

        /**
         * Check that make sure proper components loaded 
         * correctly
         */
        if( game.config.image == null )
            {
            System.out.println( "Image was not properly loaded by submodule" );
            }

    }

    /**
     * 
     * getImage
     */
    public BufferedImage getImage
        (
        //void
        )
    {
        return game.config.image;
    }

    /**
     * InitGameLogic
     * main proc for setting up the game
     */
    public static GameLogic InitGameLogic
        (
        String              playerName,
        GamePacket          out_pkt
        )
    {
        GameLogic gameLogic = new GameLogic( playerName );

        /**
         * Return the init message
         */
        out_pkt.points      =  String.valueOf( gameLogic.player.points );
        out_pkt.updt_msg    = "Welcome. The game has started";
        
        /**
         * check to make sure that the game image 
         * was properly initialized
         */
        if( gameLogic.game.config.image == null )
            {
            System.out.println( "Image was not properly loaded by submodule" );
            }
        else
            {
            System.out.println( "Image could not loaded within game module" );
            }

        /**
         * Update the current word
         */
        out_pkt.curr_word  = "";
        for( int i = 0; i < gameLogic.game.config.word.length(); i++ )
            {
            out_pkt.curr_word  = out_pkt.curr_word  + ( "_" );
            }
        
        return gameLogic;
    }

    /**
     * getWord
     * @return
     */
    public String getWord
        (
        //void
        )
    {
        return game.config.word;
    }

    /**
     * getPoints
     * 
     * This should be polled after every move
     * along with with the game return code
     * in order to update the user in real
     * time as to what is going on
     */
    public int getPoints
        (
        //void
        )
    {
        return player.points;
    }

    /**
     * insertMove
     *
     */
    public GameReturnCode insertMove
        (
        MessagePacket  in_pkt
        )
    {
        /**
         * set up local variables
         */
        String          str      = in_pkt.msg_data;
        GameReturnCode  ret_code = GameReturnCode.GM_CODE_INV_MV;

        /**
         * input protection
         */
        if( str.length() == 0 )
            {
            return GameReturnCode.GM_CODE_INV_MV;
            }
    
        /**
         * Determine what kind of move this is
         * i.e., is it a single char or is it an
         * attempt to guess the entire word?
         * 
         * REQ8: The user can send a single character, 
         *       or an entire word when playing the game
         */
        if( str.length() == 1 )
            {
            /**
             * Single char move. first check to see
             * if this move has been played before
             */
            if( game.characters.contains( str.charAt( 0 ) ) )
                {
                /**
                 * return invalid move
                 */
                in_pkt.game_pkt.updt_msg = "You have already played this character";
                ret_code =  GameReturnCode.GM_CODE_INV_MV;
                }

            /**
             * Check the word for the character
             */
            int indices[] = new int[ game.config.word.length() ];
            int count = 0;
            for( int i = 0; i < game.config.word.length(); i++ )
                {
                /*
                 * Check to see if the character is in the word
                 */
                if( game.config.word.charAt( i ) == str.charAt( 0 ) )
                    {
                       /**
                        * update the player's points and also save
                        * save off this index
                        */
                        indices[ count++ ] = i;
                        game.characters.add( game.config.word.charAt( i ) );

                        /**
                         * Update the game packet current word
                         */
                        StringBuilder updtCurrWord = new StringBuilder( in_pkt.game_pkt.curr_word );
                        updtCurrWord.setCharAt( i, str.charAt( 0 ) );

                        in_pkt.game_pkt.curr_word = updtCurrWord.toString();

                        /**
                         * Add as many points as characters 
                         * were found
                         */
                        player.points = player.points + PTS_UNIT; /* REQ10: Letter guess correct then plus one points */

                    }
                }

            /**
             * Check to see if the player got a char right
             */ 
            if( count > 0 )
                {
                in_pkt.game_pkt.updt_msg = "You got a character right";

                ret_code =  GameReturnCode.GM_CODE_CH_WN;
                }
            /**
             * Otherwise they missed this character guess
             */
            else
                {
                /**
                 * deduct the appropriate points
                 */
                player.points = player.points - PTS_UNIT; /* REQ11: Letter guess wrong minus one points */
                
                in_pkt.game_pkt.updt_msg = "You got a character wrong";

                /**
                 * update the game packet
                 * with player points
                 */
                in_pkt.game_pkt.points = String.valueOf( player.points );
                
                ret_code  = GameReturnCode.GM_CODE_CH_LS;

                }
            }
        /**
         * The only case left is a full on word move
         * the only thing we can do is check to see
         * if the player got the total move correct
         */
        else
            {
            if( game.config.word.equals( str ) )
                {
                /*
                 * update message packet update message
                 */
                in_pkt.game_pkt.updt_msg = "You got the word right. Congratulations, you won the game.";
                in_pkt.game_pkt.curr_word = game.config.word;
                player.points = player.points + PTS_WORD_UNIT; /* REQ12: word guess correct plus 5 points */
                    
                ret_code =  GameReturnCode.GM_CODE_PL_WN;
                }
            /**
             * Otherwise deduct the appropriate points
             * from the player
             */
            else
                {
                
                player.points = player.points - PTS_WORD_UNIT ; /* REQ13: word guess incorrect minus 5 points */
                in_pkt.game_pkt.updt_msg = "you got the word wrong";
                 /**
                 * update the game packet
                 * with player points
                 */
                in_pkt.game_pkt.points = String.valueOf( player.points );

                }
            }

        /**
         * update the game packet
         * with player points
         */
        in_pkt.game_pkt.points = String.valueOf( player.points );
    
        /**
         * check to see if the player has lost the game
         * REQ15: As long as the user has more than 0 points
         *        they are still in the game
         */
        if( checkLoss() )
            {
            in_pkt.game_pkt.updt_msg = "Sorry, you lost the game";
            return GameReturnCode.GM_CODE_PL_LS;
            }

        if( game.characters.size() >= game.config.word.length() )
            {
            in_pkt.game_pkt.updt_msg = "Congratulations, you won the game";
            return GameReturnCode.GM_CODE_PL_WN;
            }
       
        /**
         * Otherwise the game keeps going
         */
        return ret_code;
    }

    private boolean checkLoss() 
    {
        if( player.points <= 0 )
            {
            return true;
            }

            return false;
    }

    // MODULE TEST
    // public static void main(String[] args) throws IOException
    // {
    //     GameLogic gameLogic = new GameLogic( "paul" );

    //     System.out.println("oke doke, it has been decided. Please guess "
    //                        +  "the word");
        

    //     /**
    //      * print out the word for testing purposes
    //      */
    //     System.out.println( "The word is " +  gameLogic.game.word ); 
    //     /**
    //     *   Play the game until we get the signal that the game
    //     *   is over
    //     */
    //     GameReturnCode gm_ret_val   = GameReturnCode.GM_CODE_CNT;
    //     while(  ( gm_ret_val != GameReturnCode.GM_CODE_PL_WN )
    //     &&      ( gm_ret_val != GameReturnCode.GM_CODE_PL_LS ) )
    //         {
    //         // Enter data using BufferReader
    //         BufferedReader reader = new BufferedReader(
    //             new InputStreamReader(System.in));
    
    //         // Reading data using readLine
    //         String move = reader.readLine();

    //         gm_ret_val = gameLogic.insertMove(move);

    //         System.out.println( gm_ret_val.toString() );

    //         /**
    //          * Poll the player's points
    //          */
    //         System.out.println( "Player points="  + gameLogic.getPoints() );

    //         }

    //     /**
    //      * Print the results of the game
    //      */
    //     if( gm_ret_val == GameReturnCode.GM_CODE_PL_WN )
    //         {
    //         System.out.println( "Congrats you won!" );
    //         }
    //     else
    //         {
    //         System.out.println(  "Congrats are not in order" );          
    //         }

    // }

}
