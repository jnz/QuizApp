package javaquiz;

import java.util.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.LinkedBlockingQueue; // for the thread msg queue

// For the sound effects
import java.io.*;
import java.net.URL;
import javax.sound.sampled.*;

/**
 * QuizController is responsible for the main game logic by manipulating the
 * game state in QuizModel.
 *
 * The action listener interface is used for the communication with the user
 * interface (QuizView). Every user action that is needed, will generate an
 * event and we can process it in  the "actionPerformed" method.
 */
public class QuizController extends TimerTask implements ActionListener
{
    /** The complete game data is stored in the QuizModel */
    protected QuizModel m_q = null;
    /** Timer to call the Update() function */
    private Timer m_timer = null;
    /** Absolute time (in ms) of last state change */
    private long m_lastStateChangeTime;
    /** Our current game view */
    protected QuizView m_view = null;
    /** Thread safe message list. This is used to store commands from the Swing UI thread and process them in the Timer loop. */
    private LinkedBlockingQueue<String> m_cmdQueue;
    /** Current frame time. This gets updated for each frame. */
    private long m_frameTime;
    /** Remember the old state. */
    QuizModel.QuizState m_lastState = QuizModel.QuizState.NULL;

    /** Game update rate in [ms] */
    protected final static int UPDATE_RATE = 50;
    /** How many messages from the user interface thread do we store at max. */
    protected final static int MAX_PENDING_UI_CMDS = 30;

    /**
     * The constructor does nothing. To start the game, call the init method.  */
    public QuizController()
    {
        // If we haven't processed those user commands, they will be dropped.
        // It is very unlikely that this will happen; and if it happens, the
        // user will have to click again after the system is responsive again.
        // So this is not too bad.
        m_cmdQueue = new LinkedBlockingQueue<String>(MAX_PENDING_UI_CMDS);
    }

    /**
     * Associate the controller with a data model (QuizModel) and start the game.
     * @param q A Quiz model.
     * @param view The current quiz view.
     * @param langID Language identifier, e. g. LanguageID.ENGLISH. */
    public void init(QuizModel q, QuizView view, QuizLanguage.LanguageID langID)
    {
        // one time initialization stuff
        // e.g. only create the window once etc.

        setView(view); // save our current view
    	view.init(); // init view
    	view.addViewListener(this); // we want user events from the view
        m_q = q;

        // per game initialization.
        restartGame(langID);
    }

    /** Restart the game. Gets called by init(). Call again to restart the game.
     * You need to call init before you can do the first restartGame call.
     * @param langID Language identifier, e. g. LanguageID.ENGLISH.
     */
    public void restartGame(QuizLanguage.LanguageID langID)
    {
        if(m_view == null || m_q == null)
        {
            assert false; // you need to call init at least once.
            return;
        }

        Quiz.Print("Starting Quiz\n");

        m_lastState = QuizModel.QuizState.NULL;
        m_cmdQueue.clear();
        m_q.init(langID);
        changeLanguage(langID); // this will update the view

        // store the current time
        m_lastStateChangeTime = System.currentTimeMillis();
        update(m_lastStateChangeTime); // initial update

        // start the timer
        if(m_timer == null)
        {
            Quiz.Print("Create new timer");
            m_timer = new Timer();
            m_timer.scheduleAtFixedRate(this, UPDATE_RATE, UPDATE_RATE); // start game update timer
        }

        // sound and music
        stopBackgroundMusic(); // no music at the beginning
        playSound("intro.wav"); // play a startup sound
    }

