package javaquiz;

/**
 * The complete game state is represented by the QuizModel class.
 * E. g. serializing the content of this class to a file would
 * be the base for a file save/restore model.
 * The view part needs access to the game model (QuizModel) to
 * display the current game.
 */
public class QuizModel
{
    /** This enum identifies the current game state.
     *  The current state is important for the game logic and
     *  the display.
     */
    public enum QuizState
    {
        /** unknown state */
        NULL,
        /** intro */
        BEGIN,
        /** the player is currently answering a question */
        ASKING,
        /** the player has selected the right answer */
        RIGHT_ANSWER,
        /** the player has selected a wrong answer */
        WRONG_ANSWER,
        /** the player has lost */
        GAMEOVER,
        /** the player is now a millionaire */
        GAMEWON,
        /** the joker is active */
        JOKER_AUDIENCE,
        /** the joker is active */
        JOKER_5050
    }

    /** The current game state (ASKING, GAMEOVER, GAMEWON, etc.) */
    private QuizState m_state = QuizState.BEGIN;

    /** The questions for the user are stored here.
     *  The m_level property indicates the current question. */
    private QuizQuestion[] m_questions = null;
    /** Current question level (from 1 to max. question) */
    private int m_level = 0;
    /** Set to true, if the player has won the game, i. e. answered all questions. */
    private boolean m_won = false;
    /** Set to true, if the player has lost the game. */
    private boolean m_lost = true;
    /** 50:50 Joker eliminated questions. Contains the index of the eliminated questions. */
    private int[] m_eliminatedQuestions = new int[0];
    /** In which round was the 50:50 joker activated.
     *  The value is -1 if the joker is unused so far. */
    private int m_jokerFiftyRound;
    /** The current language. */
    private QuizLanguage m_language = null;

    /** Default constructor. To prepare the model, you have to call the init method.*/
    public QuizModel()
    {
    }

    /** Start a new game.
     *  @param langId Language identifier, e. g. LanguageID.ENGLISH.
     */
    public void init(QuizLanguage.LanguageID langId)
    {
        m_won = false;
        m_lost = false;
        m_level = 1; // we start at the first level
        m_state = QuizState.BEGIN;
        m_questions = null;
        m_eliminatedQuestions = new int[0]; // initialize with empty array
        m_jokerFiftyRound = -1;
        changeLanguage(langId); // this will also load the questions
    }

    /** Change the game language. This will load the quiz database for the language.
     *  @param langID Language identifier, e. g. LanguageID.ENGLISH */
    public void changeLanguage(QuizLanguage.LanguageID langID)
    {
        m_language = QuizLanguage.getLanguage(langID);

        // should we just reload the same questions in another language?
        // yes, if we already have some questions loaded.
        boolean loadSameQuestions = (m_questions != null) && (m_questions.length > 0);
        Quiz.Print("Load same questions? ");
        if(loadSameQuestions)
            Quiz.Print("yes");
        else
            Quiz.Print("no");
        m_questions = loadFromDB(
                getLanguage().getString(QuizLanguage.StringID.DATABASEFILE),
                loadSameQuestions);

        for(int i=0;i<m_questions.length;i++)
            Quiz.Print("Q " + i + " " + m_questions[i].getQuestion());
    }

    /** Get the current game language.
     * @return Current language. */
    public QuizLanguage getLanguage()
    {
        return m_language;
    }

    /** Set the current gamestate.
     * @param state The new game state (ASKING, GAMEOVER, GAMEWON...)
     */
    public void setState(QuizState state)
    {
        Quiz.Print("State change: {" + m_state + "} -> {" + state + "}");
        m_state = state;
    }

    /** Get the current game state.
     * @return The current game state. (ASKING, GAMEOVER, GAMEWON...)
     */
    public QuizState getState()
    {
        return m_state;
    }

    /** Is the game over (either won or lost)
     * @return false if game is still running, true if the game is over.
     */
    public boolean isGameOver()
    {
        return ( m_lost || m_won );
    }

    /** Has the player selected a wrong answer?
     * @return true if the player has lost.
     */
    public boolean hasThePlayerLost()
    {
        return m_lost;
    }

    /** Has the player selected a right answer?
     * @return true if the player has won, i.e. answered the last question correctly.
     */
    public boolean hasThePlayerWon()
    {
        return m_won;
    }

    /** Answer the current question.
     * If the answer was right, the level is incremented.
     * If the answer was wrong, the model stays in the game over state.
     * @param answer Answer index from user (0, 1, 2 or 3).
     * @return true, if the answer was right, false otherwise.
     */
    public boolean answerQuestion(int answer)
    {
        if(m_lost || m_won) // nothing to do anymore
            return false;

        QuizQuestion q = getCurrentQuestion();

        if(q.getCorrectAnswer() != answer)
        {
            m_lost = true; // game over
            return false; // wrong answer
        }

        m_level++; // right answer, so let's move up one question

        // check if the player has solved the last question
        if(m_level >= m_scoretable.length)
        {
            m_won = true;
            m_level = m_scoretable.length-1;
        }

        return true;
    }

    /** Get the current question.
     * @return Current question object.
     * */
    public QuizQuestion getCurrentQuestion()
    {
        assert m_questions != null;
        if(m_questions == null)
        {
            Quiz.Print("No questions loaded.");
            return null;
        }
        assert m_level > 0;
        if(m_level == 0)
        {
            Quiz.Print("Not in game.");
            return null;
        }

        assert m_level <= m_questions.length;
        if(m_level <= m_questions.length)
            return m_questions[m_level-1];
        else
            return null;
    }

    /** Get the current score.
     * @return Current score in euros.
     */
    public int getScore()
    {
        return m_scoretable[m_level];
    }

