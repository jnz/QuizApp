package javaquiz;

/**
 * A question in the quiz game is represented by this class.
 */
public class QuizQuestion
{
    /**
     * QuizQuestion: question encapsulation class.
     * difficulty: level between 1 and last question.
     * @param id unique id.
     * @param difficulty Difficulty level. Starts at 1.
     * @param question String for the question.
     * @param answer0 First answer.
     * @param answer1 Second answer.
     * @param answer2 Third answer.
     * @param answer3 Fourth answer.
     * @param correctAnswer Index of the correct answer (0..3)
     */
    public QuizQuestion(int id,
                        int difficulty,
                        String question,
                        String answer0,
                        String answer1,
                        String answer2,
                        String answer3,
                        int correctAnswer)
    {
        m_id = id;
        m_difficulty = difficulty;
        m_question = question;
        m_answer = new String[4];
        m_answer[0] = answer0;
        m_answer[1] = answer1;
        m_answer[2] = answer2;
        m_answer[3] = answer3;
        m_correctAnswer = correctAnswer;
        if(m_correctAnswer < 0 || m_correctAnswer > 3 ||
                difficulty < 0 || difficulty > 100)
        {
            Quiz.Print("Question: " + m_question);
            Quiz.Print("Correct answer: " + m_correctAnswer);
            Quiz.Print("Difficulty: " + difficulty);
        }
        assert correctAnswer >= 0 && correctAnswer <= 3;
        assert difficulty > 0;
    }

    /** Get the question id.
     * @return Unique question id. */
    public int getID()
    {
        return m_id;
    }

    /** Get the current difficulty level.
     * @return Difficulty level from 1-max. */
    public int getDifficulty() { return m_difficulty; }
    /** Get the current question string.
     * @return Human readable question string. */
    public String getQuestion() { return m_question; }
    /** Get the index of the correct answer.
     * @return Correct answer index (0..3). */
    public int getCorrectAnswer() { return m_correctAnswer; }

    /** Get the answer, based upon the index
     * @param index Answer index (0..3).
     * @return Human readable answer. Empty string if index is out of bounds.
     */
    public String getAnswer(int index)
    {
        if(index < m_answer.length && index >= 0)
        {
            return m_answer[index];
        }
        else
        {
            assert false;
            return "";
        }
    }

    /** How many answers are available for this question.
     * @return Answer count. */
    public int getAnswerCount()
    {
        return m_answer.length;
    }

    /** Unique question id. */
    private int m_id;
    /** Difficulty level of the question. from 1 - max. question. */
    private int m_difficulty;
    /** Human readable question string. */
    private String m_question;
    /** Answer array. */
    private String[] m_answer = null;
    /** Index of the correct answer. */
    private int m_correctAnswer;
}