    /**
     * This function is called by the timer to animate the game.
     * After this function is finished, the Viewer redraws
     * the game.
     *
     * @param time Absolute system time in [ms] from System.currentTimeMillis().
     */
    protected void update(long time)
    {
        // If the user has changed the language, we need a complete
        // user interface redraw at the end of this method.
        // So we store the current language here and compare it after
        // the processMessage loop and we see if the language has changed.
        QuizLanguage.LanguageID langID = m_q.getLanguage().getLanguageID();
        boolean hasLanguageChanged = false;
        // Don't call System.currentTimeMillis several times per frame.
        // So we save the current time in m_frameTime.
        m_frameTime = time;

        /* Process the pending user interface commands from the
         * user interface thread. This decouples the user interface
         * thread from the game logic thread, so we have basically a
         * single thread model, which is sufficient for a simple game
         * like this and saves us a lot of headaches. */
        String cmd;
        while((cmd = m_cmdQueue.poll()) != null)
        {
            processMessage(cmd);
        }
        if(langID != m_q.getLanguage().getLanguageID())
            hasLanguageChanged = true; // force redraw at the end of this method.

        // how many seconds are we in this state?
        float dt = (getFrameTime() - m_lastStateChangeTime)*0.001f;

        // check if the state has changed
        QuizModel.QuizState state = m_q.getState();
        boolean stateChanged = (state != m_lastState);
        switch(state)
        {
            case BEGIN:
                if(dt > 4.0f) // display the intro for 4 seconds
                {
                    gotoState(QuizModel.QuizState.ASKING);
                    playBackgroundMusic("background.wav");
                }
                break;
            case ASKING:
                break;
            case RIGHT_ANSWER:
                if(dt > 1.0f) // timeout in seconds
                {
                    if(m_q.hasThePlayerWon())
                    {
                        playSound("won.wav"); // yay sound!
                        gotoState(QuizModel.QuizState.GAMEWON);
                    }
                    else
                    {
                        gotoState(QuizModel.QuizState.ASKING);
                    }
                }
                break;
            case WRONG_ANSWER:
                if(dt > 2.0f) // timeout in seconds
                {
                    gotoState(QuizModel.QuizState.GAMEOVER);
                }
                break;
            case GAMEOVER:
                if(stateChanged)
                {
                    stopBackgroundMusic();
                }
                break;
            case GAMEWON:
                if(stateChanged)
                {
                    // no background music for the final screen
                    stopBackgroundMusic();
                }
                break;
            case JOKER_AUDIENCE:
                if(dt > 1.5f) // timeout in seconds
                {
                    gotoState(QuizModel.QuizState.ASKING);
                }
                break;
            case JOKER_5050:
                if(dt > 1.5f) // timeout in seconds
                {
                    gotoState(QuizModel.QuizState.ASKING);
                }
                break;
            default:
                Quiz.Print("Unknown state");
                assert false;
        }

        m_lastState = state;
        m_view.update(m_q, time, dt, hasLanguageChanged/*forceRedraw*/);
    }

    /** Get the current time of this frame.
     *  Each call per frame gets the exact same time. This would not be the
     *  case for several calls to System.currentTimeMillis() during a frame.
     * @return The absolute time of the current frame. */
    public long getFrameTime()
    {
        return m_frameTime;
    }

    /**
     * Change the game language. Nothing happens if the language
     * is the same.
     * @param langID Identifier of the new language. */
    public void changeLanguage(QuizLanguage.LanguageID langID)
    {
        if(m_q.getLanguage().getLanguageID() != langID)
        {
            Quiz.Print("- The question is: " + m_q.getCurrentQuestion().getQuestion());
            m_q.changeLanguage(langID);
            Quiz.Print("+ The question is: " + m_q.getCurrentQuestion().getQuestion());
        }

        assert m_view != null;
        if(m_view != null)
        {
            // notify the view
            m_view.setLanguage(m_q.getLanguage(), m_q);
        }
    }

    /** Toggle the game language between English and German. */
    public void toggleEnglishGerman()
    {
        if(m_q.getLanguage().getLanguageID() == QuizLanguage.LanguageID.GERMAN)
            changeLanguage(QuizLanguage.LanguageID.ENGLISH);
        else
            changeLanguage(QuizLanguage.LanguageID.GERMAN);
    }

