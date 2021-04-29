package ru.kode.arcoresample.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.kode.arcoresample.databinding.Card3dViewBinding
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.N)
internal class Card3dView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  private val localModel = "card.sfb"
  private var dragRotationController: DragRotationController? = null

  private var disposable: Disposable? = null
  private val binding = Card3dViewBinding.inflate(LayoutInflater.from(context), this)
  private val sceneView = binding.sceneView

  init {
    orientation = VERTICAL

    // Waiting for the initialization of the field "SceneView.scene.lightProbe"
    val checkLightProbeIntervalMilliseconds = 50L
    disposable = Observable.interval(checkLightProbeIntervalMilliseconds, TimeUnit.MILLISECONDS)
      .map {
        try {
          sceneView.scene?.lightProbe == null
        } catch (e: IllegalStateException) {
          true
        }
      }
      .takeUntil { lightProbeIsNull -> !lightProbeIsNull }
      .filter { lightProbeIsNull -> !lightProbeIsNull }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          setupScene()
          renderLocalObject()
        },
        { e ->
          e.printStackTrace()
        }
      )
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    binding.sceneView.resume()
    dragRotationController?.onResume()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    sceneView.pause()
    dragRotationController?.onPause()

    disposable?.run {
      if (!isDisposed) dispose()
    }
    disposable = null
  }

  private fun setupScene() {
    sceneView.scene?.camera?.verticalFovDegrees = 45f
    sceneView.scene?.camera?.localPosition = Vector3(0f, 0f, 15f)

    val lightProbe = sceneView.scene?.lightProbe
    lightProbe?.rotation = Quaternion(Vector3.up(), 78F)
    lightProbe?.intensity = 2.5f
    sceneView.scene?.lightProbe = lightProbe

    sceneView.scene?.setUseHdrLightEstimate(true)
  }

  private fun renderLocalObject() {
    ModelRenderable.builder()
      .setSource(this.context, Uri.parse(localModel))
      .setRegistryId(localModel)
      .build()
      .thenAccept { modelRenderable: ModelRenderable ->
        addTransformationNodeToScene(modelRenderable)
      }
      .exceptionally { throwable: Throwable? ->
        throwable?.printStackTrace()
        null
      }
  }

  private fun addTransformationNodeToScene(model: ModelRenderable) {
    if (sceneView.scene != null) {

      val transformationSystem = makeTransformationSystem()
      val dragTransformableNode = TransformableNode(transformationSystem)

      // Default min is 0.75, default max is 1.75.
      dragTransformableNode.scaleController.minScale = 0.01f
      val scale = 0.06f
      dragTransformableNode.localScale = Vector3(scale, scale, scale)
      dragTransformableNode.select()
      sceneView.scene
        .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
          transformationSystem.onTouch(
            hitTestResult,
            motionEvent
          )
        }

      val node = Node()
      node.renderable = model
      node.localPosition = Vector3(0F, -50F, 0F)

      dragRotationController = DragRotationController(
        dragTransformableNode,
        transformationSystem.dragRecognizer
      )

      if (isAttachedToWindow) {
        dragRotationController?.onResume()
      } else {
        dragRotationController?.onPause()
      }

      sceneView.scene.addChild(dragTransformableNode)
      dragTransformableNode.addChild(node)
    }
  }

  private fun makeTransformationSystem(): TransformationSystem {
    val footprintSelectionVisualizer = FootprintSelectionVisualizer()
    return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
  }
}
