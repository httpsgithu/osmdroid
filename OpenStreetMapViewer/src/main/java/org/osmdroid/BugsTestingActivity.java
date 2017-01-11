package org.osmdroid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import org.osmdroid.bugtestfragments.BugFactory;
import org.osmdroid.bugtestfragments.WeathForceActivity;
import org.osmdroid.model.IBaseActivity;
import org.osmdroid.samplefragments.BaseSampleFragment;
import org.osmdroid.samplefragments.ui.SamplesMenuFragment;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 6/29/16.
 */
public class BugsTestingActivity extends AppCompatActivity {
    public static final String SAMPLES_FRAGMENT_TAG = "org.osmdroid.BUGS_FRAGMENT_TAG";
    SamplesMenuFragment fragmentSamples;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.updateStoragePrefreneces(this);    //needed for unit tests
        setContentView(R.layout.activity_extra_samples);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        FragmentManager fm = this.getSupportFragmentManager();
        if (fm.findFragmentByTag(SAMPLES_FRAGMENT_TAG) == null) {
            List<IBaseActivity> extras = new ArrayList<>() ;
            extras.add(new WeathForceActivity());
            fragmentSamples = SamplesMenuFragment.newInstance(BugFactory.getInstance(),extras);
            fm.beginTransaction().add(org.osmdroid.R.id.samples_container, fragmentSamples, SAMPLES_FRAGMENT_TAG).commit();
        }
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        fragmentSamples=null;
    }

    /**
     * small example of keyboard events on the mapview
     * page up = zoom out
     * page down = zoom in
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event){
        Fragment frag = getSupportFragmentManager().findFragmentByTag(SAMPLES_FRAGMENT_TAG);
        if (frag==null) {
            return super.onKeyUp(keyCode, event);
        }
        if (!(frag instanceof BaseSampleFragment)) {
            return super.onKeyUp(keyCode,event);
        }
        MapView mMapView = ((BaseSampleFragment)frag).getmMapView();
        if (mMapView==null)
            return super.onKeyUp(keyCode,event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_DOWN:
                mMapView.getController().zoomIn();
                return true;
            case KeyEvent.KEYCODE_PAGE_UP:
                mMapView.getController().zoomOut();
                return true;
        }
        return super.onKeyUp(keyCode,event);
    }
}
