package com.aware.app.stop;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.aware.plugin.myo.ContextCard;


/**
 * A simple {@link Fragment} subclass.
 */
public class WearFragment extends Fragment {

    private FrameLayout myoView;

    public WearFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wear, container, false);

        ContextCard card = new ContextCard();
        myoView = view.findViewById(R.id.myoView);
        myoView.addView(card.getContextCard(getActivity().getApplicationContext()));

        return view;
    }
}
