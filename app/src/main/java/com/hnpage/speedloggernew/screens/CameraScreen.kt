import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.google.common.util.concurrent.ListenableFuture
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.Executors



@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val capturedImage = remember { mutableStateOf<Bitmap?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val imageAnalyzer = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(Executors.newSingleThreadExecutor(), ImageAnalyzer { bitmap ->
                    capturedImage.value = bitmap
                })
            }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }



    LaunchedEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(null)
                }
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

        }, ContextCompat.getMainExecutor(context))
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        capturedImage.value?.let { bitmap ->
            val processedBitmap = detectDots(bitmap)
            Image(painter = rememberAsyncImagePainter(processedBitmap), contentDescription = null, modifier = Modifier.size(300.dp))
        }
        Button(onClick = {
            imageCapture?.let {
                takePhoto(it) { bitmap ->
                    capturedImage.value = bitmap
                }
            }
        }) {
            Text("Capture Image")
        }
    }
}

class ImageAnalyzer(private val onImageCaptured: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()
        onImageCaptured(bitmap)
        image.close()
    }
}

fun detectDots(bitmap: Bitmap): Bitmap {
    val mat = Mat()
    org.opencv.android.Utils.bitmapToMat(bitmap, mat)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
    Imgproc.GaussianBlur(mat, mat, Size(9.0, 9.0), 2.0)
    val circles = Mat()
    Imgproc.HoughCircles(mat, circles, Imgproc.HOUGH_GRADIENT, 1.0, mat.rows() / 8.0, 100.0, 30.0, 10, 100)

    for (i in 0 until circles.cols()) {
        val data = circles.get(0, i)
        val center = Point(data[0], data[1])
        val radius = data[2].toInt()
        Imgproc.circle(mat, center, radius, Scalar(255.0, 0.0, 0.0), 3)
    }

    val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    org.opencv.android.Utils.matToBitmap(mat, outputBitmap)
    return outputBitmap
}

fun takePhoto(imageCapture: ImageCapture, onImageCaptured: (Bitmap) -> Unit) {
    val executor = Executors.newSingleThreadExecutor()
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val bitmap = image.toBitmap()
            onImageCaptured(bitmap)
            image.close()
        }
        override fun onError(exception: ImageCaptureException) {
            exception.printStackTrace()
        }
    })
}
