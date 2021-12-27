/**
    Copyright 2020 ZynkSoftware SRL

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
    associated documentation files (the "Software"), to deal in the Software without restriction,
    including without limitation the rights to use, copy, modify, merge, publish, distribute,
    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or
    substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
    INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
    DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynksoftware.documentscanner.common.utils

import android.graphics.Bitmap
import android.graphics.PointF
import com.zynksoftware.documentscanner.common.extensions.scaleRectangle
import com.zynksoftware.documentscanner.common.extensions.toBitmap
import com.zynksoftware.documentscanner.common.extensions.toMat
import com.zynksoftware.documentscanner.ui.components.Quadrilateral
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*
import org.opencv.core.Mat

import org.opencv.core.CvType
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfInt













internal class OpenCvNativeBridge {

    companion object {
        private const val ANGLES_NUMBER = 4
        private const val EPSILON_CONSTANT = 0.02
        private const val CLOSE_KERNEL_SIZE = 10.0
        private const val CANNY_THRESHOLD_LOW = 80.0
        private const val CANNY_THRESHOLD_HIGH = 100.0
        private const val CUTOFF_THRESHOLD = 155.0
        private const val TRUNCATE_THRESHOLD = 150.0
        private const val NORMALIZATION_MIN_VALUE = 0.0
        private const val NORMALIZATION_MAX_VALUE = 255.0
        private const val BLURRING_KERNEL_SIZE = 5.0
        private const val DOWNSCALE_IMAGE_SIZE = 500.0
        private const val FIRST_MAX_CONTOURS = 10
    }

    fun getScannedBitmap(bitmap: Bitmap, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Bitmap {
        val rectangle = MatOfPoint2f()
        rectangle.fromArray(
            Point(x1.toDouble(), y1.toDouble()),
            Point(x2.toDouble(), y2.toDouble()),
            Point(x3.toDouble(), y3.toDouble()),
            Point(x4.toDouble(), y4.toDouble())
        )
        val dstMat = PerspectiveTransformation.transform(bitmap.toMat(), rectangle)
        return dstMat.toBitmap()
    }

//    fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
//        var point2f = getPoint(tempBitmap)
//        if (point2f == null) point2f = MatOfPoint2f()
//        val points: List<Point> = point2f.toArray().toList()
//        val result: MutableList<PointF> = ArrayList()
//
//        for (i in points.indices) {
//            result.add(PointF(points[i].x.toFloat(), points[i].y.toFloat()))
//        }
//
//        return result
//    }

    fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
        var point2f = getPoint(tempBitmap)
//        if (point2f == null) point2f = MatOfPoint2f()
        val points: List<PointF> = point2f.toArray().toList() as List<PointF>
        val result: MutableList<PointF> = ArrayList()

        for (i in points.indices) {
            result.add(PointF(points[i].x, points[i].y))
        }

        return result
    }

    fun getPoint(bitmap: Bitmap): ArrayList<PointF> {
        val src = bitmap.toMat()

//        val downscaledSize = Size(src.width() * ratio, src.height() * ratio)
//        val downscaled = Mat(downscaledSize, src.type())
//        Imgproc.resize(src, downscaled, downscaledSize)
        val originalSize = src.size()
        val largestRectangle = detectLargestQuadrilateral(src)

//        val heightWithRatio = originalSize.width / ratio

        val mat: Mat?
        val points : ArrayList<PointF> = ArrayList()
        if(largestRectangle != null){
            val ratio: Double = originalSize.height / 500
//            val ratio = 500 / max(src.width(), src.height())
//            val widthWithRatio = originalSize.height / ratio
//            val originalPoints = arrayOfNulls<Point>(4)

            points.add(PointF(largestRectangle.points[0].x.toFloat(), largestRectangle.points[0].y.toFloat()))
            points.add(PointF(largestRectangle.points[1].x.toFloat(), largestRectangle.points[1].y.toFloat()))
            points.add(PointF(largestRectangle.points[3].x.toFloat(), largestRectangle.points[3].y.toFloat()))
            points.add(PointF(largestRectangle.points[2].x.toFloat(), largestRectangle.points[2].y.toFloat()))
//            mat = fourPointTransform(src, originalPoints as Array<Point>)
//            mat = Mat(largestRectangle.points)
        }else{
//            mat = Mat(src.size(), CvType.CV_8UC4)
//            src.copyTo
            points.add(PointF((originalSize.width * 0.14f).toFloat(), originalSize.height.toFloat() * 0.13f))
            points.add(PointF((originalSize.width * 0.84f).toFloat(), originalSize.height.toFloat() * 0.13f))
            points.add(PointF((originalSize.width * 0.14f).toFloat(), originalSize.height.toFloat() * 0.83f))
            points.add(PointF((originalSize.width * 0.84f).toFloat(), originalSize.height.toFloat() * 0.83f))
        }

//        if (mat != null) {
//            enhanceDocument(mat)
//        }


//        val mat2f = MatOfPoint2f()
//        mat?.convertTo(mat2f, CvType.CV_32FC1)
        return points
    }

    private fun fourPointTransform(src: Mat, pts: Array<Point>): Mat? {
        val ratio = src.size().height / 500
        val tl = pts[0]
        val tr = pts[1]
        val br = pts[2]
        val bl = pts[3]
        val widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0))
        val widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0))
        val dw = Math.max(widthA, widthB) * ratio
        val maxWidth = java.lang.Double.valueOf(dw).toInt()
        val heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0))
        val heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0))
        val dh = Math.max(heightA, heightB) * ratio
        val maxHeight = java.lang.Double.valueOf(dh).toInt()
        val doc = Mat(maxHeight, maxWidth, CvType.CV_8UC4)
        val src_mat = Mat(4, 1, CvType.CV_32FC2)
        val dst_mat = Mat(4, 1, CvType.CV_32FC2)
        src_mat.put(
            0,
            0,
            tl.x * ratio,
            tl.y * ratio,
            tr.x * ratio,
            tr.y * ratio,
            br.x * ratio,
            br.y * ratio,
            bl.x * ratio,
            bl.y * ratio
        )
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh)
        val m = Imgproc.getPerspectiveTransform(src_mat, dst_mat)
        Imgproc.warpPerspective(src, doc, m, doc.size())
        return doc
    }

    private fun enhanceDocument(src: Mat) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
        src.convertTo(src, CvType.CV_8UC1, 1.0, 10.0)
    }

    // patch from Udayraj123 (https://github.com/Udayraj123/LiveEdgeDetection)
    fun detectLargestQuadrilateral(src: Mat): Quadrilateral? {

//        val ratio = src.size().height / 500
//        val height = src.size().height / ratio
//        val width = src.size().width / ratio
//        val size = Size(width, height)
//
        val resizedImage = Mat(src.size(), CvType.CV_8UC3)
//
//        Imgproc.resize(src, resizedImage, size)

        Imgproc.cvtColor(src, resizedImage, Imgproc.COLOR_RGBA2GRAY, 4);

        Imgproc.GaussianBlur(resizedImage, resizedImage, Size(5.0, 5.0), 0.0);

        Imgproc.Canny(resizedImage, resizedImage, CANNY_THRESHOLD_LOW, CANNY_THRESHOLD_HIGH, 3, false)

        val largestContour: List<MatOfPoint>? = findLargestContours(resizedImage)

        resizedImage.release()
        if (null != largestContour) {
            return getQuadrilateral(largestContour, src.size())
        }
        return null
    }

    private fun findContours(src: Mat): ArrayList<MatOfPoint?> {
        val grayImage: Mat
        val cannedImage: Mat
        val resizedImage: Mat
        val ratio = src.size().height / 500
        val height = java.lang.Double.valueOf(src.size().height / ratio).toInt()
        val width = java.lang.Double.valueOf(src.size().width / ratio).toInt()
        val size = Size(width.toDouble(), height.toDouble())
        resizedImage = Mat(size, CvType.CV_8UC4)
        grayImage = Mat(size, CvType.CV_8UC4)
        cannedImage = Mat(size, CvType.CV_8UC1)
        Imgproc.resize(src, resizedImage, size)
        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGBA2GRAY, 4)
        Imgproc.GaussianBlur(grayImage, grayImage, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(grayImage, cannedImage, 80.0, 100.0, 3, false)
        val contours: ArrayList<MatOfPoint?> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            cannedImage,
            contours,
            hierarchy,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        hierarchy.release()
        contours.sortWith { lhs, rhs ->
            Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
        }
        resizedImage.release()
        grayImage.release()
        cannedImage.release()
        return contours
    }

    private fun getQuadrilateral(contours: List<MatOfPoint>, srcSize: Size): Quadrilateral? {
        val ratio = srcSize.height / 500
        val height = java.lang.Double.valueOf(srcSize.height / ratio).toInt()
        val width = java.lang.Double.valueOf(srcSize.width / ratio).toInt()
        val size = Size(width.toDouble(), height.toDouble())
        for (c in contours) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
            val points = approx.toArray()

            // select biggest 4 angles polygon
            // if (points.length == 4) {
            val foundPoints = sortPoints(points)
            if (insideArea(foundPoints, size)) {
                val mat2f = MatOfPoint2f()
                c.convertTo(mat2f, CvType.CV_32FC2)
                return Quadrilateral(mat2f, foundPoints)
            }
            // }
        }
        return null
    }

    private fun findQuadrilateral(mContourList: List<MatOfPoint>, mSize: Size): Quadrilateral? {
        for (c in mContourList) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, EPSILON_CONSTANT * peri, true)

            val points = approx.toArray()

            val foundPoints: Array<Point> = sortPoints(points)

            if(insideArea(foundPoints, mSize)){
                return Quadrilateral(approx, foundPoints)
            }
        }
        return null
    }

    private fun distance(p1: Point, p2: Point): Double {
        return sqrt((p1.x - p2.x).pow(2.0) + (p1.y - p2.y).pow(2.0))
    }

    private fun sortPoints(src: Array<Point>): Array<Point> {
        val srcPoints: ArrayList<Point> = ArrayList(src.toList())
        val result = arrayOf<Point?>(null, null, null, null)
        val sumComparator: Comparator<Point> = Comparator<Point> { lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x) }
        val diffComparator: Comparator<Point> = Comparator<Point> { lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x) }

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)
        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)
        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)
        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)
        return result.map {
            it!!
        }.toTypedArray()
    }

    private fun insideArea(rp: Array<Point>, size: Size): Boolean {
        val width = java.lang.Double.valueOf(size.width).toInt()
        val height = java.lang.Double.valueOf(size.height).toInt()
        val minimumSize = width / 10
        val isANormalShape =
            rp[0].x !== rp[1].x && rp[1].y !== rp[0].y && rp[2].y !== rp[3].y && rp[3].x !== rp[2].x
        val isBigEnough = (rp[1].x - rp[0].x >= minimumSize && rp[2].x - rp[3].x >= minimumSize
                && rp[3].y - rp[0].y >= minimumSize && rp[2].y - rp[1].y >= minimumSize)
        val leftOffset = rp[0].x - rp[3].x
        val rightOffset = rp[1].x - rp[2].x
        val bottomOffset = rp[0].y - rp[1].y
        val topOffset = rp[2].y - rp[3].y
        val isAnActualRectangle = (leftOffset <= minimumSize && leftOffset >= -minimumSize
                && rightOffset <= minimumSize && rightOffset >= -minimumSize
                && bottomOffset <= minimumSize && bottomOffset >= -minimumSize
                && topOffset <= minimumSize && topOffset >= -minimumSize)
        return isANormalShape && isAnActualRectangle && isBigEnough
    }

    private fun findLargestContours(inputMat: Mat): List<MatOfPoint>? {
        val mHierarchy = Mat()
        val mContourList: List<MatOfPoint> = ArrayList()
        //finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        //finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        Imgproc.findContours(
            inputMat,
            mContourList,
            mHierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour
        val mHullList: MutableList<MatOfPoint> = ArrayList()
        val tempHullIndices = MatOfInt()
        for (i in mContourList.indices) {
            Imgproc.convexHull(mContourList[i], tempHullIndices)
            mHullList.add(hull2Points(tempHullIndices, mContourList[i]))
        }
        // Release mContourList as its job is done
        // Release mContourList as its job is done
        for (c in mContourList) c.release()
        tempHullIndices.release()
        mHierarchy.release()

        if (mHullList.size != 0) {
            Collections.sort(
                mHullList
            ) { lhs, rhs ->
                java.lang.Double.compare(
                    Imgproc.contourArea(rhs),
                    Imgproc.contourArea(lhs)
                )
            }
            return mHullList.subList(0, Math.min(mHullList.size, 5))
        }
        return null
    }

    private fun hull2Points(hull: MatOfInt, contour: MatOfPoint): MatOfPoint {
        val indexes = hull.toList()
        val points: MutableList<Point> = ArrayList()
        val ctrList = contour.toList()
        for (index in indexes) {
            points.add(ctrList[index])
        }
        val point = MatOfPoint()
        point.fromList(points)
        return point
    }

    fun contourArea(approx: MatOfPoint2f): Double {
        return Imgproc.contourArea(approx)
    }


}