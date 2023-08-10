package com.therouter.demo.b;

import com.therouter.TheRouter;
import com.therouter.demo.di.IRingReferenceTest;
import com.therouter.demo.di.IRingReferenceTest2;
import com.therouter.inject.ServiceProvider;

/**
 * Created by ZhangTao on 17/10/13.
 */

public class RingReferenceTest implements IRingReferenceTest {

    IRingReferenceTest2 mRingReferenceTest2;

    public RingReferenceTest(IRingReferenceTest2 ringReferenceTest2) {
        mRingReferenceTest2 = ringReferenceTest2;
    }

    @ServiceProvider
    public static IRingReferenceTest create() {
        return new RingReferenceTest(TheRouter.get(IRingReferenceTest2.class));
    }

    @Override
    public String getMessage() {
        return null;
    }
}
