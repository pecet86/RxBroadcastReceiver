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

import static android.content.Context.RECEIVER_NOT_EXPORTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;


class RxBroadcastReceiver implements ObservableOnSubscribe<Intent> {

    @NonNull
    private final Context context;
    @NonNull
    private final IntentFilter intentFilter;

    RxBroadcastReceiver(@NonNull final Context context, @NonNull final IntentFilter intentFilter) {
        this.context = context.getApplicationContext();
        this.intentFilter = intentFilter;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void subscribe(final ObservableEmitter<Intent> emitter) {
        if (!Preconditions.checkLooperThread(emitter)) {
            return;
        }

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                emitter.onNext(intent);
            }
        };

        if (SDK_INT >= TIRAMISU) {
            if (myLooper() == getMainLooper()) {
                context.registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(receiver, intentFilter, null, new Handler(requireNonNull(myLooper())), RECEIVER_NOT_EXPORTED);
            }
        } else {
            if (myLooper() == getMainLooper()) {
                context.registerReceiver(receiver, intentFilter);
            } else {
                context.registerReceiver(receiver, intentFilter, null, new Handler(requireNonNull(myLooper())));
            }
        }

        emitter.setCancellable(() -> context.unregisterReceiver(receiver));
    }
}