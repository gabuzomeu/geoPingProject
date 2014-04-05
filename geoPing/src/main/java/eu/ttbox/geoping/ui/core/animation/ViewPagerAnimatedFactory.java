package eu.ttbox.geoping.ui.core.animation;

import android.support.v4.view.ViewPager;

import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.ui.core.animation.transformer.ViewPagerDepthPageTransformer;
import eu.ttbox.geoping.ui.core.animation.transformer.ViewPagerZoomOutPageTransformer;

public class ViewPagerAnimatedFactory {

    public static void aniamted(ViewPager viewPager) {
        if (VersionUtils.isHc11) {
           viewPager.setPageTransformer(true, new ViewPagerZoomOutPageTransformer());
//            viewPager.setPageTransformer(true, new ViewPagerDepthPageTransformer());

        }
    }

}
