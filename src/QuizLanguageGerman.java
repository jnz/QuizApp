package javaquiz;

/** Class for German strings */
public class QuizLanguageGerman extends QuizLanguage
{
    public String getString(StringID strid)
    {
        switch(strid)
        {
            case DATABASEFILE:
                return "de.qdb";
            case QUIT:
                return "Beenden";
            case NEWGAME:
                return "Neues Spiel";
            case WON:
                return "Sie haben GEWONNEN!";
            case LOST:
                return "Sie haben leider verloren!";
            case INTRO:
                return "Wer wird Million\u00e4r?";
            case MENU:
                return "Spiel";
            case TOGGLELANGUAGE:
                return "Wechsle zu Englisch";
            case CANCEL:
                return "Abbrechen";
            case QUITDIALOG:
                return "M\u00f6chten Sie wirklich abbrechen?";
            default:
                assert false;
                return "";
        }
    }

    /** Get the language name, e. g. "English" or "German".
     * @return Human readable language name. */
    public String getLanguageName()
    {
        return "German";
    }

    /** Get the language id.
     * @return Language id. */
    public LanguageID getLanguageID()
    {
        return LanguageID.GERMAN;
    }
}

/*
 * Unicode table for German umlaute.
 *
 *  Char 	|   Unicode
 *  --------+---------------------
 *  Ä, ä 	|   \u00c4, \u00e4
 *  Ö, ö 	|   \u00d6, \u00f6
 *  Ü, ü 	|   \u00dc, \u00fc
 *  ß 		|   \u00df
 */
