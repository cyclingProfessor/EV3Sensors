package uk.ac.rhul.cyclingprofessor.ev3sensors.ev3sensors;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ExitWithMessageDialogFragment extends DialogFragment {

    public ExitWithMessageDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static ExitWithMessageDialogFragment newInstance(String message) {
        ExitWithMessageDialogFragment frag = new ExitWithMessageDialogFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString("message", "Bad News : Cannot find message to display!");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Error - Leaving the Communicator");
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("Understood", (dialog, which) -> {
            ((MainActivity)getActivity()).exitApplication();
            dismiss();
        });

        return alertDialogBuilder.create();
    }
}
