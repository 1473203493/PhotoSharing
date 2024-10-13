package Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.PhotoShare.R;
import com.google.android.material.tabs.TabLayout;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        // Set up the ViewPager with a FragmentPagerAdapter
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new DiscoverFragment(); // "发现" Fragment
                    case 1:
                        return new FocusFragment();   // "关注" Fragment
                    default:
                        return new Fragment(); // Default case
                }
            }

            @Override
            public int getCount() {
                return 2; // Two tabs
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "发现";
                    case 1:
                        return "关注";
                    default:
                        return null;
                }
            }
        });

        // Link the TabLayout with the ViewPager
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}
