package umu.software.activityrecognition.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.preferences.PreferencesActivity;
import umu.software.activityrecognition.preferences.RecordServicePreferences;
import umu.software.activityrecognition.services.recordings.ClassificationService;
import umu.software.activityrecognition.services.recordings.RecordService;
import umu.software.activityrecognition.services.recordings.RecordServiceHelper;
import umu.software.activityrecognition.shared.permissions.Permissions;
import umu.software.activityrecognition.shared.persistance.Directories;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;

import java.io.File;
import java.nio.file.Paths;




public class MainActivity extends MenuActivity
{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Permissions.askPermissions(this);
    }



    @Override
    protected void buildMenu()
    {
        RecordServiceHelper serviceHelper = RecordServiceHelper.newInstance(this);

        addMenuEntry(getString(R.string.menu_preferences), (e) -> {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
        });



        final String startRecordingString = getString(R.string.menu_start_recording);
        final String stopRecordingString  = getString(R.string.menu_stop_recording);
        addMenuEntry(0, startRecordingString, e -> {
            ServiceConnectionHandler.<ServiceBinder<RecordService>>execute(
                    getApplicationContext(),
                    RecordService.class,
                    binder1 -> {
                        if (binder1.getService().isRecording())
                        {
                            serviceHelper.stopRecording();
                            serviceHelper.stopRecurrentSave();
                            ((Button) e).setText(startRecordingString);
                        } else
                        {
                            serviceHelper.startRecording(null);
                            serviceHelper.startRecurrentSave();
                            ((Button) e).setText(stopRecordingString);
                        }
                    });
        });


        addMenuEntry(getString(R.string.menu_save_sensor_recordings), (e) -> {
            serviceHelper.saveZipClearFiles();
        });


        addMenuEntry(getString(R.string.menu_available_sensors), (e) -> {
            Intent intent = new Intent(this, SensorsActivity.class);
            startActivity(intent);
        });


        addMenuEntry(getString(R.string.menu_classify_activity), (e) -> {
            Intent i = new Intent(this, ClassificationService.class);
            i.setAction(ClassificationService.ACTION_CLASSIFY);
            startService(i);
            new ServiceConnectionHandler<ServiceBinder<ClassificationService>>(this)
                    .enqueue(binder -> {
                        binder.getService().setCallback( (ctx, intent) -> {
                            serviceHelper.setSensorsLabel(intent.getStringExtra(ClassificationService.EXTRA_ZIP_FILENAME));
                        });
                    }).bind(ClassificationService.class);
        });


        addMenuEntry(getString(R.string.menu_launch_assistant), (e) -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        });


        addMenuEntry(getString(R.string.menu_clear_save_folder), (e) -> {
            @SuppressLint("StringFormatMatches")
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) ->
            {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        try
                        {
                            RecordServicePreferences prefs = new RecordServicePreferences(this);
                                    prefs.saveFolderPath().get();

                            Directories.peformOnDirectory(prefs.saveFolderPath().get(), null, dir -> {
                                int numDeleted = dir.delete((f)-> true);
                                Toast.makeText(this, String.format(getString(R.string.menu_files_deleted_toast), numDeleted), Toast.LENGTH_SHORT).show();
                                return null;
                            });

                        } catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.menu_alert_confirm_delete).setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener).show();
        });


        addMenuEntry(getString(R.string.menu_show_app_folder), v -> {
            Uri uri = Uri.fromFile(new File(
                    Paths.get(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                            getResources().getString(R.string.application_documents_folder)
                    ).toString()));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try
            {
                startActivity(intent);
            }
            catch (ActivityNotFoundException e)
            {
                Toast.makeText(this, R.string.menu_intent_open_folder_failed, Toast.LENGTH_SHORT).show();
            }
        });

    }

}