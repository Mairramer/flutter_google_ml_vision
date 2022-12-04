// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:ui';

import 'package:face_detection_ml_vision/google_ml_vision.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('$GoogleVision', () {
    final List<MethodCall> log = <MethodCall>[];
    dynamic returnValue;

    setUp(() {
      GoogleVision.channel.setMockMethodCallHandler((MethodCall methodCall) async {
        log.add(methodCall);

        switch (methodCall.method) {
          case 'BarcodeDetector#detectInImage':
            return returnValue;
          case 'FaceDetector#processImage':
            return returnValue;
          case 'TextRecognizer#processImage':
            return returnValue;
          default:
            return null;
        }
      });
      log.clear();
      GoogleVision.nextHandle = 0;
    });

    group('$FaceDetector', () {
      List<dynamic>? testFaces;

      setUp(() {
        testFaces = <dynamic>[
          <dynamic, dynamic>{
            'left': 0.0,
            'top': 1.0,
            'width': 2.0,
            'height': 3.0,
            'headEulerAngleY': 4.0,
            'headEulerAngleZ': 5.0,
            'leftEyeOpenProbability': 0.4,
            'rightEyeOpenProbability': 0.5,
            'smilingProbability': 0.2,
            'trackingId': 8,
            'landmarks': <dynamic, dynamic>{
              'bottomMouth': <dynamic>[0.1, 1.1],
              'leftCheek': <dynamic>[2.1, 3.1],
              'leftEar': <dynamic>[4.1, 5.1],
              'leftEye': <dynamic>[6.1, 7.1],
              'leftMouth': <dynamic>[8.1, 9.1],
              'noseBase': <dynamic>[10.1, 11.1],
              'rightCheek': <dynamic>[12.1, 13.1],
              'rightEar': <dynamic>[14.1, 15.1],
              'rightEye': <dynamic>[16.1, 17.1],
              'rightMouth': <dynamic>[18.1, 19.1],
            },
            'contours': <dynamic, dynamic>{
              'allPoints': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'face': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'leftEye': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'leftEyebrowBottom': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'leftEyebrowTop': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'lowerLipBottom': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'lowerLipTop': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'noseBottom': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'noseBridge': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'rightEye': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'rightEyebrowBottom': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'rightEyebrowTop': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'upperLipBottom': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
              'upperLipTop': <dynamic>[
                <dynamic>[1.1, 2.2],
                <dynamic>[3.3, 4.4],
              ],
            },
          },
        ];
      });

      test('processImage', () async {
        returnValue = testFaces;

        final FaceDetector detector = GoogleVision.instance.faceDetector(
          const FaceDetectorOptions(
            enableClassification: true,
            enableLandmarks: true,
            enableContours: true,
            minFaceSize: 0.5,
            mode: FaceDetectorMode.accurate,
          ),
        );

        final GoogleVisionImage image = GoogleVisionImage.fromFilePath(
          'empty',
        );

        final List<Face> faces = await detector.processImage(image);

        expect(log, <Matcher>[
          isMethodCall(
            'FaceDetector#processImage',
            arguments: <String, dynamic>{
              'handle': 0,
              'type': 'file',
              'path': 'empty',
              'bytes': null,
              'metadata': null,
              'options': <String, dynamic>{
                'enableClassification': true,
                'enableLandmarks': true,
                'enableContours': true,
                'enableTracking': false,
                'minFaceSize': 0.5,
                'mode': 'accurate',
              },
            },
          ),
        ]);

        final Face face = faces[0];
        // TODO(jackson): Use const Rect when available in minimum Flutter SDK
        // ignore: prefer_const_constructors
        expect(face.boundingBox, Rect.fromLTWH(0, 1, 2, 3));
        expect(face.headEulerAngleY, 4.0);
        expect(face.headEulerAngleZ, 5.0);
        expect(face.leftEyeOpenProbability, 0.4);
        expect(face.rightEyeOpenProbability, 0.5);
        expect(face.smilingProbability, 0.2);
        expect(face.trackingId, 8);

        for (final FaceLandmarkType type in FaceLandmarkType.values) {
          expect(face.getLandmark(type)!.type, type);
        }

        Offset p(FaceLandmarkType type) {
          return face.getLandmark(type)!.position;
        }

        expect(p(FaceLandmarkType.bottomMouth), const Offset(0.1, 1.1));
        expect(p(FaceLandmarkType.leftCheek), const Offset(2.1, 3.1));
        expect(p(FaceLandmarkType.leftEar), const Offset(4.1, 5.1));
        expect(p(FaceLandmarkType.leftEye), const Offset(6.1, 7.1));
        expect(p(FaceLandmarkType.leftMouth), const Offset(8.1, 9.1));
        expect(p(FaceLandmarkType.noseBase), const Offset(10.1, 11.1));
        expect(p(FaceLandmarkType.rightCheek), const Offset(12.1, 13.1));
        expect(p(FaceLandmarkType.rightEar), const Offset(14.1, 15.1));
        expect(p(FaceLandmarkType.rightEye), const Offset(16.1, 17.1));
        expect(p(FaceLandmarkType.rightMouth), const Offset(18.1, 19.1));

        List<Offset> c(FaceContourType type) {
          return face.getContour(type)!.positionsList;
        }

        expect(
          c(FaceContourType.allPoints),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.face),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.leftEye),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.leftEyebrowBottom),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.leftEyebrowTop),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.lowerLipBottom),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.lowerLipTop),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.noseBottom),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.noseBridge),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.rightEye),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.rightEyebrowBottom),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.rightEyebrowTop),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.upperLipBottom),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
        expect(
          c(FaceContourType.upperLipTop),
          containsAllInOrder(<Offset>[const Offset(1.1, 2.2), const Offset(3.3, 4.4)]),
        );
      });

      test('processImage with null landmark', () async {
        testFaces![0]['landmarks']['bottomMouth'] = null;
        returnValue = testFaces;

        final FaceDetector detector = GoogleVision.instance.faceDetector(
          const FaceDetectorOptions(),
        );
        final GoogleVisionImage image = GoogleVisionImage.fromFilePath(
          'empty',
        );

        final List<Face> faces = await detector.processImage(image);

        expect(faces[0].getLandmark(FaceLandmarkType.bottomMouth), isNull);
      });

      test('processImage no faces', () async {
        returnValue = <dynamic>[];

        final FaceDetector detector = GoogleVision.instance.faceDetector(
          const FaceDetectorOptions(),
        );
        final GoogleVisionImage image = GoogleVisionImage.fromFilePath(
          'empty',
        );

        final List<Face> faces = await detector.processImage(image);
        expect(faces, isEmpty);
      });
    });
  });
}
