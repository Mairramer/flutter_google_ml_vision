// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.brianmtully.flutter.plugins.googlemlvision

import com.google.mlkit.vision.common.InputImage

interface Detector {
    fun handleDetection(image: InputImage?, result: MethodChannel.Result?)

    @Throws(IOException::class)
    fun close()
}