    /** Set a different view.
     * @param view Associate a view (display frame) to the game. */
    public void setView(QuizView view)
    {
        m_view = view;
    }

    /** Change the game state. This will update the view.
     * @param state Quiz state from the QuizModel.QuizState enum. */
    protected void gotoState(QuizModel.QuizState state)
    {
        m_q.setState(state);
        m_lastStateChangeTime = getFrameTime();
    }

    /** Event function: The user clicked on the audience joker */
    protected void onJokerAudience()
    {
        assert false; // FIXME not implemented
        Quiz.Print("Audience Joker");
        if(m_q.getState() != QuizModel.QuizState.ASKING)
        {
            Quiz.Print("Joker only available in asking state");
            return;
        }

        QuizQuestion curQuestion = m_q.getCurrentQuestion();
        gotoState(QuizModel.QuizState.JOKER_AUDIENCE);
    }

    /** Event function: The user clicked on the 50:50 joker */
    protected void onJoker5050()
    {
        Quiz.Print("50:50 Joker");
        if(m_q.getState() != QuizModel.QuizState.ASKING)
        {
            Quiz.Print("Joker only available in asking state");
            return;
        }

        if(m_q.getJokerFiftyRound() >= 0)
        {
            // joker was already used.
            Quiz.Print("Joker already used");
            return;
        }

        Random random = new Random();
        QuizQuestion curQuestion = m_q.getCurrentQuestion();
        // randomly eliminate two questions
        int correct = curQuestion.getCorrectAnswer(); // the correct answer
        int rndNr = random.nextInt(3); // number between 0 and 2 (2 inclusive)
        assert curQuestion.getAnswerCount() == 4; // just to be sure

        // possible elimination combinations.
        // e.g. {0,1} means: eliminate A and B, {1,2} means: eliminate B and C
        int[][] eliminate = { {0,1}, {0,2}, {1,2}, {0,3}, {1,3}, {2,3} };
        // lookup table: which eliminations from "eliminate" are possible for
        // the given correct answer.  e.g. if the correct answer is B, then the
        // following combinations from eliminate can be chosen (i.e.
        // combinations without B(1)):
        // {1,3,5} -> {0,2}, {0,3}, {2,3}
        int[][] lookup = { {2,4,5}, {1,3,5}, {0,3,4}, {0,1,2} };

        m_q.setEliminatedQuestions(eliminate[lookup[correct][rndNr]]);

        gotoState(QuizModel.QuizState.JOKER_5050);
    }

    /** Timer callback function. Do not call this directly.
     *  This method calls the update() method. */
    public void run()
    {
        long time = System.currentTimeMillis();
        update(time);
    }

