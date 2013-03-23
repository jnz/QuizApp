package javaquiz;

/**
 * Main class for the static void main function.
 * The three essential classes are created here:
 *   <ul>
 *   <li>QuizModel (game state).</li>
 *   <li>QuizView (display the game state on the screen).</li>
 *   <li>QuizController (game logic).</li>
 *   </ul>
 */
public class Quiz
{
    /** Log function for debug messages. This function includes the caller
     *  thread name at the beginning of the line.
     *  Instead of calling System.out.println() this function should be used. */
    public static void Print(String s)
    {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] " + s);
    }

    public static void main(String[] args)
    {
        javaquiz.QuizModel q = new javaquiz.QuizModel();
        javaquiz.QuizController ctrl = new javaquiz.QuizController();
        javaquiz.QuizViewSwing view = new javaquiz.QuizViewSwing();

        ctrl.init(q, view, javaquiz.QuizLanguage.LanguageID.ENGLISH); // start the game
        Print("Startup complete. Handing over to the game timer update thread.");
    }
}

