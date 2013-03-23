package javaquiz;

/** Class for English strings */
public class QuizLanguageEnglish extends QuizLanguage
{
    public String getString(StringID strid)
    {
        switch(strid)
        {
            case DATABASEFILE:
                return "en.qdb";
            case QUIT:
                return "Quit";
            case NEWGAME:
                return "New game";
            case WON:
                return "YOU'VE WON - YOU ARE A MILLIONAIRE!";
            case LOST:
                return "YOU'VE LOST - GAME OVER!";
            case INTRO:
                return "Who Wants To Be A Millionaire?!";
            case MENU:
                return "Game";
            case TOGGLELANGUAGE:
                return "Change to German";
            case CANCEL:
                return "Cancel";
            case QUITDIALOG:
                return "Do you really want to quit?";
            default:
                return "";
        }
    }

    /** Get the language name, e. g. "English" or "German".
     * @return Human readable language name. */
    public String getLanguageName()
    {
        return "English";
    }

    /** Get the language id.
     * @return Language id. */
    public LanguageID getLanguageID()
    {
        return LanguageID.ENGLISH;
    }
}

