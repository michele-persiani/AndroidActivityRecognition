package umu.software.activityrecognition.speech.translate;

import java.util.Locale;

/**
 * Dummy translator translating to the same language
 */
public class SameLanguageTranslator implements ITranslator
{
    private final Locale language;

    public SameLanguageTranslator(Locale language)
    {
        this.language = language;
    }

    @Override
    public Locale getSourceLanguage()
    {
        return language;
    }

    @Override
    public Locale getTargetLanguage()
    {
        return language;
    }

    @Override
    public String translate(String s)
    {
        return s;
    }
}
