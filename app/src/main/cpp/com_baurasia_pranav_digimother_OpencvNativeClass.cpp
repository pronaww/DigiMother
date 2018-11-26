#include "com_baurasia_pranav_digimother_OpencvNativeClass.h"

JNIEXPORT jint JNICALL Java_com_baurasia_pranav_digimother_OpencvNativeClass_findSquares
        (JNIEnv *, jclass, jlong addrSrc, jlong addrDest) {
    Mat& mOriginal = *(Mat*)addrSrc;
    Mat& mFinal = *(Mat*)addrDest;

    int conv;
    jint retVal;
    conv = toSquares(mOriginal, mFinal);

    retVal = (jint) conv;

    return retVal;
}


int thresh = 50, N = 11;

// helper function:
// finds a cosine of angle between vectors
// from pt0->pt1 and from pt0->pt2
static double angle( Point pt1, Point pt2, Point pt0 )
{
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

// returns sequence of squares detected on the image.
static void findSquares( const Mat& image, vector<vector<Point> >& squares )
{
    squares.clear();

    Mat pyr, timg, gray0(image.size(), CV_8U), gray;

    // down-scale and upscale the image to filter out the noise
    pyrDown(image, pyr, Size(image.cols/2, image.rows/2));
    pyrUp(pyr, timg, image.size());
    vector<vector<Point> > contours;

    // find squares in every color plane of the image
    for( int c = 0; c < 3; c++ )
    {
        int ch[] = {c, 0};
        mixChannels(&timg, 1, &gray0, 1, ch, 1);

        // try several threshold levels
        for( int l = 0; l < N; l++ )
        {
            // hack: use Canny instead of zero threshold level.
            // Canny helps to catch squares with gradient shading
            if( l == 0 )
            {
                // apply Canny. Take the upper threshold from slider
                // and set the lower to 0 (which forces edges merging)
                Canny(gray0, gray, 0, thresh, 5);
                // dilate canny output to remove potential
                // holes between edge segments
                dilate(gray, gray, Mat(), Point(-1,-1));
            }
            else
            {
                // apply threshold if l!=0:
                //     tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0
                gray = gray0 >= (l+1)*255/N;
            }

            // find contours and store them all as a list
            findContours(gray, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

            vector<Point> approx;

            double tableSize = 0.47*image.cols*image.rows;
            double imageSize = gray.cols*gray.rows;
            // test each contour
            for( size_t i = 0; i < contours.size(); i++ )
            {
                // approximate contour with accuracy proportional
                // to the contour perimeter
                approxPolyDP(contours[i], approx, arcLength(contours[i], true)*0.02, true);

                // square contours should have 4 vertices after approximation
                // relatively large area (to filter out noisy contours)
                // and be convex.
                // Note: absolute value of an area is used because
                // area may be positive or negative - in accordance with the
                // contour orientation
                if( approx.size() == 4 &&
//                        fabs(contourArea(approx)) > (1000000.0/3797376.0)*imageSize
                    fabs(contourArea(approx)) > 1000000
                    && fabs(contourArea(approx)) < 2100000
//                    && fabs(contourArea(approx)) < (1700000.0/3797376.0)*imageSize
                    && isContourConvex(approx) )
                {
                    double maxCosine = 0;

                    for( int j = 2; j < 5; j++ )
                    {
                        // find the maximum cosine of the angle between joint edges
                        double cosine = fabs(angle(approx[j%4], approx[j-2], approx[j-1]));
                        maxCosine = MAX(maxCosine, cosine);
                    }

                    // if cosines of all angles are small
                    // (all angles are ~90 degree) then write quandrange
                    // vertices to resultant sequence
                    if( maxCosine < 0.1 )
                        squares.push_back(approx);
                }
            }
        }
    }
}

static void secondLargestSquare( const vector<vector<Point>> &squares, vector<Point> &second) {
    vector<Point> largest;

    if(squares.size()>=2) {
        if (contourArea(squares[0]) < contourArea(squares[1])) {
            largest = squares[1];
            second = squares[0];
        } else {
            largest = squares[0];
            second = squares[1];
        }
    }
    else{
        second = squares[0];
    }
//    if (squares.size() >= 2) {
//        for (int i = 0; i < squares.size(); i++) {
//            if (contourArea(squares[i]) > contourArea(largest)) {
//                second = largest;
//                largest = squares[i];
//            }
//                /* If current array element is less than largest but greater
//                 * then second largest ("second" variable) then copy the
//                 * element to "second"
//                 */
//            else if (contourArea(squares[i]) > contourArea(second) &&
//                     contourArea(squares[i]) != contourArea(largest)) {
//                second = squares[i];
//            }
//        }
//    }
}


// the function draws all the squares in the image
static void drawSquares( Mat& image, const vector<vector<Point> >& squares )
{
    vector<Point> secondLargestSq;

    secondLargestSquare(squares, secondLargestSq);

    const Point* p = &secondLargestSq[0];
    int n = (int)secondLargestSq.size();
    polylines(image, &p, &n, 1, true, Scalar(0,255,0), 3, LINE_AA);

//    for( size_t i = 0; i < squares.size(); i++ )
//    {
//        const Point* p = &squares[i][0];
//        int n = (int)squares[i].size();
//        polylines(image, &p, &n, 1, true, Scalar(0,255,0), 3, LINE_AA);
//    }

}

int toSquares(Mat img, Mat& imgWithSq){
//    cvtColor(img, imgWithSq, CV_RGB2GRAY);

    vector<vector<Point> > squares;
    findSquares(img, squares);
    imgWithSq = img.clone();
    drawSquares(imgWithSq, squares);

    if(imgWithSq.rows==img.rows && imgWithSq.cols==img.cols)
        return 1;
    return 0;
}

JNIEXPORT jint JNICALL Java_com_baurasia_pranav_digimother_OpencvNativeClass_convertGray
  (JNIEnv *, jclass, jlong addrRgba, jlong addrGray) {
    Mat& mRgb = *(Mat*)addrRgba;
    Mat& mGray = *(Mat*)addrGray;

    int conv;
    jint retVal;
    conv = toGray(mRgb, mGray);

    retVal = (jint) conv;

    return retVal;
    }

    int toGray(Mat img, Mat& gray){
        cvtColor(img, gray, CV_RGB2GRAY);
        if(gray.rows==img.rows && gray.cols==img.cols)
            return 1;
        return 0;
    };

static void cutTable( Mat& image, const vector<vector<Point> >& squares )
{
    vector<Point> secondLargestSq;

    secondLargestSquare(squares, secondLargestSq);

    Rect boundRect = boundingRect( Mat(secondLargestSq) );
    image = image(boundRect);
}


int cropTable(Mat img, Mat& tableImg){
//    cvtColor(img, imgWithSq, CV_RGB2GRAY);

    vector<vector<Point> > squares;
    findSquares(img, squares);
    tableImg = img.clone();
    cutTable(tableImg, squares);


    if(tableImg.rows==img.rows && tableImg.cols==img.cols)
        return 1;
    return 0;
}

JNIEXPORT jint JNICALL Java_com_baurasia_pranav_digimother_OpencvNativeClass_cropTable
    (JNIEnv *, jclass, jlong addrImage, jlong addrTable) {
        Mat& image = *(Mat*)addrImage;
        Mat& imageTable = *(Mat*)addrTable;

        int conv;
        jint retVal;
        conv = cropTable(image, imageTable);

        retVal = (jint) conv;

        return retVal;
    }