package com.brianmtully.flutter.plugins.googlemlvision

import android.content.Context

internal class MlVisionHandler(applicationContext: Context) : MethodCallHandler {
    private val detectors: SparseArray<Detector> = SparseArray()
    private val applicationContext: Context

    init {
        this.applicationContext = applicationContext
    }

    @Override
    fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "BarcodeDetector#detectInImage", "FaceDetector#processImage", "ImageLabeler#processImage", "TextRecognizer#processImage" -> handleDetection(
                call,
                result
            )
            "BarcodeDetector#close", "FaceDetector#close", "ImageLabeler#close", "TextRecognizer#close" -> closeDetector(
                call,
                result
            )
            else -> result.notImplemented()
        }
    }

    private fun handleDetection(call: MethodCall, result: MethodChannel.Result) {
        val options: Map<String, Object> = call.argument("options")
        val image: InputImage
        val imageData: Map<String, Object> = call.arguments()
        image = try {
            dataToVisionImage(imageData)
        } catch (exception: IOException) {
            result.error("MLVisionDetectorIOError", exception.getLocalizedMessage(), null)
            return
        }
        var detector: Detector? = getDetector(call)
        if (detector == null) {
            when (call.method.split("#").get(0)) {
                "FaceDetector" -> detector = GMLKFaceDetector(options)
            }
            val handle: Integer = call.argument("handle")
            addDetector(handle, detector)
        }
        detector.handleDetection(image, result)
    }

    private fun closeDetector(call: MethodCall, result: MethodChannel.Result) {
        val detector: Detector = getDetector(call)
        if (detector == null) {
            val handle: Integer = call.argument("handle")
            val message: String = String.format("Object for handle does not exists: %s", handle)
            throw IllegalArgumentException(message)
        }
        try {
            detector.close()
            result.success(null)
        } catch (e: IOException) {
            val code: String = String.format("%sIOError", detector.getClass().getSimpleName())
            result.error(code, e.getLocalizedMessage(), null)
        } finally {
            val handle: Integer = call.argument("handle")
            detectors.remove(handle)
        }
    }

    @Throws(IOException::class)
    private fun dataToVisionImage(imageData: Map<String, Object>): InputImage? {
        val imageType = imageData["type"] as String?
        assert(imageType != null)
        return when (imageType) {
            "file" -> {
                val imageFilePath = imageData["path"] as String?
                val rotation = getImageExifOrientation(imageFilePath)
                if (rotation == 0) {
                    val file = File(imageFilePath)
                    return InputImage.fromFilePath(applicationContext, Uri.fromFile(file))
                }
                val matrix = Matrix()
                matrix.postRotate(rotation)
                val bitmap: Bitmap = BitmapFactory.decodeFile(imageFilePath)
                val rotatedBitmap: Bitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
                )
                InputImage.fromBitmap(rotatedBitmap, 0)
            }
            "bytes" -> {
                @SuppressWarnings("unchecked") val metadata: Map<String, Object>? =
                    imageData["metadata"]
                val bytes = imageData["bytes"] as ByteArray?
                assert(bytes != null)
                val width = metadata!!["width"] as Double?
                val intWidth: Int = width.intValue()
                val height = metadata["height"] as Double?
                val intHeight: Int = height.intValue()
                return try {
                    InputImage.fromByteArray(
                        bytes,
                        intWidth,
                        intHeight,
                        metadata["rotation"] as Int,
                        17
                    )
                } catch (exception: IllegalArgumentException) {
                    Log.e("GoogleMLVision ", "exception:", exception)
                    null
                }
                throw IllegalArgumentException(String.format("No image type for: %s", imageType))
            }
            else -> throw IllegalArgumentException(
                String.format(
                    "No image type for: %s",
                    imageType
                )
            )
        }
    }

    @Throws(IOException::class)
    private fun getImageExifOrientation(imageFilePath: String?): Int {
        val exif = ExifInterface(imageFilePath)
        val orientation: Int =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun addDetector(handle: Int, detector: Detector?) {
        if (detectors.get(handle) != null) {
            val message: String = String.format("Object for handle already exists: %s", handle)
            throw IllegalArgumentException(message)
        }
        detectors.put(handle, detector)
    }

    private fun getDetector(call: MethodCall): Detector {
        val handle: Integer = call.argument("handle")
        return detectors.get(handle)
    }
}