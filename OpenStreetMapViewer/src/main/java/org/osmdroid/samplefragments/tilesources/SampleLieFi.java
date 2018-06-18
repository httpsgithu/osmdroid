package org.osmdroid.samplefragments.tilesources;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.samplefragments.BaseSampleFragment;
import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IFilesystemCache;
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck;
import org.osmdroid.tileprovider.modules.MapTileApproximater;
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileSqlCacheProvider;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

/**
 * Lie Fi demo: we emulate a slow online source in order to show the offline first behavior
 * @since 6.0.2
 * @author Fabrice Fontaine
 */
public class SampleLieFi extends BaseSampleFragment {

    private final GeoPoint mInitialCenter = new GeoPoint(41.8905495, 12.4924348); // Rome, Italy
    private final double mInitialZoomLevel = 10;
    private final int mLieFieLag = 1000; // 1 second

    @Override
    public String getSampleTitle() {
          return "Lie Fi - slow online source";
     }
     
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final MapTileProviderArray provider = new MapTileProviderLieFi(inflater.getContext());
        mMapView = new MapView(inflater.getContext(), provider);
        return mMapView;
    }

    @Override
    protected void addOverlays() {
        super.addOverlays();

        mMapView.post(new Runnable() { // "post" because we need View.getWidth() to be set
            @Override
            public void run() {
                mMapView.getController().setZoom(mInitialZoomLevel);
                mMapView.setExpectedCenter(mInitialCenter);
            }
        });
    }

    private class MapTileProviderLieFi extends MapTileProviderArray implements IMapTileProviderCallback {

        private IFilesystemCache tileWriter;
        private final INetworkAvailablityCheck mNetworkAvailabilityCheck;

        private MapTileProviderLieFi(final Context pContext) {
            this(new SimpleRegisterReceiver(pContext), new NetworkAvailabliltyCheck(pContext),
                    TileSourceFactory.DEFAULT_TILE_SOURCE, pContext,null);
        }

        private MapTileProviderLieFi(final IRegisterReceiver pRegisterReceiver,
                                    final INetworkAvailablityCheck aNetworkAvailablityCheck, final ITileSource pTileSource,
                                    final Context pContext, final IFilesystemCache cacheWriter) {
            super(pTileSource, pRegisterReceiver);
            mNetworkAvailabilityCheck = aNetworkAvailablityCheck;

            if (cacheWriter != null) {
                tileWriter = cacheWriter;
            } else {
                if (Build.VERSION.SDK_INT < 10) {
                    tileWriter = new TileWriter();
                } else {
                    tileWriter = new SqlTileWriter();
                }
            }
            final MapTileAssetsProvider assetsProvider = new MapTileAssetsProvider(
                    pRegisterReceiver, pContext.getAssets(), pTileSource);
            mTileProviderList.add(assetsProvider);

            final MapTileFileStorageProviderBase cacheProvider;
            if (Build.VERSION.SDK_INT < 10) {
                cacheProvider = new MapTileFilesystemProvider(pRegisterReceiver, pTileSource);
            } else {
                cacheProvider = new MapTileSqlCacheProvider(pRegisterReceiver, pTileSource);
            }
            mTileProviderList.add(cacheProvider);

            final MapTileFileArchiveProvider archiveProvider = new MapTileFileArchiveProvider(
                    pRegisterReceiver, pTileSource);
            mTileProviderList.add(archiveProvider);

            final MapTileApproximater approximationProvider = new MapTileApproximater();
            mTileProviderList.add(approximationProvider);
            approximationProvider.addProvider(assetsProvider);
            approximationProvider.addProvider(cacheProvider);
            approximationProvider.addProvider(archiveProvider);

            final MapTileDownloader downloaderProvider = new MapTileDownloader(pTileSource, tileWriter,
                    aNetworkAvailablityCheck) {
                @Override
                protected int getLieFiLag() {
                    return mLieFieLag;
                }
            };
            mTileProviderList.add(downloaderProvider);
        }

        @Override
        public IFilesystemCache getTileWriter() {
            return tileWriter;
        }

        @Override
        public void detach(){
            //https://github.com/osmdroid/osmdroid/issues/213
            //close the writer
            if (tileWriter!=null)
                tileWriter.onDetach();
            tileWriter=null;
            super.detach();
        }

        /**
         * @since 6.0
         */
        @Override
        protected boolean isDowngradedMode() {
            return (mNetworkAvailabilityCheck != null && !mNetworkAvailabilityCheck.getNetworkAvailable())
                    || !useDataConnection();
        }
    }
}
