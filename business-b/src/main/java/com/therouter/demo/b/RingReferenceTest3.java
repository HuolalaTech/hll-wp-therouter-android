package com.therouter.demo.b;

import com.therouter.TheRouter;
import com.therouter.demo.di.IRingReferenceTest;
import com.therouter.demo.di.IRingReferenceTest3;
import com.therouter.inject.ServiceProvider;

/**
 * Created by ZhangTao on 17/10/13.
 */

public class RingReferenceTest3 implements IRingReferenceTest3 {

    IRingReferenceTest mRingReferenceTest;

    public RingReferenceTest3(IRingReferenceTest ringReferenceTest) {
        mRingReferenceTest = ringReferenceTest;
    }

    @ServiceProvider
    public static IRingReferenceTest3 create() {
        return new RingReferenceTest3(TheRouter.get(IRingReferenceTest.class));
    }

    @Override
    public String getMessage() {
        return null;
    }
}
