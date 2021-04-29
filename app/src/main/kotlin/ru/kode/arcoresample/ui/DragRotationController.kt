package ru.kode.arcoresample.ui

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.BaseTransformationController
import com.google.ar.sceneform.ux.DragGesture
import com.google.ar.sceneform.ux.DragGestureRecognizer
import com.google.ar.sceneform.ux.TransformableNode

class DragRotationController(
  private val transformableNode: TransformableNode,
  gestureRecognizer: DragGestureRecognizer
) : BaseTransformationController<DragGesture>(transformableNode, gestureRecognizer) {
  private var currentRotation: Float = 0.0F

  private val rotationRateDegrees = 0.6f
  private val animator = ValueAnimator.ofFloat(0f, 360f)

  init {
    animator.duration = 8000
    animator.interpolator = LinearInterpolator()
    animator.repeatMode = ValueAnimator.RESTART
    animator.repeatCount = ValueAnimator.INFINITE

    var previousValue = 0F
    animator.addUpdateListener { valueAnimator ->
      val currentValue = valueAnimator.animatedValue as Float
      if (currentValue < previousValue) {
        previousValue = 0F
      }
      val diff = currentValue - previousValue
      previousValue = currentValue
      rotate(diff)
    }

    setNodeRotation(currentRotation)
  }

  public override fun canStartTransformation(gesture: DragGesture): Boolean {
    return transformableNode.isSelected
  }

  override fun onDeactivated(node: Node?) {
    super.onDeactivated(node)
    pauseRotateAnimation()
  }

  override fun onGestureStarted(gesture: DragGesture?) {
    super.onGestureStarted(gesture)

    pauseRotateAnimation()
  }

  override fun onFinished(gesture: DragGesture?) {
    super.onFinished(gesture)

    startRotateAnimation()
  }

  private fun startRotateAnimation() {
    animator.start()
  }

  private fun pauseRotateAnimation() {
    animator.pause()
  }

  public override fun onContinueTransformation(gesture: DragGesture) {
    currentRotation -= (gesture.delta.x * rotationRateDegrees)

    setNodeRotation(currentRotation)
  }

  private fun rotate(delta: Float) {
    currentRotation += delta
    setNodeRotation(currentRotation)
  }

  private fun setNodeRotation(long: Float) {
    transformableNode.worldRotation = Quaternion.multiply(
      Quaternion(Vector3.up(), long),
      Quaternion(Vector3.forward(), 90f)
    )
  }

  public override fun onEndTransformation(gesture: DragGesture) = Unit

  fun onPause() {
    pauseRotateAnimation()
  }

  fun onResume() {
    startRotateAnimation()
  }
}
