package javaquiz;

/**
 * This abstract base class provides i18n strings for the quiz game.
 * Use the getLanguage method to get a language subclass.
 */
public abstract class QuizLanguage
{
    /** i18n string ids */
    public enum StringID
    {
        /** This is a special id: this is the filename for the question db. */
        DATABASEFILE,
        /** Menu item quit and button text for quitting the application. */
        QUIT,
        /** Menu item new game. */
        NEWGAME,
        /** Text for the winning screen. */
        WON,
        /** Text for the game over screen. */
        LOST,
        /** Intro text. */
        INTRO,
        /** Main menu text. */
        MENU,
        /** Text for the Cancel button. */
        CANCEL,
        /** Asking the user if he wants to quit. */
        QUITDIALOG,
        /** Change language menu item. */
        TOGGLELANGUAGE
    }

    /** This enum identifies the languages for the i18n.  */
    public enum LanguageID
    {
        ENGLISH,
        GERMAN
    }

    /** Create a language object for the desired language.
     * English is the default language.
     * @param language Language id.
     * @return Language object where you can call the getString method. */
    public static QuizLanguage getLanguage(LanguageID language)
    {
        QuizLanguage lang;

        switch(language)
        {
            case GERMAN:
                lang = new QuizLanguageGerman();
                break;
            default: // English is the default language
            case ENGLISH:
                lang = new QuizLanguageEnglish();
                break;
        }
        return lang;
    }

    /** Get the language name, e. g. "English" or "German".
     * @return Human readable language name. */
    public abstract String getLanguageName();

    /** Get the language id.
     * @return Language id. */
    public abstract LanguageID getLanguageID();

    /** Provides the strings associated with the string ID.
     * @param strid String identifier from StringID enum.
     * @return Associated translated string. */
    public abstract String getString(StringID strid);
}

