// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.brianmtully.flutter.plugins.googlemlvision

import android.graphics.PointF

internal class GMLKFaceDetector(options: Map<String, Object>) : Detector {
    private val detector: FaceDetector

    init {
        detector = FaceDetection.getClient(parseOptions(options))
    }

    @Override
    fun handleDetection(image: InputImage?, result: MethodChannel.Result) {
        detector
            .process(image)
            .addOnSuccessListener(
                object : OnSuccessListener<List<Face?>?>() {
                    @Override
                    fun onSuccess(foundFaces: List<Face?>) {
                        val faces: List<Map<String, Object>> = ArrayList(foundFaces.size())
                        for (face in foundFaces) {
                            val faceData: Map<String, Object> = HashMap()
                            faceData.put("left", face.getBoundingBox().left as Double)
                            faceData.put("top", face.getBoundingBox().top as Double)
                            faceData.put("width", face.getBoundingBox().width() as Double)
                            faceData.put("height", face.getBoundingBox().height() as Double)
                            faceData.put("headEulerAngleY", face.getHeadEulerAngleY())
                            faceData.put("headEulerAngleZ", face.getHeadEulerAngleZ())
                            if (face.getSmilingProbability() != null) {
                                faceData.put("smilingProbability", face.getSmilingProbability())
                            }
                            if (face.getLeftEyeOpenProbability()
                                != null
                            ) {
                                faceData.put(
                                    "leftEyeOpenProbability",
                                    face.getLeftEyeOpenProbability()
                                )
                            }
                            if (face.getRightEyeOpenProbability()
                                != null
                            ) {
                                faceData.put(
                                    "rightEyeOpenProbability",
                                    face.getRightEyeOpenProbability()
                                )
                            }
                            if (face.getTrackingId() != null) {
                                faceData.put("trackingId", face.getTrackingId())
                            }
                            faceData.put("landmarks", getLandmarkData(face))
                            faceData.put("contours", getContourData(face))
                            faces.add(faceData)
                        }
                        result.success(faces)
                    }
                })
            .addOnFailureListener(
                object : OnFailureListener() {
                    @Override
                    fun onFailure(@NonNull exception: Exception) {
                        result.error("faceDetectorError", exception.getLocalizedMessage(), null)
                    }
                })
    }

    private fun getLandmarkData(face: Face): Map<String, DoubleArray> {
        val landmarks: Map<String, DoubleArray> = HashMap()
        landmarks.put("bottomMouth", landmarkPosition(face, FaceLandmark.MOUTH_BOTTOM))
        landmarks.put("leftCheek", landmarkPosition(face, FaceLandmark.LEFT_CHEEK))
        landmarks.put("leftEar", landmarkPosition(face, FaceLandmark.LEFT_EAR))
        landmarks.put("leftEye", landmarkPosition(face, FaceLandmark.LEFT_EYE))
        landmarks.put("leftMouth", landmarkPosition(face, FaceLandmark.MOUTH_LEFT))
        landmarks.put("noseBase", landmarkPosition(face, FaceLandmark.NOSE_BASE))
        landmarks.put("rightCheek", landmarkPosition(face, FaceLandmark.RIGHT_CHEEK))
        landmarks.put("rightEar", landmarkPosition(face, FaceLandmark.RIGHT_EAR))
        landmarks.put("rightEye", landmarkPosition(face, FaceLandmark.RIGHT_EYE))
        landmarks.put("rightMouth", landmarkPosition(face, FaceLandmark.MOUTH_RIGHT))
        return landmarks
    }