    /** Actionlistener interface, this will notify us, if something happened
     * in the user interface.
	 * @param e Event with an associated cmd string in getActionCommand(). */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
        // try to put the cmd in the queue.
        // drop the UI command if the queue is full.
        if(m_cmdQueue.offer(cmd) == false)
            Quiz.Print("Too many user interface actions.");
    }

    /** Process a user interface command.
	 * @param cmd Command string from the user interface. */
	private void processMessage(String cmd)
	{
		Quiz.Print("Command from View: " + cmd);

		if(cmd.equalsIgnoreCase("button" + QuizView.BUTTON_ID_ANSWER_A))
        {
            onButton(0);
        }
        else if(cmd.equalsIgnoreCase("button" + QuizView.BUTTON_ID_ANSWER_B))
        {
            onButton(1);
        }
        else if(cmd.equalsIgnoreCase("button" + QuizView.BUTTON_ID_ANSWER_C))
        {
            onButton(2);
        }
        else if(cmd.equalsIgnoreCase("button" + QuizView.BUTTON_ID_ANSWER_D))
        {
            onButton(3);
        }
        else if(cmd.equalsIgnoreCase("restart")) // restart the game
        {
            restartGame(m_q.getLanguage().getLanguageID());
        }
        else if(cmd.equalsIgnoreCase("togglelanguage"))
        {
            toggleEnglishGerman();
        }
        else if(cmd.equalsIgnoreCase("button" +
                    QuizView.BUTTON_ID_JOKER_FIFTY))
        {
            onJoker5050();
        }
        else if(cmd.equalsIgnoreCase("button" +
                    QuizView.BUTTON_ID_JOKER_AUDIENCE))
        {
            onJokerAudience();
        }
        else
        {
            Quiz.Print("Unknown event: " + cmd);
            assert false;
        }
	}

    /** Event function: The user clicked on a button.
     * @param button button index (e.g. from 0 to 3) */
    protected void onButton(int button)
    {
        // make sure that only a valid answer buttons is given
        assert button == QuizView.BUTTON_ID_ANSWER_A ||
               button == QuizView.BUTTON_ID_ANSWER_B ||
               button == QuizView.BUTTON_ID_ANSWER_C ||
               button == QuizView.BUTTON_ID_ANSWER_D;
        if(!( m_q.getState() == QuizModel.QuizState.ASKING ))
        {
            Quiz.Print("Ignore user button click: not in ASKING state.");
            return;
        }

        boolean answer = m_q.answerQuestion(button);
        if(answer == false)
        {
            // buzzer wrong sound
            playSound("lost.wav");
            // change to wrong answer state
            gotoState(QuizModel.QuizState.WRONG_ANSWER);
        }
        else
        {
            // play the "correct" sound
            Random random = new Random();
            int rndNr = random.nextInt(3)+1; // get a random number between 1 and 3
            playSound("correct" + rndNr + ".wav");
            // change state
            gotoState(QuizModel.QuizState.RIGHT_ANSWER);
        }
    }


    /** Play a wave file.
     * @param soundPath Wave file with sound effect.
     */
    public void playSound(String soundPath)
    {
        try
        {
            // Open an audio input stream from the jar file.
            URL soundURL = getClass().getClassLoader().getResource(soundPath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            // Get a sound clip resource.
            Clip clip = AudioSystem.getClip();
            // Open audio clip and load samples from the audio input stream.
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            Quiz.Print("Unsupported audio file: " + soundPath);
        } catch (IOException e) {
            Quiz.Print("Failed to read sound from file: " + soundPath);
        } catch (LineUnavailableException e) {
            Quiz.Print("Mixer error: " + soundPath);
        } catch (Exception e) {
            Quiz.Print("Sound error: " + soundPath);
        }
    }

    /** The background music sound object. */
    private Clip m_background_music = null;

    /** Plays the background music wave file.
     *  This method does not care if something goes wrong while playing a sound.
     * @param soundPath Wave file with music (.wav). */
    public void playBackgroundMusic(String soundPath)
    {
        stopBackgroundMusic();

        try
        {
            // Open an audio input stream from the jar.
            URL soundURL = getClass().getClassLoader().getResource(soundPath);
            //InputStream is = getClass().getClassLoader().getResourceAsStream(soundPath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            // Get a sound clip resource.
            m_background_music = AudioSystem.getClip();
            // Open audio clip and load samples from the audio input stream.
            m_background_music.open(audioIn);
            // loop the background music.
            m_background_music.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (UnsupportedAudioFileException e) {
            Quiz.Print("Unsupported audio file: " + soundPath);
        } catch (IOException e) {
            Quiz.Print("Failed to read sound from file: " + soundPath);
        } catch (LineUnavailableException e) {
            Quiz.Print("Mixer error: " + soundPath);
        } catch (Exception e) {
            Quiz.Print("Sound error: " + soundPath);
        }
    }

    /** Stops the background sound if there is currently one playing. */
    public void stopBackgroundMusic()
    {
        if(m_background_music != null)
        {
            try {
            m_background_music.stop();
            } catch (Exception e) {
                Quiz.Print("Sound error: " + e.getMessage());
            }
            m_background_music = null;
        }
    }

}

