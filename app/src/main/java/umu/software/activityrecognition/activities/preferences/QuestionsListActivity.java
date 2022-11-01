package umu.software.activityrecognition.activities.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.shared.ListViewActivity;
import umu.software.activityrecognition.activities.shared.MultiItemListViewActivity;
import umu.software.activityrecognition.preferences.ClassifyRecordingsPreferences;
import umu.software.activityrecognition.services.speech.SpeechService;
import umu.software.activityrecognition.shared.lifecycles.BinderLifecycle;


public class QuestionsListActivity extends MultiItemListViewActivity
{
    private ClassifyRecordingsPreferences mPreferences;
    private BinderLifecycle<SpeechService.SpeechBinder> mBinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        mPreferences = new ClassifyRecordingsPreferences(this);

        mBinder = new BinderLifecycle<>(this, SpeechService.class, intent -> {
            intent.putExtra(
                    SpeechService.EXTRA_LANGUAGE,
                    Locale.forLanguageTag(mPreferences.questionsLanguage().get()
                    )
            );
        });

        getLifecycle().addObserver(mBinder);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getMasterLayout()
    {
        return R.layout.holder_question_item;
    }

    @Override
    protected void registerBinders()
    {
        registerDefaultBinding(
                R.id.linearLayout_show,
                (holder, position) -> {
                    bindShowQuestionElement(holder, position-1);
                });

        registerBinding(
                0,
                R.id.linearLayout_header,
                (holder, position) -> {}
        );
        registerBinding(
                getItemCount() - 1,
                R.id.linearLayout_add,
                (holder, position) -> {
                    bindAddNewQuestionElement(holder);
                });
    }

    //@Override
    //protected View createListEntryView()
    //{
    //    View view =  getLayoutInflater().inflate(R.layout.holder_question_item, null, false);
    //    ((EditText)view.findViewById(R.id.editText_new_question)).setImeOptions(EditorInfo.IME_ACTION_DONE);
    //    return view;
    //}

    @Override
    protected int getItemCount()
    {
        return mPreferences.questions().get().size() + 2;
    }


    protected void bindAddNewQuestionElement(@NonNull ListViewActivity.ViewHolder holder)
    {

        ImageButton addButton = holder.getView().findViewById(R.id.button_add_question);
        EditText editTextQuestion = holder.getView().findViewById(R.id.editText_new_question);
        addButton.setOnClickListener(btn -> {
            List<String> updatedQuestions = Lists.newArrayList(mPreferences.questions().get());
            String newQuestion = editTextQuestion.getText().toString();
            if (newQuestion.length() > 0)
            {
                updatedQuestions.add(newQuestion);
                mPreferences.questions().set(updatedQuestions);
                refreshListView();
            }
        });
    }


    protected void bindShowQuestionElement(@NonNull ListViewActivity.ViewHolder holder, int position)
    {
        ImageButton removeButton = holder.getView().findViewById(R.id.button_remove_question);
        TextView textViewQuestion = holder.getView().findViewById(R.id.textView_question);

        List<String> updatedQuestions = Lists.newArrayList(mPreferences.questions().get());
        String question = updatedQuestions.get(position);

        textViewQuestion.setText(question);
        removeButton.setOnClickListener(btn -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) ->
            {
                updatedQuestions.remove(position);
                mPreferences.questions().set(updatedQuestions);
                refreshListView();
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete question")
                    .setMessage(String.format("You're going to remove the question \"%s\". Confirm?", question))
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", (dialog, which) -> {}).show();

        });

        holder.getView().findViewById(R.id.linearLayout_show).setOnClickListener(view -> {
            mBinder.applyBound(binder -> {
                binder.say(question, null);
            });
        });
    }


}