    private fun getContourData(face: Face): Map<String, List<DoubleArray>> {
        val contours: Map<String, List<DoubleArray>> = HashMap()
        contours.put("allPoints", allContourPoints(face))
        contours.put("face", contourPosition(face, FaceContour.FACE))
        contours.put("leftEye", contourPosition(face, FaceContour.LEFT_EYE))
        contours.put(
            "leftEyebrowBottom", contourPosition(face, FaceContour.LEFT_EYEBROW_BOTTOM)
        )
        contours.put(
            "leftEyebrowTop", contourPosition(face, FaceContour.LEFT_EYEBROW_TOP)
        )
        contours.put(
            "lowerLipBottom", contourPosition(face, FaceContour.LOWER_LIP_BOTTOM)
        )
        contours.put("lowerLipTop", contourPosition(face, FaceContour.LOWER_LIP_TOP))
        contours.put("noseBottom", contourPosition(face, FaceContour.NOSE_BOTTOM))
        contours.put("noseBridge", contourPosition(face, FaceContour.NOSE_BRIDGE))
        contours.put("rightEye", contourPosition(face, FaceContour.RIGHT_EYE))
        contours.put(
            "rightEyebrowBottom",
            contourPosition(face, FaceContour.RIGHT_EYEBROW_BOTTOM)
        )
        contours.put(
            "rightEyebrowTop", contourPosition(face, FaceContour.RIGHT_EYEBROW_TOP)
        )
        contours.put(
            "upperLipBottom", contourPosition(face, FaceContour.UPPER_LIP_BOTTOM)
        )
        contours.put("upperLipTop", contourPosition(face, FaceContour.UPPER_LIP_TOP))
        return contours
    }

    private fun landmarkPosition(face: Face, landmarkInt: Int): DoubleArray? {
        val landmark: FaceLandmark = face.getLandmark(landmarkInt)
        return if (landmark != null) {
            doubleArrayOf(landmark.getPosition().x, landmark.getPosition().y)
        } else null
    }

    private fun contourPosition(face: Face, contourInt: Int): List<DoubleArray>? {
        val contour: FaceContour = face.getContour(contourInt)
        if (contour != null) {
            val contourPoints: List<PointF> = contour.getPoints()
            val result: List<DoubleArray> = ArrayList<DoubleArray>()
            for (i in 0 until contourPoints.size()) {
                result.add(doubleArrayOf(contourPoints[i].x, contourPoints[i].y))
            }
            return result
        }
        return null
    }

    private fun allContourPoints(face: Face): List<DoubleArray> {
        val contours: List<FaceContour> = face.getAllContours()
        val result: List<DoubleArray> = ArrayList<DoubleArray>()
        for (i in 0 until contours.size()) {
            val contourPoints: List<PointF> = contours[i].getPoints()
            for (j in 0 until contourPoints.size()) {
                result.add(doubleArrayOf(contourPoints[j].x, contourPoints[j].y))
            }
        }
        return result
    }

    private fun parseOptions(options: Map<String, Object>): FaceDetectorOptions {
        val classification: Int =
            if (options["enableClassification"]) FaceDetectorOptions.CLASSIFICATION_MODE_ALL else FaceDetectorOptions.CLASSIFICATION_MODE_NONE
        val landmark: Int =
            if (options["enableLandmarks"]) FaceDetectorOptions.LANDMARK_MODE_ALL else FaceDetectorOptions.LANDMARK_MODE_NONE
        val contours: Int =
            if (options["enableContours"]) FaceDetectorOptions.CONTOUR_MODE_ALL else FaceDetectorOptions.CONTOUR_MODE_NONE
        val mode: Int
        mode = when (options["mode"]) {
            "accurate" -> FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
            "fast" -> FaceDetectorOptions.PERFORMANCE_MODE_FAST
            else -> throw IllegalArgumentException("Not a mode:" + options["mode"])
        }
        val builder: FaceDetectorOptions.Builder = Builder()
            .setClassificationMode(classification)
            .setLandmarkMode(landmark)
            .setContourMode(contours)
            .setMinFaceSize((options["minFaceSize"] as Double).toFloat())
            .setPerformanceMode(mode)
        if (options["enableTracking"]) {
            builder.enableTracking()
        }
        return builder.build()
    }

    @Override
    @Throws(IOException::class)
    fun close() {
        detector.close()
    }
}