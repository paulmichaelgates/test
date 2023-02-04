/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */

package gui;

import java.awt.event.*;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import common.GamePacket;
import common.MessageActionListener;
import common.MessagePacket;
import static common.ConsoleLogger.*;

import java.io.FileReader;
import java.io.IOException;

import java.awt.image.BufferedImage;

public class ClientGui implements OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picturePanel;
	OutputPanel outputPanel;
	boolean gameStarted = false;
	String currentMessage;
	Socket sock;
	OutputStream out;
	ObjectOutputStream os;
	BufferedReader bufferedReader;

	/**
	 * Construct dialog
	 * @throws IOException 
	 */
	public ClientGui
		(
		MessageActionListener messageActionListener
		) 
	{
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);


		// setup the top picture frame
		picturePanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picturePanel, c);

		// setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel( messageActionListener );
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picturePanel.newGame(1);
		//insertImage("img/hi.png", 0, 0);

	}

	/**
	 * Shows the current state in the GUI
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);

		frame.setVisible(true);

	}

	public void showMenu
		(
		String[] options,
		MessageActionListener messageActionListener
		)
	{
		/**
		 * Local variable 
		 */
		String selectedOptionString = null;
		JFrame frame = new JFrame( );

		frame.setLayout( new BoxLayout( frame.getContentPane(), BoxLayout.LINE_AXIS ) );

		JLabel label = new JLabel();
		
		label.setText("Menu");

		// add the label and text field to the frame
		frame.add(label);

		// specify the size of the frame
		frame.setSize(300, 100);

		// speciy the size of the label
		label.setSize(100, 30);
		
		/**
		 * Dynamically add the buttons to the menu
		 */
		for( int i = 0; i < options.length; i++ )
			{
			JButton button = new JButton();
			button.setText( options[ i ] );
			frame.add( button );
			button.addActionListener
				(
					new ActionListener()
						{
							@Override
							/**
							 * actionPerformned
							 * 
							 * Override the message data object and
							 * get the name of the data
							 */
							public void actionPerformed(ActionEvent e)
							{
								/**
								 * The name of the button is the name of the
								 * option simple as that
								 */
								messageActionListener.messagePacket.msg_data =
											( (JButton) e.getSource() ).getText(); 
														
	
								frame.setVisible( false );
	
								/**
								 * Call the Message Specific action
								 */
								messageActionListener.actionPerformed( e );
							}	
						}
				);
			}




		frame.pack();

		frame.setVisible(true);
	}

	/**
	 * showNamePrompt
	 */
	public void showNamePrompt
		(
		String 		  			messageString,
		MessageActionListener   messageActionListener
		)
	{
		// show popup
		JFrame frame = new JFrame( );

		frame.setLayout( new BoxLayout( frame.getContentPane(), BoxLayout.LINE_AXIS ) );

		JLabel label = new JLabel( messageString );
		JTextField textField = new JTextField();
		
		// specify the size of the text field
		textField.setColumns( 10 );

		// add the label and text field to the frame
		frame.add(label);

		// specify the size of the frame
		frame.setSize(300, 100);

		// speciy the size of the label
		label.setSize(100, 30);
		
		JButton okayButton = new JButton();
		okayButton.setText("OK");

		okayButton.addActionListener
			(
				new ActionListener()
					{
						@Override
						/**
						 * actionPerformned
						 * 
						 * Override the message data object and
						 * get the name of the data
						 */
						public void actionPerformed(ActionEvent e)
						{
							/**
							 * Set the message data for the message
							 * action listener
							 */
							messageActionListener.messagePacket.msg_data 
													= textField.getText(); 

							frame.setVisible(false);

							/**
							 * Call the Message Specific action
							 */
							messageActionListener.actionPerformed( e );
						}	
					}
			);

		frame.add(okayButton);

		frame.add(textField);

		frame.pack();

		frame.setVisible(true);

	}


	/**
	 * Insert an image into the grid at position (col, row)
	 * 
	 * @param filename - filename relative to the root directory
	 * @param row - the row to insert into
	 * @param col - the column to insert into
	 * @return true if successful, false if an invalid coordinate was provided
	 * @throws IOException An error occured with your image file
	 */
	public boolean insertImage( BufferedImage image ) throws IOException {
		System.out.println("Image insert");
		try {
			// insert the image
			if (picturePanel.insertImage(image, 0, 0)) {
				// put status in output
				return true;
			}
		} 
		catch(PicturePanel.InvalidCoordinateException e) 
			{
				//TODO handle this exception
			}

		//TODO output error

		return false;
	}

	/**
	 * Submit button handling
	 * 
	 * TODO: This is where your logic will go or where you will call appropriate methods you write. 
	 * Right now this method opens and closes the connection after every interaction, if you want to keep that or not is up to you. 
	 */
	@Override
	public void submitClicked() {
		
		
	}

	/**
	 * Loads the GUI with the given start up packet
	 * @param game_pkt
	 */
	public void startGamePage
		(
		GamePacket 		game_pkt,
		BufferedImage 	image
		)
	{
		assert_msg_cont( ( game_pkt != null ), "Game packet is null" );
		
		outputPanel.appendOutput( game_pkt.updt_msg );

		try
			{
			insertImage( image );
			}
		catch( IOException e )
			{
			e.printStackTrace();
			}

		this.show( true );
	}

	/**
	 * updateCurrentWord
	 */
	public void updateCurrentWord
		(
		String word
		)
	{
		this.outputPanel.setBlanks( word );
	}

	/**
	 * updatePoints
	 * Desc:	Update the points in the output panel
	 */
	public void updatePoints
		(
		int points
		)
	{
		this.outputPanel.setPoints( points );
	}

	public void showLeaderBoard
		(
		String leaderBoardString,
		MessageActionListener messageActionListener
		)
	{
		/**
		 * create a new frame with a button to close the frame
		 * and a label that is the dimensions of the entire
		 * 300 x 300 frame
		 *
		 */
		JFrame frame = new JFrame( );
		frame.setLayout( new BoxLayout( frame.getContentPane(), BoxLayout.LINE_AXIS ) );

		/*
		 * set the frame size
		 */
		frame.setSize(300, 300);

		JLabel label = new JLabel( leaderBoardString );
		/**
		 * set the label size to the size of the frame
		 *
		 */
		label.setSize(300, 300);
		JButton okayButton = new JButton();
		okayButton.setText("OK");

		okayButton.addActionListener
			(
				new ActionListener()
					{
						@Override
						/**
						 * actionPerformned
						 * 
						 * Override the message data object and
						 * get the name of the data
						 */
						public void actionPerformed(ActionEvent e)
						{
							/**
							 * Set the message data for the message
							 * action listener
							 */
							messageActionListener.messagePacket.msg_data 
													= "DONE";
							messageActionListener.actionPerformed( e );
							frame.setVisible(false);
						}	
					}
			);
		
		frame.add(okayButton);
		frame.add(label);
					
		frame.pack();
		frame.setVisible(true);

	}

	/**
	 * endGamePage
	 */
	public void endGamePage
		(
		//void
		)
	{

		/**
		 * output to the output panel
		 */
		try
			{
			this.outputPanel.appendOutput( "Game Over...." );
			this.outputPanel.appendOutput( "Please wait while scores are uploaded..." );
;
			Thread.sleep( 3000 );
			this.outputPanel.appendOutput( "OK Closing window" );


			}
		catch( InterruptedException e )
			{
			e.printStackTrace();
			}


		this.frame.setVisible( false );

	}

	/**
	 * Directly send a message to the output panel
	 */
	public void sendOutput
		(
		String msg
		)
	{
		outputPanel.appendOutput(msg);
	}
	/**
	 * Key listener for the input text box
	 * 
	 * Change the behavior to whatever you need
	 */
	@Override
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

}
