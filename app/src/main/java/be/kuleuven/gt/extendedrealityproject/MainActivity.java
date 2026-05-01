package be.kuleuven.gt.extendedrealityproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import be.kuleuven.gt.extendedrealityproject.databinding.ActivityMainBinding;
import be.kuleuven.gt.extendedrealityproject.ui.browse.BrowseFragment;
import be.kuleuven.gt.extendedrealityproject.ui.sell.SellFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            if (!handleIntent(getIntent())) {
                loadFragment(new BrowseFragment());
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_browse) {
                loadFragment(new BrowseFragment());
                return true;
            } else if (id == R.id.nav_sell) {
                loadFragment(new SellFragment());
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private boolean handleIntent(android.content.Intent intent) {
        if (intent == null) {
            return false;
        }
        String itemId = intent.getStringExtra(be.kuleuven.gt.extendedrealityproject.camera.RecordingFlowContract.EXTRA_ITEM_ID);
        String title = intent.getStringExtra(be.kuleuven.gt.extendedrealityproject.camera.RecordingFlowContract.EXTRA_RECORDING_TITLE);
        String videoPath = intent.getStringExtra(be.kuleuven.gt.extendedrealityproject.camera.RecordingFlowContract.EXTRA_VIDEO_PATH);
        if (itemId == null && title == null) {
            return false;
        }

        SellFragment fragment = SellFragment.newInstance(itemId, title, videoPath);
        binding.bottomNavigation.setSelectedItemId(R.id.nav_sell);
        loadFragment(fragment);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}