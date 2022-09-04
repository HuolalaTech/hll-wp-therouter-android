package com.therouter.demo.b;

import com.therouter.TheRouter;
import com.therouter.demo.di.IRingReferenceTest2;
import com.therouter.demo.di.IRingReferenceTest3;
import com.therouter.inject.ServiceProvider;

/**
 * Created by ZhangTao on 17/10/13.
 */

public class RingReferenceTest2 implements IRingReferenceTest2 {

    IRingReferenceTest3 mRingReferenceTest3;

    public RingReferenceTest2(IRingReferenceTest3 ringReferenceTest3) {
        mRingReferenceTest3 = ringReferenceTest3;
    }

    public RingReferenceTest2() {
    }

    @ServiceProvider
    public static IRingReferenceTest2 create() {
        return new RingReferenceTest2(TheRouter.get(IRingReferenceTest3.class));
    }

    @Override
    public String getMessage() {
        return null;
    }
}
