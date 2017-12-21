package test.so.audio_led;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import test.so.audio_led.extras.Constants;


/**
 * Created by saurabhvashisht on 07/06/17.
 */

public class GetPermissionDialogFragment extends DialogFragment implements Constants {
    private static String PERMISSION_REQUIRED = "permission_required";
    String permission;
    int messageId;
    private DialogInteractionListener mListener;

    public GetPermissionDialogFragment() {
    }

    public static GetPermissionDialogFragment newInstance(String permission) {
        GetPermissionDialogFragment fragment = new GetPermissionDialogFragment();
        Bundle args = new Bundle();
        args.putString(PERMISSION_REQUIRED, permission);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        permission = getArguments().getString(PERMISSION_REQUIRED);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


        if (permission.equalsIgnoreCase(PERMISSION_RECORD_AUDIO))
            messageId = R.string.get_record_permission;
        else if (permission.equalsIgnoreCase(PERMISSION_STORAGE))
            messageId = R.string.get_storage_permission;
        builder.setMessage(messageId)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                        mListener.requestPermissionAgain(permission);
                    }
                })
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dismiss();
                        mListener.exitApp();

                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogInteractionListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }


    public interface DialogInteractionListener {
        void exitApp();

        void requestPermissionAgain(String permissionType);
    }
}
