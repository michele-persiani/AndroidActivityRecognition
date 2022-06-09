package umu.software.activityrecognition.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.function.Consumer;


/**
 * EditText that allows to register a single TextWatcher at a time through setTextChangedListener()
 */
@SuppressLint("AppCompatCustomView")
public class PreferencesEditText extends EditText
{
    private TextWatcher mCallback;


    public PreferencesEditText(Context context)
    {
        super(context);
    }


    public PreferencesEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }


    public PreferencesEditText(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }


    public PreferencesEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Add a listener to the text changes. Unregisters the listener previously registered using this method
     * @param listener the listener that will be notified each time there is a chenge of text
     */
    public void setTextChangedListener(Consumer<String> listener)
    {
        if (mCallback != null)
            removeTextChangedListener(mCallback);
        mCallback = new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                String text = String.format("%s", editable.toString());
                if (listener != null)
                    listener.accept(text);
            }
        };
        addTextChangedListener(mCallback);
    }

}
