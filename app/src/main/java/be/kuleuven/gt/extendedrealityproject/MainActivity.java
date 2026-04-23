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

        // Default tab
        if (savedInstanceState == null) {
            loadFragment(new BrowseFragment());
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}