    /** Which question has the player reached.
     * @return The current level. (1 - max. level).
     */
    public int getLevel()
    {
        return m_level;
    }

    /** Returns the reached fallback level.
     *  E.g . the player is currently
     *  at 125000 euros and answers with a wrong answer,
     *  he falls back to 32000 euros.
     *  This method gives you the fallback level.
     *  @return Fallback level. The return value is a level index, not an amount in euros.
     *  */
    public int getFallbackLevel()
    {
        return m_fallback[m_level];
    }

    /** Gets questions that are eliminated by the 50:50 joker.
     *  Returns a zero length vector if there was no 50:50 joker so far.
     *  @return Array with the indices of eliminated questions. Empty array if no questions have been eliminated. */
    public int[] getEliminatedQuestions()
    {
        // this returns a copy of the internal state.
        return (int[])m_eliminatedQuestions.clone();
    }

    /** In which round/level was the 50:50 joker activated?
     * @return -1 if not activated so far, the respective level otherwise. */
    public int getJokerFiftyRound()
    {
        return m_jokerFiftyRound;
    }

    /** Marks some questions as eliminated from the 50:50 joker.
     * This method will store a copy of eliminatedQuestions.
     * @param eliminatedQuestions An array with indices of the eliminated questions. */
    public void setEliminatedQuestions(int[] eliminatedQuestions)
    {
        assert eliminatedQuestions.length == 2;
        for(int i=0;i<eliminatedQuestions.length;i++)
        {
            QuizQuestion q = getCurrentQuestion();
            if(q.getCorrectAnswer() == eliminatedQuestions[i])
            {
                Quiz.Print("Error! Correct question eliminated");
                assert false;
            }
            Quiz.Print("Eliminated question: " + eliminatedQuestions[i]);
        }

        assert m_eliminatedQuestions.length == 0; // normally this should be true
        assert m_jokerFiftyRound == -1; // no joker has been used so far
        m_eliminatedQuestions = (int[])eliminatedQuestions.clone();
        m_jokerFiftyRound = getLevel(); // remember the round in which we activated the joker
    }

    /** Load the questions from a question database file.
     *  If this model has already loaded some questions, e.g. in English and
     *  you call loadFromDB("de.qdb", true), then the same questions are
     *  loaded from the German quiz database.
     *  loadSameQuestions true makes no sense if no questions are loaded in the model.
     *
     * @param filePath File system path to the database file.
     * @param loadSameQuestions Load the same questions (requires that we already have questions loaded).
     * @return An array with question objects.
     */
    protected QuizQuestion[] loadFromDB(String filePath, boolean loadSameQuestions)
    {
        // This is a set of test questions
        // -------------------------------
        //QuizQuestion q1 = new QuizQuestion(1, 1, "Darth Vader is Luke's...", "Best friend", "Car mechanic", "Room mate", "Father", 3);
        //QuizQuestion q2 = new QuizQuestion(2, 2, "Who is Jimmy Wales?", "A marine biologist", "A former US president", "The Wikipedia founder", "Aberdaugleddau", 2);
        //QuizQuestion q3 = new QuizQuestion(3, 3, "Who won the FIFA worldcup in 1990?", "Germany", "USA", "France", "San Marino (as if)", 0);
        //QuizQuestion q4 = new QuizQuestion(4, 4, "Who has written the game Minecraft?", "Nitch", "Natch", "Notch", "Netch", 2);
        //QuizQuestion q5 = new QuizQuestion(5, 5, "What is the name of Bogart's establishment in Casablanca?", "Bates Motel", "Rick's Cafe", "Ratskeller", "Hard Rock", 1);
        //QuizQuestion q6 = new QuizQuestion(6, 6, "exp(i*pi) = ?", "2.71...", "0", "3.14159...", "-1", 3);
        //QuizQuestion q7 = new QuizQuestion(7, 7, "1+1 = ?", "2", "11", "1", "1.999998", 0);
        //QuizQuestion q8 = new QuizQuestion(8, 8, "This game is...", "Awesome", "Stupid", "Annoying", "Useless", 0);
        //QuizQuestion[] qarray = {q1, q2, q3, q4, q5, q6, q7, q8};

        QuizDB db = new QuizDB();
        QuizQuestion[] qarray;
        int questions = getScoretable().length-1;

        // this
        if(loadSameQuestions)
        {
            int[] ids = new int[questions];

            for(int i=0;i<questions;i++)
                ids[i] = m_questions[i].getID();

            qarray = db.getSpecificQuestions(filePath, ids);
        }
        else
        {
            qarray = db.getRandomQuestions(filePath);
        }

        if(qarray.length < questions)
        {
            Quiz.Print("Failed to load questions from file.");
        }

        return qarray;
    }

    /** Fallback level lookup table. E. g. the player is at the 25000 question and fails,
        he goes back to 5000.  */
    private static final int[] m_fallback = {0, 0, 1, 1, 1, 4, 4, 4, 4};
    /** Score lookup table for each level */
    private static final int[] m_scoretable = {       0,
                                                  /*x*/ 5000,
                                                        10000,
                                                        25000,
                                                  /*x*/ 50000,
                                                        100000,
                                                        250000,
                                                        500000,
                                                        1000000 };
    /** Returns the score lookup table (static method).
     * @return Score lookup table int array. */
    public static int[] getScoretable()
    {
        return (int[])m_scoretable.clone();
    }
    /** Returns the fallback table
     * (see getFallbackLevel function for an in-depth explanation).
     * @return Fallback table int array.
     */
    public static int[] getFallbacktable()
    {
        return (int[])m_fallback.clone();
    }
}

