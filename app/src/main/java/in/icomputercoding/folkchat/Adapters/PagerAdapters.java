package in.icomputercoding.folkchat.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import in.icomputercoding.folkchat.Fragments.ChatFragment;

public class PagerAdapters extends FragmentPagerAdapter {

    int tabcount;




    public PagerAdapters(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
        tabcount=behavior;




    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position)
        {
            case 0:
                return new ChatFragment();


            default:
                return null;
        }



    }

    @Override
    public int getCount() {
        return tabcount;
    }
}
