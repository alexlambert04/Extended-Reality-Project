package be.kuleuven.gt.extendedrealityproject.ar;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;

public class ArViewerActivity extends AppCompatActivity {

    private static final String AR_VIEWER_TAG = "ar_viewer_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentContainerView containerView = new FragmentContainerView(this);
        containerView.setId(View.generateViewId());
        setContentView(containerView);

        if (savedInstanceState == null) {
            ArViewerFragment fragment = new ArViewerFragment();
            fragment.setArguments(ArViewerContract.toFragmentArgs(getIntent()));
            getSupportFragmentManager().beginTransaction()
                    .replace(containerView.getId(), fragment, AR_VIEWER_TAG)
                    .commit();
        }
    }
}

