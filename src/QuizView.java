package javaquiz;

import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * The QuizView defines the methods that a Viewer of the Quiz has to implement.
 * The implementation (be it swing, console or opengl) is opaque to the controller.
 * The observer pattern is used here with the following functions:
 *  - addViewListener
 *  - removeViewListener
 * So classes that implement QuizView are the observables; and they will call
 * the actionPerformed() method of the observer class (that implements the ActionListener
 * interface).
 */
interface QuizView
{
    public static final int BUTTON_ANSWER_COUNT      = 4;    // how many answer buttons do we have in the game
    public static final int BUTTON_ID_ANSWER_A       = 0;    // button id of answer A
    public static final int BUTTON_ID_ANSWER_B       = 1;    // button id of answer B
    public static final int BUTTON_ID_ANSWER_C       = 2;    // button id of answer C
    public static final int BUTTON_ID_ANSWER_D       = 3;    // button id of answer D
    public static final int BUTTON_ID_JOKER_AUDIENCE = 1000; // button id of the audience joker
    public static final int BUTTON_ID_JOKER_FIFTY    = 1001; // button id of the 50:50 joker

    /** Create a new view window. */
    public void init();

    /** Gets the language.
     * @return Returns the current active language object. */
    public QuizLanguage getLanguage();

    /** Sets the language. All UI elements are updated.
     * @param language The language object. */
    public void setLanguage(QuizLanguage language, QuizModel q);

    /** Add event listener for this view (Observer pattern).
     * Events are used to notify the game logic when the
     * user is interacting with the UI.
     * @param l Action listener (normally the QuizController). */
    public void addViewListener(ActionListener l);

    /** Remove event listener for this view (Observer pattern).
     * @param l Action listener to remove. */
    public void removeViewListener(ActionListener l);

    /**
     * Update the view to display the current game state from the model.
     * @param q The current game state.
     * @param time Time is the absolute time in [ms].
     * @param dt Time since the state has changed in [s] (0.0f if the state has just changed).
     * @param forceRedraw Redraw the screen in any case. Mainly used by setLanguage.
     */
    public void update(QuizModel q, long time, float dt, boolean forceRedraw);
}

