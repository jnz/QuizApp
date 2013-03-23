package javaquiz;

import java.util.*;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** This class loads quiz questions from a CSV-like UTF-8 encoded file.
  * The question database is a simple CSV file with one question per line.
  * Each question attribute is separated by a ";" (see the QuizDB.SPLIT
  * constant).
  *
  * Example:
  *
  *      5; How is a play on words commonly described?; Pan; Pin; Pen; Pun; 3
  *
  * The first item is the difficulty level (5 here).
  * The second item is the question string.
  * This is followed by four answers (Pan, Pin, Pen, Pun)
  * And the last item is the correct answer index here.
  */
public class QuizDB
{
    /** Use this character to split the strings in our database. */
    private final static String SPLIT = ";";
    /** There are 7 fields per line in the quiz db. */
    private final static int FIELDS = 7;
    /** How many questions do we expect in the database (roughly, to reserve memory). */
    private final static int EXPECTED_QUESTIONS = 200;

    /** Constructor. */
    public QuizDB()
    {

    }

    /** Loads a random set of questions from the db.
     * One question per difficulty level.
     * @param fileName Quiz database file name (UTF-8 encoded file in jar, NOT in the filesystem).
     * @return Array of questions. Array is empty if something fails.
     */
    public QuizQuestion[] getRandomQuestions(String fileName)
    {
        QuizQuestion[] allQuestions = loadAllQuestions(fileName);
        int maxQuestions = QuizModel.getScoretable().length;
        Vector<QuizQuestion> questions = new Vector<QuizQuestion>(maxQuestions);
        Random random = new Random();

        // for each difficulty level we fill the category vector
        // with question from this level and then select randomly
        // one question from this level. this question gets added
        // to the final question array.

        // foreach difficulty level (1 = min. difficulty level)
        for(int i=1;i<=maxQuestions;i++)
        {
            // We expect roughly EXPECTED_QUESTIONS/5 per level.
            Vector<QuizQuestion> questionsInThisLevel =
                new Vector<QuizQuestion>(EXPECTED_QUESTIONS/5);
            for(int j=0;j<allQuestions.length;j++) // foreach question
            {
                // does this question match our current difficulty level?
                if(allQuestions[j].getDifficulty() == i)
                {
                    // cache it here
                    questionsInThisLevel.add(allQuestions[j]);
                }
            }

            // there is no question available for this difficulty level.
            // this is bad. we need at least one question per level.
            int questionsFound = questionsInThisLevel.size();
            if(questionsFound == 0)
            {
                continue;
            }

            // select random question from questionsInThisLevel
            int rndNr = random.nextInt(questionsFound);
            // push this question to our final return array
            questions.add(questionsInThisLevel.elementAt(rndNr));
        }

        // we give back an array instead of the internal vector
        return (QuizQuestion[])questions.toArray(new QuizQuestion[questions.size()]);
    }

    /** Loads a specific set of questions from the db.
     * This method is here because the user might change the language,
     * so we reload the same questions from another database in another language.
     * @param fileName Quiz database file name (UTF-8 encoded file in jar, NOT in the filesystem).
     * @param ids Array of question ids that we should load.
     * @return Array of questions. Empty, if there is a problem.
     */
    public QuizQuestion[] getSpecificQuestions(String fileName, int[] ids)
    {
        QuizQuestion[] allQuestions = loadAllQuestions(fileName);
        int maxQuestions = QuizModel.getScoretable().length;
        Vector<QuizQuestion> questions = new Vector<QuizQuestion>(maxQuestions);
        QuizQuestion[] qarray; // our final resulting question array

        Arrays.sort(ids); // sort the array, so we can use the binary search method

        for(int i=0;i<allQuestions.length;i++) // foreach question
        {
            // we check, if the user wants this question
            // Something in the order of: n*log(n)
            // thanks to the binary search.
            int index = Arrays.binarySearch(ids, allQuestions[i].getID());
            if(index >= 0) // the question id is in the ids[] array
            {
                // so add this to our final question array
                questions.add(allQuestions[i]);
            }
        }

        // Now we have the questions and we can sort them
        // in the order of the difficulty level
        qarray = (QuizQuestion[])questions.toArray(
                new QuizQuestion[questions.size()]);
        Arrays.sort(qarray, new QuestionComparator());
        return qarray;
    }

    /** Helper class to sort questions according to difficulty level. */
    private static class QuestionComparator implements Comparator<QuizQuestion>
    {
        public int compare(QuizQuestion a, QuizQuestion b)
        {
            return a.getDifficulty() - b.getDifficulty();
        }
    }

    /** Loads all questions from the db.
     * @param fileName Quiz database file name (UTF-8 encoded file in jar, NOT in the filesystem).
     * @return Array of questions. Returns an empty array, if there is a problem. */
    private QuizQuestion[] loadAllQuestions(String fileName)
    {
        // the quizmodel expects only as much questions as there are
        // entries in the scoretable.
        // which equals to our max. difficulty level.
        int maxDifficulty = QuizModel.getScoretable().length;
        Vector<QuizQuestion> allQuestions =
            new Vector<QuizQuestion>(EXPECTED_QUESTIONS);

        Quiz.Print("Loading from quiz database: " + fileName);

        InputStream stream = null;

        try
        {
            int lineNr = 0; // we use this as our question index
            String line;
            stream = getClass().getClassLoader().getResourceAsStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            // load all questions, line by line
            while((line = br.readLine()) != null)
            {
                lineNr++; // so 1 is our first index

                line = line.trim(); // remove leading and trailing whitespaces
                String[] split = line.split(SPLIT);
                if(split.length != FIELDS) // check the attribute count
                {
                    Quiz.Print("Invalid line in quiz database: " + line);
                    continue;
                }

                int id = lineNr; // we just use the line number as unique id.

                int difficulty = Integer.parseInt(split[0].trim());
                if(difficulty > maxDifficulty || difficulty < 1)
                    continue;

                String question = split[1].trim();
                String answer0  = split[2].trim();
                String answer1  = split[3].trim();
                String answer2  = split[4].trim();
                String answer3  = split[5].trim();
                int correctAnswer = Integer.parseInt(split[6].trim());
                if(question.length() == 0 ||
                    answer0.length() == 0 ||
                    answer1.length() == 0 ||
                    answer2.length() == 0 ||
                    answer3.length() == 0 )
                {
                    continue;
                }

                QuizQuestion q = new QuizQuestion(id,
                                                  difficulty,
                                                  question,
                                                  answer0,
                                                  answer1,
                                                  answer2,
                                                  answer3,
                                                  correctAnswer);
                allQuestions.add(q);
            }
        }
        catch(Exception e)
        {
            Quiz.Print(e.getLocalizedMessage());
            e.printStackTrace();
            Quiz.Print("QuizDB: Failed to read from " + fileName);
        }
        finally
        {
            try{
                if(stream != null)
                    stream.close();
            } catch(IOException ioe) {
                Quiz.Print("Failed to close stream.");
            }
        }

        // return as an array. this makes the interface more intuitive.
        return (QuizQuestion[]) allQuestions.toArray(
                new QuizQuestion[allQuestions.size()]);
    }
}

