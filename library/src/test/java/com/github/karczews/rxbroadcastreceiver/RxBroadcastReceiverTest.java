/*
 * Copyright (c) 2017-present, RxBroadcastReceiver Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.karczews.rxbroadcastreceiver;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.DeadObjectException;
import android.os.HandlerThread;
import android.os.Looper;

import com.github.karczews.utilsverifier.UtilsVerifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.BaseTestConsumer;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RxBroadcastReceiverTest {

    private static final String actionName = "testaction";
    private static final IntentFilter testIntentFilter = new IntentFilter(actionName);

    private static final Intent testIntent1 = new Intent(actionName).putExtra("testData", 1);
    private static final Intent testIntent2 = new Intent(actionName).putExtra("testData", 2);
    private static final Intent testIntent3 = new Intent(actionName).putExtra("testData", 3);

    @Test
    @LooperMode(LEGACY)
    public void shouldReceiveBroadcast() {
        //GIVEN
        final Context context = getApplicationContext();
        final HandlerThread handlerThread = new HandlerThread("TestHandlerThread2") {
            @Override
            protected void onLooperPrepared() {
                shadowOf(Looper.myLooper()).idle();
            }
        };
        handlerThread.start();

        final Observable<Intent> observable = RxBroadcastReceivers
                .fromIntentFilter(context, testIntentFilter)
                .subscribeOn(AndroidSchedulers.from(handlerThread.getLooper()));

        //WHEN
        final TestObserver<Intent> observer = observable.test();
        shadowOf(handlerThread.getLooper()).idle();

        //assertEquals(0, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent1);
        //assertEquals(1, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent2);
        //assertEquals(2, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent3);
        //assertEquals(3, shadowOf(handlerThread.getLooper()).getScheduler().size());

        //THEN
        observer.assertValueCount(3);
        observer.assertValues(testIntent1, testIntent2, testIntent3);
    }

    @Test
    public void shouldNotReceiveBroadcastAfterDisposed() {
        //GIVEN
        final Observable<Intent> observable = RxBroadcastReceivers
                .fromIntentFilter(getApplicationContext(), testIntentFilter);

        //WHEN
        final TestObserver<Intent> observer = observable.test(true);
        getApplicationContext().sendBroadcast(testIntent1);
        getApplicationContext().sendBroadcast(testIntent2);
        getApplicationContext().sendBroadcast(testIntent3);
        //THEN
        observer.assertValueCount(0);
        observer.assertEmpty();
    }

    @Test
    public void shouldReturnErrorWhenSubscribeOnNonLooperThread() {
        //GIVEN
        final Observable<Intent> observable = RxBroadcastReceivers
                .fromIntentFilter(getApplicationContext(), testIntentFilter)
                .subscribeOn(Schedulers.newThread());

        //WHEN
        final TestObserver<Intent> observer = observable.test();

        //THEN
        try {
            observer.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        CountDownLatch done = getDone(observer);
        if (done.getCount() != 0) {
            fail("Subscriber still running!");
        }
        long c = getCompletions(observer);
        if (c > 1) {
            fail("Terminated with multiple completions: " + c);
        }
        int s = getErrors(observer).size();
        if (s > 1) {
            fail("Terminated with multiple errors: " + s);
        }

        if (c != 0 && s != 0) {
            fail("Terminated with multiple completions and errors: " + c);
        }
    }

    /** @noinspection rawtypes */
    private static CountDownLatch getDone(BaseTestConsumer element) {
        try {
            Field field = BaseTestConsumer.class.getDeclaredField("done");
            field.setAccessible(true);
            return (CountDownLatch) field.get(element);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** @noinspection DataFlowIssue, rawtypes */
    private static long getCompletions(BaseTestConsumer element) {
        try {
            Field field = BaseTestConsumer.class.getDeclaredField("completions");
            field.setAccessible(true);
            return (Long) field.get(element);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** @noinspection rawtypes , unchecked, unchecked */
    private static List<Throwable> getErrors(BaseTestConsumer element) {
        try {
            Field field = BaseTestConsumer.class.getDeclaredField("errors");
            field.setAccessible(true);
            return (List) field.get(element);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @LooperMode(LEGACY)
    public void shouldReceiveBroadcastOnLooperThread() throws InterruptedException {
        //GIVEN
        final Context context = getApplicationContext();
        final Semaphore beforeLooperPrepare = new Semaphore(0);
        final Semaphore afterLooperPrepare = new Semaphore(0);
        //due to robolectic dirty hack to subscription to be run really on TestHandlerThread due to robolectric
        final HandlerThread handlerThread = new HandlerThread("TestHandlerThread") {
            @Override
            protected void onLooperPrepared() {
                try {
                    beforeLooperPrepare.acquire();
                } catch (final InterruptedException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                shadowOf(Looper.myLooper()).idle();
                afterLooperPrepare.release();
            }
        };

        handlerThread.start();
        final Observable<Intent> observable = RxBroadcastReceivers
                .fromIntentFilter(context, testIntentFilter)
                .subscribeOn(AndroidSchedulers.from(handlerThread.getLooper()));

        //WHEN
        final TestObserver<Intent> observer = observable.test();

        beforeLooperPrepare.release();
        afterLooperPrepare.acquire();

        assertEquals(0, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent1);
        assertEquals(1, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent2);
        assertEquals(2, shadowOf(handlerThread.getLooper()).getScheduler().size());
        context.sendBroadcast(testIntent3);
        assertEquals(3, shadowOf(handlerThread.getLooper()).getScheduler().size());

        shadowOf(handlerThread.getLooper()).idle();
        //THEN

        observer.assertValueCount(3);
        observer.assertValues(testIntent1, testIntent2, testIntent3);
    }

    @Test
    public void shouldSendErrorOnDisposalToThreadsUncaughtExceptionHandler() {
        // given context throws DeadSystemException when we try to unregister
        final UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        final Application applicationSpy = Mockito.spy(getApplicationContext());
        doAnswer(invocation -> {
            throw new DeadObjectException();
        }).when(applicationSpy).unregisterReceiver(any(BroadcastReceiver.class));
        when(applicationSpy.getApplicationContext()).thenReturn(applicationSpy);

        final TestObserver<Intent> observer = RxBroadcastReceivers
                .fromIntentFilter(applicationSpy, testIntentFilter)
                .test();
        // when dispose occurs
        observer.dispose();

        // then Threads UncaughtExceptionHandler receives the error via RxJavaPlugins.
        exceptionHandler.assertCaughtExceptionWithCauseType(DeadObjectException.class);
    }

    @Test
    public void shouldBeHaveWellDefinedUtil() {
        UtilsVerifier.forClass(RxBroadcastReceivers.class)
                .withConstructorThrowing(AssertionError.class)
                .verify();
    }

}
