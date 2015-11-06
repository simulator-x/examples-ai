package simx.applications.examples.ai.mipro.supervisedLearning.imageviewer

import simx.core.components.renderer.setup._
import simplex3d.math.floatx.{Vec3f, Mat4x3f, ConstMat4f}

/**
 * This object creates a display description for single display rendering setup.
 */
object DisplayConfiguration{

  /**
   * Creates a display setup description with two nodes that should fit for every desktop computer or laptop.
   *
   * @param widthInPx The amount of pixel in x direction. Must be larger than 0.
   * @param heightInPx The amount of pixel in y direction. Must be larger than 0.
   * @param fullscreen An optional parameter that sets if the application runs in fullscreen mode. Default value is false.
   * @param dpi The dots per inch of the screen. Must be larger than 0.0.
   * @return The display description with two windows on two different nodes.
   */
  def apply(widthInPx: Int, heightInPx: Int, fullscreen : Boolean = false, dpi : Double = 48.0) : DisplaySetupDesc = {
    require( widthInPx > 0, "The parameter 'widthInPx' must be larger than 0!" )
    require( heightInPx > 0, "The parameter 'heightInPx' must be larger than 0!" )
    require( dpi > 0.0, "The parameter 'dpi' must be larger than 0!" )

    val widthOfScreenInMeters = widthInPx / dpi * 0.0254
    val displayDesc = new DisplayDesc(
      if (fullscreen) None else Some( widthInPx -> heightInPx ),
      widthOfScreenInMeters -> widthOfScreenInMeters * heightInPx / widthInPx,
      ConstMat4f( Mat4x3f.translate( Vec3f( 0.0f, 0.0f, -0.6f ) ) ),
      new CamDesc( 0, Eye.RightEye, Some( 0.0f ) )
    )
    new DisplaySetupDesc().addDevice( new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay ), 0 )
  }
}